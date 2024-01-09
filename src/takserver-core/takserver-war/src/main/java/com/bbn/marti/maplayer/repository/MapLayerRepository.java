

package com.bbn.marti.maplayer.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.bbn.marti.maplayer.model.MapLayer;

public interface MapLayerRepository extends JpaRepository<MapLayer, Long> {

    MapLayer findByUid(String uid);

    @Query(value = " select id, create_time, modified_time, uid, creator_uid, name, description, type, url, " +
            "default_layer, enabled, min_zoom, max_zoom, tile_type, server_parts, background_color, tile_update, " +
            "ignore_errors, invert_y_coordinate, north, south, east, west, coordinate_system, " +
            "additional_parameters, opacity, version, layers, " +
            "null as mission_id from maplayer  where uid = :uid ", nativeQuery = true)
    MapLayer findByUidNoMission(@Param("uid") String uid);

    List<MapLayer> findAllByMissionIsNull(Sort sort);

    @Modifying
    @Transactional
    @Query(value = "update maplayer set default_layer = false", nativeQuery = true)
    void unsetDefault();

    @Modifying
    @Transactional
    @Query(value = "delete from maplayer where mission_id = :mission_id", nativeQuery = true)
    void deleteAllByMissionId(@Param("mission_id") Long mission_id);

    @Modifying
    @Transactional
    @Query(value = "delete from maplayer where uid = :uid", nativeQuery = true)
    void deleteByUid(@Param("uid") String uid);
}

