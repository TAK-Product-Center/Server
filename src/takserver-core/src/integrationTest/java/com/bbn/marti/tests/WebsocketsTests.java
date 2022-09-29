package com.bbn.marti.tests;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TestConfiguration;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.TestEngine;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Created on 11/24/15.
 */
public class WebsocketsTests extends AbstractTestClass {

	private static final ImmutableServerProfiles[] testServers = new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2};

	@BeforeClass
	public static void setup() {
		org.junit.Assume.assumeTrue("DB Available", TestConfiguration.getInstance().dbAvailable);
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
		engine = new TestEngine(testServers);
		// Federate things tend to take a little longer to propagate...
		engine.setSleepMultiplier(3.0);
		engine.setSendValidationDelayMultiplier(16);

	}

	@Test(timeout = 1200000)
	public void simpleWSS() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authwssuser01_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authwssuser012_012f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authwssuser_f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authwssusert_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01_anonuser_01f);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authwssuser01_01f);  // Good
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authwssuser012_012f);  // Good
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser012_012f); // Bad?
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authwssuser01_01f); // Good
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authwssuser01_01f); // Good
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authwssuser01_01f); // Good

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 1200000)
	public void basicSecureWebsocketTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authssl_authuser01_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authwssuser01_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authwssuser012_012f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01t_anonuser_01t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser12_012f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser0_0f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser_f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser2_2f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_udp12t_anonuser_12t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_tcp01t_anonuser_01t);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientAndSendMessage(true, ImmutableUsers.s0_authssl_authuser01_01f); // Good
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authwssuser01_01f);  // Good
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser012_012f); // Bad?
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser_f); // Bad?
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.connectClientAndVerify(false, ImmutableUsers.s0_authstcp_authuser0_0f); // Bad?
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authwssuser01_01f); // Good
			engine.authenticateAndVerifyClient(ImmutableUsers.s0_authstcp_authuser0_0f);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authwssuser012_012f); // Bad?
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser2_2f); // Bad?
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authwssuser01_01f); // Good
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authwssuser01_01f); // Good
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authwssuser012_012f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_udp12t_anonuser_12t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_tcp01t_anonuser_01t);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}
}
