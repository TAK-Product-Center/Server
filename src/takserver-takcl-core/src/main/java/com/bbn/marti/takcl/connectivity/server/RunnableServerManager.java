package com.bbn.marti.takcl.connectivity.server;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
import com.bbn.marti.tests.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.ignite.Ignition;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RunnableServerManager {

	private static RunnableServerManager instance;

	private final Map<String, AbstractRunnableServer> serverMap = new HashMap<>();

	private final Timer watchdog;

	private RunnableServerManager() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			for (AbstractRunnableServer server : serverMap.values()) {
				try {
					server.killServer();
				} catch (Exception e) {
					// Pass, let's try and kill the others too
				}
			}
		}));

		TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				for (AbstractRunnableServer server : serverMap.values()) {
					try {
						server.watchdogPoll();
					} catch (Exception e) {
						String msg = "The server " + server.serverIdentifier + " Appears to have stoppped running!";
						System.err.println(msg);
						destroyAllServers(30000);
						System.exit(1);
						Assert.fail(msg);
					}
				}
			}
		};

		watchdog = new Timer(true);
		watchdog.scheduleAtFixedRate(tt, 0, 30000);
	}

	public synchronized static RunnableServerManager getInstance() {
		if (instance == null) {
			instance = new RunnableServerManager();
		}
		return instance;
	}


	public synchronized AbstractRunnableServer getServerInstance(AbstractServerProfile serverIdentifier) {
		AbstractRunnableServer server = serverMap.get(serverIdentifier.getConsistentUniqueReadableIdentifier());

		if (server == null) {
			throw new RuntimeException("The server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' has not been initialized!");
		} else {
			return server;
		}
	}

	public synchronized boolean serverInstanceExists(AbstractServerProfile serverIdentifier) {
		return (serverMap.get(serverIdentifier.getConsistentUniqueReadableIdentifier()) != null);
	}

	/**
	 * Builds a new instance of the server by copying the predefined server directory files to a temporary directory while this session is running
	 *
	 * @return A server instance
	 */
	public synchronized AbstractRunnableServer buildServerInstance(AbstractServerProfile serverIdentifier) {
		if (serverMap.containsKey(serverIdentifier.getConsistentUniqueReadableIdentifier())) {
			throw new RuntimeException("Server identifier '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' is already in use!");
		}

		cloneFromModelServer(serverIdentifier);

		AbstractRunnableServer newServer;

		if (TAKCLCore.k8sMode) {
			if (serverIdentifier != ImmutableServerProfiles.SERVER_0 &&
					((serverIdentifier instanceof MutableServerProfile &&
							((MutableServerProfile) serverIdentifier).baseProfile != ImmutableServerProfiles.SERVER_0))) {
				throw new RuntimeException("Currently only single-server deployments are supported in cluster mode!");
			}
			newServer = new KubernetesRunnableCluster(serverIdentifier);

		} else {
			newServer = new LocalRunnableServer(serverIdentifier);
		}

		serverMap.put(serverIdentifier.getConsistentUniqueReadableIdentifier(), newServer);

		return newServer;
	}

	public void destroyAllServers(long serverKillDelayMS) {
		for (AbstractRunnableServer server : serverMap.values()) {
			if (server.isRunning()) {
				server.stopServer(serverKillDelayMS);
			}
			server.offlineFactoryResetServer();
			server.killServer();
		}
		serverMap.clear();

		if (!TestExceptions.DO_NOT_CLOSE_IGNITE_INSTANCES) {
			Ignition.stopAll(true);
		}
	}

	public static void cloneFromModelServer(AbstractServerProfile newServerIdentifier) {
		try {
			TAKCLConfigModule conf = TAKCLConfigModule.getInstance();

			Path modelPath = Paths.get(conf.getTakServerPath()).toAbsolutePath();

			Path farmPath = Paths.get(conf.getServerFarmDir()).toAbsolutePath();

			if (!Files.exists(farmPath)) {
				Files.createDirectory(farmPath);
			}

			String serverRootDir = newServerIdentifier.getServerPath();

			if (Files.exists(Paths.get(serverRootDir))) {
				FileUtils.cleanDirectory(new File(serverRootDir));
			}

			Path targetPath = Paths.get(serverRootDir);

			FileUtils.copyDirectory(modelPath.toFile(), targetPath.toFile());

			String[] conflictingFiles = new String[]{"CoreConfig.xml", "certs/files", "TEST_RESULTS"};

			for (String file : conflictingFiles) {
				Path targetFile = targetPath.resolve(file);
				if (Files.exists(targetFile)) {
					if (Files.isDirectory(targetFile)) {
						FileUtils.deleteDirectory(targetFile.toFile());
					} else {
						Files.delete(targetFile);
					}
				}
			}

			File certTargetDir = new File(serverRootDir + "/certs/files");
			FileUtils.forceMkdir(certTargetDir);
			Files.copy(conf.getCoreConfigExamplePath(), Paths.get(serverRootDir, "CoreConfig.xml"));
			Files.copy(Paths.get(TAKCLConfigModule.getInstance().getCertificateFilepath(newServerIdentifier.getConsistentUniqueReadableIdentifier(), "jks")), Paths.get(serverRootDir, "certs/files/takserver.jks"));
			Files.copy(Paths.get(TAKCLConfigModule.getInstance().getTruststoreJKSFilepath()), Paths.get(serverRootDir, "certs/files/truststore-root.jks"));
			Files.copy(Paths.get(TAKCLConfigModule.getInstance().getTruststoreJKSFilepath()), Paths.get(serverRootDir, "certs/files/fed-truststore.jks"));

		} catch (FileAlreadyExistsException e) {
			System.err.println("A file expected to not exist already exists! Did you forget to remove an existing TEST_RESULTS directory prior to test execution?");
			throw new RuntimeException(e);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
