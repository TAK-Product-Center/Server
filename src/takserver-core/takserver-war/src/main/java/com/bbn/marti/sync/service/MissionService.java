package com.bbn.marti.sync.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Pageable;
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
import com.fasterxml.jackson.core.JsonProcessingException;

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

    CotElement getLatestCotElement(String uid, String groupVector, Date end, ResultSetExtractor<CotElement> resultSetExtractor);

    CotElement getLatestCotForUid(String uid, String groupVector, Date end);

    CotElement getLatestCotForUid(String uid, String groupVector);

    List<CotElement> getAllCotForUid(String uid, Date start, Date end, String groupVector);

    String getCachedCot(String missionName, Set<String> uids, String groupVector);

    List<CotElement> getCotElementsByTimeAndBbox(Date start, Date end, GeospatialFilter.BoundingBox boundingBox, String groupVector);

    List<CotElement> getLatestCotForUids(Set<String> uids, String groupVector);

    boolean deleteAllCotForUids(List<String> uids, String groupVector);

    void missionInvite(String missionName, String invitee, MissionInvitation.Type type, MissionRole role, String creatorUid, String groupVector);

    void missionInvite(Mission mission, MissionInvitation missionInvitation);

    void missionUninvite(String missionName, String invitee, MissionInvitation.Type type, String creatorUid, String groupVector);

    Set<MissionInvitation> getAllMissionInvitationsForClient(String clientUid, String groupVector);

    List<MissionInvitation> getMissionInvitations(String missionName);

    List<Mission> getInviteOnlyMissions(String userName, String tool, NavigableSet<Group> groups);

    MissionSubscription missionSubscribe(String missionName, String clientUid, String groupVector);

    MissionSubscription missionSubscribe(String missionName, String clientUid, MissionRole missionRole, String groupVector);

    MissionSubscription missionSubscribe(String missionName, Long missionId, String clientUid, String username, MissionRole role, String groupVector);

    void missionUnsubscribe(String missionName, String uid, String username, String groupVector, boolean disconnectOnly);

    Mission addMissionContent(String missionName, MissionContent content, String creatorUid, String groupVector);

    Mission addMissionContentAtTime(String missionName, MissionContent missionContent, String creatorUid, String groupVector, Date date, String xmlContentForNotification);

    boolean addMissionPackage(String missionName, byte[] missionPackage, String creatorUid, NavigableSet<Group> groups, List<MissionChange> conflicts);

    Mission deleteMissionContent(String missionName, String hash, String uid, String creatorUid, String groupVector);

    Mission deleteMissionContentAtTime(String missionName, String hash, String uid, String creatorUid, String groupVector, Date date);

    byte[] archiveMission(String missionName, String groupVector, String serverName);

    void setParent(String childName, String parentName, String groupVector);

    void clearParent(String childName, String groupVector);

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

    ExternalMissionData hydrate(String externalDataUid, String externalDataName, String externalDataTool,
                                String externalDataToken, String externalDataNotes);

    String addMissionArchiveToEsync(String name, byte[] archive, String groupVector, boolean archivedWhenDeleting);

    String trimName(String name);

    void validateMission(Mission mission, String missionName);
    
	void validateMissionByGuid(Mission mission, UUID missionGuid);
    
    void invalidateMissionCache(String cacheName);

    boolean isDeleted(String missionName);
    
	boolean isDeletedByGuid(UUID missionGuid);

    Set<MissionChange> getMissionChanges(String missionName, String groupVector, Long secago, Date start, Date end, boolean squashed);
    
    Set<MissionChange> getMissionChangesByGuid(UUID missionGuid, String groupVector, Long secago, Date start, Date end, boolean squashed);

    String getMissionKml(String missionName, String urlBase, String groupVector);

    ExternalMissionData setExternalMissionData(String missionName, String creatorUid, ExternalMissionData externalMissionData, String groupVector);

    void deleteExternalMissionData(String missionName, String externalMissionDataId, String notes, String creatorUid, String groupVector);

    void notifyExternalMissionDataChanged(String missionName, String externalMissionDataId, String token, String notes, String creatorUid, String groupVector);

    MissionChange getLatestMissionChangeForContentHash(String missionName, String contentHash);

    Set<Mission> getChildren(String missionName, String groupVector);

    List<LogEntry> getLogEntriesForMission(Mission mission, Long secago, Date start, Date end);

    void deleteLogEntry(String id, String groupVector);

    LogEntry addUpdateLogEntry(LogEntry entry, Date created, String groupVector);

    String generateToken(String uid, String missionName, MissionTokenUtils.TokenType tokenType, long expirationMillis);

    MissionRole getRoleFromTypeAndInvitee(String missionName, String type, String invitee);

    MissionRole getRoleFromToken(Mission mission, MissionTokenUtils.TokenType[] validTokenTypes, HttpServletRequest request);

    MissionRole getRoleForRequest(Mission mission, HttpServletRequest request);

    boolean validateRoleAssignment(Mission mission, HttpServletRequest request, MissionRole attemptAssign);

    void setRoleByClientUid(Long missionId, String clientUid, Long roleId);

    void setRoleByUsername(Long missionId, String username, Long roleId);

    void setRoleByClientUidOrUsername(Long missionId, String clientUid, String username, Long roleId);

    boolean setRole(Mission mission, String clientUid, String username, MissionRole role, String groupVector);

    void setSubscriptionUsername(Long missionId, String clientUid, String username);

    boolean validatePermission(MissionPermission.Permission permission, HttpServletRequest request);

    List<Map.Entry<String, String>> getAllMissionSubscriptions();

    MissionRole getDefaultRole(Mission mission);
    
    int getApiVersionNumberFromRequest(HttpServletRequest request);

    boolean inviteOrUpdate(Mission mission, List<MissionSubscription> subscriptions, String creatorUid, String groupVector);

    void validatePassword(Mission mission, String password);

	CotEventContainer getLatestCotEventContainerForUid(String uid, String groupVector);

	Mission getMissionByNameCheckGroups(String missionName, String groupVector);
	
	Mission getMissionByGuidCheckGroups(UUID missionGuid, String groupVector);

    boolean setExpiration(String missionName, Long ttl, String groupVector);

    void deleteMissionByTtl(Integer ttl);

    void deleteMissionByExpiration(Long expiration);

	Map<Integer, List<String>> cachedMissionHydrate(String missionName, Set<Resource> resources);

	// resource and mission change caching
	Map<Integer, List<String>> getCachedResources(String missionName, Set<Resource> resources);

	List<MissionChange> findLatestCachedMissionChanges(String missionName, List<String> uids, List<String> hashes, int changeType);

	List<MissionChange> findLatestCachedMissionChangesForUids(String missionName, List<String> uids, int changeType);

	List<MissionChange> findLatestCachedMissionChangesForHashes(String missionName, List<String> hashes, int changeType);

	Collection<CotCacheWrapper> getLatestMissionCotWrappersForUids(String missionName, Set<String> uids, String groupVector);

	List<Resource> getCachedResourcesByHash(String missionName, String hash);

    MissionFeed getMissionFeed(String missionFeedUid);

    DataFeedDTO getDataFeed(String dataFeedUid);
    
    MissionFeed addFeedToMission(String missionName, String creatorUid, Mission mission, String dataFeedUid, String filterPolygon, List<String> filterCotTypes, String filterCallsign);
    
    MissionFeed addFeedToMission(String missionFeedUid, String missionName, String creatorUid, Mission mission, String dataFeedUid, String filterPolygon, List<String> filterCotTypes, String filterCallsign);

	void removeFeedFromMission(String missionName, String creatorUid, Mission mission, String missionFeedUid);

	MapLayer getMapLayer(String mapLayerUid);

	MapLayer addMapLayerToMission(String missionName, String creatorUid, Mission mission, MapLayer mapLayer);

    MapLayer updateMapLayer(String missionName, String creatorUid, Mission mission, MapLayer mapLayer);

    void removeMapLayerFromMission(String missionName, String creatorUid, Mission mission, String mapLayerUid);

    List<Mission> getMissionsForDataFeed(String feed_uid);

    void sendLatestFeedEvents(Mission mission, MissionFeed missionFeed, List<String> clientUidList, String groupVector);

	List<String> getMinimalMissionsJsonForDataFeed(String feed_uid) throws JsonProcessingException;
	
	int countAllMissions(boolean passwordProtected, boolean defaultRole, String tool);
	
    List<String> getMinimalMissionFeedsJsonForDataFeed(String dataFeedUid) throws JsonProcessingException;
    
	List<Mission> getAllMissionsCached(boolean passwordProtected, boolean defaultRole, String tool);

	boolean validateAccess(Mission mission, HttpServletRequest request);

    List<String> getAllCotForString(String uidSearch, String groupVector);
}