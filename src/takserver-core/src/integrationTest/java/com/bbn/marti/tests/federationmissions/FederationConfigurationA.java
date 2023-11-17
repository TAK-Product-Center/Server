package com.bbn.marti.tests.federationmissions;

import com.bbn.marti.takcl.AppModules.OnlineFileAuthModule;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TestConfiguration;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.test.shared.AbstractSingleServerTestClass;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.TestEngine;
import com.bbn.marti.test.shared.engines.state.StateEngine;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

public abstract class FederationConfigurationA extends AbstractSingleServerTestClass {

    private static final ImmutableServerProfiles[] testServers = new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1};

    @Override
    protected ImmutableServerProfiles[] getServers() {
        return testServers;
    }
    private static final long timestamp = System.currentTimeMillis();

    protected static final String missionName = "missionfilesynca_" + timestamp;
    protected static final String missionName2 = "missionfilesyncb_" + timestamp;

    protected static final byte[] dataA = Util.generateDummyData(524288);
    protected static final byte[] dataB = Util.generateDummyData(524288);

    protected static final AbstractUser admin = ImmutableUsers.s0_authstcp_authwssuser012_012f;
    protected static final AbstractUser missionOwner = ImmutableUsers.s1_authstcp_authwssuser012_012fA;
    protected static final AbstractUser existingMember = ImmutableUsers.s1_authstcp_authwssuser0_0f;
    protected static final AbstractUser newMember = ImmutableUsers.s1_authstcp_authwssuser2_2f;
    protected static final AbstractUser nonMember = ImmutableUsers.s1_authstcp_authwssusert_t;
    protected static final GroupSetProfiles groupProfile = GroupSetProfiles.Set_012;

    protected static final OnlineFileAuthModule onfam0 = new OnlineFileAuthModule();
    protected static final OnlineFileAuthModule onfam1 = new OnlineFileAuthModule();

    protected static String dataAUploadFileHash;
    protected static String dataBUploadFileHash;

    public static void innerSetupEnvironment(@NotNull Class<?> testClass) {
        try {
            SSLHelper.genCertsIfNecessary();
            if (engine != null) {
                engine.engineFactoryReset();
            }
            AbstractRunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
            TestLogger.setFileLogging(TEST_ARTIFACT_DIRECTORY);
            engine = new TestEngine(defaultServerProfile);

            // Federate things tend to take a little longer to propagate...
            engine.setSleepMultiplier(4.0);
            engine.setSendValidationDelayMultiplier(20);

            String classIdentifier = TestConfiguration.getInstance().toFormalTaggedName(testClass);
            LoggerFactory.getLogger(testClass).info(String.format(LOG_START_FORMATTER, classIdentifier));
            TestLogger.startTestWithIdentifier(classIdentifier);

            engine.offlineFederateServers(false, true, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
            engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
            engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
            engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0);
            engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, "group0");
            engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0, "group0");

            engine.offlineAddUsersAndConnectionsIfNecessary(admin);
            engine.offlineAddUsersAndConnectionsIfNecessary(existingMember);
            engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);
            engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_1);
            engine.startServer(ImmutableServerProfiles.SERVER_1, classIdentifier);
            engine.startServer(ImmutableServerProfiles.SERVER_0, classIdentifier);

            // Inserting sleep since the servers need some time to federate
            Thread.sleep(60000);

            onfam0.init(ImmutableServerProfiles.SERVER_0);
            onfam1.init(ImmutableServerProfiles.SERVER_1);

            onfam0.certmod(admin.getCertPublicPemPath().toString(), null, null, true, null, admin.getDefinedGroupSet().stringArray(), null, null);
            StateEngine.data.getState(admin).overrideAdminStatus(true);
            onfam1.certmod(missionOwner.getCertPublicPemPath().toString(), null, null, false, null, missionOwner.getDefinedGroupSet().stringArray(), null, null);
            onfam1.certmod(existingMember.getCertPublicPemPath().toString(), null, null, false, null, existingMember.getDefinedGroupSet().stringArray(), null, null);
            onfam1.certmod(nonMember.getCertPublicPemPath().toString(), null, null, false, null, nonMember.getDefinedGroupSet().stringArray(), null, null);

            engine.connectClientAndVerify(true, admin);
            engine.connectClientAndVerify(true, existingMember);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }
}
