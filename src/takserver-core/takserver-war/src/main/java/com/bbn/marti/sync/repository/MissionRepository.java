package com.bbn.marti.sync.repository;

import java.util.Date;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.model.Mission;

import tak.server.cache.MissionCacheResolver;

public interface MissionRepository extends JpaRepository<Mission, Long> {
	
	String missionAttributes = "select id, create_time, last_edited, name, creatoruid, groups, description, chatroom, base_layer, bbox, path, classification, tool, parent_mission_id, password_hash, default_role_id, expiration, bounding_polygon";

    @Query(value = missionAttributes + " from mission where name = :name ", nativeQuery = true)
    Mission getByNameNoCache(@Param("name") String name);

    @Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER,  key="{#root.args[0] + '-byName'}", sync = true)
    @Query(value = missionAttributes + " from mission where name = :name ", nativeQuery = true)
    Mission getByName(@Param("name") String name);

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
            + "delete from mission cascade where id = :id returning id;", nativeQuery = true)
    void deleteMission(@Param("id") Long missionId);

    @Cacheable(cacheResolver = MissionCacheResolver.MISSION_CACHE_RESOLVER, key="{#root.methodName, #root.args[0]}")
    @Query(value = "select uid from mission_uid mu inner join mission m on m.id = mu.mission_id where m.name = ?", nativeQuery = true)
    List<String> getMissionUids(String missionName);

    @Query(value = missionAttributes + " from mission where" +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissions(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector);

    @Query(value = missionAttributes + " from mission where" +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and tool = :tool AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissionsByTool(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("tool") String tool, @Param("groupVector") String groupVector);

    @Query(value = missionAttributes + " from mission where" +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "AND " + RemoteUtil.GROUP_CLAUSE + " and create_time < (now() - (:ttl * INTERVAL '1 second'))" + " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissionsByTtl(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole,
                                      @Param("groupVector") String groupVector, @Param("ttl") Integer ttl);

    @Query(value = missionAttributes + " from mission where" +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +                       // only include password protected missions if asked to
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +   // return new missions with default role of MISSION_SUBSCRIBER to older clients
            "and " + RemoteUtil.GROUP_CLAUSE +
            "and ((expiration != -1) and expiration <= :expiration)" +
            " order by id desc ", nativeQuery = true)
    List<Mission> getAllMissionsByExpiration(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector, @Param("expiration") Long expiration);

    @Query(value = "select name from mission where" +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +
            "AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<String> getMissionNames(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("groupVector") String groupVector);

    @Query(value = "select name from mission where" +
            "((:passwordProtected = false and password_hash is null) or :passwordProtected = true)" +
            "and ((:defaultRole = false and (default_role_id is null or default_role_id = 2)) or :defaultRole = true) " +
            "and tool = :tool AND " + RemoteUtil.GROUP_CLAUSE + " order by id desc ", nativeQuery = true)
    List<String> getMissionNamesByTool(@Param("passwordProtected") boolean passwordProtected, @Param("defaultRole") boolean defaultRole, @Param("tool") String tool, @Param("groupVector") String groupVector);

    @Query(value = "insert into mission (create_time, name, creatoruid, groups, description, chatroom, base_layer, bbox, path, classification, tool, password_hash, expiration, bounding_polygon) values (:createTime, :name, :creatorUid, "
            + RemoteUtil.GROUP_VECTOR + ", :description, :chatRoom, :baseLayer, :bbox, :path, :classification, :tool, :passwordHash, :expiration, :boundingPolygon) returning id", nativeQuery = true)
    Long create(@Param("createTime") Date createTime, @Param("name") String name, @Param("creatorUid") String creatorUid, @Param("groupVector")
            String groupVector, @Param("description") String description, @Param("chatRoom") String chatRoom, @Param("baseLayer") String baseLayer, @Param("bbox") String bbox, @Param("path") String path, @Param("classification") String classification, @Param("tool") String tool, @Param("passwordHash") String passwordHash, @Param("expiration") Long expiration, @Param("boundingPolygon") String boundingPolygon);

    @Query(value = "insert into mission (create_time, name, creatoruid, groups, description, chatroom, base_layer, bbox, path, classification, tool, password_hash, default_role_id, expiration, bounding_polygon) values (:createTime, :name, :creatorUid, "
            + RemoteUtil.GROUP_VECTOR + ", :description, :chatRoom, :baseLayer, :bbox, :path, :classification, :tool, :passwordHash, :defaultRoleId, :expiration, :boundingPolygon) returning id", nativeQuery = true)
    Long create(@Param("createTime") Date createTime, @Param("name") String name, @Param("creatorUid") String creatorUid, @Param("groupVector")
            String groupVector, @Param("description") String description, @Param("chatRoom") String chatRoom,  @Param("baseLayer") String baseLayer, 
            @Param("bbox") String bbox, @Param("path") String path, @Param("classification") String classification, @Param("tool") String tool, 
            @Param("passwordHash") String passwordHash, @Param("defaultRoleId") Long defaultRoleId, @Param("expiration") Long expiration, @Param("boundingPolygon") String boundingPolygon);

    @Query(value = "update mission set description = :description, chatroom = :chatRoom, base_layer = :baseLayer, bbox = :bbox, path = :path, classification = :classification, expiration = :expiration, bounding_polygon = :boundingPolygon where name = :name and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long update(@Param("name") String name, @Param("groupVector") String groupVector, @Param("description") String description,
                @Param("chatRoom") String chatRoom, @Param("baseLayer") String baseLayer,  @Param("bbox") String bbox, @Param("path") String path, 
                @Param("classification") String classification, @Param("expiration") Long expiration, @Param("boundingPolygon") String boundingPolygon);

    @Query(value = "update mission set groups = cast(:groupVectorMission as bit(" + RemoteUtil.GROUPS_BIT_VECTOR_LEN + ")) where name = :name and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long updateGroups(@Param("name") String name, @Param("groupVector") String groupVector, @Param("groupVectorMission") String groupVectorMission);

    @Query(value = "update mission set tool = :tool where name = :name and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long updateTool(@Param("name") String name, @Param("groupVector") String groupVector, @Param("tool") String tool);

    @Query(value = "update mission set parent_mission_id = ( select id from mission where name = :parentName ) where name = :name and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long setParent(@Param("name") String name, @Param("parentName") String parentName, @Param("groupVector") String groupVector);

    @Query(value = "update mission set parent_mission_id = null where name = :name and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long clearParent(@Param("name") String name, @Param("groupVector") String groupVector);

    @Query(value = "select parent.name from mission parent, mission child where child.parent_mission_id = parent.id and child.name = :name", nativeQuery = true)
    String getParentName(@Param("name") String name);

    @Query(value = "select child.name from mission parent, mission child where child.parent_mission_id = parent.id and parent.name = :name", nativeQuery = true)
    List<String> getChildNames(@Param("name") String name);

    @Query(value = "update mission set password_hash = :passwordHash where name = :name and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long setPasswordHash(@Param("name") String name, @Param("passwordHash") String password, @Param("groupVector") String groupVector);

    @Query(value = "update mission set default_role_id = :defaultRoleId where name = :name and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long setDefaultRoleId(@Param("name") String name, @Param("defaultRoleId") Long defaultRoleId, @Param("groupVector") String groupVector);

    @Query(value = "select name from mission, mission_resource where resource_hash = :resource_hash and id = mission_id", nativeQuery = true)
    List<String> getMissionNamesContainingHash(@Param("resource_hash") String resource_hash);

    static final String COP_QUERY = "select m.id, m.create_time, max(mc.servertime) as last_edited, m.name, m.creatoruid, m.groups, m.description, m.chatroom, m.base_layer, " +
            "m.bbox, m.bounding_polygon, m.path, m.classification, m.tool, m.parent_mission_id, m.password_hash, m.default_role_id, m.expiration " +
            "from mission m inner join mission_change mc on mc.mission_id = m.id " +
            "where m.tool = :tool and " + RemoteUtil.GROUP_CLAUSE + " and (:path is null or path = :path) group by m.id order by m.id desc";
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
}