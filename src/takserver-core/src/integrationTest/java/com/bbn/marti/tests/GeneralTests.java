package com.bbn.marti.tests;

import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created on 10/28/15.
 */
public class GeneralTests extends AbstractTestClass {

	private static final String className = "GeneralTests";

	//	@Test(timeout = 300000)
	public void simpleLatestSADisconnectTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_A, ImmutableUsers.s0_stcp_anonuser_t_B);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);

			engine.disconnectClientAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	//		@Test(timeout = 300000)
	public void simpleSsl() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authssl_authuser01_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser012_012f);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true,
					ImmutableUsers.s0_authssl_authuser01_01f,
					ImmutableUsers.s0_authstcp_authuser012_012f);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authssl_authuser01_01f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authuser012_012f);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	//	@Test(timeout = 300000)
	public void simpleFileAuth() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser12_012f);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientAndSendMessage(true, ImmutableUsers.s0_authstcp_authuser12_012f);

			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser12_012f);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authuser012_012f);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

// This tests nothing.... It likely needs updating
//	@Test(timeout = 300000)
//	public void authUserWithNoRecipientGroup() {
//		try {
//			String sessionIdentifier = initTestMethod();
//
//			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authsslA_authuser_f);
//			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);
//
//			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
//
//			engine.connectClientAndVerify(true, ImmutableUsers.s0_authsslA_authuser_f);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail(e.getMessage());
//		} finally {
//			engine.stopServers(ImmutableServerProfiles.SERVER_0);
//		}
//	}

	@Test(timeout = 800000)
	public void groupToNonGroup() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01_anonuser_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);
			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp01_anonuser_01f, ImmutableUsers.s0_stcp12_anonuser_12f, ImmutableUsers.s0_stcp_anonuser_t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp01_anonuser_01f);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void latestSADisconnectTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_ssl_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_ssl_anonuser_t_A);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_ssl_anonuser_t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_ssl_anonuser_t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_B);
			engine.onlineRemoveInputAndVerify(ImmutableConnections.s0_stcp);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_ssl_anonuser_t_A);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_ssl_anonuser_t_A);
			engine.disconnectClientAndVerify(ImmutableUsers.s0_ssl_anonuser_t);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 800000)
	public void LatestSAFileAuth() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser12_012f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser0_0f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser_f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authstcp_authuser2_2f);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientAndSendMessage(true, ImmutableUsers.s0_authstcp_authuser12_012f);

			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser_f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authstcp_authuser012_012f);
			engine.connectClientAndVerify(false, ImmutableUsers.s0_authstcp_authuser0_0f);
			engine.authenticateAndVerifyClient(ImmutableUsers.s0_authstcp_authuser0_0f);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_authstcp_authuser2_2f);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void LatestSAInputGroups() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01_anonuser_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientAndSendMessage(false, ImmutableUsers.s0_stcp01_anonuser_01f);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_stcp12_anonuser_12f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp12_anonuser_12f);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.connectClientAndVerify(false, ImmutableUsers.s0_stcp_anonuser_t_B);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void latestSAAnon() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientAndSendMessage(true, ImmutableUsers.s0_stcp_anonuser_t);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.connectClientAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_B);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void anonWithGroupInputTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01t_anonuser_01t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp2f_anonuser_2f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);

			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp01t_anonuser_01t, ImmutableUsers.s0_stcp2f_anonuser_2f, ImmutableUsers.s0_stcp_anonuser_t);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp01t_anonuser_01t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp2f_anonuser_2f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void mcastSendTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_mcast_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);
			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_mcast_anonuser_t);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void mcastTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_mcast_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_mcast01_anonuser_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_mcast12t_anonuser_12t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_mcast3f_anonuser_3f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_stcp12_anonuser_12f);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_mcast_anonuser_t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_mcast01_anonuser_01f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_mcast12t_anonuser_12t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_mcast3f_anonuser_3f);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void streamTcpTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01_anonuser_01f_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f_B);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01_anonuser_01f_B);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_A, ImmutableUsers.s0_stcp12_anonuser_12f_A, ImmutableUsers.s0_stcp01_anonuser_01f_A, ImmutableUsers.s0_stcp_anonuser_t_B, ImmutableUsers.s0_stcp12_anonuser_12f_B, ImmutableUsers.s0_stcp01_anonuser_01f_B);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp01_anonuser_01f_A);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp01_anonuser_01f_A);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void sslTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01_anonuser_01f_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f_B);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp01_anonuser_01f_B);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_ssl_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authssl_authuser01_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authssl_authuser_f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_authssl_authuser12_012f);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true,
					ImmutableUsers.s0_stcp_anonuser_t_A,
					ImmutableUsers.s0_authssl_authuser01_01f,
					ImmutableUsers.s0_authssl_authuser_f,
					ImmutableUsers.s0_authssl_authuser12_012f,
					ImmutableUsers.s0_stcp12_anonuser_12f_A,
					ImmutableUsers.s0_stcp01_anonuser_01f_A,
					ImmutableUsers.s0_ssl_anonuser_t,
					ImmutableUsers.s0_stcp_anonuser_t_B,
					ImmutableUsers.s0_stcp12_anonuser_12f_B,
					ImmutableUsers.s0_stcp01_anonuser_01f_B);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp01_anonuser_01f_B);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authssl_authuser01_01f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_authssl_authuser12_012f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_ssl_anonuser_t);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void udpTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_udp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_udp01_anonuser_01f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_udp12t_anonuser_12t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_udp3f_anonuser_3f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_stcp12_anonuser_12f);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_udp_anonuser_t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_udp01_anonuser_01f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_udp12t_anonuser_12t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_udp3f_anonuser_3f);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	@Test(timeout = 600000)
	public void tcpTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_tcp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_tcp12_anonuser_12f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_tcp01t_anonuser_01t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_tcp2f_anonuser_2f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp12_anonuser_12f);

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_stcp12_anonuser_12f);

			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_tcp_anonuser_t);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_tcp12_anonuser_12f);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}
}
