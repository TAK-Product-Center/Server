package com.bbn.marti.tests;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.TestEngine;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * Created on 11/24/15.
 */
public class AbstractFederationTests extends AbstractTestClass {

	private static final ImmutableServerProfiles[] testServers = new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2};

	private static final String className = "FederationTests";

	@BeforeClass
	public static void setup() {
		try {
			SSLHelper.genCertsIfNecessary();
			if (engine != null) {
				engine.engineFactoryReset();
			}
			AbstractRunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
			com.bbn.marti.takcl.TestLogger.setFileLogging(TEST_ARTIFACT_DIRECTORY);
			engine = new TestEngine(defaultServerProfile);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
		// Federate things tend to take a little longer to propagate...
		engine.setSleepMultiplier(3.0);
		engine.setSendValidationDelayMultiplier(10);
	}

	public void executeBasicFederationTest(boolean useV1Federation, boolean useV2Federation, String sessionIdentifier) {
		try {
			engine.offlineFederateServers(useV1Federation, useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);

			engine.offlineAddOutboundFederateConnection(useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);

			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0);

			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0, "group0");

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp0_anonuser_0f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s1_stcp0_anonuser_0f);

			engine.startServer(ImmutableServerProfiles.SERVER_1, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			// Inserting sleep since the servers need some time to federate
			Thread.sleep(30000);

			engine.connectClientAndVerify(true, ImmutableUsers.s0_stcp0_anonuser_0f);
			engine.connectClientAndVerify(true, ImmutableUsers.s1_stcp0_anonuser_0f);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp0_anonuser_0f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s1_stcp0_anonuser_0f);

		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(testServers);
		}
	}

	public void executeBasicMultiInputFederationTest(boolean useV1Federation, boolean useV2Federation, String sessionIdentifier) {
		try {
			engine.offlineFederateServers(useV1Federation, useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2);

			engine.offlineAddOutboundFederateConnection(useV2Federation, ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_0);

			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_0);

			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_0, "group0");

			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2, "group0");

			ImmutableUsers[] users = new ImmutableUsers[]{
					ImmutableUsers.s0_authstcp_authuser01_01f,
					ImmutableUsers.s2_authstcp_authuser01_01f,
					ImmutableUsers.s0_ssl_anonuser_t,
					ImmutableUsers.s2_ssl_anonuser_t,
					ImmutableUsers.s0_stcp0_anonuser_0f,
					ImmutableUsers.s2_stcp0_anonuser_0f,
					ImmutableUsers.s0_stcp12_anonuser_12f,
					ImmutableUsers.s2_stcp12_anonuser_12f,
					ImmutableUsers.s0_udp12t_anonuser_12t,
					ImmutableUsers.s2_udp12t_anonuser_12t
			};

			for (ImmutableUsers user : users) {
				engine.offlineAddUsersAndConnectionsIfNecessary(user);
			}

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_2, sessionIdentifier);

			// Inserting sleep since the servers need some time to federate
			Thread.sleep(30000);

			for (ImmutableUsers user : users) {
				if (user.getConnection().getProtocol().canConnect()) {
					engine.connectClientAndVerify(true, user);
				}
			}

			for (ImmutableUsers user : users) {
				engine.attemptSendFromUserAndVerify(user);
			}


		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(testServers);
		}
	}

	public void executeAdvancedFederationTest(boolean useV1Federation, boolean useV2Federation, String sessionIdentifier) {
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
					ImmutableUsers.s1_authstcp_authuser01_01f,
					ImmutableUsers.s2_authstcp_authuser01_01f,
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

			// Inserting sleep since the servers need some time to federate
			Thread.sleep(30000);

			for (ImmutableUsers user : users) {
				if (user.getConnection().getProtocol().canConnect()) {
					engine.connectClientAndVerify(true, user);
				}
			}

			for (ImmutableUsers user : users) {
				engine.attemptSendFromUserAndVerify(user);
			}

		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(testServers);
		}
	}

	// TODO: Add test where LatestSA is sent prior to federate connection
	public void executeFederateConnectionInitiatorWaitTest(boolean useV1Federation, boolean useV2Federation, String sessionIdentifier) {
		try {
			engine.offlineFederateServers(useV1Federation, useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2);

			engine.offlineAddOutboundFederateConnection(useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
			engine.offlineAddOutboundFederateConnection(useV2Federation, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2);

			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_0);

			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_0, "group0");

			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2, "group0");

			ImmutableUsers[] users = new ImmutableUsers[]{
					ImmutableUsers.s0_authstcp_authuser01_01f,
					ImmutableUsers.s1_authstcp_authuser01_01f,
					ImmutableUsers.s0_ssl_anonuser_t,
					ImmutableUsers.s1_ssl_anonuser_t,
					ImmutableUsers.s0_stcp0_anonuser_0f,
					ImmutableUsers.s1_stcp0_anonuser_0f,
					ImmutableUsers.s0_stcp12_anonuser_12f,
					ImmutableUsers.s1_stcp12_anonuser_12f,
					ImmutableUsers.s0_udp12t_anonuser_12t,
					ImmutableUsers.s1_udp12t_anonuser_12t
			};

			for (ImmutableUsers user : users) {
				engine.offlineAddUsersAndConnectionsIfNecessary(user);
			}

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_1, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_2, sessionIdentifier);

			// Inserting sleep since the servers need some time to federate
			Thread.sleep(30000);

			for (ImmutableUsers user : users) {
				if (user.getConnection().getProtocol().canConnect()) {
					engine.connectClientAndVerify(true, user);
				}
			}

			for (ImmutableUsers user : users) {
				engine.attemptSendFromUserAndVerify(user);
			}

		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(testServers);
		}
	}

//    @Test
//    public void advancedFederationPointToPointTest() {
//        try {
//            String sessionIdentifier = className + ".advancedFederationTest";
//            engine.engineFactoryReset();
//
//            engine.offlineFederateServers(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
//
//            engine.offlineAddOutboundFederateConnection(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
//
//            engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
//            engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0);
//
//            engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, "group0");
//            engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0, "group1");
//
//            engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_0, "group0");
//            engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, "group1");
//
//            ImmutableUsers[] users = new ImmutableUsers[]{
//                    ImmutableUsers.s0_authstcp_authuser01_01f,
//                    ImmutableUsers.s1_authstcp_authuser01_01f,
////                    ImmutableUsers.s0_ssl_anonuser_t,
////                    ImmutableUsers.s1_ssl_anonuser_t,
//                    ImmutableUsers.s0_stcp0_anonuser_0f,
//                    ImmutableUsers.s1_stcp0_anonuser_0f,
//                    ImmutableUsers.s0_stcp12_anonuser_12f,
//                    ImmutableUsers.s1_stcp12_anonuser_12f,
////                    ImmutableUsers.s0_udp12t_anonuser_12t,
////                    ImmutableUsers.s1_udp12t_anonuser_12t,
//            };
//
//            for (ImmutableUsers user : users) {
//                engine.offlineAddUsersAndConnectionsIfNecessary(user);
//            }
//
//            engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
//            engine.startServer(ImmutableServerProfiles.SERVER_1, sessionIdentifier);
//
//            for (ImmutableUsers user : users) {
//                if (user.getConnection().getProtocol().canConnect()) {
//                    engine.connectClientAndVerify(true, user);
//                }
//            }
//
//            for (ImmutableUsers user : users) {
//                engine.attemptSendFromUserAndVerify(user);
//            }
//
//            PointToPointTests.UserMixer mixer = new PointToPointTests.UserMixer();
//            for (ImmutableUsers user : users) {
//                mixer.addUserList(user);
//            }
//
//            List<AbstractUser[]> userSets = mixer.produceUserSets();
//
//            for (ImmutableUsers user : users) {
//                for (AbstractUser[] userSet : userSets) {
//                    engine.attemptSendFromUserAndVerify(user, userSet);
//                }
//            }
//
//        } finally {
//            engine.stopServers(testServers);
//        }
//    }
}
