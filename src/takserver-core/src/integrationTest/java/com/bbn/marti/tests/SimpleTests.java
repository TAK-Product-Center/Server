//package com.bbn.marti.tests;
//
//import com.bbn.marti.test.shared.AbstractTestClass;
//import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
//import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
//import com.bbn.marti.test.shared.data.users.AbstractUser;
//import org.jetbrains.annotations.NotNull;
//import org.junit.Assert;
//import org.junit.Test;
//
///**
// * Created on 10/28/15.
// */
//public class SimpleTests extends AbstractTestClass {
//
//	public void simpleExecution(@NotNull AbstractUser sendUser, @NotNull AbstractUser receiveUser) {
//		if (sendUser.getConnection().getProtocol().canConnect()) {
//			engine.connectClientAndVerify(true, sendUser);
//		}
//
//		if (receiveUser.getConnection().getProtocol().canConnect()) {
//			engine.connectClientAndVerify(true, receiveUser);
//		}
//
//		engine.attemptSendFromUserAndVerify(sendUser);
//	}
//
//	public void abstractSimpleTest(@NotNull String sessionIdentifier, @NotNull AbstractUser sendUser, @NotNull AbstractUser receiveUser) {
//		try {
//			engine.offlineAddUsersAndConnectionsIfNecessary(sendUser);
//			engine.offlineAddUsersAndConnectionsIfNecessary(receiveUser);
//
//			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
//
//			simpleExecution(sendUser, receiveUser);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail(e.getMessage());
//		} finally {
//			engine.stopServers(ImmutableServerProfiles.SERVER_0);
//		}
//	}
//
//	@Test(timeout = 600000)
//	public void simpleStcp() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_stcp_anonuser_t_A);
//	}
//
////	@Test(timeout = 600000)
////	public void simpleSaProxy() {
////		String sessionIdentifier = initTestMethod();
////		abstractSimpleTest(sessionIdentifier, );
////	}
//
//	@Test(timeout = 600000)
//	public void simpleSsl() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_ssl_anonuser_t, ImmutableUsers.s0_ssl_anonuser_t_A);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleTls() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_tls_anonuser_t, ImmutableUsers.s0_tls_anonuser_t_A);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleAuthSsl() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_authssl_authuser0_0f, ImmutableUsers.s0_authssl_authuser01_01f);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleAuthTls() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_authtls_authuser0_0f, ImmutableUsers.s0_authtls_authuser01_01f);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleAuthStcp() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_authstcp_authuser0_0f, ImmutableUsers.s0_authstcp_authuser01_01f);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleTcp() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_tcp_anonuser_t, ImmutableUsers.s0_stcp_anonuser_t);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleUdp() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_udp_anonuser_t, ImmutableUsers.s0_stcp_anonuser_t);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleMcast() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_mcast_anonuser_t, ImmutableUsers.s0_stcp_anonuser_t);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleSubMcast() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_submcast_anonuser_t);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleSubTcp() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_subtcp_anonuser_t);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleSubUdp() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_subudp_anonuser_t);
//	}
//
//	@Test(timeout = 600000)
//	public void simpleSubStcp() {
//		String sessionIdentifier = initTestMethod();
//		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_substcp_anonuser_t);
//	}
//
//
//	@Test(timeout = 600000)
//	public void simpleCombined() {
//		try {
//			String sessionIdentifier = initTestMethod();
//
//			ImmutableUsers[] transactionUsers = new ImmutableUsers[]{
//					ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_stcp_anonuser_t_A,
//					ImmutableUsers.s0_ssl_anonuser_t, ImmutableUsers.s0_ssl_anonuser_t_A,
//					ImmutableUsers.s0_tls_anonuser_t, ImmutableUsers.s0_tls_anonuser_t_A,
//					ImmutableUsers.s0_authssl_authuser0_0f, ImmutableUsers.s0_authssl_authuser01_01f,
//					ImmutableUsers.s0_authtls_authuser0_0f, ImmutableUsers.s0_authtls_authuser01_01f,
//					ImmutableUsers.s0_authstcp_authuser0_0f, ImmutableUsers.s0_authstcp_authuser01_01f,
//					ImmutableUsers.s0_tcp_anonuser_t, ImmutableUsers.s0_stcp_anonuser_t,
//					ImmutableUsers.s0_udp_anonuser_t, ImmutableUsers.s0_stcp_anonuser_t,
//					ImmutableUsers.s0_mcast_anonuser_t, ImmutableUsers.s0_stcp_anonuser_t,
//					ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_submcast_anonuser_t,
////					ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_subtcp_anonuser_t,
////					ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_subudp_anonuser_t,
////					ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_substcp_anonuser_t
//			};
//
//			engine.offlineAddUsersAndConnectionsIfNecessary(transactionUsers);
//			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
//
//			for (int i = 0; i < transactionUsers.length; i = i + 2) {
//				simpleExecution(transactionUsers[i], transactionUsers[i + 1]);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail(e.getMessage());
//		} finally {
//			engine.stopServers(ImmutableServerProfiles.SERVER_0);
//		}
//	}
//
////	@Test(timeout = 600000)
////	public void simpleSubSsl() {
////		String sessionIdentifier = initTestMethod();
////		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.s0_sub);
////	}
////
////	@Test(timeout = 600000)
////	public void simpleSubTls() {
////		String sessionIdentifier = initTestMethod();
////		abstractSimpleTest(sessionIdentifier, ImmutableUsers.s0_stcp_anonuser_t, ImmutableUsers.);
////	}
//}
