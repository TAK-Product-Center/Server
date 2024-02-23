package com.bbn.marti.takcl.connectivity.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.collect.ImmutableSet;
import io.kubernetes.client.Copy;
import io.kubernetes.client.Exec;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Streams;
import io.kubernetes.client.util.exception.CopyNotSupportedException;
import io.kubernetes.client.util.wait.Wait;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KubernetesHelper {

	public static final ImmutableSet<String> knownBinaryExtensions = ImmutableSet.of(".jks", ".p12");
	private static final String namespace = "takserver";

	private final ApiClient client;
	private final CoreV1Api api;
	private final AppsV1Api appsApi;

	private final Copy copy;

	// TODO: There must be a better way to do this. But ther "terminating" status didn't seem to be in the container data
	private final Set<String> staleContainerIdentifiers = new HashSet<>();

	public KubernetesHelper() {
		try {
			System.setProperty("jdk.tls.client.protocols", "TLSv1.2");

			client = ClientBuilder.defaultClient();

			Configuration.setDefaultApiClient(client);
			api = new CoreV1Api(client);
			appsApi = new AppsV1Api(client);
			copy = new Copy(client);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public List<V1Pod> getPodsWithAppName(String appName) throws ApiException {
		V1PodList result = api.listNamespacedPod(namespace, null, null, null, null, "app=" + appName,
				null, null, null, null, null);
		return result.getItems();
	}

	public List<V1Pod> getPods(ServerProcessDefinition process) throws ApiException {
		return getPodsWithAppName(namespace + "-" + process.identifier);
	}

	public String getPodLog(ServerProcessDefinition process) {
		try {
			return api.readNamespacedPodLog("takserver-messaging-548978449d-7grs4", "takserver", null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateConfigmap(String configMapName, Path localPath) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.getLogger("io.kubernetes.client").setLevel(Level.TRACE);
		loggerContext.getLogger("okhttp3").setLevel(Level.TRACE);

		try {
			V1ConfigMap map = new V1ConfigMap();
			if (Files.isDirectory(localPath)) {
				List<Path> files = Files.list(localPath).collect(Collectors.toList());

				for (Path filePath : files) {
					if (Files.isDirectory(filePath)) {
						throw new RuntimeException("The test harness does not currently support nested files in configmaps!");
					}
					try {
						String filename = filePath.getFileName().toString();
						String extension = filename.substring(filename.lastIndexOf('.'), filename.length());
						if (knownBinaryExtensions.contains(extension)) {
							map.putBinaryDataItem(filename, Files.readAllBytes(filePath));
						} else {
							map.putDataItem(filename, Files.readString(filePath));
						}

					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

			} else {
				map.putDataItem(localPath.getFileName().toString(), Files.readString(localPath));
			}

			V1ObjectMeta metadata = new V1ObjectMeta();
			metadata.setName(configMapName);
			metadata.setNamespace(namespace);
			map.setMetadata(metadata);
			api.replaceNamespacedConfigMap(configMapName, namespace, map, null, null, null, null);

		} catch (ApiException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void copyProcessLog(ServerProcessDefinition processDefinition, Path targetDirectory) throws ApiException, IOException {
		List<V1Pod> pods = getPods(processDefinition);
		for (V1Pod pod : pods) {
			try {
				copy.copyDirectoryFromPod(pod, "/logs", targetDirectory.resolve(pod.getMetadata().getName()));
			} catch (CopyNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void killIgnite() throws ApiException, InterruptedException {
		// Terminate Ignite
		PatchUtils.patch(V1StatefulSet.class, () -> appsApi.patchNamespacedStatefulSetCall(namespace + "-ignite",
						namespace, new V1Patch("{\"spec\":{\"replicas\":0}}"), null, null, "kubectl-scale", null, null, null),
				V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH, client);

		List<V1Pod> result = getPodsWithAppName(namespace + "-ignite");

		// Wait for shutdown
		while (result.size() > 0) {
			Thread.sleep(4000);
			result = getPodsWithAppName(namespace + "-ignite");
		}
	}

	public void restartIgnite() throws ApiException {
		try {
			killIgnite();

			// Start it back up
			PatchUtils.patch(V1StatefulSet.class, () -> appsApi.patchNamespacedStatefulSetCall(namespace + "-ignite",
							namespace, new V1Patch("{\"spec\":{\"replicas\":1}}"), null, null, "kubectl-scale", null, null, null),
					V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH, client);

			List<V1Pod> result = getPodsWithAppName(namespace + "-ignite");

			// Wait for startup
			while (result.size() == 0) {
				Thread.sleep(4000);
				result = getPodsWithAppName(namespace + "-ignite");
			}

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		System.out.println("Ignite Has Restarted");
	}

	private void terminatePods(String appName) throws ApiException {
		List<V1Pod> currentPods = getPodsWithAppName(appName);
		staleContainerIdentifiers.addAll(currentPods.stream().map(x -> x.getMetadata().getName()).collect(Collectors.toList()));

		PatchUtils.patch(V1Deployment.class,
				() -> appsApi.patchNamespacedDeploymentCall(appName, namespace, new V1Patch("{\"spec\":{\"replicas\":0}}"), null, null,
						"kubectl-scale", null, null, null), V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH, client);
	}

	public void terminateProcessPods(ServerProcessDefinition processDefinition) throws ApiException {
		terminatePods(namespace + "-" + processDefinition.identifier);
	}

	public void waitForProcessShutdown(List<ServerProcessDefinition> processes) throws ApiException {
		List<ServerProcessDefinition> runningPods = new ArrayList<>(processes);
		int timeout = 120000;
		int interval = 4000;

		while (runningPods.size() > 0 && timeout > 0) {
			for (ServerProcessDefinition process : new ArrayList<>(runningPods)) {

				if (getPodsWithAppName(namespace + "-" + process.identifier).size() == 0) {
					System.out.println("Process " + process.identifier + " is not running.");
					runningPods.remove(process);
				}
			}
			if (runningPods.size() == 0) {
				return;
			}

			try {
				Thread.sleep(interval);
				timeout = timeout - interval;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void startPod(String appName) throws ApiException {
		PatchUtils.patch(V1Deployment.class, () -> appsApi.patchNamespacedDeploymentCall(appName, namespace,
						new V1Patch("{\"spec\":{\"replicas\":1}}"), null, null, "kubectl-scale", null, null, null),
				V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH, client);
	}

	public void startProcessPod(ServerProcessDefinition processDefinition) throws ApiException {
		startPod(namespace + "-" + processDefinition.identifier);
	}

	public void rolloutServerProcess(ServerProcessDefinition processDefinition) throws ApiException {
		String deploymentName = namespace + "-" + processDefinition.identifier;
		V1Deployment runningDeployment = appsApi.readNamespacedDeployment(deploymentName, namespace, null);

		runningDeployment.getSpec().getTemplate().getMetadata()
				.putAnnotationsItem("kubectl.kubernetes.io/restartedAt", LocalDateTime.now().toString());

		String deploymentJson = client.getJSON().serialize(runningDeployment);

		PatchUtils.patch(
				V1Deployment.class,
				() ->
						appsApi.patchNamespacedDeploymentCall(
								deploymentName,
								namespace,
								new V1Patch(deploymentJson),
								null,
								null,
								"kubectl-rollout",
								null,
								null,
								null),
				V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH,
				client);
	}

	public List<String> waitForReadyReplicasByExec(ServerProcessDefinition processDefinition, int maxWaitSeconds) {
		try {
			// TODO: Should we wait for all pods of a given type to finish starting?

			List<V1Pod> pods = getPods(processDefinition);

			while (pods.size() == 0) {
				Thread.sleep(4000);
				pods = getPods(processDefinition);
			}
			System.out.println("Pod found for " + processDefinition.identifier + ".");

			V1Pod pod = pods.stream().filter(x ->
					!staleContainerIdentifiers.contains(x.getMetadata().getName())).collect(Collectors.toList()).get(0);

			String[] cmd = new String[]{"python", "/clustertestrunner.py", "readiness", processDefinition.identifier, "0"};
			Exec exec = new Exec();
			final List<String> result = new LinkedList<>();

			Wait.poll(
					Duration.ofSeconds(3),
					Duration.ofMillis(maxWaitSeconds),
					() -> {
						try {
							Process proc = exec.exec(pod, cmd, false);
							ByteArrayOutputStream baos = new ByteArrayOutputStream();

							Thread out = new Thread(
									() -> {
										try {
											Streams.copy(proc.getInputStream(), baos);
										} catch (IOException e) {
											e.printStackTrace(System.err);
										}
									}
							);

							out.start();
							proc.waitFor();
							out.join();
							proc.destroy();

							String output = baos.toString();

							result.clear();
							result.addAll(Arrays.asList(output.split("\n")));
							return output.length() <= 0;

						} catch (IOException | ApiException | InterruptedException e) {
							e.printStackTrace(System.err);
							return false;
						}
					}
			);
			return result;
		} catch (ApiException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
