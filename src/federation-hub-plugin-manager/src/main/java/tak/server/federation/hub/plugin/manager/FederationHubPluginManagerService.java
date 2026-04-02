package tak.server.federation.hub.plugin.manager;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.google.common.base.Strings;

import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.FederationHubUtils;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class FederationHubPluginManagerService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubPluginManagerService.class);
    
    private static final String DEFAULT_CONFIG_FILE = "/opt/tak/federation-hub/plugins/lifecycle-config.yml";
    
    String configFile = DEFAULT_CONFIG_FILE;

    private static Ignite ignite = null;
    
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(FederationHubPluginManagerService.class);

        ignite = Ignition.getOrStart(FederationHubUtils.getIgniteConfiguration(
           FederationHubConstants.FEDERATION_HUB_PLUGIN_MANAGER_IGNITE_PROFILE,
           true));
        if (ignite == null) {
            System.exit(1);
        }

        ApplicationContext context = application.run(args);
    }

    @Bean
    public Ignite getIgnite() {
        return ignite;
    }

    @Override
    public void run(String... args) throws Exception {
    	if (!Strings.isNullOrEmpty(System.getProperty("FEDERATION_HUB_PLUGIN_MANAGER_CONFIG"))) {
    		configFile = System.getProperties().getProperty("FEDERATION_HUB_PLUGIN_MANAGER_CONFIG");
        }
    	
    	FederationHubPluginManagerImpl hpm = new FederationHubPluginManagerImpl(ignite, configFile);
        ClusterGroup cg = ignite.cluster().forAttribute(
            FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
            FederationHubConstants.FEDERATION_HUB_PLUGIN_MANAGER_IGNITE_PROFILE);
        
        ignite.services(cg).deployNodeSingleton(
            FederationHubConstants.FED_HUB_PLUGIN_MANAGER_SERVICE, hpm);
    }
}
