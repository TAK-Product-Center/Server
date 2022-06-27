package com.bbn.marti.tests.missions;

import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.MissionUserRole;

/**
 * Created on 10/28/15.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MissionUserDefaultRolesTests extends AbstractMissionRolesConfiguration {
	public void missionSetup(@NotNull AbstractUser adminUser, @NotNull AbstractUser apiUser,
	                         @NotNull AbstractUser miscUser, @NotNull String missionName, @Nullable MissionUserRole role) {
		engine.missionAdd(adminUser, missionName, groupProfile, role);
		engine.missionSubscribe(apiUser, missionName, apiUser);
		engine.missionSubscribe(miscUser, missionName, miscUser);
	}

	@Test(timeout = LONG_TIMEOUT)
	public void aaa_setupEnvironment() {
		innerSetupEnvironment(MissionUserDefaultRolesTests.class);
	}

	// Admin tests
	@Test(timeout = LONG_TIMEOUT)
	public void admin_a_missionSetup() {
		missionSetup(admin, admin, miscUser, adminMissionName, null);
	}


	@Test(timeout = LONG_TIMEOUT)
	public void admin_b_populateMissionData() {
		engine.attemptSendFromUserAndVerify(admin, adminMissionName);
		dataAUploadFileHash = engine.fileAdd(admin, "testData.jpg", dataA);
		engine.missionAddResource(admin, adminMissionName, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void admin_c_readApiTest() {
		test_mission_1_read_api(admin, adminMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void admin_d_writeApiTest() {
		test_mission_2_write_api(admin, adminMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void admin_e_setRoleApiTest() {
		test_mission_3_set_role_api(admin, adminMissionName, miscUser, MissionUserRole.MISSION_SUBSCRIBER);
	}

//	Not currently running since it is not fully implemented and results in some less predictable behavior
//	@Test(timeout = SHORT_TIMEOUT)
//	public void admin_f_updateGroupsApiTest() {
//		test_mission_4_update_groups_api(admin, adminMissionName);
//	}

	@Test(timeout = SHORT_TIMEOUT)
	public void admin_g_setPasswordApiTest() {
		test_mission_5_set_password_api(admin, adminMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void admin_h_deleteApiTest() {
		test_mission_6_delete_api(admin, adminMissionName);
	}


	// MISSION_OWNER tests
	@Test(timeout = LONG_TIMEOUT)
	public void owner_a_missionSetup() {
		missionSetup(admin, missionOwner, miscUser, ownerMissionName, MissionUserRole.MISSION_OWNER);
	}

	@Test(timeout = LONG_TIMEOUT)
	public void owner_b_populateMissionData() {
		engine.attemptSendFromUserAndVerify(admin, ownerMissionName);
		dataAUploadFileHash = engine.fileAdd(admin, "testData.jpg", dataA);
		engine.missionAddResource(admin, ownerMissionName, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void owner_c_readApiTest() {
		test_mission_1_read_api(missionOwner, ownerMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void owner_d_writeApiTest() {
		test_mission_2_write_api(missionOwner, ownerMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void owner_e_setRoleApiTest() {
		test_mission_3_set_role_api(missionOwner, ownerMissionName, miscUser, MissionUserRole.MISSION_OWNER);
	}

//	Not currently running since it is not fully implemented and results in some less predictable behavior
//	@Test(timeout = SHORT_TIMEOUT)
//	public void owner_f_updateGroupsApiTest() {
//		test_mission_4_update_groups_api(missionOwner, ownerMissionName);
//	}
	@Test(timeout = SHORT_TIMEOUT)
	public void owner_g_setPasswordApiTest() {
		test_mission_5_set_password_api(missionOwner, ownerMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void owner_h_deleteApiTest() {
		test_mission_6_delete_api(missionOwner, ownerMissionName);
	}


	// MISSION_SUBSCRIBER tests
	@Test(timeout = LONG_TIMEOUT)
	public void subscriber_a_missionSetup() {
		missionSetup(admin, missionSubscriber, miscUser, subscriberMissionName, MissionUserRole.MISSION_SUBSCRIBER);
	}

	@Test(timeout = LONG_TIMEOUT)
	public void subscriber_b_populateMissionData() {
		engine.attemptSendFromUserAndVerify(admin, subscriberMissionName);
		dataAUploadFileHash = engine.fileAdd(admin, "testData.jpg", dataA);
		engine.missionAddResource(admin, subscriberMissionName, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void subscriber_c_readApiTest() {
		test_mission_1_read_api(missionSubscriber, subscriberMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void subscriber_d_writeApiTest() {
		test_mission_2_write_api(missionSubscriber, subscriberMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void subscriber_e_setRoleApiTest() {
		test_mission_3_set_role_api(missionSubscriber, subscriberMissionName, miscUser, MissionUserRole.MISSION_OWNER);
	}

//	Not currently running since it is not fully implemented and results in some less predictable behavior
//	@Test(timeout = SHORT_TIMEOUT)
//	public void subscriber_f_updateGroupsApiTest() {
//		test_mission_4_update_groups_api(missionSubscriber, subscriberMissionName);
//	}

	@Test(timeout = SHORT_TIMEOUT)
	public void subscriber_g_setPasswordApiTest() {
		test_mission_5_set_password_api(missionSubscriber, subscriberMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void subscriber_h_deleteApiTest() {
		test_mission_6_delete_api(missionSubscriber, subscriberMissionName);
	}


	// MISSION_READONLY_SUBSCRIBER tests
	@Test(timeout = LONG_TIMEOUT)
	public void readonlySubscriber_a_missionSetup() {
		missionSetup(admin, missionReadonlySubscriber, miscUser, readOnlyMissionName, MissionUserRole.MISSION_READONLY_SUBSCRIBER);
	}

	@Test(timeout = LONG_TIMEOUT)
	public void readonlySubscriber_b_populateMissionData() {
		engine.attemptSendFromUserAndVerify(admin, readOnlyMissionName);
		dataAUploadFileHash = engine.fileAdd(admin, "testData.jpg", dataA);
		engine.missionAddResource(admin, readOnlyMissionName, dataAUploadFileHash);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void readonlySubscriber_c_readApiTest() {
		test_mission_1_read_api(missionReadonlySubscriber, readOnlyMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void readonlySubscriber_d_writeApiTest() {
		test_mission_2_write_api(missionReadonlySubscriber, readOnlyMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void readonlySubscriber_e_setRoleApiTest() {
		test_mission_3_set_role_api(missionReadonlySubscriber, readOnlyMissionName, miscUser, MissionUserRole.MISSION_OWNER);
	}

//	Not currently running since it is not fully implemented and results in some less predictable behavior
//	@Test(timeout = SHORT_TIMEOUT)
//	public void readonlySubscriber_f_updateGroupsApiTest() {
//		test_mission_4_update_groups_api(missionReadonlySubscriber, readOnlyMissionName);
//	}

	@Test(timeout = SHORT_TIMEOUT)
	public void readonlySubscriber_g_setPasswordApiTest() {
		test_mission_5_set_password_api(missionReadonlySubscriber, readOnlyMissionName);
	}

	@Test(timeout = SHORT_TIMEOUT)
	public void readonlySubscriber_h_deleteApiTest() {
		test_mission_6_delete_api(missionReadonlySubscriber, readOnlyMissionName);
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
