package com.bbn.marti.tests;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import com.bbn.marti.test.shared.data.users.MutableUser;
import com.bbn.marti.takcl.connectivity.AbstractRunnableServer;
import com.bbn.marti.test.shared.engines.TestEngine;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Created on 2/5/16.
 */
public class UserManagementTests extends AbstractTestClass {

    private static final String className = "UserManagementTests";

    private static final MutableServerProfile serverProfile =
            ImmutableServerProfiles.SERVER_0.getMutableInstance();

    @BeforeClass
    public static void setup() {
        try {
            SSLHelper.genCertsIfNecessary();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        if (engine != null) {
            engine.engineFactoryReset();
        }
        AbstractRunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
        TestLogger.setFileLogging(TEST_ARTIFACT_DIRECTORY);
        engine = new TestEngine(serverProfile);
        // Federate things tend to take a little longer to propagate...
        engine.setSleepMultiplier(3.0);

    }

    @Test(timeout = 3600000)
    public void UserManagerTest() {
        try {
            String sessionIdentifier = initTestMethod();

            MutableConnection connection = serverProfile.generateConnection(ImmutableConnections.s0_authstcp);


            MutableUser u0 = connection.generateConnectionUser(BaseUsers.authuser0, true);
            MutableUser u1 = connection.generateConnectionUser(BaseUsers.authuser3, true);
            MutableUser u2 = connection.generateConnectionUser(BaseUsers.authuser01, true);
            MutableUser u3 = connection.generateConnectionUser(BaseUsers.authuser, true);

            engine.offlineAddUsersAndConnectionsIfNecessary(u0);
            engine.offlineAddUsersAndConnectionsIfNecessary(u1);
            engine.offlineAddUsersAndConnectionsIfNecessary(u2);
            engine.offlineAddUsersAndConnectionsIfNecessary(u3);

            engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

            engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

            engine.connectClientsAndVerify(true, u0, u1, u2, u3);

            engine.attemptSendFromUserAndVerify(u0);

            engine.onlineRemoveUsersFromGroup(ImmutableServerProfiles.SERVER_0, GroupProfiles.group0, u2);
            engine.attemptSendFromUserAndVerify(u0);

            engine.onlineAddUsersToGroup(ImmutableServerProfiles.SERVER_0, GroupProfiles.group0, u3);
            engine.attemptSendFromUserAndVerify(u0);

            MutableUser u4 = connection.generateConnectionUser(BaseUsers.authuser2, true);

            engine.onlineAddUser(u4);

            engine.onlineAddUsersToGroup(ImmutableServerProfiles.SERVER_0, GroupProfiles.group0, u4);
            engine.connectClientAndVerify(true, u4);

            engine.onlineAddUsersToGroup(ImmutableServerProfiles.SERVER_0, GroupProfiles.group2, u0, u1);

            engine.attemptSendFromUserAndVerify(u4);
            engine.onlineRemoveUsers(ImmutableServerProfiles.SERVER_0, u0);
            engine.attemptSendFromUserAndVerify(u4);

            engine.onlineUpdateUserPassword(ImmutableServerProfiles.SERVER_0, u4, "newPassword^2aX");
            engine.disconnectClientAndVerify(u4);
            engine.connectClientAndVerify(true, u4);
            engine.attemptSendFromUserAndVerify(u4);
            engine.updateLocalUserPassowrd(u4);
            engine.connectClientAndVerify(true, u4);
            engine.attemptSendFromUserAndVerify(u4);

            engine.onlineUpdateUserPassword(ImmutableServerProfiles.SERVER_0, u3, "l333tb3ans^2aX");
            engine.disconnectClientAndVerify(u3);
            engine.connectClientAndVerify(false, u3);
            engine.authenticateAndVerifyClient(u3);
            engine.attemptSendFromUserAndVerify(u3);

            engine.updateLocalUserPassowrd(u3);
            engine.connectClientAndVerify(false, u3);
            engine.authenticateAndVerifyClient(u3);
            engine.attemptSendFromUserAndVerify(u3);

        } finally {
            engine.stopServers(ImmutableServerProfiles.SERVER_0);
        }
    }

}
