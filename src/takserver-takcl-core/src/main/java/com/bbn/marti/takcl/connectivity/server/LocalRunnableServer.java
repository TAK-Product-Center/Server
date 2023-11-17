package com.bbn.marti.takcl.connectivity.server;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;

public class LocalRunnableServer extends AbstractRunnableServer {

	private final List<LocalServerProcessContainer> processes;

	public class LocalServerProcessContainer extends AbstractServerProcess {

		private Process innerProcess = null;

		public LocalServerProcessContainer(@NotNull ServerProcessDefinition processDefinition) {
			super(processDefinition);
		}

		@Override
		public boolean isRunning() {
			if (innerProcess == null) {
				return false;
			}

			try {
				innerProcess.exitValue();
				return false;
			} catch (IllegalThreadStateException e) {
				return true;
			}
		}

		@Override
		public void stop() {
			try {
				if (innerProcess != null) {
					innerProcess.destroy();
					innerProcess.waitFor();
				}
				System.out.println("Server " + serverIdentifier.getConsistentUniqueReadableIdentifier() +
						" process " + getIdentifier() + " stopped successfully.");
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void kill() {
			try {
				if (innerProcess != null) {
					innerProcess.destroyForcibly();
				}
			} catch (Exception e) {
				// Pass, I tried....
			}
		}

		@Override
		public String toString() {
			return "LocalServerProcess(identifier=\"" + getIdentifier() + "\", enabled=" + isEnabled() +
					", isRunning=" + isRunning() + ")";
		}

		@Override
		public void start(boolean enableRemoteDebug) {
			String serverPath = serverIdentifier.getServerPath();

			// Build the command string
			List<String> command = new LinkedList<>(Arrays.asList(
					"java",
					"-Dio.netty.tmpdir=" + serverPath,
					"-Djava.io.tmpdir=" + serverPath,
					"-Dio.netty.native.workdir=" + serverPath
			));

			command.addAll(JDK_JAVA_OPTIONS);
			
			if (definition.jvmFlags != null) {
				command.addAll(definition.jvmFlags);
			}
			
			if (enableRemoteDebug) {
				command.add(REMOTE_DEBUG_ARGS);
			}

			command.addAll(Arrays.asList("-Xmx2000m", "-XX:+HeapDumpOnOutOfMemoryError", "-jar", definition.jarName));
			if (!definition.jarName.toLowerCase().contains("federation"))
				command.addAll(LOGGING_ARGUMENTS);

			// Build the process
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(new File(serverPath));

			// Set up log redirection paths
			String serverName = serverIdentifier.getConsistentUniqueReadableIdentifier();
			File errorFile = logPath.resolve(serverName + "-" + definition.identifier + ".stderr.txt").toFile();
			File outputFile = logPath.resolve(serverName + "-" + definition.identifier + ".stdout.txt").toFile();
			processBuilder.redirectError(errorFile);
			processBuilder.redirectOutput(outputFile);

			// Set up environment variables
			Map<String, String> environmentVariables = new HashMap<>();
			environmentVariables.put("IGNITE_HOME", serverIdentifier.getServerPath());
			processBuilder.environment().putAll(environmentVariables);

			List<String> envVarLines = new LinkedList<>();
			for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
				envVarLines.add(entry.getKey() + "='" + entry.getValue() + "'");
			}
						
			try {
				System.out.println(
						"Starting TAKServer in directory\n\t'" + processBuilder.directory().getAbsolutePath() +
								"'\nWith command: [\n\t" + String.join("  \\\n\t", command) + "\n]" +
								"\nWith additional environment variables [\n\t" + String.join("\n\t,", envVarLines) + "\n]"
				);

				// Start the process, giving a little break to prevent any weird conflicts
				innerProcess = processBuilder.start();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				System.out.println("Server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' " +
						definition.identifier + " component started and pending validation.");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public List<String> waitForMissingLogStatements(int maxWaitDuration) {
			List<String> remainingStatementsToSee = definition.waitForMissingLogStatements(
					serverIdentifier, Paths.get(serverIdentifier.getServerPath()), maxWaitDuration);

			if (!isRunning()) {
				throw new RuntimeException("Server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' appears to have shutdown immediately after starting. Please ensure another server isn't already running and your config is valid!");
			}

			return remainingStatementsToSee;
		}
	}

	public static final String REMOTE_DEBUG_ARGS = "-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y";

	private static final List<String> JDK_JAVA_OPTIONS = Arrays.asList(
			"-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/",
			"-Djava.net.preferIPv4Stack=true",
			"-Djava.security.egd=file:/dev/./urandom",
			"-DIGNITE_UPDATE_NOTIFIER=false",
			"-Djdk.tls.client.protocols=TLSv1.2"
	);

	public LocalRunnableServer(AbstractServerProfile serverIdentifier) {
		super(serverIdentifier);
		processes = Collections.unmodifiableList(Arrays.asList(
				new LocalServerProcessContainer(ServerProcessDefinition.MessagingService),
				new LocalServerProcessContainer(ServerProcessDefinition.ApiService),
				new LocalServerProcessContainer(ServerProcessDefinition.PluginManager),
				new LocalServerProcessContainer(ServerProcessDefinition.RetentionService),
				new LocalServerProcessContainer(ServerProcessDefinition.FederationHubPolicy),
				new LocalServerProcessContainer(ServerProcessDefinition.FederationHubBroker)
		));
	}

	@Override
	protected boolean isServerProcessRunning(boolean shouldBeOnline) {

		// If they aren't all the same, raise an exception indicating the difference
		Boolean sharedState = null;

		// Get the state of all enabled processes
		Map<String, Boolean> enabledProcessStates = processes.stream().filter(
				LocalServerProcessContainer::isEnabled).collect(Collectors.toMap(
				LocalServerProcessContainer::getIdentifier, LocalServerProcessContainer::isRunning));

		for (String processName : enabledProcessStates.keySet()) {
			boolean state = enabledProcessStates.get(processName);

			if (shouldBeOnline && !state) {

				logger.error("The server process " + processName + " Should be running but it is not!  `ps -aux` output:");
				try {
					File f = File.createTempFile("PsOutput", ".txt");
					String psLog = null;
					ProcessBuilder pb = new ProcessBuilder().command("ps", "-aux").redirectErrorStream(true).redirectOutput(f);
					Process p = pb.start();
					p.waitFor();
					String psResults = Files.readString(f.toPath());
					System.out.println(psResults);

				} catch (Exception e) {
					throw new RuntimeException(e);
				}

			} else if (!shouldBeOnline && state) {
				logger.error("The server process " + processName + " Should not be running but it is!");
			}

			if (sharedState == null) {
				sharedState = state;
			}
			if (state != sharedState) {
				StringBuilder sb = new StringBuilder("Inconsistent process states for " + serverIdentifier + ":");
				for (String processName2 : enabledProcessStates.keySet()) {
					sb.append(" ").append(processName2).append(".isRunning=").append(enabledProcessStates.get(processName2));
				}
				logger.error(sb.toString());
				throw new RuntimeException(sb.toString());
			}
		}
		assert (sharedState != null);
		return sharedState;
	}

	@Override
	public List<AbstractServerProcess> getEnabledServerProcesses() {
		return processes.stream().filter(LocalServerProcessContainer::isEnabled).collect(Collectors.toList());
	}

	@Override
	protected void innerStopServer() {
		processes.parallelStream().filter(LocalServerProcessContainer::isEnabled).forEach(LocalServerProcessContainer::stop);
	}

	@Override
	protected void innerDeployServer(@NotNull String sessionIdentifier, boolean enableRemoteDebug) {
		String serverPathString = serverIdentifier.getServerPath();

		try {
			logPath = Paths.get(logDirectory).toAbsolutePath().resolve(sessionIdentifier);
			if (!Files.exists(logPath)) {
				Files.createDirectories(logPath);
			}

			String serverName = serverIdentifier.getConsistentUniqueReadableIdentifier();

			Path coreConfigTargetPath = logPath.resolve(serverName + "-CoreConfig.xml");
			if (!Files.exists(coreConfigTargetPath)) {
				Files.copy(Paths.get(serverIdentifier.getConfigFilePath()), coreConfigTargetPath);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Map<LocalServerProcessContainer, Exception> processExceptions = new HashMap<>();
		processes.stream().filter(LocalServerProcessContainer::isEnabled).forEach(b -> {
			try {
				b.start(enableRemoteDebug);
			} catch (Exception e) {
				processExceptions.put(b, e);
			}
		});

		if (!processExceptions.isEmpty()) {
			Exception e = null;
			for (LocalServerProcessContainer container : processExceptions.keySet()) {
				System.err.println("Error starting up process \"" + container.getIdentifier() + "\"!");
				e = processExceptions.get(container);
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}
			// Throw something to disrupt the tests
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void innerKillServer() {
		processes.parallelStream().filter(LocalServerProcessContainer::isEnabled).forEach(LocalServerProcessContainer::kill);
	}

//	private void checkServerState(boolean shouldBeOnline) {
//
//		boolean serverState = isRunning();
//
//		if (shouldBeOnline != serverState) {
//			String callingMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
//			throw new RuntimeException("Cannot call " + callingMethod + " on server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' while it is " +
//					(serverState ? "online!" : "offline!"));
//		}
//	}

	@Override
	protected void collectFinalLogs() {
		System.out.println("COLLECTING LOGS!");
		try {
			Path sourceLogDirectory = Paths.get(serverIdentifier.getServerPath()).resolve("logs");

			Path userAuthFilePath = Paths.get(serverIdentifier.getServerPath()).resolve("UserAuthenticationFile.xml");
			if (Files.exists(userAuthFilePath)) {
				Path target = logPath.resolve(serverIdentifier + "-" + userAuthFilePath.getFileName().toString());
				if (Files.exists(target)) {
					target = logPath.resolve(serverIdentifier + "-" + userAuthFilePath.getFileName().toString() +
							"-" + System.currentTimeMillis());
				}
				Files.copy(userAuthFilePath, target);
			}

			for (Path filePath : Files.walk(sourceLogDirectory).collect(Collectors.toList())) {
				if (!Files.isDirectory(filePath)) {
					Path target;
					if (filePath.getFileName().toString().endsWith(".log")) {
						target = logPath.resolve(serverIdentifier + "_logs-" + filePath.getFileName().toString() + ".txt");
					} else {
						target = logPath.resolve(serverIdentifier + "_logs-" + filePath.getFileName().toString());
					}
					if (Files.exists(target)) {
						target = logPath.resolve(serverIdentifier + "_logs-" + filePath.getFileName().toString() +
								"-" + System.currentTimeMillis());
					}
					Files.copy(filePath, target);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		System.out.println("COLLECTING LOGS DONE!");
	}
}
