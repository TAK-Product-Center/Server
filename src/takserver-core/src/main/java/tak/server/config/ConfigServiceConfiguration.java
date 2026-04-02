package tak.server.config;

import java.util.Properties;
import java.util.UUID;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.config.DistributedConfiguration;
import com.bbn.marti.remote.config.LocalConfiguration;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;
import tak.server.ignite.IgniteReconnectEventHandler;
import tak.server.util.ActiveProfiles;


@Configuration
@Profile(Constants.CONFIG_PROFILE_NAME)
@SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
public class ConfigServiceConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(ConfigServiceConfiguration.class);

	@Bean
	public LocalConfiguration getLocalConfiguration() {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting up local and ignite configuration.");
		}
		return LocalConfiguration.getInstance();
	}

	@Bean
	Ignite ignite() {
		if (logger.isDebugEnabled()) {
			logger.debug("Starting ignite.");
		}
		return IgniteHolder.getInstance().getIgnite();
	}
	@Bean(Constants.DISTRIBUTED_CONFIGURATION)
	public DistributedConfiguration getDistributedConfiguration(Ignite ignite) {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting up distributed configuration.");
		}
		DistributedConfiguration distributedConfiguration = DistributedConfiguration.getInstance();

		if (logger.isDebugEnabled()) {
			logger.debug("Adding distributed configuration to ignite");
		}
		
		Runnable serviceDeployment = () -> ignite.services(ClusterGroupDefinition.getConfigClusterDeploymentGroup(ignite)).deployNodeSingleton(
				Constants.DISTRIBUTED_CONFIGURATION, distributedConfiguration);
		
		IgniteReconnectEventHandler.registerService(serviceDeployment);

		return distributedConfiguration;
	}

	public static void setInitialAppProps(SpringApplication application){
		Properties properties = new Properties();
		LocalConfiguration config = LocalConfiguration.getInstance();

		if (config.getConfiguration().getNetwork().isCloudwatchEnable()) {
			String serverName = config.getConfiguration().getNetwork().getCloudwatchName();
			if (Strings.isNullOrEmpty(serverName)) {
				serverName = config.getConfiguration().getNetwork().getServerId();
			}

			if (Strings.isNullOrEmpty(serverName)) {
				serverName = UUID.randomUUID().toString();
			}

			String fullNamespace = config.getConfiguration().getNetwork().getCloudwatchNamespace() + "-" + serverName + "-" + (ActiveProfiles.getInstance().isMessagingProfileActive() ? "messaging" : "api");

			properties.put("management.metrics.export.cloudwatch.namespace", fullNamespace);

			logger.info("AWS CloudWatch metrics namespace: " + fullNamespace);

			properties.put("management.metrics.export.cloudwatch.batchSize", config.getConfiguration().getNetwork().getCloudwatchMetricsBatchSize());

		} else {
			properties.put("cloud.aws.region.auto", false);
			properties.put("cloud.aws.region.static", "us-east-1");
		}

		properties.put("cloud.aws.stack.auto", false); // make this configurable?
		
		try {
			properties.put("takserver.iconsets.dir", config.getConfiguration().getRepository().getIconsetDir());
		} catch (Exception e) {
			logger.warn("exception setting iconset directory location option from CoreConfig", e);
		}

		application.setDefaultProperties(properties);
	}

}