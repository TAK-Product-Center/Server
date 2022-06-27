package com.bbn.marti.tests.missions;

import com.bbn.marti.takcl.AppModules.OnlineFileAuthModule;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.test.shared.AbstractSingleServerTestClass;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.state.StateEngine;
import org.jetbrains.annotations.NotNull;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.MissionUserRole;

public abstract class AbstractMissionRolesConfiguration extends AbstractSingleServerTestClass {

	public void test_mission_1_read_api(@NotNull AbstractUser user, @NotNull String missionName) {
		engine.missionGetChanges(user, missionName);
	}

	public void test_mission_2_write_api(@NotNull AbstractUser user, @NotNull String missionName) {
		String newMissionName = "writeapitest_" + System.currentTimeMillis();
		dataBUploadFileHash = engine.fileAdd(user, newMissionName + "_testData.jpg", dataB);
		engine.missionAdd(admin, newMissionName, groupProfile, null);
		engine.missionSetKeywords(user, missionName, "KeywordA", "KeywordB", "KeywordC");
		engine.missionAddResource(user, newMissionName, dataBUploadFileHash);
		engine.missionClearKeywords(user, missionName);
		engine.missionRemoveResource(user, newMissionName, dataBUploadFileHash);
		engine.missionDelete(user, newMissionName);
	}

	public void test_mission_3_set_role_api(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull AbstractUser targetUser, @NotNull MissionUserRole targetRole) {
		engine.missionSubscribe(apiUser, missionName, targetUser);
		engine.missionSetUserRole(apiUser, missionName, targetUser, targetRole);
	}

	public void test_mission_4_update_groups_api(@NotNull AbstractUser apiUser, @NotNull String missionName) {
		engine.missionAdd(apiUser, missionName, groupProfile, null);
	}

	public void test_mission_5_set_password_api(@NotNull AbstractUser apiUser, @NotNull String missionName) {
		engine.missionSetPassword(apiUser, missionName, "thisIsMyPassword");
	}

	public void test_mission_6_delete_api(@NotNull AbstractUser user, @NotNull String missionName) {
		engine.missionDeepDelete(user, missionName);
	}

	protected ImmutableServerProfiles[] getServers() {
		return new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2};
	}

	private static final long timestamp = System.currentTimeMillis();

	protected static final String adminMissionName = "adminmission_" + timestamp;
	protected static final String ownerMissionName = "ownermission_" + timestamp;
	protected static final String subscriberMissionName = "subscribermission_" + timestamp;
	protected static final String readOnlyMissionName = "readonlymission_" + timestamp;

	protected static final byte[] dataA = Util.generateDummyData(524288);
	protected static final byte[] dataB = Util.generateDummyData(524288);

	protected static final AbstractUser admin = ImmutableUsers.s0_authstcp_authwssuser012_012f;
	protected static final AbstractUser missionOwner = ImmutableUsers.s0_authstcp_authwssuser012_012fA;
	protected static final AbstractUser missionSubscriber = ImmutableUsers.s0_authstcp_authwssuser0_0f;
	protected static final AbstractUser missionReadonlySubscriber = ImmutableUsers.s0_authstcp_authwssuser0_0fA;
	protected static final AbstractUser miscUser = ImmutableUsers.s0_authstcp_authwssuser01_01f;
	protected static final AbstractUser miscUser2 = ImmutableUsers.s0_authstcp_authwssuser12_012fA;
	protected static final GroupSetProfiles groupProfile = GroupSetProfiles.Set_01;

	protected static final OnlineFileAuthModule onfam = new OnlineFileAuthModule();

	protected static String dataAUploadFileHash;
	protected static String dataBUploadFileHash;

	public static void innerSetupEnvironment(@NotNull Class<?> testClass) {
		String classIdentifier = initEnvironment(testClass);

		engine.offlineAddUsersAndConnectionsIfNecessary(admin, missionOwner, missionSubscriber, missionReadonlySubscriber);
		engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);
		engine.startServer(ImmutableServerProfiles.SERVER_0, classIdentifier);

		onfam.init(ImmutableServerProfiles.SERVER_0);
		onfam.certmod(admin.getCertPublicPemPath().toString(), null, null, true, null, admin.getDefinedGroupSet().stringArray(), null, null);
		StateEngine.data.getState(admin).overrideAdminStatus(true);
		onfam.certmod(missionOwner.getCertPublicPemPath().toString(), null, null, false, null, missionOwner.getDefinedGroupSet().stringArray(), null, null);
		onfam.certmod(missionSubscriber.getCertPublicPemPath().toString(), null, null, false, null, missionSubscriber.getDefinedGroupSet().stringArray(), null, null);
		onfam.certmod(missionReadonlySubscriber.getCertPublicPemPath().toString(), null, null, false, null, missionReadonlySubscriber.getDefinedGroupSet().stringArray(), null, null);
		onfam.certmod(miscUser.getCertPublicPemPath().toString(), null, null, false, null, miscUser.getDefinedGroupSet().stringArray(), null, null);

		// TODO Missions: Connection shouldn't be necessary, but is being done for now to minimize potential issues with the rest of the StateEngine
		engine.connectClientsAndVerify(true, admin, missionOwner, missionSubscriber, missionReadonlySubscriber, miscUser);
	}

}
