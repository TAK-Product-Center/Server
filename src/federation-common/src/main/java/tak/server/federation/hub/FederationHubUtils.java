package tak.server.federation.hub;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.failure.NoOpFailureHandler;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;

public class FederationHubUtils {
	private static final Logger logger = LoggerFactory.getLogger(FederationHubUtils.class);

    public static IgniteConfiguration getIgniteConfiguration(String profile, boolean isClient) {
    	FederationHubIgniteConfig igniteConfig = null;
    	
		String igniteFile = System.getProperty("fedhub.ignite.config");
    	if (Strings.isNullOrEmpty(igniteFile)) {
			igniteFile =  "/opt/tak/federation-hub/configs/ignite.yml";
    		logger.info("Ignite config file not supplied. Assigning default to: " + igniteFile);
    	} else {
    		logger.info("Ignite Config file supplied: " + igniteFile);
    	}
    	
    	try {
    		igniteConfig = new FederationHubUtils().loadIgniteConfig(igniteFile);
    		logger.info("Loaded ignite config from file");
    	} catch (Exception e) {
    		logger.info("Ignite config not found, generating default one");
    		// failed to load file, use defaults
			igniteConfig = new FederationHubIgniteConfig();
		}
    	
        IgniteConfiguration conf = new IgniteConfiguration();
        
        String defaultWorkDir = "/opt/tak/federation-hub";
		try {
			 defaultWorkDir = U.defaultWorkDirectory();
		} catch (IgniteCheckedException e) {
			logger.error(" error getting Ignite work dir, default to /opt/tak/federation-hub ", e);
		}

		conf.setWorkDirectory(defaultWorkDir + "/" + profile + "-tmp-work");

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
        
        int poolSize;
        // dynamic
        if (igniteConfig.getIgnitePoolSize() < 0) {
        	poolSize = Math.min(Runtime.getRuntime().availableProcessors() * igniteConfig.getIgnitePoolSizeMultiplier(), 1024);
        } else {
        	poolSize = igniteConfig.getIgnitePoolSize();
        }
        
        if (isClient) {
        	ClientConnectorConfiguration ccc = conf.getClientConnectorConfiguration();
        	ccc.setThreadPoolSize(poolSize);
        }
        
        conf.setSystemThreadPoolSize(poolSize + 1);
        conf.setPublicThreadPoolSize(poolSize);
        conf.setQueryThreadPoolSize(poolSize);
        conf.setServiceThreadPoolSize(poolSize);
        conf.setStripedPoolSize(poolSize);
        conf.setDataStreamerThreadPoolSize(poolSize);
        conf.setRebalanceThreadPoolSize(poolSize);

        return conf;
    }
    
	private FederationHubIgniteConfig loadIgniteConfig(String configFile)
			throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		if (getClass().getResource(configFile) != null) {
			// It's a resource.
			return new ObjectMapper(new YAMLFactory()).readValue(getClass().getResourceAsStream(configFile),
					FederationHubIgniteConfig.class);
		}

		// It's a file.
		return new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(configFile),
				FederationHubIgniteConfig.class);
	}
}
