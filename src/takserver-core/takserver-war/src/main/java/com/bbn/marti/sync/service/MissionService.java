package com.bbn.marti.sync.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.ResultSetExtractor;

import com.bbn.marti.config.GeospatialFilter;
import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.sync.model.ExternalMissionData;
import com.bbn.marti.sync.model.LogEntry;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.sync.model.MissionInvitation;
import com.bbn.marti.sync.model.MissionLayer;
import com.bbn.marti.sync.model.MissionPermission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.model.MissionSubscription;
import com.bbn.marti.sync.model.Resource;
import com.bbn.marti.sync.model.UidDetails;
import com.bbn.marti.sync.service.MissionTokenUtils.TokenType;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import tak.server.cache.CotCacheWrapper;
import tak.server.cot.CotElement;
import tak.server.cot.CotEventContainer;
import tak.server.feeds.DataFeedDTO;


/*
 * 
 * Mission API business logic interface
 * 
 * 
 */
public interface MissionService {
	
    Mission createMission(String name, String creatorUid, String groupVector, String description, String chatRoom, String baseLayer, String bbox, String path, String classification, String tool, String passwordHash, MissionRole defaultRole, Long expiration, String boundingPolygon, Boolean inviteOnly);
    
    Mission createMission(String name, String creatorUid, String groupVector, String description, String chatRoom, String baseLayer, String bbox, String path, String classification, String tool, String passwordHash, MissionRole defaultRole, Long expiration, String boundingPolygon, Boolean inviteOnly, UUID guid);
    
    Mission getMission(String missionName, boolean hydrateDetails);
    
    Mission getMissionByGuid(UUID missionGuid, boolean hydrateDetails);
    
    String getMissionNameByGuid(UUID missionGuid);

    Mission getMission(String missionName, String groupVector);

    Mission getMissionByGuid(UUID missionGuid, String groupVector);

    Mission getMissionNoDetails(String missionName, String groupVector);

    Mission getMissionNoContent(String missionName, String groupVector);
    
	Mission getMissionNoContentByGuid(UUID missionGuid, String groupVector);

    List<Mission> validateAccess(List<Mission> missions, HttpServletRequest request);

    List<Mission> getAllMissions(boolean passwordProtected, boolean defaultRole, String tool, NavigableSet<Group> groups);
    
    List<Mission> getMissionsFiltered(boolean passwordProtected, boolean defaultRole, String tool, NavigableSet<Group> groups, int limit, int offset, String sort, Boolean ascending, String nameFilter, String uidFilter);

    List<Mission> getAllCopsMissions(String tool, NavigableSet<Group> groups, String path, Integer page, Integer size);
    
    Long getMissionCount(String tool);

    Mission deleteMission(String name, String creatorUid, String groupVector, boolean deepDelete);
    
    Mission deleteMissionByGuid(UUID missionGuid, String creatorUid, String groupVector, boolean deepDelete);

    CotElement getLatestCotElement(String uid, String groupVector, Date end, ResultSetExtractor<CotElement> resultSetExtractor);

    CotElement getLatestCotForUid(String uid, String groupVector, Date end);

    CotElement getLatestCotForUid(String uid, String groupVector);

    List<CotElement> getAllCotForUid(String uid, Date start, Date end, String groupVector);

    String getCachedCot(String missionName, Set<String> uids, String groupVector);
    
    String getCachedCot(UUID missionGuid, Set<String> uids, String groupVector);

    List<CotElement> getCotElementsByTimeAndBbox(Date start, Date end, GeospatialFilter.BoundingBox boundingBox, String groupVector);

    List<CotElement> getLatestCotForUids(Set<String> uids, String groupVector);

    boolean deleteAllCotForUids(List<String> uids, String groupVector);

    void missionInvite(UUID missionGuid, String invitee, MissionInvitation.Type type, MissionRole role, String creatorUid, String groupVector);

    void missionInvite(Mission mission, MissionInvitation missionInvitation);

    void missionUninvite(UUID missionGuid, String invitee, MissionInvitation.Type type, String creatorUid, String groupVector);

    Set<MissionInvitation> getAllMissionInvitationsForClient(String clientUid, String groupVector);

    List<MissionInvitation> getMissionInvitations(String missionName);
    
    List<MissionInvitation> getMissionInvitationsByGuid(UUID missionGuid);

    List<Mission> getInviteOnlyMissions(String userName, String tool, NavigableSet<Group> groups);

    MissionSubscription missionSubscribe(UUID missionGuid, String clientUid, String groupVector);

    MissionSubscription missionSubscribe(UUID missionGuid, String clientUid, MissionRole missionRole, String groupVector);

    MissionSubscription missionSubscribe(UUID missionGuid, Long missionId, String clientUid, String username, MissionRole role, String groupVector);

    void missionUnsubscribe(UUID missionGuid, String uid, String username, String groupVector, boolean disconnectOnly);
    
    Mission addMissionContent(UUID missionGuid, MissionContent content, String creatorUid, String groupVector);
    
	Mission addMissionContentAtTime(UUID missionGuid, MissionContent missionContent, String creatorUid, String groupVector, Date date, String xmlContentForNotification);

    boolean addMissionPackage(UUID missionGuid, byte[] missionPackage, String creatorUid, NavigableSet<Group> groups, List<MissionChange> conflicts);

    Mission deleteMissionContent(UUID missionGuid, String hash, String uid, String creatorUid, String groupVector);

    Mission deleteMissionContentAtTime(UUID missionGuid, String hash, String uid, String creatorUid, String groupVector, Date date);

    byte[] archiveMission(UUID missionGuid, String groupVector, String serverName);

    void setParent(UUID childMissionGuid, UUID parentMissionGuid, String groupVector);

    void clearParent(UUID childGuid, String groupVector);

    boolean exists(String missionName, String groupVector);
    
    // utility methods

    Map<Integer, List<String>> hydrate(Set<Resource> resources);

    MissionLayer addMissionLayer(String missionName, Mission mission, String uid, String Name, MissionLayer.Type type, String parentUid, String afterUid, String creatorUid, String groupVector);
    
    void setLayerName(String missionName, Mission mission, String layerUid, String name, String creatorUid);

    void setLayerPosition(String missionName, Mission mission, String layerUid, String afterUid, String creatorUid);

    void setLayerParent(String missionName, Mission mission, String layerUid, String parentUid, String afterUid, String creatorUid);

    void removeMissionLayer(String missionName, Mission mission, String layerUid, String creatorUid, String groupVector);

    List<MissionLayer> hydrateMissionLayers(String missionName, Mission mission);

    MissionLayer hydrateMissionLayer(String missionName, Mission mission, String layerUid);

    Mission hydrate(Mission mission, boolean hydrateDetails);

    Resource hydrate(Resource resource);

    UidDetails hydrate(UidDetails uidDetails, String uid, Date timestamp);

    ExternalMissionData hydrate(String externalDataUid, String externalDataName, String externalDataTool, String externalDataToken, String externalDataNotes);

    String addMissionArchiveToEsync(String archiveName, byte[] archiveBytes, String groupVector, boolean archivedWhenDeleting);

    String trimName(String name);

    void validateMission(Mission mission, String missionName);
    
	void validateMissionByGuid(Mission mission);
    
    void invalidateMissionCache(String cacheName); // invalidate by name only
    
    void invalidateMissionCache(UUID missionGuid, String missionName); // invalidate by both name and guid
    
	void invalidateMissionCache(UUID missionGuid); // invalidate by guid only

    boolean isDeleted(String missionName);
    
	boolean isDeletedByGuid(UUID missionGuid);

    Set<MissionChange> getMissionChanges(String missionName, String groupVector, Long secago, Date start, Date end, boolean squashed);
    
    Set<MissionChange> getMissionChangesByGuid(UUID missionGuid, String groupVector, Long secago, Date start, Date end, boolean squashed);

    String getMissionKml(String missionName, String urlBase, String groupVector);

    String getMissionKml(UUID missionGuid, String urlBase, String groupVector);

    ExternalMissionData setExternalMissionData(UUID missionGuid, String creatorUid, ExternalMissionData externalMissionData, String groupVector);

    void deleteExternalMissionData(UUID missionGuid, String externalMissionDataId, String notes, String creatorUid, String groupVector);

    void notifyExternalMissionDataChanged(UUID missioinGuid, String externalMissionDataId, String token, String notes, String creatorUid, String groupVector);

    MissionChange getLatestMissionChangeForContentHash(UUID missionGuid, String contentHash);

    Set<Mission> getChildren(UUID missionGuid, String groupVector);

    List<LogEntry> getLogEntriesForMission(Mission mission, Long secago, Date start, Date end);

    void deleteLogEntry(String id, String groupVector);

    LogEntry addUpdateLogEntry(LogEntry entry, Date created, String groupVector);

    String generateToken(String uid, UUID missionGuid, String missionName, MissionTokenUtils.TokenType tokenType, long expirationMillis);

    MissionRole getRoleFromTypeAndInvitee(UUID missionGuid, String type, String invitee);

    MissionRole getRoleFromToken(Mission mission, MissionTokenUtils.TokenType[] validTokenTypes, HttpServletRequest request);

    boolean validateMissionCreateGroupsRegex(HttpServletRequest request);

    MissionRole getRoleForRequest(Mission mission, HttpServletRequest request);

    boolean validateRoleAssignment(Mission mission, HttpServletRequest request, MissionRole attemptAssign);

    void setRoleByClientUid(Long missionId, String clientUid, Long roleId);

    void setRoleByUsername(Long missionId, String username, Long roleId);

    void setRoleByClientUidOrUsername(Long missionId, String clientUid, String username, Long roleId);

    boolean setRole(Mission mission, String clientUid, String username, MissionRole role, String groupVector);

    void setSubscriptionUsername(Long missionId, String clientUid, String username);

    boolean validatePermission(MissionPermission.Permission permission, HttpServletRequest request);
    
    List<Map.Entry<String, String>> getAllMissionSubscriptions();

    List<Map.Entry<Map.Entry<String, String>, String>> getAllMissionSubscriptionsWithGuid();

    MissionRole getDefaultRole(Mission mission);
    
    int getApiVersionNumberFromRequest(HttpServletRequest request);

    boolean inviteOrUpdate(Mission mission, List<MissionSubscription> subscriptions, String creatorUid, String groupVector);

    void validatePassword(Mission mission, String password);

	CotEventContainer getLatestCotEventContainerForUid(String uid, String groupVector);

	// get mission by name (no group check)
    Mission getMissionByName(String missionName, boolean hydrateDetails);

    Mission getMissionByNameCheckGroups(String missionName, boolean hydrateDetails, String groupVector);

    Mission getMissionByNameCheckGroups(String missionName, String groupVector);
	
	Mission getMissionByGuidCheckGroups(UUID missionGuid, String groupVector);

    boolean setExpiration(String missionName, Long ttl, String groupVector);

    boolean setExpiration(UUID missionGuid, Long ttl, String groupVector);

    void deleteMissionByTtl(Integer ttl);

    void deleteMissionByExpiration(Long expiration);

	Map<Integer, List<String>> cachedMissionHydrate(String missionName, Set<Resource> resources);

	// resource and mission change caching
	Map<Integer, List<String>> getCachedResources(String missionName, Set<Resource> resources);
	
	// resource and mission change caching
	Map<Integer, List<String>> getCachedResourcesByGuid(UUID missionGuid, Set<Resource> resources);

	List<MissionChange> findLatestCachedMissionChanges(UUID missionGuid, List<String> uids, List<String> hashes, int changeType);

	List<MissionChange> findLatestCachedMissionChangesForUids(UUID missionGuid, List<String> uids, int changeType);

	List<MissionChange> findLatestCachedMissionChangesForHashes(UUID missionGuid, List<String> hashes, int changeType);

	Collection<CotCacheWrapper> getLatestMissionCotWrappersForUids(UUID missionGuid, Set<String> uids, String groupVector);

	List<Resource> getCachedResourcesByHash(UUID missionGuid, String hash);

    MissionFeed getMissionFeed(String missionFeedUid);

    DataFeedDTO getDataFeed(String dataFeedUid);
    
    MissionFeed addFeedToMission(String creatorUid, Mission mission, String dataFeedUid, String filterPolygon, List<String> filterCotTypes, String filterCallsign);
    
    MissionFeed addFeedToMission(String missionFeedUid, String creatorUid, Mission mission, String dataFeedUid, String filterPolygon, List<String> filterCotTypes, String filterCallsign);

	void removeFeedFromMission(String missionName, String creatorUid, Mission mission, String missionFeedUid);

	MapLayer getMapLayer(String mapLayerUid);

	MapLayer addMapLayerToMission(String missionName, String creatorUid, Mission mission, MapLayer mapLayer);
	
//	MapLayer addMapLayerToMissionByGuid(UUID missionGuid, String creatorUid, Mission mission, MapLayer mapLayer);

    MapLayer updateMapLayer(String missionName, String creatorUid, Mission mission, MapLayer mapLayer);
    
//    MapLayer updateMapLayerByGuid(UUID missionGuid, String creatorUid, Mission mission, MapLayer mapLayer);

    void removeMapLayerFromMission(String missionName, String creatorUid, Mission mission, String mapLayerUid);
    
//    void removeMapLayerFromMissionByGuid(UUID missionGuid, String creatorUid, Mission mission, String mapLayerUid);

    List<Mission> getMissionsForDataFeed(String feed_uid);

    void sendLatestFeedEvents(Mission mission, MissionFeed missionFeed, List<String> clientUidList, String groupVector);

	List<String> getMinimalMissionsJsonForDataFeed(String feed_uid) throws JsonProcessingException;
	
	int countAllMissions(boolean passwordProtected, boolean defaultRole, String tool);
	
    List<String> getMinimalMissionFeedsJsonForDataFeed(String dataFeedUid) throws JsonProcessingException;
    
	boolean validateAccess(Mission mission, HttpServletRequest request);

    List<String> getAllCotForString(String uidSearch, String groupVector);
    
    void hydrateFeedNameForMission(Mission mission);
    
    void hydrateMissionChangesForMission(Mission mission);
    
    void hydrateMissionChange(MissionChange missionChange);
    
    MissionSubscription getMissionSubcriptionByMissionNameAndClientUidAndUsernameNoMission(String missionName, String clientUid, String username);
    
    MissionSubscription getMissionSubcriptionByMissionGuidAndClientUidAndUsernameNoMission(String missionGuid, String clientUid, String username);
    
    MissionSubscription getMissionSubscriptionByMissionNameAndClientUidNoMission(String missionName, String clientUid);
    
    MissionSubscription getMissionSubscriptionByMissionGuidAndClientUidNoMission(String missionGuid, String clientUid);
    
    MissionSubscription getMissionSubscriptionByMissionNameAndUsernameNoMission(String missionName, String username);
    
    MissionSubscription getMissionSubscriptionByUidAndMissionNameNoMission(String uid, String missionName);
    
    List<MissionSubscription> getMissionSubscriptionsByMissionNameNoMission(String missionName);
    
    List<MissionSubscription> getMissionSubscriptionsByMissionGuidNoMission(UUID missionGuid);
    
    List<MissionSubscription> getMissionSubscriptionsByMissionNameNoMissionNoToken(String missionName);
    
    List<MissionSubscription> getMissionSubscriptionsByMissionGuidNoMissionNoToken(UUID missionGuid);

	List<String> getAllMissionsGuids(boolean passwordProtected, boolean defaultRole, String tool);
    
}