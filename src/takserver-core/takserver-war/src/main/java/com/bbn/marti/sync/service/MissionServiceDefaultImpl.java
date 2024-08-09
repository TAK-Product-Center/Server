package com.bbn.marti.sync.service;

import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.security.PrivateKey;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.dom4j.DocumentException;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.oxm.Marshaller;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bbn.marti.classification.service.ClassificationService;
import com.bbn.marti.config.GeospatialFilter;
import com.bbn.marti.dao.kml.JDBCCachingKMLDao;
import com.bbn.marti.email.EmailClient;
import com.bbn.marti.jwt.JwtUtils;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.maplayer.MapLayerService;
import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.DataFeedCotService;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.SubscriptionManagerLite.ChangeType;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.ForbiddenException;
import com.bbn.marti.remote.exception.MissionDeletedException;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionType;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.sync.MissionChangeType;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.remote.util.SecureXmlParser;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.bbn.marti.service.kml.KMLService;
import com.bbn.marti.service.kml.KmlIconStrategyJaxb;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.ExternalMissionData;
import com.bbn.marti.sync.model.Location;
import com.bbn.marti.sync.model.LogEntry;
import com.bbn.marti.sync.model.MinimalMission;
import com.bbn.marti.sync.model.MinimalMissionFeed;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.Mission.MissionAdd;
import com.bbn.marti.sync.model.Mission.MissionAddDetails;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.model.MissionChanges;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.sync.model.MissionInvitation;
import com.bbn.marti.sync.model.MissionLayer;
import com.bbn.marti.sync.model.MissionPermission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.model.MissionSubscription;
import com.bbn.marti.sync.model.Resource;
import com.bbn.marti.sync.model.ResourceUtils;
import com.bbn.marti.sync.model.UidDetails;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.sync.repository.ExternalMissionDataRepository;
import com.bbn.marti.sync.repository.LogEntryRepository;
import com.bbn.marti.sync.repository.MissionChangeRepository;
import com.bbn.marti.sync.repository.MissionFeedRepository;
import com.bbn.marti.sync.repository.MissionInvitationRepository;
import com.bbn.marti.sync.repository.MissionLayerRepository;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.repository.MissionRoleRepository;
import com.bbn.marti.sync.repository.MissionSubscriptionRepository;
import com.bbn.marti.sync.repository.ResourceRepository;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.GeomUtils;
import com.bbn.marti.util.TimeUtils;
import com.bbn.marti.util.missionpackage.ContentType;
import com.bbn.marti.util.missionpackage.MissionPackage;
import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.Metrics;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import tak.server.Constants;
import tak.server.cache.CoTCacheHelper;
import tak.server.cache.CotCacheWrapper;
import tak.server.cache.MissionCacheHelper;
import tak.server.cache.UnproxyHelper;
import tak.server.cache.resolvers.AllCopMissionCacheResolver;
import tak.server.cache.resolvers.AllMissionCacheResolver;
import tak.server.cache.resolvers.MissionCacheResolver;
import tak.server.cache.resolvers.MissionLayerCacheResolver;
import tak.server.cot.CotElement;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.feeds.DataFeedDTO;
import tak.server.filemanager.FileManagerService;
import tak.server.ignite.grid.SubscriptionManagerProxyHandler;

/*
 *
 * Mission API business logic layer default implementation
 *
 *
 */
@SuppressWarnings("deprecation")
public class MissionServiceDefaultImpl implements MissionService {

	private static final Logger logger = LoggerFactory.getLogger(MissionServiceDefaultImpl.class);

	private final DataSource dataSource;

	private final MissionRepository missionRepository;

	private final MissionChangeRepository missionChangeRepository;

	private final ResourceRepository resourceRepository;

	private final LogEntryRepository logEntryRepository;

	private final SubscriptionManagerLite subscriptionManager;

	private final SubscriptionManagerProxyHandler subscriptionManagerProxy;

	private final RemoteUtil remoteUtil;

	private final com.bbn.marti.sync.EnterpriseSyncService syncStore;

	private final JDBCCachingKMLDao kmlDao;

	private final KMLService kmlService;

	private final SubmissionInterface submission;

	private final ExternalMissionDataRepository externalMissionDataRepository;

	private final MissionInvitationRepository missionInvitationRepository;

	private final MissionSubscriptionRepository missionSubscriptionRepository;

	private final MissionFeedRepository missionFeedRepository;

	private final MapLayerService mapLayerService;

	private final DataFeedRepository dataFeedRepository;

	private final MissionLayerRepository missionLayerRepository;

	private final GroupManager groupManager;

	private final CommonUtil commonUtil;

	private final MissionRoleRepository missionRoleRepository;

	private final CoTCacheHelper cotCacheHelper;

	private final MissionCacheHelper missionCacheHelper;
	
	private final ClassificationService classificationService;

	private final FileManagerService fileManagerService;

	private final Logger changeLogger = LoggerFactory.getLogger(Constants.CHANGE_LOGGER);

	@Autowired
	private AsyncTaskExecutor asyncExecutor;

	private static MissionService missionService;

	@Autowired
	@Qualifier(Constants.TAKMESSAGE_MAPPER)
	private ObjectMapper mapper;

	@Autowired
	DataFeedCotService dataFeedCotService;

	private final ThreadLocal<CotParser> cotParser = new ThreadLocal<>();

	public MissionServiceDefaultImpl(
			DataSource dataSource,
			MissionRepository missionRepository,
			MissionChangeRepository missionChangeRepository,
			ResourceRepository resourceRepository,
			LogEntryRepository logEntryRepository,
			SubscriptionManagerLite subscriptionManager,
			SubscriptionManagerProxyHandler subscriptionManagerProxy,
			RemoteUtil remoteUtil,
			Marshaller marshaller,
			com.bbn.marti.sync.EnterpriseSyncService syncStore,
			JDBCCachingKMLDao kmlDao,
			KMLService kmlService,
			SubmissionInterface submission,
			ExternalMissionDataRepository externalMissionDataRepository,
			MissionInvitationRepository missionInvitationRepository,
			MissionSubscriptionRepository missionSubscriptionRepository,
			MissionFeedRepository missionFeedRepository,
			DataFeedRepository dataFeedRepository,
			MissionLayerRepository missionLayerRepository,
			MapLayerService mapLayerService,
			GroupManager groupManager,
			CacheManager cacheManager,
			CommonUtil commonUtil,
			MissionRoleRepository missionRoleRepository,
			CoTCacheHelper cotCacheHelper,
			MissionCacheHelper missionCacheHelper,
			ClassificationService classificationService,
			FileManagerService fileManagerService) {
		this.dataSource = dataSource;
		this.missionRepository = missionRepository;
		this.missionChangeRepository = missionChangeRepository;
		this.resourceRepository = resourceRepository;
		this.logEntryRepository = logEntryRepository;
		this.subscriptionManager = subscriptionManager;
		this.subscriptionManagerProxy = subscriptionManagerProxy;
		this.remoteUtil = remoteUtil;
		this.syncStore = syncStore;
		this.kmlDao = kmlDao;
		this.kmlService = kmlService;
		this.submission = submission;
		this.externalMissionDataRepository = externalMissionDataRepository;
		this.missionInvitationRepository = missionInvitationRepository;
		this.missionSubscriptionRepository = missionSubscriptionRepository;
		this.missionFeedRepository = missionFeedRepository;
		this.dataFeedRepository = dataFeedRepository;
		this.missionLayerRepository = missionLayerRepository;
		this.mapLayerService = mapLayerService;
		this.groupManager = groupManager;
		this.commonUtil = commonUtil;
		this.missionRoleRepository = missionRoleRepository;
		this.cotCacheHelper = cotCacheHelper;
		this.missionCacheHelper = missionCacheHelper;
		this.classificationService = classificationService;
		this.fileManagerService = fileManagerService;
	}

	private static final Logger cacheLogger = LoggerFactory.getLogger("missioncache");

	private MissionService getMissionService() {

		// return the cached missionService if we have one
		if (missionService != null) {
			return missionService;
		}

		synchronized (this) {

			if (missionService == null) {

				try {
					// cache off and return the missionService bean
					missionService = SpringContextBeanForApi.getSpringContext()
							.getBean(com.bbn.marti.sync.service.MissionService.class);
				} catch (Exception e) {
					// if we have any problems getting bean just return this
					logger.error("exception trying to get MissionService bean!", e);
					return this;
				}
			}
		}
		
		return missionService;
	}

	@Override
	public CotElement getLatestCotElement(String uid, String groupVector, Date end,
										  ResultSetExtractor<CotElement> resultSetExtractor) {

		String sql = "select " // query
				+ "uid, ST_X(event_pt), ST_Y(event_pt), "
				+ " point_hae, cot_type, servertime, point_le, detail, id, how, stale "
				+ ", point_ce "
				+ "from cot_router where uid=? and servertime <= ? "
				+ remoteUtil.getGroupAndClause()
				+ "order by servertime desc limit 1";

		AuditLogUtil.auditLog(sql + "uid: " + uid + " groupVector: " + groupVector);

		return new JdbcTemplate(dataSource).query(sql,
				new Object[]{uid, end, groupVector }, // parameters
				resultSetExtractor);
	}

	@Override
	public CotEventContainer getLatestCotEventContainerForUid(String uid, String groupVector) {

		try {
			CotElement missionUidCot = getLatestCotForUid(uid, groupVector);

			return new CotEventContainer(getCotParser().parse(missionUidCot.toCotXml()));
		} catch (DocumentException e) {
			throw new TakException(e);
		}
	}

	@Override
	public CotElement getLatestCotForUid(String uid, String groupVector, Date end) {

		if (end.equals(TimeUtils.MAX_TS)) {
			return cotCacheHelper.getLatestCotWrapperForUid(uid, groupVector).getCotElement();
		} else {
			return getLatestCotElement(uid, groupVector, end,
					new ResultSetExtractor<CotElement>() {
						@Override
						public CotElement extractData(ResultSet results) throws SQLException {
							if (results == null) {
								throw new IllegalStateException("null result set");
							}

							if (!results.next()) {
								throw new NotFoundException("no results");
							}

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

							return cotElement;
						}
					});
		}
	}

	@Override
	public CotElement getLatestCotForUid(String uid, String groupVector) {
		try {
			return getLatestCotForUid(uid, groupVector, TimeUtils.MAX_TS);
		} catch (NotFoundException nfe) {
			return null;
		}
	}

	@Override
	public List<CotElement> getCotElementsByTimeAndBbox(
			Date start, Date end, GeospatialFilter.BoundingBox boundingBox, String groupVector) {
		try {

			String sql = "select "
					+ "uid, ST_X(event_pt), ST_Y(event_pt), "
					+ " point_hae, cot_type, servertime, point_le, detail, id, how, stale "
					+ ", point_ce "
					+ "from cot_router where " +
					"   servertime >= ? and servertime <= ? and " +
					"   ( cot_type = 'b-t-f' or ( " +
					"    (cot_type like 'a-f%' or cot_type like 'a-n%' or cot_type like 'a-u%' " +
					"       or cot_type like 'a-h%' or cot_type like 'b-m-r%' ) ";

			Object[] params = null;
			if (boundingBox != null) {
				sql += " and event_pt && ST_MakeEnvelope(?, ?, ?, ?, 4326) ";
				params = new Object[] { start, end, boundingBox.getMinLongitude(), boundingBox.getMinLatitude(),
						boundingBox.getMaxLongitude(), boundingBox.getMaxLatitude(), groupVector };
			} else {
				params = new Object[] { start, end, groupVector };
			}

			sql += remoteUtil.getGroupAndClause() + ")) order by servertime desc";

			return new JdbcTemplate(dataSource).query(sql, params,
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

								cotElements.add(cotElement);
							}

							return cotElements;
						}
					});

		} catch (Exception e) {
			logger.error("exception in getCotElementsByTimeAndBbox! " + e.getMessage());
			return null;
		}
	}

	@Override
	public List<CotElement> getLatestCotForUids(Set<String> uids, String groupVector) {

		List<CotElement> cotElements = new ArrayList<>();

		for (CotCacheWrapper wrapper : cotCacheHelper.getLatestCotWrappersForUids(uids, groupVector, false)) {

			CotElement element = wrapper.getCotElement();

			if (element != null) {
				cotElements.add(element);
			}
		}

		return cotElements;
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public Collection<CotCacheWrapper> getLatestMissionCotWrappersForUids(UUID missionGuid, Set<String> uids, String groupVector) {

		return cotCacheHelper.getLatestCotWrappersForUids(uids, groupVector, true);
	}


	private List<String> getKeywordList(String key, Map<String, List<String>> map) {
		List<String> keywords = map.get(key);
		if (keywords == null) {
			keywords = new LinkedList<>();
			map.put(key, keywords);
		}
		return keywords;
	}

	private Map<String, Map<String, List<String>>> getMissionKeywordsMap(Long missionId) {

		String sql = "(select 'uid', uid, keyword from mission_uid_keyword where mission_id = ? order by id) " +
				"union all (select 'hash', hash, keyword from mission_resource_keyword where mission_id = ? order by id)";

		return new JdbcTemplate(dataSource).query(sql,
				new Object[] { missionId, missionId },
				new ResultSetExtractor< Map<String, Map<String, List<String>>> >() {
					@Override
					public Map<String, Map<String, List<String>>> extractData(ResultSet results) throws SQLException {
						if (results == null) {
							throw new IllegalStateException("null result set");
						}

						Map<String, List<String>> uidMap = new HashMap<>();
						Map<String, List<String>> resourceMap = new HashMap<>();

						Map<String, Map<String, List<String>>> keywordMap = new HashMap<>();
						keywordMap.put("uid", uidMap);
						keywordMap.put("resource", resourceMap);

						while (results.next()) {

							String type = results.getString(1);
							String key = results.getString(2);
							String keyword = results.getString(3);

							List<String> keywords = null;

							if (type.compareTo("uid") == 0) {
								keywords = getKeywordList(key, uidMap);
							} else if (type.compareTo("hash") == 0) {
								keywords = getKeywordList(key, resourceMap);
							}

							keywords.add(keyword);
						}

						return keywordMap;
					}
				});
	}

	@Override
	public List<CotElement> getAllCotForUid(String uid, Date start, Date end, String groupVector) {

		String sql =  "select " // query
				+ "cot_type, servertime, stale, "
				+ "how, point_hae, point_ce, point_le, detail, "
				+ "ST_X(event_pt), ST_Y(event_pt), uid "
				+ "from cot_router where uid=? and servertime >= ? and servertime <= ? "
				+ remoteUtil.getGroupAndClause()
				+ "order by servertime desc";

		AuditLogUtil.auditLog(sql + "uid: " + uid + " groupVector: " + groupVector);

		return new JdbcTemplate(dataSource).query(sql,
				new Object[] { uid, start, end, groupVector }, // parameters
				new ResultSetExtractor<List<CotElement>>() {
					@Override public List<CotElement> extractData(ResultSet results) throws SQLException {
						if (results == null) {
							throw new IllegalStateException("null result set");
						}

						if (!results.next()) {
							throw new NotFoundException("no results");
						}

						List<CotElement> cotElements = new ArrayList<CotElement>();

						do {
							CotElement cotElement = new CotElement();

							cotElement.cottype = results.getString(1);
							cotElement.servertime = results.getTimestamp(2);
							cotElement.staletime = results.getTimestamp(3);
							cotElement.how = results.getString(4);
							cotElement.hae = results.getString(5);
							cotElement.ce = results.getDouble(6);
							cotElement.le = results.getDouble(7);
							cotElement.detailtext = results.getString(8);
							cotElement.lon = results.getDouble(9);
							cotElement.lat = results.getDouble(10);
							cotElement.uid = results.getString(11);

							cotElements.add(cotElement);
						} while (results.next());

						return cotElements;
					}
				});
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator", sync = true)
	public String getCachedCot(String missionName, Set<String> uids, String groupVector) {

		StringBuilder result = new StringBuilder();
		result.append(Constants.XML_HEADER);
		result.append("<events>");

		// Get mission uids, then the latest CoT for each
		List<CotElement> cot = getLatestCotForUids(uids, groupVector);
		@SuppressWarnings("rawtypes")
		Iterator it = cot.iterator();
		while (it.hasNext()) {
			CotElement cotElement = (CotElement)it.next();
			result.append(cotElement.toCotXml());
			result.append('\n');
		}

		result.append("</events>");

		return result.toString();
	}
	
	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator", sync = true)
	public String getCachedCot(UUID missionGuid, Set<String> uids, String groupVector) {

		StringBuilder result = new StringBuilder();
		result.append(Constants.XML_HEADER);
		result.append("<events>");

		// Get mission uids, then the latest CoT for each
		List<CotElement> cot = getLatestCotForUids(uids, groupVector);
		@SuppressWarnings("rawtypes")
		Iterator it = cot.iterator();
		while (it.hasNext()) {
			CotElement cotElement = (CotElement)it.next();
			result.append(cotElement.toCotXml());
			result.append('\n');
		}

		result.append("</events>");

		return result.toString();
	}

	@Override
	public boolean deleteAllCotForUids(List<String> uids, String groupVector) {
		try {
			String sql = "delete from cot_router where uid = any(?) " + remoteUtil.getGroupAndClause();

			AuditLogUtil.auditLog(sql + "uids: " + uids + " groupVector: " + groupVector);

			Connection connection = dataSource.getConnection();
			Array uidArray = connection.createArrayOf("varchar", uids.toArray());
			connection.close();

			int updated = new JdbcTemplate(dataSource).update(sql,
					new Object[] {
							uidArray,
							groupVector
					});

			return updated > 0;

		} catch (SQLException e) {
			logger.error("sql exception in deleteAllCotForUids! " + e.getMessage());
			return false;
		}
	}
	
	@Override
	@CacheEvict(cacheResolver = MissionLayerCacheResolver.MISSION_LAYER_CACHE_RESOLVER, allEntries = true)
	public synchronized MissionLayer addMissionLayer(
			String missionName, Mission mission, String uid, String name, MissionLayer.Type type,
			String parentUid, String afterUid, String creatorUid, String groupVector) {

		if (uid != null) {
			MissionLayer existing =  missionLayerRepository.findByUidNoMission(uid);
			if (existing != null) {
				setLayerParent(missionName, mission, uid, parentUid, afterUid, creatorUid);
				return existing;
			}
		}

		MissionLayer missionLayer = new MissionLayer(uid);

		if (!Strings.isNullOrEmpty(parentUid)) {
			MissionLayer parentLayer = missionLayerRepository.findByUidNoMission(parentUid);
			missionLayer.setParent(parentLayer);
		}

		missionLayer.setName(name);
		missionLayer.setType(type);
		missionLayer.setAfter(afterUid);

		missionLayerRepository.fixupAfter(missionLayer.getUid(), parentUid, afterUid);

		missionLayerRepository.save(missionLayer.getUid(), missionLayer.getName(), missionLayer.getType().ordinal(),
				parentUid, afterUid, mission.getId());

		subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), missionName, SubscriptionManagerLite.ChangeType.MISSION_LAYER,
				creatorUid, mission.getTool(), commonUtil.toXml(missionLayer));


		return missionLayer;
	}
	
	

	@Override
	@CacheEvict(cacheResolver = MissionLayerCacheResolver.MISSION_LAYER_CACHE_RESOLVER, allEntries = true)
	public void setLayerName(String missionName, Mission mission, String layerUid, String name, String creatorUid) {

		MissionLayer layer = missionLayerRepository.findByUidNoMission(layerUid);
		if (layer == null) {
			throw new NotFoundException();
		}

		MissionLayer updated = new MissionLayer(layer);

		missionLayerRepository.setName(layerUid, name);
		updated.setName(name);

		subscriptionManager.announceMissionChange(mission.getGuidAsUUID(), mission.getName(), SubscriptionManagerLite.ChangeType.MISSION_LAYER,
				creatorUid, mission.getTool(), commonUtil.toXml(updated));
	}

	@Override
	@CacheEvict(cacheResolver = MissionLayerCacheResolver.MISSION_LAYER_CACHE_RESOLVER, allEntries = true)
	public synchronized void setLayerPosition(
			String missionName, Mission mission, String layerUid, String afterUid, String creatorUid) {

		MissionLayer layer = missionLayerRepository.findByUidNoMission(layerUid);
		if (layer == null) {
			throw new NotFoundException();
		}

		MissionLayer updated = new MissionLayer(layer);

		missionLayerRepository.fixupAfter(layer.getAfter(), layer.getParentUid(), layerUid);
		missionLayerRepository.fixupAfter(layerUid, layer.getParentUid(), afterUid);
		missionLayerRepository.setAfter(layerUid, layer.getParentUid(), afterUid);
		updated.setAfter(afterUid);

		subscriptionManager.announceMissionChange(mission.getGuidAsUUID(), mission.getName(), SubscriptionManagerLite.ChangeType.MISSION_LAYER,
				creatorUid, mission.getTool(), commonUtil.toXml(updated));
	}

	@Override
	@CacheEvict(cacheResolver = MissionLayerCacheResolver.MISSION_LAYER_CACHE_RESOLVER, allEntries = true)
	public synchronized void setLayerParent(
			String missionName, Mission mission, String layerUid, String parentUid, String afterUid, String creatorUid) {

		MissionLayer layer = missionLayerRepository.findByUidNoMission(layerUid);
		MissionLayer parent = parentUid == null ? null : missionLayerRepository.findByUidNoMission(parentUid);

		if (layer == null || (parentUid != null && parent == null)) {
			throw new NotFoundException();
		}

		MissionLayer updated = new MissionLayer(layer);

		updated.setParent(parent);

		missionLayerRepository.fixupAfter(layer.getAfter(), layer.getParentUid(), layerUid);
		missionLayerRepository.fixupAfter(layerUid, parentUid, afterUid);

		if (layer.getParentUid() == null || !layer.getParentUid().equals(parentUid)) {
			missionLayerRepository.setParent(layerUid, parentUid);
		}

		missionLayerRepository.setAfter(layerUid, parentUid, afterUid);
		updated.setAfter(afterUid);

		subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), missionName, SubscriptionManagerLite.ChangeType.MISSION_LAYER,
				creatorUid, mission.getTool(), commonUtil.toXml(updated));
	}

	private void removeMissionLayerData(
			MissionLayer layer,  Mission mission, String creatorUid, String groupVector) {

		for (MissionLayer child : layer.getChildren()) {
			getMissionService().removeMissionLayer(mission.getName(), mission, child.getUid(), creatorUid, groupVector);
		}

		MissionLayer hydrated = hydrateMissionLayers(mission, Arrays.asList(layer)).get(0);

		for (MissionAdd<String> missionAdd : hydrated.getUidAdds()) {
			getMissionService().deleteMissionContent(
					UUID.fromString(mission.getGuid()), null, missionAdd.getData(), creatorUid, groupVector);
		}

		for (MissionAdd<Resource> missionAdd : hydrated.getResourceAdds()) {
			getMissionService().deleteMissionContent(
					UUID.fromString(mission.getGuid()), ((Resource) missionAdd.getData()).getHash(), null, creatorUid, groupVector);
		}

		for (MissionAdd<MapLayer> missionAdd : hydrated.getMaplayerAdds()) {
			getMissionService().removeMapLayerFromMission(
					mission.getName(), creatorUid, mission, ((MapLayer) missionAdd.getData()).getUid());
		}

		missionLayerRepository.deleteByUid(layer.getUid());

		for (MissionLayer child : layer.getChildren()) {
			removeMissionLayerData(child, mission, creatorUid, groupVector);
		}
	}

	@Override
	@CacheEvict(cacheResolver = MissionLayerCacheResolver.MISSION_LAYER_CACHE_RESOLVER, allEntries = true)
	public synchronized void removeMissionLayer(
			String missionName, Mission mission, String layerUid, String creatorUid, String groupVector) {

		try {
			MissionLayer layer = missionLayerRepository.findByUidNoMission(layerUid);
			if (layer == null) {
				throw new NotFoundException();
			}

			missionLayerRepository.fixupAfter(layer.getAfter(), layer.getParentUid(), layerUid);

			removeMissionLayerData(layer,  mission, creatorUid, groupVector);

			subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), missionName, SubscriptionManagerLite.ChangeType.MISSION_LAYER,
					creatorUid, mission.getTool(), commonUtil.toXml(layer));

		} catch (Exception e) {
			logger.error("exception in removeMissionLayer", e);
		}
	}

	@Override
	@Cacheable(cacheResolver = MissionLayerCacheResolver.MISSION_LAYER_CACHE_RESOLVER,
			key="{#root.methodName, #root.args[0], #root.args[2]}", sync = true)
	public MissionLayer hydrateMissionLayer(String missionName, Mission mission, String layerUid) {
		MissionLayer missionLayer = missionLayerRepository.findByUidNoMission(layerUid);
		if (missionLayer == null) {
			throw new NotFoundException();
		}

		return hydrateMissionLayers(mission, Arrays.asList(missionLayer)).get(0);
	}

	@Override
	@Cacheable(cacheResolver = MissionLayerCacheResolver.MISSION_LAYER_CACHE_RESOLVER,
			key="{#root.methodName, #root.args[0]}", sync = true)
	public List<MissionLayer> hydrateMissionLayers(String missionName, Mission mission) {
		return hydrateMissionLayers(mission, missionLayerRepository.findMissionLayers(missionName));
	}

	private void hydrateLayerItems(
			MissionLayer parent, List<MissionLayer> layers, Map<String, Mission.MissionAdd<String>> uidMap,
			Map<String, Mission.MissionAdd<Resource>> resourceMap, Map<String, Mission.MissionAdd<MapLayer>> mapLayerMap) {

		if (parent != null) {
			List<MissionLayer> items = new ArrayList<>();
			for (MissionLayer item : layers) {
				if (item.getType() != MissionLayer.Type.ITEM) {
					continue;
				}
				items.add(item);
				if (parent.getType() == MissionLayer.Type.UID) {
					if (uidMap.get(item.getUid()) != null) {
						parent.getUidAdds().add(uidMap.get(item.getUid()));
					} else {
						logger.error("unable to hydrate layer content for uid " + item.getUid());
					}
				} else if (parent.getType() == MissionLayer.Type.CONTENTS) {
					if (resourceMap.get(item.getUid()) != null) {
						parent.getResourceAdds().add(resourceMap.get(item.getUid()));
					} else {
						logger.error("unable to hydrate layer content for file " + item.getUid());
					}
				} else if (parent.getType() == MissionLayer.Type.MAPLAYER) {
					if (mapLayerMap.get(item.getUid()) != null) {
						parent.getMaplayerAdds().add(mapLayerMap.get(item.getUid()));
					} else {
						logger.error("unable to hydrate layer content for map layer " + item.getUid());
					}
				}
			}

			parent.getChildren().removeAll(items);
		}

		for (MissionLayer layer : layers) {
			hydrateLayerItems(layer, layer.getChildren(), uidMap, resourceMap, mapLayerMap);
		}
	}

	private List<MissionLayer> hydrateMissionLayers(
			Mission mission, List<MissionLayer> missionLayers) {

		Map<String, Mission.MissionAdd<String>> uidMap = new HashMap<>();
		Map<String, Mission.MissionAdd<Resource>> resourceMap = new HashMap<>();
		Map<String, Mission.MissionAdd<MapLayer>> mapLayerMap = new HashMap<>();

		//
		// build up maps for quick reference of mission content
		//
		for (Mission.MissionAdd<String> missionAdd : mission.getUidAdds()) {
			uidMap.put((String)missionAdd.getData(), missionAdd);
		}

		for (Mission.MissionAdd<Resource> missionAdd : mission.getResourceAdds()) {
			resourceMap.put(((Resource)missionAdd.getData()).getHash(), missionAdd);
		}

		for (MapLayer mapLayer : mission.getMapLayers()) {
			if (!Strings.isNullOrEmpty(mapLayer.getPath())) {
				MissionAdd<MapLayer> missionAdd = new MissionAdd<>();
				missionAdd.setData(mapLayer);
				mapLayerMap.put(mapLayer.getUid(), missionAdd);
			}
		}

		// recursively sort the mission layers
		missionLayers = MissionLayer.sortMissionLayers(mission.getName(), missionLayers);

		// populate the sorted layer items
		hydrateLayerItems(null, missionLayers, uidMap, resourceMap, mapLayerMap);

		return missionLayers;
	}

	@Override
	public Mission hydrate(Mission mission, boolean hydrateDetails) {
		
		long start = System.currentTimeMillis();

		logger.debug("hyrdate mission details: {} mission: {} ", hydrateDetails, mission);
		
		if (mission == null) {
			return null;
		}

		if (Strings.isNullOrEmpty(mission.getName())) {
			logger.warn("empty mission name");

			return mission;
		}

		// Remove hibernate proxies from the mission before manipulating the context during hydration (below).
		UnproxyHelper.unproxyMission(mission);

		List<MissionAdd<String>> uidAdds = new LinkedList<>();
		List<MissionAdd<Resource>> resourceAdds = new LinkedList<>();

		// collect up the hashes & fill in keywords
		HashMap<String, Resource> resourceMap = new HashMap<>();
		
		if (!mission.getContents().isEmpty()) {
			
			logger.debug("fetching contents of mission {} {}", mission.getName(), mission.getGuid());
			
			Set<Resource> resources = mission.getContents();
			Map<Integer, List<String>> keywordMap = getMissionService().getCachedResourcesByGuid(mission.getGuidAsUUID(), resources);  // hydrate resources
				for (Resource resource : resources) {
					List<String> keywords = keywordMap.get(resource.getId());
					resource.setKeywords(keywords);
					resourceMap.put(resource.getHash(), resource);
				}
			}

		Metrics.timer("method-exec-timer-hydrateMission-hashes", "takserver", "MissionService").record(Duration.ofMillis(System.currentTimeMillis() - start));

		long startUids = System.currentTimeMillis();

		// collect up the uids
		Map<String, UidDetails> uidDetailsMap = new ConcurrentHashMap<>();
		if (hydrateDetails && !mission.getUids().isEmpty()) {
			Collection<CotCacheWrapper> cotWrappers = getMissionService().getLatestMissionCotWrappersForUids(mission.getGuidAsUUID(), mission.getUids(), mission.getGroupVector());

			cotWrappers.forEach((wrapper) -> uidDetailsMap.put(wrapper.getUid(), wrapper.getUidDetails()));
		}

		Metrics.timer("method-exec-timer-hydrateMission-uids", "takserver", "MissionService").record(Duration.ofMillis(System.currentTimeMillis() - startUids));

		long startChanges = System.currentTimeMillis();

		// get the latest mission change for each item in the mission
		List<MissionChange> changes = null;
		if (!mission.getUids().isEmpty() && !mission.getContents().isEmpty()) {
			changes = getMissionService().findLatestCachedMissionChanges(// db / cache
					mission.getGuidAsUUID(), new ArrayList<>(mission.getUids()),
					new ArrayList<>(resourceMap.keySet()), MissionChangeType.ADD_CONTENT.ordinal());
		} else if (!mission.getUids().isEmpty()) {
			changes = getMissionService().findLatestCachedMissionChangesForUids(// db / cache
					mission.getGuidAsUUID(), new ArrayList<>(mission.getUids()), MissionChangeType.ADD_CONTENT.ordinal());
		} else if (!mission.getContents().isEmpty()) {
			changes = getMissionService().findLatestCachedMissionChangesForHashes( // db / cache
					mission.getGuidAsUUID(), new ArrayList<>(resourceMap.keySet()), MissionChangeType.ADD_CONTENT.ordinal());
			
		} else {
			return mission;
		}

		Metrics.timer("method-exec-timer-hydrateMission-getChanges", "takserver", "MissionService").record(Duration.ofMillis(System.currentTimeMillis() - startChanges));


		Map<String, List<String>> uidKeywordMap = null;
		Map<String, List<String>> contentKeywordMap = null;

		if (hydrateDetails) {
			Map<String, Map<String, List<String>>> keywordMap = getMissionKeywordsMap(mission.getId());
			uidKeywordMap = keywordMap.get("uid");
			contentKeywordMap = keywordMap.get("resource");
		}

		long startProcessChanges = System.currentTimeMillis();

		for (MissionChange change : changes) { // iteration
			if (change.getContentUid() != null && change.getContentUid().length() > 0) {

				MissionAddDetails<String> uidAdd = new MissionAddDetails<>();
				uidAdd.setData(change.getContentUid());
				uidAdd.setTimestamp(change.getTimestamp());
				uidAdd.setCreatorUid(change.getCreatorUid());

				if (hydrateDetails) {
					UidDetails uidDetails = uidDetailsMap.get(change.getContentUid());
					if (uidDetails != null) {
						uidAdd.setUidDetails(uidDetails);
					}

					if (uidKeywordMap != null) {
						uidAdd.setKeywords(uidKeywordMap.get(change.getContentUid()));
					}
				}

				uidAdds.add(uidAdd);

			} else if (change.getContentHash() != null && change.getContentHash().length() > 0) {
				MissionAdd<Resource> resourceAdd = new MissionAdd<>();
				resourceAdd.setData(resourceMap.get(change.getContentHash()));
				resourceAdd.setTimestamp(change.getTimestamp());
				resourceAdd.setCreatorUid(change.getCreatorUid());

				if (hydrateDetails && contentKeywordMap != null) {
					resourceAdd.setKeywords(contentKeywordMap.get(change.getContentHash()));
				}

				resourceAdds.add(resourceAdd);
			}
		}

		Metrics.timer("method-exec-timer-hydrateMission-processChanges", "takserver", "MissionService").record(Duration.ofMillis(System.currentTimeMillis() - startProcessChanges));


		mission.getUidAdds().clear();
		mission.getUidAdds().addAll(uidAdds);

		mission.getResourceAdds().clear();
		mission.getResourceAdds().addAll(resourceAdds);

		long startFinish = System.currentTimeMillis();

		Metrics.timer("method-exec-timer-hydrateMission-finish", "takserver", "MissionService").record(Duration.ofMillis(System.currentTimeMillis() - startFinish));

		if (cacheLogger.isTraceEnabled()) {
			cacheLogger.trace("mission after hydrate " + hydrateDetails + " complete : " + mission);
		}

		return mission;
	}


	// Fill in keywords, return a map of resource ids to keywords
	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator", sync = true)
	public Map<Integer, List<String>> cachedMissionHydrate(String missionName, Set<Resource> resources) {
		return hydrate(resources);
	}

	public Map<Integer, List<String>> hydrate(Set<Resource> resources) {
		try {
			List<Integer> resourceIds = new LinkedList<>();
			for (Resource resource : resources) {
				resourceIds.add(resource.getId());
			}

			Array idArray = null;
			try (Connection connection = dataSource.getConnection()) {
				idArray = connection.createArrayOf("int", resourceIds.toArray());
			}
			
			logger.debug("hydrating resourceIds {}", resourceIds);

			Map<Integer, List<String>> keywordsMap = new HashMap<>();

			return new JdbcTemplate(dataSource).query(
					"select id, keywords from resource where id = any (?) ", // query
					new Object[] { idArray }, // parameters
					new ResultSetExtractor<Map<Integer, List<String>>>() {
						@Override public Map<Integer, List<String>> extractData(ResultSet results) throws SQLException {
							if (results == null) {
								throw new IllegalStateException("null result set");
							}

							while (results.next()) {
								int id = results.getInt(1);
								Array keywordsArray = results.getArray(2);

								List<String> keywords = new ArrayList<>();
								if (keywordsArray != null) {
									keywords.addAll(Lists.newArrayList((String[]) keywordsArray.getArray()));
								}
								keywordsMap.put(id, keywords);
							}

							return keywordsMap;
						}
					});

		} catch (Exception e) {
			logger.error("exception in hydrate resources (thread " + Thread.currentThread().getName() + ")", e);
			return null;
		}
	}

	public Resource hydrate(@NotNull Resource resource) {

		if (resource == null) {
			throw new IllegalArgumentException("null resource");
		}

		resource.setKeywords(new JdbcTemplate(dataSource).query(
				"select keywords from resource where id = ?", // query
				new Object[] { resource.getId() }, // parameters
				new ResultSetExtractor<List<String>>() {
					@Override public List<String> extractData(ResultSet results) throws SQLException {
						if (results == null) {
							throw new IllegalStateException("null result set");
						}

						List<String> keywords = new ArrayList<>();

						if (!results.next()) {
							logger.debug("no keywords");
							return keywords;
						}

						Array kwSqlArray = results.getArray(1);

						if (kwSqlArray != null) {
							keywords = Lists.newArrayList((String[]) kwSqlArray.getArray());
						}

						return keywords;
					}
				}));

		return resource;
	}

	public UidDetails hydrate(@NotNull UidDetails uidDetails, @NotNull CotElement cot) {

		if (uidDetails == null) {
			throw new IllegalArgumentException("null uidDetails");
		}

		if (logger.isDebugEnabled()) {
			if (cot == null) {
				logger.debug("null not in hydrate");
			}
		}

		uidDetails.type = cot.cottype;

		try {
			if (cot.lat != 0 && cot.lon != 0) {
				uidDetails.location = new Location(cot.lat, cot.lon);
			}

			Document doc = SecureXmlParser.makeDocument(cot.detailtext);

			NodeList nodeList = doc.getElementsByTagName("detail");
			if (nodeList == null || nodeList.getLength() != 1) {
				return null;
			}

			nodeList = nodeList.item(0).getChildNodes();

			String medevacTitle = null;

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
				} else if (name.compareTo("attachment_list") == 0) {
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

			KmlIconStrategyJaxb.ParseUserIcon(doc, cot);
			JDBCCachingKMLDao.ParseDetailText(cot);

			// we need to unescape the callsign here to prevent double encoding of &'s by the serialization layer
			uidDetails.callsign = StringEscapeUtils.unescapeXml(cot.callsign);

			if (cot.iconSetPath != CotElement.errorMessage) {
				uidDetails.iconsetPath = cot.iconSetPath;
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


	public UidDetails hydrate(@NotNull UidDetails uidDetails, String uid, Date timestamp) {

		if (uidDetails == null) {
			throw new IllegalArgumentException("null uidDetails");
		}

		CotElement cot;

		try {
			// try to get the most recent version of this cot prior to the change's timestamp
			cot = getLatestCotForUid(
					uid, RemoteUtil.getInstance().getBitStringAllGroups(), timestamp);
		} catch (NotFoundException nfe) {
			// default to the most recent version
			cot = getLatestCotForUid(
					uid, RemoteUtil.getInstance().getBitStringAllGroups());
		}

		return hydrate(uidDetails, cot);
	}

	@Override
	public ExternalMissionData hydrate(String externalDataUid, String externalDataName, String externalDataTool,
									   String externalDataToken, String externalDataNotes) {

		try {
			String urlData = null;
			String urlView = null;
			ExternalMissionData externalMissionData = externalMissionDataRepository.findByIdNoMission(externalDataUid);
			if (externalMissionData != null) {
				urlData = externalMissionData.getUrlData();
				urlView = externalMissionData.getUrlDisplay();
				if (externalDataToken != null) {
					urlData += "?token=" + externalDataToken;
					urlView += "?token=" + externalDataToken;
				}
			}

			ExternalMissionData result = new ExternalMissionData(
					externalDataName,
					externalDataTool,
					urlData,
					urlView,
					externalDataNotes);
			result.setId(externalDataUid);
			return result;

		} catch (Exception e) {
			logger.error("exception hydrating ExternalMissionData!", e);
		}

		return null;
	}

	@Override
	@CacheEvict(value = Constants.INVITE_ONLY_MISSION_CACHE, allEntries = true)
	public void missionInvite(
			UUID missionGuid, String invitee, MissionInvitation.Type type, MissionRole role, String creatorUid, String groupVector) {
		try {

			// validate existence of mission
			Mission mission = getMissionService().getMissionByGuidCheckGroups(missionGuid, groupVector);
			validateMissionByGuid(mission);

			String token = generateToken(
					UUID.randomUUID().toString(), mission.getGuidAsUUID(), mission.getName(), MissionTokenUtils.TokenType.INVITATION, -1);

			MissionInvitation missionInvitation = new MissionInvitation(mission.getName(),
					UUID.fromString(mission.getGuid()), invitee, type.name(), creatorUid, new Date(), token, role, mission.getId());

			missionInvite(mission, missionInvitation);

		} catch (Exception e) {
			logger.error("exception in missionInvite!", e);
		}
	}

	@Override
	@CacheEvict(value = Constants.INVITE_ONLY_MISSION_CACHE, allEntries = true)
	public void missionInvite(Mission mission, MissionInvitation missionInvitation) {

		try {

			try {
				missionInvitationRepository.save(missionInvitation);
			} catch (DataIntegrityViolationException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("attempt to add duplicate invitation : " + missionInvitation.toString());
				}
			} catch (Exception e) {
				logger.error("Exception calling missionInvitationRepository.save", e);
			}

			String[] contactUids = null;

			String roleXml = commonUtil.toXml(missionInvitation.getRole());

			switch (MissionInvitation.Type.valueOf(missionInvitation.getType())) {
				case clientUid: {
					contactUids = new String[] { missionInvitation.getInvitee() };
					break;
				}
				case callsign: {
					RemoteSubscription subscription = subscriptionManager
							.getSubscriptionByCallsign(missionInvitation.getInvitee());
					if (subscription != null) {
						contactUids = new String[] { subscription.clientUid };
					}
					break;
				}
				case userName: {

					RemoteSubscription subscription = subscriptionManager
							.getSubscriptionByUsersDisplayName(missionInvitation.getInvitee());
					if (subscription != null) {
						// notify the user directly if they are online
						contactUids = new String[]{subscription.clientUid};
					}

					try {

						CoreConfig coreConfig = CoreConfigFacade.getInstance();

						// notify this user via email if the username is an email address and email is configured
						if (EmailValidator.getInstance().isValid(missionInvitation.getInvitee()) &&
								coreConfig.getRemoteConfiguration().getEmail() != null) {

							StringBuilder builder = new StringBuilder(
									"You have been invited to the following mission");
							builder.append("\n\nName: ");
							builder.append(mission.getName());

							if (!Strings.isNullOrEmpty(mission.getDescription())) {
								builder.append("\nDescription: ");
								builder.append(mission.getDescription());
							}

							if (!Strings.isNullOrEmpty(coreConfig.getRemoteConfiguration()
									.getNetwork().getTakServerHost())) {
								builder.append("\nServer: ");
								builder.append(coreConfig.getRemoteConfiguration()
										.getNetwork().getTakServerHost());
							}

							builder.append("\nRole: ");
							builder.append(missionInvitation.getRole().getRole().name());

							EmailClient.sendEmail(
									coreConfig.getRemoteConfiguration().getEmail(), "Mission Invitation",
									builder.toString(), missionInvitation.getInvitee(), null, null);
						}
					} catch (Exception e) {
						logger.error("exception sending mission notification", e);
					}

					break;
				}
				case group: {
					List<String> contacts = new LinkedList<>();
					Group group = groupManager.getGroup(missionInvitation.getInvitee(), Direction.IN);
					if (group != null) {
						for (com.bbn.marti.remote.groups.Node node : group.getNeighbors()) {
							if (node.isLeaf() && (node instanceof AuthenticatedUser)
									&& ((AuthenticatedUser) node).getConnectionType() == ConnectionType.CORE) {
								contacts.add(((AuthenticatedUser) node).getCotSaUid());
							}
						}
					}
					contactUids = contacts.toArray(new String[contacts.size()]);
					break;
				}
				case team: {
					break;
				}
			}

			if (contactUids == null || contactUids.length == 0) {
				logger.error("Unable to determine contactUids for invite! "
						+ StringUtils.normalizeSpace(missionInvitation.getType()) + ", " + StringUtils.normalizeSpace(missionInvitation.getInvitee()));
			} else {
				subscriptionManager.sendMissionInvite(UUID.fromString(mission.getGuid()) ,mission.getName(), contactUids,
						missionInvitation.getCreatorUid(), mission.getTool(), missionInvitation.getToken(), roleXml);
			}

		} catch (Exception e) {
			logger.error("Exception in missionInvite2!", e);
		}
	}

	@Override
	@CacheEvict(value = Constants.INVITE_ONLY_MISSION_CACHE, allEntries = true)
	public void missionUninvite(
			UUID missionGuid, String invitee, MissionInvitation.Type type, String creatorUid, String groupVector) {

		// validate existence of mission
		Mission mission = getMissionService().getMissionByGuidCheckGroups(missionGuid, groupVector);

		validateMissionByGuid(mission);

		// TODO: make sure this updated SQL is correct
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("missionGuid", missionGuid);
		namedParameters.addValue("invitee", invitee);
		namedParameters.addValue("type", type.name());
		String sql = "delete from mission_invitation where mission_id in (select id from mission where guid = uuid(:missionGuid)) and " +
				" lower(invitee) = lower(:invitee) and type = :type";
		new NamedParameterJdbcTemplate(dataSource).update(sql, namedParameters);

	}

	@Override
	public List<MissionInvitation> getMissionInvitations(String missionName) {

		Long missionId = missionRepository.getLatestMissionIdForName(missionService.trimName(missionName.toLowerCase()));

		if (missionId == null) {
			throw new NotFoundException("mission " + missionName + " does not exist.");
		}

		return missionInvitationRepository.findAllByMissionId(missionId);
	}
	
	@Override
	public List<MissionInvitation> getMissionInvitationsByGuid(UUID missionGuid) {

		Long missionId = missionRepository.getLatestMissionIdForMissionGuid(missionGuid.toString());

		if (missionId == null) {
			throw new NotFoundException("mission " + missionGuid + " does not exist.");
		}

		return missionInvitationRepository.findAllByMissionId(missionId);
	}

	@Override
	public Set<MissionInvitation> getAllMissionInvitationsForClient(String clientUid, String groupVector) {

		try {
			Set<MissionInvitation> missionInvitations = new HashSet<>();

			// add invitations for the clientUid
			missionInvitations.addAll(missionInvitationRepository.findAllMissionInvitationsByInviteeIgnoreCaseAndType(
					clientUid, MissionInvitation.Type.clientUid.name(), groupVector));

			// get the streaming channel subscription for this clientUid
			RemoteSubscription subscription = subscriptionManagerProxy.getSubscriptionManagerForClientUid(clientUid).getRemoteSubscriptionByClientUid(clientUid);
			if (subscription != null) {

				// add any invitations for the associated streaming channel callsign
				missionInvitations.addAll(missionInvitationRepository.findAllMissionInvitationsByInviteeIgnoreCaseAndType(
						subscription.callsign, MissionInvitation.Type.callsign.name(), groupVector));
			}

			// add any invitations for the authenticated username
			String username = SecurityContextHolder.getContext().getAuthentication().getName();
			missionInvitations.addAll(missionInvitationRepository.findAllMissionInvitationsByInviteeIgnoreCaseAndType(
					username, MissionInvitation.Type.userName.name(), groupVector));

			return missionInvitations;

		} catch (Exception e) {
			logger.error("Exception in getAllMissionInvitationsForClient!", e);
			return null;
		}
	}

	@Override
	@Transactional
	public MissionSubscription missionSubscribe(UUID missionGuid, String clientUid, MissionRole missionRole, String groupVector) {
		Mission mission = getMissionService().getMissionByGuid(missionGuid, groupVector);

		String username = "";
		try {
			username = SecurityContextHolder.getContext().getAuthentication().getName();
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception getting username", e);
			}
		}

		return missionSubscribe(missionGuid, mission.getId(), clientUid, username, missionRole, groupVector);
	}

	@Override
	@Transactional
	public MissionSubscription missionSubscribe(UUID missionGuid, String clientUid, String groupVector) {
		Mission mission = getMissionService().getMissionByGuid(missionGuid, groupVector);
		return missionSubscribe(missionGuid, clientUid, getDefaultRole(mission), groupVector);
	}

	@Override
	public MissionSubscription missionSubscribe(UUID missionGuid, Long missionId, String clientUid, String username, MissionRole role, String groupVector) {
		try {

			subscriptionManagerProxy.getSubscriptionManagerForClientUid(clientUid).missionSubscribe(missionGuid, clientUid);

			MissionSubscription missionSubscription = null;
			
			logger.debug("findByMissionGuidAndClientUidAndUsernameNoMission {} {} {}", missionGuid.toString(), clientUid, username);
			
			Mission m = getMissionByGuid(missionGuid, false);
			
			missionSubscription = missionSubscriptionRepository.findByMissionGuidAndClientUidAndUsernameNoMission(missionGuid.toString(), clientUid, username);

			if (missionSubscription == null) {

				String subscriptionUid = UUID.randomUUID().toString();
				String token = generateToken(
						subscriptionUid, missionGuid, m.getName(), MissionTokenUtils.TokenType.SUBSCRIPTION,-1);

				logger.debug("creating subscription with uid {} ", subscriptionUid);

				missionSubscription = new MissionSubscription(subscriptionUid, token, null, clientUid, username, new Date(), role);

				missionSubscriptionRepository.subscribe(missionId, clientUid, new Date(), subscriptionUid, token, role.getId(), username);

			} else {
				if (!role.isUsingMissionDefault() && missionSubscription.getRole() != null && missionSubscription.getRole().compareTo(role) != 0) {
					if (logger.isDebugEnabled()) {
						logger.debug("updating subscription role");
					}
					missionSubscription.setRole(role);

					getMissionService().setRoleByClientUidOrUsername(missionId, clientUid, null, role.getId());
				}
			}

			return missionSubscription;

		} catch (DataIntegrityViolationException e) {
			logger.debug("mission already contained subscription {} {} ", missionGuid, clientUid);
			return null;
		} catch (Exception e) {
			logger.error("Exception in missionSubscribe!", e);
			return null;
		}
	}

	@Override
	@Transactional
	public void missionUnsubscribe(UUID missionGuid, String uid, String username, String groupVector, boolean disconnectOnly) {
		try {
			subscriptionManagerProxy.getSubscriptionManagerForClientUid(uid).missionUnsubscribe(missionGuid, uid, username, disconnectOnly);
		} catch (Exception e) {
			throw new TakException(e);
		}
	}

	@Override
	public List<Map.Entry<String, String>> getAllMissionSubscriptions()  {
		List<Map.Entry<String, String>> results = new ArrayList<>();
		for (MissionSubscription subscription : missionSubscriptionRepository.findAll()) {
			results.add(Maps.immutableEntry(subscription.getMission().getName(), subscription.getClientUid()));
		}
		return results;
	}
	
	@Override
	public List<Map.Entry<Map.Entry<String, String>, String>> getAllMissionSubscriptionsWithGuid()  {
		List<Map.Entry<Map.Entry<String, String>, String>> results = new ArrayList<>();
		for (MissionSubscription subscription : missionSubscriptionRepository.findAll()) {
			Map.Entry<String, String> missionId = Maps.immutableEntry(subscription.getMission().getGuidAsUUID().toString(), subscription.getMission().getName());
			results.add(Maps.immutableEntry(missionId, subscription.getClientUid()));
		}
		return results;
	}

	private AtomicInteger addCount = new AtomicInteger();
	
	@Override
	public Mission addMissionContentAtTime(UUID missionGuid, MissionContent missionContent, String creatorUid, String groupVector, Date date, String xmlContentForNotification) {
		
		if (missionGuid == null) {
			throw new IllegalArgumentException("null missionGuid");
		}

		logger.debug("addMissionContentAtTime missionContent {} missionGuid: {} creatorUid: {} ", missionContent, missionGuid, creatorUid);
		
		Mission mission = getMissionService().getMissionByGuidCheckGroups(missionGuid, groupVector);
		getMissionService().validateMissionByGuid(mission);

		logger.debug("mission for add content: {}", mission);
		
		Collection<MissionChange> changes = new CopyOnWriteArrayList<MissionChange>();

		Map<String, List<MissionContent>> contentMap = new HashMap<>();
		contentMap.put(null, Arrays.asList(missionContent));
		if (missionContent.getPaths() != null) {
			contentMap.putAll(missionContent.getPaths());
		}

		CoreConfig coreConfig = CoreConfigFacade.getInstance();

		for (Map.Entry<String, List<MissionContent>> pathContentEntry : contentMap.entrySet()) {

			String path = pathContentEntry.getKey();
			List<MissionContent> contents = pathContentEntry.getValue();


			for (MissionContent content : contents) {

				String after = content.getAfter();

				// add the resource by hash if it exists
				for (String hash : content.getHashes()) {

					if (mission.getContents().size() >= coreConfig.getRemoteConfiguration().getBuffer().getQueue().getMissionContentLimit()) {
						logger.error("File limit (" + coreConfig.getRemoteConfiguration().getBuffer().getQueue().getMissionContentLimit() + ") exceeded for mission " + mission.getName() + " " + mission.getGuid());
						break;
					}

					if (hash != null) {

						List<Resource> resourceList = getMissionService().getCachedResourcesByHash(mission.getGuidAsUUID(), hash);

						if (!resourceList.isEmpty() && resourceList.get(0) != null) {

							mission.getContents().add(resourceList.get(0));

							// track change
							MissionChange change = new MissionChange(MissionChangeType.ADD_CONTENT, mission, resourceList.get(0).getHash(), null);
							change.setTimestamp(date);
							change.setCreatorUid(creatorUid);
							missionChangeRepository.saveAndFlush(change);

							MissionAdd<Resource> resourceAdd = new MissionAdd<>();
							resourceAdd.setData(resourceList.get(0));
							resourceAdd.setTimestamp(change.getTimestamp());
							resourceAdd.setCreatorUid(change.getCreatorUid());

							List<MissionAdd<Resource>> resourceAdds = new CopyOnWriteArrayList<>();
							resourceAdds.add(resourceAdd);
							mission.setResourceAdds(resourceAdds);

							// explicitly save in case it didn't propagate from the change
							try {
								missionRepository.addMissionResource(mission.getId(), resourceList.get(0).getId(), resourceList.get(0).getHash());
							} catch (Exception e) {
								logger.debug("exception explicitly saving mission resource", e);
							}

							changes.add(change);

							if (logger.isDebugEnabled()) {
								logger.debug("Adding mission content mission id " + mission.getId() + " resource id " + resourceList.get(0).getId() + " resource name " + resourceList.get(0).getName() + " hash " + hash + " mission change " + change);
							}

							if (path != null) {
								try {
									getMissionService().addMissionLayer(
											mission.getName(), mission, resourceList.get(0).getHash(), null,
											MissionLayer.Type.ITEM, path, after, creatorUid, groupVector);
									after = resourceList.get(0).getHash();
								} catch (Exception e) {
									logger.error("exception adding mission layer", e);
								}
							}
						}
					}
				}

				for (String uid : content.getUids()) {
					logger.debug("adding mission content uid {}", uid);
					try {

						try {

							if (mission.getUids().size() >= coreConfig.getRemoteConfiguration().getBuffer().getQueue().getMissionUidLimit()) {
								logger.error("Track limit (" + coreConfig.getRemoteConfiguration().getBuffer().getQueue().getMissionUidLimit() + ") exceeded for mission {} {}", mission.getName(), mission.getGuid());
								break;
							}

							try {
								// also track in core services
								subscriptionManager.putMissionContentUid(mission.getGuidAsUUID(), uid);
							} catch (Exception e) {
								if (logger.isDebugEnabled()) {
									logger.debug("exception tracking mission content uid " + e.getMessage(), e);
								}
							}

						} catch (DataIntegrityViolationException e) {
							logger.info("mission already contains resource {} ", e.getMessage(), e);
						}

						// track change
						MissionChange change = new MissionChange(MissionChangeType.ADD_CONTENT, mission, null, uid);
						change.setTimestamp(date);
						change.setCreatorUid(creatorUid);

						asyncExecutor.execute(() -> {

							missionChangeRepository.saveAndFlush(change);

							// explicitly save in case it didn't propagate from the change
							try {
								missionRepository.addMissionUid(mission.getId(), uid);
							} catch (Exception e) {
								logger.debug("exception explicitly saving mission uid", e);
							}

						});

						changes.add(change);

						if (path != null) {
							try {
								getMissionService().addMissionLayer(
										mission.getName(), mission, uid, null,
										MissionLayer.Type.ITEM, path, after, creatorUid, groupVector);
								after = uid;
							} catch (Exception e) {
								logger.error("exception adding mission layer", e);
							}
						}

					} catch (Exception e) {
						logger.warn("exception saving mission change", e);
					}
				}

			}
		}

		if (changeLogger.isDebugEnabled()) {
			addCount.addAndGet(changes.size());
			changeLogger.debug("mission changes to save: " + changes.size() + " total changes: " + addCount.get());
		}

		for (MissionChange change : changes) {

			try {

				MissionChanges missionChanges = new MissionChanges();
				missionChanges.add(change);

				hydrateMissionChange(change);

				String changeXml = commonUtil.toXml(missionChanges);

				if (changeLogger.isTraceEnabled()) {
					changeLogger.trace(" announcing change " +  changeXml);
				}

				subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), mission.getName(), ChangeType.CONTENT, creatorUid, mission.getTool(), changeXml, xmlContentForNotification);

				if (logger.isDebugEnabled()) {
					logger.debug("mission change announced");
				}
			} catch (Exception e) {
				logger.warn("exception announcing mission change " + e.getMessage(), e);
			}
		}

		// only empty the cache if something was actually added
		if (!changes.isEmpty()) {
			try {
				getMissionService().invalidateMissionCache(UUID.fromString(mission.getGuid()), mission.getName());
			} catch (Exception e) {
				logger.warn("exception clearing mission cache {} {}", mission.getName(), mission.getGuid(), e);
			}
		}

		return mission;
	}

	@Override
	public Mission addMissionContent(UUID missionGuid, MissionContent content,  String creatorUid, String groupVector) {
		return addMissionContentAtTime(missionGuid, content, creatorUid, groupVector, new Date(), null);
	}

	public Metadata addToEnterpriseSync(byte[] contents, String name, String mimeType, List<String> keywords,
										String groupVector, Date submissionTime) {
		try {
			//
			// build up the metadata for adding to enterprise sync
			//
			Metadata toStore = new Metadata();
			toStore.set(Metadata.Field.DownloadPath, name);
			toStore.set(Metadata.Field.Name, name);
			toStore.set(Metadata.Field.Tool, "public");

			if (keywords != null) {
				toStore.set(Metadata.Field.Keywords, keywords.toArray(new String[0]));
			}

			if (mimeType != null) {
				toStore.set(Metadata.Field.MIMEType, mimeType);
			}

			if (submissionTime != null) {
				SimpleDateFormat sdf = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
				sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
				toStore.set(Metadata.Field.SubmissionDateTime, sdf.format(submissionTime));
			}

			// Get the user name from the request
			String userName = SecurityContextHolder.getContext().getAuthentication().getName();
			if (userName != null) {
				toStore.set(Metadata.Field.SubmissionUser, userName);
			}

			//
			// add mission package to enterprise sync
			//
			com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(
					com.bbn.marti.sync.EnterpriseSyncService.class);
			Metadata metadata = syncStore.insertResource(toStore, contents, groupVector);
			return metadata;

		} catch (Exception e) {
			logger.error("Exception in addToEnterpriseSync!", e);
			return null;
		}
	}

	private boolean hasUidConflict(MissionChange pendingChange, Set<MissionChange> changes) {
		String uid = pendingChange.getContentUid();
		for (MissionChange change : changes) {

			// ignore non uid changes
			if (change.getContentUid() == null) {
				continue;
			}

			// skip anything that's not our pending uid
			if (uid.compareToIgnoreCase(change.getContentUid()) != 0) {
				continue;
			}

			// return true if the pending change happened before the committed change
			if (pendingChange.getTimestamp().before(change.getTimestamp())) {
				return true;
			}
		}

		return false;
	}

	private boolean hasFileConflict(MissionChange pendingChange, Set<MissionChange> changes) {
		Resource resource = pendingChange.getTempResource();
		String filename = resource.getName();

		for (MissionChange change : changes) {

			findContentResource(change);

			// ignore non file changes
			if (change.getContentResource() == null) {
				continue;
			}

			// skip anything that's not our pending file
			if (filename.compareToIgnoreCase(change.getContentResource().getName()) != 0) {
				continue;
			}

			// return true if the pending change happened before the committed change
			if (pendingChange.getTimestamp().before(change.getTimestamp())) {
				return true;
			}
		}

		return false;
	}

	private boolean hasExternalDataConflict(MissionChange pendingChange, Set<MissionChange> changes) {

		for (MissionChange change : changes) {

			findExternalMissionData(change);
			ExternalMissionData externalMissionData = change.getExternalMissionData();

			// ignore non ExternalData changes
			if (externalMissionData== null) {
				continue;
			}

			// skip anything that's not our ExternalData
			if (pendingChange.getTempExternalData().getId().compareToIgnoreCase(externalMissionData.getId()) != 0) {
				continue;
			}

			// return true if the pending change happened before the committed change
			if (pendingChange.getTimestamp().before(change.getTimestamp())) {
				return true;
			}
		}

		return false;
	}

	public List<LogEntry> getLogEntriesForMission(Mission mission, Long secago, Date start, Date end) {

		// validate time interval filter
		Map.Entry<Date, Date> timeInterval = TimeUtils.validateTimeInterval(secago, start, end);

		start = timeInterval.getKey();
		end = timeInterval.getValue();

		if (start.compareTo(mission.getCreateTime()) < 0) {
			start = mission.getCreateTime();
		}

		int count = logEntryRepository.getLogCount(mission.getName(), start, end);

		if (count == 0) {
			return new ArrayList<LogEntry>();
		}

		return logEntryRepository.getMissionLog(mission.getName(), start, end);
	}

	public void deleteLogEntry(String id, String groupVector) {

		// get the set of missions this entry is associated with before deleting it
		LogEntry entry = null;

		try {
			entry = logEntryRepository.getOne(id);
		} catch (EntityNotFoundException efne) { }

		if (entry == null) {
			throw new NotFoundException("Log entry with id " + id + " not found");
		}

		// delete the log entry
		logEntryRepository.deleteById(id);

		for (String missionName : entry.getMissionNames()) {
			getMissionService().invalidateMissionCache(missionName);
		}

		// send change notifications
		for (String missionName : entry.getMissionNames()) {
			try {
				Mission mission = getMissionService().getMissionByNameCheckGroups(missionName, groupVector);
				subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), missionName, SubscriptionManagerLite.ChangeType.LOG, entry.getCreatorUid(), mission.getTool(), null);
			} catch (Exception e) {
				logger.warn("exception announcing mission change " + e.getMessage(), e);
			}
		}
	}

	public LogEntry addUpdateLogEntry(LogEntry entry, Date created, String groupVector) {

		if (entry.getMissionNames().isEmpty()) {
			throw new IllegalArgumentException("one or more mission names must be specified");
		}

		Map<String, String> toolMap = new HashMap<String, String>();
		Map<String, UUID> guidMap = new HashMap<String, UUID>();
		for (String missionName : entry.getMissionNames()) {
			Mission mission = getMissionService().getMissionByNameCheckGroups(trimName(missionName), groupVector);
			validateMission(mission, missionName);
			toolMap.put(missionName, mission.getTool());
			guidMap.put(missionName, mission.getGuidAsUUID());
		}

		entry.setServertime(new Date());
		entry.setCreated(created);

		// save the log entry
		entry = logEntryRepository.save(entry);

		for (String missionName : entry.getMissionNames()) {
			getMissionService().invalidateMissionCache(missionName);
		}

		for (String missionName : entry.getMissionNames()) {
			try {
				subscriptionManager.announceMissionChange(guidMap.get(missionName), missionName, SubscriptionManagerLite.ChangeType.LOG, entry.getCreatorUid(), toolMap.get(missionName),null);
			} catch (Exception e) {
				logger.warn("exception announcing mission change " + e.getMessage(), e);
			}
		}

		return entry;
	}

	private boolean hasLogEntryConflict(MissionChange pendingChange, Mission mission) {
		LogEntry pendingLogEntry = pendingChange.getTempLogEntry();

		for (LogEntry logEntry : getLogEntriesForMission(mission, null, null, null)) {

			// skip anything that's not our pending logEntry
			if (Strings.isNullOrEmpty(pendingLogEntry.getId()) ||
					pendingLogEntry.getId().compareTo(logEntry.getId()) != 0) {
				continue;
			}

			// return true if the pending change happened before the committed change was created
			if (pendingChange.getTimestamp().before(logEntry.getCreated())) {
				return true;
			}
		}

		return false;
	}

	private boolean hasConflicts(Mission mission,
								 Date missionCreateTime,
								 MissionChanges pendingChanges,
								 List<MissionChange> conflicts) {
		try {
			conflicts.clear();

			if (mission.getId() == null) {
				throw new IllegalArgumentException("null mission id in hasConflicts()");
			}


			// pull the complete change set for the mission
			Set<MissionChange> changes = missionChangeRepository.changesForMission(mission.getId(),
					TimeUtils.MIN_TS, TimeUtils.MAX_TS);

			// iterate over the pending changes
			for (MissionChange pendingChange : pendingChanges.getMissionChanges()) {

				if (pendingChange.getType() != MissionChangeType.ADD_CONTENT &&
						pendingChange.getType() != MissionChangeType.REMOVE_CONTENT) {
					logger.error("found illegal change type in changes.json : " + pendingChange.getType());
					continue;
				}

				// return true if the pending change happened before the mission create time
				if (pendingChange.getTimestamp().before(missionCreateTime)) {
					return true;
				}

				//
				// check each pending change for a conflict
				//

				if (!Strings.isNullOrEmpty(pendingChange.getContentUid())) {
					if (hasUidConflict(pendingChange, changes)) {
						conflicts.add(pendingChange);
					}
				} else if (pendingChange.getTempResource() != null) {
					if (hasFileConflict(pendingChange, changes)) {
						conflicts.add(pendingChange);
					}
				} else if (pendingChange.getTempExternalData() != null) {
					if (hasExternalDataConflict(pendingChange, changes)) {
						conflicts.add(pendingChange);
					}
				} else if (pendingChange.getTempLogEntry() != null) {
					if (hasLogEntryConflict(pendingChange, mission)) {
						conflicts.add(pendingChange);
					}
				}

			}

			return conflicts.size() > 0;

		} catch (Exception e) {
			logger.error("exception in hasConflicts! missionName : " + mission.getName());
			return true;
		}
	}

	private String trimByteOrderMark(String input) {
		byte[] bytes = input.getBytes();
		if (bytes.length > 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
			return new String(Arrays.copyOfRange(bytes, 3, bytes.length));
		} else {
			return input;
		}
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public boolean addMissionPackage(UUID missionGuid, byte[] missionPackage, String creatorUid,
									 NavigableSet<Group> groups, List<MissionChange> conflicts) {

		String groupVector = RemoteUtil.getInstance().bitVectorToString(
				RemoteUtil.getInstance().getBitVectorForGroups(groups));

		logger.debug("addMissionPackage missionGuid {}", missionGuid);
		
		Mission mission = getMissionService().getMissionByGuidCheckGroups(missionGuid, groupVector);
		validateMissionByGuid(mission);

		logger.debug("mission for add mission package {}", mission);
		
		try {

			// extract the contents of the mission packages?
			HashMap<String, byte[]> files = MissionPackage.extractMissionPackage(missionPackage);
			if (files == null) {
				return false;
			}

			// do we have an offline changes.json file?
			String changesJson = null;
			if (files.get("changes.json") != null) {
				changesJson = new String(files.get("changes.json"), "UTF-8");
			}

			//
			// process offline changes
			//
			if (changesJson != null) {

				// read in changes.json
				ObjectMapper mapper = new ObjectMapper();
				MissionChanges changes = mapper.readValue(changesJson, MissionChanges.class);

				// bail if there are any conflicts
				if (hasConflicts(mission, mission.getCreateTime(), changes, conflicts)) {
					logger.debug("detected conflicts in attempted merge on mission {}" + missionGuid);
					return false;
				}

				//
				// iterate across the offline changes
				//
				for (int ndx = 0; ndx < changes.getMissionChanges().size(); ndx++) {
					MissionChange pendingChange = changes.getMissionChanges().get(ndx);

					if (pendingChange.getType() != MissionChangeType.ADD_CONTENT &&
							pendingChange.getType() != MissionChangeType.REMOVE_CONTENT) {
						logger.error("found illegal change type in changes.json : " + pendingChange.getType());
						continue;
					}

					// process uid changes
					if (!Strings.isNullOrEmpty(pendingChange.getContentUid())) {

						if (pendingChange.getType() == MissionChangeType.ADD_CONTENT) {
							// pass the event to the submission service
							String path = ndx + "/" + pendingChange.getContentUid() + ".cot";
							String cot = new String(files.get(path));
							cot = trimByteOrderMark(cot);
							submission.submitMissionPackageCotAtTime(cot, missionGuid, pendingChange.getTimestamp(), groups, creatorUid);
						} else {
							deleteMissionContentAtTime(missionGuid, null, pendingChange.getContentUid(),
									creatorUid, groupVector, pendingChange.getTimestamp());
						}

						// process file changes
					} else if (pendingChange.getTempResource() != null) {

						if (pendingChange.getType() == MissionChangeType.ADD_CONTENT) {
							Resource resource = pendingChange.getTempResource();
							String hash = resource.getHash();
							String path = ndx + "/" + resource.getName();
							byte[] contents = files.get(path);

							if (contents != null) {
								// add the file to enterprise sync
								Metadata metadata = addToEnterpriseSync(contents, resource.getName(), resource.getMimeType(),
										resource.getKeywords(), groupVector, pendingChange.getTimestamp());
								hash = metadata.getHash();
							}

							// add the file to the mission
							MissionContent content = new MissionContent();
							content.getHashes().add(hash);
							addMissionContentAtTime(
									missionGuid, content, creatorUid, groupVector, pendingChange.getTimestamp(), null);
						} else {
							String hash = pendingChange.getTempResource().getHash();
							deleteMissionContentAtTime(missionGuid, hash, null,
									creatorUid, groupVector, pendingChange.getTimestamp());
						}

					} else if (pendingChange.getTempExternalData() != null) {

						if (pendingChange.getType() == MissionChangeType.ADD_CONTENT) {
							setExternalMissionDataAtTime(
									missionGuid, creatorUid, pendingChange.getTempExternalData(), groupVector, new Date());
						} else {
							deleteExternalMissionDataAtTime(missionGuid,
									pendingChange.getTempExternalData().getId(), pendingChange.getTempExternalData().getNotes(),
									creatorUid, groupVector, new Date());
						}

					} else if (pendingChange.getTempLogEntry() != null) {

						if (pendingChange.getType() == MissionChangeType.ADD_CONTENT) {
							LogEntry pendingLogEntry = pendingChange.getTempLogEntry();
							// TODO: check if this log needs to include the mission guid also
							pendingLogEntry.getMissionNames().add(mission.getName());
							addUpdateLogEntry(pendingLogEntry, pendingChange.getTimestamp(), groupVector);
						} else {
							deleteLogEntry(pendingChange.getTempLogEntry().getId(), groupVector);
						}

					}
				}

				//
				// add contents of mission package to mission at current time
				//
			} else {
				for (Map.Entry<String, byte[]> fileAdd : files.entrySet()) {
					String filename = fileAdd.getKey();

					// skip the mission package manifest
					if (filename.toLowerCase().contains("manifest.xml")) {
						continue;
					}

					byte[] contents = fileAdd.getValue();
					if (filename.endsWith(".cot")) {
						String cot = new String(contents);
						cot = trimByteOrderMark(cot);
						submission.submitMissionPackageCotAtTime(cot, missionGuid, new Date(), groups, creatorUid);
					} else {
						Date now = new Date();
						// add the file to enterprise sync
						Metadata metadata = addToEnterpriseSync(contents, filename, null,
								null, groupVector, now);

						// add the new checklist to the checklist mission
						MissionContent content = new MissionContent();
						content.getHashes().add(metadata.getHash());
						addMissionContentAtTime(missionGuid, content, creatorUid, groupVector, now, null);
					}
				}
			}

			return true;

		} catch (Exception e) {
			logger.error("exception adding mission package! " + missionGuid + " groupVector: " + groupVector, e);
			return false;
		} finally {
			missionCacheHelper.clearAllMissionAndCopsCache();
		}
	}

	@Override
	@Transactional
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public Mission deleteMissionContentAtTime(UUID missionGuid, String hash, String uid, String creatorUid, String groupVector, Date date) {

		if (Strings.isNullOrEmpty(hash) && Strings.isNullOrEmpty(uid)) {
			throw new IllegalArgumentException("either hash or uid parameter must be specified");
		}

		if (!(Strings.isNullOrEmpty(hash) || Strings.isNullOrEmpty(uid))) {
			throw new IllegalArgumentException("both hash and uid specified");
		}

		Mission mission = getMissionService().getMissionByGuidCheckGroups(missionGuid, groupVector);
		validateMissionByGuid(mission);

		try {

			if (hash != null) {
				// remove hash in database
				missionRepository.removeMissionResource(mission.getId(), hash);

				// remove from return object
				mission.getContents().remove(new Resource(hash));
			} else {
				// remove uid in database
				missionRepository.removeMissionUid(mission.getId(), uid);

				try {
					// remove uid in core services

					Set<String> uids = new HashSet<>();
					uids.add(uid);

					subscriptionManager.removeMissionContentUids(missionGuid, uids);
				} catch (Exception e) {
					logger.debug("exception removing mission content uid " + e.getMessage(), e);
				}

				// remove from return object
				mission.getUids().remove(uid);
			}

			// track the change by either hash or uid
			MissionChange change = new MissionChange(MissionChangeType.REMOVE_CONTENT, mission, hash, uid);
			change.setTimestamp(date);
			change.setCreatorUid(creatorUid);

			missionChangeRepository.save(change);

			MissionChanges changes = new MissionChanges();
			changes.add(change);

			hydrateMissionChange(change);

			try {
				subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), mission.getName(), creatorUid, mission.getTool(),
						commonUtil.toXml(changes));
			} catch (Exception e) {
				logger.debug("exception announcing mission change " + e.getMessage(), e);
			}

			return mission;
		} finally {
			// only empty the cache if something was actually added
			try {
				getMissionService().invalidateMissionCache(UUID.fromString(mission.getGuid()), mission.getName());
			} catch (Exception e) {
				logger.warn("exception clearing mission cache {} {}", mission.getName(), mission.getGuid(), e);
			}
		}
	}

	@Override
	@Transactional
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public Mission deleteMissionContent(UUID missionGuid, String hash, String uid, String creatorUid, String groupVector) {
		return deleteMissionContentAtTime(missionGuid, hash, uid, creatorUid, groupVector, new Date());
	}

	@Override
	@Transactional
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public byte[] archiveMission(UUID missionGuid, String groupVector, String serverName) {
		try {
			// This query considers the vector, so you can only delete missions with which you share common group membership.
			Mission mission = getMissionService().getMissionByGuidCheckGroups(missionGuid, groupVector);
			validateMissionByGuid(mission);

			MissionPackage mp = new MissionPackage(mission.getName() + "_" + mission.getGuid() + ".zip");
			mp.addParameter("uid", UUID.randomUUID().toString());
			mp.addParameter("name", mission.getName());
			mp.addParameter("mission_guid", mission.getGuid());
			mp.addParameter("password_hash", mission.getPasswordHash());
			mp.addParameter("creatorUid", mission.getCreatorUid());
			mp.addParameter("create_time", String.valueOf(mission.getCreateTime().getTime()));
			mp.addParameter("expiration", mission.getExpiration().toString());
			mp.addParameter("chatroom", mission.getChatRoom());
			mp.addParameter("description", mission.getDescription());
			mp.addParameter("tool", mission.getTool());
			mp.addParameter("onReceiveImport", "true");
			mp.addParameter("onReceiveDelete", "false");
			mp.addParameter("mission_name", mission.getName());
			mp.addParameter("mission_label", mission.getName());
			mp.addParameter("mission_uid", serverName + "-8443-ssl-" + mission.getName());
			mp.addParameter("mission_server", serverName + ":8443:ssl");

			mp.addDirectory("cot/");
			mp.addDirectory("contents/");

			// archive the mission groups
			Collection<Group> allOutGroups = groupManager.getAllGroups();
			NavigableSet<Group> outGroups = RemoteUtil.getInstance().getGroupsForBitVectorString(mission.getGroupVector(), allOutGroups);
			outGroups.stream().forEach(group-> mp.addGroup(group.getName()));
			// archive the mission role + permissions
			if (mission.getDefaultRole() != null) {
				mp.addRoleName(mission.getDefaultRole().getRole().name());
				mission.getDefaultRole().getMissionPermissions().forEach(permission->mp.addPermission(permission.getPermission().name()));
			}

			//
			// iterate over cot events
			//
			for (String uid : mission.getUids()) {
				try {
					CotElement cot = getLatestCotForUid(uid, groupVector);
					if (cot == null) {
						logger.error("Unable to add cot to mission archive: " + uid);
						continue;
					}

					mp.addCotFile("cot/" + uid + ".cot", cot.toCotXml().getBytes(), uid);
				} catch (NotFoundException nfe) {
					logger.error("Unable to add cot to mission archive: " + uid, nfe);
					continue;
				}
			}

			int ndx = 0;
			for (Resource resource : mission.getContents()) {
				byte[] content = syncStore.getContentByHash(resource.getHash(), groupVector);

				// this is a hack because for some reason the resources from mission.getContents()
				// don't return keywords
				try {
					Resource resourceWithKeywords = ResourceUtils.fetchResourceByHash(resource.getHash());
					resource.setKeywords(resourceWithKeywords.getKeywords());
				} catch (Exception e) {
					logger.info("could not fetch keywords for " + resource.getHash());
				}

				if (content == null) {
					logger.error("Unable to add content to mission archive: " + resource.getHash());
					continue;
				}

				String filename = resource.getHash();

				if (resource.getName() != null) {
					filename = ndx++ + "_" + resource.getName();
				}


				mp.addContentFile("contents/" + filename, content, resourceToContentType(resource));
			}

			return mp.save();
		} catch (Exception e) {
			String msg = "Exception in archiveMission!";
			logger.error(msg, e);
			throw new TakException(msg, e);
		}
	}

	private ContentType resourceToContentType(Resource resource) {
		ContentType contentType = new ContentType();

		if (resource.getAltitude() != null) {
			contentType.setAltitude(resource.getAltitude());
		}
		if (resource.getCreatorUid() != null) {
			contentType.setCreatorUid(resource.getCreatorUid());
		}
		if (resource.getKeywords() != null) {
			contentType.setKeywords(String.join(",", resource.getKeywords()));
		}
		if (resource.getLatitude() != null) {
			contentType.setLatitude(resource.getLatitude());
		}
		if (resource.getLongitude() != null) {
			contentType.setLongitude(resource.getLongitude());
		}
		if (resource.getMimeType() != null) {
			contentType.setMimeType(resource.getMimeType());
		}
		if (resource.getName() != null) {
			contentType.setName(resource.getName());
		}
		if (resource.getSize() != null) {
			contentType.setSize(resource.getSize());
		}
		if (resource.getSubmitter() != null) {
			contentType.setSubmitter(resource.getSubmitter());
		}
		if (resource.getTool() != null) {
			contentType.setTool(resource.getTool());
		}
		if (resource.getUid() != null) {
			contentType.setUid(resource.getUid());
		}
		if (resource.getFilename() != null) {
			contentType.setFilename(resource.getFilename());
		}
		if (resource.getName() != null) {
			contentType.setName(resource.getName());
		}
		if (resource.getSubmissionTime() != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
			dateFormat.format(resource.getSubmissionTime());
			contentType.setSubmissionTime(dateFormat.format(resource.getSubmissionTime()));
		}
		return contentType;
	}

	@Override
	@Transactional
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public String addMissionArchiveToEsync(String archiveName, byte[] archiveBytes, String groupVector, boolean archivedWhenDeleting) {
		try {
			// build up the metadata for adding to enterprise sync
			Metadata toStore = new Metadata();
			if (archivedWhenDeleting) {
				toStore.set(Metadata.Field.Keywords, "ARCHIVED_MISSION");
			}
			toStore.set(Metadata.Field.DownloadPath, archiveName + ".zip");
			toStore.set(Metadata.Field.Name, archiveName);
			toStore.set(Metadata.Field.MIMEType, "application/zip");
			toStore.set(Metadata.Field.UID, new String[]{UUID.randomUUID().toString()});

			// add mission package to enterprise sync

			toStore = syncStore.insertResource(toStore, archiveBytes, groupVector);
			return toStore.getHash();
		} catch (Exception e) {
			throw new TakException("Exception in addMissionArchiveToEsync!", e);
		}
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public Mission deleteMission(String name, String creatorUid, String groupVector, boolean deepDelete) {

		// This query considers the vector, so you can only delete missions with which you share common group membership.
		Mission mission = getMissionService().getMissionByNameCheckGroups(trimName(name), groupVector);
		validateMission(mission, name);

		try {

			try {
				// remove uid in core services
				HashSet<String> uids = new HashSet<String>(mission.getUids());
				subscriptionManager.removeMissionContentUids(UUID.fromString(mission.getGuid()), uids);
			} catch (Exception e) {
				logger.error("exception removing mission content uid " + e.getMessage(), e);
			}

			try {
				// un-subscribe everyone from this mission
				subscriptionManager.removeAllMissionSubscriptions(UUID.fromString(mission.getGuid()));
			} catch (Exception e) {
				logger.error("exception removing mission subscriptions" + e.getMessage(), e);
			}

			if (deepDelete) {
				for (Resource resource : mission.getContents()) {
					try {
						syncStore.delete(resource.getHash(), groupVector);
					} catch (Exception e) {
						logger.error("exception deleting file with hash : " + resource.getHash());
					}
				}

				deleteAllCotForUids(new LinkedList<String>(mission.getUids()), groupVector);
			}

			try {
				if (mission.getParent() != null) {
					getMissionService().invalidateMissionCache(missionRepository.getParentName(name));
				}
			} catch (Exception e) {
				logger.error("exception clearing parent mission cache " + e.getMessage(), e);
			}

			try {
				missionRepository.deleteMission(mission.getId());
			} catch (JpaSystemException e) { 
				logger.debug("JpaSystemException deleting mission", e); // meaningless "multiple results sets" exception due to multiple sql statements
			} 

			MissionChange change = new MissionChange(MissionChangeType.DELETE_MISSION, mission);

			change.setCreatorUid(creatorUid);

			missionChangeRepository.save(change);

			try {
				subscriptionManager.broadcastMissionAnnouncement(UUID.fromString(mission.getGuid()) ,name, mission.getGroupVector(), creatorUid,
						SubscriptionManagerLite.ChangeType.MISSION_DELETE, mission.getTool());
			} catch (Exception e) {
				logger.error("exception announcing mission change " + e.getMessage(), e);
			}

			return mission;

		} finally {
			missionCacheHelper.clearAllMissionAndCopsCache();
		}
	}
	
	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public Mission deleteMissionByGuid(UUID missionGuid, String creatorUid, String groupVector, boolean deepDelete) {
		
		if (missionGuid == null) {
			throw new IllegalArgumentException("null mission guid in deleteMissionByGuid");
		}
 
		// This query considers the vector, so you can only delete missions with which you share common group membership.
		Mission mission = getMissionService().getMissionByGuidCheckGroups(missionGuid, groupVector);
		validateMissionByGuid(mission);

		try {

			try {
				// remove uid in core services
				HashSet<String> uids = new HashSet<String>(mission.getUids());
				subscriptionManager.removeMissionContentUids(UUID.fromString(mission.getGuid()), uids);
			} catch (Exception e) {
				logger.error("exception removing mission content uid " + e.getMessage(), e);
			}

			try {
				// un-subscribe everyone from this mission
				subscriptionManager.removeAllMissionSubscriptions(UUID.fromString(mission.getGuid()));
			} catch (Exception e) {
				logger.error("exception removing mission subscriptions" + e.getMessage(), e);
			}

			if (deepDelete) {
				for (Resource resource : mission.getContents()) {
					try {
						syncStore.delete(resource.getHash(), groupVector);
					} catch (Exception e) {
						logger.error("exception deleting file with hash : " + resource.getHash());
					}
				}

				deleteAllCotForUids(new LinkedList<String>(mission.getUids()), groupVector);
			}

			// TODO: address parent mission cases for guid
			try {
				if (mission.getParent() != null) {
					getMissionService().invalidateMissionCache(missionRepository.getParentName(mission.getName()));
				}
			} catch (Exception e) {
				logger.error("exception clearing parent mission cache " + e.getMessage(), e);
			}

			try {
				missionRepository.deleteMission(mission.getId());
			} catch (JpaSystemException e) { } // meaningless "multiple results sets" exception due to multiple sql statements

			MissionChange change = new MissionChange(MissionChangeType.DELETE_MISSION, mission);

			change.setCreatorUid(creatorUid);

			missionChangeRepository.save(change);

			try {
				// TODO: correctly announce change for guid case
				subscriptionManager.broadcastMissionAnnouncement(UUID.fromString(mission.getGuid()), mission.getName(), mission.getGroupVector(), creatorUid,
						SubscriptionManagerLite.ChangeType.MISSION_DELETE, mission.getTool());
			} catch (Exception e) {
				logger.error("exception announcing mission change " + e.getMessage(), e);
			}

			return mission;

		} catch (Exception e) {
			logger.error("exception deleting and archiving mission {} {} ", mission.getName(), mission.getGuid());
			throw e;
		} finally {
			missionCacheHelper.clearAllMissionAndCopsCache();
		}
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public Mission createMission(String name, String creatorUid, String groupVector, String description, String chatRoom,
								 String baseLayer, String bbox, String path, String classification, String tool, String passwordHash,
								 MissionRole defaultRole, Long expiration, String boundingPolygon, Boolean inviteOnly, UUID guid) {

		if (logger.isDebugEnabled()) {
			logger.debug("create mission " + name + " " + tool + " " + creatorUid);
		}

		Mission mission = new Mission();
		mission.setGuid(guid.toString());

		MissionChange createChange = new MissionChange(MissionChangeType.CREATE_MISSION, null);

		createChange.setCreatorUid(creatorUid);

		mission.setCreatorUid(creatorUid);

		mission.setCreateTime(new Date());

		mission.setTool(tool);

		mission.setPasswordHash(passwordHash);

		mission.setDefaultRole(defaultRole);

		mission.setExpiration(expiration);

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		mission.setGroupVector(groupVector);

		CoreConfig coreConfig = CoreConfigFacade.getInstance();

		if (Strings.isNullOrEmpty(classification) &&
				coreConfig.getRemoteConfiguration().getVbm() != null &&
				coreConfig.getRemoteConfiguration().getVbm().isEnabled() &&
				coreConfig.getRemoteConfiguration().getVbm().isIsmStrictEnforcing()
				&& !Strings.isNullOrEmpty(coreConfig.getRemoteConfiguration().getVbm().getNetworkClassification())) {
			classification = coreConfig.getRemoteConfiguration().getVbm().getNetworkClassification();
		}

		// Idempotent mission creation - the unique constraint on mission name in the database handles this.
		try {
			Long id = null;
			Long expirationValue = null; // jpa query cannot map a null value to Long or Integer parameters

			if (expiration == null) {
				expirationValue = -1L;
			} else {
				expirationValue = expiration;
			}
			if (defaultRole == null) {
				id = missionRepository.create(new Date(), trimName(name), creatorUid, groupVector, description, chatRoom, baseLayer, bbox, path, classification, tool, passwordHash, expirationValue, boundingPolygon, inviteOnly, guid);
			} else {
				id = missionRepository.create(new Date(), trimName(name), creatorUid, groupVector, description, chatRoom, baseLayer, bbox, path, classification, tool, passwordHash, defaultRole.getId(), expirationValue, boundingPolygon, inviteOnly, guid);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("saved mission " + name + " in missionRepository");
			}

			mission.setId(id);
			mission = missionRepository.getOne(id);

			createChange.setMission(mission);
			createChange.setMissionName(trimName(name));

			// record the create mission action in the changelog
			missionChangeRepository.save(createChange);


			if (logger.isDebugEnabled()) {
				logger.debug("saved createChange in missionRepository");
			}

			// in case someone subscribed to it before it was created / or concurrently
			try {
				subscriptionManager.broadcastMissionAnnouncement(UUID.fromString(mission.getGuid()) ,trimName(name), groupVector, creatorUid,
						SubscriptionManagerLite.ChangeType.MISSION_CREATE, tool);
			} catch (Exception e) {
				logger.warn("exception announcing mission change " + e.getMessage(), e);
			}



		} catch (DataIntegrityViolationException e) { // safely ignored in the case of mission that already exists
			if (logger.isDebugEnabled()) {
				logger.debug("mission " + name + " already exists", e);
			}
		} catch (Exception e) {
			throw new TakException("exception saving mission", e);
		} finally {
			missionCacheHelper.clearAllMissionAndCopsCache();
		}

		return mission;
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public Mission createMission(String name, String creatorUid, String groupVector, String description, String chatRoom,
								 String baseLayer, String bbox, String path, String classification, String tool, String passwordHash,
								 MissionRole defaultRole, Long expiration, String boundingPolygon, Boolean inviteOnly) {

		Mission result = createMission(name, creatorUid, groupVector, description, chatRoom, baseLayer, bbox, path, classification, tool,
				passwordHash, defaultRole, expiration, boundingPolygon, inviteOnly,  UUID.randomUUID());

		// clear the all missions cache in addition to the per-mission cache (see CacheEvict on this method)
		missionCacheHelper.clearAllMissionAndCopsCache();
		
		return result;
	}

	@Override
	public String trimName(@NotNull String name) {

		if (Strings.isNullOrEmpty(name)) {
			throw new IllegalArgumentException("empty name");
		}

		name = name.trim();

		return name;
	}

	// checks if the mission was previously deleted
	@Override
	public void validateMission(Mission mission, String missionName) {

		logger.debug("validateMission {} {} ", mission, missionName);

		if (mission == null) {
			// if a mission was deleted, respond with a 410
			if (isDeleted(missionName)) {
				throw new MissionDeletedException("Mission named '" + missionName + "' was deleted");
				// if a mission doesn't exist, respond with a 404
			} else {

				String msg = "Mission named '" + missionName + "' not found - not deleted";

				if (logger.isDebugEnabled()) {
					logger.debug(msg);
				}

				throw new NotFoundException(msg);
			}
		}
	}

	// checks if the mission was previously deleted
	@Override
	public void validateMissionByGuid(Mission mission) {

		logger.debug("validateMissionByGuid {} {} ", mission, mission.getGuid(), mission.getName());
		
		if (mission == null) {
			// if a mission was deleted, respond with a 410
			if (isDeletedByGuid(UUID.fromString(mission.getGuid()))) {
				throw new MissionDeletedException("Mission  '" + mission.getGuid() + "' was deleted");
				// if a mission doesn't exist, respond with a 404
			} else {

				String msg = "Mission '" + mission.getGuid() + "' not found - not deleted";

				if (logger.isDebugEnabled()) {
					logger.debug(msg);
				}

				throw new NotFoundException(msg);
			}
		}
	}

	@Override
	public boolean validateAccess(Mission mission, HttpServletRequest request) {
		if (!CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().isEnabled()) {
			return true;
		}

		try {
			return classificationService.canAccess(
					commonUtil.getUserClassificationFromRequest(request),
					mission.getClassification());
		} catch (RemoteException e) {
			logger.error("exception in validateAccess", e);
			return false;
		}
	}

	@Override
	public List<Mission> validateAccess(List<Mission> missions, HttpServletRequest request) {
		if (!CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().isEnabled()) {
			return missions;
		}

		return missions.stream()
				.filter(mission -> validateAccess(mission, request))
				.collect(Collectors.toList());
	}

	@Override
	public boolean isDeleted(String missionName) {

		if (logger.isDebugEnabled()) {
			logger.debug("checking isDeleted " + missionName);
		}

		MissionChangeType changeType = missionChangeRepository.getLatestChangeTypeForMissionName(missionName);

		if (logger.isDebugEnabled()) {
			logger.debug("last change type for mission {} : {} ", missionName, changeType);
		}

		// mission doesn't exist, and never did
		if (changeType == null) {
			return false;
		}

		// mission was deleted
		if (changeType.equals(MissionChangeType.DELETE_MISSION)) {
			return true;
		}

		// mission currently exists
		return false;
	}

	@Override
	public boolean isDeletedByGuid(UUID guid) {

		if (logger.isDebugEnabled()) {
			logger.debug("checking isDeleted {}", guid);
		}

		MissionChangeType changeType = missionChangeRepository.getLatestChangeTypeForMissionGuid(guid);

		if (logger.isDebugEnabled()) {
			logger.debug("last change type for mission {} : {} ", guid, changeType);
		}

		// mission doesn't exist, and never did
		if (changeType == null) {
			return false;
		}

		// mission was deleted
		if (changeType.equals(MissionChangeType.DELETE_MISSION)) {
			return true;
		}

		// mission currently exists
		return false;
	}


	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, key="{#root.methodName, #root.args[0]}")
	private Set<MissionChange> getAllSquashedChangesForMission(Long missionId) {
		return missionChangeRepository.squashedChangesForMission(missionId, TimeUtils.MIN_TS, TimeUtils.MAX_TS);
	}

	@Override
	public Mission getMissionNoContent(String missionName, String groupVector) {

		Mission mission = missionCacheHelper.getMission(missionName, false, false);

		if (mission != null && !remoteUtil.isGroupVectorAllowed(groupVector, mission.getGroupVector())) {
			mission = null;
		}

		getMissionService().validateMission(mission, missionName);

		return mission;
	}

	@Override
	public Mission getMissionNoContentByGuid(UUID missionGuid, String groupVector) {

		Mission mission = missionCacheHelper.getMissionByGuid(missionGuid, false, false);

		if (mission != null && !remoteUtil.isGroupVectorAllowed(groupVector, mission.getGroupVector())) {
			mission = null;
		}

		getMissionService().validateMissionByGuid(mission);

		return mission;
	}

	@Override
	public Mission getMission(String missionName, boolean hydrateDetails) {

		return missionCacheHelper.getMission(trimName(missionName), hydrateDetails, false);
	}

	@Override
	public Mission getMissionByGuid(UUID missionGuid, boolean hydrateDetails) {

		return missionCacheHelper.getMissionByGuid(missionGuid, hydrateDetails, false);
	}

	@Override
	public Mission getMission(String missionName, String groupVector) {

		Mission mission = getMissionService().getMission(missionName, true);
		
		if (mission != null && !remoteUtil.isGroupVectorAllowed(groupVector, mission.getGroupVector())) {
			mission = null;
		}

		// will throw MissionDeletedException if deleted, NotFoundException if not found
		getMissionService().validateMission(mission, missionName);

		return mission;
	}

	@Override
	public Mission getMissionByGuid(UUID missionGuid, String groupVector) {

		Mission mission = getMissionService().getMissionByGuid(missionGuid, true);

		if (mission != null && !remoteUtil.isGroupVectorAllowed(groupVector, mission.getGroupVector())) {
			mission = null;
		}
		
		logger.debug("return mission in getMissionByGuid {} {}", missionGuid, mission);

		// will throw MissionDeletedException if deleted, NotFoundException if not found
		getMissionService().validateMissionByGuid(mission);

		return mission;
	}

	@Override
	public Mission getMissionNoDetails(String missionName, String groupVector) {

		Mission mission = missionCacheHelper.getMission(missionName, false, false);

		if (mission != null && !remoteUtil.isGroupVectorAllowed(groupVector, mission.getGroupVector())) {
			mission = null;
		}

		getMissionService().validateMission(mission, missionName);

		return mission;
	}
	
	@Override
	public Mission getMissionByName(String missionName, boolean hydrateDetails) {

		return missionCacheHelper.getMission(missionName, hydrateDetails, false);
	}

	@Override
	public Mission getMissionByNameCheckGroups(String missionName, boolean hydrateDetails, String groupVector) {

		Mission mission = missionCacheHelper.getMission(missionName, hydrateDetails, false);

		if (mission != null) {
			if (!remoteUtil.isGroupVectorAllowed(groupVector, mission.getGroupVector())) {
				throw new AccessDeniedException("access denied for mission " + mission.getName());
			}
		}

		return mission;
	}

	@Override
	public Mission getMissionByNameCheckGroups(String missionName, String groupVector) {
		return getMissionByNameCheckGroups(missionName, false, groupVector);
	}

	@Override
	//@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public Mission getMissionByGuidCheckGroups(UUID missionGuid, String groupVector) {

		Mission mission = missionCacheHelper.getMissionByGuid(missionGuid, false, false);

		if (mission != null) {
			if (!remoteUtil.isGroupVectorAllowed(groupVector, mission.getGroupVector())) {
				throw new AccessDeniedException("access denied for mission " + mission.getName());
			}
		}

		return mission;
	}


	@Override
	public void setParent(UUID childMissionGuid, UUID parentMissionGuid, String groupVector) {
		String currentParentGuid = missionRepository.getParentMissionGuid(childMissionGuid.toString());
		if (currentParentGuid == null || !currentParentGuid.equals(parentMissionGuid)) {
			missionRepository.setParent(childMissionGuid.toString(), parentMissionGuid.toString(), groupVector);
			getMissionService().invalidateMissionCache(childMissionGuid);
			getMissionService().invalidateMissionCache(parentMissionGuid);
		}
	}

	@Override
	public void clearParent(UUID childMissionGuid, String groupVector) {
		Mission child = getMissionService().getMissionByGuid(childMissionGuid, groupVector);
		if (child != null && child.getParent() != null) {
			String parentGuid = missionRepository.getParentGuid(childMissionGuid.toString());
			getMissionService().invalidateMissionCache(UUID.fromString(parentGuid));
		}

		missionRepository.clearParentByGuid(childMissionGuid.toString(), groupVector);
		getMissionService().invalidateMissionCache(childMissionGuid);
	}
	
	@Override
	public List<Mission> getAllMissions(boolean passwordProtected, boolean defaultRole, String tool, NavigableSet<Group> requestGroups)  {

		String requestGroupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(requestGroups));
		
		// get the cached list of missions guids
		List<String> missionsGuids = getMissionService().getAllMissionsGuids(passwordProtected, defaultRole, tool);
		
		// get all individually cached missions
		Set<Mission> missions = new ConcurrentSkipListSet<>();
		for (String missionGuid: missionsGuids) {
			Mission mission = null;

			int retry = 0;
			while (true) {
				try {
					mission = missionCacheHelper.getMissionByGuid(UUID.fromString(missionGuid), false, false);
					break;
				} catch (Exception e) {
					logger.error("exception calling getMissionByGuid", e);
					if (++retry == 3) {
						break;
					}
				}
			}

			if (mission != null) {
				missions.add(mission);
			} else {
				logger.error("null mission for {}", missionGuid);
			}
		}
				
		// filter out missions the user is not permitted to access
		List<Mission> missionsFiltered = new ArrayList<Mission>();
		for (Mission mission : missions) {
			try {
				if (remoteUtil.isGroupVectorAllowed(requestGroupVector, mission.getGroupVector())) {
					// clear contents of password protected missions
					if (mission.isPasswordProtected()) {
						mission.clear();
					}
					mission.setGroups(RemoteUtil.getInstance().getGroupNamesForBitVectorString(mission.getGroupVector(), requestGroups));
					missionsFiltered.add(mission);
				}
			} catch (Exception e) {
				logger.error("exception filtering missions", e);
			}
		}

		return missionsFiltered;
	}
	
	@Override
	@Cacheable(cacheResolver = AllMissionCacheResolver.ALL_MISSION_CACHE_RESOLVER, keyGenerator = "allMissionsCacheKeyGenerator", sync = true)
	public List<String> getAllMissionsGuids(boolean passwordProtected, boolean defaultRole, String tool)  {
		if (logger.isDebugEnabled()) {
			logger.debug("mission service getAllMissionsCached cache miss");
		}

		List<Mission> missions;

		if (tool == null) {
			missions = missionRepository.getAllMissionsNoGroupCheck(passwordProtected, defaultRole);
		} else {
			missions = missionRepository.getAllMissionsByToolNoGroupCheck(passwordProtected, defaultRole, tool);
		}
		
		return missions.stream()
				.filter(mission -> mission != null)
				.map(mission -> mission.getGuid())
				.toList();
	}

	@Override
	public List<Mission> getMissionsFiltered(boolean passwordProtected, boolean defaultRole, String tool, NavigableSet<Group> groups,
											 int limit, int offset, String sort, Boolean ascending, String nameFilter, String uidFilter)  {
		
		logger.debug("getMissionsFiltered");

		String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));

		List<Mission> missions;
		int page = offset / limit;
		Pageable pr;
		if (sort.isBlank()) {
			pr = PageRequest.of(page, limit);
		} else {
			if (ascending) {
				pr = PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, sort));
			} else {
				pr = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, sort));
			}
		}

		if (tool != null) {
			logger.debug("getAllMissionsByToolPage");
			missions = missionRepository.getAllMissionsByToolPage(passwordProtected, defaultRole, tool, groupVector, pr).getContent();
		} else if (!nameFilter.isBlank()) {
			logger.debug("getAllMissionsByNamePage");
			missions = missionRepository.getAllMissionsByNamePage(passwordProtected, defaultRole, groupVector, nameFilter, pr).getContent();
		} else if (!uidFilter.isBlank()) {
			if (sort.isBlank()) {
				logger.debug("getAllMissionsByUidPage");
				missions = missionRepository.getAllMissionsByUidPage(passwordProtected, defaultRole, uidFilter, groupVector, pr);
			} else {
				logger.debug("getMissionsByUidWithSort");
				missions = getMissionsByUidWithSort(passwordProtected, defaultRole, limit, offset, sort, ascending, uidFilter, groupVector);
			}
		} else {
			logger.debug("getAllMissionsPage");
			missions = missionRepository.getAllMissionsPage(passwordProtected, defaultRole, groupVector, pr).getContent();
		}
		
		logger.debug("got {} missions. About to hydrate each.", missions.size());

		for (Mission mission : missions) {
			if (mission.isPasswordProtected()) {
				mission.clear();
			} else if (!uidFilter.isBlank() && !sort.isBlank()){
				hydrate(mission, true);
			} else {
				hydrate(mission, false);
			}

			mission.setGroups(RemoteUtil.getInstance().getGroupNamesForBitVectorString(
					mission.getGroupVector(), groups));
			
			hydrateFeedNameForMission(mission);
			hydrateMissionChangesForMission(mission);
		}

		return missions;
	}

	private List<Mission> getMissionsByUidWithSort(boolean passwordProtected, boolean defaultRole, int limit, int offset, String sort, Boolean ascending, String uidFilter, String groupVector) {
		String sql = "select id, create_time, last_edited, name, creatoruid, groups, description, chatroom, base_layer, bbox, path, classification, tool, password_hash, expiration, bounding_polygon, invite_only, guid "
				+ "from mission inner join mission_uid on mission.id = mission_uid.mission_id where invite_only = false and "
				+ "((:passwordProtected = false and password_hash is null) or :passwordProtected = true) "
				+ "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) "
				+ "and uid like CONCAT('%', :uid, '%') AND " + RemoteUtil.GROUP_CLAUSE + "order by " + sort;
		if (ascending) {
			sql = sql + " asc ";
		} else {
			sql = sql + " desc ";
		}
		sql = sql + "offset :offset limit :limit;";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("passwordProtected", passwordProtected);
		namedParameters.addValue("defaultRole", defaultRole);
		namedParameters.addValue("uid", uidFilter);
		namedParameters.addValue("offset", offset);
		namedParameters.addValue("limit", limit);
		namedParameters.addValue("groupVector", groupVector);
		return new NamedParameterJdbcTemplate(dataSource).query(sql,
				namedParameters,
				new ResultSetExtractor<List<Mission>>() {
					@Override
					public List<Mission> extractData(ResultSet results) throws SQLException {
						if (results == null) {
							throw new IllegalStateException("null result set");
						}

				List<Mission> missions = new ArrayList<Mission>();
				
				while (results.next()) {
					Mission nextMission = new Mission();
					nextMission.setId(results.getLong(1));
					nextMission.setCreateTime(results.getDate(2));
					nextMission.setLastEdited(results.getDate(3));
					nextMission.setName(results.getString(4));
					nextMission.setCreatorUid(results.getString(5));
					nextMission.setGroupVector(results.getString(6));
					nextMission.setDescription(results.getString(7));
					nextMission.setChatRoom(results.getString(8));
					nextMission.setBaseLayer(results.getString(9));
					nextMission.setBbox(results.getString(10));
					nextMission.setPath(results.getString(11));
					nextMission.setClassification(results.getString(12));
					nextMission.setTool(results.getString(13));
					nextMission.setPasswordHash(results.getString(14));
					nextMission.setExpiration(results.getLong(15));
					nextMission.setBoundingPolygon(results.getString(16));
					nextMission.setInviteOnly(results.getBoolean(17));
					nextMission.setGuid(results.getString(18));
					nextMission.setUids(new HashSet<String>(missionRepository.getMissionUids(nextMission.getName())));
					nextMission.setKeywords(new HashSet<String>(missionRepository.getMissionKeywords(nextMission.getName())));
					nextMission.setContents(new HashSet<Resource>(fileManagerService.getResourcesByMission(nextMission.getName(), 0, 0, "", false, "", groupVector)));
					missions.add(nextMission);
				}

			return missions;

            }
		});
	}


	@Override
	@Cacheable(value = Constants.INVITE_ONLY_MISSION_CACHE, keyGenerator = "inviteOnlyMissionsCacheKeyGenerator", sync = true)
	public List<Mission> getInviteOnlyMissions(String userName, String tool, NavigableSet<Group> groups) {

		String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));

		List<Mission> missions = missionRepository.getInviteOnlyMissions(userName, tool, groupVector);

		for (Mission mission : missions) {

			hydrate(mission, false);

			mission.setGroups(RemoteUtil.getInstance().getGroupNamesForBitVectorString(
					mission.getGroupVector(), groups));
			
			UnproxyHelper.unproxyMission(mission);
		}

		return missions;
	}

	@Override
	@Cacheable(cacheResolver = AllCopMissionCacheResolver.ALL_COP_MISSION_CACHE_RESOLVER, keyGenerator = "allCopsMissionsCacheKeyGenerator", sync = true)
	public List<Mission> getAllCopsMissions(String tool, NavigableSet<Group> groups, String path, Integer offset, Integer size)  {

		if (logger.isDebugEnabled()) {
			logger.debug("mission service getAllMissions cache miss");
		}

		String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));

		List<Mission> missions;

		if (offset != null && size != null) {
			missions = missionRepository.getAllCopMissionsWithPaging(groupVector, path, tool, offset, size);
		} else {
			missions = missionRepository.getAllCopMissions(groupVector, path, tool);
		}

		for (Mission mission : missions) {
			if (mission.isPasswordProtected()) {
				mission.clear();
			}

			mission.setGroups(RemoteUtil.getInstance().getGroupNamesForBitVectorString(
					mission.getGroupVector(), groups));
			
			UnproxyHelper.unproxyMission(mission);
		}

		return missions;
	}
	public Long getMissionCount(String tool) {

		if (tool == null) {
			return 0L;
		}

		return missionRepository.getMissionCountByTool(tool);
	}

	@Override
	public boolean exists(String missionName, String groupVector) {
		Mission mission = getMissionService().getMissionByNameCheckGroups(missionName, groupVector);

		if (mission == null) {
			return false;
		}

		return true;

	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public void invalidateMissionCache(String missionName) {
		logger.debug("invalidateMissionCache name only {}", missionName);
	}
	
	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public void invalidateMissionCache(UUID missionGuid, String missionName) {
		logger.debug("invalidateMissionCache guid and name {} {}", missionGuid, missionName);
	}
	
	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public void invalidateMissionCache(UUID missionGuid) {
		logger.debug("invalidateMissionCache guid only {}", missionGuid);
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, sync = true)
	public Set<MissionChange> getMissionChanges(String missionName, String groupVector,
												Long secago, Date start, Date end, boolean squashed) {

		if (logger.isDebugEnabled()) {
			logger.debug("MissionServiceDefaultImpl get mission changes for: " + missionName);
		}

		try {
			if (Strings.isNullOrEmpty(missionName)) {
				throw new IllegalArgumentException("empty 'name' path parameter");
			}

			if (logger.isDebugEnabled()) {
				logger.debug("getting mission changes for mission " + missionName);
			}

			Mission mission = getMissionService().getMissionByNameCheckGroups(trimName(missionName), groupVector);
			validateMission(mission, missionName);

			// validate time interval
			Map.Entry<Date, Date> timeInterval = TimeUtils.validateTimeInterval(secago, start, end);

			start = timeInterval.getKey();
			end = timeInterval.getValue();

			if (mission.getId() == null) {
				throw new IllegalArgumentException("null mission id in getMissionChanges()");
			}

			Set<MissionChange> changes;
			if (squashed) {

				if (logger.isDebugEnabled()) {
					logger.debug("squashed changes query: " + MissionChangeRepository.MISSION_CHANGES);
				}

				changes = missionChangeRepository.squashedChangesForMission(mission.getId(), start, end);
			} else {

				if (logger.isDebugEnabled()) {
					logger.debug("changes query: " + MissionChangeRepository.MISSION_CHANGES_FULL_HISTORY);
				}

				changes = missionChangeRepository.changesForMission(mission.getId(), start, end);
			}
			
			for (MissionChange missionChange: changes) {
				hydrateMissionChange(missionChange);
			}

			return changes;
		} catch (Exception e) {
			logger.error("exception in getMissionChanges", e);
			return null;
		}
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER)
	public Set<MissionChange> getMissionChangesByGuid(UUID missionGuid, String groupVector, Long secago, Date start, Date end, boolean squashed) {

		logger.debug("MissionServiceDefaultImpl get mission changes for {} ", missionGuid);

		try {
			if (missionGuid == null) {
				throw new IllegalArgumentException("empty mission guid parameter");
			}

			Mission mission = getMissionService().getMissionByGuidCheckGroups(missionGuid, groupVector);
			validateMissionByGuid(mission);

			// validate time interval
			Map.Entry<Date, Date> timeInterval = TimeUtils.validateTimeInterval(secago, start, end);

			start = timeInterval.getKey();
			end = timeInterval.getValue();

			if (mission.getId() == null) {
				throw new IllegalArgumentException("null mission id in getMissionChangesByGuid()");
			}

			Set<MissionChange> changes;
			if (squashed) {

				if (logger.isDebugEnabled()) {
					logger.debug("squashed changes query: " + MissionChangeRepository.MISSION_CHANGES);
				}

				changes = missionChangeRepository.squashedChangesForMission(mission.getId(), start, end);
			} else {

				if (logger.isDebugEnabled()) {
					logger.debug("changes query: " + MissionChangeRepository.MISSION_CHANGES_FULL_HISTORY);
				}

				changes = missionChangeRepository.changesForMission(mission.getId(), start, end);
			}

			for (MissionChange missionChange: changes) {
    			hydrateMissionChange(missionChange);
			}
			
			return changes;
		} catch (Exception e) {
			logger.error("exception in getMissionChanges", e);
			return null;
		}
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER)
	public String getMissionKml(String missionName, String urlBase, String groupVector) {
		try {
			if (Strings.isNullOrEmpty(missionName)) {
				throw new IllegalArgumentException("empty 'name' path parameter");
			}

			Mission mission = getMissionService().getMissionNoContent(trimName(missionName), groupVector);

			LinkedList<CotElement> cotElements = new LinkedList<CotElement>();
			for (String uid : mission.getUids()) {
				CotElement cot = getLatestCotElement(
						uid, RemoteUtil.getInstance().getBitStringAllGroups(),
						TimeUtils.MAX_TS,
						kmlDao.new CotElementResultExtractor());

				if (cot == null) {
					logger.error("skipping uid in getMissionKml " + uid);
					continue;
				}

				cotElements.add(cot);
			}

			Kml kml = kmlService.process(cotElements);
			if (urlBase != null) {
				kmlService.setStyleUrlBase(kml, urlBase);
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			kml.marshal(baos);
			return baos.toString();

		} catch (Exception e) {
			logger.error("exception in getMissionKml", e);
			return null;
		}
	}
	
	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER)
	public String getMissionKml(UUID missionGuid, String urlBase, String groupVector) {
		try {
			
			Mission mission = getMissionService().getMissionNoContentByGuid(missionGuid, groupVector);

			LinkedList<CotElement> cotElements = new LinkedList<CotElement>();
			for (String uid : mission.getUids()) {
				CotElement cot = getLatestCotElement(
						uid, RemoteUtil.getInstance().getBitStringAllGroups(),
						TimeUtils.MAX_TS,
						kmlDao.new CotElementResultExtractor());

				if (cot == null) {
					logger.error("skipping uid in getMissionKml " + uid);
					continue;
				}

				cotElements.add(cot);
			}

			Kml kml = kmlService.process(cotElements);
			if (urlBase != null) {
				kmlService.setStyleUrlBase(kml, urlBase);
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			kml.marshal(baos);
			return baos.toString();

		} catch (Exception e) {
			logger.error("exception in getMissionKml by guid", e);
			return null;
		}
	}

	private MissionChanges saveExternalDataChange(ExternalMissionData externalMissionData, String externalDataToken, String notes,
												  String creatorUid, MissionChangeType missionChangeType,
												  Mission mission) {

		return saveExternalDataChangeAtTime(
				externalMissionData, externalDataToken, notes, creatorUid, missionChangeType, mission, new Date());
	}

	private MissionChanges saveExternalDataChangeAtTime(ExternalMissionData externalMissionData, String externalDataToken, String notes,
														String creatorUid, MissionChangeType missionChangeType,
														Mission mission, Date date) {

		MissionChanges changes = new MissionChanges();
		MissionChange change = new MissionChange(missionChangeType, mission);
		changes.add(change);

		change.setTimestamp(date);
		change.setCreatorUid(creatorUid);
		change.setExternalDataUid(externalMissionData.getId());
		change.setExternalDataName(externalMissionData.getName());
		change.setExternalDataTool(externalMissionData.getTool());
		if (externalDataToken != null) {
			change.setExternalDataToken(externalDataToken);
		}
		if (notes != null) {
			change.setExternalDataNotes(notes);
		}

		missionChangeRepository.saveAndFlush(change);

		hydrateMissionChange(change);

		return changes;
	}

	private ExternalMissionData setExternalMissionDataAtTime(UUID missionGuid, String creatorUid,
															 ExternalMissionData externalMissionData, String groupVector, Date date)  {

		Mission mission = getMissionService().getMissionByGuid(missionGuid, groupVector);

		try {
			ExternalMissionData externalMissionDataResult = externalMissionDataRepository
					.findByIdNoMission(externalMissionData.getId());
			if (externalMissionDataResult != null) {
				externalMissionDataRepository.update(
						externalMissionData.getId(), externalMissionData.getName(), externalMissionData.getTool(),
						externalMissionData.getUrlData(), externalMissionData.getUrlDisplay(), externalMissionData.getNotes(),
						mission.getId());
			} else {
				externalMissionDataRepository.create(
						externalMissionData.getId(), externalMissionData.getName(), externalMissionData.getTool(),
						externalMissionData.getUrlData(), externalMissionData.getUrlDisplay(), externalMissionData.getNotes(),
						mission.getId());
			}

			// record the change
			MissionChanges changes = saveExternalDataChangeAtTime(externalMissionData, null, externalMissionData.getNotes(),
					creatorUid, MissionChangeType.ADD_CONTENT, mission, date);

			// notify users of the change
			subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), mission.getName(),
					SubscriptionManagerLite.ChangeType.EXTERNAL_DATA,
					creatorUid, mission.getTool(), commonUtil.toXml(changes));

			return externalMissionData;

		} catch (Exception e) {
			logger.error("exception in setExternalMissionDataAtTime!", e);
			throw new TakException();
		}
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public ExternalMissionData setExternalMissionData(UUID missionGuid, String creatorUid,
													  ExternalMissionData externalMissionData, String groupVector)  {

		return setExternalMissionDataAtTime(missionGuid, creatorUid, externalMissionData, groupVector, new Date());
	}

	private void deleteExternalMissionDataAtTime(UUID missionGuid, String externalMissionDataId, String notes,
												 String creatorUid, String groupVector, Date date)  {

		Mission mission = getMissionService().getMissionByGuid(missionGuid, groupVector);

		// get the external mission data
		ExternalMissionData externalMissionData = externalMissionDataRepository.findByIdNoMission(externalMissionDataId);
		if (externalMissionData == null) {
			throw new NotFoundException("externalMissionDataId " + externalMissionDataId + " not found");
		}

		try {
			// record the change
			MissionChanges changes = saveExternalDataChangeAtTime(externalMissionData, null, notes,
					creatorUid, MissionChangeType.REMOVE_CONTENT, mission, date);

			// delete the externalMissionData
			externalMissionDataRepository.delete(externalMissionData);

			// notify users of the change
			subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), mission.getName(),
					SubscriptionManagerLite.ChangeType.EXTERNAL_DATA,
					creatorUid, mission.getTool(), commonUtil.toXml(changes));

		} catch (Exception e) {
			logger.error("exception in deleteExternalMissionDataAtTime!", e);
			throw new TakException();
		}
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public void deleteExternalMissionData(UUID missionGuid, String externalMissionDataId, String notes,
										  String creatorUid, String groupVector)  {

		deleteExternalMissionDataAtTime(missionGuid, externalMissionDataId, notes, creatorUid, groupVector, new Date());
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public void notifyExternalMissionDataChanged(UUID missionGuid, String externalMissionDataId,
												 String token, String notes, String creatorUid, String groupVector)  {

		Mission mission = getMissionService().getMissionByGuid(missionGuid, groupVector);

		// get the external mission data
		ExternalMissionData externalMissionData = externalMissionDataRepository.findByIdNoMission(externalMissionDataId);
		if (externalMissionData == null) {
			throw new NotFoundException("externalMissionDataId " + externalMissionDataId + " not found");
		}

		try {
			// record the change
			MissionChanges changes = saveExternalDataChange(externalMissionData, token, notes,
					creatorUid, MissionChangeType.ADD_CONTENT, mission);

			// notify users of the change
			subscriptionManager.announceMissionChange(UUID.fromString(mission.getGuid()), mission.getName(),
					SubscriptionManagerLite.ChangeType.EXTERNAL_DATA,
					creatorUid, mission.getTool(), commonUtil.toXml(changes));

		} catch (Exception e) {
			logger.error("exception in notifyExternalMissionDataChanged!", e);
			throw new TakException();
		}
	}

	@Override
	public MissionChange getLatestMissionChangeForContentHash(
			UUID missionGuid, String contentHash) {

		Long missionId = missionRepository.getLatestMissionIdForMissionGuid(missionGuid.toString());

		if (missionId == null) {
			return null;
		}

		List<MissionChange> changes = missionChangeRepository.findByTypeAndMissionIdAndContentHashOrderByIdAsc(
				MissionChangeType.ADD_CONTENT, missionId, contentHash);

		MissionChange change = null;
		if (!changes.isEmpty()) {
			change = changes.get(changes.size() - 1);
		}

		return change;
	}

	@Override
	public Set<Mission> getChildren(UUID missionGuid, String groupVector) {

		List<String> childGuids = missionRepository.getChildGuids(missionGuid.toString());
		if (childGuids == null || childGuids.isEmpty()) {
			throw new NotFoundException("Child missions not found for parent mission guid " + missionGuid);
		}

		Set<Mission> children = new ConcurrentSkipListSet<>();
		for (String childGuid : childGuids) {
			try {
				Mission childMission = getMissionService().getMissionByGuid(UUID.fromString(childGuid), groupVector);
				if (childMission != null) {
					children.add(childMission);
				}
			} catch (NotFoundException e) {
				logger.error("child mission not found! " + childGuid);
			} catch (Exception e) {
				logger.error("exception getting child mission! " + childGuid, e);
			}
		}

		return children;
	}

	@Override
	public String generateToken(
			String uid, UUID missionGuid, String missionName, MissionTokenUtils.TokenType tokenType, long expirationMillis) {
		try {
			PrivateKey privateKey = JwtUtils.getInstance().getPrivateKey();
			String flowTag = CoreConfigFacade.getInstance().getRemoteConfiguration().getFilter().getFlowtag().getText();
			return MissionTokenUtils
					.getInstance(privateKey)
					.createMissionToken(uid, missionName, tokenType, expirationMillis, flowTag, missionGuid);

		} catch (Exception e) {
			logger.error("exception in generateToken!", e);
			return null;
		}
	}

	@Override
	public MissionRole getRoleFromTypeAndInvitee(UUID missionGuid, String type, String invitee) {

		Long missionId = missionRepository.getLatestMissionIdForMissionGuid(missionGuid.toString());

		if (missionId == null) {
			throw new NotFoundException("mission " + missionGuid + " does not exist.");
		}


		MissionInvitation invitation = missionInvitationRepository.
				findByMissionIdAndTypeAndInvitee(missionId, type, invitee);

		if (invitation == null) {
			logger.debug("no invite found for {} {} {}", missionGuid, type, invitee);
			return null;
		}

		return invitation.getRole();
	}

	@Override
	public MissionRole getRoleFromToken(
			Mission mission, MissionTokenUtils.TokenType[] validTokenTypes, HttpServletRequest request) {
		try {

			if (commonUtil.isAdmin()) {
				if (logger.isDebugEnabled()) {
					logger.debug("getRoleFromToken granting admin MISSION_OWNER for : " + request.getServletPath());
				}

				return missionRoleRepository.findFirstByRole(MissionRole.Role.MISSION_OWNER);
			}

			//
			// if the request has an Authorization header, get the role from the token
			//
			String authorization = request.getHeader("MissionAuthorization") != null ?
					request.getHeader("MissionAuthorization") : request.getHeader("Authorization");
			if (authorization == null) {
				return null;
			}

			// see if the request is using bearer/token authentication
			if (!authorization.startsWith("Bearer ")) {
				logger.error("Bearer not found : " + StringUtils.normalizeSpace(authorization) + " : " + StringUtils.normalizeSpace(request.getServletPath()));
				return null;
			}

			// parse the token's claims
			Claims claims = null;
			String token = authorization.substring(7);
			try {

				if (CoreConfigFacade.getInstance().getRemoteConfiguration().getSecurity().getTls() == null) {
					logger.error("unable to find tls configuration!");
				}

				claims = MissionTokenUtils.getInstance(JwtUtils.getInstance().getPrivateKey()).decodeMissionToken(token);

			} catch (Exception e) {
				logger.error("decodeMissionToken failed! : " + authorization + " : " + request.getServletPath());
				return null;
			}

			if (claims == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("unable to parse mission claims from token");
				}
				return null;
			}

			MissionTokenUtils.TokenType tokenType = MissionTokenUtils.TokenType.valueOf(claims.getSubject());

			//
			// make sure we have the correct token type
			//
			if (!ArrayUtils.contains(validTokenTypes, tokenType)) {
				String expected = "";
				for (MissionTokenUtils.TokenType type : validTokenTypes) {
					expected += type.name() + " ";
				}
				logger.error("found unexpected token type, expected " + expected
						+ ", found " + tokenType.name() + ", " + request.getServletPath());
				return null;
			}

			String missionName = (String) claims.get(MissionTokenUtils.MISSION_NAME_CLAIM);
			if (missionName == null) {
				logger.error("found token without MISSION_NAME_CLAIM!");
				return null;
			}

			if (missionName.compareTo(mission.getName()) != 0) {
				logger.error("illegal attempt to re-use token for different mission! : " + request.getServletPath());
				return null;
			}

			if (tokenType == MissionTokenUtils.TokenType.SUBSCRIPTION) {

				String subscriptionUid = claims.get(tokenType.name(), String.class);
				if (subscriptionUid == null) {
					logger.error("missing subscription uid for subscription token! : " + request.getServletPath());
					return null;
				}

				MissionSubscription missionSubscription = missionSubscriptionRepository.
						findByUidAndMissionNameNoMission(subscriptionUid, mission.getName());
				if (missionSubscription == null) {
					logger.error("can't find subscription for token! : " + subscriptionUid
							+ ", " + request.getServletPath());
					return null;
				}

				return missionSubscription.getRole();

			} else if (tokenType == MissionTokenUtils.TokenType.INVITATION) {

				MissionInvitation invitation = missionInvitationRepository.findByToken(token);
				if (invitation == null) {
					logger.error("can't find invitation for token! : " + request.getServletPath());
					return null;
				}

				if (invitation.getMissionName().compareToIgnoreCase(mission.getName()) != 0) {
					logger.error("illegal attempt to re-use invitation token for different mission! : "
							+ request.getServletPath());
					return null;
				}

				return invitation.getRole();

			} else if (tokenType == MissionTokenUtils.TokenType.ACCESS) {

				return getDefaultRole(mission);

			} else {
				logger.error("unknown token type : " + tokenType.name() + ", " + request.getServletPath());
				return null;
			}

		} catch (Exception e) {
			logger.error("exception in getRoleFromToken!", e);
			return null;
		}
	}

	@Override
	public boolean validateMissionCreateGroupsRegex(HttpServletRequest request) {
		try {
			String missionCreateGroupsRegex = CoreConfigFacade.getInstance().getRemoteConfiguration()
					.getNetwork().getMissionCreateGroupsRegex();
			if (Strings.isNullOrEmpty(missionCreateGroupsRegex)) {
				return true;
			}

			boolean match = false;
			Pattern pattern = Pattern.compile(
					missionCreateGroupsRegex, Pattern.CASE_INSENSITIVE);
			for (Group group : commonUtil.getGroupsFromSessionId(request.getSession().getId())) {
				if (match = pattern.matcher(group.getName()).find()) {
					break;
				}
			}
			return match;
		} catch (Exception e) {
			logger.error("exception in validateMissionCreateGroupsRegex", e);
			return false;
		}
	}

	@Override
	public MissionRole getRoleForRequest(Mission mission, HttpServletRequest request) {
		try {

			if (logger.isDebugEnabled()) {
				logger.debug("getRoleForRequest " + (mission == null ? "null" : mission.getName()));
			}

			// check to see if there was a token submitted with the request
			MissionRole role = getRoleFromToken(
					mission, new MissionTokenUtils.TokenType[] {
							MissionTokenUtils.TokenType.ACCESS,
							MissionTokenUtils.TokenType.SUBSCRIPTION
					}, request);
			if (role != null) {

				if (logger.isDebugEnabled()) {
					logger.debug("request had token: " + role);
				}

				return role;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("getRoleFromToken didnt find token for : "
						+ request.getMethod() + " " + request.getServletPath());
			}

			if (commonUtil.isAdmin()) {
				if (logger.isDebugEnabled()) {
					logger.debug("getRoleForRequest granting admin MISSION_OWNER for : " + request.getServletPath());
				}

				return missionRoleRepository.getRoleByOrdinalId(MissionRole.Role.MISSION_OWNER.ordinal());
			}

			// Missions that require a token, so bail if we haven't found a role yet
			if (mission.isPasswordProtected() || mission.isInviteOnly()) {
				logger.error("getRoleForRequest failed to get role for password protected mission! : "
						+ request.getServletPath());
				return null;
			}

			return getDefaultRole(mission);

		} catch (Exception e) {
			logger.error("exception in getRoleForRequest!", e);
			return null;
		}
	}

	@Override
	public boolean validateRoleAssignment(Mission mission, HttpServletRequest request, MissionRole attemptAssign) {

		try {
			MissionRole currentRole = (MissionRole)request.getAttribute(MissionRole.class.getName());
			if (currentRole == null) {
				logger.error("getRoleFromRequest failed!");
				return false;
			}

			if (attemptAssign == null) {
				logger.error("validateRoleAssignment checking null assignment!");
				return false;
			}

			//
			// make sure that current user/request possesses all permissions that they're attempting to assign
			//
			if (!currentRole.hasAllPermissions(attemptAssign)) {
				logger.error("currentRole permissions check failed!");
				return false;
			}

			//
			// prevent assignment of anything < the defaultRole for non password-protected missions
			//
			if (!mission.isPasswordProtected() && !attemptAssign.hasAllPermissions(getDefaultRole(mission))) {
				logger.error("attemptAssign permissions check failed!");
				return false;
			}

			return true;

		} catch (Exception e) {
			logger.error("exception in validateRoleAssignment!", e);
			return false;
		}
	}

	@Override
	@CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
	public void setRoleByClientUid(Long missionId, String clientUid, Long roleId) {
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("roleId", roleId);
		namedParameters.addValue("clientUid", clientUid);
		namedParameters.addValue("missionId", missionId);
		String sql = "update mission_subscription set role_id = :roleId where client_uid = :clientUid and mission_id = :missionId";
		new NamedParameterJdbcTemplate(dataSource).update(sql, namedParameters);
	}

	@Override
	@CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
	public void setRoleByUsername(Long missionId, String username, Long roleId) {
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("roleId", roleId);
		namedParameters.addValue("username", username);
		namedParameters.addValue("missionId", missionId);
		String sql = "update mission_subscription set role_id = :roleId where username = :username and mission_id = :missionId";
		new NamedParameterJdbcTemplate(dataSource).update(sql, namedParameters);
	}

	@Override
	@CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
	public void setRoleByClientUidOrUsername(Long missionId, String clientUid, String username, Long roleId) {
		if (!Strings.isNullOrEmpty(username)) {
			getMissionService().setRoleByUsername(missionId, username, roleId);
		} else {
			getMissionService().setRoleByClientUid(missionId, clientUid, roleId);
		}
	}

	@Override
	public boolean setRole(Mission mission, String clientUid, String username, MissionRole role, String groupVector) {
		try {

			String roleXml = null;
			if (role != null) {
				roleXml = commonUtil.toXml(role);
				getMissionService().setRoleByClientUidOrUsername(
						mission.getId(), clientUid, username, role.getId());
			} else {
				getMissionService().missionUnsubscribe(
						UUID.fromString(mission.getGuid()), clientUid, username, groupVector,false);
			}

			if (!Strings.isNullOrEmpty(username)) {
				RemoteSubscription subscription = subscriptionManager.getSubscriptionByUsersDisplayName(username);
				if (subscription != null) {
					clientUid = subscription.clientUid;
				}
			}

			if (!Strings.isNullOrEmpty(clientUid)) {
				subscriptionManager.sendMissionRoleChange(
						UUID.fromString(mission.getGuid()), mission.getName(), clientUid, "", mission.getTool(), roleXml);
			}

			return true;
		} catch (Exception e) {
			logger.error("exception in setRole!", e);
			return false;
		}
	}

	@Override
	@CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
	public void setSubscriptionUsername(Long missionId, String clientUid, String username) {
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("missionId", missionId);
		namedParameters.addValue("clientUid", clientUid);
		namedParameters.addValue("username", username);
		String sql = "update mission_subscription set username = :username where client_uid = :clientUid and mission_id = :missionId";
		new NamedParameterJdbcTemplate(dataSource).update(sql, namedParameters);
	}

	@Override
	public boolean validatePermission(MissionPermission.Permission permission, HttpServletRequest request) {
		try {
			MissionRole role = (MissionRole)request.getAttribute(MissionRole.class.getName());
			if (role == null) {
				logger.error("getRoleFromRequest failed!");
				return false;
			}

			return role.hasPermission(permission);

		} catch (Exception e) {
			logger.error("exception in validatePermission!", e);
			return false;
		}
	}

	@Override
	public MissionRole getDefaultRole(Mission mission) {
		try {
			MissionRole defaultMissionRole;

			if (mission.getDefaultRole() != null) {
				defaultMissionRole = mission.getDefaultRole();
			} else {
				defaultMissionRole = missionRoleRepository.findFirstByRole(MissionRole.defaultRole);
			}

			if (defaultMissionRole == null) {
				logger.error("missionRoleRepository unable to find defaultRole!");
			}

			defaultMissionRole.setUsingMissionDefault(true);

			return defaultMissionRole;

		} catch (Exception e) {
			logger.error("exception in getDefaultRole!", e);
			return null;
		}
	}

	@Override
	public int getApiVersionNumberFromRequest(HttpServletRequest request) {
		try {
			String requestedApiVersionNumber = request.getHeader(Constants.API_VERSION_HEADER);
			if (requestedApiVersionNumber != null) {
				return Integer.parseInt(requestedApiVersionNumber);
			}
		} catch (Exception e) {
			logger.error("exception in getApiVersionNumberFromRequest!");
		}

		return 2;
	}

	@Override
	public boolean inviteOrUpdate(Mission mission, List<MissionSubscription> subscriptions,
								  String creatorUid, String groupVector) {

		try {
			for (MissionSubscription next : subscriptions) {

				MissionSubscription existing = null;

				if (!Strings.isNullOrEmpty(next.getUsername())) {
					existing = getMissionService().
							getMissionSubscriptionByMissionNameAndUsernameNoMission(mission.getName(), next.getUsername());
				} else if (!Strings.isNullOrEmpty(next.getClientUid())) {
					existing = missionSubscriptionRepository.
							findByMissionNameAndClientUidNoMission(mission.getName(), next.getClientUid());
				}

				MissionRole role = missionRoleRepository.findFirstByRole(next.getRole().getRole());

				if (existing != null) {
					setRole(mission, next.getClientUid(), next.getUsername(), role, groupVector);
				} else {
					if (!Strings.isNullOrEmpty(next.getUsername())) {
						missionInvite(UUID.fromString(mission.getGuid()), next.getUsername(),
								MissionInvitation.Type.userName, role, creatorUid, groupVector);
					} else if (!Strings.isNullOrEmpty(next.getClientUid())) {
						missionInvite(UUID.fromString(mission.getGuid()), next.getClientUid(),
								MissionInvitation.Type.clientUid, role, creatorUid, groupVector);
					}
				}
			}

			return true;

		} catch (Exception e) {
			logger.error("Exception in inviteOrUpdate", e);
		}

		return true;
	}

	@Override
	public void validatePassword(Mission mission, String password) {
		if (mission.isPasswordProtected()) {
			if ((!Strings.isNullOrEmpty(password) && !BCrypt.checkpw(password, mission.getPasswordHash()))
					|| Strings.isNullOrEmpty(password)) {
				throw new ForbiddenException("Illegal attempt to access mission!");
			}
		}
	}

	private CotParser getCotParser() {

		if (cotParser.get() == null) {
			cotParser.set(new CotParser(
					CoreConfigFacade.getInstance().getRemoteConfiguration().getSubmission() == null ? false : CoreConfigFacade.getInstance().getRemoteConfiguration().getSubmission().isValidateXml()));
		}

		return cotParser.get();

	}

	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	@Override
	public boolean setExpiration(String missionName, Long expiration, String groupVector) {
		if (logger.isDebugEnabled()) {
			logger.debug(" setting expiration on mission " + missionName + " expiration " + expiration);
		}
		if (Strings.isNullOrEmpty(missionName)) {
			throw new IllegalArgumentException("empty 'missionName' path parameter");
		}
		String missionNameTrim = trimName(missionName);
		MapSqlParameterSource namedParameters;

		if (expiration == null) {
			namedParameters = new MapSqlParameterSource("expiration", -1L);
		} else {
			if (expiration < -1) {
				throw new IllegalArgumentException("bad expiration  parameter" + expiration);
			} else {
				namedParameters = new MapSqlParameterSource("expiration", expiration);
			}
		}

		namedParameters.addValue("missionName", missionNameTrim);

		String sql = "update mission set expiration = :expiration where name = :missionName";
		int updated = new NamedParameterJdbcTemplate(dataSource).update(sql, namedParameters);
		if (logger.isDebugEnabled()) {
			logger.debug(" did the mission get updated " + updated);
		}
		return updated > 0;
	}
	
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	@Override
	public boolean setExpiration(UUID missionGuid, Long expiration, String groupVector) {
		
		if (logger.isDebugEnabled()) {
			logger.debug(" setting expiration on mission " + missionGuid + " expiration " + expiration);
		}
		
		MapSqlParameterSource namedParameters;

		if (expiration == null) {
			namedParameters = new MapSqlParameterSource("expiration", -1L);
		} else {
			if (expiration < -1) {
				throw new IllegalArgumentException("bad expiration  parameter" + expiration);
			} else {
				namedParameters = new MapSqlParameterSource("expiration", expiration);
			}
		}

		namedParameters.addValue("missionGuid", missionGuid.toString());

		String sql = "update mission set expiration = :expiration where guid = uuid(:missionName)";
		int updated = new NamedParameterJdbcTemplate(dataSource).update(sql, namedParameters);
		if (logger.isDebugEnabled()) {
			logger.debug(" did the mission get updated " + updated);
		}
		return updated > 0;
	}

	@Override
	public void deleteMissionByTtl(Integer ttl) {
		if (ttl == null || ttl <=-1) {
			if (logger.isDebugEnabled()) {
				logger.debug(" bad ttl, query ignored " + ttl);
			}
			return;
		}
		String groupVectorMission = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(commonUtil.getAllInOutGroups()));
		try {
			List<Mission> missions = missionRepository.getAllMissionsByTtl(true, true, groupVectorMission, ttl);
			logger.info( " delete mission by time to live found " + missions.size() + " mission ");
			for (Mission mission : missions) {
				logger.info(" mission " + mission.getName());
				getMissionService().deleteMission(mission.getName(), mission.getCreatorUid(), mission.getGroupVector(), true);
			}
		} catch (Exception e) {
			logger.error(" error querying deleteMissionByTtl ", e);
		}
	}

	@Override
	public void deleteMissionByExpiration(Long expiration) {

		if (expiration == null || (expiration.longValue() <= -1L) ) {
			if (logger.isDebugEnabled()) {
				logger.debug(" expiration is null or -1 query ignored, expiration: " + expiration);
			}
			return;
		}
		try {
			String groupVectorMission = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(commonUtil.getAllInOutGroups()));
			List<Mission> missions = missionRepository.getAllMissionsByExpiration(true, true, groupVectorMission, expiration);
			logger.info( " delete mission by expiration found " + missions.size() + " mission ");
			for (Mission mission : missions) {
				logger.info(" deleting mission " + mission.getName());
				getMissionService().deleteMission(mission.getName(), mission.getCreatorUid(), mission.getGroupVector(), true);
			}
		} catch (Exception e) {
			logger.error(" error querying deleteMissionByExpiration ", e);
		}
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public List<MissionChange> findLatestCachedMissionChanges(UUID missionGuid, List<String> uids, List<String> hashes, int changeType) {

		Long missionId = missionRepository.getLatestMissionIdForMissionGuid(missionGuid.toString());

		if (missionId == null) {
			return null;
		}

		return missionChangeRepository.findLatest(missionId, uids, hashes, changeType);
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public Map<Integer, List<String>> getCachedResources(String missionName, Set<Resource> resources) {
		return getMissionService().hydrate(resources);
	}
	
	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public Map<Integer, List<String>> getCachedResourcesByGuid(UUID missionGuid, Set<Resource> resources) {
		return getMissionService().hydrate(resources);
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public List<MissionChange> findLatestCachedMissionChangesForUids(UUID missionGuid, List<String> uids, int changeType) {

		Long missionId = missionRepository.getLatestMissionIdForMissionGuid(missionGuid.toString());

		if (missionId == null) {
			return null;
		}

		return missionChangeRepository.findLatestForUids(missionId, uids, changeType);
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public List<MissionChange> findLatestCachedMissionChangesForHashes(UUID missionGuid, List<String> hashes, int changeType) {

		Long missionId = missionRepository.getLatestMissionIdForMissionGuid(missionGuid.toString());

		if (missionId == null) {
			return null;
		}

		return missionChangeRepository.findLatestForHashes(missionId, hashes, changeType);
	}

	@Override
	@Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, keyGenerator = "methodNameMultiStringArgCacheKeyGenerator")
	public List<Resource> getCachedResourcesByHash(UUID missionGuid, String hash) {
		return resourceRepository.findByHash(hash);
	}

	private MissionChanges saveFeedChangeAtTime(String missionFeedUid, String creatorUid, MissionChangeType missionChangeType,
												Mission mission, Date date) {

		MissionChanges changes = new MissionChanges();
		MissionChange change = new MissionChange(missionChangeType, mission);
		change.setMissionFeedUid(missionFeedUid);
		changes.add(change);

		change.setTimestamp(date);
		change.setCreatorUid(creatorUid);

		missionChangeRepository.saveAndFlush(change);

		hydrateMissionChange(change);

		return changes;
	}

	@Override
	public MissionFeed getMissionFeed(String missionFeedUid) {
		return missionFeedRepository.getByUidNoMission(missionFeedUid);
	}

	@Override
	public DataFeedDTO getDataFeed(String dataFeedUid) {
		if (!Strings.isNullOrEmpty(dataFeedUid)) {
			List<DataFeedDTO> results = dataFeedRepository.getDataFeedByUUID(dataFeedUid);

			if (results != null && results.size() > 0) {
				return results.get(0);
			}
		}

		return null;
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public MissionFeed addFeedToMission(String creatorUid, Mission mission, String dataFeedUid, String filterPolygon, List<String> filterCotTypes, String filterCallsign) {
		return getMissionService().addFeedToMission(UUID.randomUUID().toString(), creatorUid, mission, dataFeedUid, filterPolygon, filterCotTypes, filterCallsign);
	}

	//     MissionFeed addFeedToMission(String creatorUid, Mission mission, String dataFeedUid, String filterPolygon, List<String> filterCotTypes, String filterCallsign);

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public MissionFeed addFeedToMission(String missionFeedUid, String creatorUid, Mission mission, String dataFeedUid, String filterPolygon, List<String> filterCotTypes, String filterCallsign) {
		MissionFeed missionFeed = missionFeedRepository.getByUidNoMission(missionFeedUid);

		if (missionFeed == null) {
			missionFeed = new MissionFeed();
			missionFeed.setUid(missionFeedUid);
			missionFeed.setDataFeedUid(dataFeedUid);
			missionFeed.setFilterPolygon(filterPolygon);
			missionFeed.setFilterCotTypes(filterCotTypes);
			missionFeed.setFilterCallsign(filterCallsign);
			missionFeed.setMission(mission);

			missionFeed = missionFeedRepository.save(missionFeed); // FIXME:  org.postgresql.util.PSQLException: The column name resource3_18_1_ was not found in this ResultSet

			// record the change
			MissionChanges changes = saveFeedChangeAtTime(
					missionFeed.getUid(), creatorUid, MissionChangeType.CREATE_DATA_FEED, mission, new Date());

			// notify users of the change
			subscriptionManager.announceMissionChange(mission.getGuidAsUUID(), mission.getName(),
					ChangeType.DATA_FEED, creatorUid, mission.getTool(), commonUtil.toXml(changes));

			findNameForMissionFeed(missionFeed);

		}else {
			findNameForMissionFeed(missionFeed);
		}

		return missionFeed;
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public void removeFeedFromMission(String missionName, String creatorUid, Mission mission, String missionFeedUid) {
		MissionFeed missionFeed = missionFeedRepository.getByUidNoMission(missionFeedUid);

		if (missionFeed != null) {
			// record the change
			MissionChanges changes = saveFeedChangeAtTime(
					missionFeedUid, creatorUid, MissionChangeType.DELETE_DATA_FEED, mission, new Date());

			// notify users of the change
			subscriptionManager.announceMissionChange(mission.getGuidAsUUID(), mission.getName(),
					ChangeType.DATA_FEED, creatorUid, mission.getTool(), commonUtil.toXml(changes));

			missionFeedRepository.deleteByUid(missionFeedUid);
		}
	}

	private MissionChanges saveMapLayerChangeAtTime(String mapLayerUid, String creatorUid, MissionChangeType missionChangeType,
													Mission mission, Date date) {

		MissionChanges changes = new MissionChanges();
		MissionChange change = new MissionChange(missionChangeType, mission);
		change.setMapLayerUid(mapLayerUid);
		changes.add(change);

		change.setTimestamp(date);
		change.setCreatorUid(creatorUid);

		missionChangeRepository.saveAndFlush(change);

		hydrateMissionChange(change);

		return changes;
	}

	@Override
	public MapLayer getMapLayer(String mapLayerUid) {
		MapLayer mapLayer = null;
		try {
			mapLayer = mapLayerService.getMapLayerForUid(mapLayerUid);
		} catch (Exception e) {
			logger.debug("exception in getMapLayer " + e.getMessage());
		}
		return mapLayer;
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public MapLayer addMapLayerToMission(String missionName, String creatorUid, Mission mission, MapLayer mapLayer) {
		MapLayer newMapLayer = mapLayerService.createMapLayer(mapLayer);

		// record the change
		MissionChanges changes = saveMapLayerChangeAtTime(
				mapLayer.getUid(), creatorUid, MissionChangeType.ADD_CONTENT, mission, new Date());

		// notify users of the change
		subscriptionManager.announceMissionChange(mission.getGuidAsUUID(), mission.getName(),
				ChangeType.MAP_LAYER, creatorUid, mission.getTool(), commonUtil.toXml(changes));

		return newMapLayer;
	}
	
	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public MapLayer updateMapLayer(String missionName, String creatorUid, Mission mission, MapLayer mapLayer) {
		return mapLayerService.updateMapLayer(mapLayer);
	}

	@Override
	@CacheEvict(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, allEntries = true)
	public void removeMapLayerFromMission(String missionName, String creatorUid, Mission mission, String mapLayerUid) {

		// record the change
		MissionChanges changes = saveMapLayerChangeAtTime(
				mapLayerUid, creatorUid, MissionChangeType.REMOVE_CONTENT, mission, new Date());

		// notify users of the change
		subscriptionManager.announceMissionChange(mission.getGuidAsUUID(), mission.getName(),
				ChangeType.MAP_LAYER, creatorUid, mission.getTool(), commonUtil.toXml(changes));

		mapLayerService.deleteMapLayer(mapLayerUid);
	}
	@Override
	@Cacheable(cacheResolver = AllMissionCacheResolver.ALL_MISSION_CACHE_RESOLVER, key="{#root.methodName, #root.args[0]}", sync = true)
	public List<Mission> getMissionsForDataFeed(String feed_uid) {
		List<Mission> missions = missionRepository.getMissionsForDataFeed(feed_uid);
		
		for (Mission mission : missions) {
			UnproxyHelper.unproxyMission(mission);
		}
		
		return missions;
	}


	@Override
	@Cacheable(cacheResolver = AllMissionCacheResolver.ALL_MISSION_CACHE_RESOLVER, key="{#root.methodName, #root.args[0]}", sync = true)
	public List<String> getMinimalMissionsJsonForDataFeed(String feed_uid) throws JsonProcessingException {

		List<String> result = new ArrayList<>();

		for (Mission m : missionRepository.getMissionsForDataFeed(feed_uid)) {
			result.add(mapper.writeValueAsString(new MinimalMission(m))); // convert the mission to a MinimalMission
		}

		return result;
	}

	@Override
	@Cacheable(cacheResolver = AllMissionCacheResolver.ALL_MISSION_CACHE_RESOLVER, key="{#root.methodName, #root.args[0]}", sync = true)
	public List<String> getMinimalMissionFeedsJsonForDataFeed(String dataFeedUid) throws JsonProcessingException {
		List<String> result = new ArrayList<>();

		for (MissionFeed mf : missionFeedRepository.getMissionFeedsByDataFeedUid(dataFeedUid)) {
			
			if (mf != null && mf.getMission() != null && mf.getMission().getGuid() != null) {
				result.add(mapper.writeValueAsString(new MinimalMissionFeed(mf)));
			}
		}

		return result;
	}

	@Override
	public void sendLatestFeedEvents(Mission mission, MissionFeed missionFeed, List<String> clientUidList, String groupVector) {
		try {
			if (!CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().isEnabled()) return;

			Polygon polygon = null;
			GeospatialFilter.BoundingBox boundingBox = null;
			// use polygon over bbox
			if (!Strings.isNullOrEmpty(mission.getBoundingPolygon())) {
				polygon = GeomUtils.postgisBoundingPolygonToPolygon(mission.getBoundingPolygon());
			}
			// fallback to bbox
			else {
				boundingBox = GeomUtils.getBoundingBoxFromBboxString(mission.getBbox());
			}

			int sent = 0;
			for (DataFeedDTO datafeed : dataFeedRepository.getDataFeedByUUID(missionFeed.getDataFeedUid())) {
				if (!datafeed.isSync()) {
					continue;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("found datafeed to sync " + datafeed.getUUID());
				}

				Collection<CotEventContainer> feedEvents = dataFeedCotService.getCachedDataFeedEvents(datafeed.getUUID());

				if (logger.isDebugEnabled()) {
					logger.debug("found events to sync : " + feedEvents.size());
				}

				// iterate over the events
				for (CotEventContainer feedEvent : feedEvents) {
					if (polygon != null && !GeomUtils.polygonContainsCoordinate(polygon,
							feedEvent.getLatDouble(), feedEvent.getLonDouble())) {
						continue;
					} else if (boundingBox != null && !GeomUtils.bboxContainsCoordinate(
							boundingBox, feedEvent.getLatDouble(), feedEvent.getLonDouble())) {
						continue;
					}

					long now = new Date().getTime();
					Timestamp nowTs = new Timestamp(now);
					feedEvent.setServerTime(nowTs == null ? "" : DateUtil.toCotTime(nowTs.getTime()));

					long staleSeconds = 365 * 24 * 60 * 60;
					Timestamp staleTs = new Timestamp(staleSeconds);
					feedEvent.setStale(staleTs == null ? "" : DateUtil.toCotTime(staleTs.getTime()));

					@SuppressWarnings("unchecked")
					NavigableSet<Group> groups = (NavigableSet<Group>)feedEvent.getContext(Constants.GROUPS_KEY);
					if (groups == null || groups.isEmpty()) {
						logger.error("sendLatestFeedEvents found cached feedEvent with no groups");
						groups = groupManager.groupVectorToGroupSet(mission.getGroupVector());
					}

					// send the event to the COP subscriber
					submission.submitCot(feedEvent, clientUidList, new ArrayList<String>(), groups, false, true);

					sent++;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("filtered events sent : " + sent);
				}
			}
		} catch (Exception e) {
			logger.error("exception in sendLatestFeedEvents", e);
		}
	}

	@Override
	public int countAllMissions(boolean passwordProtected, boolean defaultRole, String tool)  {

		if (logger.isDebugEnabled()) {
			logger.debug("mission service getAllMissions cache miss");
		}



		int missions = missionRepository.countAllMissions(passwordProtected, defaultRole);


		return missions;
	}

	@Override
	public List<String> getAllCotForString(String uidSearch, String groupVector) {

		String sql =  "select distinct uid from cot_router where uid like ? " // querys
				+ remoteUtil.getGroupAndClause()
				+ " limit 5";
		String likeValue = "";
		if (!uidSearch.isBlank()) {
			likeValue = "%" + uidSearch + "%";
		} else {
			likeValue = "%";
		}
		return new JdbcTemplate(dataSource).query(sql,
				new Object[] { likeValue, groupVector }, // parameters
				new ResultSetExtractor<List<String>>() {
					@Override public List<String> extractData(ResultSet results) throws SQLException {
						if (results == null) {
							throw new IllegalStateException("null result set");
						}

						if (!results.next()) {
							throw new NotFoundException("no results");
						}

						List<String> cotUids = new ArrayList<String>();

						do {
							cotUids.add(results.getString(1));
						} while (results.next());

						return cotUids;
					}
				});
	}
	
	@Override
    public void hydrateFeedNameForMission(Mission mission) {
		
    	if (mission.getFeeds()!=null) {
    		for (MissionFeed missionFeed: mission.getFeeds()) {
    			if (missionFeed.getDataFeedUid() != null) {
    				DataFeedDTO dataFeedDao = getDataFeed(missionFeed.getDataFeedUid());
    		        if (dataFeedDao != null) {
    		        	missionFeed.setName(dataFeedDao.getName());
    		        }
    			}
			}
    	}
    }
	
	@Override
    public void hydrateMissionChange(MissionChange missionChange) {
		
		findContentResource(missionChange);
		
		findUidDetails(missionChange);
		
		findMissionFeed(missionChange);
		
		if (missionChange.getMissionFeed()!=null) {
			findNameForMissionFeed(missionChange.getMissionFeed());
		}
		
		findMapLayer(missionChange);
		
		findExternalMissionData(missionChange);
    }

	@Override	
    public void hydrateMissionChangesForMission(Mission mission) {
    	
		if (mission.getMissionChanges()!=null) {
			
    		for (MissionChange missionChange: mission.getMissionChanges()) {
    			hydrateMissionChange(missionChange);
			}
    	}
	}

    private void findContentResource(MissionChange missionChange) {

        Resource resource = null;

        if (missionChange.getContentHash() != null) {

            try {
                resource = ResourceUtils.fetchResourceByHash(missionChange.getContentHash());
               
            } catch (Exception e) {
                logger.debug("exception fetching resource by hash " + e.getMessage(), e);
            }
            
            // in case the resource has been deleted, just capture the hash
            if (resource == null) {
                resource = new Resource();
                resource.setHash(missionChange.getContentHash());
                resource.setCreatorUid(null);
                resource.setFilename(null);
                resource.setId(null);
                resource.setKeywords(null);
                resource.setMimeType(null);
                resource.setName(null);
                resource.setSubmissionTime(null);
                resource.setSize(null);
                resource.setSubmitter(null);
                resource.setUid(null);
            }
        }

        missionChange.setContentResource(resource);

    }
    
    private void findUidDetails(MissionChange missionChange) {

        if (missionChange.getContentUid() != null && missionChange.getTimestamp() != null)   { // FIXME: Why this one check getTimestamp(), but the below use Server time???) 
            try {
            	UidDetails uidDetails = missionChange.getUidDetails();
                if (uidDetails == null) {
                    uidDetails = new UidDetails();
                	
                    hydrate(uidDetails, missionChange.getContentUid(), missionChange.getServerTime());
                    
                }
                missionChange.setUidDetails(uidDetails);
            } catch (Exception e) {
                logger.debug("Exception in findUidDetails!", e);
            }
        }
    }
    
    private void findMapLayer(MissionChange missionChange) {
        try {
        	String mapLayerUid = missionChange.getMapLayerUid();
            if (mapLayerUid != null) {
            	
                MapLayer mapLayer = getMapLayer(mapLayerUid);
                if (mapLayer == null) {
                    mapLayer = new MapLayer();
                    mapLayer.setUid(mapLayerUid);
                }

                missionChange.setMapLayer(mapLayer);
            }
        } catch (Exception e) {
            logger.error("exception in findMapLayer", e);
        }

    }
    
    private void findExternalMissionData(MissionChange missionChange) {

        if (missionChange.getExternalDataUid() != null && missionChange.getExternalDataName()!= null
                && missionChange.getExternalDataTool()!= null && missionChange.getExternalDataNotes() != null){
        	ExternalMissionData externalMissionData = hydrate(
            		missionChange.getExternalDataUid(), missionChange.getExternalDataName(), missionChange.getExternalDataTool(), missionChange.getExternalDataToken(), missionChange.getExternalDataNotes());
        	missionChange.setExternalMissionData(externalMissionData);
        }
    }
    
    private void findMissionFeed(MissionChange missionChange) {
    	String missionFeedUid = missionChange.getMissionFeedUid();
        if (missionFeedUid != null) {
        	
            MissionFeed missionFeed = getMissionFeed(missionFeedUid);
            if (missionFeed == null) {
                missionFeed = new MissionFeed();
                missionFeed.setUid(missionFeedUid);
            }
            missionChange.setMissionFeed(missionFeed); 
        }

    }

    private void findNameForMissionFeed(MissionFeed missionFeed) {
    	if (missionFeed.getDataFeedUid() == null) {
    		return;
    	}
        DataFeedDTO dataFeedDao = getDataFeed(missionFeed.getDataFeedUid());
        if (dataFeedDao != null) {
        	missionFeed.setName(dataFeedDao.getName());
        }
    }
    
    // caching methods for MissionSubscriptions. Could be moved to another service class.
	@Override
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
	public MissionSubscription getMissionSubcriptionByMissionNameAndClientUidAndUsernameNoMission(String missionName, String clientUid, String username) {
		return (MissionSubscription) Hibernate.unproxy(missionSubscriptionRepository.findByMissionNameAndClientUidAndUsernameNoMission(missionName, clientUid, username));
		
	}

	@Override
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
	public MissionSubscription getMissionSubscriptionByMissionNameAndClientUidNoMission(String missionName, String clientUid) {
		return (MissionSubscription) Hibernate.unproxy(missionSubscriptionRepository.findByMissionNameAndClientUidNoMission(missionName, clientUid));
	}
	
	@Override
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
	public MissionSubscription getMissionSubscriptionByMissionGuidAndClientUidNoMission(String missionGuid, String clientUid) {
		return (MissionSubscription) Hibernate.unproxy(missionSubscriptionRepository.findByMissionGuidAndClientUidNoMission(missionGuid, clientUid));
	}
	
	@Override
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
	public MissionSubscription getMissionSubcriptionByMissionGuidAndClientUidAndUsernameNoMission(String missionGuid, String clientUid, String username) {
		return (MissionSubscription) Hibernate.unproxy(missionSubscriptionRepository.findByMissionGuidAndClientUidAndUsernameNoMission(missionGuid, clientUid, username));
	}

	@Override
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
	public MissionSubscription getMissionSubscriptionByMissionNameAndUsernameNoMission(String missionName, String username) {
		return (MissionSubscription) Hibernate.unproxy(missionSubscriptionRepository.findByMissionNameAndUsernameNoMission(missionName, username));
	}

	@Override
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
    public MissionSubscription getMissionSubscriptionByUidAndMissionNameNoMission(String uid, String missionName) {
		return (MissionSubscription) Hibernate.unproxy(missionSubscriptionRepository.findByUidAndMissionNameNoMission(uid, missionName));
	}

	@Override
	@Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
	public List<MissionSubscription> getMissionSubscriptionsByMissionNameNoMission(String missionName) {
		
		List<MissionSubscription> msl = missionSubscriptionRepository.findAllByMissionNameNoMission(missionName);
		
		if (msl == null) {
			return null;
		}
		
		List<MissionSubscription> result = new ArrayList<>();
		
		for (MissionSubscription ms : msl) {
			result.add((MissionSubscription) Hibernate.unproxy(ms));
		}
		
		return result;
	}
	
	@Override
	@Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
	public List<MissionSubscription> getMissionSubscriptionsByMissionGuidNoMission(UUID missionGuid) {
		
		if (missionGuid == null) {
			throw new IllegalArgumentException("null missionGuid");
		}
		
		List<MissionSubscription> msl = missionSubscriptionRepository.findAllByMissionGuidNoMission(missionGuid.toString());

		if (msl == null) {
			return null;
		}

		List<MissionSubscription> result = new ArrayList<>();

		for (MissionSubscription ms : msl) {
			result.add((MissionSubscription) Hibernate.unproxy(ms));
		}

		return result;

	}

	
	@Override
    public List<MissionSubscription> getMissionSubscriptionsByMissionNameNoMissionNoToken(String missionName) {
		
		List<MissionSubscription> msl = missionSubscriptionRepository.findAllByMissionNameNoMissionNoToken(missionName);
		
		if (msl == null) {
			return null;
		}
		
		List<MissionSubscription> result = new ArrayList<>();
		
		for (MissionSubscription ms : msl) {
			result.add((MissionSubscription) Hibernate.unproxy(ms));
		}
		
		return result;
		
	}
	
	@Override
    public List<MissionSubscription> getMissionSubscriptionsByMissionGuidNoMissionNoToken(UUID missionGuid) {
		
		List<MissionSubscription> msl = missionSubscriptionRepository.findAllByMissionGuidNoMissionNoToken(missionGuid.toString());
		
		if (msl == null) {
			return null;
		}
		
		List<MissionSubscription> result = new ArrayList<>();
		
		for (MissionSubscription ms : msl) {
			result.add((MissionSubscription) Hibernate.unproxy(ms));
		}
		
		return result;
		
	}

	@Override
	public String getMissionNameByGuid(UUID missionGuid) {
		return missionRepository.getMissionNameForMissionGuid(missionGuid);
	}
	

}
