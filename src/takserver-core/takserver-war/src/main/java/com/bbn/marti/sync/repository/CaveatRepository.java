package com.bbn.marti.sync.repository;

import com.bbn.marti.sync.model.Caveat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CaveatRepository extends JpaRepository<Caveat, Long> {
	
	@Transactional
	@Modifying
	@Query(value = "delete from classification_caveat where caveat_id = :caveat_id", nativeQuery = true)
	void unlinkAllClassifications(@Param("caveat_id") long caveat_id);
	
	@Transactional
	Long deleteByName(String name);
	
	Caveat findByName(String name);
}