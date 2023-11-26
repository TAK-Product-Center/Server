package com.bbn.marti.takcl.connectivity.server;

import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;

public class LocalRunnableServer extends AbstractRunnableServer {

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

		public List<String> waitForMissingLogStatements(int maxWaitTimeMs) {
			List<String> remainingStatementsToSee = definition.waitForMissingLogStatements(
					serverIdentifier, Paths.get(serverIdentifier.getServerPath()), maxWaitTimeMs);

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
	}

	@Override
	public List<AbstractServerProcess> getEnabledServerProcesses() {
		return processes.stream().filter(AbstractServerProcess::isEnabled).collect(Collectors.toList());
	}

	@Override
	protected void innerStopServer() {
		processes.parallelStream().filter(AbstractServerProcess::isEnabled).forEach(AbstractServerProcess::stop);
	}

	@Override
	protected void innerDeployServer(@NotNull String sessionIdentifier, boolean enableRemoteDebug) {
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

		Map<AbstractServerProcess, Exception> processExceptions = new HashMap<>();
		processes.stream().filter(AbstractServerProcess::isEnabled).forEach(b -> {
			try {
				b.start(enableRemoteDebug);
			} catch (Exception e) {
				processExceptions.put(b, e);
			}
		});

		if (!processExceptions.isEmpty()) {
			Exception e = null;
			for (AbstractServerProcess container : processExceptions.keySet()) {
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
		processes.parallelStream().filter(AbstractServerProcess::isEnabled).forEach(AbstractServerProcess::kill);
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

	protected List<LocalServerProcessContainer> createProcessContainerList() {
		ServerProcessDefinition[] definitions = ServerProcessDefinition.values();
		ArrayList<LocalServerProcessContainer> containers = new ArrayList<>(definitions.length);
		for (ServerProcessDefinition definition : ServerProcessDefinition.values()) {
			containers.add(new LocalServerProcessContainer(definition));
		}
		return Collections.unmodifiableList(containers);
	}

}
