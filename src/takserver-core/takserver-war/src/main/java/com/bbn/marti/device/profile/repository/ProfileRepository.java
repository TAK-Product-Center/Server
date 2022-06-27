package com.bbn.marti.device.profile.repository;

import com.bbn.marti.device.profile.model.Profile;
import com.bbn.marti.remote.util.RemoteUtil;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * Created on 5/8/2018.
 */
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    //List<Profile> findAllOrderById();
    List<Profile> findAll(Sort sort);

    Profile findByName(String name);

    @Query(value = "select name, groups, apply_on_enrollment, apply_on_connect, id, active, updated, tool from device_profile where " +
            "name = :name and " + RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    Profile findByNameAndGroupVector(@Param("name") String name,
                                     @Param("groupVector") String groupVector);

    void deleteById(Long id);

    @Query(value = "select name, groups, apply_on_enrollment, apply_on_connect, id, active, updated, tool from device_profile where " +
            "active = true and ( " +
            "(apply_on_enrollment = true and apply_on_enrollment = :applyOnEnrollment) " +
            "or (apply_on_connect = true and apply_on_connect = :applyOnConnect) ) and " +
            " (tool is null or char_length(tool)<=0) and " +
            RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    List<Profile> getAllProfiles(@Param("applyOnEnrollment") boolean applyOnEnrollment,
                                 @Param("applyOnConnect") boolean applyOnConnect,
                                 @Param("groupVector") String groupVector);

    @Query(value = "select name, groups, apply_on_enrollment, apply_on_connect, id, active, updated, tool from device_profile where " +
            "active = true and updated > :lastSyncTime and ( " +
            "(apply_on_enrollment = true and apply_on_enrollment = :applyOnEnrollment) " +
            "or (apply_on_connect = true and apply_on_connect = :applyOnConnect) ) and " +
            " (tool is null or char_length(tool)<=0) and " +
            RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    List<Profile> getLatestProfiles(@Param("applyOnEnrollment") boolean applyOnEnrollment,
                                    @Param("applyOnConnect") boolean applyOnConnect,
                                    @Param("lastSyncTime") Date lastSyncTime,
                                    @Param("groupVector") String groupVector);

    @Query(value = "select name, groups, apply_on_enrollment, apply_on_connect, id, active, updated, tool from device_profile where " +
            "active = true and tool = :tool and " +
            RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    List<Profile> getAllProfilesForTool(@Param("tool") String tool,
                                        @Param("groupVector") String groupVector);

    @Query(value = "select name, groups, apply_on_enrollment, apply_on_connect, id, active, updated, tool from device_profile where " +
            "active = true and updated > :lastSyncTime and tool = :tool and " +
            RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    List<Profile> getLatestProfilesForTool(@Param("tool") String tool,
                                    @Param("lastSyncTime") Date lastSyncTime,
                                    @Param("groupVector") String groupVector);

    @Query(value = "insert into device_profile (name, groups, updated) values (:name, "
            + RemoteUtil.GROUP_VECTOR + ", now()) returning id", nativeQuery = true)
    Long create(@Param("name") String name, @Param("groupVector") String groupVector);

    @Query(value = "update device_profile set groups = " + RemoteUtil.GROUP_VECTOR +
            ", updated=now() where id = :id returning id", nativeQuery = true)
    Long updateGroups(@Param("id") Long id, @Param("groupVector") String groupVector);

    @Query(value = "update device_profile set updated=now() where id = :id returning id", nativeQuery = true)
    Long updated(@Param("id") Long id);
}
