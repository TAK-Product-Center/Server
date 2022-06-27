package com.bbn.marti.sync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.sync.model.ExternalMissionData;

import java.util.List;

/**
 * Created on 2/9/2019.
 */
public interface ExternalMissionDataRepository extends JpaRepository<ExternalMissionData, String> {

    @Query(value = "select id, name, tool, url_data, url_display, null as mission_id, notes " +
            "from mission_external_data where id = :id ", nativeQuery = true)
    ExternalMissionData findByIdNoMission(@Param("id") String id);

    @Query(value = "insert into mission_external_data (id, name, tool, url_data, url_display, notes, mission_id ) values (:id, :name, :tool, :url_data, :url_display, :notes, :mission_id) returning id", nativeQuery = true)
    String create(@Param("id") String id, @Param("name") String name, @Param("tool") String tool, @Param("url_data") String url_data, @Param("url_display") String url_display, @Param("notes") String notes, @Param("mission_id") Long mission_id);

    @Query(value = "update mission_external_data set name = :name, tool = :tool, url_data = :url_data, url_display = :url_display, notes = :notes, mission_id = :mission_id where id = :id returning id", nativeQuery = true)
    List<String> update(@Param("id") String id, @Param("name") String name, @Param("tool") String tool, @Param("url_data") String url_data, @Param("url_display") String url_display, @Param("notes") String notes, @Param("mission_id") Long mission_id);
}
