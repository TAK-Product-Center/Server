package tak.server.ignite;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

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

import com.bbn.marti.config.TAKIgniteConfiguration;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;

import tak.server.Constants;
import tak.server.util.ActiveProfiles;
import tak.server.util.JAXBUtils;

import jakarta.xml.bind.JAXBException;

public class IgniteConfigurationHolder {

	private static IgniteConfigurationHolder instance = null;

	private static final Logger logger = LoggerFactory.getLogger(IgniteConfigurationHolder.class);

	public String CONFIG_FILE = null;
	private final String DEFAULT_CONFIG_FILE = "TAKIgniteConfig.xml";
	private final String EXAMPLE_BASE_CONFIG_FILE = "TAKIgniteConfig.example.xml";
	private final int IGNITE_POOL_SIZE_LIMIT = 1024;


	// our TAK specific Ignite configuration
	private TAKIgniteConfiguration takIgniteConfiguration;
	// Ignite's configuration
	private IgniteConfiguration configuration;

	private IgniteConfigurationHolder() {
		// Load TAKIgniteConfig.xml from file. This always needs to be done first - even
		// when using the cluster. We need to look at the config file to figure if
		// clustering is enabled, and
		// whether we are using Kubernetes. This info is important for ignite
		// (distributed cache) initialization.
		TAKIgniteConfiguration conf = null;
		try {
			CONFIG_FILE = DEFAULT_CONFIG_FILE;
			Path configPath = Paths.get(CONFIG_FILE);

			if (!Files.exists(configPath)) {
			   if (!Files.exists(Paths.get(EXAMPLE_BASE_CONFIG_FILE))) {
				   throw new NotFoundException("The TAKIgniteConfiguration file '" + CONFIG_FILE + "' and the example file '" +
						  EXAMPLE_BASE_CONFIG_FILE + "' do not exist!");
			   } else {
				   Files.copy(Paths.get(EXAMPLE_BASE_CONFIG_FILE), configPath);
				   configPath.toFile().setWritable(true);
			   }
			}

			conf = JAXBUtils.loadJAXifiedXML(CONFIG_FILE, TAKIgniteConfiguration.class.getPackage().getName());

		} catch (NotFoundException | NoSuchFileException nfe) {
			logger.debug("Config file does not exist");
		} catch (Exception e) {
			logger.error("Exception parsing config", e);
			throw new TakException(e);
		}

		// for unit tests - this was copied from LocalConfiguration but we shouldn't have test code in production code.
		// TODO: Use a mock or better approach
		if (conf == null) {
			conf = new TAKIgniteConfiguration();
		}

		this.takIgniteConfiguration = conf;

	}

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

	public void setIgniteConfiguration(IgniteConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * This is the underlying XML configuration used to configure the IgniteConfiguration initially from the file
	 * @return
	 */
	public TAKIgniteConfiguration getTAKIgniteConfiguration()
	{
		return takIgniteConfiguration;
	}

	/**
	 * This is the underlying XML configuration used to configure the IgniteConfiguration initially from the file
	 * @return
	 */
	public void setTAKIgniteConfiguration(TAKIgniteConfiguration takIgniteConfiguration)
	{
		this.takIgniteConfiguration = takIgniteConfiguration;
	}

	public IgniteConfiguration getIgniteConfiguration()
	{
		if (configuration == null) {

			System.out.println("messaging process Xmx (bytes) " + Runtime.getRuntime().maxMemory());

			// Note - using System.out here instead of logger due to logger init race
			if (takIgniteConfiguration.getCacheOffHeapMaxSizeBytes() == -1) {
				// this RAM calculation is a rough estimate based on JVM maxMemory(). Use explicit size (CoreConfig cacheOffHeapMaxSizeBytes to be exact).
				takIgniteConfiguration.setCacheOffHeapMaxSizeBytes((long) (Runtime.getRuntime().maxMemory() * takIgniteConfiguration.getCacheOffHeapPercentageMax()));
			}
			System.out.println("cache computed offheap max size " + takIgniteConfiguration.getCacheOffHeapMaxSizeBytes() + " bytes");

			if (takIgniteConfiguration.getCacheOffHeapInitialSizeBytes() == -1) {
				// this RAM calculation is a rough estimate based on JVM maxMemory(). Use explicit size (CoreConfig cacheOffHeapMaxSizeBytes to be exact).
				takIgniteConfiguration.setCacheOffHeapInitialSizeBytes((long) (Runtime.getRuntime().maxMemory() * takIgniteConfiguration.getCacheOffHeapPercentageInitial()));
			}
			System.out.println("cache computed offheap initial size " + takIgniteConfiguration.getCacheOffHeapInitialSizeBytes() + " bytes");


			if ((!takIgniteConfiguration.isEmbeddedIgnite())	&& (ActiveProfiles.getInstance().isApiProfileActive() || ActiveProfiles.getInstance().isMonolithProfileActive() ||
				ActiveProfiles.getInstance().isMessagingProfileActive() || ActiveProfiles.getInstance().isConfigProfileActive())) {
				takIgniteConfiguration.setEmbeddedIgnite(true);
			}

			System.out.println("ignite thread pool size: " + takIgniteConfiguration.getIgnitePoolSize());

			// set ignite pool size based on processor count
			if (takIgniteConfiguration.getIgnitePoolSize() < 1) {
				takIgniteConfiguration.setIgnitePoolSize(Runtime.getRuntime().availableProcessors() * takIgniteConfiguration.getIgnitePoolSizeMultiplier());

				if (takIgniteConfiguration.getIgnitePoolSize() > IGNITE_POOL_SIZE_LIMIT) { // ignite hard limit on pool size
					takIgniteConfiguration.setIgnitePoolSize(IGNITE_POOL_SIZE_LIMIT);
				}
			}

			configuration = getIgniteConfiguration(ActiveProfiles.getInstance().getProfile(), takIgniteConfiguration);
		}

		return configuration;
	}

	// It is preferred to let the class set this from the config file but this allows overriding the values for the legacy
	// code that touches this directly
	public TAKIgniteConfiguration getTAKIgniteConfiguration(String igniteHost, boolean isCluster, boolean isKubernetes, boolean isEmbedded,
															boolean isMulticastDiscovery, @Nullable Integer nonMulticastDiscoveryPort,
													  @Nullable Integer nonMulticastDiscoveryPortCount, Integer communicationPort,
															Integer communicationPortCount, int maxQueue, long workerTimeoutMilliseconds,
															long dataRegionInitialSize, long dataRegionMaxSize) {
		return getTAKIgniteConfiguration(igniteHost, isCluster, isKubernetes, isEmbedded, isMulticastDiscovery,
				nonMulticastDiscoveryPort, nonMulticastDiscoveryPortCount, communicationPort, communicationPortCount,
				maxQueue, workerTimeoutMilliseconds, dataRegionInitialSize, dataRegionMaxSize, -1, false,
				-1.f, false, false, -1,
				false, -1, -1, -1);
	}

	// It is preferred to let the class set this from the config file but this allows overriding the values for the legacy
	// code that touches this directly
	public TAKIgniteConfiguration getTAKIgniteConfiguration(String igniteHost, boolean isCluster, boolean isKubernetes,
															boolean isEmbedded, boolean isMulticastDiscovery, @Nullable Integer nonMulticastDiscoveryPort,
													        @Nullable Integer nonMulticastDiscoveryPortCount, Integer communicationPort,
															Integer communicationPortCount, int maxQueue, long workerTimeoutMilliseconds,
															long dataRegionInitialSize, long dataRegionMaxSize, int poolSize,
															boolean enablePersistence, float evictionThreshold, boolean ignitePoolSizeUseDefaultsForApi,
															boolean igniteDefaultSpiConnectionsPerNode, int igniteExplicitSpiConnectionsPerNode,
															boolean apiServiceIgniteServer, long spiConnectionTimeoutMs,
															long clientConnectionTimeoutMs, long failureDetectionTimeoutMs) {

		takIgniteConfiguration.setIgniteHost(igniteHost);
		takIgniteConfiguration.setClusterEnabled(isCluster);
		takIgniteConfiguration.setClusterKubernetes(isKubernetes);
		takIgniteConfiguration.setEmbeddedIgnite(isEmbedded);
		takIgniteConfiguration.setIgniteMulticast(isMulticastDiscovery);
		takIgniteConfiguration.setIgniteNonMulticastDiscoveryPort(nonMulticastDiscoveryPort);
		takIgniteConfiguration.setIgniteNonMulticastDiscoveryPortCount(nonMulticastDiscoveryPortCount);
		takIgniteConfiguration.setIgniteCommunicationPort(communicationPort);
		takIgniteConfiguration.setIgniteCommunicationPortCount(communicationPortCount);
		takIgniteConfiguration.setCapacity(maxQueue);
		takIgniteConfiguration.setIgniteWorkerTimeoutMilliseconds(Long.valueOf(workerTimeoutMilliseconds));
		takIgniteConfiguration.setCacheOffHeapInitialSizeBytes(dataRegionInitialSize);
		takIgniteConfiguration.setCacheOffHeapMaxSizeBytes(dataRegionMaxSize);
		takIgniteConfiguration.setIgnitePoolSize(poolSize);
		takIgniteConfiguration.setEnableCachePersistence(enablePersistence);
		takIgniteConfiguration.setCacheOffHeapEvictionThreshold(evictionThreshold);
		takIgniteConfiguration.setIgnitePoolSizeUseDefaultsForApi(ignitePoolSizeUseDefaultsForApi);
		takIgniteConfiguration.setIgniteDefaultSpiConnectionsPerNode(igniteDefaultSpiConnectionsPerNode);
		takIgniteConfiguration.setIgniteExplicitSpiConnectionsPerNode(igniteExplicitSpiConnectionsPerNode);
		takIgniteConfiguration.setIgniteApiServerMode(apiServiceIgniteServer);
		if (spiConnectionTimeoutMs < 0) {
			takIgniteConfiguration.setIgniteConnectionTimeoutSeconds(spiConnectionTimeoutMs);
		} else {
			takIgniteConfiguration.setIgniteConnectionTimeoutSeconds(TimeUnit.MILLISECONDS.toSeconds(spiConnectionTimeoutMs));
		}

		if (clientConnectionTimeoutMs < 0) {
			takIgniteConfiguration.setIgniteClientConnectionTimeoutSeconds(clientConnectionTimeoutMs);
		} else {
			takIgniteConfiguration.setIgniteClientConnectionTimeoutSeconds(TimeUnit.MILLISECONDS.toSeconds(clientConnectionTimeoutMs));
		}

		if (failureDetectionTimeoutMs < 0) {
			takIgniteConfiguration.setIgniteFailureDetectionTimeoutSeconds(failureDetectionTimeoutMs);
		} else {
			takIgniteConfiguration.setIgniteFailureDetectionTimeoutSeconds(TimeUnit.MILLISECONDS.toSeconds(failureDetectionTimeoutMs));
		}

		return takIgniteConfiguration;
	}


	// It is preferred to let the class set this from the config file but this allows overriding the values for the legacy
	// code that touches this directly
	public IgniteConfiguration getIgniteConfiguration(String igniteProfile, TAKIgniteConfiguration takIgniteConfiguration) {


		if (takIgniteConfiguration.isClusterEnabled()) {
			IgniteConfiguration clusterConf = new IgniteConfiguration();

			TcpDiscoverySpi tds = new TcpDiscoverySpi();

			if (takIgniteConfiguration.isClusterKubernetes()) {

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

			IgniteConfiguration standaloneConf = new IgniteConfiguration();

			String defaultWorkDir = "/opt/tak";
			try {
				 defaultWorkDir = U.defaultWorkDirectory();
			} catch (IgniteCheckedException e) {
				logger.error(" error getting Ignite work dir, default to /opt/tak ", e);
			}

			standaloneConf.setWorkDirectory(defaultWorkDir + "/" + igniteProfile + "-tmp-work");

			TcpDiscoverySpi spi = new TcpDiscoverySpi();
			
			if (!takIgniteConfiguration.isIgniteMulticast()) {

				TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

				String address = takIgniteConfiguration.getIgniteHost() + ":" + takIgniteConfiguration.getIgniteNonMulticastDiscoveryPort() +
						".." + (takIgniteConfiguration.getIgniteNonMulticastDiscoveryPort() + takIgniteConfiguration.getIgniteNonMulticastDiscoveryPortCount());
				ipFinder.setAddresses(Arrays.asList(address));
				
				logger.trace("ignite grid discovery address: {}", address);

				spi.setIpFinder(ipFinder);

				spi.setLocalPort(takIgniteConfiguration.getIgniteNonMulticastDiscoveryPort());
				spi.setLocalPortRange(takIgniteConfiguration.getIgniteNonMulticastDiscoveryPortCount());
			}
			
			standaloneConf.setDiscoverySpi(spi);
			TcpCommunicationSpi tcpSpiConf = new TcpCommunicationSpi();
			tcpSpiConf.setLocalPort(takIgniteConfiguration.getIgniteCommunicationPort());
			tcpSpiConf.setLocalPortRange(takIgniteConfiguration.getIgniteCommunicationPortCount());
			tcpSpiConf.setLocalAddress(takIgniteConfiguration.getIgniteHost());
			tcpSpiConf.setMessageQueueLimit(takIgniteConfiguration.getCapacity());

			if (takIgniteConfiguration.getIgniteConnectionTimeoutSeconds() > -1) {
				tcpSpiConf.setConnectTimeout(TimeUnit.SECONDS.toMillis(takIgniteConfiguration.getIgniteConnectionTimeoutSeconds())); // milliseconds
			}

            standaloneConf.setLocalHost(takIgniteConfiguration.getIgniteHost());

            if (takIgniteConfiguration.isIgniteDefaultSpiConnectionsPerNode()) {
				// use default
			} else if (takIgniteConfiguration.getIgniteExplicitSpiConnectionsPerNode() < 1) {
				// autodetect to num CPU cores
				tcpSpiConf.setConnectionsPerNode(Runtime.getRuntime().availableProcessors());
			} else {
				tcpSpiConf.setConnectionsPerNode(takIgniteConfiguration.getIgniteExplicitSpiConnectionsPerNode());
			}
			
			// Using System.out due to log init race
			System.out.println("Ignite SPI connections per node: " + tcpSpiConf.getConnectionsPerNode());
			
			standaloneConf.setCommunicationSpi(tcpSpiConf);

			boolean isIgniteApiServerMode = false;
			if (ActiveProfiles.getInstance().isApiProfileActive() && takIgniteConfiguration.isIgniteApiServerMode()) {
				isIgniteApiServerMode = true;
			}

			// If this process is the API micro-service, optionally run in ignite embedded server mode. In standalone, messaging service is always an ignite server. Plugin manager and retention service are always ignite clients. 
			if (isIgniteApiServerMode) {
				standaloneConf.setClientMode(false);
			} else {
				standaloneConf.setClientMode(!takIgniteConfiguration.isEmbeddedIgnite());
			}
			standaloneConf.setIgniteInstanceName(Constants.IGNITE_INSTANCE_NAME);

			standaloneConf.setUserAttributes(Collections.singletonMap(Constants.TAK_PROFILE_KEY, igniteProfile));
			
			standaloneConf.setFailureHandler(new NoOpFailureHandler());

			if (takIgniteConfiguration.getIgniteClientConnectionTimeoutSeconds() > -1) {
				standaloneConf.setClientFailureDetectionTimeout(TimeUnit.SECONDS.toMillis(takIgniteConfiguration.getIgniteClientConnectionTimeoutSeconds()));
			}

			if (takIgniteConfiguration.getIgniteFailureDetectionTimeoutSeconds() > -1) {
				long failureDetectionTimeoutMs = TimeUnit.SECONDS.toMillis(takIgniteConfiguration.getIgniteFailureDetectionTimeoutSeconds());
				standaloneConf.setFailureDetectionTimeout(failureDetectionTimeoutMs);
				standaloneConf.setSystemWorkerBlockedTimeout(failureDetectionTimeoutMs);
			}
			
			if (takIgniteConfiguration.isEmbeddedIgnite()) {

				DataStorageConfiguration storageConfig = new DataStorageConfiguration();

				DataRegionConfiguration takserverStorageRegion = new DataRegionConfiguration();
				takserverStorageRegion.setName("takserver-cache-region");
				takserverStorageRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU); // cache eviction policy

				if (takIgniteConfiguration.getCacheOffHeapEvictionThreshold() != -1.f) {
					takserverStorageRegion.setEvictionThreshold(takIgniteConfiguration.getCacheOffHeapEvictionThreshold());
				}


				takserverStorageRegion.setInitialSize(takIgniteConfiguration.getCacheOffHeapInitialSizeBytes());
				takserverStorageRegion.setMaxSize(takIgniteConfiguration.getCacheOffHeapMaxSizeBytes());

				storageConfig.setDefaultDataRegionConfiguration(takserverStorageRegion);

				if (takIgniteConfiguration.isEnableCachePersistence()) {
					takserverStorageRegion.setPersistenceEnabled(true);
					String basePath = Paths.get("").toAbsolutePath().toString();
					FileSystem fs = FileSystems.getDefault();
					storageConfig.setStoragePath(basePath + fs.getSeparator() + "tmp" + fs.getSeparator() + "cache-" +  igniteProfile);
				}

				standaloneConf.setDataStorageConfiguration(storageConfig);
			} else { // client mode - API process
				ClientConnectorConfiguration ccc = standaloneConf.getClientConnectorConfiguration();

				if (takIgniteConfiguration.getIgnitePoolSize() > ccc.getThreadPoolSize()) {
					ccc.setThreadPoolSize(takIgniteConfiguration.getIgnitePoolSize());
				}
			}

			if (!takIgniteConfiguration.isEmbeddedIgnite() && !takIgniteConfiguration.isIgnitePoolSizeUseDefaultsForApi()) {
				int poolSize = takIgniteConfiguration.getIgnitePoolSize();
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

	public void saveChanges() {
		try {
			synchronized (TAKIgniteConfiguration.class) {
				JAXBUtils.saveJAXifiedObject(CONFIG_FILE, takIgniteConfiguration, false);
			}
		} catch (FileNotFoundException fnfe) {
			// do nothing. Happens in unit test.
		} catch (JAXBException | IOException e) {
			throw new RuntimeException(e);
		}
	}

}
