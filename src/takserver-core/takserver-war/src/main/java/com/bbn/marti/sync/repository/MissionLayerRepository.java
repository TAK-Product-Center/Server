package com.bbn.marti.sync.repository;

import com.bbn.marti.sync.model.MissionLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface MissionLayerRepository extends JpaRepository<MissionLayer, String> {

    @Modifying
    @Transactional
    @Query(value = "insert into mission_layer ( uid, name, type, parent_node_uid, after, mission_id ) " +
            " values ( :uid, :name, :type, :parentNodeUid, :afterUid, :missionId ) ", nativeQuery = true)
    void save(@Param("uid") String uid, @Param("name") String name, @Param("type") int type,
                   @Param("parentNodeUid") String parentNodeUid, @Param("afterUid") String afterUid, @Param("missionId") Long missionId);

    @Modifying
    @Transactional
    @Query(value = "update mission_layer set name = :name where uid = :uid ", nativeQuery = true)
    void setName(@Param("uid") String uid, @Param("name") String name);

    @Modifying
    @Transactional
    @Query(value = "update mission_layer set parent_node_uid = :parentNodeUid, after = null where uid = :uid ", nativeQuery = true)
    void setParent(@Param("uid") String uid, @Param("parentNodeUid") String parentNodeUid);

    @Modifying
    @Transactional
    @Query(value = "update mission_layer set after = :after where uid = :uid and " +
            "((:parent_node_uid is null and parent_node_uid is null) or (:parent_node_uid is not null and parent_node_uid = :parent_node_uid))  ", nativeQuery = true)
    void setAfter(@Param("uid") String uid, @Param("parent_node_uid") String parent_node_uid, @Param("after") String after);

    @Modifying
    @Transactional
    @Query(value = "update mission_layer set after = :uid where " +
            "(:uid is null  or (:uid != uid)) and " +
            "((:parent_node_uid is null and parent_node_uid is null) or (:parent_node_uid is not null and parent_node_uid = :parent_node_uid)) and " +
            "((:after is null and after is null) or (:after is not null and after = :after)) ", nativeQuery = true)
    void fixupAfter(@Param("uid") String uid, @Param("parent_node_uid") String parent_node_uid, @Param("after") String after);

    @Query(value = "select uid, name, type, parent_node_uid, null as mission_id, after " +
            "from mission_layer where uid = :uid ", nativeQuery = true)
    MissionLayer findByUidNoMission(@Param("uid") String uid);

    @Query(value = "select ml.uid, ml.name, ml.type, ml.parent_node_uid, null as mission_id, ml.after " +
            "from mission_layer ml inner join mission m on m.id = ml.mission_id where lower(m.name) = lower(:missionName) and ml.parent_node_uid is null ", nativeQuery = true)
    List<MissionLayer> findMissionLayers(@Param("missionName") String missionName);

    @Query(value = "delete from mission_layer where uid = :uid returning :uid", nativeQuery = true)
    void deleteByUid(@Param("uid") String uid);
}
