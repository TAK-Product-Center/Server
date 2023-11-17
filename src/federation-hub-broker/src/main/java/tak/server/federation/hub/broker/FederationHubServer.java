package tak.server.federation.hub.broker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;

import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.FederationHubUtils;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.policy.FederationHubPolicyManagerProxyFactory;

@SpringBootApplication
public class FederationHubServer implements CommandLineRunner {

    private static final String DEFAULT_CONFIG_FILE = "/opt/tak/federation-hub/configs/federation-hub-broker.yml";

    private static final Logger logger = LoggerFactory.getLogger(FederationHubServer.class);

    private static Ignite ignite = null;

    private static String configFile;

    public static void main(String[] args) {
    	
        if (args.length > 1) {
            System.err.println("Usage: java -jar federation-hub-broker.jar [CONFIG_FILE_PATH]");
            return;
        } else if (args.length == 1) {
            configFile = args[0];
        } else if (!Strings.isNullOrEmpty(System.getProperty("FEDERATION_HUB_BROKER_CONFIG"))) {
        	configFile = System.getProperties().getProperty("FEDERATION_HUB_BROKER_CONFIG");
        } else {
            configFile = DEFAULT_CONFIG_FILE;
        }

        SpringApplication application = new SpringApplication(FederationHubServer.class);

        ignite = Ignition.getOrStart(FederationHubUtils.getIgniteConfiguration(
           FederationHubConstants.FEDERATION_HUB_BROKER_IGNITE_PROFILE,
           true));
        if (ignite == null) {
            System.exit(1);
        }

        ApplicationContext context = application.run(args);
    }
    
    @Override
    public void run(String... args) throws Exception {}

    @Bean
    public FederationHubDependencyInjectionProxy dependencyProxy() {
        return new FederationHubDependencyInjectionProxy();
    }
    
    @Bean
    public FederationHubBroker federationHubBroker(Ignite ignite) {
    	FederationHubBrokerImpl hb = new FederationHubBrokerImpl();
        ClusterGroup cg = ignite.cluster().forAttribute(
            FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
            FederationHubConstants.FEDERATION_HUB_BROKER_IGNITE_PROFILE);
        ignite.services(cg).deployClusterSingleton(
            FederationHubConstants.FED_HUB_BROKER_SERVICE, hb);
        
        return hb;
    }

    @Bean
    public Ignite getIgnite() {
        return ignite;
    }

    @Bean
    public FederationHubPolicyManagerProxyFactory fedHubPolicyManagerProxyFactory() {
        return new FederationHubPolicyManagerProxyFactory();
    }

    @Bean
    public SSLConfig getSslConfig() {
        return new SSLConfig();
    }
    
    @Bean
    public HubConnectionStore hubConnectionStore() {
    	return new HubConnectionStore();
    }

    @Bean
    public FederationHubServerConfig getFedHubConfig()
            throws JsonParseException, JsonMappingException, IOException {
    	FederationHubServerConfig config = loadConfig(configFile);
    	if (Strings.isNullOrEmpty(config.getId())) {
    		config.setId(UUID.randomUUID().toString().replace("-", ""));
    		saveConfig(configFile, config);
    	}
    	    	
        return loadConfig(configFile);
    }
    
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public FederationHubBrokerService FederationHubBrokerService(Ignite ignite, SSLConfig getSslConfig, FederationHubServerConfig fedHubConfig, FederationHubPolicyManager fedHubPolicyManager, HubConnectionStore hubConnectionStore) {
    	return new FederationHubBrokerService(ignite, getSslConfig, fedHubConfig, fedHubPolicyManager, hubConnectionStore);
    }
    
    private FederationHubServerConfig loadConfig(String configFile)
            throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
        if (getClass().getResource(configFile) != null) {
            // It's a resource.
            return new ObjectMapper(new YAMLFactory()).readValue(getClass().getResourceAsStream(configFile),
                FederationHubServerConfig.class);
        }

        // It's a file.
        return new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(configFile),
            FederationHubServerConfig.class);
    }
    
    private void saveConfig(String configFile, FederationHubServerConfig config)
            throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
    	
    	ObjectMapper om = new ObjectMapper(new YAMLFactory());
    	om.writeValue(new File(configFile), config);
    }
}
