package tak.server.cache;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.eviction.fifo.FifoEvictionPolicyFactory;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bbn.marti.dao.kml.JDBCCachingKMLDao;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.remote.util.SecureXmlParser;
import com.bbn.marti.service.kml.KmlIconStrategyJaxb;
import com.bbn.marti.sync.model.Location;
import com.bbn.marti.sync.model.UidDetails;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import tak.server.Constants;
import tak.server.cot.CotElement;
import tak.server.cot.CotEventContainer;

@SuppressWarnings("deprecation")
public class CoTCacheHelper {

	@Autowired
	private Ignite ignite;

	@Autowired
	private RemoteUtil remoteUtil;

	@Autowired
	private DataSource dataSource;

	private boolean messagingProfileActive = false;

	private int maxCacheSize;

	private static final Logger logger = LoggerFactory.getLogger(CoTCacheHelper.class);

	@PostConstruct
	private void postConstruct() {
		maxCacheSize = CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue().getCotCacheMaxSize();
	}

	private IgniteCache<Object, Object> getCoTCache() {

		CacheConfiguration<Object, Object> cacheConfig = new CacheConfiguration<>(Constants.LATEST_COT_CACHE);
		cacheConfig.setAtomicityMode(CacheAtomicityMode.ATOMIC);

		IgniteCache<Object, Object> cache = null;

		String profilesActive = System.getProperty("spring.profiles.active");

		if (profilesActive != null) {
			// Due to initialization order, can't use spring environment and therefore
			// ProfileTracker class yet, so look at the expected system property.

			if (profilesActive.toLowerCase().contains(Constants.MESSAGING_PROFILE_NAME)) {
				messagingProfileActive = true;
			}
		}

		CoreConfig coreConfig = CoreConfigFacade.getInstance();
		// use on-heap memory
		boolean onHeapEnabled = coreConfig.getRemoteConfiguration().getBuffer().getQueue().isOnHeapEnabled();
		cacheConfig.setOnheapCacheEnabled(onHeapEnabled);
		if (onHeapEnabled) {

			int cotCacheMaxSize = coreConfig.getRemoteConfiguration().getBuffer().getQueue().getCotCacheMaxSize();
			int cotCacheBatchSize = coreConfig.getRemoteConfiguration().getBuffer().getQueue().getCotCacheBatchSize();
			int cotCacheMaxMemorySize = coreConfig.getRemoteConfiguration().getBuffer().getQueue().getCotCacheMaxMemorySize();

			if (cotCacheBatchSize == -1 || cotCacheMaxMemorySize == -1) {
				cacheConfig.setEvictionPolicyFactory(new FifoEvictionPolicyFactory<>(cotCacheMaxSize));
			} else {
				cacheConfig.setEvictionPolicyFactory(new FifoEvictionPolicyFactory<>(
						cotCacheMaxSize, cotCacheBatchSize, cotCacheMaxMemorySize));
			}

		}

		if (!messagingProfileActive) {

			NearCacheConfiguration<Object, Object> nearCfg =  new NearCacheConfiguration<>();
			nearCfg.setNearEvictionPolicyFactory(new FifoEvictionPolicyFactory<>((int) coreConfig.getRemoteConfiguration().getBuffer().getQueue().getCotCacheMaxSize()));

			cache = ignite.getOrCreateCache(cacheConfig, nearCfg);

		} else {
			cache = ignite.getOrCreateCache(cacheConfig);
		}

		return cache;
	}

	public String getKeyGetCachedClientEndpointData(boolean connected, boolean recent) {
		return "getCachedClientEndpointData_" + connected + "_" + recent;
	}

	public void cacheCoT(CotEventContainer cot, String groupsBitVectorString) {

		if (isDisabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("CoT cache disabled");
			}
			return;
		}

		if (RemoteUtil.getInstance().getBitStringNoGroups().equals(groupsBitVectorString)) {
			if (logger.isDebugEnabled()) {
				logger.debug("ignoring request to cache CoT event with no group access");
			}
			return;
		}

		try {

			CotElement cotElement = cot.asCotElement();

			UidDetails hydratedUidDetails = generateUidDetails(new UidDetails(), cotElement);

			CotCacheWrapper wrapper = new CotCacheWrapper();

			wrapper.setUid(cotElement.uid);
			wrapper.setGroupsBitVectorString(groupsBitVectorString);
			wrapper.setCotElement(cotElement);
			wrapper.setUidDetails(hydratedUidDetails);

			getCoTCache().putAsync(wrapper.getUid(), wrapper);

			if (logger.isDebugEnabled()) {
				logger.debug("cached CoT: " + wrapper);
			}

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception caching CoT", e);
			}
		}

	}

	private UidDetails generateUidDetails(@NotNull UidDetails uidDetails, @NotNull CotElement cotElement) {

		if (uidDetails == null) {
			throw new IllegalArgumentException("null uidDetails");
		}


		if (cotElement == null) {
			throw new IllegalArgumentException("null cotElement");
		}

		uidDetails.type = cotElement.cottype;

		try {
			if (cotElement.lat != 0 && cotElement.lon != 0) {
				uidDetails.location = new Location(cotElement.lat, cotElement.lon);
			}

			String medevacTitle = null;

			if (cotElement.detailtext != null) {
				Document doc = SecureXmlParser.makeDocument(cotElement.detailtext);

				NodeList nodeList = doc.getElementsByTagName("detail");
				if (nodeList == null || nodeList.getLength() != 1) {
					return null;
				}

				nodeList = nodeList.item(0).getChildNodes();



				for (int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					if (node.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}

					String name = node.getNodeName();
					NamedNodeMap namedNodeMap = node.getAttributes();

					Node colorAttr;
					Node titleAttr;
					Node attachmentAttr;

					if (name.compareTo("color") == 0) {
						if ((colorAttr = namedNodeMap.getNamedItem("argb")) != null) {
							uidDetails.color = colorAttr.getNodeValue();
						} else if ((colorAttr = namedNodeMap.getNamedItem("value")) != null) {
							uidDetails.color = colorAttr.getNodeValue();
						}
					} else if (name.compareTo("strokeColor") == 0) {
						if ((colorAttr = namedNodeMap.getNamedItem("value")) != null) {
							uidDetails.color = colorAttr.getNodeValue();
						}
					} else if (name.compareTo("link_attr") == 0) {
						if ((colorAttr = namedNodeMap.getNamedItem("color")) != null) {
							uidDetails.color = colorAttr.getNodeValue();
						}
					} else if (name.compareTo("bullseye") == 0 && uidDetails.type.equals("u-r-b-bullseye")) {
						if ((colorAttr = namedNodeMap.getNamedItem("edgeToCenter")) != null) {
							uidDetails.color = colorAttr.getNodeValue().equals("true") ? "-65536" : "-16711936";
						}
					}  else if (name.compareTo("attachment_list") == 0) {
						if ((attachmentAttr = namedNodeMap.getNamedItem("hashes")) != null) {
							String attachmentJson = attachmentAttr.getNodeValue();
							// trim off [ ]
							attachmentJson = attachmentJson.substring(1, attachmentJson.length() - 1);
							// remove all quotes (they get automatically decoded from &quot; to " by selectSingleNode)
							attachmentJson = attachmentJson.replace("\"", "");
							// we're left with a comma separated list to split
							uidDetails.attachments = Arrays.asList(attachmentJson.split(","));
						}
					} else if (name.compareTo("title") == 0) {
						if ((titleAttr = namedNodeMap.getNamedItem("title")) != null) {
							uidDetails.title = titleAttr.getNodeValue();
						}
					} else if (name.compareTo("_medevac_") == 0) {
						if ((titleAttr = namedNodeMap.getNamedItem("title")) != null) {
							medevacTitle = titleAttr.getNodeValue();
						}
					} else if (name.compareTo("model") == 0 && uidDetails.type.equals("u-d-v-m")) {
						Node modelAttr;

						if ((modelAttr = namedNodeMap.getNamedItem("name")) != null) {
							uidDetails.name = modelAttr.getNodeValue();
						}

						if ((modelAttr = namedNodeMap.getNamedItem("category")) != null) {
							uidDetails.category = modelAttr.getNodeValue();
						}
					}
				}

				KmlIconStrategyJaxb.ParseUserIcon(doc, cotElement);
				JDBCCachingKMLDao.ParseDetailText(cotElement);
			}

			// we need to unescape the callsign here to prevent double encoding of &'s by the serialization layer
			uidDetails.callsign = StringEscapeUtils.unescapeXml(cotElement.callsign);

			if (cotElement.iconSetPath != CotElement.errorMessage) {
				uidDetails.iconsetPath = cotElement.iconSetPath;
			}

			if (uidDetails.callsign == null && uidDetails.title != null) {
				uidDetails.callsign = uidDetails.title;
			}

			if (!Strings.isNullOrEmpty(medevacTitle)) {
				uidDetails.callsign = medevacTitle;
			}

		} catch (Exception ex) {
			logger.error("Exception parsing cot detail!", ex);
		}

		return uidDetails;
	}

	public CotCacheWrapper getLatestCotWrapperForUid(String uid, String groupVector) {

		if (!isDisabled()) {

			CotCacheWrapper cacheWrapper = (CotCacheWrapper) getCoTCache().get(uid);

			if (cacheWrapper != null && cacheWrapper.getCotElement() != null) {

				if (logger.isDebugEnabled()) {
					logger.debug("CoT cache hit: " + cacheWrapper);
				}

				// validate group visibility for this CoT

				if (remoteUtil.isGroupVectorAllowed(groupVector, cacheWrapper.getGroupsBitVectorString())) {
					return cacheWrapper;
				}

				throw new TakException("not allowed");
			}

			if (logger.isDebugEnabled()) {
				logger.debug("CoT cache miss for uid: " + uid);
			}
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("CoT cache disabled");
			}
		}

		CotElement cotElement = queryLatestCotElementForUid(uid, groupVector);

		if (cotElement == null) {
			throw new NotFoundException("CoT for uid " + uid + " not found in database ");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("groups from CoT query result: " + cotElement.groupString + " groups parameter: " + groupVector);
		}

		UidDetails uidDetails = generateUidDetails(new UidDetails(), cotElement);

		CotCacheWrapper wrapper = new CotCacheWrapper();

		wrapper.setGroupsBitVectorString(cotElement.groupString);
		wrapper.setUid(uid);
		wrapper.setCotElement(cotElement);
		wrapper.setUidDetails(uidDetails);

		if (!isDisabled()) {
			getCoTCache().put(uid, wrapper);
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("CoT cache disabled");
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("cached CoT due to miss: " + wrapper);
		}

		// validate group visibility for this CoT

		if (remoteUtil.isGroupVectorAllowed(groupVector, wrapper.getGroupsBitVectorString())) {
			return wrapper;
		}

		throw new TakException("not allowed");
	}

	public Collection<CotCacheWrapper> getLatestCotWrappersForUids(Set<String> uids, String groupVector, boolean addDetails) {

		Collection<CotCacheWrapper> results = new HashSet<>();

		if (isDisabled()) {

			if (logger.isTraceEnabled()) {
				logger.trace("CoT cache disabled");
			}

			Map<Object, Object> cachePutMap = new ConcurrentHashMap<>();

			Collection<CotElement> cot = queryLatestCotElementsForUids(uids, groupVector);
			Iterator it = cot.iterator();
			while (it.hasNext()) {
				CotElement cotElement = (CotElement)it.next();

				//
				// only do the groupVector check when not adding details. When we're adding details this method is
				// is called with uids that are pulled from a  mission. We've already performed the groupVector
				// check when accessing the mission so no need to do it again here
				//
				if (!addDetails) {
					if (!remoteUtil.isGroupVectorAllowed(groupVector, cotElement.groupString)) {
						logger.error("!isGroupVectorAllowed for " + cotElement.uid);
						continue;
					}
				}

				try {
					UidDetails uidDetails = addDetails ? generateUidDetails(new UidDetails(), cotElement) : null;

					CotCacheWrapper wrapper = new CotCacheWrapper();

					wrapper.setGroupsBitVectorString(cotElement.groupString);
					wrapper.setUid(cotElement.uid);
					wrapper.setCotElement(cotElement);
					wrapper.setUidDetails(uidDetails);

					cachePutMap.put(cotElement.uid, wrapper);

				} catch (Exception e) {
					logger.warn("exception generating details", e);
				}
			}

			for (Object wrapper : cachePutMap.values()) {
				results.add((CotCacheWrapper) wrapper);
			}

			return results;
		}

		Map<?, ?> cacheWrapperMap = getCoTCache().getAll(uids);

		Set<String> uidsNotCached = Sets.difference(uids, cacheWrapperMap.keySet());

		if (logger.isTraceEnabled()) {
			logger.trace("uids not cached: " + uidsNotCached);
		}

		Map<Object, Object> cachePutMap = new ConcurrentHashMap<>();

		for (CotElement cotElement : queryLatestCotElementsForUids(uidsNotCached, groupVector)) {
			UidDetails uidDetails = generateUidDetails(new UidDetails(), cotElement);

			CotCacheWrapper wrapper = new CotCacheWrapper();

			wrapper.setGroupsBitVectorString(cotElement.groupString);
			wrapper.setUid(cotElement.uid);
			wrapper.setCotElement(cotElement);
			wrapper.setUidDetails(uidDetails);

			cachePutMap.put(cotElement.uid, wrapper);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("caching " + cachePutMap.size() + " wrappers");
		}

		getCoTCache().putAll(cachePutMap);

		for (Object val : cacheWrapperMap.values()) {

			if (addDetails || remoteUtil.isGroupVectorAllowed(groupVector, ((CotCacheWrapper) val).getGroupsBitVectorString())) {
				results.add((CotCacheWrapper) val);
			}
		}

		for (Object val : cachePutMap.values()) {
			if (addDetails || remoteUtil.isGroupVectorAllowed(groupVector, ((CotCacheWrapper) val).getGroupsBitVectorString())) {
				results.add((CotCacheWrapper) val);
			}
		}

		return results;

	}

	private CotElement queryLatestCotElementForUid(String uid, String groupVector) {

		try {
			String sql = "select "
					+ "uid, ST_X(event_pt), ST_Y(event_pt), "
					+ " point_hae, cot_type, servertime, point_le, detail, id, how, stale, point_ce, groups from "
					+ "(select * from cot_router where uid = ? order by id desc) as cot "
					+ " where " + remoteUtil.getGroupClause()
					+ " order by id desc limit 1 ";

			return new JdbcTemplate(dataSource).query(sql,
					new Object[] {
							uid,
							groupVector},
					new ResultSetExtractor<CotElement>() {
						@Override
						public CotElement extractData(ResultSet results) throws SQLException {
							if (results == null) {
								throw new IllegalStateException("null result set");
							}

							if (results.next()) {
								CotElement cotElement = new CotElement();
								cotElement.uid = results.getString(1);
								cotElement.lon = results.getDouble(2);
								cotElement.lat = results.getDouble(3);
								cotElement.hae = results.getString(4);
								cotElement.cottype = results.getString(5);
								cotElement.servertime = results.getTimestamp(6);
								cotElement.le = results.getDouble(7);
								cotElement.detailtext = results.getString(8);
								cotElement.cotId = results.getLong(9);
								cotElement.how = results.getString(10);
								cotElement.staletime = results.getTimestamp(11);
								cotElement.ce = results.getDouble(12);
								cotElement.groupString = results.getString(13);

								return cotElement;
							}

							return null;
						}
					});

		} catch (Exception e) {
			logger.error("sql exception in getLatestCotForUids! " + e.getMessage());
			return null;
		}
	}

	private Collection<CotElement> queryLatestCotElementsForUids(Set<String> uids, String groupVector) {

		try (Connection connection = dataSource.getConnection()) {
			String sql = "select "
					+ "uid, ST_X(event_pt), ST_Y(event_pt), "
					+ " point_hae, cot_type, servertime, point_le, detail, id, how, stale, point_ce, groups "
					+ " from cot_router where id in ("
					+ " 	select max(id) from cot_router where uid = any (?) "
					+ " 	group by uid ) ";

			AuditLogUtil.auditLog(sql + "uids: " + uids + " groupVector: " + groupVector);

			Array uidArray = connection.createArrayOf("varchar", uids.toArray());

			return new JdbcTemplate(dataSource).query(sql,
					new Object[] {
							uidArray
					},
					new ResultSetExtractor<List<CotElement>>() {
						@Override
						public List<CotElement> extractData(ResultSet results) throws SQLException {
							if (results == null) {
								throw new IllegalStateException("null result set");
							}

							LinkedList<CotElement> cotElements = new LinkedList<>();
							while (results.next()) {
								CotElement cotElement = new CotElement();
								cotElement.uid = results.getString(1);
								cotElement.lon = results.getDouble(2);
								cotElement.lat = results.getDouble(3);
								cotElement.hae = results.getString(4);
								cotElement.cottype = results.getString(5);
								cotElement.servertime = results.getTimestamp(6);
								cotElement.le = results.getDouble(7);
								cotElement.detailtext = results.getString(8);
								cotElement.cotId = results.getLong(9);
								cotElement.how = results.getString(10);
								cotElement.staletime = results.getTimestamp(11);
								cotElement.ce = results.getDouble(12);
								cotElement.groupString = results.getString(13);

								cotElements.add(cotElement);
							}

							return cotElements;
						}
					});

		} catch (Exception e) {
			logger.error("exception in getLatestCotForUids ", e);
			return null;
		}
	}


	private boolean isDisabled() {
		return maxCacheSize < 1;
	}
}
