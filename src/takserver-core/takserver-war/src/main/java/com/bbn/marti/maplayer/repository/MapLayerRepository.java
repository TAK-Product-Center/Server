

package com.bbn.marti.maplayer.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.bbn.marti.maplayer.model.MapLayer;

public interface MapLayerRepository extends JpaRepository<MapLayer, Long> {

    MapLayer findByUid(String uid);
    List<MapLayer> findAll(Sort sort);

    @Transactional
    void deleteByUid(String uid);

    @Modifying
    @Transactional
    @Query(value = "update maplayer set default_layer = false")
    void unsetDefault();
}

