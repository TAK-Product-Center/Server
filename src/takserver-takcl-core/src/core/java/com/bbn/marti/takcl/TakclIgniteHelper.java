package com.bbn.marti.takcl;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.xml.bind.JAXBException;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.bbn.marti.config.TAKIgniteConfiguration;
import com.bbn.marti.remote.groups.FileUserManagementInterface;
import com.bbn.marti.remote.service.InputManager;
import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;

import tak.server.Constants;
import tak.server.ignite.IgniteConfigurationHolder;
import tak.server.util.JAXBUtils;

import static com.bbn.marti.takcl.TAKCLCore.igniteManualRetryTimeout;
import static com.bbn.marti.takcl.TAKCLCore.igniteNetworkTimeout;
import static com.bbn.marti.takcl.TAKCLCore.ignitePortOverride;
import static com.bbn.marti.takcl.TAKCLCore.ignitePortRangeOverride;
import static com.bbn.marti.takcl.TAKCLCore.igniteScanInterfaces;
import static com.bbn.marti.takcl.TAKCLCore.k8sMode;
import static com.bbn.marti.takcl.TAKCLCore.useTakclIgniteConfig;

public class TakclIgniteHelper {

	private static final Logger logger = TAKCLogging.getLogger(TakclIgniteHelper.class);

	private static final String DISCOVERY_ADDRESSES_KEY = "IGNITE_TCP_DISCOVERY_ADDRESSES";

	private static final Map<String, IgniteConfiguration> serverConfigurationMap = new HashMap<>();

	private static final AtomicInteger identifierCounter = new AtomicInteger();

	public static final void overrideServerConfigurationFromCoreConfig(@NotNull AbstractServerProfile serverProfile,
																	   @NotNull TAKIgniteConfiguration serverIgniteConfiguration) {
		if (serverConfigurationMap.containsKey(serverProfile.getConsistentUniqueReadableIdentifier())) {
			logger.warn("Override already configured for '" + serverProfile.getConsistentUniqueReadableIdentifier() + "'!");
			return;
		}
		logger.debug("Using Ignite configuration details from TAKIgniteConfig.xml");

		String igniteProfile = "takcl" + identifierCounter.getAndIncrement();
		String igniteHost = serverIgniteConfiguration.getIgniteHost();
		boolean isCluster = serverIgniteConfiguration.isClusterEnabled();
		boolean isKubernetes = serverIgniteConfiguration.isClusterKubernetes();
		boolean useEmbeddedIgnite = false;
		boolean isIgniteMulticast = serverIgniteConfiguration.isIgniteMulticast();
		Integer igniteNonMulticastDiscoveryPort = serverIgniteConfiguration.getIgniteNonMulticastDiscoveryPort();
		Integer igniteNonMulticastDiscoveryPortCount = serverIgniteConfiguration.getIgniteNonMulticastDiscoveryPortCount();
		int igniteCommunicationPort = serverIgniteConfiguration.getIgniteCommunicationPort();
		int igniteCommunicationPortCount = serverIgniteConfiguration.getIgniteCommunicationPortCount();
		int igniteQueueCapacity = serverIgniteConfiguration.getCapacity();
		long workerTimeoutMS = serverIgniteConfiguration.getIgniteWorkerTimeoutMilliseconds();

		TAKIgniteConfiguration takIgniteConfig = IgniteConfigurationHolder.getInstance().getTAKIgniteConfiguration(
				igniteHost, isCluster, isKubernetes,	useEmbeddedIgnite, isIgniteMulticast,
				igniteNonMulticastDiscoveryPort, igniteNonMulticastDiscoveryPortCount,
				igniteCommunicationPort, igniteCommunicationPortCount, igniteQueueCapacity, workerTimeoutMS,
				serverIgniteConfiguration.getCacheOffHeapInitialSizeBytes(),
				serverIgniteConfiguration.getCacheOffHeapMaxSizeBytes(),
				-1, false, -1.f, false,
				false, -1, true,
				-1, -1, -1);

		IgniteConfiguration igniteConfig =  IgniteConfigurationHolder.getInstance().getIgniteConfiguration(igniteProfile, takIgniteConfig);
		igniteConfig.setGridLogger(new Slf4jLogger(TAKCLogging.getLogger("org.apache.ignite")));



		if (TAKCLCore.igniteClientFailureDetectionTimeout != null) {
			logger.trace("Setting clientFailureDetectionTimout to " + TAKCLCore.igniteClientFailureDetectionTimeout);
			igniteConfig.setClientFailureDetectionTimeout(TAKCLCore.igniteClientFailureDetectionTimeout);
		}

		if (TAKCLCore.igniteFailureDetectionTimeout != null) {
			logger.trace("Setting failureDetectionTimeout to " + TAKCLCore.igniteFailureDetectionTimeout);
			igniteConfig.setFailureDetectionTimeout(TAKCLCore.igniteFailureDetectionTimeout);
		}

		if (TAKCLCore.igniteNetworkTimeout != null) {
			logger.trace("Setting networkTimeout to " + TAKCLCore.igniteNetworkTimeout);
			igniteConfig.setNetworkTimeout(TAKCLCore.igniteNetworkTimeout);
		}

		if (igniteConfig.getDiscoverySpi() instanceof TcpDiscoverySpi) {
			TcpDiscoverySpi spi = (TcpDiscoverySpi) igniteConfig.getDiscoverySpi();

			if (TAKCLCore.ignitePortOverride != null) {
				logger.trace("Overriding ignite discovery port from override to " + TAKCLCore.ignitePortOverride);
				spi.setLocalPort(ignitePortOverride);
			}
			if (TAKCLCore.ignitePortRangeOverride != null) {
				logger.trace("Overriding ignite discovery port range from override to " + TAKCLCore.ignitePortRangeOverride);
				spi.setLocalPortRange(ignitePortRangeOverride);
			}

			if (TAKCLCore.igniteIpAddressOverride != null && spi.getIpFinder() instanceof TcpDiscoveryVmIpFinder) {
				String portAppendValue = "";

				Integer igniteDiscoveryPort;
				Integer igniteDiscoveryPortCount;
				if (TAKCLCore.ignitePortOverride != null) {
					logger.trace("Overriding ignite discovery port from override to " + TAKCLCore.ignitePortOverride);
					igniteDiscoveryPort = TAKCLCore.ignitePortOverride;
					if (TAKCLCore.ignitePortRangeOverride != null) {
						logger.trace("Overriding ignite discovery port range from override to " + TAKCLCore.ignitePortRangeOverride);
						igniteDiscoveryPortCount = TAKCLCore.ignitePortRangeOverride;
					} else {
						igniteDiscoveryPortCount = 100;
					}
					portAppendValue = ":" + igniteDiscoveryPort + ".." + (igniteDiscoveryPort + igniteDiscoveryPortCount);
				}

				TcpDiscoveryVmIpFinder ipFinder = (TcpDiscoveryVmIpFinder) spi.getIpFinder();
				logger.trace("Using supplied override discovery address '" + TAKCLCore.igniteIpAddressOverride + portAppendValue + " for ignite.");
				ipFinder.setAddresses(Arrays.asList(TAKCLCore.igniteIpAddressOverride + portAppendValue));
			}
		}


		serverConfigurationMap.put(serverProfile.getConsistentUniqueReadableIdentifier(), igniteConfig);
	}

	public static class TakclIgniteInstance {
		private static final Logger logger = TAKCLogging.getLogger(TakclIgniteInstance.class);

		private final Ignite ignite;
		private final AbstractServerProfile serverProfile;
		private FileUserManagementInterface userManager;
		private InputManager inputManager;

		public TakclIgniteInstance(@NotNull AbstractServerProfile serverProfile, @NotNull Ignite ignite) {
			this.serverProfile = serverProfile;
			this.ignite = ignite;
		}

		public synchronized FileUserManagementInterface getUserManager() {
			logger.debug("Getting UserManager");
			if (userManager == null) {
				ClusterGroup group;
				// if a monolith server - monolith
				if (ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MONOLITH_PROFILE_NAME).nodes().size() > 0) {
					logger.trace("Loading Monolith Configuration");
					group = ignite.cluster().forRemotes().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MONOLITH_PROFILE_NAME);
				}
				// if a messaging server - multiprocess
				else if (ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME).nodes().size() > 0) {
					logger.trace("Loading Multiprocess Configuration");
					group = ignite.cluster().forRemotes().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME);
				}
				// else we must be in the cluster
				else {
					logger.trace("Loading Cluster Configuration");
					group = ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME);
				}

				logger.debug("Creating new UserManager");
				userManager = ignite.services(group).serviceProxy(Constants.DISTRIBUTED_USER_FILE_MANAGER, FileUserManagementInterface.class, false);
			}

			return userManager;
		}

		public synchronized InputManager getInputManager() {
			logger.debug("Getting InputManager");
			if (inputManager == null) {
				ClusterGroup group;
				// if a monolith server - monolith
				if (ignite.cluster().forServers().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MONOLITH_PROFILE_NAME).nodes().size() > 0) {
					logger.trace("Loading Monolith Messaging Configuration");
					group = ignite.cluster().forServers().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MONOLITH_PROFILE_NAME);
				}
				// if a messaging server - multiprocess
				else if (ignite.cluster().forServers().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME).nodes().size() > 0) {
					logger.trace("Loading Multiprocess Messaging Configuration");
					group = ignite.cluster().forServers().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME);
				}
				// else we must be in the cluster
				else {
					logger.trace("Loading Cluster Messaging Configuration");
					group = ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME);
				}
				logger.debug("Creating new InputManager");
				inputManager = ignite.services(group).serviceProxy(Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class, false);
			}
			return inputManager;
		}

		public synchronized void closeIgniteInstance() {
			if (!TestExceptions.DO_NOT_CLOSE_IGNITE_INSTANCES) {
				logger.debug("Closing Ignite Instance for " + serverProfile);
				ignite.close();
				logger.debug("Ignite instance for " + serverProfile + " closed.");
			}
		}
	}

	private static final HashMap<String, TakclIgniteInstance> igniteInstances = new HashMap<>();

	public static List<String> getExternalIpAddresses(@Nullable Integer igniteDiscoveryPort, @Nullable Integer igniteDiscoveryPortCount) {
		String portAppend;

		if (igniteDiscoveryPort != null) {
			if (igniteDiscoveryPortCount != null) {
				portAppend = ":" + igniteDiscoveryPort + ".." + (igniteDiscoveryPort + igniteDiscoveryPortCount);
			} else {
				portAppend = ":" + igniteDiscoveryPort;
			}
		} else {
			portAppend = "";
		}

		try {
			List<String> ipAddresses = new LinkedList<>();

			String discoveryAddresses = System.getProperty(DISCOVERY_ADDRESSES_KEY);

			if (discoveryAddresses != null && !discoveryAddresses.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("grid discovery addresses: " + discoveryAddresses);
				}

			} else if (TAKCLCore.igniteIpAddressOverride != null) {
				logger.trace("Using supplied override address '" + TAKCLCore.igniteIpAddressOverride + " for ignite.");
				ipAddresses.add(TAKCLCore.igniteIpAddressOverride + portAppend);

			} else if (igniteScanInterfaces) {
				logger.trace("Scanning all interfaces for IP Addresses to bind ignite to");
				Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
				for (NetworkInterface netint : Collections.list(nets)) {
					for (InetAddress addr : Collections.list(netint.getInetAddresses())) {
						ipAddresses.add(addr.getHostAddress() + portAppend);
					}
				}
			} else {
				logger.trace("Using default address of 127.0.0.1 for ignite");
				ipAddresses.add("127.0.0.1" + portAppend);
			}
			return ipAddresses;
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

	public static synchronized FileUserManagementInterface getUserManager(@NotNull AbstractServerProfile serverProfile) {
		return getIgnite(serverProfile).getUserManager();
	}

	public static synchronized InputManager getInputManager(@NotNull AbstractServerProfile serverProfile) {
		return getIgnite(serverProfile).getInputManager();
	}

	public static synchronized void closeAssociatedIgniteInstance(AbstractServerProfile serverProfile) {
		logger.debug("Closing ignite instance for " + serverProfile);
		String tag = serverProfile.getConsistentUniqueReadableIdentifier();
		if (serverConfigurationMap.containsKey(tag)) {
			serverConfigurationMap.remove(tag);
		}
		if (igniteInstances.containsKey(tag)) {
			TakclIgniteInstance igniteInstance = igniteInstances.get(tag);
			igniteInstance.closeIgniteInstance();
			igniteInstances.remove(tag);
		}
	}

	public static synchronized void closeAllIgniteInstances() {
		serverConfigurationMap.clear();
		for (TakclIgniteInstance tii : igniteInstances.values()) {
			tii.closeIgniteInstance();
		}
		igniteInstances.clear();
	}

	private static synchronized TakclIgniteInstance getIgnite(AbstractServerProfile serverProfile) {
		String tag = serverProfile.getConsistentUniqueReadableIdentifier();
		if (igniteInstances.containsKey(tag)) {
			logger.debug("Getting Existing Ignite instance for " + serverProfile);
			return igniteInstances.get(tag);
		}

		IgniteConfiguration igniteConfiguration;
		if (serverConfigurationMap.containsKey(tag)) {
			logger.debug("Getting Existing Server Configuration from TAKIgniteConfig.xml for " + serverProfile);
			igniteConfiguration = serverConfigurationMap.get(tag);

		} else if (k8sMode) {
			logger.debug("Getting Cluster Server Configuration for " + serverProfile);
			igniteConfiguration = createClusterIgniteConfiguration();

		} else {
			try {
				logger.debug("Creating new Server Configuration for " + serverProfile);
				if (useTakclIgniteConfig) {
					igniteConfiguration = createOldIgniteConfiguration(serverProfile);
				} else {
					TAKIgniteConfiguration takIgniteConfig = JAXBUtils.loadJAXifiedXML(serverProfile.getTAKIgniteConfigFilePath(), TAKIgniteConfiguration.class.getPackage().getName());
					overrideServerConfigurationFromCoreConfig(serverProfile, takIgniteConfig);
					igniteConfiguration = serverConfigurationMap.get(tag);
				}
			} catch (FileNotFoundException | JAXBException e) {
				throw new RuntimeException(e);
			}

			logger.trace("Using takserver-common IgniteConfiguration creation.");
		}

		igniteConfiguration.setGridLogger(new Slf4jLogger(logger));
		logger.debug("Starting Ignite instance...");

		Ignite ignite = null;
		long timeout = igniteManualRetryTimeout;
		logger.trace("Using manual retry");
		boolean isStarted = false;
		long startTime = System.currentTimeMillis();

		long duration = 0;

		while (!isStarted && duration <= timeout) {
			try {
				logger.trace("Starting ignite...");
				ignite = Ignition.getOrStart(igniteConfiguration);
				isStarted = true;
			} catch (Exception e) {
				logger.trace("Ignite start failed. details: " + e.getMessage());
				isStarted = false;
			}
			duration = System.currentTimeMillis() - startTime;
		}

		if (duration >= timeout && !isStarted) {
			throw new EndUserReadableException("Could not connect to server within the " + timeout + " ms timeout!",
					new RuntimeException("Ignite failed to connect within the " + timeout + " ms timeout!"));
		}

		logger.debug("Ignite instance starting finished");
		TakclIgniteInstance igniteInstance = new TakclIgniteInstance(serverProfile, ignite);
		igniteInstances.put(tag, igniteInstance);
		return igniteInstance;
	}

	public static synchronized void halt() {
		for (TakclIgniteInstance igniteInstance : igniteInstances.values()) {
			igniteInstance.closeIgniteInstance();
		}
		igniteInstances.clear();
		serverConfigurationMap.clear();
		logger.debug("Cached Ignite Data Cleared");
	}

	private static synchronized IgniteConfiguration createClusterIgniteConfiguration() {
		IgniteConfiguration conf = new IgniteConfiguration();
		TcpDiscoverySpi tds = new TcpDiscoverySpi();

		TcpDiscoveryKubernetesIpFinder ipFinder = new TcpDiscoveryKubernetesIpFinder();

		ipFinder.setServiceName("takserver-ignite");
		ipFinder.setNamespace("takserver");

		tds.setIpFinder(ipFinder);
		conf.setDiscoverySpi(tds);
		conf.setClientMode(true);
		conf.setUserAttributes(Collections.singletonMap(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME));
		return conf;
	}

	private static synchronized IgniteConfiguration createOldIgniteConfiguration(@NotNull AbstractServerProfile serverProfile) {
		Integer igniteDiscoveryPort;
		if (TAKCLCore.ignitePortOverride == null) {
			igniteDiscoveryPort = serverProfile.getIgniteDiscoveryPort();
		} else {
			logger.trace("Overriding ignite discovery port from override to " + TAKCLCore.ignitePortOverride);
			igniteDiscoveryPort = TAKCLCore.ignitePortOverride;
		}

		Integer igniteDiscoveryPortCount;
		if (TAKCLCore.ignitePortRangeOverride == null) {
			igniteDiscoveryPortCount = serverProfile.getIgniteDiscoveryPortCount();
		} else {
			logger.trace("Overriding ignite discovery port range from override to " + TAKCLCore.ignitePortRangeOverride);
			igniteDiscoveryPortCount = TAKCLCore.ignitePortRangeOverride;
		}

		String tag = "discoveryPort=" + igniteDiscoveryPort + ",discoveryPortCount=" + igniteDiscoveryPortCount;
		IgniteConfiguration conf = new IgniteConfiguration();

		TcpDiscoverySpi tds = new TcpDiscoverySpi();
		TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
		ipFinder.setAddresses(getExternalIpAddresses(igniteDiscoveryPort, igniteDiscoveryPortCount));
		tds.setIpFinder(ipFinder);

		if (igniteDiscoveryPort != null) {
			tds.setLocalPort(igniteDiscoveryPort);
		}

		if (igniteDiscoveryPortCount != null) {
			tds.setLocalPortRange(igniteDiscoveryPortCount);
		}

		conf.setDiscoverySpi(tds);
		conf.setClientMode(true);

		conf.setGridLogger(new Slf4jLogger(TAKCLogging.getLogger("org.apache.ignite")));

		conf.setIgniteInstanceName(tag.replace("=", "").replace(",", "_"));
		if (TAKCLCore.igniteFailureDetectionTimeout != null) {
			conf.setFailureDetectionTimeout(TAKCLCore.igniteFailureDetectionTimeout);
		}
		if (TAKCLCore.igniteClientFailureDetectionTimeout != null) {
			conf.setClientFailureDetectionTimeout(TAKCLCore.igniteClientFailureDetectionTimeout);
		}

		if (igniteNetworkTimeout != null) {
			logger.trace("Setting networkTimeout to " + igniteNetworkTimeout);
			conf.setNetworkTimeout(igniteNetworkTimeout);
		}

		TcpCommunicationSpi comms = new TcpCommunicationSpi();
		comms.setMessageQueueLimit(2048);
		conf.setCommunicationSpi(comms);

		conf.setUserAttributes(Collections.singletonMap(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME));
		return conf;
	}
}
