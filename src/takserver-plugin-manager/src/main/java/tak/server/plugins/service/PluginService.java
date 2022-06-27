package tak.server.plugins.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.ServerInfo;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.Constants;
import tak.server.PluginManager;
import tak.server.PluginRegistry;
import tak.server.ignite.IgniteConfigurationHolder;
import tak.server.messaging.Messenger;
import tak.server.plugins.PluginManagerConstants;
import tak.server.plugins.PluginStarter;
import tak.server.plugins.manager.loader.PluginLoader;
import tak.server.plugins.messaging.MessageConverter;
import tak.server.plugins.messaging.PluginClusterMessenger;
import tak.server.plugins.messaging.PluginMessenger;
import tak.server.plugins.util.PluginManagerDependencyInjectionProxy;

@SpringBootApplication
public class PluginService implements CommandLineRunner {
	
	private static Ignite ignite = null;
	
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(PluginService.class);
		
		boolean isK8Cluster = Arrays.stream(System.getProperties().getProperty("spring.profiles.active", "").split(","))
				.anyMatch("k8cluster"::equals);
		
		boolean isLocalCluster = Arrays.stream(System.getProperties().getProperty("spring.profiles.active", "").split(","))
				.anyMatch("localcluster"::equals);
		
		boolean isCluster = isK8Cluster || isLocalCluster;
		
		if (isCluster) {
			List<String> profiles = new ArrayList<String>();
			profiles.add(Constants.CLUSTER_PROFILE_NAME);
			application.setAdditionalProfiles(profiles.toArray(new String[0]));
		}
		
		ignite =  Ignition.getOrStart(IgniteConfigurationHolder.getInstance().getIgniteConfiguration(PluginManagerConstants.PLUGIN_MANAGER_IGNITE_PROFILE, "127.0.0.1", isCluster, isK8Cluster, false, false, 47500, 100, 47100, 100, 512, 600000, 52428800, 52428800));
		
		if (ignite == null) {
			System.exit(1);
		}
		
		// start sping boot app
		application.run(args);
    }
	
	@Override
	public void run(String... args) throws Exception { }
	
	@Bean
	@Profile("!" + Constants.CLUSTER_PROFILE_NAME)
	PluginStarter pluginIntializer(Ignite ignite) {
		return new PluginStarter("", "");
	}
	
	@Bean
	@Profile(Constants.CLUSTER_PROFILE_NAME)
	PluginStarter pluginClusterIntializer(Ignite ignite) {
		ServerInfo serverInfo = ignite.services(ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME))
				.serviceProxy(Constants.DISTRIBUTED_SERVER_INFO, ServerInfo.class, false);
		
		return new PluginStarter(serverInfo.getNatsURL(), serverInfo.getNatsClusterId());
	}
	
	@Bean
	PluginRegistry registrar() {
		return new CachingPluginRegistry();
	}
	
	@Bean
	PluginLoader pluginLoader(PluginRegistry registrar) {
		return new PluginLoader(registrar);
	}
	
	@Bean
	MessageConverter messageConverter() {
		return new MessageConverter();
	}
	
	@Bean
	@Profile("!" + Constants.CLUSTER_PROFILE_NAME)
	Messenger<Message> pluginMessenger() {
		return new PluginMessenger();
	}
	
	@Bean
	@Profile(Constants.CLUSTER_PROFILE_NAME)
	Messenger<Message> pluginClusterMessenger(Ignite ignite) {
		ServerInfo serverInfo = ignite.services(ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME))
			.serviceProxy(Constants.DISTRIBUTED_SERVER_INFO, ServerInfo.class, false);

		return new PluginClusterMessenger(serverInfo.getNatsURL());
	}
	
	@Bean 
	Ignite ignite() {
		return ignite;
	}
	
	@Bean
	@Profile("!" + Constants.CLUSTER_PROFILE_NAME)
	public PluginManager pluginManager(Ignite ignite) {
		DistributedPluginManager dpm = new DistributedPluginManager();
		ignite.services(ClusterGroupDefinition.getPluginManagerClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_PLUGIN_MANAGER, dpm);

		return ignite.services(ClusterGroupDefinition.getPluginManagerClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_MANAGER, PluginManager.class, false);
	}
	
	@Bean
	@Profile(Constants.CLUSTER_PROFILE_NAME)
	public PluginManager pluginClusterManager(Ignite ignite) {
		DistributedPluginManager dpm = new DistributedClusterPluginManager();
		ignite.services(ClusterGroupDefinition.getPluginManagerClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_PLUGIN_MANAGER, dpm);

		return ignite.services(ClusterGroupDefinition.getPluginManagerClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_MANAGER, PluginManager.class, false);
	}
	
	@Bean
	public PluginManagerDependencyInjectionProxy pmdip() {
		return new PluginManagerDependencyInjectionProxy();
	}
}
