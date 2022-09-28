package tak.server.plugins.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import tak.server.plugins.PluginApi;
import tak.server.plugins.PluginDataFeedApi;
import tak.server.plugins.PluginManagerConstants;
import tak.server.plugins.PluginSelfStopApi;
import tak.server.plugins.PluginStarter;
import tak.server.plugins.SystemInfoApi;
import tak.server.plugins.manager.loader.PluginLoader;
import tak.server.plugins.messaging.MessageConverter;
import tak.server.plugins.messaging.PluginClusterMessenger;
import tak.server.plugins.messaging.PluginMessenger;
import tak.server.plugins.util.PluginManagerDependencyInjectionProxy;

@SpringBootApplication
public class PluginService implements CommandLineRunner {
	
	private static Ignite ignite = null;
	
	private static final Logger logger = LoggerFactory.getLogger(PluginService.class);
	
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
	ServerInfo serverInfo(Ignite ignite) {
		return ignite.services(ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME))
				.serviceProxy(Constants.DISTRIBUTED_SERVER_INFO, ServerInfo.class, false);
	}
	
	@Bean
	PluginStarter pluginIntializer(Ignite ignite, PluginDataFeedApi pdfApi, ServerInfo serverInfo, PluginApi pluginApi, PluginSelfStopApi pluginSelfStopApi) {
		return new PluginStarter(serverInfo, pluginApi);
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
	public SystemInfoApi systemInfoApi(Ignite ignite) {
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_SYSTEM_INFO_API, SystemInfoApi.class, false);
	}
	
	@Bean
	public PluginManagerDependencyInjectionProxy pmdip() {
		return new PluginManagerDependencyInjectionProxy();
	}
	
	private boolean accessApi(PluginDataFeedApi api) {
		api.getAllPluginDataFeeds();
		return true;
	}
	
	private CompletableFuture<Boolean> canAccessApi(final PluginDataFeedApi api) {

		try {
			return CompletableFuture.completedFuture(accessApi(api));
		} catch (Exception e) {
			try {
				Thread.sleep(250L);
			} catch (InterruptedException e1) {
				logger.error("interruped sleep", e1);
			}
			return canAccessApi(api);
		}
	}
	
	@Bean
	public PluginDataFeedApi pluginDataFeedApi(Ignite ignite) {
		
		
		final PluginDataFeedApi api = ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_DATA_FEED_API, PluginDataFeedApi.class, false);
		
		boolean isApiAvailable = false;
		
		// block and wait for PluginDataFeedApi to become available in messaging process
		try {
			isApiAvailable = canAccessApi(api).get();
		} catch (InterruptedException | ExecutionException e) {
			logger.error("interrupted checking api availablity", e);
		}
		
		logger.info("data feed api available: " + isApiAvailable);
		
		return api;
	}
	
	@Bean
	public PluginApi pluginApi(Ignite ignite) {
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_API, PluginApi.class, false);
	}
	
	@Bean
	public PluginSelfStopApi pluginSelfStopApi(Ignite ignite) {
		
		final PluginSelfStopApi api = ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_SELF_STOP_API, PluginSelfStopApi.class, false);
		
		return api;
	}
}
