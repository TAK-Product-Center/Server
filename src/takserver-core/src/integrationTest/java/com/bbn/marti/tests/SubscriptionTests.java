package com.bbn.marti.tests;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.connectivity.AbstractRunnableServer;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.TestEngine;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Created on 11/17/15.
 */
public class SubscriptionTests extends AbstractTestClass {

	private static final ImmutableServerProfiles[] testServers = new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1};

	private static final String className = "SubscriptionTest";

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
		engine = new TestEngine(testServers);
		engine.setSleepMultiplier(3.0);
	}

//	@Test(timeout = 3600000)
	public void crossServerSubscriptions() {
		try {
			String sessionIdentifier = initTestMethod();

			ImmutableConnections[] subscriptionArray = new ImmutableConnections[]{
					ImmutableConnections.s0_mcast,
					ImmutableConnections.s0_tcp,
					ImmutableConnections.s0_udp,
					ImmutableConnections.s0_stcp
			};

			for (ImmutableConnections connection : subscriptionArray) {
				engine.engineFactoryReset();


				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_mcast_anonuser_t);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authssl_authuser_f);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_tcp01t_anonuser_01t);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01_anonuser_01f);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);

				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s1_stcp_anonuser_t_A);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s1_mcast_anonuser_t);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s1_authssl_authuser_f);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s1_tcp01t_anonuser_01t);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s1_stcp01_anonuser_01f);
				engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s1_stcp_anonuser_t_B);

				engine.offlineAddSubscriptionFromInputToServer(connection, ImmutableServerProfiles.SERVER_1);

				for (ImmutableServerProfiles server : testServers) {
					engine.startServer(server, sessionIdentifier);
				}

				engine.connectClientsAndVerify(true,
						ImmutableUsers.s0_stcp_anonuser_t_A,
						ImmutableUsers.s0_authssl_authuser_f,
						ImmutableUsers.s0_stcp01_anonuser_01f,
						ImmutableUsers.s1_authssl_authuser_f,
						ImmutableUsers.s1_stcp01_anonuser_01f,
						ImmutableUsers.s0_stcp_anonuser_t_B,
						ImmutableUsers.s1_stcp_anonuser_t_A);


				engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s1_authssl_authuser_f);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp01_anonuser_01f);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authssl_authuser_f);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s1_stcp01_anonuser_01f);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s1_tcp01t_anonuser_01t);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_tcp01t_anonuser_01t);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s1_mcast_anonuser_t);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s1_stcp_anonuser_t_A);
				engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_mcast_anonuser_t);

				engine.stopServers(testServers);
			}

		} finally {
			engine.stopServers(testServers);
		}
	}

	@Test(timeout = 680000)
	public void clientSubscriptions() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_udp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_submcast_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_subudp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_subtcp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_substcp_anonuser_t);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_A, ImmutableUsers.s0_stcp_anonuser_t_B);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_udp_anonuser_t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);

		} finally {
			engine.stopServers(testServers);
		}
	}
}
