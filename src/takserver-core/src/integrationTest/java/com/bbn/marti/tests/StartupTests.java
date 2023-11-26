package com.bbn.marti.tests;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.takcl.connectivity.server.ServerProcessDefinition;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.ActionEngine;
import com.bbn.marti.test.shared.engines.TestEngine;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created on 10/28/15.
 */
public class StartupTests extends AbstractTestClass {

	private static final String className = "StartupTests";

	@BeforeClass
	public static void setup() {
		ServerProcessDefinition.PluginManager.setEnabled(false);
		ServerProcessDefinition.RetentionService.setEnabled(true);
		AbstractTestClass.setup();
	}

	@Test(timeout = 420000)
	public void jarStartupValiationTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_B);
			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);
			engine.startServerWithStartupValidation(ImmutableServerProfiles.SERVER_0, sessionIdentifier, false, true);
			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_A, ImmutableUsers.s0_stcp_anonuser_t_B);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp_anonuser_t_A);


		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}
}
