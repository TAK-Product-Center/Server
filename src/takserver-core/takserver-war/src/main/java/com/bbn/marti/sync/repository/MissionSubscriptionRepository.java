package com.bbn.marti.sync.repository;

import java.util.Date;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.bbn.marti.sync.model.MissionSubscription;

import tak.server.Constants;

public interface MissionSubscriptionRepository extends JpaRepository<MissionSubscription, String> {

    @Modifying
    @Transactional
    @Query(value = "insert into mission_subscription ( mission_id, client_uid, create_time, uid, token, role_id, username ) " +
            " values ( :missionId, :clientUid, :createTime, :uid, :token, :roleId, :username ) ", nativeQuery = true)
    @CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
    void subscribe(@Param("missionId") long missionId, @Param("clientUid") String clientUid, @Param("createTime") Date createTime,
                   @Param("uid") String uid, @Param("token") String token, @Param("roleId") long roleId, @Param("username") String username);

    @Modifying
    @Transactional
    @Query(value = "delete from mission_subscription where client_uid = :clientUid and mission_id in " +
            "( select id from mission where lower(name) = lower(:missionName) )  ", nativeQuery = true)
    @CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
    void deleteByMissionNameAndClientUid(@Param("missionName") String missionName, @Param("clientUid") String clientUid);
    
    @Modifying
    @Transactional
    @Query(value = "delete from mission_subscription where client_uid = :clientUid and mission_id in " +
            "( select id from mission where guid = uuid(:missionGuid))  ", nativeQuery = true)
    @CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
    void deleteByMissionGuidAndClientUid(@Param("missionGuid") String missionGuid, @Param("clientUid") String clientUid);

    @Modifying
    @Transactional
    @Query(value = "delete from mission_subscription where client_uid = :clientUid and ( username is null or username = :username ) and mission_id in " +
            "( select id from mission where lower(name) = lower(:missionName) )  ", nativeQuery = true)
    @CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
    void deleteByMissionNameAndClientUidAndUsername(@Param("missionName") String missionName, @Param("clientUid") String clientUid, @Param("username") String username);
    
    @Modifying
    @Transactional
    @Query(value = "delete from mission_subscription where client_uid = :clientUid and ( username is null or username = :username ) and mission_id in " +
            "( select id from mission where guid = uuid(:missionGuid)) )  ", nativeQuery = true)
    @CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
    void deleteByMissionGuidAndClientUidAndUsername(@Param("missionGuid") String missionGuid, @Param("clientUid") String clientUid, @Param("username") String username);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username,  ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where ms.client_uid = :clientUid and lower(m.name) = lower(:missionName)", nativeQuery = true)
    MissionSubscription findByMissionNameAndClientUidNoMission(@Param("missionName") String missionName, @Param("clientUid") String clientUid);
    
    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username,  ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where ms.client_uid = :clientUid and m.guid = uuid(:missionGuid)", nativeQuery = true)
    MissionSubscription findByMissionGuidAndClientUidNoMission(@Param("missionGuid") String missionGuid, @Param("clientUid") String clientUid);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username,  ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where ms.user_name = :username and lower(m.name) = lower(:missionName)", nativeQuery = true)
    MissionSubscription findByMissionNameAndUsernameNoMission(@Param("missionName") String missionName, @Param("username") String username);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where ms.client_uid = :clientUid and ( ms.username is null or ms.username = :username ) and lower(m.name) = lower(:missionName)", nativeQuery = true)
    MissionSubscription findByMissionNameAndClientUidAndUsernameNoMission(@Param("missionName") String missionName, @Param("clientUid") String clientUid, @Param("username") String username);
    
    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where ms.client_uid = :clientUid and ( ms.username is null or ms.username = :username ) and m.guid = uuid(:missionGuid)", nativeQuery = true)
    MissionSubscription findByMissionGuidAndClientUidAndUsernameNoMission(@Param("missionGuid") String missionGuid, @Param("clientUid") String clientUid, @Param("username") String username);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where lower(m.name) = lower(:missionName) and ms.uid = :uid", nativeQuery = true)
    MissionSubscription findByUidAndMissionNameNoMission(@Param("uid") String uid, @Param("missionName") String missionName);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where lower(m.name) = lower(:missionName)", nativeQuery = true)
    List<MissionSubscription> findAllByMissionNameNoMission(@Param("missionName") String missionName);
    
    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where m.guid = uuid(:missionGuid)", nativeQuery = true)
    List<MissionSubscription> findAllByMissionGuidNoMission(@Param("missionGuid") String missionGuid);

    @Query(value = "select ms.uid, null as token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where m.name = :missionName", nativeQuery = true)
    List<MissionSubscription> findAllByMissionNameNoMissionNoToken(@Param("missionName") String missionName);

    @Query(value = "select ms.uid, ms.token, m.id as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where m.tool = 'public' ", nativeQuery = true)
    List<MissionSubscription> findAll();
}


