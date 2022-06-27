package com.bbn.marti.takcl.connectivity;

import com.bbn.marti.takcl.AppModules.OfflineConfigModule;
import com.bbn.marti.takcl.AppModules.OfflineFileAuthModule;
import com.bbn.marti.takcl.AppModules.OnlineFileAuthModule;
import com.bbn.marti.takcl.AppModules.OnlineInputModule;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TakclIgniteHelper;
import com.bbn.marti.takcl.TestConfiguration;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public abstract class AbstractRunnableServer {

	private final Logger logger = LoggerFactory.getLogger(AbstractRunnableServer.class);

	private static final List<String> LOG_ENTRIES_FOR_READY_SERVER = Arrays.asList(
			" c.b.m.s.DistributedConfiguration - execute method DistributedConfiguration",
			"t.s.f.DistributedFederationManager - execute method DistributedFederationManager",
			"c.b.m.s.DistributedSubscriptionManager - DistributedSubscriptionManager execute",
			"c.b.m.s.DistributedSubscriptionManager - DistributedSubscriptionManager execute",
			"c.b.m.g.DistributedPersistentGroupManager - execute method DistributedPersistentGroupManager",
			"t.s.profile.DistributedServerInfo - execute method DistributedServerInfo",
			"t.s.c.DistributedSecurityManager - execute method DistributedSecurityManager",
			"c.b.m.s.DistributedContactManager - execute method DistributedContactManager",
			"c.b.m.r.DistributedRepeaterManager - execute method DistributedRepeaterManager",
			"c.b.m.groups.DistributedUserManager - DistributedUserManager execute",
			"t.s.cluster.DistributedInputManager - execute method DistributedInputManager",
			"c.b.m.DistributedMetricsCollector - execute method DistributedMetricsCollector",
			"c.b.m.service.MessagingInitializer - takserver-core init complete",
			"o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started"
	);

	private static final List<String> LOGGING_ARGUMENTS = Arrays.asList(
			"--logging.level.com.bbn=TRACE",
			"--logging.level.org.apache.ignite=INFO",
			"--logging.level.tak=TRACE");

	public static RUNMODE runMode = RUNMODE.AUTOMATIC;

	public static Integer debuggeeIdentifier = null;

	protected static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss_SSS");

	public enum RUNMODE {
		AUTOMATIC(true, true), // Automatic server control
		MANUAL_SERVER_MANAGEMENT(false, true), // Manual server control to run your own server instance (console prompts)
		MANUAL_SERVER_SLEEP_MANAGEMENT(false, false); // Manual server control and sleep control (console prompts)

		public final boolean startAutomatically;
		public final boolean sleepAutomatically;

		RUNMODE(boolean startAutomatically, boolean sleepAutomatically) {
			this.startAutomatically = startAutomatically;
			this.sleepAutomatically = sleepAutomatically;
		}
	}

	public enum ServerState {
		CONFIGURING,
		DEPLOYING,
		RUNNING,
		STOPPING,
		STOPPED,
	}

	private ServerState serverState = ServerState.STOPPED;

	private final OnlineInputModule onlineInputModule = new OnlineInputModule();
	private final OnlineFileAuthModule onlineFileAuthModule = new OnlineFileAuthModule();
	private final OfflineConfigModule offlineConfigModule = new OfflineConfigModule();
	private final OfflineFileAuthModule offlineFileAuthtModule = new OfflineFileAuthModule();
	protected File coreConfigFile;
	protected Path logPath;

	protected static String logDirectory;
	private String sessionIdentifier;

	public final OfflineConfigModule getOfflineConfigModule() {
		checkServerState(false);
		return offlineConfigModule;
	}

	public final OnlineInputModule getOnlineInputModule() {
		checkServerState(true);
		return onlineInputModule;
	}

	public final OfflineFileAuthModule getOfflineFileAuthModule() {
		checkServerState(false);
		return offlineFileAuthtModule;
	}

	public final OnlineFileAuthModule getOnlineFileAuthModule() {
		checkServerState(true);
		return onlineFileAuthModule;
	}

	/**
	 * If set to true, all tests will pause when the server start or shutdown instruction is sent, allowing the user to
	 * manually turn the server on and off (useful for debugging)
	 *
	 * @param runMode What run mode to run in
	 */
	public static synchronized void setControlMode(RUNMODE runMode) {
		AbstractRunnableServer.runMode = runMode;
	}

	public static synchronized void setDebuggee(@Nullable Integer serverIdentifier) {
		debuggeeIdentifier = serverIdentifier;

	}

	public static void setLogDirectory(@NotNull String newLogDirectory) {
		logDirectory = newLogDirectory;
	}

	protected final AbstractServerProfile serverIdentifier;

	protected AbstractRunnableServer(AbstractServerProfile serverIdentifier) {
		this.serverIdentifier = serverIdentifier;
		this.offlineConfigModule.init(serverIdentifier);
		this.offlineFileAuthtModule.init(serverIdentifier);
	}

	public final synchronized void stopServer() {
		if (serverState == ServerState.STOPPING || serverState == ServerState.STOPPED) {
			logger.warn("Server '" + serverIdentifier.toString() + "' Stop requested even though it is already stopped!");
		}

		try {
			serverState = ServerState.STOPPING;

			Exception igniteException = null;
			try {
				TakclIgniteHelper.closeAssociatedIgniteInstance(serverIdentifier);
			} catch (Exception e) {
				igniteException = e;
			}

			if (!TAKCLCore.keepServersRunning) {
				innerStopServer();
			}

			onlineInputModule.halt();
			onlineFileAuthModule.halt();
			offlineConfigModule.halt();
			offlineFileAuthtModule.halt();

			serverState = ServerState.STOPPED;

			if (igniteException != null) {
				throw new RuntimeException(igniteException);
			}
		} finally {
			serverIdentifier.rerollIgnitePorts();
			collectFinalLogs();
		}
	}


	public final synchronized void startServer(@Nullable String sessionIdentifier, int connectionDelayMS) {
		this.sessionIdentifier = sessionIdentifier;
		if (serverState != ServerState.STOPPED) {
			logger.warn("Server '" + serverIdentifier.toString() + "' Start requested even though it is already running or starting!!");
		}

		boolean isFileAuthEnabled = offlineConfigModule.isFileAuthEnabled();

		if (TAKCLCore.useRunningServer) {
			serverState = ServerState.RUNNING;
		} else {

			long startTimeMs = System.currentTimeMillis();

			serverState = ServerState.CONFIGURING;

			TestConfiguration.getInstance().configureDatabase(serverIdentifier, this.offlineConfigModule.getRepository());
			this.offlineConfigModule.saveChanges();

			this.offlineConfigModule.enableSwagger();

			String serverPath = serverIdentifier.getServerPath();

			// Changing the flow tag to match the server ID
			this.offlineConfigModule.setFlowTag(serverIdentifier.getConsistentUniqueReadableIdentifier());

			String debugServerStr = System.getProperty("com.bbn.marti.takcl.takserver.debug");

			boolean enableRemoteDebug = (debugServerStr != null && debugServerStr.toLowerCase().equals("true"));

			if (enableRemoteDebug && serverIdentifier.getUrl().equals(ImmutableServerProfiles.DEFAULT_LOCAL_IP)) {
				throw new RuntimeException("Debug mode is not currently supported for Docker hosts!");
			}

			if (!runMode.startAutomatically && serverIdentifier.getUrl().equals(ImmutableServerProfiles.DEFAULT_LOCAL_IP)) {
				throw new RuntimeException("Manual starting of docker instances is currently not supported!");
			}

			innerConfigureServer(sessionIdentifier, enableRemoteDebug);
			serverState = ServerState.DEPLOYING;

			innerDeployServer(sessionIdentifier, startTimeMs, enableRemoteDebug);
			System.out.println(serverIdentifier.getConsistentUniqueReadableIdentifier() + "' started.");
			serverState = ServerState.RUNNING;

			if (!TAKCLCore.useRunningServer) {
				if (TAKCLCore.disableApiProcess) {
					waitForServerReady(connectionDelayMS,
							Paths.get(serverPath).resolve("logs/takserver.log").toFile(),
							Paths.get(serverPath).resolve("logs/takserver-messaging.log").toFile());
				} else {
					waitForServerReady(connectionDelayMS,
							Paths.get(serverPath).resolve("logs/takserver.log").toFile(),
							Paths.get(serverPath).resolve("logs/takserver-messaging.log").toFile(),
							Paths.get(serverPath).resolve("logs/takserver-api.log").toFile());
				}
			}

			System.out.println("Server started successfully after " + ((System.currentTimeMillis() - startTimeMs) / 1000) + " seconds.");
		}
		if (!isRunning()) {
			throw new RuntimeException("Server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' appears to have shutdown immediately after starting. Please ensure another server isn't already running and your config is valid!");
		}

		onlineInputModule.init(serverIdentifier);

		if (isFileAuthEnabled) {
			onlineFileAuthModule.init(serverIdentifier);
		}
	}

	private void waitForServerReady(int maxWaitTime, File... logFiles) {
		List<File> missingLogFiles = new ArrayList<>(Arrays.asList(logFiles));

		boolean serverReady = false;
		int duration = 0;
		List<String> remainingStatementsToSee = new ArrayList<>(LOG_ENTRIES_FOR_READY_SERVER);

		List<BufferedReader> logFileReaders = new ArrayList<>(missingLogFiles.size());

		try {
			List<File> filesToRemove = new LinkedList<>();
			while (!serverReady && duration < maxWaitTime) {
				filesToRemove.clear();
				for (File logFile : missingLogFiles) {
					if (logFile.exists()) {
						logFileReaders.add(new BufferedReader(new FileReader(logFile)));
						filesToRemove.add(logFile);
					}
				}
				missingLogFiles.removeAll(filesToRemove);

				List<String> statementsToRemove = new ArrayList<>(remainingStatementsToSee.size());
				for (BufferedReader reader : logFileReaders) {
					String logLine = reader.readLine();
					while (logLine != null) {
						for (String value : remainingStatementsToSee) {
							if (logLine.contains(value)) {
								statementsToRemove.add(value);
							}
						}
						logLine = reader.readLine();
					}
				}
				for (String value : statementsToRemove) {
					remainingStatementsToSee.remove(value);
				}
				statementsToRemove.clear();

				if (remainingStatementsToSee.isEmpty()) {
					serverReady = true;
				} else {
					Thread.sleep(500);
					duration += 500;
				}
			}

			if (TAKCLCore.serverStartupWaitTime != null && duration < TAKCLCore.serverStartupWaitTime) {
					System.err.println("Sleeping for " + (TAKCLCore.serverStartupWaitTime - duration) + " minutes.");
					Thread.sleep(TAKCLCore.serverStartupWaitTime - duration);
			}

			if (serverReady) {
				System.out.println("Server appears to be ready based on log statements after " + duration + " ms");
			} else {
				System.out.println("Server init timeout of " + maxWaitTime + " ms reached. The following log statements were not seen:\n\t" +
						String.join("\"\n\t\"", remainingStatementsToSee) + "\n There is a good chance the tests may fail!");

			}
			if (!isRunning()) {
				throw new RuntimeException("Server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' appears to have shutdown immediately after starting. Please ensure another server isn't already running and your config is valid!");
			}
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void collectFinalLogs() {
		System.err.println("COLLECTING LOGS!");
		try {
			Path sourceLogDirectory = Paths.get(serverIdentifier.getServerPath()).resolve("logs");

			Path userAuthFilePath = Paths.get(serverIdentifier.getServerPath()).resolve("UserAuthenticationFile.xml");
			if (Files.exists(userAuthFilePath)) {
				Path target = logPath.resolve(serverIdentifier + "-" + userAuthFilePath.getFileName().toString());
				if (Files.exists(target)) {
					target = logPath.resolve(serverIdentifier + "-" + userAuthFilePath.getFileName().toString() +
							"-" + Long.toString(System.currentTimeMillis()));
				}
				Files.copy(userAuthFilePath, target);
			}

			for (Path filePath : Files.walk(sourceLogDirectory).collect(Collectors.toList())) {
				if (!Files.isDirectory(filePath)) {
					Path target = logPath.resolve(serverIdentifier + "_logs-" + filePath.getFileName().toString());
					if (Files.exists(target)) {
						target = logPath.resolve(serverIdentifier + "_logs-" + filePath.getFileName().toString() +
								"-" + Long.toString(System.currentTimeMillis()));
					}
					Files.copy(filePath, target);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized boolean isRunning() {
		return serverState == ServerState.RUNNING;
	}

	public synchronized void watchdogPoll() throws Exception {
		if (isRunning()) {
			checkServerState(true);
		}
	}

	private synchronized void checkServerState(boolean shouldBeOnline) {
		boolean currentStateValid;
		boolean serverProcessRunning = isServerProcessRunning();

		switch (serverState) {
			case CONFIGURING:
			case STOPPED:
				currentStateValid = !serverProcessRunning && !shouldBeOnline;
				break;

			case RUNNING:
				currentStateValid = shouldBeOnline && (serverProcessRunning || TAKCLCore.useRunningServer);
				break;

			case DEPLOYING:
			case STOPPING:
				currentStateValid = false;
				break;

			default:
				throw new RuntimeException("Unexpected state " + serverState + "!");
		}

		if (!currentStateValid) {
			String callingMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
			throw new RuntimeException("Cannot call " + callingMethod + " on server '" +
					serverIdentifier.getConsistentUniqueReadableIdentifier() + "' while it its state is " +
					serverState.name() + " and the server process is " + (serverProcessRunning ? "" : "not") + " running!!");
		}
	}

	protected void offlineFactoryResetServer() {
		checkServerState(false);
		offlineConfigModule.resetConfig();
		offlineFileAuthtModule.resetConfig();
	}

	public final void killServer() {
		if (!TAKCLCore.keepServersRunning) {
			innerKillServer();
		}
	}

	protected abstract void innerStopServer();

	protected abstract void innerConfigureServer(@Nullable String sessionIdentifier, boolean enableRemoteDebug);

	protected abstract void innerDeployServer(@Nullable String sessionIdentifier, long startTimeMs, boolean enableRemoteDebug);

	protected abstract void innerKillServer();

	protected abstract boolean isServerProcessRunning();
}
