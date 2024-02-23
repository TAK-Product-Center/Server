package com.bbn.marti.takcl.connectivity.server;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class KubernetesRunnableCluster extends AbstractRunnableServer {

	private final KubernetesHelper kh;

	public class KubernetesRunnableProcess extends AbstractServerProcess {

		public KubernetesRunnableProcess(ServerProcessDefinition definition) {
			super(definition);
		}

		@Override
		public boolean isRunning() {
			try {
				return !kh.getPods(this.getDefinition()).isEmpty();
			} catch (ApiException e) {
				logger.error(e.getResponseBody());
				throw new RuntimeException(e);
			}
		}

		@Override
		public List<String> waitForMissingLogStatements(int maxWaitTimeMs) {
			List<String> result = kh.waitForReadyReplicasByExec(definition, maxWaitTimeMs);
			if (definition == ServerProcessDefinition.MessagingService) {
				try {
					List<V1Pod> pods = kh.getPods(definition);
					serverIdentifier.setUrl(pods.get(0).getStatus().getPodIP());
				} catch (ApiException e) {
					logger.error(e.getResponseBody());
					throw new RuntimeException(e);
				}
			}
			return result;
		}

		@Override
		public void start(boolean enableRemoteDebug) {
			throw new RuntimeException("The startup of all cluster processes should be handled atomically!");
		}

		@Override
		public void stop() {
			throw new RuntimeException("The stopping of all cluster processes should be handled atomically!");
		}

		@Override
		public void kill() {
			try {
				kh.terminateProcessPods(this.definition);
			} catch (ApiException e) {
				logger.error(e.getResponseBody());
				throw new RuntimeException(e);
			}
		}
	}

	public KubernetesRunnableCluster(AbstractServerProfile serverIdentifier) {
		super(serverIdentifier);
		kh = new KubernetesHelper();
		// This could be done cleanly. But the less "clean-slate" of K8s makes this necessary
		killServer();
	}

	@Override
	protected void innerStopServer() {

	}

	public void restartServerProcesses(List<ServerProcessDefinition> processDefinitions) {
		try {

			// Terminate server processes
			for (ServerProcessDefinition processDefinition : processDefinitions) {
				kh.terminateProcessPods(processDefinition);
			}

			// Wait for them all to shut down
			kh.waitForProcessShutdown(processDefinitions);


			// Redeploy with updated configurations
			for (ServerProcessDefinition processDefinition : processDefinitions) {
				kh.rolloutServerProcess(processDefinition);
			}

			// Restart ignite. Leaving it running resulted in issues
			kh.restartIgnite();

			try {
				Thread.sleep(16000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			// Start the processes back up
			for (ServerProcessDefinition processDefinition : processDefinitions) {
				kh.startProcessPod(processDefinition);
			}

		} catch (ApiException ae) {
			logger.error(ae.getResponseBody());
			throw new RuntimeException(ae);
		}
	}


	@Override
	protected void innerDeployServer(@NotNull String sessionIdentifier, boolean enableRemoteDebug) {
		try {
			logPath = Paths.get(logDirectory).toAbsolutePath().resolve(sessionIdentifier);
			if (!Files.exists(logPath)) {
				Files.createDirectories(logPath);
			}

			Path certPath = TAKCLConfigModule.getInstance().getCertificateDir();
			FileUtils.copyFile(certPath.resolve("SERVER_0.jks").toFile(), certPath.resolve("takserver.jks").toFile());
			FileUtils.copyFile(certPath.resolve("truststore-root.jks").toFile(), certPath.resolve("truststore.jks").toFile());

			kh.updateConfigmap("core-config", Paths.get(serverIdentifier.getConfigFilePath()));
			kh.updateConfigmap("tak-ignite-config", Paths.get(serverIdentifier.getTAKIgniteConfigFilePath()));
			kh.updateConfigmap("cert-migration", TAKCLConfigModule.getInstance().getCertificateDir());
			kh.updateConfigmap("readiness-config", Paths.get("/clustertestrunner.py"));

			restartServerProcesses(getEnabledServerProcesses().stream().map(AbstractServerProcess::getDefinition)
					.collect(Collectors.toList()));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void innerKillServer() {
		processes.parallelStream().filter(AbstractServerProcess::isEnabled).forEach(AbstractServerProcess::kill);
	}

	@Override
	public List<AbstractServerProcess> getEnabledServerProcesses() {
		return processes.stream().filter(AbstractServerProcess::isEnabled).collect(Collectors.toList());
	}

	@Override
	protected void collectFinalLogs() {
		try {
			for (AbstractServerProcess process : getEnabledServerProcesses()) {
				kh.copyProcessLog(process.definition, logPath);
			}
		} catch (ApiException e) {
			logger.error(e.getResponseBody());
			throw new RuntimeException(e);
		} catch (IOException e) {
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	protected List<? extends AbstractServerProcess> createProcessContainerList() {
		ServerProcessDefinition[] definitions = ServerProcessDefinition.values();
		ArrayList<KubernetesRunnableProcess> containers = new ArrayList<>(definitions.length);
		for (ServerProcessDefinition definition : ServerProcessDefinition.values()) {
			containers.add(new KubernetesRunnableProcess(definition));
		}
		return Collections.unmodifiableList(containers);
	}
}
