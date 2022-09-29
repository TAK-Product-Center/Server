package com.bbn.marti.sync.repository;

import java.util.Date;
import java.util.List;

import javax.persistence.Tuple;

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
    
    @Query(value = "select uid from resource where submissiontime between :start and :end", nativeQuery = true)
    List<String> getResourceHashesForTimeInterval(@Param("start") Date start, @Param("end") Date end);
    
    @Query(value = "delete from fed_event returning 1", nativeQuery = true)
    void clearFederationEvents();
}

