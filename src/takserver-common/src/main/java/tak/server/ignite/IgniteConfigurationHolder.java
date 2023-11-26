package tak.server.ignite;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.DataPageEvictionMode;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.failure.NoOpFailureHandler;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.Constants;

public class IgniteConfigurationHolder {

	private static IgniteConfigurationHolder instance = null;

	private static final Logger logger = LoggerFactory.getLogger(IgniteConfigurationHolder.class);

	public static IgniteConfigurationHolder getInstance() {
		if (instance == null) {
			synchronized (IgniteConfigurationHolder.class) {
				if (instance == null) {
					instance = new IgniteConfigurationHolder();
				}
			}
		}

		return instance;
	}

	private IgniteConfiguration configuration;

	public IgniteConfiguration getConfiguration() {

		if (configuration == null) {
			logger.warn("ignite configuration not populated yet");
		}
	
		return configuration;
	}

	public void setConfiguration(IgniteConfiguration configuration) {
		this.configuration = configuration;
	}
	
	public IgniteConfiguration getIgniteConfiguration(String igniteProfile, String igniteHost, boolean isCluster, boolean isKubernetes, boolean isEmbedded, boolean isMulticastDiscovery, @Nullable Integer nonMulticastDiscoveryPort,
            @Nullable Integer nonMulticastDiscoveryPortCount, Integer communicationPort, Integer communicationPortCount, int maxQueue, long workerTimeoutMilliseconds, long dataRegionInitialSize, long dataRegionMaxSize) {
		return getIgniteConfiguration(igniteProfile, igniteHost, isCluster, isKubernetes, isEmbedded, isMulticastDiscovery, nonMulticastDiscoveryPort, nonMulticastDiscoveryPortCount, communicationPort, communicationPortCount, 
				maxQueue, workerTimeoutMilliseconds, dataRegionInitialSize, dataRegionMaxSize, -1, false, -1.f, false, false, -1, false, -1, -1, -1);
	}

	public IgniteConfiguration getIgniteConfiguration(String igniteProfile, String igniteHost, boolean isCluster, boolean isKubernetes, boolean isEmbedded, boolean isMulticastDiscovery, @Nullable Integer nonMulticastDiscoveryPort,
	                                                  @Nullable Integer nonMulticastDiscoveryPortCount, Integer communicationPort, Integer communicationPortCount, int maxQueue, long workerTimeoutMilliseconds, long dataRegionInitialSize, long dataRegionMaxSize, int poolSize, boolean enablePersistence, double evictionThreashold,
	                                                  boolean ignitePoolSizeUseDefaultsForApi, boolean igniteDefaultSpiConnectionsPerNode, int igniteExplicitSpiConnectionsPerNode, boolean apiServiceIgniteServer,
	                                                  long spiConnectionTimeoutMs, long clientConnectionTimeoutMs, long failureDetectionTimeoutMs) {
		
		if (isCluster) {
			IgniteConfiguration clusterConf = new IgniteConfiguration();

			TcpDiscoverySpi tds = new TcpDiscoverySpi();

			if (isKubernetes) {

				TcpDiscoveryKubernetesIpFinder ipFinder = new TcpDiscoveryKubernetesIpFinder();

				//			metadata:
				//				  # The name must be equal to TcpDiscoveryKubernetesIpFinder.serviceName
				//				  name: ignite
				//				  # The name must be equal to TcpDiscoveryKubernetesIpFinder.namespaceName
				//				  namespace: ignite
				//				spec:

				ipFinder.setServiceName("takserver-ignite");
				ipFinder.setNamespace("takserver");

				// default is
				//     /** Kubernetes API server URL in a string form. */
				// private String master = "https://kubernetes.default.svc.cluster.local:443";

				// what it is using:
				//https://kubernetes.default.svc.cluster.local:443
				// did not help
				//ipFinder.setMasterUrl("https://kubernetes.ignite.svc.cluster.local:443");

				tds.setIpFinder(ipFinder);
			}

			clusterConf.setDiscoverySpi(tds);
			clusterConf.setClientMode(true);
			clusterConf.setUserAttributes(Collections.singletonMap(Constants.TAK_PROFILE_KEY, igniteProfile));
			
			return clusterConf;

		} else {

			@SuppressWarnings("unused")
			boolean isMessagingProfile = igniteProfile.equals(Constants.MESSAGING_PROFILE_NAME);

			IgniteConfiguration standaloneConf = new IgniteConfiguration();

			String defaultWorkDir = "/opt/tak";
			try {
				 defaultWorkDir = U.defaultWorkDirectory();
			} catch (IgniteCheckedException e) {
				logger.error(" error getting Ignite work dir, default to /opt/tak ", e);
			}

			standaloneConf.setWorkDirectory(defaultWorkDir + "/" + igniteProfile + "-tmp-work");

			TcpDiscoverySpi spi = new TcpDiscoverySpi();
			
			if (!isMulticastDiscovery) {

				TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

				String address = igniteHost + ":" + nonMulticastDiscoveryPort + ".." + (nonMulticastDiscoveryPort + nonMulticastDiscoveryPortCount); 
				ipFinder.setAddresses(Arrays.asList(address));
				
				logger.trace("ignite grid discovery address: {}", address);

				spi.setIpFinder(ipFinder);

				if (nonMulticastDiscoveryPort != null) {
					spi.setLocalPort(nonMulticastDiscoveryPort);
				}
				if (nonMulticastDiscoveryPortCount != null) {
					spi.setLocalPortRange(nonMulticastDiscoveryPortCount);
				}
			}
			
			standaloneConf.setDiscoverySpi(spi);
			TcpCommunicationSpi tcpSpiConf = new TcpCommunicationSpi();
			tcpSpiConf.setLocalPort(communicationPort);
			tcpSpiConf.setLocalPortRange(communicationPortCount);
			tcpSpiConf.setLocalAddress(igniteHost);
			tcpSpiConf.setMessageQueueLimit(maxQueue);
			
			if (spiConnectionTimeoutMs > -1) {
				tcpSpiConf.setConnectTimeout(spiConnectionTimeoutMs); // milliseconds
			}

            standaloneConf.setLocalHost(igniteHost);

            if (igniteDefaultSpiConnectionsPerNode) {
				// use default
			} else if (igniteExplicitSpiConnectionsPerNode < 1) {
				// autodetect to num CPU cores
				tcpSpiConf.setConnectionsPerNode(Runtime.getRuntime().availableProcessors());
			} else {
				tcpSpiConf.setConnectionsPerNode(igniteExplicitSpiConnectionsPerNode);
			}
			
			// Using System.out due to log init race
			System.out.println("Ignite SPI connections per node: " + tcpSpiConf.getConnectionsPerNode());
			
			standaloneConf.setCommunicationSpi(tcpSpiConf);
			
			// If this process is the API micro-service, optionally run in ignite embedded server mode. In standalone, messaging service is always an ignite server. Plugin manager and retention service are always ignite clients. 
			if (apiServiceIgniteServer) {
				standaloneConf.setClientMode(false);
			} else {
				standaloneConf.setClientMode(!isEmbedded);
			}
			standaloneConf.setIgniteInstanceName(Constants.IGNITE_INSTANCE_NAME);

			standaloneConf.setUserAttributes(Collections.singletonMap(Constants.TAK_PROFILE_KEY, igniteProfile));
			
			standaloneConf.setFailureHandler(new NoOpFailureHandler());
			
			if (clientConnectionTimeoutMs > -1) {
				standaloneConf.setClientFailureDetectionTimeout(clientConnectionTimeoutMs);
			}
			
			if (failureDetectionTimeoutMs > -1) {
				standaloneConf.setFailureDetectionTimeout(failureDetectionTimeoutMs);
				standaloneConf.setSystemWorkerBlockedTimeout(failureDetectionTimeoutMs);
			}
			
			if (isEmbedded) {

				DataStorageConfiguration storageConfig = new DataStorageConfiguration();

				DataRegionConfiguration takserverStorageRegion = new DataRegionConfiguration();
				takserverStorageRegion.setName("takserver-cache-region");
				takserverStorageRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU); // cache eviction policy

				if (evictionThreashold != -1.f) {
					takserverStorageRegion.setEvictionThreshold(evictionThreashold);
				}
				
				takserverStorageRegion.setInitialSize(dataRegionInitialSize);
				takserverStorageRegion.setMaxSize(dataRegionMaxSize);
				
				if (enablePersistence) {
					takserverStorageRegion.setPersistenceEnabled(true);
				}
			
				storageConfig.setDefaultDataRegionConfiguration(takserverStorageRegion);
				
				if (enablePersistence) {
					
					String basePath = Paths.get("").toAbsolutePath().toString();
					
					FileSystem fs = FileSystems.getDefault();
					
					storageConfig.setStoragePath(basePath + fs.getSeparator() + "tmp" + fs.getSeparator() + "cache-" +  igniteProfile);
				}

				standaloneConf.setDataStorageConfiguration(storageConfig);
			} else { // client mode - API process
				ClientConnectorConfiguration ccc = standaloneConf.getClientConnectorConfiguration();

				if (poolSize > ccc.getThreadPoolSize()) {
					ccc.setThreadPoolSize(poolSize);
				}
			}

			if (!isEmbedded && !ignitePoolSizeUseDefaultsForApi) {
				if (poolSize > 0) {
					// ignite thread pools
					standaloneConf.setSystemThreadPoolSize(poolSize + 1);
					standaloneConf.setPublicThreadPoolSize(poolSize);
					standaloneConf.setQueryThreadPoolSize(poolSize);
					standaloneConf.setServiceThreadPoolSize(poolSize);
					standaloneConf.setStripedPoolSize(poolSize);
					standaloneConf.setDataStreamerThreadPoolSize(poolSize);
					standaloneConf.setRebalanceThreadPoolSize(poolSize);
				}
			}

			IgniteLogger log = new Slf4jLogger();

			standaloneConf.setGridLogger(log);

			return standaloneConf;
		}
	}
}
