package tak.server.federation.hub.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.FederationHubUtils;
import tak.server.federation.hub.broker.FederationHubBrokerProxyFactory;
import tak.server.federation.hub.policy.FederationHubPolicyManagerProxyFactory;
import tak.server.federation.hub.ui.manage.AuthorizationFileWatcher;

@SpringBootApplication
public class FederationHubUIServer {

    private static final String DEFAULT_CONFIG_FILE = "/opt/tak/federation-hub/configs/federation-hub-ui.yml";

    private static final Logger logger = LoggerFactory.getLogger(FederationHubUIServer.class);

    private static Ignite ignite = null;

    private static String configFile;


    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -jar federation-hub-ui.jar [CONFIG_FILE_PATH]");
            return;
        } else if (args.length == 1) {
            configFile = args[0];
        } else {
            configFile = DEFAULT_CONFIG_FILE;
        }

        SpringApplication application = new SpringApplication(FederationHubUIServer.class);

        ignite = Ignition.getOrStart(FederationHubUtils.getIgniteConfiguration(
            FederationHubConstants.FEDERATION_HUB_UI_IGNITE_PROFILE,
            true));
        if (ignite == null) {
            System.exit(1);
        }

        setupInitialConfig(application);

        ApplicationContext context = application.run(args);
    }

    private static void setupInitialConfig(SpringApplication application) {
        List<String> profiles = new ArrayList<String>();
        application.setAdditionalProfiles(profiles.toArray(new String[0]));
        Properties properties = new Properties();
        properties.put("cloud.aws.region.auto", false);
        properties.put("cloud.aws.region.static", "us-east-1");
        properties.put("cloud.aws.stack.auto", false);
        application.setDefaultProperties(properties);
    }

    @Bean
    public Ignite getIgnite() {
        return ignite;
    }

    @Bean
    public FederationHubBrokerProxyFactory fedHubBrokerProxyFactory() {
        return new FederationHubBrokerProxyFactory();
    }

    @Bean
    public FederationHubPolicyManagerProxyFactory fedHubPolicyManagerProxyFactory() {
        return new FederationHubPolicyManagerProxyFactory();
    }

    @Bean
    public ConfigurableServletWebServerFactory jettyServletFactory(FederationHubUIConfig fedHubConfig) {
        return new JettyServletWebServerFactory(fedHubConfig.getPort());
    }

    private FederationHubUIConfig loadConfig(String configFile)
            throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
        if (getClass().getResource(configFile) != null) {
            // It's a resource.
            return new ObjectMapper(new YAMLFactory()).readValue(getClass().getResourceAsStream(configFile),
                FederationHubUIConfig.class);
        }

        // It's a file.
        return new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(configFile),
            FederationHubUIConfig.class);
    }

    @Bean
    public FederationHubUIConfig getFedHubConfig()
            throws JsonParseException, JsonMappingException, IOException {
        return loadConfig(configFile);
    }

    @Bean
    public AuthorizationFileWatcher authFileWatcher(FederationHubUIConfig fedHubConfig) {
        AuthorizationFileWatcher authFileWatcher = new AuthorizationFileWatcher(fedHubConfig);
        try {
            authFileWatcher.start();
        } catch (IOException e) {
            logger.error("Could not start watch service on authorization file: " + e);
            return null;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(authFileWatcher::stop));
        return authFileWatcher;
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public FederationHubUIService FederationHubBrokerService() {
    	return new FederationHubUIService();
    }
}
