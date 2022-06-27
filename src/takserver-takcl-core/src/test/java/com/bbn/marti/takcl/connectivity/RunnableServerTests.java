package com.bbn.marti.takcl.connectivity;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.ActionEngine;
import org.junit.After;
import org.junit.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RunnableServerTests {

//	public static final String TEST_ARTIFACT_DIRECTORY = TAKCLConfigModule.getInstance().getTestArtifactDirectory();
//
//
//	private static List<String> logFailureStrings = Arrays.asList(
//			"asdf",
//			"asdfasdf"
//
//	);
//
//	RunnableServer server;
//
//	@After
//	public void halt() {
//		if (server != null) {
//			server.stopServer();
//		}
//	}
//
//	//	@Test
//	public void testRunnableServerBasicStart() {
//		RunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
//		RunnableServer server = RunnableServer.buildServerInstance(ImmutableServerProfiles.SERVER_0);
//		server.startServer("testRunnableServerBasicStart", ActionEngine.getServerStartTimeDelay());
//		Assert.assertTrue(server.isRunning());
//		server.stopServer();
//		File errorFile = server.getStderrFile();
//		File outputFile = server.getStdoutFile();
//
//
//	}
}
