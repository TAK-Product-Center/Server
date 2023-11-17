package tak.server.federation.hub.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.logging.log4j.util.Strings;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.FederationHubUtils;
import tak.server.federation.hub.broker.FederationHubBrokerProxyFactory;
import tak.server.federation.hub.policy.FederationHubPolicyManagerProxyFactory;
import tak.server.federation.hub.ui.keycloak.KeycloakTokenParser;
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
    
	private void makeConnector(FederationHubUIConfig fedHubConfig, Server server, int port, boolean clientAuth) {
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(port);
		httpConfig.setOutputBufferSize(32768);
		httpConfig.setRequestHeaderSize(8192);
		httpConfig.setResponseHeaderSize(8192);
		httpConfig.setSendServerVersion(true);
		httpConfig.setSendDateHeader(false);

		SslContextFactory sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(fedHubConfig.getKeystoreFile());
		sslContextFactory.setKeyStorePassword(fedHubConfig.getKeystorePassword());
		sslContextFactory.setKeyStoreType(fedHubConfig.getKeystoreType());

		sslContextFactory.setTrustStorePath(fedHubConfig.getTruststoreFile());
		sslContextFactory.setTrustStorePassword(fedHubConfig.getTruststorePassword());
		sslContextFactory.setTrustStoreType(fedHubConfig.getTruststoreType());

		sslContextFactory.setNeedClientAuth(clientAuth);

		// SSL HTTP Configuration
		HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());

		// SSL Connector
		ServerConnector sslConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(httpsConfig));
		sslConnector.setPort(port);
		server.addConnector(sslConnector);
	}

    @Bean
    public ConfigurableServletWebServerFactory jettyServletFactory(FederationHubUIConfig fedHubConfig) {
      	JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
    	
    	factory.addServerCustomizers(new JettyServerCustomizer() {

			@Override
			public void customize(Server server) {
				try {
					Connector defaultConnector = server.getConnectors()[0];
					logger.info("Stopping default Jetty Connector: " + defaultConnector);
					server.getConnectors()[0].stop();
				} catch (Exception e) {}
				
				makeConnector(fedHubConfig, server, fedHubConfig.getPort(), true);
								
				if (fedHubConfig.isAllowOauth() && Strings.isNotEmpty(fedHubConfig.getKeycloakAccessTokenName()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakAdminClaimValue()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakAuthEndpoint()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakClaimName()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakClientId()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakDerLocation()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakRefreshTokenName()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakrRedirectUri()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakSecret()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakServerName()) &&
						Strings.isNotEmpty(fedHubConfig.getKeycloakTokenEndpoint()))
					makeConnector(fedHubConfig, server, fedHubConfig.getOauthPort(), false);
			}
    		
    	});
    	
    	factory.addErrorPages(new ErrorPage(HttpStatus.UNAUTHORIZED, "/login"), new ErrorPage(HttpStatus.FORBIDDEN, "/login"));
    	
        return factory;
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
    public PasswordEncoder passwordEncoder() {
    	return new BCryptPasswordEncoder();
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
    public KeycloakTokenParser keycloakTokenParser(FederationHubUIConfig getFedHubConfig) {
    	return new KeycloakTokenParser(getFedHubConfig);
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public FederationHubUIService federationHubUIService() {
    	return new FederationHubUIService();
    }
}
