

package com.bbn.marti.sync.repository;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.remote.sync.MissionChangeType;
import com.bbn.marti.sync.model.MissionChange;

import tak.server.cache.MissionChangeCacheResolver;

public interface MissionChangeRepository extends JpaRepository<MissionChange, Long> {

    static final String START_END_PRED = "\nand servertime between :start and :end\n";

    static final String MISSION_CREATES_AND_DELETES =
            "select id, change_type, hash, ts, servertime, uid, mc.creatoruid as creatoruid, null as mission_id, mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid\n" +
                    "from mission_change mc\n" +
                    "where mission_name = :missionName\n" +
                    "and change_type between 0 and 1\n" + // mission create and delete change types
                    "and ts >= (select max(ts) from mission_change where mission_name = :missionName and change_type = 0)\n" +
                    START_END_PRED;

    static final String HASH_ADDS =
            "select max(mc.id) as id, 2 as change_type, mc.hash as hash, max(mc.ts) as ts, max(mc.servertime) as servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc \n" +
                    "inner join resource r on mc.hash = r.hash\n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "inner join mission_resource mr on mr.resource_id = r.id and mr.mission_id = m.id\n" + //this join needs two criteria in the case of multiple missions and the same resource / or same hash different resource row
                    "where \n" +
                    "mc.change_type = 2" + // add change type by hash
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED +
                    "group by mc.hash, mc.creatoruid\n";

    static final String HASH_ADDS_FULL_HISTORY =
            "select mc.id as id, 2 as change_type, mc.hash as hash, mc.ts as ts, mc.servertime as servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc \n" +
                    "inner join resource r on mc.hash = r.hash\n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "left join mission_resource mr on mr.resource_id = r.id and mr.mission_id = m.id\n" + //this join needs two criteria in the case of multiple missions and the same resource / or same hash different resource row
                    "where \n" +
                    "mc.change_type = 2" + // add change type by hash
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED;

    static final String HASH_REMOVES =
            "select max(mc.id) as id, 3 as change_type, mc.hash as hash, max(mc.ts) as ts, max(mc.servertime) as servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc \n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "left join mission_resource mr on mc.hash = mr.resource_hash and mr.mission_id = m.id\n" + // inverse of the add content case
                    "where \n" +
                    "mc.change_type = 3" + // change type remove hash content
                    "and mc.hash is not null\n" +
                    "and mr.resource_hash is null\n" +
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED +
                    "group by mc.hash, mc.change_type, mc.creatoruid\n";

    static final String HASH_REMOVES_FULL_HISTORY =
            "select mc.id as id, 3 as change_type, mc.hash as hash, mc.ts as ts, mc.servertime as servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc \n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "left join mission_resource mr on mc.hash = mr.resource_hash and mr.mission_id = m.id\n" + // inverse of the add content case
                    "where \n" +
                    "mc.change_type = 3" + // change type remove hash content
                    "and mc.hash is not null\n" +
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED;

    static final String UID_ADDS =
            "select max(mc.id) as id, 2 as change_type, null as hash, max(mc.ts) as ts, max(mc.servertime) as servertime, mc.uid as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc\n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "inner join mission_uid mu on mc.uid = mu.uid and mu.mission_id = m.id\n" +
                    "where \n" +
                    "mc.change_type = 2\n" + // change type add uid content
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED +
                    "group by mc.uid, mc.creatoruid\n";

    static final String UID_ADDS_FULL_HISTORY =
            "select mc.id as id, 2 as change_type, null as hash, mc.ts as ts, mc.servertime as servertime, mc.uid as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc\n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "left join mission_uid mu on mc.uid = mu.uid and mu.mission_id = m.id\n" +
                    "where \n" +
                    "mc.change_type = 2\n" + // change type add uid content
                    "and mc.uid is not null\n" +
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED;

    static final String UID_REMOVES =
            "select max(mc.id) as id, 3 as change_type, null as hash, max(mc.ts) as ts, max(mc.servertime) as servertime, mc.uid as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc\n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "left join mission_uid mu on mc.uid = mu.uid and mu.mission_id = m.id\n" +
                    "where \n" +
                    "mc.change_type = 3\n" + // change type remove uid content
                    "and mc.uid is not null \n" +
                    "and m.name = :missionName\n" +
                    "and mu.uid is null\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED +
                    "group by mc.uid, mc.creatoruid\n";

    static final String UID_REMOVES_FULL_HISTORY =
            "select mc.id as id, 3 as change_type, null as hash, mc.ts as ts, mc.servertime as servertime, mc.uid as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc\n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "left join mission_uid mu on mc.uid = mu.uid and mu.mission_id = m.id\n" +
                    "where \n" +
                    "mc.change_type = 3\n" + // change type remove uid content
                    "and mc.uid is not null \n" +
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED;

    static final String EXTERNAL_DATA_ADDS =
            "select max(mc.id) as id, 2 as change_type, null as hash, max(mc.ts) as ts, max(mc.servertime) as servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, mc.external_data_uid as external_data_uid, mc.external_data_name as external_data_name, mc.external_data_tool as external_data_tool, mc.external_data_token as external_data_token, mc.external_data_notes as external_data_notes, null as mission_feed_uid, null as map_layer_uid  \n" +
                    "from mission_change mc \n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "left join mission_external_data med on med.id = mc.external_data_uid and med.mission_id = m.id\n" + //this join needs two criteria in the case of multiple missions and the same resource / or same hash different resource row
                    "where \n" +
                    "mc.change_type = 2\n" + // add change type by hash
                    "and mc.external_data_uid is not null \n" +
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED +
                    "group by mc.external_data_uid, mc.external_data_token, mc.external_data_name, mc.external_data_tool, mc.external_data_notes, mc.creatoruid\n";

    static final String EXTERNAL_DATA_REMOVES =
            "select max(mc.id) as id, 3 as change_type, null as hash, max(mc.ts) as ts, max(mc.servertime) as servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, mc.external_data_uid as external_data_uid, mc.external_data_name as external_data_name, mc.external_data_tool as external_data_tool, mc.external_data_token as external_data_token, mc.external_data_notes as external_data_notes, null as mission_feed_uid, null as map_layer_uid  \n" +
                    "from mission_change mc \n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    //"left join mission_external_data med on med.id = mc.external_data_uid and med.mission_id = m.id\n" + //this join needs two criteria in the case of multiple missions and the same resource / or same hash different resource row
                    "where \n" +
                    "mc.change_type = 3\n" + // add change type by hash
                    "and mc.external_data_uid is not null \n" +
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED +
                    "group by mc.external_data_uid, mc.external_data_token, mc.external_data_name, mc.external_data_tool, mc.external_data_notes, mc.creatoruid\n";


    static final String EXTERNAL_DATA_ADDS_FULL_HISTORY =
            "select mc.id as id, 2 as change_type, null as hash, mc.ts as ts, mc.servertime as servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, mc.external_data_uid as external_data_uid, mc.external_data_name as external_data_name, mc.external_data_tool as external_data_tool, mc.external_data_token as external_data_token, mc.external_data_notes as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc \n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "left join mission_external_data med on med.id = mc.external_data_uid and med.mission_id = m.id\n" + //this join needs two criteria in the case of multiple missions and the same resource / or same hash different resource row
                    "where \n" +
                    "mc.change_type = 2\n" + // add change type by hash
                    "and mc.external_data_uid is not null \n" +
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED;

    static final String EXTERNAL_DATA_REMOVES_FULL_HISTORY =
            "select mc.id as id, 3 as change_type, null as hash, mc.ts as ts, mc.servertime as servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, mc.external_data_uid as external_data_uid, mc.external_data_name as external_data_name, mc.external_data_tool as external_data_tool, mc.external_data_token as external_data_token, mc.external_data_notes as external_data_notes, null as mission_feed_uid, null as map_layer_uid \n" +
                    "from mission_change mc \n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    //"left join mission_external_data med on med.id = mc.external_data_uid and med.mission_id = m.id\n" + //this join needs two criteria in the case of multiple missions and the same resource / or same hash different resource row
                    "where \n" +
                    "mc.change_type = 3\n" + // add change type by hash
                    "and mc.external_data_uid is not null \n" +
                    "and m.name = :missionName\n" +
                    "and mc.ts >= m.create_time\n" +
                    START_END_PRED;

    static final String MAP_LAYERS =
            "select mc.id as id, change_type, null as hash, ts, servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, external_data_uid, external_data_name, external_data_tool, external_data_token, external_data_notes, mission_feed_uid, map_layer_uid \n" +
                    "from mission_change mc \n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "where \n" +
                    "(change_type = 2 or change_type = 3)\n" + // add change type by hash
                    "and m.name = :missionName\n" +
                    "and ts >= create_time\n" +
                    "and (map_layer_uid is not null)\n"+
                    START_END_PRED;
    
    static final String MISSION_FEEDS =
            "select mc.id as id, change_type, null as hash, ts, servertime, null as uid, mc.creatoruid as creatoruid, null as mission_id, :missionName as mission_name, 0 as mission_createTime, external_data_uid, external_data_name, external_data_tool, external_data_token, external_data_notes, mission_feed_uid, map_layer_uid \n" +
                    "from mission_change mc \n" +
                    "inner join mission m on mc.mission_id = m.id\n" +
                    "where \n" +
                    "(change_type = 4 or change_type = 5)\n" + // add change type by hash
                    "and m.name = :missionName\n" +
                    "and ts >= create_time\n" +
                    "and (mission_feed_uid is not null)\n"+
                    START_END_PRED;

    static final String MISSION_CHANGES = MISSION_CREATES_AND_DELETES + "union all " + HASH_ADDS + "union all " +  HASH_REMOVES + "union all " + UID_ADDS + "union all " + UID_REMOVES + "union all " + EXTERNAL_DATA_ADDS + "union all " + EXTERNAL_DATA_REMOVES + "union all " + MAP_LAYERS + "union all " + MISSION_FEEDS;
    static final String MISSION_CHANGES_FULL_HISTORY = MISSION_CREATES_AND_DELETES + "union all " + HASH_ADDS_FULL_HISTORY + "union all " +  HASH_REMOVES_FULL_HISTORY + "union all " + UID_ADDS_FULL_HISTORY + "union all " + UID_REMOVES_FULL_HISTORY + "union all " + EXTERNAL_DATA_ADDS_FULL_HISTORY + "union all " + EXTERNAL_DATA_REMOVES_FULL_HISTORY + "union all " + MAP_LAYERS + "union all " + MISSION_FEEDS;

    // Execute all mission change queries back-to-back, and combine the results
    @Query(value = MISSION_CHANGES, nativeQuery = true)
    Set<MissionChange> squashedChangesForMission(@Param("missionName") String missionName, @Param("start") Date start, @Param("end") Date end);

    @Query(value = MISSION_CHANGES_FULL_HISTORY, nativeQuery = true)
    Set<MissionChange> changesForMission(@Param("missionName") String missionName, @Param("start") Date start, @Param("end") Date end);

//    @Cacheable(cacheResolver = MissionChangeCacheResolver.MISSION_CHANGE_CACHE_RESOLVER)
    List<MissionChange> findByTypeAndMissionNameAndContentHashOrderByIdAsc(MissionChangeType type, String missionName, String contentHash);

//    @Cacheable(cacheResolver = MissionChangeCacheResolver.MISSION_CHANGE_CACHE_RESOLVER)
    @Query(value = "select distinct on (uid) id, change_type, hash, ts, servertime, uid, mc.creatoruid as creatoruid, null as mission_id, mission_name, 0 as mission_createTime, " +
            "null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid  " +
            "    from mission_change mc where mission_name = :missionName and change_type = :changeType and " +
            "    uid IN (:uids) " +
            "    order by uid, id desc ",
            nativeQuery = true)
    List<MissionChange> findLatestForUids(@Param("missionName") String missionName, @Param("uids") List<String> uids, @Param("changeType") int changeType);

//    @Cacheable(cacheResolver = MissionChangeCacheResolver.MISSION_CHANGE_CACHE_RESOLVER)
    @Query(value = "select distinct on (hash) id, change_type, hash, ts, servertime, uid, mc.creatoruid as creatoruid, null as mission_id, mission_name, 0 as mission_createTime, " +
            "null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid  " +
            "    from mission_change mc where mission_name = :missionName and change_type = :changeType and " +
            "    hash IN (:hashes) " +
            "    order by hash, id desc ",
            nativeQuery = true)
    List<MissionChange> findLatestForHashes(@Param("missionName") String missionName, @Param("hashes") List<String> hashes, @Param("changeType") int changeType);

//    @Cacheable(cacheResolver = MissionChangeCacheResolver.MISSION_CHANGE_CACHE_RESOLVER, key="{#root.methodName, #root.args[0]}")
    @Query(value = "select distinct on (uid, hash) id, change_type, hash, ts, servertime, uid, mc.creatoruid as creatoruid, null as mission_id, mission_name, 0 as mission_createTime, " +
            "null as external_data_uid, null as external_data_name, null as external_data_tool, null as external_data_token, null as external_data_notes, null as mission_feed_uid, null as map_layer_uid  " +
            "    from mission_change mc where mission_name = :missionName and change_type = :changeType and " +
            "    (uid IN (:uids) or hash IN (:hashes)) " +
            "    order by uid, hash, id desc ",
            nativeQuery = true)
    List<MissionChange> findLatest(@Param("missionName") String missionName, @Param("uids") List<String> uids, @Param("hashes") List<String> hashes, @Param("changeType") int changeType);
}