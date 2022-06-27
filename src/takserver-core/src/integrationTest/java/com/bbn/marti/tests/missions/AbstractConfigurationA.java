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

public abstract class AbstractConfigurationA extends AbstractSingleServerTestClass {

	protected ImmutableServerProfiles[] getServers() {
		return new ImmutableServerProfiles[]{
				ImmutableServerProfiles.SERVER_0,
				ImmutableServerProfiles.SERVER_1,
				ImmutableServerProfiles.SERVER_2
		};
	}

	private static final long timestamp = System.currentTimeMillis();

	protected static final String missionName = "missionfilesynca_" + timestamp;
	protected static final String missionName2 = "missionfilesyncb_" + timestamp;

	protected static final byte[] dataA = Util.generateDummyData(524288);
	protected static final byte[] dataB = Util.generateDummyData(524288);

	protected static final AbstractUser admin = ImmutableUsers.s0_authstcp_authwssuser012_012f;
	protected static final AbstractUser missionOwner = ImmutableUsers.s0_authstcp_authwssuser012_012fA;
	protected static final AbstractUser existingMember = ImmutableUsers.s0_authstcp_authwssuser0_0f;
	protected static final AbstractUser newMember = ImmutableUsers.s0_authstcp_authwssuser2_2f;
	protected static final AbstractUser nonMember = ImmutableUsers.s0_authstcp_authwssusert_t;
	protected static final GroupSetProfiles groupProfile = GroupSetProfiles.Set_012;

	protected static final OnlineFileAuthModule onfam = new OnlineFileAuthModule();

	protected static String dataAUploadFileHash;
	protected static String dataBUploadFileHash;

	public static void innerSetupEnvironment(@NotNull Class<?> testClass) {
		String classIdentifier = initEnvironment(testClass);

		engine.offlineAddUsersAndConnectionsIfNecessary(admin);
		engine.offlineAddUsersAndConnectionsIfNecessary(existingMember);
		engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);
		engine.startServer(ImmutableServerProfiles.SERVER_0, classIdentifier);

		onfam.init(ImmutableServerProfiles.SERVER_0);
		onfam.certmod(admin.getCertPublicPemPath().toString(), null, null, true, null, admin.getDefinedGroupSet().stringArray(), null, null);
		StateEngine.data.getState(admin).overrideAdminStatus(true);
		onfam.certmod(missionOwner.getCertPublicPemPath().toString(), null, null, false, null, missionOwner.getDefinedGroupSet().stringArray(), null, null);
		onfam.certmod(existingMember.getCertPublicPemPath().toString(), null, null, false, null, existingMember.getDefinedGroupSet().stringArray(), null, null);
		onfam.certmod(nonMember.getCertPublicPemPath().toString(), null, null, false, null, nonMember.getDefinedGroupSet().stringArray(), null, null);

		// TODO Missions: Connection shouldn't be necessary, but is being done for now to minimize potential issues with the rest of the StateEngine
		engine.connectClientsAndVerify(false, admin, existingMember);
	}

}
