package com.bbn.marti.device.profile.repository;

import com.bbn.marti.device.profile.model.ProfileFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created on 5/8/2018.
 */
public interface ProfileFileRepository extends JpaRepository<ProfileFile, Long> {
    void deleteById(Long id);
    void deleteByProfileId(Long profileId);
    List<ProfileFile> findAllByProfileIdOrderById(long profileId);
}