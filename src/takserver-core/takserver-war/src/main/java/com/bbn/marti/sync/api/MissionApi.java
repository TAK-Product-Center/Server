package com.bbn.marti.sync.api;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGcircle;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.maplayer.repository.MapLayerRepository;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.exception.ForbiddenException;
import com.bbn.marti.remote.exception.MissionDeletedException;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.service.RetentionPolicyConfig;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.ExternalMissionData;
import com.bbn.marti.sync.model.LogEntry;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.sync.model.MissionInvitation;
import com.bbn.marti.sync.model.MissionPermission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.model.MissionSubscription;
import com.bbn.marti.sync.model.Resource;
import com.bbn.marti.sync.repository.LogEntryRepository;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.repository.MissionFeedRepository;
import com.bbn.marti.sync.repository.MissionRoleRepository;
import com.bbn.marti.sync.repository.MissionSubscriptionRepository;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.sync.service.MissionTokenUtils;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.KmlUtils;
import com.bbn.marti.util.spring.RequestHolderBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import tak.server.Constants;
import tak.server.ignite.grid.SubscriptionManagerProxyHandler;

/*
 * 
 * REST API for data sync missions
 * 
 */
@RestController
public class MissionApi extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(MissionApi.class);
    
    private static final Logger cacheLogger = LoggerFactory.getLogger("missioncache");

    // keep a reference to the currently active request
    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private SubscriptionManagerLite subscriptionManager;

    @Autowired
    private SubscriptionManagerProxyHandler subscriptionManagerProxy;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private MissionService missionService;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private CommonUtil martiUtil;

    @Autowired
    private RemoteUtil remoteUtil;

    @Autowired
    private Validator validator;

    @Autowired
    private EnterpriseSyncService syncStore;

    @Autowired
    private SubmissionInterface submission;

    @Autowired
    private MissionRoleRepository missionRoleRepository;

    @Autowired
    private MissionSubscriptionRepository missionSubscriptionRepository;

	@Autowired
    private RequestHolderBean requestHolderBean;

	@Autowired
	private MissionFeedRepository missionFeedRepository;

	@Autowired
	private MapLayerRepository mapLayerRepository;

	@Autowired(required = false)
	private RetentionPolicyConfig retentionPolicyConfig;


    /*
     * get all missions
     */
    @RequestMapping(value = "/missions", method = RequestMethod.GET)
    Callable<ApiResponse<List<Mission>>> getAllMissions(
    		@RequestParam(value = "passwordProtected", defaultValue = "false") boolean passwordProtected,
    		@RequestParam(value = "defaultRole", defaultValue = "false") boolean defaultRole,
    		@RequestParam(value = "tool", defaultValue = "public") String tool)
    				throws RemoteException {

    	if (logger.isDebugEnabled()) {
    		logger.debug("mission API getAllMissions");
    	}

    	NavigableSet<Group> groups = martiUtil.getGroupsFromRequest(request);

    	List<Mission> missions = missionService.getAllMissions(passwordProtected, defaultRole, tool, groups);
    	
    	return () -> {

    		return new ApiResponse<List<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), missions);
    	};
    }

    /*
     * get one mission by name
     */
    @RequestMapping(value = "/missions/{name:.+}", method = RequestMethod.GET)
    Callable<ApiResponse<Set<Mission>>> getMission(
    		@PathVariable("name") @NotNull String name,
    		@RequestParam(value = "password", defaultValue = "") String password,
    		@RequestParam(value = "changes", defaultValue = "false") boolean changes,
    		@RequestParam(value = "logs", defaultValue = "false") boolean logs,
    		@RequestParam(value = "secago", required = false) Long secago,
    		@RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
    		@RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end)
    {

    	final HttpServletRequest request = requestHolderBean.getRequest();

    	final String sessionId = requestHolderBean.sessionId();

    	if (logger.isDebugEnabled()) {
    		logger.debug("session id: " + requestHolderBean.sessionId());
    	}

    	if (logger.isDebugEnabled()) {
    		logger.debug("request: " + request);
    	}

		final String groupVector = martiUtil.getGroupVectorBitString(sessionId);
		final NavigableSet<Group> userGroups = martiUtil.getUserGroups(sessionId);

    	return () -> {

    		if (Strings.isNullOrEmpty(name)) {
    			throw new IllegalArgumentException("empty 'name' path parameter");
    		}

    		String missionName = missionService.trimName(name);

    		if (logger.isDebugEnabled()) {
    			logger.debug("getMission " + missionName);
    		}

    		Mission mission = missionService.getMission(missionName, groupVector);

    		try {
    			if (!Strings.isNullOrEmpty(password)) {
    				missionService.validatePassword(mission, password);

    				String token = missionService.generateToken(
    						UUID.randomUUID().toString(), missionName, MissionTokenUtils.TokenType.ACCESS, -1);
    				mission.setToken(token);

    			} else if (!missionService.validatePermission(MissionPermission.Permission.MISSION_READ, request)) {
    				throw new ForbiddenException("Illegal attempt to access mission! Request did not have read access.");
    			}

    			if (changes) {
    				Set<MissionChange> missionChanges = missionService.getMissionChanges(
    						mission.getName(), groupVector, secago, start, end, false);
    				mission.setMissionChanges(missionChanges);
    			}

    			if (logs) {
    				List<LogEntry> missionLogs = missionService.getLogEntriesForMission(mission, secago, start, end);
    				mission.setLogs(missionLogs);
    			}

    			mission.setGroups(RemoteUtil.getInstance().getGroupNamesForBitVectorString(
    					mission.getGroupVector(), userGroups));

    		} catch (ForbiddenException e) {
    			if (missionService.getApiVersionNumberFromRequest(request) <= 2) {
    				throw e;
    			}

    			mission.clear();
    		}

    		Set<Mission> result = new HashSet<>();
    		result.add(mission);
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("GET mission: " + result);
    		}

    		return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), result);
    	};
    }

    /*
     * create a new mission.
     *
     * If the mission already exists, respond with 409 Conflict and duplicate error message.
     *
     */
    @RequestMapping(value = "/missions/{name:.+}", method = RequestMethod.PUT)
    public Callable<ApiResponse<Set<Mission>>> createMission(@PathVariable("name") @NotNull String nameParam,
    		@RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUidParam,
    		@RequestParam(value = "group", defaultValue = "__ANON__") @ValidatedBy("MartiSafeString") String[] groupNamesParam,
    		@RequestParam(value = "description", defaultValue = "") @ValidatedBy("MartiSafeString") String descriptionParam,
    		@RequestParam(value = "chatRoom", defaultValue = "") @ValidatedBy("MartiSafeString") String chatRoomParam,
			@RequestParam(value = "baseLayer", defaultValue = "") @ValidatedBy("MartiSafeString") String baseLayerParam,
		    @RequestParam(value = "bbox", defaultValue = "") @ValidatedBy("MartiSafeString") String bboxParam,
		    @RequestParam(value = "boundingPolygon", defaultValue = "") @ValidatedBy("MartiSafeString") List<String> boundingPolygonParam,
		    @RequestParam(value = "path", defaultValue = "") @ValidatedBy("MartiSafeString") String pathParam,
		    @RequestParam(value = "classification", defaultValue = "") @ValidatedBy("MartiSafeString") String classificationParam,
    		@RequestParam(value = "tool", defaultValue = "public") @ValidatedBy("MartiSafeString") String toolParam,
    		@RequestParam(value = "password", required = false) @ValidatedBy("MartiSafeString") String passwordParam,
    		@RequestParam(value = "defaultRole", required = false) @ValidatedBy("MartiSafeString") MissionRole.Role roleParam,
	        @RequestParam(value = "expiration", required = false) Long expirationParam,
    		@RequestBody(required = false) byte[] requestBody)
    				throws ValidationException, IntrusionException, RemoteException {

    	if (Strings.isNullOrEmpty(nameParam)) {
    		throw new IllegalArgumentException("empty 'name' path parameter");
    	}

    	final HttpServletRequest request = requestHolderBean.getRequest();

    	final String sessionId = requestHolderBean.sessionId();

    	if (logger.isDebugEnabled()) {
    		logger.debug("session id: " + requestHolderBean.sessionId());
    	}

    	if (logger.isDebugEnabled()) {
    		logger.debug("request: " + request);
    	}

		final String groupVectorUser = martiUtil.getGroupVectorBitString(sessionId);

    	final MissionRole adminRole = martiUtil.isAdmin() ?
				missionRoleRepository.findFirstByRole(MissionRole.Role.MISSION_OWNER) : null;

    	final String username = SecurityContextHolder.getContext().getAuthentication().getName();

		return () -> {

			String creatorUid = creatorUidParam;
			String[] groupNames = groupNamesParam;
			String description = descriptionParam;
			String chatRoom = chatRoomParam;
			String baseLayer = baseLayerParam;
			String bbox = bboxParam;
			String boundingPolygon = boundingPolygonPointsToString(boundingPolygonParam);
			String path = pathParam;
			String classification = classificationParam;
			String tool = toolParam;
			String password = passwordParam;
			MissionRole.Role role = roleParam;
			Long expiration = expirationParam;

			byte[] missionPackage = null;

			Mission reqMission = null;
			String contentType = request.getHeader("content-type");
			if (contentType != null && contentType.toLowerCase().contains("application/json")) {
				try {
					ObjectMapper objectMapper = new ObjectMapper().configure(
							DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
					reqMission = objectMapper.readValue(new String(requestBody), Mission.class);
					if (reqMission != null) {
						description = reqMission.getDescription() != null ? reqMission.getDescription() : description;
						chatRoom = reqMission.getChatRoom() != null ? reqMission.getChatRoom() : chatRoom;
						baseLayer = reqMission.getBaseLayer() != null ? reqMission.getBaseLayer() : baseLayer;
						bbox = reqMission.getBbox() != null ? reqMission.getBbox() : bbox;
						boundingPolygon = reqMission.getBoundingPolygon() != null ? reqMission.getBoundingPolygon() : boundingPolygon;
						path = reqMission.getPath() != null ? reqMission.getPath() : path;
						classification = reqMission.getClassification() != null ? reqMission.getClassification() : classification;
						tool = reqMission.getTool() != null ? reqMission.getTool() : tool;
						role = reqMission.getDefaultRole() != null ? reqMission.getDefaultRole().getRole() : role;
						expiration = reqMission.getExpiration() != null ? reqMission.getExpiration() : expiration;
						groupNames = reqMission.getGroups() != null ? reqMission.getGroups().toArray(new String[0]) : groupNames;
					}
				} catch (JsonProcessingException e) {
					logger.error("exception parsing mission json!", e);
					throw new IllegalArgumentException("exception parsing mission json!");
				}
			} else {
				missionPackage = requestBody;
			}

			BigInteger bitVectorUser = remoteUtil.bitVectorStringToInt(groupVectorUser);

    		Set<Group> groups = groupManager.findGroups(Arrays.asList(groupNames));
    		String groupVectorMission = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));
    		BigInteger bitVectorMission = remoteUtil.bitVectorStringToInt(groupVectorMission);

			if (bitVectorUser.compareTo(BigInteger.ZERO) == 0) {
				throw new ForbiddenException("Missing groups for user!");
			}

			if (bitVectorMission.compareTo(BigInteger.ZERO) == 0) {
				throw new ForbiddenException("Missing groups for mission!");
			}

			// ensure that the user has access to all groups the mission is being added to
    		if (bitVectorUser.and(bitVectorMission).compareTo(bitVectorMission) != 0) {
    			throw new ForbiddenException("Illegal attempt to set groupVector for Mission!");
    		}

    		// validate this differently since it's a path variable
    		validator.getValidInput(context, nameParam, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);

    		validateParameters(new Object() {}.getClass().getEnclosingMethod());

    		MissionRole defaultRole = null;
    		if (role != null) {
    			defaultRole = missionRoleRepository.findFirstByRole(role);
    		}

    		String name = missionService.trimName(nameParam);

    		Mission mission;
    		try {

				mission = missionService.getMission(name, groupVectorUser);

				// make sure the user has write permissions
				if (!missionService.validatePermission(MissionPermission.Permission.MISSION_WRITE, request)) {
					throw new ForbiddenException("Illegal attempt to update mission!");
				}

				boolean updated = false;

				if (!StringUtils.equals(description, mission.getDescription())) {
					mission.setDescription(description);
					updated = true;
				}

				if (!StringUtils.equals(chatRoom, mission.getChatRoom())) {
					mission.setChatRoom(chatRoom);
					updated = true;
				}

				if (!StringUtils.equals(baseLayer, mission.getBaseLayer())) {
					mission.setBaseLayer(baseLayer);
					updated = true;
				}

				if (!StringUtils.equals(bbox, mission.getBbox())) {
					mission.setBbox(bbox);
					updated = true;
				}
				
				if (!StringUtils.equals(boundingPolygon, mission.getBoundingPolygon())) {
					mission.setBoundingPolygon(boundingPolygon);
					updated = true;
				}

				if (!StringUtils.equals(path, mission.getPath())) {
					mission.setPath(path);
					updated = true;
				}

				if (!StringUtils.equals(classification, mission.getClassification())) {
					mission.setClassification(classification);
					updated = true;
				}

				if (!(expiration == null ?
						mission.getExpiration() == null || mission.getExpiration() == -1L :
						mission.getExpiration() != null && expiration.equals(mission.getExpiration()))) {
					mission.setExpiration(expiration);
					updated = true;
				}

				if (updated) {
					if (expiration != null) {
						missionRepository.update(name, groupVectorUser, description, chatRoom, baseLayer, bbox, path, classification, expiration, boundingPolygon);
					} else {
						missionRepository.update(name, groupVectorUser, description, chatRoom, baseLayer, bbox, path, classification, -1L, boundingPolygon);
					}
				}

    			if (password != null && password.length() > 0) {
    				if (!missionService.validatePermission(MissionPermission.Permission.MISSION_SET_PASSWORD, request)) {
    					throw new ForbiddenException("Illegal attempt to update mission password!");
    				}

    				String newPasswordHash = BCrypt.hashpw(password, BCrypt.gensalt());

    				if (!StringUtils.equals(newPasswordHash, mission.getPasswordHash())) {
						missionRepository.setPasswordHash(name, newPasswordHash, groupVectorMission);
						mission.setPasswordHash(newPasswordHash);
						updated = true;
					}
    			}

    			if (defaultRole != null) {

    				MissionRole tokenRole = adminRole != null ? adminRole :
							missionService.getRoleFromToken(mission, new MissionTokenUtils.TokenType[]{
									MissionTokenUtils.TokenType.SUBSCRIPTION
							}, request);
    				if (tokenRole == null) {
    					throw new ForbiddenException("Could not extract role from token to set mission default role!");
    				}

    				// ensure that the token roles grants the MISSION_SET_ROLE permission
    				if (!tokenRole.hasPermission(MissionPermission.Permission.MISSION_SET_ROLE)) {
    					throw new ForbiddenException(
    							"Illegal attempt to update default mission role with insufficient permissions!");
    				}

    				// ensure that the token role contains all permissions that are being set as defaults
    				if (!tokenRole.hasAllPermissions(defaultRole)) {
    					throw new ForbiddenException(
    							"Illegal attempt to update default mission role beyond current permissions!");
    				}

    				if (mission.getDefaultRole() == null || mission.getDefaultRole().compareTo(defaultRole) != 0) {
						mission.setDefaultRole(defaultRole);
						missionRepository.setDefaultRoleId(name, defaultRole.getId(), groupVectorMission);
						updated = true;
					}
    			}

				// does the current user have permission to update groups?
				if (missionService.validatePermission(MissionPermission.Permission.MISSION_UPDATE_GROUPS, request)) {
					// did the groups change?
					if (mission.getGroupVector().compareTo(groupVectorMission) != 0) {
						missionRepository.updateGroups(name, groupVectorUser, groupVectorMission);
						updated = true;
					}
				}

				if (reqMission != null && createOrUpdateMissionRequestBody(mission, reqMission, creatorUid)) {
					updated = true;
				}

				if (updated) {
					missionService.invalidateMissionCache(name);

					try {
						subscriptionManager.broadcastMissionAnnouncement(name, groupVectorMission, creatorUid,
								SubscriptionManagerLite.ChangeType.METADATA, mission.getTool());
					} catch (Exception e) {
						logger.debug("exception announcing mission change " + e.getMessage(), e);
					}
				}

    			response.setStatus(HttpServletResponse.SC_OK);

    		} catch (MissionDeletedException | NotFoundException e) {

    			String passwordHash = null;
    			if (!Strings.isNullOrEmpty(password)) {
    				passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
    			}

    			// For now there are no properties to be concerted with besides name, so PUT missions doesn't need a JSON body yet
				if (expiration != null) {
					mission = missionService.createMission(
							name, creatorUid, groupVectorMission, description, chatRoom, baseLayer, bbox, path, classification, tool, passwordHash, defaultRole, expiration, boundingPolygon);
				} else {
					mission = missionService.createMission(
							name, creatorUid, groupVectorMission, description, chatRoom, baseLayer, bbox, path, classification, tool, passwordHash, defaultRole, -1L, boundingPolygon);
				}

    			MissionRole ownerRole = missionRoleRepository.findFirstByRole(MissionRole.Role.MISSION_OWNER);
    			MissionSubscription ownerSubscription = missionService.missionSubscribe(
    					name, mission.getId(), creatorUid, username, ownerRole, groupVectorUser);
    			mission.setToken(ownerSubscription.getToken());
    			mission.setOwnerRole(ownerRole);

    			if (missionPackage != null) {
    				List<MissionChange> conflicts = new ArrayList<>();
    				missionService.addMissionPackage(
							name, missionPackage, creatorUid, martiUtil.getGroupsFromRequest(request), conflicts);
    				mission = missionService.getMission(name, groupVectorUser);
    			}

    			if (reqMission != null) {
					createOrUpdateMissionRequestBody(mission, reqMission, creatorUid);
				}

    			response.setStatus(mission.getId() != 0 ? HttpServletResponse.SC_CREATED
    					: HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    		}

    		return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), Sets.newHashSet(mission));
    	};

    }

	private boolean createOrUpdateMissionRequestBody(Mission mission, Mission reqMission, String creatorUid) {

    	if (reqMission == null) {
    		return false;
		}

    	boolean updated = false;
		if (reqMission.getMapLayers() != null) {

			if (mission.getMapLayers() != null && mission.getMapLayers().size() > 0) {
				mapLayerRepository.deleteAllByMissionId(mission.getId());
				mission.getMapLayers().clear();
				updated = true;
			}

			for (MapLayer mapLayer : reqMission.getMapLayers()) {
				mission.getMapLayers().add(mapLayer);
				mapLayer.setMission(mission);
				mapLayer.setCreatorUid(creatorUid);
				missionService.addMapLayerToMission(mission.getName(), creatorUid, mission, mapLayer);
			}
		}

		if (reqMission.getFeeds() != null) {

			if (mission.getFeeds() != null && mission.getFeeds().size() > 0) {
				missionFeedRepository.deleteAllByMissionId(mission.getId());
				mission.getFeeds().clear();
				updated = true;
			}

			for (MissionFeed missionFeed : reqMission.getFeeds()) {

				missionFeed = missionService.addFeedToMission(mission.getName(), creatorUid, mission,
						missionFeed.getDataFeedUid(), missionFeed.getFilterBbox(),
						missionFeed.getFilterType(), missionFeed.getFilterCallsign());

				mission.getFeeds().add(missionFeed);
			}
		}

		return updated;
	}

	private void copyMissionContainers(Mission origMission, Mission missionCopy, String creatorUid, String groupVector) {

    	try {

			String missionCopyName = missionCopy.getName();

			for (String keyword : origMission.getKeywords()) {
				missionRepository.addMissionKeyword(missionCopy.getId(), keyword);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("copy key words " + origMission.getKeywords());
			}

			for (MapLayer mapLayer : origMission.getMapLayers()) {
				MapLayer mapLayerCopy = new MapLayer(mapLayer);
				missionCopy.getMapLayers().add(mapLayerCopy);
				mapLayerCopy.setMission(missionCopy);
				mapLayerCopy.setCreatorUid(creatorUid);
				mapLayerCopy.setMission(missionCopy);
				missionService.addMapLayerToMission(missionCopy.getName(), creatorUid, missionCopy, mapLayerCopy);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("copy map layers " + origMission.getMapLayers());
			}

			for (MissionFeed missionFeed : origMission.getFeeds()) {
				missionFeed = missionService.addFeedToMission(missionCopyName, creatorUid, missionCopy,
						missionFeed.getDataFeedUid(), missionFeed.getFilterBbox(),
						missionFeed.getFilterType(), missionFeed.getFilterCallsign());
				missionCopy.getFeeds().add(missionFeed);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("copy mission feeds " + origMission.getFeeds());
			}

			for (ExternalMissionData externalMissionData : origMission.getExternalData()) {
				externalMissionData = missionService.setExternalMissionData(missionCopyName, creatorUid, externalMissionData, groupVector);
				missionCopy.getExternalData().add(externalMissionData);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("copy external data" + origMission.getExternalData());
			}

			for (Resource resource : origMission.getContents()) {
				MissionContent mc = new MissionContent();
				mc.getHashes().add(resource.getHash());
				missionService.addMissionContent(missionCopyName, mc, creatorUid, groupVector);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("copied mission content " + origMission.getContents());
			}

			for (String uid : origMission.getUids()) {
				missionCopy.getUids().add(uid);

				MissionContent mc = new MissionContent();
				mc.getUids().add(uid);
				missionService.addMissionContent(missionCopyName, mc, creatorUid, groupVector);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("copied mission uids " + origMission.getUids());
			}

			for (LogEntry logEntry : missionService.getLogEntriesForMission(origMission, null, null, null)) {
				logEntry.getMissionNames().add(missionCopyName);
				missionService.addUpdateLogEntry(logEntry, new Date(), groupVector);
			}

		} catch (Exception e) {
    		logger.error("exception in copyMissionContainers", e);
    		throw e;
		}
	}

	@PreAuthorize("hasPermission(#request, 'MISSION_READ')")
	@RequestMapping(value = "/missions/{missionName:.+}/copy", method = RequestMethod.PUT)
	Callable<ApiResponse<Set<Mission>>> copyMission(
			@PathVariable("missionName") @NotNull String missionName,
			@RequestParam(value = "creatorUid", required = true) @ValidatedBy("MartiSafeString") String creatorUidParam,
			@RequestParam(value = "copyName", required = true) @ValidatedBy("MartiSafeString") String copyName,
			@RequestParam(value = "copyPath", required = false) @ValidatedBy("MartiSafeString") String copyPath,
			@RequestParam(value = "defaultRole", required = false) @ValidatedBy("MartiSafeString") MissionRole.Role roleParam,
			@RequestParam(value = "password", required = false) String passwordParam)
	{

		if (Strings.isNullOrEmpty(missionName)) {
			throw new IllegalArgumentException("empty 'name' path parameter");
		}

		if (Strings.isNullOrEmpty(copyName)) {
			throw new IllegalArgumentException("empty 'copyName' request parameter");
		}

		final HttpServletRequest request = requestHolderBean.getRequest();
		final String sessionId = requestHolderBean.sessionId();

		if (logger.isDebugEnabled()) {
			logger.debug("session id: " + requestHolderBean.sessionId());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("request: " + request);
		}

		final String groupVector = martiUtil.getGroupVectorBitString(sessionId);
		final String username = SecurityContextHolder.getContext().getAuthentication().getName();

		return () -> {
			String name = missionService.trimName(missionName);
			String copy = missionService.trimName(copyName);

			if (logger.isDebugEnabled()) {
				logger.debug("copyMission " + missionName + " copy name " + copy);
			}

			// find the mission for which to copy
			Mission mission = null;
			mission = missionService.getMission(missionName, groupVector);

			if (mission == null) {
				if (logger.isDebugEnabled()) {
					logger.debug(" mission to copy was not found " + name);
				}
			}

			// see if the copy mission already exists
			if (missionService.getMission(copy, false) != null) {
				throw new IllegalArgumentException("mission named " + copy + " already exists");
			}

			MissionRole defaultRole = null;
			if (roleParam != null) {
				defaultRole = missionRoleRepository.findFirstByRole(roleParam);
			}

			String passwordHash = null;
			if (!Strings.isNullOrEmpty(passwordParam)) {
				passwordHash = BCrypt.hashpw(passwordParam, BCrypt.gensalt());
			}

			// create a mission from the existing mission
			Mission missionCopy = missionService.createMission(copy, creatorUidParam, mission.getGroupVector(), mission.getDescription(),
					mission.getChatRoom(), mission.getBaseLayer(), mission.getBbox(), copyPath != null ? copyPath : mission.getPath(), mission.getClassification(),
					mission.getTool(), passwordHash, defaultRole, mission.getExpiration(), mission.getBoundingPolygon());

			if (logger.isDebugEnabled()) {
				logger.debug("mission copy created " +  missionCopy);
			}

			MissionRole ownerRole = missionRoleRepository.findFirstByRole(MissionRole.Role.MISSION_OWNER);
			MissionSubscription ownerSubscription = missionService.missionSubscribe(copy, missionCopy.getId(),
					creatorUidParam, username, ownerRole, groupVector);

			if (missionCopy.getId() != null) {
				copyMissionContainers(mission, missionCopy, creatorUidParam, groupVector);
			}

			missionCopy.setToken(ownerSubscription.getToken());
			missionCopy.setOwnerRole(ownerRole);

			response.setStatus((missionCopy != null && missionCopy.getId() != 0 )? HttpServletResponse.SC_CREATED
					: HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), Sets.newHashSet(missionCopy));
		};
	}

	/*
     * Delete a mission by name. Respond with the deleted mission JSON.
     *
     */
	@PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
	@RequestMapping(value = "/missions/{name:.+}", method = RequestMethod.DELETE)
	@Transactional(noRollbackFor = Exception.class)
	ApiResponse<Set<Mission>> deleteMission(
			@PathVariable("name") @NotNull String name,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
			@RequestParam(value = "deepDelete", defaultValue = "false") boolean deepDelete,
            HttpServletRequest request
    ) throws ValidationException, IntrusionException {

        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("empty 'name' path parameter");
        }

        logger.debug("delete mission " + name + " " + creatorUid);

        // validate this differently since it's a path variable
        validator.getValidInput(context, name, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);

        validateParameters(new Object() {}.getClass().getEnclosingMethod());

        String groupVector = martiUtil.getGroupVectorBitString(request);

        logger.trace("group vector " + groupVector);

		Mission mission = missionService.getMissionByNameCheckGroups(name, groupVector);

		if (logger.isDebugEnabled()) {
			logger.debug("mission to delete: " + mission);
		}

		missionService.validateMission(mission, name);

        if (deepDelete) {
            MissionRole role = missionService.getRoleForRequest(mission, request);
            if (role == null) {
                throw new IllegalArgumentException("no role for request!");
            }

            if (!role.hasPermission(MissionPermission.Permission.MISSION_DELETE)) {
                String msg = "Attempt to deepDelete mission: " + name + ", by unauthorized user: " + creatorUid;
                logger.error(msg);
                throw new ForbiddenException(msg);
            }
        }

			logger.debug("archiving mission");

        byte[] archive = missionService.archiveMission(mission.getName(), groupVector, request.getServerName());
        missionService.addMissionArchiveToEsync(mission.getName(), archive, groupVector, true);

			logger.debug("added archived mission to esync " + mission.getName());

        mission = missionService.deleteMission(name, creatorUid, groupVector, deepDelete);

        logger.debug("mission deleted");

        Set<Mission> result = new HashSet<>();

        result.add(mission);

        return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), result);
    }

    /*
     * Packages up and returns the requested mission as a mission package
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{name:.+}/archive", method = RequestMethod.GET)
    byte[] getMissionArchive(@PathVariable("name") @NotNull String name, HttpServletRequest request) throws ValidationException, IntrusionException {

        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("empty 'name' path parameter");
        }

        // validate this differently since it's a path variable
        validator.getValidInput(context, name, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);

        validateParameters(new Object() {}.getClass().getEnclosingMethod());

        String groupVector = martiUtil.getGroupVectorBitString(request);
		Mission mission = missionService.getMissionByNameCheckGroups(name, groupVector);
		missionService.validateMission(mission, name);

        byte[] archive = missionService.archiveMission(name, groupVector, request.getServerName());

        response.addHeader(
                "Content-Disposition",
                "attachment; filename=" + name + ".zip");

        return archive;
    }

    /*
     * Send a mission package to a list of contacts
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{name:.+}/send", method = RequestMethod.POST)
    ApiResponse<Set<Mission>> sendMissionArchive(@PathVariable("name") @NotNull String missionName, HttpServletRequest request) throws ValidationException, IntrusionException {

        if (Strings.isNullOrEmpty(missionName)) {
            throw new IllegalArgumentException("empty mission name");
        }

        missionName = missionService.trimName(missionName);

		Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

        String[] contactUids = request.getParameterValues("contacts");

        if (contactUids == null || contactUids.length == 0) {
            throw new IllegalArgumentException("empty contacts array");
        }

        // validate the uids before sending on to the subscriptionManager
        for (String uid : contactUids) {
            validator.getValidInput(context, uid, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
        }
        validateParameters(new Object() {}.getClass().getEnclosingMethod());

        String groupVector = martiUtil.getGroupVectorBitString(request);

        byte[] archive = missionService.archiveMission(missionName, groupVector, request.getServerName());

        String shaHash = missionService.addMissionArchiveToEsync(missionName, archive, groupVector, false);

        String requestUrl = request.getRequestURL().toString();
        String url = requestUrl.substring(0, requestUrl.indexOf(request.getServletPath()))
                + "/Marti/sync/content?hash=" + shaHash; // yes: uid will be set to the shaHash in the CoT message (see below)

        // Generate the CoT message
        String cotMessage = CommonUtil.getFileTransferCotMessage(
                /*String uid*/ shaHash, // yes: set the UID to be the hash, this makes it consistent with ATAK-generated mission packages
                /*String shaHash*/ shaHash,
                /*String callsign*/  SecurityContextHolder.getContext().getAuthentication().getName(),
                /*String filename*/ missionName + ".zip",
                /*String url*/ url,
                /*long sizeInBytes*/ archive.length,
                /*String[] contacts*/ contactUids);

        try {
            submission.submitCot(cotMessage, martiUtil.getGroupsFromRequest(request));
        } catch (Exception e) {
            throw new TakException(e);
        }

        Set<Mission> result = new HashSet<>();
        result.add(mission);
        return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), result);
    }

    /*
     * add single or multiple mission content, by hash or UID
     *
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/contents", method = RequestMethod.PUT)
    public Callable<ApiResponse<Set<Mission>>> addMissionContent(@PathVariable("name") String name,
    		@RequestBody MissionContent content,
    		@RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid)
    				throws ValidationException, IntrusionException {

    	final String sessionId = requestHolderBean.sessionId();

		final String groupVector = martiUtil.getGroupVectorBitString(sessionId);

    	return () -> {

    		// validate this differently since it's a path variable
    		validator.getValidInput(context, name, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);

    		validateParameters(new Object() {}.getClass().getEnclosingMethod());

    		if (content.getHashes().isEmpty() && content.getUids().isEmpty()) {
    			throw new IllegalArgumentException("at least one hash or uid must be provided in request");
    		}

    		Mission mission = missionService.addMissionContent(name, content, creatorUid, groupVector);

    		Set<Mission> result = new HashSet<>();
    		result.add(mission);

    		return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), result);
    	};

    }

    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/contents/missionpackage", method = RequestMethod.PUT)
    public ApiResponse<List<MissionChange>> addMissionPackage(@PathVariable("name") String name,
                                                              @RequestBody byte[] missionPackage,
                                                              @RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
                                                              HttpServletRequest request)
            throws ValidationException, IntrusionException, RemoteException {

        // validate this differently since it's a path variable
        validator.getValidInput(context, name, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);

        validateParameters(new Object() {}.getClass().getEnclosingMethod());

		Mission mission = missionService.getMissionByNameCheckGroups(name, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, name);

        List<MissionChange> conflicts = new ArrayList<>();

        boolean success = missionService.addMissionPackage(
                name, missionPackage, creatorUid, martiUtil.getGroupsFromRequest(request), conflicts);

        if (!success) {
            if (conflicts.size() > 0) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        return new ApiResponse<List<MissionChange>>(Constants.API_VERSION, MissionChange.class.getSimpleName(), conflicts);
    }

    /*
     * remove mission content by hash or uid
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/contents", method = RequestMethod.DELETE)
    ApiResponse<Set<Mission>> removeMissionContent(@PathVariable("name") String name,
                                                   @RequestParam(value = "hash", required = false) @ValidatedBy("MartiSafeString") String hash,
                                                   @RequestParam(value = "uid", required = false) @ValidatedBy("MartiSafeString") String uid,
                                                   @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
                                                   HttpServletRequest request) throws ValidationException, IntrusionException {

        // validate this differently since it's a path variable
        validator.getValidInput(context, name, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);

        validateParameters(new Object() {}.getClass().getEnclosingMethod());

        // remove the content and track change
        Mission mission = missionService.deleteMissionContent(name, hash, uid, creatorUid, martiUtil.getGroupVectorBitString(request));

        // return mission object without resource and uid list (since the query could be expensive)
        return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), Sets.newHashSet(mission));
    }

    /*
     * get the content change set for a missions given a time period
     *
     * parameters are seconds ago or {start, end} timespan
     *
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{name:.+}/changes", method = RequestMethod.GET)
    ApiResponse<Set<MissionChange>> getMissionChanges(
            @PathVariable("name") @NotNull String name,
            @RequestParam(value = "secago", required = false) Long secago,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end,
            @RequestParam(value = "squashed", required = false, defaultValue = "true") boolean squashed,
            HttpServletRequest request) {

    	try {
		Mission mission = missionService.getMissionByNameCheckGroups(missionService.trimName(name), martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionService.trimName(name));

        Set<MissionChange> changes = missionService.getMissionChanges(name, martiUtil.getGroupVectorBitString(request),
                secago, start, end, squashed);

        return new ApiResponse<Set<MissionChange>>(Constants.API_VERSION, MissionChange.class.getSimpleName(),
                new ConcurrentSkipListSet<>(changes));
		} catch (Exception e) {
    		logger.error("exception in getMissionChanges", e);
    		return null;
		}
    }


    /*
     * mission keywords
     *
     */

    /*
     * remove all keywords for a mission
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/keywords", method = RequestMethod.DELETE)
    ApiResponse<Set<Mission>> clearKeywords(
            @PathVariable("name") @NotNull String name,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("empty 'name' path parameter");
        }

        Mission mission = missionService.getMission(missionService.trimName(name), martiUtil.getGroupVectorBitString(request));

        // remove all keywords
        mission.getKeywords().clear();

        missionRepository.removeAllKeywordsForMission(mission.getId());

        missionService.invalidateMissionCache(name);

        try {
            subscriptionManager.broadcastMissionAnnouncement(name, mission.getGroupVector(), creatorUid,
                    SubscriptionManagerLite.ChangeType.KEYWORD, mission.getTool());
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }

        return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), Sets.newHashSet(mission));
    }

    /*
     * sets the keywords for the mission
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/keywords", method = RequestMethod.PUT)
    ApiResponse<Set<Mission>> setKeywords(
            @PathVariable("name") @NotNull String name,
            @RequestBody List<String> keywords,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("empty 'name' path parameter");
        }

        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("empty keywords array");
        }

        Mission mission = missionService.getMission(missionService.trimName(name), martiUtil.getGroupVectorBitString(request));

        // remove all keywords
        mission.getKeywords().clear();

        missionRepository.removeAllKeywordsForMission(mission.getId());

		// do validation
		try {
			for (String keyword : keywords) {
				validator.getValidInput(context, keyword, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
			}
		} catch (ValidationException | IntrusionException e) {
			throw new com.bbn.marti.remote.exception.ValidationException("invalid keyword!", e);
		}

        // add keywords
        mission.getKeywords().addAll(keywords);

        for (String keyword : keywords) {
            try {
                missionRepository.addMissionKeyword(mission.getId(), keyword);
            } catch (DataIntegrityViolationException e) {
                logger.debug("can't add duplicate keyword " + keyword);
            }
        }

        missionService.invalidateMissionCache(name);

        try {
            subscriptionManager.broadcastMissionAnnouncement(name, mission.getGroupVector(), creatorUid,
                    SubscriptionManagerLite.ChangeType.KEYWORD, mission.getTool());
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }

        return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), Sets.newHashSet(mission));
    }

    /*
     * remove one keyword from a mission
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/keywords/{keyword}", method = RequestMethod.DELETE)
    ApiResponse<Set<Mission>> removeKeyword(
            @PathVariable("name") @NotNull String name,
            @PathVariable("keyword") String keyword,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("empty 'name' path parameter");
        }

        if (Strings.isNullOrEmpty(keyword)) {
            throw new IllegalArgumentException("keyword to delete must be provided in URL");
        }

        Mission mission = missionService.getMission(missionService.trimName(name), martiUtil.getGroupVectorBitString(request));

        if (!mission.getKeywords().remove(keyword)) {
            throw new IllegalArgumentException("mission '" + name + "' did not contain keyword " + keyword);
        }

        missionRepository.removeMissionKeyword(mission.getId(), keyword);

        missionService.invalidateMissionCache(name);

        try {
            subscriptionManager.broadcastMissionAnnouncement(name, mission.getGroupVector(), creatorUid,
                    SubscriptionManagerLite.ChangeType.KEYWORD, mission.getTool());
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }

        return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), Sets.newHashSet(mission));
    }


    /*
     * add one or more keywords to a mission uid
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/uid/{uid:.+}/keywords", method = RequestMethod.PUT)
    void addUidKeyword(
            @PathVariable("name") @NotNull @ValidatedBy("MartiSafeString") String name,
            @PathVariable("uid") @NotNull @ValidatedBy("MartiSafeString") String uid,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            @RequestBody List<String> keywords, HttpServletRequest request) {

        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("empty keywords array");
        }

        Mission mission = missionService.getMission(missionService.trimName(name), martiUtil.getGroupVectorBitString(request));
        missionService.validateMission(mission, name);

        missionRepository.removeAllKeywordsForMissionUid(mission.getId(), uid);

        StringWriter changes = new StringWriter();
        changes.append("<uidKeywords uid=\"" + uid + "\">");

        for (String keyword : keywords) {
            try {
                missionRepository.addMissionUidKeyword(mission.getId(), uid, keyword);
                changes.append("<uidKeyword keyword=\"" + keyword + "\" />");
            } catch (DataIntegrityViolationException e) {
                logger.debug("can't add duplicate keyword " + keyword);
            }
        }

        changes.append("</uidKeywords>");

        missionService.invalidateMissionCache(name);

        try {
            subscriptionManager.announceMissionChange(name, SubscriptionManagerLite.ChangeType.UID_KEYWORD,
                    creatorUid, mission.getTool(), changes.toString());
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }
    }

    /*
     * remove all keywords for a mission uid
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/uid/{uid:.+}/keywords", method = RequestMethod.DELETE)
    void clearUidKeywords(
            @PathVariable("name") @NotNull @ValidatedBy("MartiSafeString") String name,
            @PathVariable("uid") @NotNull @ValidatedBy("MartiSafeString") String uid,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

        Mission mission = missionService.getMission(missionService.trimName(name), martiUtil.getGroupVectorBitString(request));
        missionService.validateMission(mission, name);

        missionRepository.removeAllKeywordsForMissionUid(mission.getId(), uid);

        missionService.invalidateMissionCache(name);

        try {
            subscriptionManager.announceMissionChange(name, SubscriptionManagerLite.ChangeType.UID_KEYWORD,
                    creatorUid, mission.getTool(), "<uidKeywords uid=\"" + uid + "\"/>");
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }
    }

    /*
     * add one or more keywords to a mission resource
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/content/{hash:.+}/keywords", method = RequestMethod.PUT)
    void addContentKeyword(
            @PathVariable("name") @NotNull @ValidatedBy("MartiSafeString") String name,
            @PathVariable("hash") @NotNull @ValidatedBy("MartiSafeString") String hash,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            @RequestBody List<String> keywords,
            HttpServletRequest request) {

        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("empty keywords array");
        }

        Mission mission = missionService.getMission(missionService.trimName(name), martiUtil.getGroupVectorBitString(request));
        missionService.validateMission(mission, name);

        missionRepository.removeAllKeywordsForMissionResource(mission.getId(), hash);

        StringWriter changes = new StringWriter();
        changes.append("<contentKeywords hash=\"" + hash + "\">");

        for (String keyword : keywords) {
            try {
                missionRepository.addMissionResourceKeyword(mission.getId(), hash, keyword);
                changes.append("<contentKeyword keyword=\"" + keyword + "\" />");
            } catch (DataIntegrityViolationException e) {
                logger.debug("can't add duplicate keyword " + keyword);
            }
        }

        changes.append("</contentKeywords>");

        missionService.invalidateMissionCache(name);

        try {
            subscriptionManager.announceMissionChange(name, SubscriptionManagerLite.ChangeType.RESOURCE_KEYWORD,
                    creatorUid, mission.getTool(), changes.toString());
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }
    }

    /*
     * remove all keywords for a mission uid
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name:.+}/content/{hash:.+}/keywords", method = RequestMethod.DELETE)
    void clearContentKeywords(
            @PathVariable("name") @NotNull @ValidatedBy("MartiSafeString") String name,
            @PathVariable("hash") @NotNull @ValidatedBy("MartiSafeString") String hash,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

        Mission mission = missionService.getMission(missionService.trimName(name), martiUtil.getGroupVectorBitString(request));
        missionService.validateMission(mission, name);

        missionRepository.removeAllKeywordsForMissionResource(mission.getId(), hash);

        missionService.invalidateMissionCache(name);

        try {
            subscriptionManager.announceMissionChange(name, SubscriptionManagerLite.ChangeType.RESOURCE_KEYWORD,
                    creatorUid, mission.getTool(), "<contentKeywords />");
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }
    }


    private static final int DEFAULT_PARAMETER_LENGTH = 1024;

    /*
     * Enterprise Sync search
     *
     * Search the resource table for enterprise sync / mission files
     */
    @RequestMapping(value = "/sync/search", method = RequestMethod.GET)
    ApiResponse<NavigableSet<Resource>> searchSync(
            @RequestParam(value = "box", required = false) @ValidatedBy("Coordinates") String box,
            @RequestParam(value = "circle", required = false) @ValidatedBy("Coordinates") String circle,
            @RequestParam(value = "startTime", required = false) @ValidatedBy("MartiSafeString") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(value = "endTime", required = false) @ValidatedBy("MartiSafeString") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end,
            @RequestParam(value = "minAltitude", required = false) @ValidatedBy("Double") Double minAltitude,
            @RequestParam(value = "maxAltitude", required = false) @ValidatedBy("Double") Double maxAltitude,
            @RequestParam(value = "filename", required = false) @ValidatedBy("RestrictedRegex") String filename,
            @RequestParam(value = "keyword", required = false) @ValidatedBy("RestrictedRegex") List<String> keywords,
            @RequestParam(value = "mimetype", required = false) @ValidatedBy("RestrictedRegex") String mimeType,
            @RequestParam(value = "name", required = false) @ValidatedBy("RestrictedRegex") String name,
            @RequestParam(value = "uid", required = false) @ValidatedBy("RestrictedRegex") String uid,
            @RequestParam(value = "hash", required = false) @ValidatedBy("RestrictedRegex") String hash,
            @RequestParam(value = "mission", required = false) @ValidatedBy("RestrictedRegex") String missionName,
            @RequestParam(value = "tool", required = false) @ValidatedBy("RestrictedRegex") String tool,
            HttpServletRequest request
    ) {

        String groupVector = null;

        try {
            // Get group vector for the user associated with this session
            groupVector = martiUtil.getGroupBitVector(request);
            logger.trace("groups bit vector: " + groupVector);
        } catch (Exception e) {
            logger.debug("exception getting group membership for current web user " + e.getMessage());
        }

        if (Strings.isNullOrEmpty(groupVector)) {
            throw new IllegalStateException("empty group vector");
        }

        // init audit log if required
        AuditLogUtil.init(request);

        // get the currently executing method and validate its arguments. Luckily the http request param list takes care of the problem that Java lacks named parameters!
        // Validation exceptions will be propagated to custom exception handler level and trigger an appropriate HTTP response, and error payload

        validateParameters(new Object() {}.getClass().getEnclosingMethod());

        logger.debug("keywords: " + keywords);

        logger.debug("box: " + box);

        logger.debug("circle: " + circle);

        logger.debug("startTime: " + start);

        logger.debug("endTime: " + end);

        if (!Strings.isNullOrEmpty(missionName)) {
            missionName = missionService.trimName(missionName);
        }

        logger.debug("mission name: " + missionName);

        // Use Postgres PGobject for geospatial
        PGobject spatialConstraint = null;
        if (!Strings.isNullOrEmpty(box) && !Strings.isNullOrEmpty(circle)) {
            throw new IllegalArgumentException("Both circle and box specified");
        }

        if (!Strings.isNullOrEmpty(box)) {
            Double[] coordinates = KmlUtils.parseSpatialCoordinates(box);
            spatialConstraint = new PGbox(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
        }

        if (!Strings.isNullOrEmpty(circle)) {
            Double[] coordinates = KmlUtils.parseSpatialCoordinates(circle);
            spatialConstraint = new PGcircle(coordinates[0], coordinates[1], coordinates[2]);
        }

        logger.debug("Spatial constraint: " + spatialConstraint);

        NavigableSet<Resource> results = new ConcurrentSkipListSet<>();

        // also process search constraints that are not special enough to have their own arguments
        Map<Metadata.Field, String> constraints = new HashMap<>();

        // note: can't search by filename - using name since it appears to usually be that
        if (!Strings.isNullOrEmpty(filename)) {
            constraints.put(Metadata.Field.Name, filename);
        }

        if (keywords != null && !keywords.isEmpty()) {
            constraints.put(Metadata.Field.Keywords, Joiner.on(',').join(keywords));
        }

        if (!Strings.isNullOrEmpty(mimeType)) {
            constraints.put(Metadata.Field.MIMEType, mimeType);
        }

        if (!Strings.isNullOrEmpty(name)) {
            constraints.put(Metadata.Field.Name, name);
        }

        if (!Strings.isNullOrEmpty(uid)) {
            constraints.put(Metadata.Field.UID, uid);
        }

        if (!Strings.isNullOrEmpty(hash)) {
            constraints.put(Metadata.Field.Hash, hash);
        }


        // execute the search with the persistence store.
        try {
            SortedMap<String, List<Metadata>> searchResults =
                    syncStore.search(
                            minAltitude,
                            maxAltitude,
                            constraints,
                            spatialConstraint,
                            start != null ? new Timestamp(start.getTime()) : null,
                            end != null ? new Timestamp(end.getTime()) : null,
                            Boolean.FALSE,
                            missionName,
                            tool,
                            groupVector
                    );

            for (List<Metadata> list : searchResults.values()) {
                for (Metadata metadata : list) {
                    results.add(new Resource(metadata));
                }
            }
        } catch (Exception e) {
            throw new TakException("exception executing search", e);
        }

        return new ApiResponse<NavigableSet<Resource>>(Constants.API_VERSION, Resource.class.getSimpleName(), results);
    }

    // Validation annotation.
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface ValidatedBy {

        public String value() default "";

    }

    private static final String context = "GET request parameters";


    // BACKLOG: consider refactoring validation and searching to use a model object with validation capabilities in conjunction with ESAPI-style validation.
    public void validateParameters(Method method) {

        if (method == null) {
            throw new IllegalArgumentException("null method");
        }

        // validate host and parameters. Validation exceptions will be propagated.

        if (request.getRemoteHost() != null) {
            try {
                validator.getValidInput("HttpServletRequest.getRemoteHost()", request.getRemoteHost(), "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
            } catch (ValidationException | IntrusionException e) {
                throw new com.bbn.marti.remote.exception.ValidationException("invalid host name");
            }
        }

        // no way to do this without reflection
        for (Parameter param : method.getParameters()) {

            if (param.isAnnotationPresent(ValidatedBy.class)) {

                // for each parameter, get the request param name, value, and validator. Then validate.
                if (param.isAnnotationPresent(RequestParam.class)) {

                    String paramName = param.getAnnotation(RequestParam.class).value();

                    String validatorPattern = param.getAnnotation(ValidatedBy.class).value();

                    if (Strings.isNullOrEmpty(validatorPattern)) {
                        throw new TakException("Empty validator name specified for parameter " + paramName);
                    }

                    // validate each value for this parameter
                    if (request.getParameterValues(paramName) != null) {

                        for (String paramValue : request.getParameterValues(paramName)) {
							if (logger.isTraceEnabled()) {
								logger.trace("param name: " + StringUtils.normalizeSpace(paramName) + " param value: " + StringUtils.normalizeSpace(paramValue)
										+ " validation pattern: " + StringUtils.normalizeSpace(validatorPattern) + " ");
							}
                            // do validation
                            try {
                                validator.getValidInput(context, paramValue, validatorPattern, DEFAULT_PARAMETER_LENGTH, false);
                            } catch (ValidationException | IntrusionException e) {
                                throw new com.bbn.marti.remote.exception.ValidationException("invalid parameter value for " + paramName, e);
                            }

                            logger.trace("param name: " + paramName + " validated");
                        }
                    } else {
                        logger.trace("ignoring null parameter " + paramName);
                        continue;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private String trimKeyword(@NotNull String keyword) {

        if (Strings.isNullOrEmpty(keyword)) {
            throw new IllegalArgumentException("empty keyword");
        }

        keyword = keyword.trim();
        keyword = keyword.toLowerCase();

        return keyword;
    }

    @RequestMapping(value = "/missions/{missionName:.+}/token", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String>  getReadOnlyAccessToken(
            @RequestParam(value = "password", defaultValue = "") String password,
            @PathVariable("missionName") String missionName) {

        missionName = missionService.trimName(missionName);

		// validate existence of mission
		Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

        missionService.validatePassword(mission, password);

        String token = missionService.generateToken(
                UUID.randomUUID().toString(), missionName, MissionTokenUtils.TokenType.ACCESS, -1);

        return new ApiResponse<String>(
                Constants.API_VERSION, String.class.getName(), token);
    }

	/*
	 * Returns the mission subscription for the current user
	 */
	@RequestMapping(value = "/missions/{missionName:.+}/subscription", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
	public ApiResponse<MissionSubscription> getSubscriptionForUser(
			@PathVariable("missionName") String missionName,
			@RequestParam(value = "uid", defaultValue = "") String uid)
	{
		missionName = missionService.trimName(missionName);

		// validate existence of mission
		Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

		final String username = SecurityContextHolder.getContext().getAuthentication().getName();
		MissionSubscription missionSubscription = missionSubscriptionRepository
				.findByMissionNameAndClientUidAndUsernameNoMission(missionName, uid, username);

		if (missionSubscription == null) {
			throw new NotFoundException("Mission subscription not found");
		}

		return new ApiResponse<MissionSubscription>(
				Constants.API_VERSION, MissionSubscription.class.getName(), missionSubscription);
	}

    /*
     * subscribe to mission changes
     */
    @RequestMapping(value = "/missions/{missionName:.+}/subscription", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public Callable<ApiResponse<MissionSubscription>> createMissionSubscription(
            @RequestParam(value = "uid", defaultValue = "") String uid,
            @RequestParam(value = "topic", defaultValue = "") String topic,
            @RequestParam(value = "password", defaultValue = "") String password,
            @RequestParam(value = "secago", required = false) Long secago,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end,
            @PathVariable("missionName") String missionNameParam) {

    	final HttpServletRequest request = requestHolderBean.getRequest();
     	
     	final String sessionId = requestHolderBean.sessionId();

		final String groupVector = martiUtil.getGroupVectorBitString(sessionId);

		final String username = SecurityContextHolder.getContext().getAuthentication().getName();

		return () -> {

     		String missionName = missionService.trimName(missionNameParam);
     		
     		
     		 Mission mission;
             if (missionService.getApiVersionNumberFromRequest(request) >= 4) {
                 mission = missionService.getMission(missionName, groupVector);
             } else {
				 mission = missionService.getMissionByNameCheckGroups(missionName, groupVector);
				 missionService.validateMission(mission, missionName);
             }

     		MissionRole tokenRole = missionService.getRoleFromToken(mission,
     				new MissionTokenUtils.TokenType[]{
     						MissionTokenUtils.TokenType.INVITATION,
     						MissionTokenUtils.TokenType.SUBSCRIPTION,
     						MissionTokenUtils.TokenType.ACCESS
     		}, request);

     		//
     		// for password protected missions, the caller must provide a password or a token invite
     		//
     		if (mission.isPasswordProtected()) {
     			if (!Strings.isNullOrEmpty(password)) {
     				if (!BCrypt.checkpw(password, mission.getPasswordHash())) {
     					throw new ForbiddenException("Illegal attempt to subscribe to mission! Password did not match.");
     				}
     			}  else if (tokenRole == null) {
     				throw new ForbiddenException("Illegal attempt to subscribe to mission! No token role provided.");
     			}
     		} else if (!Strings.isNullOrEmpty(password)) {
     			throw new ForbiddenException("Illegal attempt to subscribe to mission! No password provided.");
     		}

     		MissionRole role = null;
     		if (tokenRole != null) {
     			role = tokenRole;
     		} else {
     			role = missionService.getDefaultRole(mission);
     		}

     		if (Strings.isNullOrEmpty(uid) && Strings.isNullOrEmpty(topic)) {
     			throw new IllegalArgumentException("either 'uid' or 'topic' parameter must be specified");
     		}

     		MissionSubscription missionSubscription = missionService.missionSubscribe(missionName, mission.getId(),
					Strings.isNullOrEmpty(topic) ? uid : "topic:" + topic, username, role, groupVector);

     		if (mission.getFeeds() != null) {
     			for (MissionFeed missionFeed : mission.getFeeds()) {
					missionService.sendLatestFeedEvents(mission, missionFeed, uid, groupVector);
				}
			}

			try {
				// clear out any uid invitations now that the uid has subscribed
				missionService.missionUninvite(missionName,
						uid, MissionInvitation.Type.clientUid, uid, groupVector);

     			// clear out any callsign invitations for the devices current callsign
     			RemoteSubscription subscription = subscriptionManagerProxy.getSubscriptionManagerForClientUid(uid).getRemoteSubscriptionByClientUid(uid);
     			if (subscription != null && !Strings.isNullOrEmpty(subscription.callsign)) {
     				missionService.missionUninvite(missionName, subscription.callsign,
     						MissionInvitation.Type.callsign, uid, groupVector);
     			}
     		}
     		catch (Exception e) {
     			throw new TakException(e);
     		}

     		// return changes and logs with API 3+
     		if (missionService.getApiVersionNumberFromRequest(request) >= 3) {

				missionSubscription.setMission(mission);

				Set<MissionChange> changes = missionService.getMissionChanges(
     					mission.getName(), groupVector, secago, start, end, false);
     			missionSubscription.getMission().setMissionChanges(changes);

     			List<LogEntry> logs = missionService.getLogEntriesForMission(mission, secago, start, end);
     			missionSubscription.getMission().setLogs(logs);

     		} else {
     			//  mission is not returned < API 3
     			missionSubscription.setMission(null);
     		}

     		return new ApiResponse<MissionSubscription>(
     				Constants.API_VERSION,  MissionSubscription.class.getName(), missionSubscription);
     	};
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_SET_ROLE')")
    @RequestMapping(value = "/missions/{missionName:.+}/subscription", method = RequestMethod.POST)
    public void setSubscriptionRole(
            @RequestBody List<MissionSubscription> subscriptions,
            @PathVariable("missionName") String missionName,
            @RequestParam(value = "creatorUid", required = true) @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

        missionName = missionService.trimName(missionName);

        String groupVector = martiUtil.getGroupVectorBitString(request);

        // validate existence of mission
		Mission mission = missionService.getMissionByNameCheckGroups(missionName, groupVector);
		missionService.validateMission(mission, missionName);

        // validate subscription list
        for (MissionSubscription subscription : subscriptions) {
            if (subscription.getClientUid() == null || subscription.getRole() == null) {
                throw new IllegalArgumentException("missing clientUid or role");
            }

            MissionRole role = missionRoleRepository.findFirstByRole(subscription.getRole().getRole());

            if (!missionService.validateRoleAssignment(mission, request, role)) {
                throw new ForbiddenException("validateRoleAssignment failed! Illegal attempt to assign role "
                        + role.getRole().name());
            }
        }

        boolean success = missionService.inviteOrUpdate(mission, subscriptions, creatorUid, groupVector);
        response.setStatus(success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /*
     * unsubscribe to mission changes
     *
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_READ')") // use MISSION_READ here to allow readonly users to unsubscribe
    @RequestMapping(value = "/missions/{missionName:.+}/subscription", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public Callable<Void> deleteMissionSubscription(
    		@RequestParam(value = "uid", defaultValue = "") String uid,
			@RequestParam(value = "topic", defaultValue = "") String topic,
			@RequestParam(value = "disconnectOnly", defaultValue = "false") boolean disconnectOnly,
    		@PathVariable("missionName") String missionNameParam) {

    	final String groupVector = martiUtil.getGroupVectorBitString(request);

		final String username = SecurityContextHolder.getContext().getAuthentication().getName();

    	return () -> {

    		String missionName = missionService.trimName(missionNameParam);

			// validate existence of mission
			Mission mission = missionService.getMissionByNameCheckGroups(missionName, groupVector);
			missionService.validateMission(mission, missionName);

    		if (Strings.isNullOrEmpty(uid) && Strings.isNullOrEmpty(topic)) {
    			throw new IllegalArgumentException("either 'uid' or 'topic' parameter must be specified");
    		}

    		missionService.missionUnsubscribe(missionName, Strings.isNullOrEmpty(topic) ? uid : topic, username, groupVector, disconnectOnly);
    		
    		return null;
    	};
    }

    /*
     * Get all mission subscriptions
     *
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')") // restrict to admin
    @RequestMapping(value = "/missions/all/subscriptions", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Map.Entry<String, String>>> getAllMissionSubscriptions() {
        return new ApiResponse<List<Map.Entry<String, String>>>(Constants.API_VERSION, "MissionSubscription", missionService.getAllMissionSubscriptions());
    }

    /*
     * Get subscriptions to the specified mission
     *
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{missionName:.+}/subscriptions", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<String>> getMissionSubscriptions(
            @PathVariable("missionName") String missionName, HttpServletRequest request) {

		Mission mission = missionService.getMissionByNameCheckGroups(missionService.trimName(missionName), martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

        try {
            return new ApiResponse<List<String>>(Constants.API_VERSION, "MissionSubscription", subscriptionManager.getMissionSubscriptions(missionName, false));
        } catch (Exception e) {
            throw new TakException(e);
        }
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{missionName:.+}/subscriptions/roles", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<MissionSubscription>> getMissionSubscriptionRoles(
            @PathVariable("missionName") String missionName, HttpServletRequest request) {

		Mission mission = missionService.getMissionByNameCheckGroups(missionService.trimName(missionName), martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

		// ensure tokens are removed from the output
		List<MissionSubscription> missionSubscriptions = missionSubscriptionRepository.findAllByMissionNameNoMissionNoToken(missionName);
		for (MissionSubscription missionSubscription : missionSubscriptions) {
			missionSubscription.setToken(null);
		}

        return new ApiResponse<List<MissionSubscription>>(Constants.API_VERSION, "MissionSubscription",
                missionSubscriptions);
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{missionName:.+}/role", method = RequestMethod.GET)
    public ApiResponse<MissionRole> getMissionRoleFromToken(
            @PathVariable("missionName") String missionName, HttpServletRequest request) {

        missionName = missionService.trimName(missionName);

        // validate existence of mission
		Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

        MissionRole role = (MissionRole)request.getAttribute(MissionRole.class.getName());

        return new ApiResponse<MissionRole>(
                Constants.API_VERSION,  MissionRole.class.getName(), role);
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_SET_ROLE')")
    @RequestMapping(value = "/missions/{missionName:.+}/role", method = RequestMethod.PUT)
    public void setMissionRole(
            @PathVariable("missionName") String missionName,
			@RequestParam(value = "clientUid", defaultValue = "") @ValidatedBy("MartiSafeString") String clientUid,
			@RequestParam(value = "username", defaultValue = "") @ValidatedBy("MartiSafeString") String username,
            @RequestParam(value = "role", defaultValue = "") @ValidatedBy("MartiSafeString") MissionRole.Role newRole,
            HttpServletRequest request) {

        missionName = missionService.trimName(missionName);

        // validate existence of mission
		Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

        if (mission.getDefaultRole() == null) {
            logger.error("illegal attempt to set role on non role-enabled mission!");
            throw new IllegalArgumentException();
        }

        MissionRole role = missionRoleRepository.findFirstByRole(newRole);

        if (!missionService.validateRoleAssignment(mission, request, role)) {
            throw new ForbiddenException(
                    "validateRoleAssignment failed! Illegal attempt to assign role " + newRole.name());
        }

        boolean result = missionService.setRole(mission, clientUid, username, role);
        response.setStatus(result ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /*
     * Get all mission invitations for a given clientUid
     *
     */
    @RequestMapping(value = "/missions/all/invitations", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Set<String>> getAllMissionInvitations(
            @RequestParam(value = "clientUid", defaultValue = "") @ValidatedBy("MartiSafeString") String clientUid) {

        Set<MissionInvitation> missionInvitations = missionService.getAllMissionInvitationsForClient(
                clientUid, martiUtil.getGroupVectorBitString(request));

        Set<String> missionNames = new HashSet<String>();
        for (MissionInvitation mi : missionInvitations) {
            missionNames.add(mi.getMissionName());
        }

        return new ApiResponse<Set<String>>(Constants.API_VERSION, "MissionInvitation", missionNames);
    }

    @RequestMapping(value = "/missions/invitations", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Set<MissionInvitation>> getAllMissionInvitationsWithPasswords(
            @RequestParam(value = "clientUid", required = true) @ValidatedBy("MartiSafeString") String clientUid) {

        return new ApiResponse<Set<MissionInvitation>>(Constants.API_VERSION, "MissionInvitation",
                missionService.getAllMissionInvitationsForClient(clientUid, martiUtil.getGroupVectorBitString(request)));
    }

    /*
     * Get invitations to the specified mission
     *
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{missionName:.+}/invitations", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<MissionInvitation>> getMissionInvitations(
            @PathVariable("missionName") String missionName, HttpServletRequest request) {

		Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

        List<MissionInvitation> missionInvitations = missionService.getMissionInvitations(mission.getName());

        return new ApiResponse<List<MissionInvitation>>(Constants.API_VERSION, "MissionInvitation", missionInvitations);
    }

    @RequestMapping(value = "/missions/{name:.+}/invite/{type:.+}/{invitee:.+}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void inviteToMission(
            @PathVariable("name") @NotNull String missionName,
            @PathVariable("type") @NotNull MissionInvitation.Type type,
            @PathVariable("invitee") @NotNull String invitee,
            @RequestParam(value = "creatorUid", required = true) @ValidatedBy("MartiSafeString") String creatorUid,
            @RequestParam(value = "role", defaultValue = "") @ValidatedBy("MartiSafeString") MissionRole.Role role,
            HttpServletRequest request) {

        if (Strings.isNullOrEmpty(missionName)) {
            throw new IllegalArgumentException("empty mission name");
        }

        missionName = missionService.trimName(missionName);

        String groupVector = martiUtil.getGroupVectorBitString(request);

		Mission mission = missionService.getMissionByNameCheckGroups(missionName, groupVector);
		missionService.validateMission(mission, missionName);

        MissionRole inviteRole = null;

        if (role != null) {
            inviteRole = missionRoleRepository.findFirstByRole(role);
        } else {
            inviteRole = missionService.getDefaultRole(mission);
        }

        if (!missionService.validateRoleAssignment(mission, request, inviteRole)) {
            throw new ForbiddenException("validateRoleAssignment failed! Illegal attempt to assign role "
                    + inviteRole.getRole().name());
        }

        missionService.missionInvite(missionName, invitee, type, inviteRole, creatorUid, groupVector);
    }

    @RequestMapping(value = "/missions/{name:.+}/invite/{type:.+}/{invitee:.+}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
	@Transactional
    public void uninviteFromMission(
            @PathVariable("name") @NotNull String missionName,
            @PathVariable("type") @NotNull MissionInvitation.Type type,
            @PathVariable("invitee") @NotNull String invitee,
            @RequestParam(value = "creatorUid", required = true) @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

    	try {
			if (Strings.isNullOrEmpty(missionName)) {
				throw new IllegalArgumentException("empty mission name");
			}

			missionName = missionService.trimName(missionName);

			Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
			missionService.validateMission(mission, missionName);

			MissionRole role = missionService.getRoleFromToken(mission, new MissionTokenUtils.TokenType[]{
					MissionTokenUtils.TokenType.INVITATION,
					MissionTokenUtils.TokenType.SUBSCRIPTION
			}, request);

			if (mission.isPasswordProtected() && role == null) {
				throw new ForbiddenException("Illegal attempt to delete invitation");
			}

			missionService.missionUninvite(
					missionName, invitee, type, creatorUid, martiUtil.getGroupVectorBitString(request));
		} catch (Exception e) {
			logger.error("exception in uninviteFromMission!", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
    }

    // Mission Log
    /*
     * Create a new log entry (autogenerating the entry id)
     *
     */
    @RequestMapping(value = "/missions/logs/entries", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LogEntry> postLogEntry(@RequestBody @NotNull LogEntry entry) {

        if (!Strings.isNullOrEmpty(entry.getId())) {
            throw new IllegalArgumentException("log entry id must not be included for POST");
        }

        String groupVector = martiUtil.getGroupVectorBitString(request);

        for (String name : entry.getMissionNames()) {
            String missionName = missionService.trimName(name);
			Mission mission = missionService.getMissionByNameCheckGroups(missionName, groupVector);
			missionService.validateMission(mission, missionName);

            MissionRole role = missionService.getRoleForRequest(mission, request);
            if (role == null) {
                logger.error("no role for request : " + request.getServletPath());
                continue;
            }

            if (!role.hasPermission(MissionPermission.Permission.MISSION_WRITE)) {
                throw new ForbiddenException("Illegal attempt to access mission!");
            }
        }

        // save the new log entry and let the database generate the id
        return new ApiResponse<>(Constants.API_VERSION, LogEntry.class.getName(),
                missionService.addUpdateLogEntry(entry, new Date(), groupVector));
    }

    /*
     * Get all log entries combined in one list (regardless of mission)
     *
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')") // restrict to admin
    @RequestMapping(value = "/missions/all/logs", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<LogEntry>> getAllLogEntries() {

        return new ApiResponse<>(Constants.API_VERSION, LogEntry.class.getName(), logEntryRepository.findAll());
    }

    /*
     * Get one log entry by id
     *
     */
    @RequestMapping(value = "/missions/logs/entries/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<LogEntry>> getOneLogEntry(@PathVariable("id") @NotNull String id) {

        LogEntry entry = logEntryRepository.getOne(id);

        String msg = "log entry " + id + " not found";

        if (entry == null) {
            throw new NotFoundException(msg);
        }

        for (String name : entry.getMissionNames()) {
            String missionName = missionService.trimName(name);
			Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
			missionService.validateMission(mission, missionName);

            MissionRole role = missionService.getRoleForRequest(mission, request);
            if (role == null) {
                logger.error("no role for request : " + request.getServletPath());
                continue;
            }

            if (!role.hasPermission(MissionPermission.Permission.MISSION_READ)) {
                throw new ForbiddenException("Illegal attempt to access mission!");
            }
        }

        return new ApiResponse<List<LogEntry>>(Constants.API_VERSION, LogEntry.class.getName(), Lists.newArrayList(entry));
    }

    /*
     *
     * Update an existing log entry
     *
     */
    @RequestMapping(value = "/missions/logs/entries", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<LogEntry> updateLogEntry(@RequestBody LogEntry entry) {

        if (Strings.isNullOrEmpty(entry.getId())) {
            throw new IllegalArgumentException("log entry id must be included for PUT");
        }

        logger.info("servertime: " + entry.getServertime());

        if (entry.getServertime() != null) {
            throw new IllegalArgumentException("servertime can't be modified");
        }

        String groupVector = martiUtil.getGroupVectorBitString(request);

        for (String name : entry.getMissionNames()) {
            String missionName = missionService.trimName(name);
			Mission mission = missionService.getMissionByNameCheckGroups(missionName, groupVector);
			missionService.validateMission(mission, missionName);

            MissionRole role = missionService.getRoleForRequest(mission, request);
            if (role == null) {
                logger.error("no role for request : " + request.getServletPath());
                continue;
            }

            if (!role.hasPermission(MissionPermission.Permission.MISSION_WRITE)) {
                throw new ForbiddenException("Illegal attempt to access mission!");
            }
        }

        // Don't allow create through this path (by specifying primary key)
        if (!logEntryRepository.existsById(entry.getId())) {
            throw new IllegalArgumentException("Log entry " + entry.getId() + " not found");
        }

        entry = missionService.addUpdateLogEntry(entry, new Date(), groupVector);

        return new ApiResponse<>(Constants.API_VERSION, LogEntry.class.getName(), entry);
    }

    /*
     *
     * Delete a log entry
     *
     */
    @RequestMapping(value = "/missions/logs/entries/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deleteLogEntry(@PathVariable(value = "id") String id) {

        if (Strings.isNullOrEmpty(id)) {
            throw new IllegalArgumentException("log entry id must be provided for DELETE");
        }

        LogEntry entry = logEntryRepository.getOne(id);
        if (entry == null) {
            throw new NotFoundException("log entry " + id + " not found");
        }

        String groupVector = martiUtil.getGroupVectorBitString(request);

        for (String name : entry.getMissionNames()) {
            String missionName = missionService.trimName(name);
			Mission mission = missionService.getMissionByNameCheckGroups(missionName, groupVector);
			missionService.validateMission(mission, missionName);

            MissionRole role = missionService.getRoleForRequest(mission, request);
            if (role == null) {
                logger.error("no role for request : " + request.getServletPath());
                continue;
            }

            if (!role.hasPermission(MissionPermission.Permission.MISSION_WRITE)) {
                throw new ForbiddenException("Illegal attempt to access mission!");
            }
        }

        missionService.deleteLogEntry(id, groupVector);
    }

    /*
     * For a given mission, return all log entries, optionally filtered by time interval.
     *
     * If time filter is specified, and there are no changes, return an empty list.
     *
     */
    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{missionName:.+}/log", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<LogEntry>> getLogEntry(@PathVariable("missionName") @NotNull String missionName,
                                                   @RequestParam(value = "secago", required = false) Long secago,
                                                   @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
                                                   @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end,
                                                   HttpServletRequest request) {

        String name = missionService.trimName(missionName);

		Mission mission = missionService.getMissionByNameCheckGroups(name, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, name);

        return new ApiResponse<List<LogEntry>>(Constants.API_VERSION, LogEntry.class.getName(),
                missionService.getLogEntriesForMission(mission, secago, start, end));
    }

    /*
     * Get resource metadata by hash
     */
    @RequestMapping(value = "/resources/{hash}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_ADMIN')") // restrict to admin, since this path doesn't check group membership
    public ApiResponse<List<Resource>> getResource(@PathVariable("hash") @NotNull String hash) {

    	if (logger.isDebugEnabled()) {
    		logger.debug("fetch resource by hash " + hash);
    	}

        Resource resource = Resource.fetchResourceByHash(hash);

        if (resource == null) {
            throw new NotFoundException("no resource found for hash " + hash);
        }

        return new ApiResponse<List<Resource>>(Constants.API_VERSION, Resource.class.getSimpleName(), Lists.newArrayList(resource));
    }

    // Get all latest CoT events for a mission
    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{name:.+}/cot", method = RequestMethod.GET)
    ResponseEntity<String> getLatestMissionCotEvents(@PathVariable("name") @NotNull String missionName, HttpServletRequest request) {

    	final String sessionId = requestHolderBean.sessionId();

    	if (logger.isDebugEnabled()) {
    		logger.debug("session id: " + requestHolderBean.sessionId());
    	}

    	if (Strings.isNullOrEmpty(missionName)) {
    		throw new IllegalArgumentException("empty mission name");
    	}

    	String fmissionName = missionService.trimName(missionName);

    	String groupVector = martiUtil.getGroupVectorBitString(sessionId);

		// will throw appropriate exeception if mission does not exist or has been deleted
		Mission mission = missionService.getMissionByNameCheckGroups(fmissionName, groupVector);
		missionService.validateMission(mission, fmissionName);

		String cot = missionService.getCachedCot(missionName, mission.getUids(), groupVector);

    	HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(org.springframework.http.MediaType.APPLICATION_XML);

    	return new ResponseEntity<String>(cot, headers, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{name:.+}/contacts", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<RemoteSubscription>> results(
            @PathVariable("name") @NotNull String missionName, HttpServletRequest request) throws RemoteException {

        if (Strings.isNullOrEmpty(missionName)) {
            throw new IllegalArgumentException("empty mission name");
        }

        missionName = missionService.trimName(missionName);

		Mission mission = missionService.getMissionByNameCheckGroups(missionName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, missionName);

        List<RemoteSubscription> results = subscriptionManager.getSubscriptionsWithGroupAccess(mission.getGroupVector(), false);

        return new ResponseEntity<List<RemoteSubscription>>(results, new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/missions/{name:.+}/invite", method = RequestMethod.POST)
    public void sendMissionInvites(
            @PathVariable("name") @NotNull String missionName,
            @RequestParam(value = "creatorUid", required = false) @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request
    ) throws RemoteException, ValidationException, IOException {

        try {

            if (Strings.isNullOrEmpty(missionName)) {
                throw new IllegalArgumentException("empty mission name");
            }

            missionName = missionService.trimName(missionName);

            String groupVector = martiUtil.getGroupVectorBitString(request);

			Mission mission = missionService.getMissionByNameCheckGroups(missionName, groupVector);
			missionService.validateMission(mission, missionName);

            if (missionService.getApiVersionNumberFromRequest(request) > 2) {

                String body = new String(IOUtils.toByteArray(request.getInputStream()));

                ObjectMapper mapper = new ObjectMapper();
                List<MissionInvitation> missionInvitations =
                        mapper.readValue(body, new TypeReference<List<MissionInvitation>>(){});

                for (MissionInvitation missionInvitation : missionInvitations) {

                    if (missionInvitation.getType() == null || missionInvitation.getInvitee() == null) {
                        throw new IllegalArgumentException("invitation found without type or invitee attribute!");
                    }

                    missionInvitation.setMissionName(missionName);
                    missionInvitation.setCreatorUid(creatorUid);
                    missionInvitation.setCreateTime(new Date());

                    MissionRole role = missionInvitation.getRole();
                    if (role == null) {
                        role = missionService.getDefaultRole(mission);
                    } else {
                        role = missionRoleRepository.findFirstByRole(role.getRole());
                    }

                    if (!missionService.validateRoleAssignment(mission, request, role)) {
                        throw new ForbiddenException("validateRoleAssignment failed! Illegal attempt to assign role "
                                + role.getRole().name());
                    }

                    String token = missionService.generateToken(
                            UUID.randomUUID().toString(), missionName, MissionTokenUtils.TokenType.INVITATION, -1);
                    missionInvitation.setToken(token);

                    missionInvitation.setRole(role);

                    missionService.missionInvite(mission, missionInvitation);
                }

            } else {

                String[] contactUids = request.getParameterValues("contacts");

                if (contactUids == null || contactUids.length == 0) {
                    throw new IllegalArgumentException("empty contacts array");
                }

                // validate the uids before sending on to the subscriptionManager
                for (String uid : contactUids) {
                    validator.getValidInput(context, uid, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
                }
                validateParameters(new Object() {
                }.getClass().getEnclosingMethod());

                User user = groupManager.getUserByConnectionId(RequestContextHolder.currentRequestAttributes().getSessionId());

                String author = Strings.isNullOrEmpty(creatorUid) ? user.getName() : creatorUid;

                MissionRole inviteRole = missionService.getDefaultRole(mission);

                if (!missionService.validateRoleAssignment(mission, request, inviteRole)) {
                    throw new ForbiddenException("validateRoleAssignment failed! Illegal attempt to assign role "
                            + inviteRole.getRole().name());
                }

                for (String uid : contactUids) {
                    try {
                        missionService.missionInvite(
                                missionName, uid, MissionInvitation.Type.clientUid, inviteRole, author, groupVector);
                    } catch (Exception e) {
                        logger.debug("Attempt to re-invite clientUid: " + uid + " to: " + missionName);
                        continue;
                    }
                }
            }

            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            logger.error("exception in sendMissionInvites!", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{childName:.+}/parent/{parentName:.+}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void setParent(@PathVariable("childName") @NotNull String childName,
                          @PathVariable("parentName") @NotNull String parentName,
                          HttpServletRequest request) {

        childName = missionService.trimName(childName);
        parentName = missionService.trimName(parentName);

		Mission mission = missionService.getMissionByNameCheckGroups(childName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, childName);

        missionService.setParent(childName, parentName, martiUtil.getGroupVectorBitString(request));
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{childName:.+}/parent", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void clearParent(@PathVariable("childName") @NotNull String childName, HttpServletRequest request) {

        childName = missionService.trimName(childName);

		Mission mission = missionService.getMissionByNameCheckGroups(childName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, childName);

        missionService.clearParent(childName, martiUtil.getGroupVectorBitString(request));
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{name:.+}/children", method = RequestMethod.GET)
    ApiResponse<Set<Mission>> getChildren(@PathVariable("name") @NotNull String parentName, HttpServletRequest request) {
        String groupVector = martiUtil.getGroupVectorBitString(request);

        parentName = missionService.trimName(parentName);

		Mission mission = missionService.getMissionByNameCheckGroups(parentName, martiUtil.getGroupVectorBitString(request));
		missionService.validateMission(mission, parentName);

        Set<Mission> children = missionService.getChildren(parentName, groupVector);

        return new ApiResponse<Set<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), children);
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{name:.+}/parent", method = RequestMethod.GET)
    ApiResponse<Mission> getParent(@PathVariable("name") @NotNull String parentName, HttpServletRequest request) {
        String groupVector = martiUtil.getGroupVectorBitString(request);
        Mission mission = missionService.getMission(missionService.trimName(parentName), groupVector);

        if (mission.getParent() == null) {
            throw new NotFoundException("Parent mission not found");
        }

        return new ApiResponse<Mission>(Constants.API_VERSION, Mission.class.getSimpleName(),
                missionService.hydrate(mission.getParent(), true));
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_READ')")
    @RequestMapping(value = "/missions/{name:.+}/kml", method = RequestMethod.GET)
    ResponseEntity<String> getKml(
            @PathVariable("name") @NotNull String missionName,
            @RequestParam(value = "download", defaultValue = "false") boolean download,
            HttpServletRequest request) {
        String groupVector = martiUtil.getGroupVectorBitString(request);
     
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new org.springframework.http.MediaType(
                "application", "vnd.google-earth.kml+xml"));

        String urlBase = null;
        if (download) {
            headers.setContentDispositionFormData("kml", missionName + ".kml");
        } else {
            String requestUrl = request.getRequestURL().toString();
            try {
                requestUrl = URLDecoder.decode(requestUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("error decoding URL!" + requestUrl);
            }
            urlBase = requestUrl.substring(0, requestUrl.indexOf(request.getServletPath()));
        }

        return new ResponseEntity<String>(
                missionService.getMissionKml(missionName, urlBase, groupVector), headers, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name}/externaldata", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExternalMissionData> setExternalMissionData(
            @PathVariable(value = "name") String missionName,
            @RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
            @RequestBody @NotNull ExternalMissionData externalMissionData,
            HttpServletRequest request) {
        if (Strings.isNullOrEmpty(externalMissionData.getId())) {
            throw new IllegalArgumentException("ExternalMissionData id must be included");
        }

        String groupVector = martiUtil.getGroupVectorBitString(request);

        externalMissionData = missionService.setExternalMissionData(missionName, creatorUid, externalMissionData, groupVector);

        // save the new log entry and let the database generate the id
        return new ApiResponse<>(Constants.API_VERSION, ExternalMissionData.class.getName(), externalMissionData);
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name}/externaldata/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deleteExternalMissionData(
            @PathVariable(value = "name") String missionName,
            @PathVariable(value = "id") String externalMissionDataId,
            @RequestParam(value = "notes") @ValidatedBy("MartiSafeString") String notes,
            @RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {
        String groupVector = martiUtil.getGroupVectorBitString(request);

        missionService.deleteExternalMissionData(missionName, externalMissionDataId, notes, creatorUid, groupVector);
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
    @RequestMapping(value = "/missions/{name}/externaldata/{id}/change", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void notifyExternalDataChanged(
            @PathVariable(value = "name") String missionName,
            @PathVariable(value = "id") String externalMissionDataId,
            @RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
            @RequestParam(value = "notes") @ValidatedBy("MartiSafeString") String notes,
            @RequestBody String token,
            HttpServletRequest request) {
        String groupVector = martiUtil.getGroupVectorBitString(request);

        missionService.notifyExternalMissionDataChanged(missionName, externalMissionDataId, token, notes, creatorUid, groupVector);
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_SET_PASSWORD')")
    @RequestMapping(value = "/missions/{name:.+}/password", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void setPassword(
            @PathVariable(value = "name") @ValidatedBy("MartiSafeString") String missionName,
            @RequestParam(value = "password", defaultValue = "") @ValidatedBy("MartiSafeString") String password,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

        missionName = missionService.trimName(missionName);

        String groupVector = martiUtil.getGroupVectorBitString(request);
        Mission mission = missionService.getMission(missionName, groupVector);

        String newPasswordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        missionRepository.setPasswordHash(missionName, newPasswordHash, groupVector);
        missionService.invalidateMissionCache(missionName);

        try {
            subscriptionManager.broadcastMissionAnnouncement(missionName, mission.getGroupVector(), creatorUid,
                    SubscriptionManagerLite.ChangeType.METADATA, mission.getTool());
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }
    }

    @PreAuthorize("hasPermission(#request, 'MISSION_SET_PASSWORD')")
    @RequestMapping(value = "/missions/{name:.+}/password", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void removePassword(
            @PathVariable(value = "name") @ValidatedBy("MartiSafeString") String missionName,
            @RequestParam(value = "creatorUid", defaultValue = "") @ValidatedBy("MartiSafeString") String creatorUid,
            HttpServletRequest request) {

        missionName = missionService.trimName(missionName);

        String groupVector = martiUtil.getGroupVectorBitString(request);
        Mission mission = missionService.getMission(missionName, groupVector);

        missionRepository.setPasswordHash(missionName, null, groupVector);
        missionService.invalidateMissionCache(missionName);

        try {
            subscriptionManager.broadcastMissionAnnouncement(missionName, mission.getGroupVector(), creatorUid,
                    SubscriptionManagerLite.ChangeType.METADATA, mission.getTool());
        } catch (Exception e) {
            logger.debug("exception announcing mission change " + e.getMessage(), e);
        }
    }

	@RequestMapping(value = "/missions/{name}/expiration", method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	public void setExpiration(
			@PathVariable(value = "name") @ValidatedBy("MartiSafeString") String missionName,
			@RequestParam(value = "expiration", required = false) Long expiration,
			HttpServletRequest request) {

		try {
			boolean result = missionService.setExpiration(missionName, expiration);
			response.setStatus(result ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			try {
				if (logger.isDebugEnabled()) {
					logger.debug(" setting the mission expiration " + missionName + " time " + expiration);
				}
				retentionPolicyConfig.setMissionExpiryTask(missionName, expiration);
			} catch (Exception e) {
				logger.error(" Exception getting Retention service, task not scheduled immediately " + missionName);
			}
		} catch (Exception e) {
			logger.debug("exception setting mission expiration " + e.getMessage(), e);
		}
	}

	@PreAuthorize("hasPermission(#request, 'MISSION_MANAGE_FEEDS')")
	@RequestMapping(value = "/missions/{missionName:.+}/feed", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void addFeed(
			@PathVariable(value = "missionName") String missionName,
			@RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
			@RequestParam(value = "dataFeedUid") @ValidatedBy("MartiSafeString") String dataFeedUid,
			@RequestParam(value = "filterBbox", required = false) @ValidatedBy("MartiSafeString") String filterBbox,
			@RequestParam(value = "filterType", required = false) @ValidatedBy("MartiSafeString") String filterType,
			@RequestParam(value = "filterCallsign", required = false) @ValidatedBy("MartiSafeString") String filterCallsign,
			HttpServletRequest request) {

		missionName = missionService.trimName(missionName);

		String groupVector = martiUtil.getGroupVectorBitString(request);
		Mission mission = missionService.getMission(missionName, groupVector);

		try {
			missionService.addFeedToMission(mission.getName(), creatorUid, mission, dataFeedUid, filterBbox, filterType, filterCallsign);
    	} catch (Exception e) {
    		logger.error("exception in addFeed!", e);
		}
    }

	@PreAuthorize("hasPermission(#request, 'MISSION_MANAGE_FEEDS')")
	@RequestMapping(value = "/missions/{missionName:.+}/feed/{uid:.+}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void removeFeed(
			@PathVariable(value = "missionName") String missionName,
			@PathVariable(value = "uid") String uid,
			@RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
			HttpServletRequest request) {

    	missionName = missionService.trimName(missionName);

		String groupVector = martiUtil.getGroupVectorBitString(request);
		Mission mission = missionService.getMission(missionName, groupVector);

		try {
			missionService.removeFeedFromMission(mission.getName(), creatorUid, mission, uid);
		} catch (Exception e) {
			logger.error("exception in removeFeed!", e);
		}
	}

	@PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
	@RequestMapping(value = "/missions/{missionName:.+}/maplayers", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	ApiResponse<MapLayer> createMapLayer(
			@PathVariable(value = "missionName") String missionName,
			@RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
			@RequestBody MapLayer mapLayer) {

		missionName = missionService.trimName(missionName);

		String groupVector = martiUtil.getGroupVectorBitString(request);
		Mission mission = missionService.getMission(missionName, groupVector);

		mapLayer.setMission(mission);

		MapLayer newMapLayer = missionService.addMapLayerToMission(missionName, creatorUid, mission, mapLayer);

		return new ApiResponse<>(Constants.API_VERSION, "MapLayer", newMapLayer);
	}

	@PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
	@RequestMapping(value = "/missions/{missionName:.+}/maplayers/{uid}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	void deleteMapLayer(
			@PathVariable(value = "missionName") String missionName,
			@RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
			@PathVariable("uid") @NotNull String uid) {

		missionName = missionService.trimName(missionName);

		String groupVector = martiUtil.getGroupVectorBitString(request);
		Mission mission = missionService.getMission(missionName, groupVector);

		missionService.removeMapLayerFromMission(missionName, creatorUid, mission, uid);
	}

	@PreAuthorize("hasPermission(#request, 'MISSION_WRITE')")
	@RequestMapping(value = "/missions/{missionName:.+}/maplayers", method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	ApiResponse<MapLayer> updateMapLayer(
			@PathVariable(value = "missionName") String missionName,
			@RequestParam(value = "creatorUid") @ValidatedBy("MartiSafeString") String creatorUid,
			@RequestBody MapLayer mapLayer) {

		missionName = missionService.trimName(missionName);

		String groupVector = martiUtil.getGroupVectorBitString(request);
		Mission mission = missionService.getMission(missionName, groupVector);

		mapLayer.setMission(mission);

		MapLayer newMapLayer = missionService.updateMapLayer(missionName, creatorUid, mission, mapLayer);

		return new ApiResponse<>(Constants.API_VERSION, "MapLayer", newMapLayer);
	}
	
	private static String boundingPolygonPointsToString(List<String> points) {
		if (points == null || points.size() == 0) {
			return null;
		}
		
		// poly needs 3 points minimum
		if (points.size() < 3) {
			logger.info("Polygon requires at least 3 points. Found size " + points.size());
			return null;
		}

		try {
			// check that all points are valid numbers
			for (String point : points) {
				String[] xy = point.split(",");
				if (xy.length != 2) {
					logger.info("Point is not in the format of <x>,<y>" + Arrays.deepToString(xy));
					return null;
				}
				Double.parseDouble(xy[0]);
				Double.parseDouble(xy[1]);
			}
		} catch (Exception e) {
			logger.error("Error parsing points for data feed bounds", e);
			return null;
		}

		// first != last, so append first to end to close the poly
		if (!points.get(0).replace(" ", "").equals(points.get(points.size() - 1).replace(" ", ""))) {
			points.add(points.get(0));
		}

		return points.stream().map(p -> p.replace(" ", "")).map(p -> p.replace(",", " ")).collect(Collectors.joining(","));
	}
}