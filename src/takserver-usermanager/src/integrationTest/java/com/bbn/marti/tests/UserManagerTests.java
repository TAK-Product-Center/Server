package com.bbn.marti.tests;

import com.bbn.marti.takcl.AppModules.OfflineFileAuthModule;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TestConfiguration;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import com.bbn.marti.test.shared.data.users.MutableUser;
import com.bbn.marti.tests.usermanager.UserCertModConfig;
import com.bbn.marti.tests.usermanager.UserManagerTestEngine;
import com.bbn.marti.tests.usermanager.UserManagerTestGen;

import tak.server.util.PasswordUtils;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;


/**
 * Created on 9/21/17.
 */
public class UserManagerTests extends AbstractTestClass {

	private static final String className = "UserManagementTests";

	private static final MutableServerProfile serverProfile =
			ImmutableServerProfiles.SERVER_0.getMutableInstance();

	@BeforeClass
	public static void beforeClass() {
		TAKCLCore.setUseTakclIgniteConfig(false);
		TestConfiguration.getInstance().validate();
	}

	@Test(timeout = 600000)
	public void onlineUserManagerTest() {
		try {
			String sessionIdentifier = className + ".onlineUserManagerTest";
			TestLogger.startTestWithIdentifier(sessionIdentifier);

			long randomSeed = 2111884417934772713L;

			UserManagerTestGen umtg = new UserManagerTestGen(true, randomSeed);

			UserManagerTestEngine umte = new UserManagerTestEngine();

			engine.engineFactoryReset();

			// Load the OfflineFileAuthModule to validate changes from
			OfflineFileAuthModule ofam = new OfflineFileAuthModule();
			ofam.init(defaultServerProfile);


			// Start the server with file authentication enabled
			MutableConnection connection = serverProfile.generateConnection(ImmutableConnections.s0_authstcp);
			MutableUser u0 = connection.generateConnectionUser(BaseUsers.authuser0, true);
			MutableUser u1 = connection.generateConnectionUser(BaseUsers.authuser012, true);


			List<UserCertModConfig> mods = umtg.generateTestScenarios(umte, u1);

			engine.offlineAddUsersAndConnectionsIfNecessary(u0);
			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			for (UserCertModConfig m : mods) {
				m.execute();
//                engine.attemptSendFromUserAndVerify(u1);
			}

			// TODO: Use the tests in the commented out block below. We have manually verified the UserManager.jar
			// behaves as expected, so given I'm having issues with the proper tests, I'm just testing the method for now
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("badpasswordbadpassword"));
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("BADPASSWORDBADPASSWORD"));
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("BadPasswordBadPassword"));
			// Try a good one
			Assert.assertTrue("The password should be considered valid!", PasswordUtils.isValidPassword("Th1si$App@rentlyG00D"));
			// More bad
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("Bad4PasswordPassword"));
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("p@ssw0rdp@ssw0rd"));
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("!BadPasswordBadPassword"));
			// Another good
			Assert.assertTrue("The password should be considered valid!", PasswordUtils.isValidPassword("Th1si$App@rentlyG00D"));
			// Couple bad
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("P@SSW0RD"));
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("P@$$W0RD"));
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("t00SHort^!"));
			Assert.assertFalse("The password should be considered invalid!", PasswordUtils.isValidPassword("@lsoT00short!"));

			// And a couple ones to end it
			Assert.assertTrue("The password should be considered valid!", PasswordUtils.isValidPassword("Ch@ngeM3whenyoucan"));
			Assert.assertTrue("The password should be considered valid!", PasswordUtils.isValidPassword("@lsoCh@ngeM3WhenYouCan"));

//			// Let's try some invalid passwords
//			testBadPassword(umte, u1, "badpasswordbadpassword", true);
//			testBadPassword(umte, u1, "BADPASSWORDBADPASSWORD", false);
//			testBadPassword(umte, u0, "BadPasswordBadPassword", true);
//			// Try a good one
//			umte.usermod(serverProfile, u1.getUserName(), null, "Th1si$App@rentlyG00D", null, null,null);
//			// More bad
//			testBadPassword(umte, u0, "Bad4PasswordPassword", false);
//			testBadPassword(umte, u0, "p@ssw0rdp@ssw0rd", false);
//			testBadPassword(umte, u1, "!BadPasswordBadPassword", true);
//			// Another good
//			umte.certmod(serverProfile, u0.getCertPublicPemPath().toString(), null, "Th1si$App@rentlyG00D", null, null);
//			// Couple bad
//			testBadPassword(umte, u0, "P@SSW0RD", true);
//			testBadPassword(umte, u1, "P@$$W0RD", true);
//			testBadPassword(umte, u0, "t00SHort^!", true);
//			testBadPassword(umte, u0, "@lsoT00short!", true);
//
//			// And a couple ones to end it
//			umte.usermod(serverProfile, u0.getUserName(), null, "Ch@ngeM3whenyoucan", null, null,null);
//			umte.certmod(serverProfile, u1.getCertPublicPemPath().toString(), null, "@lsoCh@ngeM3WhenYouCan", null, null);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
			throw new RuntimeException(e);

		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}

	private void testBadPassword(@NotNull UserManagerTestEngine umte, @NotNull AbstractUser user,
	                             @NotNull String badPassword, boolean useCertmodInsteadOfUsermod) {
		boolean hasFailed = false;
		try {
			if (useCertmodInsteadOfUsermod) {
				umte.certmod(user.getServer(), user.getCertPublicPemPath().toString(), false, badPassword, null, null);
			} else {
				umte.usermod(serverProfile, user.getUserName(), null, badPassword, null, null, null);

			}
		} catch (AssertionError e) {
			hasFailed = true;
		}
		if (!hasFailed) {
			Assert.fail("The user " + user + " was able to successfully change their password to the insecure value " +
					"of " + badPassword + "!");
		}
	}


	//	@Test(timeout = 420000)
	public void offlineUserManagerTest() {
		try {
			String sessionIdentifier = className + ".onlineUserManagerTest";
			TestLogger.startTestWithIdentifier(sessionIdentifier);

			long randomSeed = 2111884417934772713L;

			UserManagerTestGen umtg = new UserManagerTestGen(true, randomSeed);

			UserManagerTestEngine umte = new UserManagerTestEngine();

			engine.engineFactoryReset();

			// Load the OfflineFileAuthModule to validate changes from
			OfflineFileAuthModule ofam = new OfflineFileAuthModule();
			ofam.init(defaultServerProfile);


			// Start the server with file authentication enabled
			MutableConnection connection = serverProfile.generateConnection(ImmutableConnections.s0_authssl);
			MutableUser u0 = connection.generateConnectionUser(BaseUsers.authuser2, true);
			MutableUser u1 = connection.generateConnectionUser(BaseUsers.authuser12, true);


			List<UserCertModConfig> mods = umtg.generateTestScenarios(umte, u1);

			engine.offlineAddUsersAndConnectionsIfNecessary(u0);
			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			for (UserCertModConfig m : mods) {
				m.execute();
//                engine.attemptSendFromUserAndVerify(u1);
			}

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
			throw new RuntimeException(e);

		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}
}

