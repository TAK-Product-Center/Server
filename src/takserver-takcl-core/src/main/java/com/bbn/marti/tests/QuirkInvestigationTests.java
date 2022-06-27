package com.bbn.marti.tests;

import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.connectivity.AbstractRunnableServer;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import com.bbn.marti.test.shared.data.users.MutableUser;
import com.bbn.marti.test.shared.engines.TestEngine;
import com.bbn.marti.test.shared.engines.UserIdentificationData;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created on 11/24/15.
 */
public class QuirkInvestigationTests extends AbstractTestClass {

	private static final MutableServerProfile server0 = ImmutableServerProfiles.SERVER_0.getMutableInstance();
	private static final MutableServerProfile server1 = ImmutableServerProfiles.SERVER_1.getMutableInstance();

	private static final MutableServerProfile[] testServers = new MutableServerProfile[]{server0, server1};

//    private static final ImmutableServerProfiles[] testServers = new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.SERVER_3};

	private static final String className = "FederationTests";

	@BeforeClass
	public static void setup() {
		if (engine != null) {
			engine.engineFactoryReset();
		}
		AbstractRunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
		TestLogger.setFileLogging(TEST_ARTIFACT_DIRECTORY);
		engine = new TestEngine(testServers);
		// Federate things tend to take a little longer to propagate...
		engine.setSleepMultiplier(1.4);

	}

	@Test
	public void localPointToPointQuirksTest() {
		try {
			String sessionIdentifier = initTestMethod();

			MutableConnection c0 = server0.generateConnection(ImmutableConnections.s0_stcp0);
			MutableConnection c1 = server1.generateConnection(ImmutableConnections.s1_stcp01);

			MutableUser u0A = c0.generateConnectionUser(BaseUsers.anonuser, true, "A").overrideCotUid("UID_A").overrideCotCallsign("CS_A");
			MutableUser u0B = c0.generateConnectionUser(BaseUsers.anonuser, true, "B").overrideCotUid("UID_0_B").overrideCotCallsign("CS_B");
			MutableUser u0C = c0.generateConnectionUser(BaseUsers.anonuser, true, "C").overrideCotUid("UID_C").overrideCotCallsign("CS_0_C");
			MutableUser u0D = c0.generateConnectionUser(BaseUsers.anonuser, true, "D").overrideCotUid("UID_D").overrideCotCallsign("CS_D");
			MutableUser u0E = c0.generateConnectionUser(BaseUsers.anonuser, true, "E").overrideCotUid("UID_0_E").overrideCotCallsign("CS_0_E");
			MutableUser[] s0Users = new MutableUser[]{u0A, u0B, u0C, u0D, u0E};
			MutableUser u1A = c0.generateConnectionUser(BaseUsers.anonuser, true, "F").overrideCotUid("UID_A").overrideCotCallsign("CS_E");
			MutableUser u1B = c0.generateConnectionUser(BaseUsers.anonuser, true, "G").overrideCotUid("UID_1_B").overrideCotCallsign("CS_D");
			MutableUser u1C = c0.generateConnectionUser(BaseUsers.anonuser, true, "H").overrideCotUid("UID_C").overrideCotCallsign("CS_1_C");
			MutableUser u1D = c0.generateConnectionUser(BaseUsers.anonuser, true, "I").overrideCotUid("UID_D").overrideCotCallsign("CS_B");
			MutableUser u1E = c0.generateConnectionUser(BaseUsers.anonuser, true, "J").overrideCotUid("UID_1_E").overrideCotCallsign("CS_1_A");

			MutableUser[] s1Users = new MutableUser[]{u1A, u1B, u1C, u1D, u1E};


			sessionIdentifier = initTestMethod();


			engine.offlineFederateServers(true, true, server0, server1);
			engine.offlineAddOutboundFederateConnection(false, server0, server1);
			engine.offlineAddFederate(server0, server1);
			engine.offlineAddFederate(server1, server0);
			engine.offlineAddOutboundFederateGroup(server0, server1, "group0");
			engine.offlineAddInboundFederateGroup(server1, server0, "group0");
			engine.offlineAddOutboundFederateGroup(server1, server0, "group0");
			engine.offlineAddInboundFederateGroup(server0, server1, "group0");
			engine.offlineAddUsersAndConnectionsIfNecessary(s0Users);
			engine.offlineAddUsersAndConnectionsIfNecessary(s1Users);

			engine.startServer(server0, sessionIdentifier);
			engine.startServer(server1, sessionIdentifier);

			boolean sw = false;
			for (int i = 0; i < s0Users.length; i++) {
				if (sw) {
					engine.connectClientAndVerify(true, s0Users[i]);
					engine.connectClientAndVerify(true, s1Users[i]);
					sw = !sw;
				} else {
					engine.connectClientAndVerify(true, s1Users[i]);
					engine.connectClientAndVerify(true, s0Users[i]);
					sw = !sw;
				}
			}

			for (MutableUser user : s0Users) {
				engine.attemptSendFromUserAndVerify(user);
			}
			for (MutableUser user : s1Users) {
				engine.attemptSendFromUserAndVerify(user);
			}


			for (MutableUser sender : s0Users) {
				for (MutableUser receiver : s0Users) {
					engine.attemptSendFromUserAndVerify(UserIdentificationData.UID_AND_CALLSIGN, sender, UserIdentificationData.UID_AND_CALLSIGN, receiver);
				}
				for (MutableUser receiver : s1Users) {
					engine.attemptSendFromUserAndVerify(UserIdentificationData.UID_AND_CALLSIGN, sender, UserIdentificationData.UID_AND_CALLSIGN, receiver);
				}
			}

		} finally {
			engine.stopServers(testServers);
		}
	}

	@Test
	public void federatePointToPointQuirksTest() {
		try {
			String sessionIdentifier = initTestMethod();

			MutableConnection c0 = server0.generateConnection(ImmutableConnections.s0_stcp0);
			MutableConnection c1 = server1.generateConnection(ImmutableConnections.s1_stcp01);

			MutableUser u0A = c0.generateConnectionUser(BaseUsers.anonuser, true, "A").overrideCotUid("UID_A").overrideCotCallsign("CS_A");
			MutableUser u0B = c0.generateConnectionUser(BaseUsers.anonuser, true, "B").overrideCotUid("UID_0_B").overrideCotCallsign("CS_B");
			MutableUser u0C = c0.generateConnectionUser(BaseUsers.anonuser, true, "C").overrideCotUid("UID_C").overrideCotCallsign("CS_0_C");
			MutableUser u0D = c0.generateConnectionUser(BaseUsers.anonuser, true, "D").overrideCotUid("UID_D").overrideCotCallsign("CS_D");
			MutableUser u0E = c0.generateConnectionUser(BaseUsers.anonuser, true, "E").overrideCotUid("UID_0_E").overrideCotCallsign("CS_0_E");
			MutableUser[] s0Users = new MutableUser[]{u0A, u0B, u0C, u0D, u0E};

			MutableUser u1A = c1.generateConnectionUser(BaseUsers.anonuser, true, "A").overrideCotUid("UID_A").overrideCotCallsign("CS_A");
			MutableUser u1B = c1.generateConnectionUser(BaseUsers.anonuser, true, "B").overrideCotUid("UID_1_B").overrideCotCallsign("CS_B");
			MutableUser u1C = c1.generateConnectionUser(BaseUsers.anonuser, true, "C").overrideCotUid("UID_C").overrideCotCallsign("CS_1_C");
			MutableUser u1D = c1.generateConnectionUser(BaseUsers.anonuser, true, "D").overrideCotUid("UID_D").overrideCotCallsign("CS_D");
			MutableUser u1E = c1.generateConnectionUser(BaseUsers.anonuser, true, "E").overrideCotUid("UID_1_E").overrideCotCallsign("CS_1_E");

			MutableUser[] s1Users = new MutableUser[]{u1A, u1B, u1C, u1D, u1E};

			sessionIdentifier = initTestMethod();

			engine.offlineFederateServers(true, true, server0, server1);
			engine.offlineAddOutboundFederateConnection(false, server0, server1);
			engine.offlineAddFederate(server0, server1);
			engine.offlineAddFederate(server1, server0);
			engine.offlineAddOutboundFederateGroup(server0, server1, "group0");
			engine.offlineAddInboundFederateGroup(server1, server0, "group0");
			engine.offlineAddOutboundFederateGroup(server1, server0, "group0");
			engine.offlineAddInboundFederateGroup(server0, server1, "group0");
			engine.offlineAddUsersAndConnectionsIfNecessary(s0Users);
			engine.offlineAddUsersAndConnectionsIfNecessary(s1Users);

			engine.startServer(server0, sessionIdentifier);
			engine.startServer(server1, sessionIdentifier);

			for (MutableUser user : s0Users) {
				engine.connectClientAndVerify(true, user);
			}
			for (MutableUser user : s1Users) {
				engine.connectClientAndVerify(true, user);
			}

			for (MutableUser user : s0Users) {
				engine.attemptSendFromUserAndVerify(user);
			}
			for (MutableUser user : s1Users) {
				engine.attemptSendFromUserAndVerify(user);
			}


			for (MutableUser sender : s0Users) {
				for (MutableUser receiver : s0Users) {
					engine.attemptSendFromUserAndVerify(sender, receiver);
				}

			}

		} finally {
			engine.stopServers(testServers);
		}
	}
}
