

package com.bbn.marti.sync.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.sync.model.LogEntry;

public interface LogEntryRepository extends JpaRepository<LogEntry, String> {
    
    @Query("select count(le) from LogEntry le where :missionName in elements(le.missionNames) and le.servertime between :start and :end")
    int getLogCount(@Param("missionName") String missionName, @Param("start") Date start, @Param("end") Date end);

    @Query("select le from LogEntry le where :missionName in elements(le.missionNames) and le.servertime between :start and :end")
    List<LogEntry> getMissionLog(@Param("missionName") String missionName, @Param("start") Date start, @Param("end") Date end);
}
