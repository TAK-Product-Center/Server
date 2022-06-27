package com.bbn.marti.device.profile.repository;

import com.bbn.marti.device.profile.model.ProfileDirectory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * Created on 5/8/2018.
 */
public interface ProfileDirectoryRepository extends JpaRepository<ProfileDirectory, Long> {
    void deleteById(Long id);
    void deleteByProfileId(Long profileId);
    List<ProfileDirectory> findAllByProfileIdOrderById(long profileId);
}