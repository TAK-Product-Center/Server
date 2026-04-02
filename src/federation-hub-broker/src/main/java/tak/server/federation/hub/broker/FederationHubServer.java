package tak.server.federation.hub.broker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.FederationHubUtils;
import tak.server.federation.hub.broker.db.FederationHubDatabaseService;
import tak.server.federation.hub.broker.db.FederationHubDatabaseServiceImpl;
import tak.server.federation.hub.db.FederationHubDatabase;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.policy.FederationHubPolicyManagerProxyFactory;
import tak.server.federation.hub.broker.FederationHubBrokerMetricsPoller;
import tak.server.federation.hub.broker.db.FederationHubMissionDisruptionManager;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class})
@EnableCaching
public class FederationHubServer implements CommandLineRunner {

	private static final String DEFAULT_CONFIG_FILE = "/opt/tak/federation-hub/configs/federation-hub-broker.yml";
	
	private static boolean isCloudwatchEnable = false;

	private static final Logger logger = LoggerFactory.getLogger(FederationHubServer.class);

	private static Ignite ignite = null;

	private static String configFile;

	public static void main(String[] args) throws FileNotFoundException, IOException {
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
		
		FederationHubServerConfigManager manager = new FederationHubServerConfigManager(configFile);
		isCloudwatchEnable = manager.getConfig().isCloudwatchEnable();

		SpringApplication application = new SpringApplication(FederationHubServer.class);
		
		ignite = Ignition.getOrStart(FederationHubUtils
				.getIgniteConfiguration(FederationHubConstants.FEDERATION_HUB_BROKER_IGNITE_PROFILE, true));
		if (ignite == null) {
			System.exit(1);
		}

		ApplicationContext context = application.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
	}

	@Bean
	public FederationHubDependencyInjectionProxy dependencyProxy() {
		return new FederationHubDependencyInjectionProxy();
	}

	@Bean
	public FederationHubBroker federationHubBroker(Ignite ignite) {
		FederationHubBrokerImpl hb = new FederationHubBrokerImpl();
		ClusterGroup cg = ignite.cluster().forAttribute(FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
				FederationHubConstants.FEDERATION_HUB_BROKER_IGNITE_PROFILE);
		
		ignite.services(cg).deployNodeSingleton(FederationHubConstants.FED_HUB_BROKER_SERVICE, hb);
	
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
	public FederationHubBrokerMetrics federationHubBrokerMetrics() {
		return new FederationHubBrokerMetrics();
	}

	@Bean
	public FederationHubBrokerGlobalMetrics federationHubBrokerGlobalMetrics() { return new FederationHubBrokerGlobalMetrics(); }

	@Bean
	public FederationHubServerConfigManager getFedHubConfig() throws JsonParseException, JsonMappingException, IOException {
		return new FederationHubServerConfigManager(configFile);
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public FederationHubBrokerService FederationHubBrokerService(Ignite ignite, SSLConfig getSslConfig,
			FederationHubServerConfigManager fedHubConfigManager, FederationHubPolicyManager fedHubPolicyManager,
			HubConnectionStore hubConnectionStore, FederationHubMissionDisruptionManager federationHubMissionDisruptionManager,
		 	FederationHubBrokerMetrics fedHubBrokerMetrics, FederationHubBrokerGlobalMetrics fedHubBrokerGlobalMetrics,
																 ActuatorMetricsService actuatorMetricsService) {

		return new FederationHubBrokerService(ignite, getSslConfig, fedHubConfigManager,
				fedHubPolicyManager, hubConnectionStore, federationHubMissionDisruptionManager,
				fedHubBrokerMetrics, fedHubBrokerGlobalMetrics, actuatorMetricsService);
	}

	@Bean
	public CacheManager cacheManager() {
		Caffeine<Object, Object>  caffeine = Caffeine.newBuilder()
				.initialCapacity(100)
				.maximumSize(150)
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.recordStats();

		CaffeineCacheManager cacheManager = new CaffeineCacheManager("federate-connect-disconnect", "federate_metadata");
		cacheManager.setAllowNullValues(true); // can happen if you get a value from a @Cachable that returns null
		cacheManager.setCaffeine(caffeine);
		return cacheManager;
	}

	@Bean
	public FederationHubMissionDisruptionManager federationHubMissionDisruptionManager(FederationHubDatabaseService federationHubDatabaseService) {
		return new FederationHubMissionDisruptionManager(federationHubDatabaseService);
	}

	@Bean
	public FederationHubDatabaseService HubDataBaseService(FederationHubDatabase federationHubDatabase, CacheManager cacheManager) {
		return new FederationHubDatabaseServiceImpl(federationHubDatabase, cacheManager);
	}

	@Bean
	public FederationHubDatabase federationHubDatabase(FederationHubServerConfigManager fedHubConfigManager) {
		FederationHubServerConfig fedHubConfig = fedHubConfigManager.getConfig();
		
		if (fedHubConfig.isMissionFederationDisruptionEnabled()) {
			return new FederationHubDatabase(fedHubConfig.getDbUsername(), fedHubConfig.getDbPassword(),
					fedHubConfig.getDbHost(), fedHubConfig.getDbPort());
		} else {
			return new FederationHubDatabase();
		}
	}
	
	@Bean
	@Conditional(IsCloudWatchCondition.class)
    public FederationHubBrokerMetricsPoller federationHubBrokerMetricsPoller(MeterRegistry meterRegistry) {
        return new FederationHubBrokerMetricsPoller();
    }

	@Bean
	@Conditional(IsCloudWatchCondition.class)
	public FederationHubBrokerGlobalMetricsPoller federationHubBrokerGlobalMetricsPoller(MeterRegistry meterRegistry) {
		return new FederationHubBrokerGlobalMetricsPoller();
	}
	
	@Bean
	@Conditional(IsCloudWatchCondition.class)
	public CloudWatchAsyncClient cloudWatchAsyncClient() {
		return CloudWatchAsyncClient.create();
	}

	@Bean
	@Conditional(IsCloudWatchCondition.class)
	public MeterRegistry meterRegistry(FederationHubServerConfigManager fedHubConfigManager) {
		CloudWatchConfig cloudWatchConfig = setupCloudWatchConfig(fedHubConfigManager);

		CloudWatchMeterRegistry cloudWatchMeterRegistry = 
				new CloudWatchMeterRegistry(
						cloudWatchConfig, 
						Clock.SYSTEM,
						cloudWatchAsyncClient());

		return cloudWatchMeterRegistry;
	}

	
	private static CloudWatchConfig setupCloudWatchConfig(FederationHubServerConfigManager fedHubConfigManager) {
		String fullNamespace = fedHubConfigManager.getConfig().getCloudwatchNamespace() + "-" + fedHubConfigManager.getConfig().getId() + "-broker";

		int batchSize = fedHubConfigManager.getConfig().getCloudwatchMetricsBatchSize();

		CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {

			private Map<String, String> configuration = Map.of(
					"cloudwatch.namespace", fullNamespace,
					"cloudwatch.step", Duration.ofSeconds(fedHubConfigManager.getConfig().getCloudwatchStepSeconds()).toString(),
					"cloudwatch.batchSize", Integer.toString(batchSize));

			@Override
			public String get(String key) {
				return configuration.get(key);
			}
		};
		return cloudWatchConfig;
	}
	
	private static final class IsCloudWatchCondition implements Condition {
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return isCloudwatchEnable;
		}
	}
}