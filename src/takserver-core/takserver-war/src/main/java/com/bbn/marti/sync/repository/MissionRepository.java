package com.bbn.marti.sync.repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.model.Mission;

public interface MissionRepository extends JpaRepository<Mission, Long> {
	
	String missionAttributes = "select id, create_time, last_edited, name, creatoruid, groups, description, chatroom, base_layer, bbox, path, classification, tool, parent_mission_id, password_hash, default_role_id, expiration, bounding_polygon, invite_only, guid ";

    @Query(value = missionAttributes + " from mission where lower(name) = lower(:name) order by id desc limit 1", nativeQuery = true)
    Mission getByNameNoCache(@Param("name") String name);

    @Query(value = missionAttributes + " from mission where guid = uuid(:guid)", nativeQuery = true)
    Mission getByGuidNoCache(@Param("guid") UUID guid);

    @Query(value = missionAttributes + " from mission where guid = uuid(:guid)", nativeQuery = true)
    Mission getByGuid(@Param("guid") UUID missionGuid);
    
    // Only fetch the mission name for the guid (rather than the whole mission)
    @Query(value = "select name from mission where guid = uuid(:guid)", nativeQuery = true)
    String getMissionNameForMissionGuid(@Param("guid") UUID missionGuid);

    Long findMissionIdByName(String name);
        
    void deleteByName(String name);
    
    @Query(value = "insert into mission_resource (mission_id, resource_id, resource_hash) values (:mission_id, :resource_id, :hash) returning resource_hash", nativeQuery = true)
    void addMissionResource(@Param("mission_id") Long missionId, @Param("resource_id") Integer resourceId, @Param("hash") String hash);
    
    @Query(value = "delete from mission_resource where mission_id = :mission_id and resource_hash = :resource_hash returning resource_hash", nativeQuery = true)
    List<String> removeMissionResource(@Param("mission_id") Long missionId, @Param("resource_hash") String hash);
    
    @Query(value = "insert into mission_uid (mission_id, uid) values (:mission_id, :uid) returning uid", nativeQuery = true)
    void addMissionUid(@Param("mission_id") Long missionId, @Param("uid") String uid);
    
    @Query(value = "insert into mission_uid (mission_id, uid) values (:mission_id, :uid) returning uid", nativeQuery = true)
    void persist(@Param("mission_id") Long missionId, @Param("uid") String uid);
    
    @Query(value = "delete from mission_uid where mission_id = :mission_id and uid = :uid returning uid", nativeQuery = true)
    void removeMissionUid(@Param("mission_id") Long missionId, @Param("uid") String uid);
    
    @Query(value = "insert into mission_keyword (mission_id, keyword) values (:mission_id, :keyword) returning keyword", nativeQuery = true)
    void addMissionKeyword(@Param("mission_id") Long missionId, @Param("keyword") String keyword);
    
    @Query(value = "delete from mission_keyword where mission_id = :mission_id and keyword = :keyword returning keyword", nativeQuery = true)
    void removeMissionKeyword(@Param("mission_id") Long missionId, @Param("keyword") String keyword);
    
    @Query(value = "delete from mission_keyword where mission_id = :mission_id returning mission_id", nativeQuery = true)
    List<Long> removeAllKeywordsForMission(@Param("mission_id") Long missionId);

    @Query(value = "insert into mission_uid_keyword (mission_id, uid, keyword) values (:mission_id, :uid, :keyword) returning keyword", nativeQuery = true)
    void addMissionUidKeyword(@Param("mission_id") Long missionId, @Param("uid") String uid, @Param("keyword") String keyword);

    @Query(value = "delete from mission_uid_keyword where mission_id = :mission_id and uid = :uid returning mission_id", nativeQuery = true)
    List<Long> removeAllKeywordsForMissionUid(@Param("mission_id") Long missionId, @Param("uid") String uid);

    @Query(value = "insert into mission_resource_keyword (mission_id, hash, keyword) values (:mission_id, :hash, :keyword) returning keyword", nativeQuery = true)
    void addMissionResourceKeyword(@Param("mission_id") Long missionId, @Param("hash") String hash, @Param("keyword") String keyword);

    @Query(value = "delete from mission_resource_keyword where mission_id = :mission_id and hash = :hash returning mission_id", nativeQuery = true)
    List<Long> removeAllKeywordsForMissionResource(@Param("mission_id") Long missionId, @Param("hash") String hash);

    @Query(value = "delete from mission_resource cascade where mission_id = :id ;"
            + "delete from mission_uid cascade where mission_id = :id ;"
            + "delete from mission_keyword cascade where mission_id = :id ;"
            + "delete from mission_uid_keyword cascade where mission_id = :id ;"
            + "delete from mission_resource_keyword cascade where mission_id = :id ;"
            + "delete from mission_layer cascade where mission_id = :id ;"
            + "delete from maplayer cascade where mission_id = :id ;"
            + "delete from mission_feed cascade where mission_id = :id ;"
            + "delete from mission cascade where id = :id ;", nativeQuery = true)
    @Modifying // necessary so that spring doesn't expect a result from the query
    @Transactional
    void deleteMission(@Param("id") Long missionId);

    @Query(value = "select uid from mission_uid mu inner join mission m on m.id = mu.mission_id where lower(m.name) = lower(?)", nativeQuery = true)
    List<String> getMissionUids(String missionName);
    
    @Query(value = "select keyword from mission_keyword mk inner join mission m on m.id = mk.mission_id where lower(m.name) = lower(?)", nativeQuery = true)
    List<String> getMissionKeywords(String missionName);

    @Query(value = missionAttributes + " from mission where invite_only = true and tool = :tool  " +
            "and ( (lower(name) in ( select lower(mission_name) from mission_invitation where invitee = :userName and type = 'userName' )) or  " +
            "      (id in ( select mission_id from mission_subscription where username = :userName )) )" +
            "and " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<Mission> getInviteOnlyMissions(@Param("userName") String userName,  @Param("tool") String tool, @Param("groupVector") String groupVector);

    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissions(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector);


    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc offset :offset limit :limit",
            nativeQuery = true)
    List<Mission> getAllMissions(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector, @Param("limit")  int limit, @Param("offset") int offset);

    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "AND " + RemoteUtil.GROUP_CLAUSE,
            countQuery = "select count (*) from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "AND " + RemoteUtil.GROUP_CLAUSE,
            nativeQuery = true)
    Page<Mission> getAllMissionsPage(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector, Pageable pageable);

    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and name like %:name% " +
            "AND " + RemoteUtil.GROUP_CLAUSE,
            countQuery = "select count (*) from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and name like %:name% " +
            "AND " + RemoteUtil.GROUP_CLAUSE,
            nativeQuery = true)
    Page<Mission> getAllMissionsByNamePage(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector, @Param("name") String name, Pageable pageable);
    
    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and tool = :tool AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissionsByTool(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("tool") String tool, @Param("groupVector") String groupVector);

    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and tool = :tool AND " + RemoteUtil.GROUP_CLAUSE, 
            countQuery = "select count (*) from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and tool = :tool AND " + RemoteUtil.GROUP_CLAUSE,
            nativeQuery = true)
    Page<Mission> getAllMissionsByToolPage(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("tool") String tool, @Param("groupVector") String groupVector, Pageable pageable);

    @Query(value = missionAttributes + " from mission inner join mission_uid on mission.id = mission_uid.mission_id " +
    		"where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " + 
            "and uid like %:uid% AND " + RemoteUtil.GROUP_CLAUSE,  
            nativeQuery = true)
    List<Mission> getAllMissionsByUidPage(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("uid") String uid, @Param("groupVector") String groupVector, Pageable pageable);
    
    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and tool = :tool AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc offset :offset limit :limit", nativeQuery = true)
    List<Mission> getAllMissionsByTool(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("tool") String tool, @Param("groupVector") String groupVector,  @Param("limit")  int limit, @Param("offset") int offset);

    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) order by id desc " , nativeQuery = true)   // return new missions with default role of MISSION_SUBSCRIBER to older clients
    List<Mission> getAllMissionsNoGroupCheck(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole);

    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and tool = :tool order by id desc ", nativeQuery = true)
    List<Mission> getAllMissionsByToolNoGroupCheck(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("tool") String tool);

    
    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and tool in :tools AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissionsByTools(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("tools") List<String> tools, @Param("groupVector") String groupVector);

    @Query(value = missionAttributes + " from mission where name != 'exchecktemplates' and name != 'citrap' and invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "AND " + RemoteUtil.GROUP_CLAUSE + " and create_time < (now() - (:ttl * INTERVAL '1 second'))" + " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissionsByTtl(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole,
                                      @Param("groupVector") String groupVector, @Param("ttl") Integer ttl);

    @Query(value = missionAttributes + " from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and " + RemoteUtil.GROUP_CLAUSE +
            "and ((expiration != -1) and expiration <= :expiration)" +
            " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissionsByExpiration(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector, @Param("expiration") Long expiration);

    @Query(value = "select name from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +
            "AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<String> getMissionNames(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector);

    @Query(value = "select name from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +
            "and tool = :tool AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<String> getMissionNamesByTool(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("tool") String tool, @Param("groupVector") String groupVector);

    @Query(value = "insert into mission (create_time, name, creatoruid, groups, description, chatroom, base_layer, bbox, path, classification, tool, password_hash, expiration, bounding_polygon, invite_only, guid) values (:createTime, :name, :creatorUid, "
            + RemoteUtil.GROUP_VECTOR + ", :description, :chatRoom, :baseLayer, :bbox, :path, :classification, :tool, :passwordHash, :expiration, :boundingPolygon, :inviteOnly, :guid) returning id", nativeQuery = true)
    Long create(@Param("createTime") Date createTime, @Param("name") String name, @Param("creatorUid") String creatorUid, @Param("groupVector")
            String groupVector, @Param("description") String description, @Param("chatRoom") String chatRoom, @Param("baseLayer") String baseLayer, @Param("bbox") String bbox, @Param("path") String path, @Param("classification") String classification, @Param("tool") String tool, @Param("passwordHash") String passwordHash, @Param("expiration") Long expiration, @Param("boundingPolygon") String boundingPolygon, @Param("inviteOnly") Boolean inviteOnly,
            @Param("guid") UUID guid);

    @Query(value = "insert into mission (create_time, name, creatoruid, groups, description, chatroom, base_layer, bbox, path, classification, tool, password_hash, default_role_id, expiration, bounding_polygon, invite_only, guid) values (:createTime, :name, :creatorUid, "
            + RemoteUtil.GROUP_VECTOR + ", :description, :chatRoom, :baseLayer, :bbox, :path, :classification, :tool, :passwordHash, :defaultRoleId, :expiration, :boundingPolygon, :inviteOnly, :guid) returning id", nativeQuery = true)
    Long create(@Param("createTime") Date createTime, @Param("name") String name, @Param("creatorUid") String creatorUid, @Param("groupVector")
            String groupVector, @Param("description") String description, @Param("chatRoom") String chatRoom,  @Param("baseLayer") String baseLayer, 
            @Param("bbox") String bbox, @Param("path") String path, @Param("classification") String classification, @Param("tool") String tool, 
            @Param("passwordHash") String passwordHash, @Param("defaultRoleId") Long defaultRoleId, @Param("expiration") Long expiration, @Param("boundingPolygon") String boundingPolygon, @Param("inviteOnly") Boolean inviteOnly, 
            @Param("guid") UUID guid);

    @Query(value = "update mission set description = :description, chatroom = :chatRoom, base_layer = :baseLayer, bbox = :bbox, path = :path, classification = :classification, expiration = :expiration, bounding_polygon = :boundingPolygon where lower(name) = lower(:name) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long update(@Param("name") String name, @Param("groupVector") String groupVector, @Param("description") String description,
                @Param("chatRoom") String chatRoom, @Param("baseLayer") String baseLayer,  @Param("bbox") String bbox, @Param("path") String path, 
                @Param("classification") String classification, @Param("expiration") Long expiration, @Param("boundingPolygon") String boundingPolygon);

    @Query(value = "update mission set groups = cast(:groupVectorMission as bit(" + RemoteUtil.GROUPS_BIT_VECTOR_LEN + ")) where lower(name) = lower(:name) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long updateGroups(@Param("name") String name, @Param("groupVector") String groupVector, @Param("groupVectorMission") String groupVectorMission);

    @Query(value = "update mission set tool = :tool where lower(name) = lower(:name) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long updateTool(@Param("name") String name, @Param("groupVector") String groupVector, @Param("tool") String tool);

    @Query(value = "update mission set parent_mission_id = (select id from mission where guid = uuid(:parentMissionGuid)) where guid = uuid(:missionGuid) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long setParent(@Param("missionGuid") String missionGuid, @Param("parentMissionGuid") String parentMissionGuid, @Param("groupVector") String groupVector);

    @Query(value = "update mission set parent_mission_id = null where lower(name) = lower(:name) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long clearParent(@Param("name") String name, @Param("groupVector") String groupVector);
    
    @Query(value = "update mission set parent_mission_id = null where guid = uuid(:guid) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long clearParentByGuid(@Param("guid") String guid, @Param("groupVector") String groupVector);

    @Query(value = "select parent.name from mission parent, mission child where child.parent_mission_id = parent.id and lower(child.name) = lower(:name)", nativeQuery = true)
    String getParentName(@Param("name") String name);
    
    @Query(value = "select parent.guid from mission parent, mission child where child.parent_mission_id = parent.id and child.guid = uuid(:missionGuid)", nativeQuery = true)
    String getParentGuid(@Param("missionGuid") String missionGuid);
    
    @Query(value = "select parent.guid from mission parent, mission child where child.parent_mission_id = parent.id and child.guid = uuid(:missionGuid)", nativeQuery = true)
    String getParentMissionGuid(@Param("missionGuid") String missionGuid);

    @Query(value = "select child.name from mission parent, mission child where child.parent_mission_id = parent.id and lower(parent.name) = lower(:name)", nativeQuery = true)
    List<String> getChildNames(@Param("name") String name);
    
    @Query(value = "select child.guid from mission parent, mission child where child.parent_mission_id = parent.id and parent.guid = uuid(:guid)", nativeQuery = true)
    List<String> getChildGuids(@Param("guid") String guid);

    @Query(value = "update mission set password_hash = :passwordHash where lower(name) = lower(:name) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long setPasswordHash(@Param("name") String name, @Param("passwordHash") String password, @Param("groupVector") String groupVector);
    
    @Query(value = "update mission set password_hash = :passwordHash where guid = uuid(:guid) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long setPasswordHashByGuid(@Param("guid") String guid, @Param("passwordHash") String password, @Param("groupVector") String groupVector);

    @Query(value = "update mission set default_role_id = :defaultRoleId where lower(name) = lower(:name) and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long setDefaultRoleId(@Param("name") String name, @Param("defaultRoleId") Long defaultRoleId, @Param("groupVector") String groupVector);

    @Query(value = "select name from mission, mission_resource where resource_hash = :resource_hash and id = mission_id", nativeQuery = true)
    List<String> getMissionNamesContainingHash(@Param("resource_hash") String resource_hash);

    static final String COP_QUERY = "select m.id, m.create_time, max(mc.servertime) as last_edited, m.name, m.creatoruid, m.groups, m.description, m.chatroom, m.base_layer, " +
            "m.bbox, m.bounding_polygon, m.path, m.classification, m.tool, m.parent_mission_id, m.password_hash, m.default_role_id, m.expiration, m.invite_only, m.guid " +
            "from mission m inner join mission_change mc on mc.mission_id = m.id " +
            "where m.tool = :tool and invite_only = false and " +
            RemoteUtil.GROUP_CLAUSE + " and (:path is null or m.path = :path) group by m.id order by m.id desc";
    @Query(value = COP_QUERY, nativeQuery = true)
    List<Mission> getAllCopMissions(@Param("groupVector") String groupVector, @Param("path") String path, @Param("tool") String tool);

    @Query(value = COP_QUERY + " offset :offset rows fetch next :size rows only", nativeQuery = true)
    List<Mission> getAllCopMissionsWithPaging(@Param("groupVector") String groupVector, @Param("path") String path, @Param("tool") String tool, @Param("offset") Integer offset, @Param("size") Integer size);

    @Query(value = "select count(*) from public.mission where tool = :tool", nativeQuery= true)
    Long getMissionCountByTool(@Param("tool") String tool);
    
    @Query(value = "SELECT * FROM mission m INNER JOIN (SELECT DISTINCT data_feed_uid, mission_id FROM mission_feed) f ON f.mission_id = m.id WHERE f.data_feed_uid = :data_feed_uid", nativeQuery = true)
    List<Mission> getMissionsForDataFeed(@Param("data_feed_uid") String dataFeedUuid);

    @Query(value = "select distinct path from mission where tool = :tool and" + RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    List<String> getMissionPathsByTool(@Param("tool") String tool, @Param("groupVector") String groupVector);
    
    @Query(value = "select id from mission where lower(name) = lower(:missionName) order by id asc limit 1", nativeQuery = true)
    Long getLatestMissionIdForName(@Param("missionName") String missionName);
    
    @Query(value = "select id from mission where guid = uuid(:missionGuid) order by id asc limit 1", nativeQuery = true)
    Long getLatestMissionIdForMissionGuid(@Param("missionGuid") String missionGuid);
    
    @Query(value ="select count(*) from mission where invite_only = false and " +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) ",
            nativeQuery = true)
    int countAllMissions(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole);

}