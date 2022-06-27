package tak.server.federation.hub;

import java.util.Arrays;
import java.util.Collections;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.failure.NoOpFailureHandler;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

public class FederationHubUtils {

    public static IgniteConfiguration getIgniteConfiguration(String profile, boolean isClient) {
        IgniteConfiguration conf = new IgniteConfiguration();

        String address = FederationHubConstants.FEDERATION_HUB_IGNITE_HOST + ":" +
            FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT + ".." +
            (FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT +
                FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT_COUNT);
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList(address));

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        spi.setIpFinder(ipFinder);
        spi.setLocalPort(FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT);
        spi.setLocalPortRange(FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT_COUNT);

        conf.setDiscoverySpi(spi);

        TcpCommunicationSpi comms = new TcpCommunicationSpi();
        comms.setLocalPort(FederationHubConstants.COMMUNICATION_PORT);
        comms.setLocalPortRange(FederationHubConstants.COMMUNICATION_PORT_COUNT);
        comms.setLocalAddress(FederationHubConstants.FEDERATION_HUB_IGNITE_HOST);
        comms.setMessageQueueLimit(512);

        conf.setCommunicationSpi(comms);

        conf.setClientMode(isClient);

        conf.setUserAttributes(
            Collections.singletonMap(
                FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
                profile));

        conf.setFailureHandler(new NoOpFailureHandler());

        return conf;
    }
}
