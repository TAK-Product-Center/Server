package com.bbn.marti.sync.repository;

import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.model.MissionInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MissionInvitationRepository extends JpaRepository<MissionInvitation, String> {

    List<MissionInvitation> findAllByMissionId(Long missionId);
    
    MissionInvitation findByToken(String token);

    MissionInvitation findByMissionIdAndTypeAndInvitee(Long missionId, String type, String invitee);

    
    @Query(value = "select mi.id, mi.mission_name, mi.invitee, mi.type, mi.creator_uid, mi.create_time, mi.token, mi.role_id, mi.mission_id, m.guid as mission_guid " +
            "from mission m inner join mission_invitation mi on m.id = mi.mission_id " +
            "where mi.invitee ~* :invitee and mi.type = :type and " + RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    List<MissionInvitation> findAllMissionInvitationsByInviteeIgnoreCaseAndType(
            @Param("invitee") String invitee, @Param("type") String type, @Param("groupVector") String groupVector);
}
