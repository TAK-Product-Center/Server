

package com.bbn.marti.sync.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.model.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

public interface ResourceRepository extends JpaRepository<Resource, Integer> { 
	
	String resourceAttributes = "select id, filename, keywords, mimetype, name, submissiontime, submitter, uid, hash, data, tool, location, altitude, groups from resource";

    Resource findOneByHash(String hash);

    List<Resource> findByHash(String hash);
    List<Resource> findByUid(String uid);

    @Query(value = "update resource set groups = cast(:groupVectorResource as bit(" + RemoteUtil.GROUPS_BIT_VECTOR_LEN + ")) where hash = :hash and" + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long updateGroups(@Param("hash") String hash, @Param("groupVector") String groupVector, @Param("groupVectorResource") String groupVectorResource);
    
    List<Resource> findAll();
    Page<Resource> findByKeywordsContaining(String keywords, Pageable pageable);
    
    long count();
    
}