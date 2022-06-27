package com.bbn.marti.sync.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bbn.marti.sync.model.MissionRole;

import tak.server.Constants;

public interface MissionRoleRepository extends JpaRepository<MissionRole, String> {
    @Cacheable(value = Constants.MISSION_ROLE_CACHE)
    MissionRole findFirstByRole(MissionRole.Role role);
}