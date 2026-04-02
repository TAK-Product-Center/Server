package tak.server.federation.hub.plugins;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.FederateProvenance;

import tak.server.federation.FederationException;
import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.plugin.DistributedFederationHubPluginService;
import tak.server.federation.hub.plugin.FederationHubPluginType;
import tak.server.federation.hub.plugin.manager.FederationHubPluginManager;
import tak.server.federation.hub.plugins.clients.PluginClient;

public abstract class PluginBase {
	private static final Logger logger = LoggerFactory.getLogger(PluginBase.class);

	protected PluginIgniteConfig pluginIgniteConfig;
	protected PluginIgniteManager igniteMgr;
	protected Ignite ignite;

	protected PluginClient client;

	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	protected final AtomicBoolean isRegistered = new AtomicBoolean(false);

	protected PluginBase(PluginClient client, FederationHubPluginType type) {
		this(client, type, new PluginIgniteConfig(client.getMetadata().getName()));
	}

	protected PluginBase(PluginClient client, FederationHubPluginType type, PluginIgniteConfig pluginIgniteConfig) {
		this.client = client;
		this.client.getMetadata().setType(type);

		this.pluginIgniteConfig = pluginIgniteConfig;

		this.igniteMgr = new PluginIgniteManager(pluginIgniteConfig, this.client.getMetadata());
		this.ignite = igniteMgr.start();

		initIgniteLifecycle();
	}
	
	private void initIgniteLifecycle() {
		listenForPluginManagerDisconnect();
		listenForClientNodeEvents();

		deployPluginService();
		registerPlugin();
	}

	private void deployPluginService() {
		DistributedFederationHubPluginService pluginService = new DistributedFederationHubPluginService(ignite);
		ClusterGroup clusterGroup = ignite.cluster().forAttribute(
				FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY, pluginIgniteConfig.getIgniteProfile());

		ignite.services(clusterGroup).deployNodeSingleton(FederationHubConstants.FED_HUB_PLUGIN_SERVICE, pluginService);
	}

	// if the plugin manager disconnects from the cluster, we must re-register once
	// it becomes available again
	private void listenForPluginManagerDisconnect() {
		ignite.events().localListen(new IgnitePredicate<DiscoveryEvent>() {
			@Override
			public boolean apply(DiscoveryEvent event) {
				String nodeName = event.eventNode().attribute(FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY);

				if (FederationHubConstants.FEDERATION_HUB_PLUGIN_MANAGER_IGNITE_PROFILE.equals(nodeName)) {
					logger.info("Plugin Manager disconnected from cluster!");
					isRegistered.set(false);
					onUnRegistered();
					registerPlugin();
				}
				return true;
			}
		}, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED);
	}

	// if cluster goes down or this node disconnects, we need to re-register
	// listeners and services
	private void listenForClientNodeEvents() {
		ignite.events().localListen(event -> {
			switch (event.type()) {
			case EventType.EVT_CLIENT_NODE_DISCONNECTED:
				logger.info("This node disconnected from cluster");
				isRegistered.set(false);
				onUnRegistered();
				break;
			case EventType.EVT_CLIENT_NODE_RECONNECTED:
				logger.info("This node reconnected to cluster");
				scheduler.schedule(this::initIgniteLifecycle, 1, TimeUnit.SECONDS);
				return false; // stop listening, because will re-register
			}
			return true;
		}, EventType.EVT_CLIENT_NODE_DISCONNECTED, EventType.EVT_CLIENT_NODE_RECONNECTED);
	}

	protected synchronized void registerPlugin() {
		if (isRegistered.get())
			return;

		// Don’t retry if we’re not connected to the cluster
		if (!ignite.cluster().forClients().nodes().contains(ignite.cluster().localNode()))
			return;

		String errorMessage = null;
		try {
			FederationHubPluginManager pluginManager = ignite.services(ignite.cluster()).serviceProxy(
					FederationHubConstants.FED_HUB_PLUGIN_MANAGER_SERVICE, FederationHubPluginManager.class, false);

			FederationException exception = pluginManager.registerPlugin(client.getMetadata());
			if (exception != null) {
				errorMessage = "Could not register plugin: " + exception.getMessage();
			} else {
				isRegistered.set(true);
				onRegistered();
			}
		} catch (Exception e) {
			logger.error("", e);
			errorMessage = "Could not connect to Plugin Manager!";
		}

		if (isRegistered.get()) {
			logger.info("Plugin registered!");
		} else {
			logger.info("{} Attempting to register again.", errorMessage);
			scheduler.schedule(this::registerPlugin, 5, TimeUnit.SECONDS);
		}
	}

	protected FederateProvenance getProvenance() {
		String provenanceId = FederationHubConstants.FEDERATION_HUB_PLUGIN_PROVENANCE + " "
				+ client.getMetadata().getName();
		return FederateProvenance.newBuilder().setFederationServerId(provenanceId).setFederationServerName(provenanceId)
				.build();
	}
	
	protected abstract void onRegistered();
	protected abstract void onUnRegistered();
	
	public void shutDown() {
		isRegistered.set(false);
		onRegistered();
		scheduler.shutdown();
		ignite.close();
	}
}
