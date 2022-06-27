package com.bbn.marti.tests.missions;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Created on 10/28/15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MissionAddRetrieveRemove extends AbstractConfigurationA {

	@Test(timeout = LONG_TIMEOUT)
	public void a_setupEnvironment() {
		innerSetupEnvironment(MissionAddRetrieveRemove.class);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void b_ownerAddMission() {
		engine.missionAdd(admin, missionName, groupProfile, null);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void c_ownerGetMission() {
		engine.missionDetailsGetByName(missionName, admin);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void d_memberGetMission() {
		engine.missionDetailsGetByName(missionName, existingMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void e_nonMemberGetMission() {
		engine.missionDetailsGetByName(missionName, nonMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void f_addNewGroupMember() {
		engine.onlineAddUser(newMember);
		engine.connectClientsAndVerify(true, newMember);
		onfam.certmod(newMember.getCertPublicPemPath().toString(), null, null, false, null, newMember.getDefinedGroupSet().stringArray(), null, null);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void h_newMemberGetMission() {
		engine.missionDetailsGetByName(missionName, newMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void i_ownerAddSecondMission() {
		engine.missionAdd(admin, missionName2, groupProfile, null);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void j_ownerGetAllMissions() {
		engine.missionDetailsGet(admin);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void k_memberGetAllMissions() {
		engine.missionDetailsGet(existingMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void l_newMemberGetAllMissions() {
		engine.missionDetailsGet(newMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void m_ownerDeleteFirstMission() {
		engine.missionDelete(admin, missionName);
	}
//	@Test(timeout = SHORT_TIMEOUT)
//	public void n_nonMemberGetAllMissions() {
//		Assert.fail("Not yet implemented!");
//	}
//

	@Test(timeout = SHORT_TIMEOUT)
	public void o_ownerGetAllMissions2() {
		engine.missionDetailsGet(admin);
	}
//
	@Test(timeout = SHORT_TIMEOUT)
	public void p_memberGetAllMissions2() {
		engine.missionDetailsGet(existingMember);
	}

//	@Test(timeout = SHORT_TIMEOUT)
//	public void q_nonMemberGetAllMissions2() {
//		Assert.fail("Not yet implemented!");
//	}

	@Test(timeout = SHORT_TIMEOUT)
 	public void r_newMemberGetAllMissions2() {
		engine.missionDetailsGet(newMember);
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
