package tak.server.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.exception.MissionDeletedException;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.model.LogEntry;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.model.MissionRole.Role;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.repository.MissionRoleRepository;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;
import com.google.common.base.Strings;

import tak.server.plugins.PluginMissionApi;
import tak.server.system.ApiDependencyProxy;;

public class DistributedPluginMissionApi implements PluginMissionApi, org.apache.ignite.services.Service {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(DistributedPluginMissionApi.class);

	private MissionRepository missionRepository() {
	    return ApiDependencyProxy.getInstance().missionRepository();
	}
	
	private SubscriptionManagerLite subscriptionManager() {
		return ApiDependencyProxy.getInstance().subscriptionManagerLite();
	}

	private MissionService missionService() {
	    return ApiDependencyProxy.getInstance().missionService();
	}
	
	private GroupManager groupManager() {
	    return ApiDependencyProxy.getInstance().groupManager();
	}

//	@Autowired
//	private CommonUtil martiUtil;
	private CommonUtil commonUtil() {
	    return ApiDependencyProxy.getInstance().commonUtil();
	}

	private MissionRoleRepository missionRoleRepository() {
	    return ApiDependencyProxy.getInstance().missionRoleRepository();
	}

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	@Override
	public List<Mission> getAllMissions(boolean passwordProtected, boolean defaultRole, String tool)
			throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("MissionApiForPlugin getAllMissions");
		}

		NavigableSet<Group> allGroups = commonUtil().getAllInOutGroups();

		MissionService missionService = missionService();
		List<Mission> missions = missionService.getAllMissions(passwordProtected, defaultRole, tool, allGroups);

		return missions;
	}

	@Override
	public Mission readMission(String name, boolean changes, boolean logs, Long secago, Date start, Date end) throws Exception {
				
		final String groupVectorForAdminUser = RemoteUtil.getInstance().getBitStringAllGroups();

		final NavigableSet<Group> allGroups = commonUtil().getAllInOutGroups();

		if (Strings.isNullOrEmpty(name)) {
			throw new IllegalArgumentException("empty 'name' path parameter");
		}

		MissionService missionService = missionService();
		
		String missionName = missionService.trimName(name);

		if (logger.isDebugEnabled()) {
			logger.debug("getMission " + missionName);
		}

		Mission mission = missionService.getMission(missionName, groupVectorForAdminUser);
		
		if (changes) {
			Set<MissionChange> missionChanges = missionService.getMissionChanges(
					mission.getName(), groupVectorForAdminUser, secago, start, end, false);
			mission.setMissionChanges(missionChanges);
		}

		if (logs) {
			List<LogEntry> missionLogs = missionService.getLogEntriesForMission(mission, secago, start, end);
			mission.setLogs(missionLogs);
		}

		mission.setGroups(RemoteUtil.getInstance().getGroupNamesForBitVectorString(
				mission.getGroupVector(), allGroups));

		return mission;
		
	}

	@Override
	public Mission createMission(String name, String creatorUid, String[] groupNames,
		 String description, String chatRoom, String baseLayer, String bbox,
		 List<String> boundingPolygonParam, String path, String classification, String tool,
		 String password, String roleParam, Long expiration, byte[] missionPackage) throws Exception {

		return createMission(name, creatorUid, groupNames,
				description, chatRoom, baseLayer, bbox,
				boundingPolygonParam, path, classification, tool,
				password, roleParam, expiration, false, missionPackage);
	}

	@Override
	public Mission createMission(String name, String creatorUid, String[] groupNames,
			String description, String chatRoom, String baseLayer, String bbox,
			List<String> boundingPolygonParam, String path, String classification, String tool,
			String password, String roleParam, Long expiration, Boolean inviteOnly, byte[] missionPackage) throws Exception {

		if (Strings.isNullOrEmpty(name)) {
			throw new IllegalArgumentException("Mission name cannot be empty or null");
		}
		
		MissionService missionService = missionService();
		GroupManager groupManager = groupManager();
		MissionRepository missionRepository = missionRepository();
		MissionRoleRepository missionRoleRepository = missionRoleRepository();		
		SubscriptionManagerLite subscriptionManager = subscriptionManager();
	
		name = missionService.trimName(name);

		final String groupVectorForAdminUser = RemoteUtil.getInstance().getBitStringAllGroups();

		String boundingPolygon = boundingPolygonPointsToString(boundingPolygonParam);
		MissionRole.Role role = Role.valueOf(roleParam);

		Set<Group> groups = groupManager.findGroups(Arrays.asList(groupNames));
		String groupVectorMission = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));

		MissionRole defaultRole = null;
		if (role != null) {
			defaultRole = missionRoleRepository.findFirstByRole(role);
		}

		Mission mission;
		try {

			mission = missionService.getMission(name, groupVectorForAdminUser);

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

			if (!(expiration == null ? mission.getExpiration() == null || mission.getExpiration() == -1L
					: mission.getExpiration() != null && expiration.equals(mission.getExpiration()))) {
				mission.setExpiration(expiration);
				updated = true;
			}

			if (updated) {
				if (expiration != null) {
					missionRepository.update(name, groupVectorForAdminUser, description, chatRoom, baseLayer, bbox,
							path, classification, expiration, boundingPolygon);
				} else {
					missionRepository.update(name, groupVectorForAdminUser, description, chatRoom, baseLayer, bbox,
							path, classification, -1L, boundingPolygon);
				}
			}

			if (password != null && password.length() > 0) {

				String newPasswordHash = BCrypt.hashpw(password, BCrypt.gensalt());

				if (!StringUtils.equals(newPasswordHash, mission.getPasswordHash())) {
					missionRepository.setPasswordHash(name, newPasswordHash, groupVectorMission);
					mission.setPasswordHash(newPasswordHash);
					updated = true;
				}
			}

			if (defaultRole != null) {

				if (mission.getDefaultRole() == null || mission.getDefaultRole().compareTo(defaultRole) != 0) {
					mission.setDefaultRole(defaultRole);
					missionRepository.setDefaultRoleId(name, defaultRole.getId(), groupVectorMission);
					updated = true;
				}
			}

			// did the groups change?
			if (mission.getGroupVector().compareTo(groupVectorMission) != 0) {
				missionRepository.updateGroups(name, groupVectorForAdminUser, groupVectorMission);
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

			return mission;

		} catch (MissionDeletedException | NotFoundException e) {

			String passwordHash = null;
			if (!Strings.isNullOrEmpty(password)) {
				passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
			}

			// For now there are no properties to be concerted with besides name, so PUT
			// missions doesn't need a JSON body yet
			if (expiration != null) {
				mission = missionService.createMission(name, creatorUid, groupVectorMission, description, chatRoom,
						baseLayer, bbox, path, classification, tool, passwordHash, defaultRole, expiration,
						boundingPolygon, inviteOnly);
			} else {
				mission = missionService.createMission(name, creatorUid, groupVectorMission, description, chatRoom,
						baseLayer, bbox, path, classification, tool, passwordHash, defaultRole, -1L, boundingPolygon, inviteOnly);
			}

			MissionRole ownerRole = missionRoleRepository.findFirstByRole(MissionRole.Role.MISSION_OWNER);
			mission.setOwnerRole(ownerRole);

			if (missionPackage != null) {
				List<MissionChange> conflicts = new ArrayList<>();
				missionService.addMissionPackage(name, missionPackage, creatorUid, commonUtil().getAllInOutGroups(),
						conflicts);
				mission = missionService.getMission(name, groupVectorForAdminUser);
			}

			if (mission.getId() != 0) {
				return mission;
			} else {
				throw new Exception("Unable to create new mission");
			}
		}
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

		return points.stream().map(p -> p.replace(" ", "")).map(p -> p.replace(",", " "))
				.collect(Collectors.joining(","));
	}
	

	@Override
	public Mission deleteMission(String name, String creatorUid, boolean deepDelete) throws Exception {
		
		if (Strings.isNullOrEmpty(name)) {
			throw new IllegalArgumentException("Mission name cannot be empty or null");
		}

		logger.debug("delete mission " + name + ",creatorUid: " + creatorUid);
		
		MissionService missionService = missionService();

		final String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMissionByNameCheckGroups(name, groupVectorForAdmin);

		if (logger.isDebugEnabled()) {
			logger.debug("mission to delete: " + mission);
		}

		missionService.validateMission(mission, name);

		logger.debug("archiving mission");

		String serverName = ""; // Don't have server name

		byte[] archive = missionService.archiveMission(mission.getName(), groupVectorForAdmin, serverName);
		missionService.addMissionArchiveToEsync(mission.getName(), archive, groupVectorForAdmin, true);

		logger.debug("added archived mission to esync " + mission.getName());

		mission = missionService.deleteMission(name, creatorUid, groupVectorForAdmin, deepDelete);

		logger.debug("mission deleted");

		return mission;
	}

	@Override
    public Mission addMissionContent(String name, MissionContent content, String creatorUid) throws Exception {

		MissionService missionService = missionService();

		final String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		if (content.getHashes().isEmpty() && content.getUids().isEmpty()) {
			throw new IllegalArgumentException("at least one hash or uid must be provided in request");
		}

		Mission mission = missionService.addMissionContent(name, content, creatorUid, groupVectorForAdmin);

		return mission;
    	
    }
	
	@Override
	public Mission removeMissionContent(String name, String hash, String uid, String creatorUid) throws Exception {

		MissionService missionService = missionService();

		final String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		// remove the content and track change
		Mission mission = missionService.deleteMissionContent(name, hash, uid, creatorUid, groupVectorForAdmin);

		return mission;
	}

	@Override
	public Mission clearKeywords(String name, String creatorUid) {

		if (Strings.isNullOrEmpty(name)) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
		
		MissionService missionService = missionService();
		MissionRepository missionRepository = missionRepository();
		SubscriptionManagerLite subscriptionManager = subscriptionManager();

		final String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMission(missionService.trimName(name), groupVectorForAdmin);

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

		return mission;
	}

	@Override
	public Mission setKeywords(String name, List<String> keywords, String creatorUid) {

		MissionService missionService = missionService();
		MissionRepository missionRepository = missionRepository();
		SubscriptionManagerLite subscriptionManager = subscriptionManager();
		
		if (Strings.isNullOrEmpty(name)) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}

		if (keywords == null || keywords.isEmpty()) {
			throw new IllegalArgumentException("empty keywords array");
		}

		final String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMission(missionService.trimName(name), groupVectorForAdmin);

		// remove all keywords
		mission.getKeywords().clear();

		missionRepository.removeAllKeywordsForMission(mission.getId());

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

		return mission;
	}

	@Override
	public Mission removeKeyword(String name, String keyword, String creatorUid) {

		MissionService missionService = missionService();
		MissionRepository missionRepository = missionRepository();
		SubscriptionManagerLite subscriptionManager = subscriptionManager();

		if (Strings.isNullOrEmpty(name)) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}

		if (Strings.isNullOrEmpty(keyword)) {
			throw new IllegalArgumentException("keyword to delete must be provided");
		}

		final String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMission(missionService.trimName(name), groupVectorForAdmin);

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

		return mission;

	}


	@Override
	public void setParent(String childName, String parentName) {

		MissionService missionService = missionService();

		childName = missionService.trimName(childName);
		parentName = missionService.trimName(parentName);

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMissionByNameCheckGroups(childName, groupVectorForAdmin);
		missionService.validateMission(mission, childName);

		missionService.setParent(childName, parentName, groupVectorForAdmin);
	}

	@Override
	public void clearParent(String childName) throws Exception {

		MissionService missionService = missionService();

		childName = missionService.trimName(childName);

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMissionByNameCheckGroups(childName, groupVectorForAdmin);
		missionService.validateMission(mission, childName);

		missionService.clearParent(childName, groupVectorForAdmin);
	}

	@Override
	public Set<Mission> getChildren(String parentName) throws Exception {

		MissionService missionService = missionService();

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		parentName = missionService.trimName(parentName);

		Mission mission = missionService.getMissionByNameCheckGroups(parentName, groupVectorForAdmin);
		missionService.validateMission(mission, parentName);

		Set<Mission> children = missionService.getChildren(parentName, groupVectorForAdmin);

		return children;

	}

	@Override
	public Mission getParent(String parentName) {

		MissionService missionService = missionService();

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMission(missionService.trimName(parentName), groupVectorForAdmin);

		if (mission.getParent() == null) {
			throw new NotFoundException("Parent mission not found");
		}

		Mission re = missionService.hydrate(mission.getParent(), true);
		
		return re;

	}
	
	@Override
	public void addFeed(String missionName, String creatorUid, String dataFeedUid, String filterBbox, String filterType, String filterCallsign) {

		MissionService missionService = missionService();

		missionName = missionService.trimName(missionName);

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();
		
		Mission mission = missionService.getMission(missionName, groupVectorForAdmin);

		if (mission == null) {
			throw new NotFoundException("Mission not found");
		}
		try {
			missionService.addFeedToMission(mission.getName(), creatorUid, mission, dataFeedUid, filterBbox, filterType, filterCallsign);
    	} catch (Exception e) {
    		logger.error("exception in addFeed!", e);
    		throw e;
		}
    }

	@Override
	public void removeFeed(String missionName, String feedUid, String creatorUid) {

		MissionService missionService = missionService();

    	missionName = missionService.trimName(missionName);

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();
		
		Mission mission = missionService.getMission(missionName, groupVectorForAdmin);

		if (mission == null) {
			throw new NotFoundException("Mission not found");
		}
		try {
			missionService.removeFeedFromMission(mission.getName(), creatorUid, mission, feedUid);
		} catch (Exception e) {
			logger.error("exception in removeFeed!", e);
			throw e;
		}
	}

	@Override
	public MapLayer createMapLayer(String missionName, String creatorUid, MapLayer mapLayer) {

		MissionService missionService = missionService();

		missionName = missionService.trimName(missionName);

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMission(missionName, groupVectorForAdmin);
				
		if (mission == null) {
			throw new NotFoundException("Mission not found");
		}
		
		mapLayer.setMission(mission);

		MapLayer createdMapLayer  = missionService.addMapLayerToMission(missionName, creatorUid, mission, mapLayer);
		
		return createdMapLayer;

	}

	@Override
	public void deleteMapLayer(String missionName, String creatorUid, String mapLayerUid) {

		MissionService missionService = missionService();

		missionName = missionService.trimName(missionName);

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMission(missionName, groupVectorForAdmin);
		
		if (mission == null) {
			throw new NotFoundException("Mission not found");
		}

		missionService.removeMapLayerFromMission(missionName, creatorUid, mission, mapLayerUid);
	}

	@Override
	public MapLayer updateMapLayer(String missionName, String creatorUid, MapLayer mapLayer) {

		MissionService missionService = missionService();

		missionName = missionService.trimName(missionName);

		String groupVectorForAdmin = RemoteUtil.getInstance().getBitStringAllGroups();

		Mission mission = missionService.getMission(missionName, groupVectorForAdmin);

		if (mission == null) {
			throw new NotFoundException("Mission not found");
		}
		
		mapLayer.setMission(mission);

		MapLayer newMapLayer = missionService.updateMapLayer(missionName, creatorUid, mission, mapLayer);

		return newMapLayer;

	}
	
}
