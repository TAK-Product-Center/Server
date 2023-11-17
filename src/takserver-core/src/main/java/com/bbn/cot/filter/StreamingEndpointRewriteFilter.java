

package com.bbn.cot.filter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import javax.naming.ldap.LdapName;

import org.dom4j.Element;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.atakmap.Tak.ROL;
import tak.server.federation.DistributedFederationManager;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.sync.MissionMetadata;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.model.MissionPermission;
import com.bbn.marti.sync.model.MissionSubscription;
import com.bbn.marti.sync.repository.MissionSubscriptionRepository;
import com.bbn.marti.sync.service.MissionService;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;

public class StreamingEndpointRewriteFilter implements CotFilter {

	public static String DEFAULT_STREAMINGENDPOINT_KEY = "filter.streamingbroker";
	public static String EXPLICIT_PUBLISH_KEY = "explicitBrokeringPub";
	public static String EXPLICIT_CALLSIGN_KEY = "explicitBrokeringCallsign";
	public static String EXPLICIT_UID_KEY = "explicitBrokeringUid";
	public static String EXPLICIT_FEED_UID_KEY = "explicitFeedBrokeringUid";
	public static String EXPLICIT_MISSION_KEY = "explicitBrokeringMission";

	public static String UID_ATTR = "uid";
	public static String CALLSIGN_ATTR = "callsign";
	public static String PUBLISH_ATTR = "publish";
	public static String MISSION_ATTR = "mission";

  	public static String DEST_XPATH = String.format("/event/detail/marti/dest[@%s or @%s or @%s or @%s]", CALLSIGN_ATTR, PUBLISH_ATTR, UID_ATTR, MISSION_ATTR);

  	private static final Logger logger = LoggerFactory.getLogger(StreamingEndpointRewriteFilter.class);

  	@Autowired
  	private DistributedConfiguration config;

  	@Autowired
  	private SubscriptionManager subscriptionManager;

  	@Autowired
  	private SubscriptionStore subscriptionStore;

  	@Autowired
  	private RepositoryService repositoryService;

  	@Autowired
   	private DistributedFederationManager federationManager;

   	@Autowired
   	ApplicationContext context;

	@Autowired
	private MissionSubscriptionRepository missionSubscriptionRepository;

	private MissionService missionService;
	
	private final Logger changeLogger = LoggerFactory.getLogger(Constants.CHANGE_LOGGER);

	public StreamingEndpointRewriteFilter(MissionService missionService) {
		this.missionService = missionService;
	}


	@SuppressWarnings("unchecked")
    @Override
	public CotEventContainer filter(final CotEventContainer cot) {

		if (logger.isTraceEnabled()) {
			logger.trace("StreamingEndpointFilter start");
		}

		if (config.getFilter().getStreamingbroker().isEnable()) {
			List<Node> destList = cot.getDocument().selectNodes(DEST_XPATH);

			if (destList.size() > 0) {
				List<String> publishList = new LinkedList<String>();
				List<String> callsignList = new LinkedList<String>();
				Set<String> uids = new HashSet<>();
				Set<String> missionNames = new HashSet<>();

				String clientUid = "";
				try {
					if (cot.getContextValue(Constants.CLIENT_UID_KEY) != null) {
						clientUid = (String)cot.getContextValue(Constants.CLIENT_UID_KEY);
					}

					Subscription sub = null;
					if (clientUid == null || clientUid.length() == 0 ) {
						Object handler = cot.getContextValue(Constants.SOURCE_TRANSPORT_KEY);
						if (handler != null) {
							sub = subscriptionStore.getByHandler((ChannelHandler) handler);
							clientUid = sub.clientUid;
						}
					}

					if (clientUid == null || clientUid.length() == 0 ) {
	                    User user = (User) cot.getContextValue(Constants.USER_KEY);
	                    if (user != null) {
							try {
								sub = subscriptionManager.getSubscription(user);
							} catch (IllegalStateException e) {
								logger.trace("filtering cot for non-socket web connection");
							}

							if (sub != null && sub.clientUid != null && sub.clientUid.length() > 0) {
								clientUid = sub.clientUid;
							} else if (user != null && user.getCert() != null
									&& user.getCert().getSubjectX500Principal() != null) {
								clientUid = user.getCert().getSubjectX500Principal().getName();
							}
						}
				    }

				} catch (Exception e) {
				    logger.error("exception getting client uid " + e.getMessage(), e);
				}

				for (Node destElem : destList) {
					Element detached = (Element) destElem.detach();

					if(detached.attribute(CALLSIGN_ATTR) != null) {
						callsignList.add(detached.attributeValue(CALLSIGN_ATTR));
					} else if(detached.attribute(PUBLISH_ATTR) != null) {
						publishList.add(detached.attributeValue(PUBLISH_ATTR));
					} else if(detached.attribute(UID_ATTR) != null) {
						uids.add(detached.attributeValue(UID_ATTR));
					} else if(detached.attribute(MISSION_ATTR) != null) {
					    missionNames.add(detached.attributeValue(MISSION_ATTR));
				        logger.debug("mission destination specified in message: " + detached.attributeValue(MISSION_ATTR));
                    }
				}

				if (publishList.size() > 0) {
                    cot.setContextValue(EXPLICIT_PUBLISH_KEY, publishList);
                }

				if (callsignList.size() > 0 && !callsignList.contains("All Streaming")) {
                    cot.setContextValue(EXPLICIT_CALLSIGN_KEY, callsignList);
                }

				if (!missionNames.isEmpty()) {
				    cot.setContextValue(EXPLICIT_MISSION_KEY, missionNames);
				    
				    if (changeLogger.isDebugEnabled()) {
				    	changeLogger.debug("explicit missonNames in message: " + missionNames);
				    }
				}

				// add the client uid for each mission subscriber to the explicit uid list
                try {

					String groupVector = RemoteUtil.getInstance().bitVectorToString(
							RemoteUtil.getInstance().getBitVectorForGroups(
									(NavigableSet<Group>)cot.getContext(Constants.GROUPS_KEY)));

                    for (final String missionName : missionNames) {

						MissionSubscription missionSubscription = null;
						User user = (User) cot.getContextValue(Constants.USER_KEY);
						if (user != null) {
							missionSubscription = missionSubscriptionRepository
									.findByMissionNameAndClientUidAndUsernameNoMission(
									missionName, clientUid, user.getName());

							if (missionSubscription == null
									&& user.getCert() != null && user.getCert().getSubjectX500Principal() != null) {
								// lookup the mission subscription based on CN, needed when input auth=ldap or auth=file
								String cn = new LdapName(user.getCert().getSubjectX500Principal().getName())
										.getRdns().stream().filter(i -> i.getType().equalsIgnoreCase("CN"))
											.findFirst().get().getValue().toString();
								missionSubscription = missionSubscriptionRepository
										.findByMissionNameAndClientUidAndUsernameNoMission(
										missionName, clientUid, cn);
							}

						} else {
							missionSubscription = missionSubscriptionRepository.findByMissionNameAndClientUidNoMission(
									missionName, clientUid);
						}
					
						if (missionSubscription == null) {
							logger.error("unable to find mission subscription for client " + missionName + ", " + clientUid);
							continue;
						} else {
							if (changeLogger.isDebugEnabled()) {
								changeLogger.debug("mission sub for explcit mission sender to " + missionName + ": " + missionSubscription);
							}
						}

						if (missionSubscription.getRole() != null && !missionSubscription.getRole().
								hasPermission(MissionPermission.Permission.MISSION_WRITE)) {
							logger.error("Illegal attempt to adding streaming content to mission!");
							continue;
						}

						for (String missionClientUid : subscriptionManager.getMissionSubscriptions(missionName, true)) {
							// don't send the event back to the submitter
							if (clientUid != null && clientUid.compareTo(missionClientUid) == 0) {
								continue;
							}

							uids.add(missionClientUid);
						}
						
						if (changeLogger.isDebugEnabled()) {
							changeLogger.debug("mission client uid for mission sub count: " + uids.size());
						}

						// final copy of variable to use in inner class
						final String finClientUid = clientUid;
						final String finGroupVector = groupVector;

						// TODO: refactor this for performance checks, separation of concerns
						CotEventContainer copyCot = cot.copy();
						
						final String fclientUid = clientUid;
						
						if (changeLogger.isDebugEnabled()) {
							changeLogger.debug("sending change to executor for clientUid: " + fclientUid + " and add uid: " + copyCot.getUid());
						}
												
						Resources.missionContentProcessor.execute(() -> {
							// Don't add content if this message came over NATS. It was already added on the origin node
							if (!cot.hasContextKey(Constants.NATS_MESSAGE_KEY)) {
								try {
									MissionContent missionContent = new MissionContent();
									missionContent.getUids().add(copyCot.getUid());
									missionService.addMissionContent(missionName, missionContent, finClientUid, finGroupVector);
								} catch (Exception e) {
									logger.error("exception adding content uid to mission " + e.getMessage(), e);
								}
							}
							
							if (config.getRemoteConfiguration().getFederation().isEnableFederation()
									&&	config.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
								// federate this mission update (subject to group filtering)
								try {

									NavigableSet<Group> groups = null;

									String uid = cot.getUid();

									if (Strings.isNullOrEmpty(uid)) {
										throw new IllegalArgumentException("empty uid in cot for mission content add");
									}

									MissionContent content = new MissionContent();
									content.getUids().add(uid);

									if (cot.getContextValue(Constants.GROUPS_KEY) != null) {
										try {
											groups = (NavigableSet<Group>) cot.getContextValue(Constants.GROUPS_KEY);

											if (logger.isDebugEnabled()) {
												logger.debug("groups for message: " + cot + ": " + groups);
											}

										} catch (ClassCastException e) {
											logger.debug("Not trying to get group info for message with invalid type of groups object: " + cot);
										}
									} else {
										if (logger.isDebugEnabled()) {
											logger.debug("Groups context key not set for message: " + cot);
										}
									}

									if (groups != null) {

										MissionMetadata mission = repositoryService.getMissionMetadata(missionName);

										if (mission == null) {
											logger.debug("nothing to federate for non-existent mission " + missionName);
											return;
										}

										
										if (DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions()) {
											if ("public".equals(mission.getTool())) {
												// allow public. no action needed as of now
											} else if (config.getNetwork().getMissionCopTool().equals(mission.getTool())) {
												if (!DistributedConfiguration.getInstance().getRemoteConfiguration().getVbm().isEnabled()) {
													logger.debug("not federating vbm mission action for mission " + missionName + " since vbm is disabled");
													return;
												}
											} else {
												logger.debug("not federating non-public mission action for mission " + missionName);
												return;
											}
										}

										ROL rol = RemoteUtil.getInstance().getROLforMissionChange(content, missionName, fclientUid, mission.getCreatorUid(), mission.getChatRoom(), mission.getTool(), mission.getDescription());

										if (logger.isDebugEnabled()) {
											logger.debug("rol to federate for mission change " + rol + " to groups " + groups);
										}

										federationManager.submitMissionFederateROL(rol, groups, missionName);
									} else {
										logger.warn("unable to federate mission uid add - cot message specified no groups");
									}

								} catch (Exception e) {
									logger.debug("exception adding content uid to mission " + e.getMessage(), e);
								}
							}
						});
                    }
                } catch (Exception e) {
                	logger.debug("exception getting mission subscriber uids: " + e.getMessage(), e);
                }

                logger.debug("explicit uids for message " + cot.getUid() + " " + uids);
                logger.debug("explicit callsigns for message " + cot.getUid() + " " + callsignList);

                if (uids.size() > 0) {
                	cot.setContextValue(EXPLICIT_UID_KEY, new ArrayList<String>(uids));
                }
			}
		}

        // remove marti element completely
		Element martiElem = (Element) cot.getDocument().selectSingleNode(Constants.MARTI_XPATH);
		if (martiElem != null) {
			martiElem.detach();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("StreamingEndpointFilter complete");
		}

		return cot;
	}


}
