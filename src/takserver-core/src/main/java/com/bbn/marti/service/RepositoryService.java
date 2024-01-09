

package com.bbn.marti.service;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import jakarta.annotation.PostConstruct;
import javax.naming.NamingException;
import jakarta.xml.bind.DatatypeConverter;

import com.bbn.marti.config.Configuration;
import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.core.instrument.Metrics;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.bbn.cot.filter.Filter;
import com.bbn.cot.filter.ImageFormattingFilter;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Repository;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.remote.ConnectionEventTypeValue;
import com.bbn.marti.remote.ImagePref;
import com.bbn.marti.remote.QueueMetric;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionMetadata;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.remote.util.SecureXmlParser;
import com.bbn.marti.util.FixedSizeBlockingQueue;
import com.bbn.marti.util.Iterables;
import com.bbn.marti.remote.util.SpringContextBeanForApi;

import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.Constants;
import tak.server.cache.CoTCacheHelper;
import tak.server.cache.MissionCacheResolver;
import tak.server.cot.CotElement;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;

public class RepositoryService extends BaseService {

	public static String CALLSIGN_DEST_XPATH = "/event/detail/marti/dest[@callsign]";

	@Autowired
	private SubscriptionManager subscriptionManager;

    // the DataSource will automatically obtained from the spring context
    @Autowired
    private HikariDataSource dataSource;

    @Autowired
    private CoTCacheHelper cotCacheHelper;

	private static RepositoryService instance = null;

	public static RepositoryService getInstance() {
		if (instance == null) {
			synchronized (RepositoryService.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(RepositoryService.class);
				}
			}
		}
		return instance;
	}

	private static final Logger log = Logger.getLogger(RepositoryService.class);
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

	private FixedSizeBlockingQueue<CotEventContainer> inputQueue = new FixedSizeBlockingQueue<CotEventContainer>();


	private static final Calendar utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

	private static int INSERTION_BATCH_SIZE;

	@PostConstruct
	public void init() {

		Repository repository = CoreConfigFacade.getInstance().getRemoteConfiguration().getRepository();
		dateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));

		INSERTION_BATCH_SIZE = repository.getInsertionBatchSize();

		if (INSERTION_BATCH_SIZE < 1) {
			throw new IllegalArgumentException("Invalid repository.insertionBatchSize " + INSERTION_BATCH_SIZE + " specified in CoreConfig");
		}
	}

	public RepositoryService() { }

	@Override
	public String name() {
		return "Repository";
	}

	@Override
	public boolean addToInputQueue(CotEventContainer c) {

	    if (c == null) {
            return false;
        }

	    try {
	    	if (c.hasContextKey(Constants.CLUSTER_MESSAGE_KEY)) {
	    		String messagingArchiver = (String) c.getContext((Constants.MESSAGING_ARCHIVER));
	    		if (messagingArchiver == null || IgniteHolder.getInstance().getIgniteId().compareTo(UUID.fromString(messagingArchiver)) != 0) {
	    			// don't archive clustered messages
		    		return false;
	    		}
	    	}

	    	// This is the case where c is a message from a plugin and the plugin archive is disabled
	        if (c.getContextValue(Constants.ARCHIVE_EVENT_KEY) != null && !((Boolean) c.getContextValue(Constants.ARCHIVE_EVENT_KEY)).booleanValue()) {
	            return false;
	        }

	        // This is the case where c is a datafeed message, and the datafeed archive is disabled
			if (c.getContextValue(Constants.DATA_FEED_KEY) != null && ((DataFeed) c.getContextValue(Constants.DATA_FEED_KEY)).isArchive() == false) {
				return false;
			}
	    } catch (Exception e) {
	        log.debug("exception checking archive flag in message", e);
	    }

		return inputQueue.add(c);
	}

	@Override
	protected void processNextEvent() {
		final List<CotEventContainer> batch = new ArrayList<>(INSERTION_BATCH_SIZE);
		final List<CotEventContainer> chat_batch = new ArrayList<>(INSERTION_BATCH_SIZE);

		try {

			CotEventContainer element = inputQueue.take(); // block for first message in batch

			if (element.getType().startsWith("b-t-f")) {
				chat_batch.add(element);
			} else {
				batch.add(element);
			}

			// try to get the rest of the batch as fast as possible
			for (int i = 0; i < INSERTION_BATCH_SIZE - 1; i++) {

				element = inputQueue.poll();

				if (element != null) {
					if (element.getType().startsWith("b-t-f")) {
						chat_batch.add(element);
					} else {
						batch.add(element);
					}
				}
			}
		} catch (InterruptedException e) { }

		if (!batch.isEmpty()) {

			try {

				Resources.messagePersistenceProcessor.execute(() -> {

					try {
						insertBatchCotData(batch);
					} catch (Exception e) {

						String msg = "Could not commit batch to DB: " + batch + " " + e.getClass().getName();

						if (e instanceof BatchUpdateException) {
							msg += " " + ((BatchUpdateException) e).getNextException().getMessage();
						}

						if (log.isErrorEnabled()) {
						log.error(msg, e);
					}
					}
				});

			} catch (RejectedExecutionException ree) {
				// count how often full queue has blocked message send
				Metrics.counter(Constants.METRIC_REPOSITORY_QUEUE_FULL_SKIP).increment();
			};
		}

		if (!chat_batch.isEmpty()) {

			try {

				Resources.messagePersistenceProcessor.execute(() -> {

					try {
						insertBatchChatData(chat_batch);
					} catch (Exception e) {

						String msg = "Could not commit chat to DB: " + chat_batch + " " + e.getClass().getName();

						if (e instanceof BatchUpdateException) {
							msg += " " + ((BatchUpdateException) e).getNextException().getMessage();
						}

						if (log.isErrorEnabled()) {
						log.error(msg, e);
					}
					}
				});

			} catch (RejectedExecutionException ree) {
				// count how often full queue has blocked message send
				Metrics.counter(Constants.METRIC_REPOSITORY_QUEUE_FULL_SKIP).increment();
			}
		}
	}

	private static final String cotRouterTableName = "cot_router";
	private static final String cotRouterChatTableName = "cot_router_chat";

	private static final Filter<CotEventContainer> imageFilter = new ImageFormattingFilter(ImagePref.DATABASE);

	public void insertBatchCotData(List<CotEventContainer> events) {

		try (Connection connection = dataSource.getConnection()) {
			LinkedList<CotEventContainer> dataFeedEvents = new LinkedList<>();
			try (PreparedStatement cotRouterInsert = connection.prepareStatement("INSERT INTO "
							+ cotRouterTableName
							+ " (uid, event_pt, cot_type, "
							+ "start, time, stale, detail, "
							+ "access, qos, opex, "
							+ "how, point_hae, point_ce, point_le, groups, "
							+ "id, servertime, caveat, releaseableto) VALUES "
							+ "(?,ST_GeometryFromText(?, 4326),?,?,?,?,?,?,?,?,?,?,?,?,(?)::bit(" + RemoteUtil.GROUPS_BIT_VECTOR_LEN + "), nextval('cot_router_seq'),?,?,?) ")) {

				// formats each cot message as we iterate over it
				Iterable<CotEventContainer> imageFormattedEvents = Iterables.filter(events, imageFilter);
				LinkedList<CotEventContainer> toRemove = new LinkedList<>();
				for (CotEventContainer event : imageFormattedEvents) {
					try {
						boolean[] groupsBitVector = new boolean[RemoteUtil.GROUPS_BIT_VECTOR_LEN];

						if (event.getContextValue(Constants.GROUPS_KEY) != null) {
							try {
								@SuppressWarnings("unchecked")
								NavigableSet<Group> groups = (NavigableSet<Group>) event.getContextValue(Constants.GROUPS_KEY);

								if (log.isDebugEnabled()) {
									log.debug("groups for message: " + event + ": " + groups);
								}

								groupsBitVector = RemoteUtil.getInstance().getBitVectorForGroups(groups);

								final boolean[] fgroupBitVector = groupsBitVector;

								if (CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue().isCacheCotInRepository()) {
									// cache latest CoT for each uid
									Resources.messageCacheProcessor.execute(() -> {
										cotCacheHelper.cacheCoT(event, RemoteUtil.getInstance().bitVectorToString(fgroupBitVector));
									});
								}

							} catch (ClassCastException e) {
								if (log.isDebugEnabled()) {
									log.debug("Not trying to get group info for message with invalid type of groups object: " + event);
								}
							}
						} else {
							if (log.isDebugEnabled()) {
                                log.debug("Groups context key not set for message: " + event);
                            }
						}

						if (Strings.isNullOrEmpty(event.getStart()) || Strings.isNullOrEmpty(event.getStale()) || Strings.isNullOrEmpty(event.getTime())) {
							if (log.isDebugEnabled()) {
							  log.debug("not archiving invalid CoT message (missing start, stale or time): " + event.asXml());
							}
							continue;
						}

						event.setContext(Constants.GROUPS_BIT_VECTOR_KEY, groupsBitVector);

						// CoT is valid, but came from a data feed. add it to the list and let {#archiveBatchDataFeedCot()}	handle it
						if (event.getContextValue(Constants.DATA_FEED_KEY) != null) {
							dataFeedEvents.add(event);
							continue;
						}

						setCotQueryParams(cotRouterInsert, event);

						cotRouterInsert.addBatch();
					} catch (Exception e) {
						log.error("Error parsing CoT message for insert into DB: " + e.toString(), e);
						cotRouterInsert.clearBatch();
						toRemove.add(event);
					}
				}
				for (CotEventContainer event : toRemove) {
					events.remove(event);
				}
				cotRouterInsert.executeBatch();

			} catch (SQLException e) {

				if (log.isDebugEnabled()) {
					log.debug("exception executing CoT insert batch ", e);
				}
			}

			archiveBatchDataFeedCot(dataFeedEvents, connection);

		} catch (SQLException eee) {
			if (log.isWarnEnabled()) {
				log.warn("unable to obtain database connection");
			}
		}
	}

	private void setCotQueryParams(PreparedStatement dataFeedInsert, CotEventContainer event) throws SQLException {

		dataFeedInsert.setString(1, event.getUid());
		dataFeedInsert.setString(2, "POINT(" + event.getLon() + " "
				+ event.getLat() + ")");
		dataFeedInsert.setString(3, event.getType());

		dataFeedInsert.setTimestamp(4, new Timestamp(DatatypeConverter
				.parseDateTime(event.getStart()).getTimeInMillis()), utcCalendar);
		dataFeedInsert.setTimestamp(5, new Timestamp(DatatypeConverter
				.parseDateTime(event.getTime()).getTimeInMillis()), utcCalendar);
		dataFeedInsert.setTimestamp(6, new Timestamp(DatatypeConverter
				.parseDateTime(event.getStale()).getTimeInMillis()), utcCalendar);

		dataFeedInsert.setString(7, event.getDetailXml());
		dataFeedInsert.setString(8, event.getAccess());
		dataFeedInsert.setString(9, event.getQos());
		dataFeedInsert.setString(10, event.getOpex());
		dataFeedInsert.setString(11, event.getHow());
		dataFeedInsert.setDouble(12, event.getHae());
		dataFeedInsert.setDouble(13, event.getCe());
		dataFeedInsert.setDouble(14, event.getLe());

		dataFeedInsert.setString(15, RemoteUtil.getInstance().bitVectorToString((boolean[]) event.getContext(Constants.GROUPS_BIT_VECTOR_KEY)));

		//
		// check to see if this event has serverTime (set by SubmissionService.processNextEvent)
		//
		String serverTime;
		if (event.hasServerTime()) {
			serverTime = event.getTime();
		} else {
			serverTime = DateUtil.toCotTime(new Date().getTime());
		}
		dataFeedInsert.setTimestamp(16, new Timestamp(DatatypeConverter
				.parseDateTime(serverTime).getTimeInMillis()), utcCalendar);

		dataFeedInsert.setString(17, event.getCaveat());
		dataFeedInsert.setString(18, event.getReleaseableTo());
	}

	// link the Cot UID to the data feed it came from
	private void archiveBatchDataFeedCot(List<CotEventContainer> events, Connection connection) {

		if (events.size() != 0) {
			try (PreparedStatement dataFeedInsert = connection.prepareStatement(""
					+ "WITH inserted_row as (INSERT INTO "
					+ cotRouterTableName
					+ " (uid, event_pt, cot_type, "
					+ "start, time, stale, detail, "
					+ "access, qos, opex, "
					+ "how, point_hae, point_ce, point_le, groups, "
					+ "id, servertime, caveat, releaseableto) VALUES "
					+ "(?,ST_GeometryFromText(?, 4326),?,?,?,?,?,?,?,?,?,?,?,?,(?)::bit("
					+ RemoteUtil.GROUPS_BIT_VECTOR_LEN + "), nextval('cot_router_seq'),?,?,?) returning id)"
					+ " INSERT INTO data_feed_cot (cot_router_id, data_feed_id) VALUES ((SELECT id FROM inserted_row), (SELECT id FROM data_feed WHERE uuid = ?))")) {

				for (CotEventContainer event : events) {
					try {
						setCotQueryParams(dataFeedInsert, event);

						dataFeedInsert.setString(19, ((DataFeed) event.getContext(Constants.DATA_FEED_KEY)).getUuid());

						dataFeedInsert.execute();
					} catch (Exception e) {
						log.error("Error with datafeed batch insert: " + e.toString(), e);
						dataFeedInsert.clearBatch();
					}
				}
			} catch (SQLException e) {
				log.error("exception executing datafeed insert batch ", e);
				if (log.isDebugEnabled()) {
					log.debug("exception executing datafeed insert batch ", e);
				}
			}
		}
	}

	private void extractGroupContacts(org.w3c.dom.Node node, HashMap<String, String> callsignUidMap) {
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			org.w3c.dom.Node child = nodeList.item(i);
			if (child.getNodeName().compareTo("group") == 0) {
				extractGroupContacts(child, callsignUidMap);
			} else if (child.getNodeName().compareTo("contact") == 0) {
				String name = child.getAttributes().getNamedItem("name").getNodeValue();
				String value = child.getAttributes().getNamedItem("uid").getNodeValue();
				callsignUidMap.put(name, value);
			} else {
				log.error("found unknown chat element type : " + child.getNodeName());
			}
		}
	}

	private void insertBatchChatData(List<CotEventContainer> events) {
		try (Connection connection = dataSource.getConnection()) {

			try (PreparedStatement cotRouterChatInsert = connection.prepareStatement("INSERT INTO "
					+ cotRouterChatTableName
					+ " (uid, event_pt, cot_type, "
					+ "start, time, stale, detail, "
					+ "access, qos, opex, "
					+ "how, point_hae, point_ce, point_le, groups, "
					+ "id, servertime, sender_callsign, dest_callsign, dest_uid, chat_content, chat_room) VALUES "
					+ "(?,ST_GeometryFromText(?, 4326),?,?,?,?,?,?,?,?,?,?,?,?,(?)::bit(" +
					RemoteUtil.GROUPS_BIT_VECTOR_LEN + "), nextval('cot_router_seq'),?,?,?,?,?,?) ")) {

				LinkedList<CotEventContainer> toRemove = new LinkedList<>();
				for (CotEventContainer event : events) {
					try {
						boolean[] groupsBitVector = new boolean[RemoteUtil.GROUPS_BIT_VECTOR_LEN];

						if (event.getContextValue(Constants.GROUPS_KEY) != null) {
							try {
								@SuppressWarnings("unchecked")
								NavigableSet<Group> groups = (NavigableSet<Group>) event.getContextValue(Constants.GROUPS_KEY);

								if (log.isDebugEnabled()) {
									log.debug("groups for message: " + event + ": " + groups);
								}

								groupsBitVector = RemoteUtil.getInstance().getBitVectorForGroups(groups);

							} catch (ClassCastException e) {
								if (log.isDebugEnabled()) {
									log.debug("Not trying to get group info for message with invalid type of groups object: " + event);
								}
							}
						} else {
							if (log.isDebugEnabled()) {
							log.debug("Groups context key not set for message: " + event);
						}
						}

						cotRouterChatInsert.setString(1, event.getUid());
						cotRouterChatInsert.setString(2, "POINT(" + event.getLon() + " "
								+ event.getLat() + ")");
						cotRouterChatInsert.setString(3, event.getType());
						cotRouterChatInsert.setTimestamp(4, new Timestamp(DatatypeConverter
								.parseDateTime(event.getStart()).getTimeInMillis()), utcCalendar);
						cotRouterChatInsert.setTimestamp(5, new Timestamp(DatatypeConverter
								.parseDateTime(event.getTime()).getTimeInMillis()), utcCalendar);
						cotRouterChatInsert.setTimestamp(6, new Timestamp(DatatypeConverter
								.parseDateTime(event.getStale()).getTimeInMillis()), utcCalendar);
						cotRouterChatInsert.setString(7, event.getDetailXml());
						cotRouterChatInsert.setString(8, event.getAccess());
						cotRouterChatInsert.setString(9, event.getQos());
						cotRouterChatInsert.setString(10, event.getOpex());
						cotRouterChatInsert.setString(11, event.getHow());
						cotRouterChatInsert.setDouble(12, event.getHae());
						cotRouterChatInsert.setDouble(13, event.getCe());
						cotRouterChatInsert.setDouble(14, event.getLe());
						cotRouterChatInsert.setString(15, RemoteUtil.getInstance().
								bitVectorToString(groupsBitVector));

						//
						// check to see if this event has serverTime (set by SubmissionService.processNextEvent)
						//
						String serverTime;
						if (event.hasServerTime()) {
							serverTime = event.getTime();
						} else {
							serverTime = DateUtil.toCotTime(new Date().getTime());
						}
						cotRouterChatInsert.setTimestamp(16, new Timestamp(DatatypeConverter
								.parseDateTime(serverTime).getTimeInMillis()), utcCalendar);

						//
						// Parse out the dest_uid from the chat message
						//

						//
						// Example P2P chat message. In this case the client passes the dest Uid as chatgrp.uid1
						//
						//<?xml version="1.0" encoding="UTF-8"?>
						//<event version="2.0" uid="GeoChat.ANDROID-d8c7f552857ee744.HUMMER.a77c739b-213a-4f74-879f-014175867dc9" type="b-t-f" how="h-g-i-g-o" time="2021-04-19T15:14:08Z" start="2021-04-19T15:14:08Z" stale="2021-04-20T15:14:08Z">
						//  <point lat="0.0" lon="0.0" hae="9999999.0" ce="20.0" le="9999999.0"/>
						//  <detail>
						//    <__chat parent="RootContactGroup" groupOwner="false" chatroom="HUMMER" id="S-1-5-21-3476584777-3751678839-889941227-17669" senderCallsign="DEMO2">
						//      <chatgrp uid0="ANDROID-d8c7f552857ee744" uid1="S-1-5-21-3476584777-3751678839-889941227-17669" id="S-1-5-21-3476584777-3751678839-889941227-17669"/>
						//    </__chat>
						//    <link uid="ANDROID-d8c7f552857ee744" type="a-f-G-U-C" relation="p-p"/>
						//    <remarks source="BAO.F.ATAK.ANDROID-d8c7f552857ee744" to="S-1-5-21-3476584777-3751678839-889941227-17669" time="2021-04-19T15:14:08.769Z">Roger</remarks>
						//    <__serverdestination destinations="10.0.2.16:4242:tcp:ANDROID-d8c7f552857ee744"/>
						//    <marti>
						//      <dest callsign="HUMMER"/>
						//    </marti>
						//  </detail>
						//</event>

						//
						// Example group chat message. In this case the client passes the dest uid in the contact list
						//
						//<?xml version="1.0" encoding="UTF-8"?>
						//<event version="2.0" uid="GeoChat.ANDROID-d8c7f552857ee744.testing.eb47ce94-efdc-4841-a9a7-6ee061200355" type="b-t-f" how="h-g-i-g-o" time="2021-04-19T16:18:39Z" start="2021-04-19T16:18:39Z" stale="2021-04-20T16:18:39Z">
						//  <point lat="0.0" lon="0.0" hae="9999999.0" ce="20.0" le="9999999.0"/>
						//  <detail>
						//    <__chat parent="UserGroups" groupOwner="true" chatroom="testing" id="d3317b6f-bd26-4bed-a5f2-eba268651a0d" senderCallsign="DEMO2">
						//      <chatgrp uid2="S-1-5-21-3476584777-3751678839-889941227-17669" uid0="ANDROID-d8c7f552857ee744" uid1="ANDROID-304275b91c93405e" id="d3317b6f-bd26-4bed-a5f2-eba268651a0d"/>
						//      <hierarchy>
						//        <group uid="UserGroups" name="Groups">
						//          <group uid="d3317b6f-bd26-4bed-a5f2-eba268651a0d" name="testing">
						//            <contact uid="S-1-5-21-3476584777-3751678839-889941227-17669" name="HUMMER"/>
						//            <contact uid="ANDROID-d8c7f552857ee744" name="DEMO2"/>
						//            <contact uid="ANDROID-304275b91c93405e" name="TRON"/>
						//          </group>
						//        </group>
						//      </hierarchy>
						//    </__chat>
						//    <link uid="ANDROID-d8c7f552857ee744" type="a-f-G-U-C" relation="p-p"/>
						//    <remarks source="BAO.F.ATAK.ANDROID-d8c7f552857ee744" time="2021-04-19T16:18:39.428Z">Roger</remarks>
						//    <__serverdestination destinations="10.0.2.16:4242:tcp:ANDROID-d8c7f552857ee744"/>
						//    <marti>
						//      <dest callsign="TRON"/>
						//    </marti>
						//  </detail>
						//</event>

						String destUid = null;
						String remarks = null;
						String chatRoom = null;
						String senderCallsign = null;
						HashMap<String, String> callsignUidMap = new HashMap<>();
						LinkedList<String> allGroupUids = new LinkedList<>();

						try {
							Document detailDocument = SecureXmlParser.makeDocument(event.getDetailXml());
							org.w3c.dom.Node detail = detailDocument.getElementsByTagName("detail").item(0);
							NodeList detailList = detail.getChildNodes();

							// iterate over the chat message's detail elements
							for (int i = 0; i < detailList.getLength(); i++) {
								org.w3c.dom.Node node = detailList.item(i);
								if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
									continue;
								}

								if (node.getNodeName().compareTo("__chat") == 0 ||
										node.getNodeName().compareTo("__chatreceipt") == 0) {

									NodeList chat = node.getChildNodes();
									for (int j = 0; j < chat.getLength(); j++) {
										org.w3c.dom.Node chatNode = chat.item(j);
										if (chatNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
											if (chatNode.getNodeName().compareTo("chatgrp") == 0) {
												//
												// take a look at the chatgrp, for p2p messages this will be have 3 attributes
												// id=chat group id, uid0=sender uid, uid1=dest uid
												//
												NamedNodeMap attributes = chatNode.getAttributes();
												if (attributes.getLength() == 3) {
													destUid = attributes.getNamedItem("uid1").getNodeValue();
												} else if (attributes.getLength() > 3) {
													for (int k = 0; k < attributes.getLength(); k++) {
														org.w3c.dom.Node uidAttr = attributes.item(k);
														if (uidAttr.getNodeName().startsWith("uid") &&
																!uidAttr.getNodeName().equalsIgnoreCase("uid0")) {
															allGroupUids.add(uidAttr.getNodeValue());
														}
													}
												}
											} else if (chatNode.getNodeName().compareTo("hierarchy") == 0) {
												// if there is hierarchy present that means we have a group message
												extractGroupContacts(chatNode, callsignUidMap);
											}
										}
									}

									NamedNodeMap attributes = node.getAttributes();
									if (attributes != null) {
										if (attributes.getNamedItem("senderCallsign") != null) {
											senderCallsign = attributes.getNamedItem("senderCallsign").getNodeValue();
										}
										if (attributes.getNamedItem("chatroom") != null) {
											chatRoom = attributes.getNamedItem("chatroom").getNodeValue();
										}
									}

								} else if (node.getNodeName().compareTo("remarks") == 0) {
									remarks = node.getTextContent();
								}
							}
						} catch (Exception e) {
							log.error("exception parsing chat detail!", e);
							continue;
						}

						String destCallsign = null;
						List<Node> callsignDestList = event.getDocument().selectNodes(CALLSIGN_DEST_XPATH);
						for (Node destElem : callsignDestList) {
							Element detached = (Element) destElem.detach();
							if (detached.attribute("callsign") != null) {
								destCallsign = detached.attributeValue("callsign");
								// if we haven't gotten the destUid from the chatgrp above, we must have a group
								// chat message, so get the destUid from the callsignUid map
								if (destUid == null) {
									destUid = callsignUidMap.get(destCallsign);
								}
							}
						}

						// bail if we haven't been able to get a destUid by this point
						if (destUid == null && allGroupUids.size() == 0) {
							log.error("unable to parse destUid from chat message! " + event.getDetailXml());
							continue;
						}

						cotRouterChatInsert.setString(17, senderCallsign);
						cotRouterChatInsert.setString(18, destCallsign);
						cotRouterChatInsert.setString(20, remarks);
						cotRouterChatInsert.setString(21, chatRoom);

						if (destUid != null) {
							cotRouterChatInsert.setString(19, destUid);
							cotRouterChatInsert.addBatch();
						} else if (allGroupUids.size() != 0) {
							for (String allGroupUid : allGroupUids) {
								cotRouterChatInsert.setString(19, allGroupUid);
								cotRouterChatInsert.addBatch();
							}
						}

					} catch (Exception e) {
						log.error("Error parsing chat message for insert into DB: " + e.toString(), e);
						cotRouterChatInsert.clearBatch();
						toRemove.add(event);
					}
				}
				for (CotEventContainer event : toRemove) {
					events.remove(event);
				}
				cotRouterChatInsert.executeBatch();

			} catch (SQLException e) {

				if (log.isDebugEnabled()) {
					log.debug("exception executing chat insert batch ", e);
				}
			}

		} catch (SQLException eee) {
			if (log.isWarnEnabled()) {
				log.warn("unable to obtain database connection");
			}
		}
	}

	public List<CotElement> getChatMessagesForUidSinceLastDisconnect(String uid, long storeForwardQueryBufferMs, String groupVector) {

		try (Connection connection = dataSource.getConnection()) {
			String sql = "select  " +
					" id, uid, cot_type, how, ST_X(event_pt), ST_Y(event_pt), " +
					" point_hae, point_le, point_ce, servertime, stale, detail, groups " +
					" from cot_router_chat where (dest_uid = 'All Chat Rooms' or ? = dest_uid )" +
					" and servertime >  " +

					"( select  max(cee.created_ts) - cast(? || 'milliseconds' as interval) from client_endpoint_event cee " +
					" join client_endpoint ce on cee.client_endpoint_id = ce.id " +
					" join connection_event_type cet on cee.connection_event_type_id = cet.id " +
					" where cet.event_name = 'Disconnected' and ce.uid = ? ) " +

					RemoteUtil.getInstance().getGroupAndClause() +
					" order by servertime ";

			return new JdbcTemplate(dataSource).query(sql,
					new ResultSetExtractor<List<CotElement>>() {
						@Override
						public List<CotElement> extractData(ResultSet results) throws SQLException {
							if (results == null) {
								throw new IllegalStateException("null result set");
							}

							LinkedList<CotElement> cotElements = new LinkedList<>();
							while (results.next()) {
								CotElement cotElement = new CotElement();
								cotElement.cotId = results.getLong(1);
								cotElement.uid = results.getString(2);
								cotElement.cottype = results.getString(3);
								cotElement.how = results.getString(4);
								cotElement.lon = results.getDouble(5);
								cotElement.lat = results.getDouble(6);
								cotElement.hae = results.getString(7);
								cotElement.le = results.getDouble(8);
								cotElement.ce = results.getDouble(9);
								cotElement.servertime = results.getTimestamp(10);
								cotElement.staletime = results.getTimestamp(11);
								cotElement.detailtext = results.getString(12);
								cotElement.groupString = results.getString(13);

								cotElements.add(cotElement);
							}

							return cotElements;
						}
					},
					uid, storeForwardQueryBufferMs, uid, groupVector);

		} catch (Exception e) {
			log.error("exception in getChatMessagesForUidSinceLastDisconnect ", e);
			return null;
		}
	}

	private Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public boolean hasRoomInQueueFor(CotEventContainer c) {
		return inputQueue.getQueueMetrics().currentSize.get()
				< inputQueue.getQueueMetrics().capacity.get();
	}

	/**
	 * Insert a row into the client_endpoint table to represent a callsign/uid pair (if a row doesn't already exist)
	 * and creates a new client_endpoint_event row to mark a new connection or disconnection event (or some other event in the future).
	 * Exceptions are indicated by the return value but not thrown. This allows critical processing to continue.
	 *
	 *
	 * @param callsign  String (required)
	 * @param uid       String (required)
	 * @param eventType ConnectionEventTypeValue (required)
	 */
	public void auditCallsignUIDEventAsync(String callsign, String uid, String username, ConnectionEventTypeValue eventType, String groupVector) {

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();
		if (!config.getRepository().isEnable()) {
			return;
		}

		if (!config.getRepository().isEnableCallsignAudit()) {
			return;
		}

		// don't include the generated uuid for anonymous users in the audit
		if (username.startsWith(GroupFederationUtil.ANONYMOUS_USERNAME_BASE)) {
			username = GroupFederationUtil.ANONYMOUS_USERNAME_BASE;
		}

		final String fusername = username;

		Resources.callsignAuditExecutor.execute(() -> {

			try {

				if (uid != null && uid.trim().length() > 0 &&
						callsign != null && callsign.trim().length() > 0 && eventType != null) {

					String ep_sql = new String("insert into client_endpoint (callsign, uid, username) " +
							"select ?, ?, ? where not exists " +
							"(select * from client_endpoint ce where ce.callsign = ? and ce.uid = ? and username = ?)");

					String epe_sql = new String("insert into client_endpoint_event (client_endpoint_id, connection_event_type_id, created_ts, groups) " +
							"select ce.id, cet.id, current_timestamp, (?)::bit(" + RemoteUtil.GROUPS_BIT_VECTOR_LEN + ") " +
							"from client_endpoint ce join connection_event_type cet on cet.event_name = ? " +
							"where ce.callsign = ? and ce.uid = ? and username = ?");

					if (log.isDebugEnabled()) {
						log.debug("Insert client endpoint callsign: " + callsign + " uid: " + uid);
					}

					try (Connection conn = dataSource.getConnection()) {

						try (PreparedStatement ps_ep = conn.prepareStatement(ep_sql)) {
							ps_ep.setString(1, callsign);
							ps_ep.setString(2, uid);
							ps_ep.setString(3, fusername);
							ps_ep.setString(4, callsign);
							ps_ep.setString(5, uid);
							ps_ep.setString(6, fusername);
							ps_ep.executeUpdate();

							// Insert a client endpoint row
							try (PreparedStatement ps_epe = conn.prepareStatement(epe_sql)) {
								ps_epe.setString(1, groupVector);
								ps_epe.setString(2, eventType.value());
								ps_epe.setString(3, callsign);
								ps_epe.setString(4, uid);
								ps_epe.setString(5, fusername);
								ps_epe.executeUpdate();
							}
						}
					} catch (Exception e) {
						if (log.isDebugEnabled()) {
							log.debug("Problem inserting client endpoint data", e);
						}
					}
				}

			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("exception saving client connection event to database", e);
				}
			}
		});
	}

	/**
	 * Insert Disconnect events for any client endpoints that may have not been properly
	 * disconnected due to a sudden CORE broker shutdown. Called from MartiMain during startup.
	 */

	//    @CacheEvict(value = Constants.CONTACTS_CACHE, allEntries = true)
	public void closeOpenCallsignAudits() {

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

    	if (!config.getRepository().isEnable()) {
			return;
		}

		if (!config.getRepository().isEnableCallsignAudit()) {
			return;
		}

		Resources.callsignAuditExecutor.execute(() -> {

			//Insert a Disconnect event for any Connect events that don't have at least one Disconnect following it in time
			String sql = "insert into client_endpoint_event (id, client_endpoint_id, connection_event_type_id, created_ts) " +
					"select nextval('client_endpoint_event_id_seq'::regclass), ce1.id, cet1.id, current_timestamp " +
					"from client_endpoint ce1 join connection_event_type cet1 on cet1.event_name = 'Disconnected' " +
					"	join (	select ce.id, max(cee.created_ts) created_ts " +
					"			from client_endpoint ce join client_endpoint_event cee on ce.id = cee.client_endpoint_id " +
					"				join connection_event_type cet on cee.connection_event_type_id = cet.id " +
					"			where cet.event_name = 'Connected' and cee.created_ts <= current_timestamp " +
					"			group by ce.id) tC on ce1.id = tC.id " +
					"	left join (	select ce.id, cee.created_ts " +
					"				from client_endpoint ce join client_endpoint_event cee on ce.id = cee.client_endpoint_id " +
					"					join connection_event_type cet on cee.connection_event_type_id = cet.id " +
					"				where cet.event_name = 'Disconnected') tD on ce1.id = tD.id and tD.created_ts >= tC.created_ts " +
					"where tD.id is null";


			//Alternate query (cost index was roughly the same, but the above query may perform better, though it is a little harder to maintain)
			/*String sql = "insert into client_endpoint_event (id, client_endpoint_id, connection_event_type_id, created_ts) " +
					 "select nextval('client_endpoint_event_id_seq'::regclass), ce.id, cet.id, current_timestamp " +
					 "from client_endpoint ce join connection_event_type cet on cet.event_name = 'Disconnected' " +
					 "where exists (select * " +
					 "		from client_endpoint_event cee2 join connection_event_type cet2 on cee2.connection_event_type_id = cet2.id " +
					 "		where cee2.client_endpoint_id = ce.id and cet2.event_name = 'Connected' and cee2.created_ts <= current_timestamp " +
					 "			and not exists (select * " +
					 "					from client_endpoint_event cee3 join connection_event_type cet3 on cee3.connection_event_type_id = cet3.id " +
					 "					where cee3.client_endpoint_id = cee2.client_endpoint_id and cet3.event_name = 'Disconnected' and cee3.created_ts >= cee2.created_ts)) ";
			 */

			try (Connection conn = dataSource.getConnection()) {

				try (PreparedStatement ps = conn.prepareStatement(sql)) {
					int numberRows = ps.executeUpdate();
					if (log.isDebugEnabled()) {
						log.debug(numberRows + " row(s) inserted in Repository.closeOpenCallsignAudits");
					}
				}
			} catch (Exception e) {
				log.error("Exception closing open callsign audits during startup. Logging and continuing: ", e);
			}
		});
	}

	// seed data structures with {missionName, contentUid} pairs from db at startup
	public void initializeMissionData() {

		try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("select m.name missionName, mu.uid contentUid "
				+ "from mission_uid mu "
				+ "inner join mission m on m.id = mu.mission_id"); ResultSet rs = ps.executeQuery();) {

			while (rs.next()) {
				String missionName = rs.getString(1);
				String contentUid = rs.getString(2);

				subscriptionManager.putMissionContentUid(missionName, contentUid);
			}

		} catch (Exception e) {
			log.error("Exception initializing mission data", e);
		}
	}

	public String getMissionTool(String missionName) {
		if (Strings.isNullOrEmpty(missionName)) {
			throw new IllegalArgumentException("empty mission name");
		}

		String tool = null;

		try (Connection connection = getConnection(); PreparedStatement ps = connection.prepareStatement("select tool from mission where name = ?")) {

			ps.setString(1, missionName);

			try (ResultSet rs = ps.executeQuery()) {

				if (rs.next()) {
					tool = rs.getString(1);
				}
			}

		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Exception in getMissionTool! " + e.getMessage(), e);
			}
		}

		return tool;
	}

	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public MissionMetadata getMissionMetadata(String missionName) {
		if (Strings.isNullOrEmpty(missionName)) {
			throw new IllegalArgumentException("empty mission name");
		}

		MissionMetadata mm = new MissionMetadata();

		try (Connection connection = getConnection(); PreparedStatement ps = connection.prepareStatement("select creatoruid, chatroom, tool, description from mission where name = ?")) {
			ps.setString(1, missionName);
			try (ResultSet rs = ps.executeQuery()) {

				if (rs.next()) {
					mm.setName(missionName);
					mm.setCreatorUid(rs.getString(1));
					mm.setChatRoom(rs.getString(2));
					mm.setTool(rs.getString(3));
					mm.setDescription(rs.getString(4));
				} else {
					throw new TakException("mission " + missionName + " does not exist");
				}
			}

		} catch (Exception e) {
			throw new TakException("Exception getting mission metadata from database for mission " + missionName, e);
		}

		if (log.isDebugEnabled()) {
			log.debug("retrieved " + mm + " for mission name " + missionName);
		}

		return mm;
	}

	public byte[] getContentByHash(String hash) throws SQLException, NamingException {
		byte[] result = null;
		try (Connection conn = getConnection(); PreparedStatement query = conn.prepareStatement("SELECT data FROM resource WHERE hash = ? ")) {

			query.setString(1, hash);

			if (log.isDebugEnabled()) {
				log.debug("Executing SQL: " + query);
			}

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					result = queryResults.getBytes(1);
				}
			}
		}
		return result;
	}

	public QueueMetric getQueueMetrics() {
		return inputQueue.getQueueMetrics();
	}

	public void testDatabaseConnection() throws Exception {
		try (Connection connection = getConnection(); PreparedStatement sql = connection.prepareStatement("select 1")) {
			sql.execute();
		} catch (Exception e) {
			throw e;
		}
	}

	public int getMaxConnections() {
		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

	    if (!config.getRepository().isEnable()) {
	        log.info("the repository is not enabled in CoreConfig.xml");
	        return 1;
	    }
	    int maxConnections = 1;
	    Connection conn = null;
	    try {
	        conn = getConnection();
	        ResultSet res = conn.createStatement().executeQuery("show max_connections");
	        res.next();
	        maxConnections = res.getInt(1);
	        conn.close();
	    } catch (Exception e) {
	    } finally {
	        try { conn.close(); } catch (Exception e) {}
	    }
	    return maxConnections;
	}

	public void reinitializeConnectionPool() {
		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

	    if (!config.getRepository().isEnable()) {
	        log.info("the repository is not enabled in CoreConfig.xml");
	        return;
	    }
	    log.info("reinitialize the connection pool");
	    Repository repository = config.getRepository();
        int numDbConnections;
        if (repository.isConnectionPoolAutoSize()) {
            numDbConnections = 200 + (int) Math.min(845, (Runtime.getRuntime().availableProcessors() - 4) * 9.2);
        } else {
            numDbConnections = repository.getNumDbConnections();
        }

        int maxConnectionsAllowed = Math.max(1, (getMaxConnections() / 2) - 1);

        int newConnectionPoolSize = Math.min(maxConnectionsAllowed, numDbConnections);

        dataSource.setMaximumPoolSize(newConnectionPoolSize);
        dataSource.getHikariPoolMXBean().softEvictConnections();
        dataSource.getHikariPoolMXBean().suspendPool();
        dataSource.getHikariPoolMXBean().resumePool();

        log.info("new datasource pool size: " + dataSource.getMaximumPoolSize());
	}

	public String getServerVersion() {
	    if (!CoreConfigFacade.getInstance().getRemoteConfiguration().getRepository().isEnable()) {
	        log.info("the repository is not enabled in CoreConfig.xml");
	        return "";
	    }
	    String serverVersion = "";
	    Connection conn = null;
	    try {
	        conn = getConnection();
	        ResultSet res = conn.createStatement().executeQuery("show server_version");
	        res.next();
	        serverVersion = res.getString(1);
	        conn.close();
	    } catch (Exception e) {
	    } finally {
	        try { conn.close(); } catch (Exception e) {}
	    }
	    return serverVersion;
	}
}
