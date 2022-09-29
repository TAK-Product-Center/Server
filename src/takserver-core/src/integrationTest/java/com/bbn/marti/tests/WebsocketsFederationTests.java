package com.bbn.marti.tests;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TestConfiguration;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.TestEngine;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Created on 11/24/15.
 */
public class WebsocketsFederationTests extends AbstractTestClass {

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

	@Test(timeout = 10000000)
	public void advancedWebsocketsFederationV1Test() {
		String sessionIdentifier = initTestMethod();
		executeAdvancedWebsocketsFederationTest(sessionIdentifier, true, false);
	}

	@Test(timeout = 10000000)
	public void advancedWebsocketsFederationV2Test() {
		String sessionIdentifier = initTestMethod();
		executeAdvancedWebsocketsFederationTest(sessionIdentifier, false, true);
	}

	public void executeAdvancedWebsocketsFederationTest(@NotNull String sessionIdentifier, boolean useV1Federation, boolean useV2Federation) {
		try {
			engine.offlineFederateServers(useV1Federation, useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2);

			engine.offlineAddOutboundFederateConnection(useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
			engine.offlineAddOutboundFederateConnection(useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2);
			engine.offlineAddOutboundFederateConnection(useV2Federation, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2);

			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_1);

			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0, "group1");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2, "group2");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_0, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_1, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_1, "group2");

			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, "group1");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_1, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_1, "group2");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2, "group2");

			ImmutableUsers[] users = new ImmutableUsers[]{
					ImmutableUsers.s0_authstcp_authuser01_01f,
					ImmutableUsers.s0_authstcp_authwssuser01_01f,
					ImmutableUsers.s1_authstcp_authuser01_01f,
					ImmutableUsers.s1_authstcp_authwssuser0_0f,
					ImmutableUsers.s2_authstcp_authuser01_01f,
					ImmutableUsers.s2_authstcp_authwssuser12_012f,
					ImmutableUsers.s0_ssl_anonuser_t,
					ImmutableUsers.s1_ssl_anonuser_t,
					ImmutableUsers.s2_ssl_anonuser_t,
					ImmutableUsers.s0_stcp0_anonuser_0f,
					ImmutableUsers.s1_stcp0_anonuser_0f,
					ImmutableUsers.s2_stcp0_anonuser_0f,
					ImmutableUsers.s0_stcp12_anonuser_12f,
					ImmutableUsers.s1_stcp12_anonuser_12f,
					ImmutableUsers.s2_stcp12_anonuser_12f,
					ImmutableUsers.s0_udp12t_anonuser_12t,
					ImmutableUsers.s1_udp12t_anonuser_12t,
					ImmutableUsers.s2_udp12t_anonuser_12t
			};

			for (ImmutableUsers user : users) {
				engine.offlineAddUsersAndConnectionsIfNecessary(user);
			}

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_2, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_1, sessionIdentifier);

			for (ImmutableUsers user : users) {
				if (user.getConnection().getProtocol().canConnect()) {
					engine.connectClientAndVerify(true, user);
				}
			}

			for (ImmutableUsers user : users) {
				engine.attemptSendFromUserAndVerify(user);
			}

		} finally {
			engine.stopServers(testServers);
		}
	}

}
