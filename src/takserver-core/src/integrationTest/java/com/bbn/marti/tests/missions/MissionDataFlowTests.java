package com.bbn.marti.tests.missions;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.MissionUserRole;

/**
 * Created on 10/28/15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MissionDataFlowTests extends AbstractConfigurationA {

	@Test(timeout = LONG_TIMEOUT)
	public void a_setupEnvironment() {
		innerSetupEnvironment(MissionDataFlowTests.class);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void b_adminAddMission() {
		engine.missionAdd(admin, missionName, groupProfile, MissionUserRole.MISSION_SUBSCRIBER);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void c_adminSubscribeMemberToMission() {
		engine.missionSubscribe(existingMember, missionName, existingMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void d_memberSendCotToMission() {
		engine.attemptSendFromUserAndVerify(existingMember, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void e_member_get_missionChanges() {
		engine.missionGetChanges(existingMember, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void f_memberAddFileResource() {
		dataAUploadFileHash = engine.fileAdd(admin, "testData.jpg", dataA);
		engine.missionAddResource(existingMember, missionName, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void g_adminGetMissionChanges() {
		engine.missionGetChanges(admin, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void h_subscribeNewMemberToMission() {
		engine.onlineAddUser(newMember);
		engine.connectClientsAndVerify(true, newMember);
		onfam.certmod(newMember.getCertPublicPemPath().toString(), null, null, false, null, newMember.getDefinedGroupSet().stringArray(), null, null);
		engine.missionSubscribe(newMember, missionName, newMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void i_newMemberGetMissionChanges() {
		engine.missionGetChanges(newMember, missionName);

	}

	@Test(timeout = SHORT_TIMEOUT)
	public void j_newMemberSetKeywords() {
		engine.missionSetKeywords(newMember, missionName, "KeywordA", "KeywordB", "KeywordC");
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void k_memberGetMissionChanges() {
		engine.missionGetChanges(existingMember, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void l_newMemberGetMissionDetails() {
		engine.missionDetailsGetByName(missionName, newMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void m_adminGetMissionFile() {
		engine.missionDetailsGetByName(missionName, admin);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void n_newMemberGetMissionFile() {
		engine.missionDetailsGetByName(missionName, newMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void o_newMemberSendCotToMission() {
		engine.attemptSendFromUserAndVerify(newMember, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void p_memberSendCotToMission() {
		engine.attemptSendFromUserAndVerify(existingMember, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void q_adminGetMissionChanges() {
		engine.missionGetChanges(admin, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void r_memberGetMissionDetails() {
		engine.missionDetailsGetByName(missionName, existingMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void s_subscribeOwnerToMission() {
		engine.missionSubscribe(admin, missionName, missionOwner);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void t_setOwnerRole() {
		engine.missionSetUserRole(admin, missionName, missionOwner, MissionUserRole.MISSION_OWNER);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void u_missionOwnerGetChanges() {
		engine.missionGetChanges(missionOwner, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void v_ownerDeleteMission() {
		engine.missionDelete(missionOwner, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void w_memberGetMissionChanges() {
		engine.missionGetChanges(existingMember, missionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void x_newMemberGetMission() {
		engine.missionDetailsGetByName(missionName, newMember);
	}

	@Test(timeout=SHORT_TIMEOUT)
	public void zzz_teardown() {
		try {
			engine.stopServers(getServers());
		} catch (Exception e) {
			// It doesn't matter if we run into issues here, so print and move on
			System.err.println(e.getMessage());
		}
	}
}
