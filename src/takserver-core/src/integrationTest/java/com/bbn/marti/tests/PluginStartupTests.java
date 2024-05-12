package com.bbn.marti.tests;

import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.takcl.connectivity.server.ServerProcessConfiguration;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.takcl.connectivity.server.ServerProcessDefinition;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.ActionEngine;

import javax.annotation.concurrent.Immutable;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PluginStartupTests extends AbstractTestClass {

	private static final String className = "PluginStartupTests";

	@Test(timeout = 420000)
	public void pluginStartupValiationTest() {
		try {
			String sessionIdentifier = initTestMethod();

			engine.overrideDefaultProcessConfiguration(ImmutableServerProfiles.SERVER_0, ServerProcessConfiguration.ConfigMessagingApiPlugins);
			System.out.println("--- Starting offlineAddUsersAndConnectionsIfNecessary for s0_stcp_anonuser_t_A ...");
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp_anonuser_t_A);
			System.out.println("--- Done with offlineAddUsersAndConnectionsIfNecessary for s0_stcp_anonuser_t_A");

			System.out.println("--- Starting offlineEnableLatestSA for SERVER_0 ...");
			engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);
			System.out.println("--- Done with offlineEnableLatestSA for SERVER_0");

			System.out.println("--- Starting startServerWithStartupValidation ...");
			engine.startServerWithStartupValidation(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
			System.out.println("--- Done with startServerWithStartupValidation");
			
			System.out.println("--- Starting connectClientsAndVerify ...");
			engine.connectClientsAndVerify(true, ImmutableUsers.s0_stcp_anonuser_t_A);
			System.out.println("--- Done with connectClientsAndVerify");

			System.out.println("Sleep for 10s...");
			Thread.sleep(10000);
			
			System.out.println("--- Starting verifyReceivedMessageSentFromPlugin ...");
			engine.verifyReceivedMessageSentFromPlugin(ImmutableUsers.s0_stcp_anonuser_t_plugin1, ImmutableUsers.s0_stcp_anonuser_t_A);
			System.out.println("--- Done with verifyReceivedMessageSentFromPlugin");
			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
		}
	}
}
