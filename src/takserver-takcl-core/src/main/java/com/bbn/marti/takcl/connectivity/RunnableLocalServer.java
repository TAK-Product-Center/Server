package com.bbn.marti.takcl.connectivity;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.bbn.marti.config.Input;
import com.bbn.marti.config.Network;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;

public class RunnableLocalServer extends AbstractRunnableServer {

	public static final String REMOTE_DEBUG_ARGS = "-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y";

	private static final List<String> JDK_JAVA_OPTIONS = Arrays.asList(
			"-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/",
			"-Djava.net.preferIPv4Stack=true",
			"-Djava.security.egd=file:/dev/./urandom",
			"-DIGNITE_UPDATE_NOTIFIER=false",
			"-Djdk.tls.client.protocols=TLSv1.2"
	);

	private static final List<String> LOGGING_ARGUMENTS = Arrays.asList(
			"--logging.level.com.bbn=TRACE",
			"--logging.level.org.apache.ignite=INFO",
			"--logging.level.tak=TRACE");

	private Process messagingProcess;
	private Process apiProcess;

	private boolean isManualServerControlOverrideServerRunning = false;

	public RunnableLocalServer(AbstractServerProfile serverIdentifier) {
		super(serverIdentifier);
	}

	@Override
	protected boolean isServerProcessRunning() {
		if (TAKCLCore.useMonolithProfile) {
			if (messagingProcess == null) {
				return false;
			} else {
				return true;
			}
		} else if (apiProcess == null && messagingProcess == null) {
			return false;
		} else if ((apiProcess != null || TAKCLCore.disableApiProcess) && messagingProcess != null) {
			boolean mpRunning = false;
			boolean apiRunning = false;
			try {
				messagingProcess.exitValue();
			} catch (IllegalThreadStateException e) {
				mpRunning = true;
			}

			try {
				apiProcess.exitValue();
			} catch (IllegalThreadStateException e) {
				apiRunning = true;
			}
			return mpRunning && apiRunning;
		} else {
			throw new RuntimeException("apiProcess == " + (TAKCLCore.disableApiProcess ? "DISBLED" : apiProcess) +
					", messagingProcess == " + messagingProcess + "!");
		}
	}

	public static final List<String> buildExecCommand(AbstractServerProfile serverProfile, boolean enableRemoteDebug, String profile) {
		String serverPath = serverProfile.getServerPath();
		List<String> command = new LinkedList<>(Arrays.asList(
				"java",
				"-Dio.netty.tmpdir=" + serverPath,
				"-Djava.io.tmpdir=" + serverPath,
				"-Dio.netty.native.workdir=" + serverPath
		));

		command.addAll(JDK_JAVA_OPTIONS);
		command.add("-Dspring.profiles.active=" + profile);

		if (enableRemoteDebug) {
			command.add(REMOTE_DEBUG_ARGS);
		}

		command.addAll(Arrays.asList(
				"-jar", "-Xmx2000m", "-XX:+HeapDumpOnOutOfMemoryError", serverProfile.getJarFileName()
		));

		command.addAll(LOGGING_ARGUMENTS);

		return command;
	}

	@Override
	protected void innerStopServer() {
		if (runMode.startAutomatically && !TAKCLCore.useRunningServer) {
			if (messagingProcess != null || apiProcess != null) {
				try {
					if (messagingProcess != null) {
						messagingProcess.destroy();
					}
					if (apiProcess != null) {
						apiProcess.destroy();
					}
					if (messagingProcess != null) {
						messagingProcess.waitFor();
					}
					if (apiProcess != null) {
						apiProcess.waitFor();
					}
					System.out.println("Server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' stopped successfully.");
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

		} else {
			Console console = System.console();
			if (console == null) {
				throw new RuntimeException("Manual control mode enabled but console is not available. This is sometimes due to running in an IDE where the output window is not an interactive console.");
			} else {
				console.readLine("Please stop the TAK server located at '" + serverIdentifier.getJarFileName() + "' and press Enter.");
			}
			isManualServerControlOverrideServerRunning = false;
		}
	}

	@Override
	protected void innerConfigureServer(@Nullable String sessionIdentifier, boolean enableRemoteDebug) {
		// Removing default inputs from servers other than SERVER_0 since they will cause bind conflicts
		if (!serverIdentifier.getConsistentUniqueReadableIdentifier().equals(ImmutableServerProfiles.SERVER_0.getConsistentUniqueReadableIdentifier())) {
			List<Input> inputList = new LinkedList<>(this.getOfflineConfigModule().getInputs());
			for (Input input : inputList) {
				if ((input.getPort() == 8088 && input.getName().equals("streamtcp")) ||
						(input.getPort() == 8087 && (input.getName().equals("stdudp") || input.getName().equals("stdtcp")))) {
					this.getOfflineConfigModule().removeInput(input.getName());
				}
			}
		} else {
			for (Input input : this.getOfflineConfigModule().getInputs()) {
				Integer networkVersion = ProtocolProfiles.getInputByValue(input.getProtocol()).getCoreNetworkVersion();
				if (networkVersion != null) {
					input.setCoreVersion(networkVersion);
				}
			}
		}

		this.getOfflineConfigModule().setCertHttpsPort(serverIdentifier.getCertHttpsPort());
		this.getOfflineConfigModule().setFedHttpsPort(serverIdentifier.getFedHttpsPort());
		this.getOfflineConfigModule().sethttpPlaintextPort(serverIdentifier.getHttpPlaintextPort());
		this.getOfflineConfigModule().setHttpsPort(serverIdentifier.getHttpsPort());
		this.getOfflineConfigModule().setIgnitePortRange(serverIdentifier.getIgniteDiscoveryPort(), serverIdentifier.getIgniteDiscoveryPortCount());
	}

	@Override
	protected void innerDeployServer(@Nullable String sessionIdentifier, long startTimeMs, boolean enableRemoteDebug) {
		String serverPath = serverIdentifier.getServerPath();

		Map<String, String> additionalEnvironmentVariables = new HashMap<>();

		List<String> profiles;
		if (TAKCLCore.useMonolithProfile) {
			profiles = new ArrayList<>(1);
			profiles.add("monolith");
			System.out.println("Using Monolith Server Configuration.");
		} else {
			if (TAKCLCore.disableApiProcess) {
				profiles = Arrays.asList("messaging");
			} else {
				profiles = Arrays.asList("messaging", "api");
			}
		}

		for (String profile : profiles) {
			List<String> command = buildExecCommand(serverIdentifier, enableRemoteDebug, profile);
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(new File(serverPath));
			initFilepaths(sessionIdentifier, startTimeMs, profile, processBuilder);

			additionalEnvironmentVariables.put("IGNITE_HOME", serverPath);
			processBuilder.environment().putAll(additionalEnvironmentVariables);

			List<String> envVarLines = new LinkedList<>();
			for (Map.Entry<String, String> entry : additionalEnvironmentVariables.entrySet()) {
				envVarLines.add(entry.getKey() + "='" + entry.getValue() + "'");
			}

			try {
				if (runMode.startAutomatically) {
					System.out.println(
							"Starting TAKServer in directory\n\t'" + processBuilder.directory().getAbsolutePath() +
									"'\nWith command: [\n\t" + String.join("  \\\n\t", command) + "\n]" +
									"\nWith additional environment variables [\n\t" + String.join("\n\t,", envVarLines) + "\n]"
					);

					if (!TAKCLCore.useRunningServer) {
						if (profile.equals("messaging")) {
							messagingProcess = processBuilder.start();
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}

						} else if (profile.equals("api")) {
							apiProcess = processBuilder.start();

						} else if (profile.equals("monolith")) {
							messagingProcess = processBuilder.start();

						} else {
							throw new RuntimeException("Unexpected Profile '" + profile + "'!");
						}
					}
				} else {
					Console console = System.console();
					if (console == null) {
						throw new RuntimeException("Manual control mode enabled but console is not available. This is sometimes due to running in an IDE where the output window is not an interactive console.");
					} else {
						console.readLine(
								"Please start TAKServer in directory\n\t'" + processBuilder.directory().getAbsolutePath() +
										"'\nWith command: [\n\t" + String.join("  \\\n\t", command) + "\n]" +
										"\nWith additional environment variables [\n\t" + String.join("\n\t,", envVarLines) + "\n]" +
										"\n and press Enter."
						);
					}
				}
				System.out.println("Server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' " + profile + " component started successfully.");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void initFilepaths(String sessionIdentifier, long startTimeMs, String profile, ProcessBuilder processBuilder) {
		try {
			logPath = Paths.get(logDirectory).toAbsolutePath().resolve(sessionIdentifier);
			if (!Files.exists(logPath)) {
				Files.createDirectories(logPath);
			}

			String serverName = serverIdentifier.getConsistentUniqueReadableIdentifier();

			if (profile.equals("monolith") || profile.equals("messaging")) {
				coreConfigFile = logPath.resolve(serverName + "-CoreConfig.xml").toFile();
				Files.copy(Paths.get(serverIdentifier.getConfigFilePath()), coreConfigFile.toPath());
			}

			if (profile.equals("monolith") || profile.equals("messaging") || profile.equals("api")) {
				File errorFile = logPath.resolve(serverName + "-" + profile + ".stderr.txt").toFile();
				File outputFile = logPath.resolve(serverName + "-" + profile + ".stdout.txt").toFile();
				processBuilder.redirectError(errorFile);
				processBuilder.redirectOutput(outputFile);

			} else if (profile.equals("psaux")) {
				profile = profile + "-" + System.currentTimeMillis();
				File errorFile = logPath.resolve(profile + ".stderr.txt").toFile();
				File outputFile = logPath.resolve(profile + ".stdout.txt").toFile();
				processBuilder.redirectError(errorFile);
				processBuilder.redirectOutput(outputFile);

			} else {
				throw new RuntimeException("Unexpected profile '" + profile + "'!");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void innerKillServer() {
		try {
			if (messagingProcess != null) {
				messagingProcess.destroyForcibly();
			}
		} catch (Exception e) {
			// Pass, let's try and kill the others too
		}
		try {
			if (apiProcess != null) {
				apiProcess.destroyForcibly();
			}
		} catch (Exception e) {
			// Pass, let's try and kill the others too
		}
	}

	private void checkServerState(boolean shouldBeOnline) {

		boolean serverState = runMode.startAutomatically ? isRunning() : isManualServerControlOverrideServerRunning;

		if (shouldBeOnline != serverState) {
			String callingMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
			throw new RuntimeException("Cannot call " + callingMethod + " on server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' while it is " +
					(serverState ? "online!" : "offline!"));
		}
	}
}
