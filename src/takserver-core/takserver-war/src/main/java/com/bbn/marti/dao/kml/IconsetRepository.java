

package com.bbn.marti.dao.kml;

import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.bbn.marti.model.kml.Iconset;

public interface IconsetRepository extends CrudRepository<Iconset, Long> { 
    Long countByUid(String uid);
    
    // invalidate the whole iconIconsetCache when deleting any iconset
    @CacheEvict(value = "iconIconsetCache", allEntries = true)
    void deleteByUid(String uid);
    
    Iconset findByUid(String uid);
    
    @Query("select i.uid from Iconset i")
    Set<String> getAllUids();
    
}