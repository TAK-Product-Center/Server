package com.bbn.marti.sync.repository;

import com.bbn.marti.sync.model.Classification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ClassificationRepository extends JpaRepository<Classification, Long> {
	
//	@Transactional
//	Long deleteByLevel(String level);
	
	@Transactional
	@Modifying
	@Query(value = "delete from classification where id = :classification_id", nativeQuery = true)
	void deleteClassificationOnly(@Param("classification_id") long classification_id);
	
	Classification findByLevel(String level);
	
	@Transactional
	@Modifying
	@Query(value = "delete from classification_caveat where classification_id = :classification_id", nativeQuery = true)
	void unlinkAllCaveats(@Param("classification_id") long classification_id);
	
	@Transactional
	@Modifying
	@Query(value = "insert into classification_caveat(classification_id, caveat_id) values (:classification_id, :caveat_id)", nativeQuery = true)
	void linkCaveat(@Param("classification_id") long classification_id, @Param("caveat_id") long caveat_id);

}