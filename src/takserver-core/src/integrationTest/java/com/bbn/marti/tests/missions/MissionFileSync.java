package com.bbn.marti.tests.missions;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Created on 10/28/15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MissionFileSync extends AbstractConfigurationA {

	@Test(timeout = LONG_TIMEOUT)
	public void a_setupEnvironment() {
		innerSetupEnvironment(MissionFileSync.class);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void b_addFileA() {
		dataAUploadFileHash = engine.fileAdd(admin, "testData.jpg", dataA);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void c_addMission() {
		engine.missionAdd(admin, missionName, groupProfile, null);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void d_addFileToMission() {
		engine.missionAddResource(admin, missionName, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void e_ownerGetMissionFile() {
		engine.missionDetailsGetByName(missionName, admin);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void f_memberGetMissionFile() {
		engine.missionDetailsGetByName(missionName, existingMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void g_nonmemberGetMissionFile() {
		engine.missionDetailsGetByName(missionName, nonMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void h_removeFileFromMission() {
		engine.missionRemoveResource(admin, missionName, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void i_ownerGetRemovedMissionFile() {
		engine.missionDetailsGetByName(missionName, admin);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void j_memberGetRemovedMissionFile() {
		engine.missionDetailsGetByName(missionName, existingMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void k_nonmemberGetRemovedMissionFile() {
		engine.missionDetailsGetByName(missionName, nonMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void l_addFileB() {
		dataBUploadFileHash = engine.fileAdd(admin, "testData2.jpg", dataB);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void m_addFileBToMission() {
		engine.missionAddResource(admin, missionName, dataBUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void n_ownerGetMissionFileB() {
		engine.missionDetailsGetByName(missionName, admin);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void o_memberGetMissionFileB() {
		engine.missionDetailsGetByName(missionName, existingMember);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void p_nonmemberGetMissionFileB() {
		engine.missionDetailsGetByName(missionName, nonMember);
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
