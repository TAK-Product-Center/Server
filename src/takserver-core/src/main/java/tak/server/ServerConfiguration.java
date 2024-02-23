package tak.server;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import com.bbn.marti.jwt.JwtUtils;
import com.bbn.marti.remote.config.CoreConfigFacade;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.spring.SpringCacheManager;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.request.RequestContextListener;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.AltitudeConverter;
import com.bbn.marti.JDBCQueryAuditLogHelper;
import com.bbn.marti.classification.service.ClassificationService;
import com.bbn.marti.classification.service.ClassificationServiceImpl;
import com.bbn.marti.config.Cluster;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.Connection;
import com.bbn.marti.config.Network;
import com.bbn.marti.config.Oauth;
import com.bbn.marti.config.Security;
import tak.server.config.ApiConfiguration;
import tak.server.config.ApiOnlyConfiguration;
import tak.server.config.ConfigServiceConfiguration;
import tak.server.config.MessagingConfiguration;
import tak.server.config.MessagingOnlyConfiguration;
import tak.server.util.ActiveProfiles;
import com.bbn.marti.config.Tls;
import com.bbn.marti.config.Tls.Crl;
import com.bbn.marti.dao.kml.JDBCCachingKMLDao;
import com.bbn.marti.feeds.DataFeedService;
import com.bbn.marti.groups.DummyAuthenticator;
import com.bbn.marti.groups.FileAuthenticator;
import com.bbn.marti.groups.FileAuthenticatorAgent;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.OAuthAuthenticator;
import com.bbn.marti.groups.X509Authenticator;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.maplayer.MapLayerService;
import com.bbn.marti.model.kml.Icon;
import com.bbn.marti.model.kml.Iconset;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.LoggingConfigPropertiesSetupUtil;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.ClusterSubscriptionStore;
import com.bbn.marti.remote.config.LocalConfiguration;
import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.service.kml.KMLService;
import com.bbn.marti.service.kml.KMLServiceImpl;
import com.bbn.marti.service.kml.KmlIconStrategyJaxb;
import com.bbn.marti.sync.EnterpriseSyncCacheHelper;
import com.bbn.marti.sync.cache.AllCopMissionsCacheKeyGenerator;
import com.bbn.marti.sync.cache.AllMissionsCacheKeyGenerator;
import com.bbn.marti.sync.cache.InviteOnlyMissionCacheKeyGenerator;
import com.bbn.marti.sync.cache.MethodNameMultiStringArgCacheKeyGenerator;
import com.bbn.marti.sync.federation.MissionChangeAspect;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.model.MissionChanges;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.sync.model.MissionLayer;
import com.bbn.marti.sync.model.Resource;
import com.bbn.marti.sync.model.UidDetails;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.sync.repository.ExternalMissionDataRepository;
import com.bbn.marti.sync.repository.LogEntryRepository;
import com.bbn.marti.sync.repository.MissionChangeRepository;
import com.bbn.marti.sync.repository.MissionFeedRepository;
import com.bbn.marti.sync.repository.MissionInvitationRepository;
import com.bbn.marti.sync.repository.MissionLayerRepository;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.repository.MissionRoleRepository;
import com.bbn.marti.sync.repository.MissionSubscriptionRepository;
import com.bbn.marti.sync.repository.ResourceRepository;
import com.bbn.marti.sync.service.MissionPermissionEvaluator;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.sync.service.MissionServiceDefaultImpl;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.VersionBean;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.bbn.marti.util.spring.TakAuthenticationProvider;
import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.MissionServiceAspect;
import com.bbn.metrics.endpoint.NetworkMetricsEndpoint;
import com.bbn.security.web.MartiValidator;
import com.bbn.tak.tls.repository.TakCertRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;
import tak.server.cache.ActiveGroupCacheHelper;
import tak.server.cache.CoTCacheHelper;
import tak.server.cache.MissionCacheHelper;
import tak.server.cache.TakIgniteSpringCacheManager;
import tak.server.cluster.ClusterManager;
import tak.server.cluster.DistributedSubmissionService;
import tak.server.filemanager.FileManagerService;
import tak.server.filemanager.FileManagerServiceDefaultImpl;
import tak.server.ignite.IgniteConfigurationHolder;
import tak.server.ignite.IgniteHolder;
import tak.server.ignite.grid.SubscriptionManagerProxyHandler;
import tak.server.messaging.MessageConverter;
import tak.server.profile.ProfileTracker;
import tak.server.util.DataSourceUtils;
import tak.server.util.JavaVersionChecker;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@org.springframework.context.annotation.Configuration
@ImportResource({"classpath:security-context.xml"})
@Import({ApiConfiguration.class, ApiOnlyConfiguration.class, MessagingConfiguration.class, MessagingOnlyConfiguration.class})
@EnableCaching
@EnableAsync
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableJpaRepositories(basePackages= {"com.bbn.marti.sync.repository", "com.bbn.marti.dao.kml", "com.bbn.tak.tls.repository", "com.bbn.marti.device.profile.repository", "com.bbn.marti.maplayer.repository", "com.bbn.user.registration.repository", "com.bbn.marti.video", "tak.server.feeds"})
@EntityScan(basePackages={"com.bbn.marti.sync.model", "tak.server.feeds"})
@EnableTransactionManagement
public class ServerConfiguration extends SpringBootServletInitializer  {
	/*
	 * This configuration contains beans that common to all configs (See Constants.java for configurations)
	 */
	private static final Logger logger = LoggerFactory.getLogger(ServerConfiguration.class);

	private static final String SYSARG_ENABLE_IGNITE_LOGGING = "takserver.ignite.log.debug";

	private static boolean noTlsConfig = true;

	public static void main(String[] args) {
		JavaVersionChecker.check();
		// Redirect JUL logging to slf4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

		Locale.setDefault(Locale.US);

    	if (args.length > 0 && !args[0].startsWith("--")) {

			if (ActiveProfiles.getInstance().isConfigProfileActive()) {
				LocalConfiguration.CONFIG_FILE = args[0];
			}
            args = Arrays.copyOfRange(args, 1, args.length);
        }

		SpringApplication application = null;

		if (ActiveProfiles.getInstance().isConfigProfileActive())
		{
			application = new SpringApplication(ConfigServiceConfiguration.class);
			application.setWebApplicationType(WebApplicationType.NONE);
		} else {
			application = new SpringApplication(ServerConfiguration.class);
		}


		try {
			ClusterGroupDefinition.setCluster(
					IgniteConfigurationHolder.getInstance().getTAKIgniteConfiguration().isClusterEnabled());
			ClusterGroupDefinition.setProfile(ActiveProfiles.getInstance().getProfile());
		} catch (Exception e) {
			logger.error("exception initializing remote configuration", e);
			throw new TakException(e);

		}
		if (!ActiveProfiles.getInstance().isConsoleProfileActive() &&
				!System.getProperties().containsKey(SYSARG_ENABLE_IGNITE_LOGGING)) {
			disableAccessWarnings();
			disableStdout();
		}

		if (ActiveProfiles.getInstance().isConfigProfileActive()) {
			ConfigServiceConfiguration.setInitialAppProps(application);
			logger.info("Starting Config Microservice");
		} else {
			try {
				logger.info("Starting Ignite for TAK Server" + ActiveProfiles.getInstance().getProfile() + "Service.");
				Configuration configuration = CoreConfigFacade.getInstance().getRemoteConfiguration();

				setupInitialConfig(application, configuration);

				if (!configuration.getNetwork().isCloudwatchEnable()) {
					AwsCloudEnvironmentCheckUtils.setIsCloudEnvironment(false);
				}
			} catch (Exception e) {
				logger.error("exception initializing remote configuration", e);
				throw new TakException(e);
			}
		}

		@SuppressWarnings("unused")
		ApplicationContext context = application.run(args);
		logger.info("Started TAK Server " + ActiveProfiles.getInstance().getProfile() + " Microservice.");
		JavaVersionChecker.check(logger);
	}

	@Value("${takserver.compat.context-path}")
	private String compatServletPath;

	@Value("${takserver.geoid.resourcename}")
	private String geoidResource;

	@Bean
	@Profile({Constants.API_PROFILE_NAME, Constants.MONOLITH_PROFILE_NAME})
	public TomcatServletWebServerFactory containerFactory() throws RemoteException {

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

		final List<Network.Connector> connectors = config.getNetwork().getConnector();

		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

		factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/error"));

		String loginUrl = "/login";
		Oauth oauthConfig = config.getAuth().getOauth();
		if (oauthConfig != null && !oauthConfig.isUseTakServerLoginPage() && !oauthConfig.getAuthServer().isEmpty()) {
			loginUrl = "/login/auth";
		}

		factory.addErrorPages(new ErrorPage(HttpStatus.UNAUTHORIZED, loginUrl));

		factory.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN, loginUrl));

		factory.addErrorPages(new ErrorPage("/error"));

		Security security = config.getSecurity();

		if (!noTlsConfig) {
			// set up crl for all connectors
			if (security != null) {
				Tls tls = security.getTls();

				if (tls != null) {
					List<Crl> crls = tls.getCrl();

					if (crls != null && !crls.isEmpty()) {
						Crl crl = crls.get(0);

						if (crl != null) {
							if (!Strings.isNullOrEmpty(crl.getCrlFile())) {
								factory.addConnectorCustomizers(connector -> {
//									((AbstractHttp11Protocol<?>) connector.getProtocolHandler()).setCrlFile(crl.getCrlFile()); 
									AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) connector.getProtocolHandler();
									for (SSLHostConfig sSLHostConfig: protocol.findSslHostConfigs()) {
										sSLHostConfig.setCertificateRevocationListFile(crl.getCrlFile());
									}
								    
								});
							}
						}
					}
				}
			}
		}

		TomcatContextCustomizer contextCustomizer = new TomcatContextCustomizer() {
			@Override
			public void customize(Context context) {
				context.addWelcomeFile("index.html"); // this enables the <host>:8443/ and <host>:8443 redirects
				context.setWebappVersion("3.1");

				// make sure JSPs aren't run in development mode
				Container jsp = context.findChild("jsp");
				if (jsp instanceof Wrapper) {
					((Wrapper)jsp).addInitParameter("development", "false");
				}
			}
		};

		factory.addContextCustomizers(contextCustomizer);

		// skip the first connector in the list, since that will be added automatically by Spring Boot
		if (!noTlsConfig && connectors.size() > 1) {
			for (int i = 1; i < connectors.size(); i++) {
				try {
					factory.addAdditionalTomcatConnectors(configureConnector(connectors.get(i)));
				} catch (Exception e) { // if there is a problem setting up one of the connectors, still try to create the others.
					logger.warn("Exception setting up http connector " + (connectors.get(i).getName()), e);
				}
			}
		}

		// Set Tomcat base directory to /opt/tak
		try {
			File cwd = new File(Paths.get(".").toAbsolutePath().normalize().toString());

			factory.setBaseDirectory(cwd); // default to the current working directory otherwise

			logger.info("Tomcat base directory: " + cwd);
		} catch (Exception e) {
			logger.warn("Exception setting Tomcat working directory", e);
		}

		return factory;
	}

	private Connector configureConnector(Network.Connector coreConnector) {

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

		Tls tls = config.getSecurity().getTls();

		if (tls == null) {
			throw new TakException("TLS must be configured in CoreConfig");
		}

		boolean fedTruststoreExists = false;
		boolean tlsKeystoreTruststoreExist = false;

		String keystore = tls.getKeystore();
		String keystoreFile = tls.getKeystoreFile();
		String keystorePass = tls.getKeystorePass();
		String truststore = tls.getTruststore();
		String truststoreFile = tls.getTruststoreFile();
		String truststorePass = tls.getTruststorePass();

		if (coreConnector.getKeystore() != null && coreConnector.getKeystore().length() > 0) {
			keystore = coreConnector.getKeystore();
		}

		if (coreConnector.getKeystoreFile() != null && coreConnector.getKeystoreFile().length() > 0) {
			keystoreFile = coreConnector.getKeystoreFile();
		}

		if (coreConnector.getKeystorePass() != null && coreConnector.getKeystorePass().length() > 0) {
			keystorePass = coreConnector.getKeystorePass();
		}

		if (coreConnector.getTruststore() != null && coreConnector.getTruststore().length() > 0) {
			truststore = coreConnector.getTruststore();
		}

		if (coreConnector.getTruststoreFile() != null && coreConnector.getTruststoreFile().length() > 0) {
			truststoreFile = coreConnector.getTruststoreFile();
		}

		if (coreConnector.getTruststorePass() != null && coreConnector.getTruststorePass().length() > 0) {
			truststorePass = coreConnector.getTruststorePass();
		}

		try {

			File ksFile = new File(keystoreFile);
			File tsFile = new File(truststoreFile);

			if (ksFile != null && ksFile.exists()) {
				if (tsFile != null) {
					tlsKeystoreTruststoreExist = tsFile.exists();
				}
			}

		} catch (Exception e) {
			logger.warn("Exception checking tls keystore and truststore files", e);
		}

		try {

			Tls fedTls = requireNonNull(requireNonNull(config.getFederation().getFederationServer(), "CoreConfig federation section").getTls(), "federation tls section");

			File ftsFile = new File(requireNonNull(fedTls, "CoreConfig tls section").getTruststoreFile());

			if (ftsFile != null) {
				fedTruststoreExists = ftsFile.exists();
			}

		} catch (Exception e) {
			logger.warn("exception checking tls keystore and truststore files", e);
		}

		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");

		Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

		try {

			connector.setPort(coreConnector.getPort());

			if (tls != null && coreConnector.isTls()) {
				connector.setScheme("https");
				connector.setSecure(true);
				protocol.setSSLEnabled(true);
				
				SSLHostConfig sslHostConfig = new SSLHostConfig();
								
//				protocol.setClientAuth(coreConnector.getClientAuth());
				
				// Starting from Tomcat 10
				sslHostConfig.setCertificateVerification(coreConnector.getClientAuth());

				if (!config.getNetwork().getWebCiphers().isEmpty()) {
//					protocol.setCiphers(config.getNetwork().getWebCiphers());
					sslHostConfig.setCiphers(config.getNetwork().getWebCiphers());
				}
				
				SSLHostConfigCertificate sslHostConfigCertificate = new SSLHostConfigCertificate(sslHostConfig,
					SSLHostConfigCertificate.Type.UNDEFINED);

//				protocol.setKeystoreType(keystore);
				sslHostConfigCertificate.setCertificateKeystoreType(keystore);
//				protocol.setKeystoreFile(keystoreFile);
				sslHostConfigCertificate.setCertificateKeystoreFile(keystoreFile);
//				protocol.setKeystorePass(keystorePass);
				sslHostConfigCertificate.setCertificateKeystorePassword(keystorePass);
				
				sslHostConfig.addCertificate(sslHostConfigCertificate);

				if (coreConnector.getCrlFile() != null) {
//					protocol.setCrlFile(coreConnector.getCrlFile());
					sslHostConfig.setRevocationEnabled(true);
					sslHostConfig.setCertificateRevocationListFile(coreConnector.getCrlFile());
				}

				if (fedTruststoreExists && coreConnector.isUseFederationTruststore()) {
					if (config.getFederation() == null || config.getFederation().getFederationServer().getTls() == null) {
						throw new TakException("web connector specified federate truststore, but federation not enabled");
					}

					Tls fedTls = config.getFederation().getFederationServer().getTls();

//					protocol.setTruststoreType(fedTls.getTruststore());
					sslHostConfig.setTruststoreType(fedTls.getTruststore());
//					protocol.setTruststoreFile(fedTls.getTruststoreFile());
					sslHostConfig.setTruststoreFile(fedTls.getTruststoreFile());
//					protocol.setTruststorePass(fedTls.getTruststorePass());
					sslHostConfig.setTruststorePassword(fedTls.getTruststorePass());

					connector.setProperty("bindOnInit", "false");

				} else {
					if (tlsKeystoreTruststoreExist) {
						
//						protocol.setTruststoreType(truststore);
						sslHostConfig.setTruststoreType(truststore);
//						protocol.setTruststoreFile(truststoreFile);
						sslHostConfig.setTruststoreFile(truststoreFile);
//						protocol.setTruststorePass(truststorePass);
						sslHostConfig.setTruststorePassword(truststorePass);
					}
				}
//				protocol.setSslEnabledProtocols("TLSv1.2,TLSv1.3");
				sslHostConfig.setProtocols("TLSv1.2+TLSv1.3");
				
				protocol.addSslHostConfig(sslHostConfig);
				
			} else {
				connector.setScheme("http");
				connector.setSecure(false);
				protocol.setSSLEnabled(false);
			}

			return connector;

		} catch (Exception e) {
			throw new TakException("Exception configuring HTTP connector", e);
		}
	}

	@Bean
	public SpringContextBeanForApi springContextBean() {
		return new SpringContextBeanForApi();
	}

	@Bean
	AltitudeConverter altitudeConverter() {
		return new AltitudeConverter(geoidResource);
	}

	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
	    ObjectMapper mapper = new ObjectMapper();
	    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	    mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
	    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);

	    return converter;
	}

	@Bean("dummyAuthenticator")
	DummyAuthenticator dummyAuthenticator(GroupManager groupManager) {
		return new DummyAuthenticator(groupManager);
	}

	@Bean("x509Authenticator")
	X509Authenticator x509Authenticator(GroupManager groupManager, ActiveGroupCacheHelper activeGroupCacheHelper, TakCertRepository takCertRepository) {
		return new X509Authenticator(groupManager, activeGroupCacheHelper, takCertRepository);
	}

	@Bean("OauthAuthenticator")
	OAuthAuthenticator OAuthAuthenticator(GroupManager groupManager, ActiveGroupCacheHelper activeGroupCacheHelper) {
		return new OAuthAuthenticator(groupManager, activeGroupCacheHelper);
	}

	@Bean("fileAuthenticator")
	FileAuthenticator fileAuthenticator() {
		return FileAuthenticator.getInstance();
	}

	@Bean
    public FileAuthenticatorAgent fileAuthenticatorAgent() {
        return new FileAuthenticatorAgent();
    }

	@Bean
	Ignite ignite() {
		return IgniteHolder.getInstance().getIgnite();
	}

	@Bean
	CacheManager cacheManager(Ignite ignite) {
		Configuration configuration = null;

		if (ActiveProfiles.getInstance().isConfigProfileActive()) {
			configuration = LocalConfiguration.getInstance().getConfiguration();
		}
		else {
			configuration = CoreConfigFacade.getInstance().getRemoteConfiguration();
		}


		Cluster clusterConfig = configuration.getCluster();

		SpringCacheManager scm = new TakIgniteSpringCacheManager(ignite);

		if (clusterConfig.isEnabled() && clusterConfig.isKubernetes()) {

			CacheConfiguration<Object, Object> dynCacheConf = new CacheConfiguration<>();

			scm.setDynamicCacheConfiguration(dynCacheConf);

		}

		scm.setConfiguration(IgniteConfigurationHolder.getInstance().getIgniteConfiguration());

		return scm;
	}

	@Bean
    public KeyGenerator allCopsMissionsCacheKeyGenerator() {
        return new AllCopMissionsCacheKeyGenerator();
    }

	@Bean
    public KeyGenerator allMissionsCacheKeyGenerator() {
        return new AllMissionsCacheKeyGenerator();
    }

	
	@Bean
	public KeyGenerator inviteOnlyMissionsCacheKeyGenerator() {
		return new InviteOnlyMissionCacheKeyGenerator();
	}

	@Bean
    public KeyGenerator methodNameMultiStringArgCacheKeyGenerator() {
        return new MethodNameMultiStringArgCacheKeyGenerator();
    }

	private static void setupInitialConfig(SpringApplication application, Configuration config) {
		List<String> profiles = new ArrayList<String>();

		if (config.getCluster().isEnabled()) {
			profiles.add(Constants.CLUSTER_PROFILE_NAME);
		}

		// plugins are always enabled
		profiles.add(Constants.PLUGINS_ENABLED_PROFILE_NAME);
		
		application.setAdditionalProfiles(profiles.toArray(new String[0]));

		if (config.getSecurity().getTls() == null) {
			throw new TakException("TLS must be configured in CoreConfig");
		}

		Properties properties = new Properties();

		Network.Connector connector = null;

		if (ActiveProfiles.getInstance().isApiProfileActive() || ActiveProfiles.getInstance().isMonolithProfileActive()) {
			// If no connectors are defined in CoreConfig: fail startup
			if (config.getNetwork().getConnector().isEmpty()) {
				throw new TakException("No connectors defined in CoreConfig");
			}

			connector = config.getNetwork().getConnector().get(0);

			if (connector.getPort() <= 0) {
				throw new TakException("invalid connector port " + connector.getPort());
			}
		}

		boolean fedTruststoreExists = false;
		boolean tlsKeystoreTruststoreExist = false;

		Tls tls = null;

		try {

			tls = config.getSecurity().getTls();

			File ksFile = new File(tls.getKeystoreFile());
			File tsFile = new File(tls.getTruststoreFile());

			if (ksFile != null && ksFile.exists()) {
				if (tsFile != null) {
					tlsKeystoreTruststoreExist = tsFile.exists();
					noTlsConfig = false;
				}
			}

		} catch (Exception e) {
			logger.warn("exception checking tls keystore and truststore files", e);
		}

		try {

			File ftsFile = new File(tls.getTruststoreFile());

			if (ftsFile != null) {
				fedTruststoreExists = ftsFile.exists();
			}

		} catch (Exception e) {
			logger.warn("exception checking tls keystore and truststore files", e);
		}

		if (connector != null) {
			if (noTlsConfig) {
				// default to 8080 if tls config is not valid
				properties.put("server.port", 8080);
			} else {
				// use the first connector as the spring boot-defined connector
				properties.put("server.port", connector.getPort());
			}
		}

		if ((ActiveProfiles.getInstance().isApiProfileActive() || ActiveProfiles.getInstance().isMonolithProfileActive()) && tlsKeystoreTruststoreExist && connector.isTls()) {

			properties.put("server.ssl.client-auth", connector.getClientAuth().toLowerCase(Locale.ENGLISH).equals("true") ? "NEED" : connector.getClientAuth());
			properties.put("server.ssl.key-store", tls.getKeystoreFile());
			properties.put("server.ssl.key-store-password", tls.getKeystorePass());
			properties.put("server.ssl.key-store-type", tls.getKeystore());
			properties.put("server.ssl.enabled-protocols", "TLSv1.2, TLSv1.3");

			if (!config.getNetwork().getWebCiphers().isEmpty()) {
				properties.put("server.ssl.ciphers", config.getNetwork().getWebCiphers());
			}
		}

		if (config.getNetwork().getTomcatMaxPool() > -1) {
			properties.put("server.tomcat.threads.max", config.getNetwork().getTomcatMaxPool());
			
			if (config.getNetwork().isTomcatPoolIdleToMax()) {
				properties.put("server.tomcat.threads.min-spare", config.getNetwork().getTomcatMaxPool());
			}

			System.out.println("Tomcat explicit worker pool size: " + config.getNetwork().getTomcatMaxPool());

		} else {
			
			int tomcatPoolSize = Runtime.getRuntime().availableProcessors() * config.getNetwork().getTomcatPoolMultiplier();
			
			properties.put("server.tomcat.threads.max", tomcatPoolSize);
			
			if (config.getNetwork().isTomcatPoolIdleToMax()) {
				properties.put("server.tomcat.threads.min-spare", tomcatPoolSize);
			}
			
			System.out.println("Tomcat computed worker pool size: " + tomcatPoolSize);

		}

		Connection coreDbConnection = config.getRepository().getConnection();

		requireNonNull(coreDbConnection.getUrl(), "CoreConfig db url");
		if (!coreDbConnection.isSslEnabled()) {
			requireNonNull(requireNonNull(coreDbConnection, "CoreConfig db connection").getUsername(), "CoreConfig db username");
			requireNonNull(coreDbConnection.getPassword(), "CoreConfig db connection password");	
		}else {
			requireNonNull(coreDbConnection.getSslCert(), "CoreConfig db connection ssl cert");
			requireNonNull(coreDbConnection.getSslKey(), "CoreConfig db connection ssl key");
		}

		// http session timeout
		properties.put("server.session.timeout", config.getNetwork().getHttpSessionTimeoutMinutes() * 60);
		properties.put("server.servlet.session.cookie.max-age", config.getNetwork().getHttpSessionTimeoutMinutes() * 60);

		// cloudwatch

		if (config.getNetwork().isCloudwatchEnable()) {


			String serverName = config.getNetwork().getCloudwatchName();
			if (Strings.isNullOrEmpty(serverName)) {
				serverName = config.getNetwork().getServerId();
			}

			if (Strings.isNullOrEmpty(serverName)) {
				serverName = UUID.randomUUID().toString();
			}

			String fullNamespace = config.getNetwork().getCloudwatchNamespace() + "-" + serverName + "-" + (ActiveProfiles.getInstance().isMessagingProfileActive() ? "messaging" : "api");

			properties.put("management.metrics.export.cloudwatch.namespace", fullNamespace);

			logger.info("AWS CloudWatch metrics namespace: " + fullNamespace);

			properties.put("management.metrics.export.cloudwatch.batchSize", config.getNetwork().getCloudwatchMetricsBatchSize());

		} else {
			properties.put("cloud.aws.region.auto", false);
			properties.put("cloud.aws.region.static", "us-east-1");
		}

		properties.put("cloud.aws.stack.auto", false); // make this configurable?

		LoggingConfigPropertiesSetupUtil.getInstance().setupLoggingProperties(config);

		if (tls != null && fedTruststoreExists && connector != null && connector.isUseFederationTruststore()) {
			if (config.getFederation() == null || config.getFederation().getFederationServer().getTls() == null) {
				throw new TakException("web connector specified federate truststore, but federation not enabled");
			}

			Tls fedTls = config.getFederation().getFederationServer().getTls();

			properties.put("server.ssl.trust-store", fedTls.getTruststoreFile());
			properties.put("server.ssl.trust-store-password", fedTls.getTruststorePass());
			properties.put("server.ssl.trust-store-type", fedTls.getTruststore());
		} else {
			if (tlsKeystoreTruststoreExist) {
				properties.put("server.ssl.trust-store", tls.getTruststoreFile());
				properties.put("server.ssl.trust-store-password", tls.getTruststorePass());
				properties.put("server.ssl.trust-store-type", tls.getTruststore());
			}
		}

		try {
			properties.put("takserver.iconsets.dir", config.getRepository().getIconsetDir());
		} catch (Exception e) {
			logger.warn("exception setting iconset directory location option from CoreConfig", e);
		}

		if (ActiveProfiles.getInstance().isMessagingProfileActive()) {
			properties.put("server.ssl.enabled", false);
		}
		// make static content available
		try {
			properties.put("spring.web.resources.static-locations", "file:" + config.getNetwork().getExtWebContentDir() + ",classpath:/META-INF/resources/,classpath:/resources/,classpath:/static/,classpath:/public/");
		} catch (Exception e) {
			logger.warn("exception setting webcontent directory location option", e);
		}
		// if other connectors are defined, they will be set up as additional connectors
		application.setDefaultProperties(properties);
	}

	@Bean
	public HikariDataSource dataSource() {
        return DataSourceUtils.setupDataSourceFromCoreConfig();
        
	}

	@Bean
	public GroupFederationUtil groupFederationUtil() {
		return new GroupFederationUtil();
	}

	@Bean
	protected RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}

	@Bean
	public SubmissionInterface submission() {
		return new DistributedSubmissionService();
	}

	// RepositoryService and its dependencies are used by API and messaging
	@Bean
	RepositoryService repositoryService() {
		return new RepositoryService();
	}

	@Bean
	MartiValidator validator() {
		return new MartiValidator();
	}

	@Bean
	JDBCQueryAuditLogHelper jDBCQueryAuditLogHelper() {
		return new JDBCQueryAuditLogHelper();
	}

	@Bean
	VersionBean versionBean() {
		return new VersionBean();
	}

	@Bean
	CommonUtil commonUtil() {
		return new CommonUtil();
	}

	@Bean
	TakAuthenticationProvider takAuthenticationProvider() {
		return new TakAuthenticationProvider();
	}

	@Bean
	@Profile(Constants.CLUSTER_PROFILE_NAME)
	SubscriptionStore clusterSubscriptionStore() {
		return new ClusterSubscriptionStore();
	}

	@Bean
	@Profile("!" + Constants.CLUSTER_PROFILE_NAME)
	SubscriptionStore subscriptionStore() {
		return new SubscriptionStore();
	}

	@Bean
	ProfileTracker profileTracker() {
		return new ProfileTracker();
	}

	@Bean
	AuditLogUtil auditLogUtil() {
		return new AuditLogUtil();
	}

	@Bean
	MessageConverter clusterMessageConverter() {
		return new MessageConverter();
	}

	@Bean
	@Profile(Constants.CLUSTER_PROFILE_NAME)
	ClusterManager clusterManager(CacheManager cacheManager, MessageConverter clusterMessageConverter) {
		return new ClusterManager(cacheManager, clusterMessageConverter);
	}

	@Bean
	RemoteUtil remoteUtil() {
		return new RemoteUtil();
	}

	@Bean
	MapLayerService mapLayerService() { return new MapLayerService(); }
	
	@Bean
	public FileManagerService fileManagerService(DataSource dataSource) {
		return new FileManagerServiceDefaultImpl(dataSource);
	}

	@Bean
	public SubscriptionManagerProxyHandler subscriptionManagerProxyHandler() {
		return new SubscriptionManagerProxyHandler();
	}

	// Used by messaging and API processes
	@Bean
	MissionService missionService(
			DataSource dataSource,
			MissionRepository missionRepository,
			MissionChangeRepository missionChangeRepository,
			ResourceRepository resourceRepository,
			LogEntryRepository logEntryRepository,
			SubscriptionManagerLite subscriptionManager,
			SubscriptionManagerProxyHandler subscriptionManagerProxy,
			RemoteUtil remoteUtil,
			Marshaller marshaller,
			com.bbn.marti.sync.EnterpriseSyncService syncStore,
			JDBCCachingKMLDao kmlDao,
			KMLService kmlService,
			SubmissionInterface submission,
			ExternalMissionDataRepository externalMissionDataRepository,
			MissionInvitationRepository missionInvitationRepository,
			MissionSubscriptionRepository missionSubscriptionRepository,
			MissionFeedRepository missionFeedRepository,
			DataFeedRepository dataFeedRepository,
			MissionLayerRepository missionLayerRepository,
			MapLayerService mapLayerService,
			GroupManager groupManager,
			CacheManager cacheManager,
			CommonUtil commonUtil,
			MissionRoleRepository missionRoleRepository,
			CoTCacheHelper cotCacheHelper,
			MissionCacheHelper missionCacheHelper,
			ClassificationService classificationService,
			FileManagerService fileManagerService) {
		return new MissionServiceDefaultImpl(
				dataSource,
	    		missionRepository,
	    		missionChangeRepository,
	    		resourceRepository,
	    		logEntryRepository,
	    		subscriptionManager,
	    		subscriptionManagerProxy,
	    		remoteUtil,
	    		marshaller,
	    		syncStore,
	    		kmlDao,
	    		kmlService,
	    		submission,
	    		externalMissionDataRepository,
	    		missionInvitationRepository,
	    		missionSubscriptionRepository,
	    		missionFeedRepository,
	    		dataFeedRepository,
	    		missionLayerRepository,
	    		mapLayerService,
	    		groupManager,
	    		cacheManager,
	    		commonUtil,
	    		missionRoleRepository,
	    		cotCacheHelper,
	    		missionCacheHelper,
				classificationService,
				fileManagerService);
	}
	
	@Bean
	DataFeedService dataFeedService(DataSource dataSource, DataFeedRepository dataFeedRepository) {
		return new DataFeedService(dataSource, dataFeedRepository);
	}

	@Bean("kmlDao")
	public JDBCCachingKMLDao kmlDao() {
		return new JDBCCachingKMLDao();
	}

	@Bean
	public MissionChange missionChange() {
		return new MissionChange();
	}

	@Bean
	public MissionFeed missionFeed() {
		return new MissionFeed();
	}

	@Bean
	public PermissionEvaluator missionPermissionEvaluator() {
		return new MissionPermissionEvaluator();
	}

	// may be able to use the interface here instead of implementation
	@Bean
	public KMLServiceImpl kmlServiceImpl() {
		return new KMLServiceImpl();
	}

	@Bean
	public KmlIconStrategyJaxb kmlIconStrategyJaxb() {
		return new KmlIconStrategyJaxb();
	}

	@SuppressWarnings("unchecked")
	public static void disableAccessWarnings() {
		try {
			Class unsafeClass = Class.forName("sun.misc.Unsafe");
			Field field = unsafeClass.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			Object unsafe = field.get(null);

			Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
			Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

			Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
			Field loggerField = loggerClass.getDeclaredField("logger");
			Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
			putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
		} catch (Exception ignored) {
		}
	}

	public static void disableStdout() {
		System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
			@Override
			public void write(int b) {
			}
		}) {
			@Override
			public void flush() {
			}

			@Override
			public void close() {
			}

			@Override
			public void write(int b) {
			}

			@Override
			public void write(byte[] b) {
			}

			@Override
			public void write(byte[] buf, int off, int len) {
			}

			@Override
			public void print(boolean b) {
			}

			@Override
			public void print(char c) {
			}

			@Override
			public void print(int i) {
			}

			@Override
			public void print(long l) {
			}

			@Override
			public void print(float f) {
			}

			@Override
			public void print(double d) {
			}

			@Override
			public void print(char[] s) {
			}

			@Override
			public void print(String s) {
			}

			@Override
			public void print(Object obj) {
			}

			@Override
			public void println() {
			}

			@Override
			public void println(boolean x) {
			}

			@Override
			public void println(char x) {
			}

			@Override
			public void println(int x) {
			}

			@Override
			public void println(long x) {
			}

			@Override
			public void println(float x) {
			}

			@Override
			public void println(double x) {
			}

			@Override
			public void println(char[] x) {
			}

			@Override
			public void println(String x) {
			}

			@Override
			public void println(Object x) {
			}

			@Override
			public java.io.PrintStream printf(String format, Object... args) {
				return this;
			}

			@Override
			public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) {
				return this;
			}

			@Override
			public java.io.PrintStream format(String format, Object... args) {
				return this;
			}

			@Override
			public java.io.PrintStream format(java.util.Locale l, String format, Object... args) {
				return this;
			}

			@Override
			public java.io.PrintStream append(CharSequence csq) {
				return this;
			}

			@Override
			public java.io.PrintStream append(CharSequence csq, int start, int end) {
				return this;
			}

			@Override
			public java.io.PrintStream append(char c) {
				return this;
			}
		});
	}

	@Bean(Constants.TAKMESSAGE_MAPPER)
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true);
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

		return mapper;
	}

	@Bean
	public CoTCacheHelper cotCacheHelper() {
		return new CoTCacheHelper();
	}

	@Bean
	public MissionCacheHelper missionCacheHelper() {
		return new MissionCacheHelper();
	}

	@Bean
	public ClassificationService classificationService() { return new ClassificationServiceImpl(); }

	@Bean
	public MissionServiceAspect missionServiceAspect() {
		return new MissionServiceAspect();
	}
	
	@Bean
	public MissionChangeAspect missionChangeAspect() {
		return new MissionChangeAspect();
	}
	
	@Bean
	public NetworkMetricsEndpoint networkMetrics(@Lazy MetricsCollector metricsCollector, SubscriptionManager subscriptionManager) {
		return new NetworkMetricsEndpoint(metricsCollector, subscriptionManager);
	}
	
	@Bean
	public EnterpriseSyncCacheHelper enterpriseSyncCacheHelper() {
		return new EnterpriseSyncCacheHelper();
	}
	
	@Bean
	@Lazy
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Lazy DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
		bean.setDataSource(dataSource);
		bean.setPersistenceUnitName("martiPersistenceUnit");
		bean.setPersistenceProviderClass(HibernatePersistenceProvider.class);

		Map<String, Object> jpaProperty = new HashMap<>();
		jpaProperty.put("hibernate.connection.driver_class", org.postgresql.Driver.class);
		jpaProperty.put("hibernate.hbm2ddl.auto", "");
		jpaProperty.put("hibernate.cache.use_query_cache", false);
		jpaProperty.put("hibernate.cache.use_second_level_cache", false);
		jpaProperty.put("hibernate.dialect", org.hibernate.dialect.PostgreSQLDialect.class);
		jpaProperty.put("jakarta.persistence.sharedCache.mode", "ENABLE_SELECTIVE");
		jpaProperty.put("hibernate.enable_lazy_load_no_trans", true);
		bean.setJpaPropertyMap(jpaProperty);

		bean.setJpaDialect(new HibernateJpaDialect());

		bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

		bean.setPackagesToScan("com.bbn.marti.model.kml", "com.bbn.marti.sync.model", "com.bbn.tak.tls",
				"com.bbn.marti.device.profile.model", "com.bbn.marti.maplayer.model", "com.bbn.user.registration.model",
				"com.bbn.marti.video", "tak.server.feeds");

		return bean;
	}

	@Bean
	public PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
		return new PersistenceAnnotationBeanPostProcessor();
	}

	@Bean
	@Scope("prototype")
	public Jaxb2Marshaller jaxb2Marshaller() {
		
		Jaxb2Marshaller bean = new Jaxb2Marshaller();
		bean.setClassesToBeBound(Iconset.class, Icon.class, MissionChanges.class, MissionChange.class, MissionLayer.class, UidDetails.class, Resource.class);
		
		return bean;

	}
	
	@Bean
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager bean = new JpaTransactionManager(entityManagerFactory);
		return bean;
	}
	
	@Bean("mvcConversionService")
	public FormattingConversionServiceFactoryBean mvcConversionService(){
	  return new FormattingConversionServiceFactoryBean();
	}

	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		RSAPublicKey publicKey = (RSAPublicKey) JwtUtils.getInstance().getPublicKey();
		RSAPrivateKey privateKey = (RSAPrivateKey) JwtUtils.getInstance().getPrivateKey();
		com.nimbusds.jose.jwk.RSAKey rsaKey = new com.nimbusds.jose.jwk.RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.build();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return new ImmutableJWKSet<SecurityContext>(jwkSet);
	}
}
