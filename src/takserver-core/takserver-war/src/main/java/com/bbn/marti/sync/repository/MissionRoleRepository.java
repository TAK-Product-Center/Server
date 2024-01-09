package com.bbn.marti.sync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bbn.marti.sync.model.MissionRole;

public interface MissionRoleRepository extends JpaRepository<MissionRole, String> {
    MissionRole findFirstByRole(MissionRole.Role role);
    
    @Query(value = "select role from role where id = :roleId", nativeQuery = true)
    MissionRole getRoleByOrdinalId(long roleId);
}