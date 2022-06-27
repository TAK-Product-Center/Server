

package com.bbn.marti.dao.kml;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;

import com.bbn.marti.model.kml.Icon;


public interface IconRepository extends CrudRepository<Icon, Long> {
    
    @Cacheable("iconIconsetCache")
    Icon findIconByIconsetUidAndGroupAndName(String iconsetUid, String group, String name);
}