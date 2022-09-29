package com.bbn.marti.sync.repository;

import com.bbn.marti.sync.model.MissionSubscription;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tak.server.Constants;

import java.util.Date;
import java.util.List;

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
            "( select id from mission where name = :missionName )  ", nativeQuery = true)
    @CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
    void deleteByMissionNameAndClientUid(@Param("missionName") String missionName, @Param("clientUid") String clientUid);

    @Modifying
    @Transactional
    @Query(value = "delete from mission_subscription where client_uid = :clientUid and ( username is null or username = :username ) and mission_id in " +
            "( select id from mission where name = :missionName )  ", nativeQuery = true)
    @CacheEvict(value = Constants.MISSION_SUBSCRIPTION_CACHE, allEntries = true)
    void deleteByMissionNameAndClientUidAndUsername(@Param("missionName") String missionName, @Param("clientUid") String clientUid, @Param("username") String username);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username,  ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where ms.client_uid = :clientUid and m.name = :missionName", nativeQuery = true)
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
    MissionSubscription findByMissionNameAndClientUidNoMission(@Param("missionName") String missionName, @Param("clientUid") String clientUid);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username,  ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where ms.user_name = :username and m.name = :missionName", nativeQuery = true)
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
    MissionSubscription findByMissionNameAndUsernameNoMission(@Param("missionName") String missionName, @Param("username") String username);

    @Query(value = "select ms.uid, ms.username, ms.token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where ms.client_uid = :clientUid and ( ms.username is null or ms.username = :username ) and m.name = :missionName", nativeQuery = true)
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
    MissionSubscription findByMissionNameAndClientUidAndUsernameNoMission(@Param("missionName") String missionName, @Param("clientUid") String clientUid, @Param("username") String username);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where m.name = :missionName and ms.uid = :uid", nativeQuery = true)
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
    MissionSubscription findByUidAndMissionNameNoMission(@Param("uid") String uid, @Param("missionName") String missionName);

    @Query(value = "select ms.uid, ms.token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where m.name = :missionName", nativeQuery = true)
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
    List<MissionSubscription> findAllByMissionNameNoMission(@Param("missionName") String missionName);

    @Query(value = "select ms.uid, null as token, null as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where m.name = :missionName", nativeQuery = true)
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
    List<MissionSubscription> findAllByMissionNameNoMissionNoToken(@Param("missionName") String missionName);

    @Query(value = "select ms.uid, ms.token, m.id as mission_id, ms.client_uid, ms.username, ms.create_time, ms.role_id from mission m " +
            "inner join mission_subscription ms on m.id = ms.mission_id where m.tool = 'public' ", nativeQuery = true)
    @Cacheable(Constants.MISSION_SUBSCRIPTION_CACHE)
    List<MissionSubscription> findAll();
}


