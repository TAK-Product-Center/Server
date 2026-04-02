package tak.server.federation.hub.plugins;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.failure.NoOpFailureHandler;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.plugin.FederationHubPluginMetadata;

public class PluginIgniteManager {
    private static final Logger logger = LoggerFactory.getLogger(PluginIgniteManager.class);
    private Ignite ignite;
    private final PluginIgniteConfig config;
    private final FederationHubPluginMetadata metadata;

    public PluginIgniteManager(PluginIgniteConfig config, FederationHubPluginMetadata metadata) {
        this.config = config;
        this.metadata = metadata;
    }

    public Ignite start() {
        if (ignite != null && ignite.cluster().active())
            return ignite;

        IgniteConfiguration conf = new IgniteConfiguration();
        conf.setIgniteInstanceName(config.getIgniteProfile());
        conf.setClientMode(true);
        conf.setFailureHandler(new NoOpFailureHandler());
        conf.setMetricsLogFrequency(config.getMetricsLogFrequency());

        conf.setIncludeEventTypes(
                EventType.EVT_NODE_JOINED,
                EventType.EVT_NODE_LEFT,
                EventType.EVT_NODE_FAILED,
                EventType.EVT_CLIENT_NODE_DISCONNECTED,
                EventType.EVT_CLIENT_NODE_RECONNECTED
        );

        configureDiscovery(conf);
        configureCommunication(conf);
        configureThreadPools(conf);
        configureAttributes(conf);

        ignite = Ignition.start(conf);
        logger.info("Ignite started for profile {}", config.getIgniteProfile());
        return ignite;
    }

    private void configureDiscovery(IgniteConfiguration conf) {
        TcpDiscoveryVmIpFinder finder = new TcpDiscoveryVmIpFinder();
        finder.setAddresses(Collections.singletonList(config.getIgniteHost() + ":" +
                config.getNonMulticastDiscoveryPort() + ".." +
                (config.getNonMulticastDiscoveryPort() + config.getNonMulticastDiscoveryPortCount())));

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        spi.setIpFinder(finder);
        spi.setLocalPort(config.getNonMulticastDiscoveryPort());
        spi.setLocalPortRange(config.getNonMulticastDiscoveryPortCount());
        conf.setDiscoverySpi(spi);
    }

    private void configureCommunication(IgniteConfiguration conf) {
        TcpCommunicationSpi comms = new TcpCommunicationSpi();
        comms.setLocalPort(config.getCommunicationPort());
        comms.setLocalPortRange(config.getCommunicationPortCount());
        comms.setLocalAddress(config.getIgniteHost());
        comms.setMessageQueueLimit(512);
        conf.setCommunicationSpi(comms);
    }

    private void configureThreadPools(IgniteConfiguration conf) {
        int poolSize = config.getIgnitePoolSize() > 0 ?
                config.getIgnitePoolSize() :
                Math.min(Runtime.getRuntime().availableProcessors() *
                        config.getIgnitePoolSizeMultiplier(), 1024);

        conf.setPublicThreadPoolSize(poolSize);
        conf.setSystemThreadPoolSize(poolSize + 1);
        conf.setServiceThreadPoolSize(poolSize);
        conf.setQueryThreadPoolSize(poolSize);
    }

    private void configureAttributes(IgniteConfiguration conf) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY, config.getIgniteProfile());
        attrs.put("plugin-name", metadata.getName());
        conf.setUserAttributes(attrs);
    }

    public Ignite getIgnite() {
        return ignite;
    }

    public void stop() {
        if (ignite != null) {
            ignite.close();
            logger.info("Ignite stopped for {}", config.getIgniteProfile());
        }
    }
}
