package com.bbn.marti.sync.repository;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Tuple;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.sync.model.Mission;

// Using Mission as a entity type here as a placeholder - since it must be some managed type
public interface FederationEventRepository extends JpaRepository<Mission, Long> {

	// For a given federate name, get the latest time of a disconnect or connect event
    @Query(value = "select max(event_time) from fed_event where fed_name = :fedName and event_kind_id = 3 or event_kind_id = 1", nativeQuery = true)
    Date getLatestDistruptionEventTimeForFederate(@Param("fedName") String fedName);
    
    @Query(value = "insert into fed_event(fed_id, fed_name, remote, event_kind_id, event_time) values (:fedId, :fedName, :isRemote, 1, now()) returning 1", nativeQuery = true)
    void trackConnectEventForFederate(@Param("fedId") String fedId, @Param("fedName") String fedName, @Param("isRemote") boolean isRemote);
    
    // not currently used
    @Query(value = "insert into fed_event(fed_id, fed_name, remote, event_kind_id, event_time) values (:fedId, :fedName, :isRemote, 2, now()) returning 1", nativeQuery = true)
    void trackDisconnectEventForFederate(@Param("fedId") String fedId, @Param("fedName") String fedName, @Param("isRemote") boolean isRemote);
    
    @Query(value = "insert into fed_event(fed_id, fed_name, remote, event_kind_id, event_time) values (:fedId, :fedName, :isRemote, 3, now()) returning 1", nativeQuery = true)
    void trackSendChangesEventForFederate(@Param("fedId") String fedId, @Param("fedName") String fedName, @Param("isRemote") boolean isRemote);
    
    @Query(value = "select fe.event_time, fek.event_kind from fed_event fe inner join fed_event_kind_pl fek on fe.event_kind_id = fek.id where fe.fed_id = :fedId order by fe.event_time desc limit 1", nativeQuery = true)
    Tuple getLastEventForFederate(@Param("fedId") String fedId);
    
    @Query(value = "delete from fed_event returning 1", nativeQuery = true)
    void clearFederationEvents();
    
    @Query(value = "WITH resource_join AS (SELECT mission_id, resource_id, uid FROM resource INNER JOIN mission_resource ON mission_resource.resource_id = resource.id WHERE resource.submissiontime BETWEEN :start AND :end) select resource_join.uid from mission INNER JOIN resource_join ON mission.id = resource_join.mission_id;", nativeQuery = true)
    List<String> getMissionResourceHashesForTimeInterval(@Param("start") Date start, @Param("end") Date end);
    
    @Query(value = "WITH resource_join AS (SELECT mission_id, resource_id, uid FROM resource INNER JOIN mission_resource ON mission_resource.resource_id = resource.id WHERE resource.submissiontime BETWEEN :start AND :end) select resource_join.uid from mission INNER JOIN resource_join ON mission.id = resource_join.mission_id WHERE mission.tool = :tool ;", nativeQuery = true)
    List<String> getMissionResourceHashesForToolForTimeInterval(@Param("tool") String  tool, @Param("start") Date start, @Param("end") Date end);
}

