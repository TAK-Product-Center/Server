package com.bbn.marti.sync.repository;

import com.bbn.marti.sync.model.MissionFeed;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MissionFeedRepository extends JpaRepository<MissionFeed, String> {
    MissionFeed save(MissionFeed missionFeed);

    MissionFeed getByUid(String uid);
    
    @Query(value = "select * from mission_feed where data_feed_uid = :dataFeedUid and mission_id = :missionId", nativeQuery = true)
    MissionFeed getMissionFeedByDataFeedUid(@Param("dataFeedUid") String dataFeedUid, @Param("missionId") long missionId);
    
    @Query(value = "select * from mission_feed where data_feed_uid = :dataFeedUid", nativeQuery = true)
    List<MissionFeed> getMissionFeedsByDataFeedUid(@Param("dataFeedUid") String dataFeedUid);

    @Query(value = "select m.name from mission m, mission_feed mf where mf.mission_id = m.id and mf.uid = :missionFeedUid", nativeQuery = true)
    String getMissionNameByMissionFeedUid(@Param("missionFeedUid") String missionFeedUid);
    
    @Query(value = "select m.guid from mission m, mission_feed mf where mf.mission_id = m.id and mf.uid = :missionFeedUid", nativeQuery = true)
    String getMissionGuidByMissionFeedUid(@Param("missionFeedUid") String missionFeedUid);

    @Query(value = "select uid, data_feed_uid, filter_polygon, filter_cot_types, filter_callsign, null as mission_id " +
            "from mission_feed where uid = :uid ", nativeQuery = true)
    MissionFeed getByUidNoMission(@Param("uid") String uid);

    @Transactional
    void deleteByUid(String uid);

    @Transactional
    @Modifying
    @Query(value = "delete from mission_feed where mission_id = :mission_id", nativeQuery = true)
    void deleteAllByMissionId(@Param("mission_id") Long mission_id);
}

