package tak.server.config;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

import jakarta.servlet.ServletContext;

import com.bbn.marti.remote.config.CoreConfigFacade;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.autoconfigure.context.properties.AwsS3ResourceLoaderProperties;
import io.awspring.cloud.autoconfigure.metrics.CloudWatchExportAutoConfiguration;

import org.apache.ignite.Ignite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SubProtocolHandler;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.cot.filter.DataFeedFilter;
import com.bbn.cot.filter.FlowTagFilter;
import com.bbn.cot.filter.ScrubInvalidValues;
import com.bbn.cot.filter.StreamingEndpointRewriteFilter;
import com.bbn.cot.filter.UrlAddingFilter;
import com.bbn.cot.filter.VBMSASharingFilter;
import com.bbn.marti.groups.DistributedPersistentGroupManager;
import com.bbn.marti.groups.DistributedUserManager;
import com.bbn.marti.groups.FileAuthenticator;
import com.bbn.marti.groups.GroupDao;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.GroupStore;
import com.bbn.marti.groups.InMemoryGroupStore;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.groups.PersistentGroupDao;
import com.bbn.marti.injector.ClusterUidCotTagInjector;
import com.bbn.marti.injector.InjectionManager;
import com.bbn.marti.injector.UidCotTagInjector;
import com.bbn.marti.network.PluginDataFeedJdbc;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.nio.server.NioServer;
import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.DataFeedCotService;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.groups.FileUserManagementInterface;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.service.InjectionService;
import com.bbn.marti.remote.service.InputManager;
import com.bbn.marti.remote.service.SecurityManager;
import com.bbn.marti.remote.socket.TakMessage;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.repeater.DistributedRepeaterManager;
import com.bbn.marti.repeater.RepeaterStore;
import com.bbn.marti.service.BrokerService;
import com.bbn.marti.service.DistributedContactManager;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.marti.service.MessagingInitializer;
import com.bbn.marti.service.MissionPackageExtractor;
import com.bbn.marti.service.PluginStore;
import com.bbn.marti.service.RepeaterService;
import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.federation.DataFeedFederationAspect;
import com.bbn.marti.sync.federation.FederationROLHandler;
import com.bbn.marti.sync.federation.MissionActionROLConverter;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.sync.service.DistributedDataFeedCotService;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.spring.RequestHolderBean;
import com.bbn.metrics.DistributedMetricsCollector;
import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.service.ActuatorMetricsService;
import com.bbn.metrics.service.DatabaseMetricsService;
import com.bbn.metrics.service.NetworkMetricsService;
import com.bbn.metrics.service.QueueMetricsService;

import tak.server.Constants;
import tak.server.cache.ActiveGroupCacheHelper;
import tak.server.cache.classification.ClassificationCacheHelper;
import tak.server.cache.DataFeedCotCacheHelper;
import tak.server.cache.MissionCacheResolver;
import tak.server.cache.MissionLayerCacheResolver;
import tak.server.cache.DatafeedCacheHelper;
import tak.server.cluster.DistributedInjectionService;
import tak.server.cluster.DistributedInputManager;
import tak.server.cluster.DistributedSecurityManager;
import tak.server.cot.CotEventContainer;
import tak.server.federation.DistributedFederationManager;
import tak.server.federation.FederationServer;
import tak.server.federation.MissionDisruptionManager;
import tak.server.federation.TakFigClient;
import tak.server.messaging.DistributedCotMessenger;
import tak.server.messaging.DistributedPluginApi;
import tak.server.messaging.DistributedPluginDataFeedApi;
import tak.server.messaging.DistributedPluginSelfStopApi;
import tak.server.messaging.DistributedTakMessenger;
import tak.server.messaging.MessageConverter;
import tak.server.messaging.Messenger;
import tak.server.plugins.PluginApi;
import tak.server.plugins.PluginDataFeedApi;
import tak.server.plugins.PluginSelfStopApi;
import tak.server.plugins.SystemInfoApi;
import tak.server.profile.DistributedServerInfo;
import tak.server.qos.DistributedQoSManager;
import tak.server.qos.MessageDOSStrategy;
import tak.server.qos.MessageDeliveryStrategy;
import tak.server.qos.MessageReadStrategy;
import tak.server.qos.QoSManager;
/*
 * services that are only used in separate messaging process, and monolith
 */
@Configuration
@EnableAutoConfiguration
@Profile({Constants.MESSAGING_PROFILE_NAME, Constants.MONOLITH_PROFILE_NAME})
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, ErrorMvcAutoConfiguration.class, MetricsAutoConfiguration.class, MetricsEndpointAutoConfiguration.class, CloudWatchExportAutoConfiguration.class, AwsS3ResourceLoaderProperties.class})
public class MessagingConfiguration {

	@Autowired
	ServletContext servletContext;

	@Bean
	public SubmissionService submissionService(DistributedFederationManager dfm, NioNettyBuilder nb, MessagingUtilImpl mui, NioServer ns, GroupManager gm,
											   ScrubInvalidValues siv, MessageConversionUtil mcu, GroupFederationUtil gfu, InjectionManager im, RepositoryService rs, Ignite ig, SubscriptionManager sm,
											   SubscriptionStore ss, FlowTagFilter flowTag, ContactManager contactManager, ServerInfo serverInfo,
											   MessageConverter messageConverter, ActiveGroupCacheHelper activeGroupCacheHelper, RemoteUtil remoteUtil, DataFeedRepository dfr) {

		return new SubmissionService(dfm, nb, mui, ns, gm, siv, mcu, gfu, im, rs, ig, sm, ss, flowTag, contactManager, serverInfo, messageConverter, activeGroupCacheHelper, remoteUtil, dfr);
	}

	@Bean
	public BrokerService brokerService() {
		return new BrokerService();
	}

	@Bean
	public RepeaterService repeaterService(BrokerService brokerService, GroupManager groupManager, DistributedRepeaterManager repeaterManager) {
		return new RepeaterService(brokerService, groupManager, repeaterManager);
	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	@Bean
	public DistributedFederationManager distributedFederationManager(
			Ignite ignite) throws RemoteException {
		DistributedFederationManager distributedFederationManager = new DistributedFederationManager(ignite);
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_FEDERATION_MANAGER, distributedFederationManager);
		return distributedFederationManager;
	}

	@Bean
	public FileUserManagementInterface distributedUserManager(Ignite ignite, FileAuthenticator fileAuthenticator) {
		DistributedUserManager distributedUserManager =  new DistributedUserManager();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_USER_FILE_MANAGER, distributedUserManager);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_USER_FILE_MANAGER, FileUserManagementInterface.class, false);
	}
	
	@Bean
	public SystemInfoApi distributedSystemInfoApi(Ignite ignite) {
		DistributedSystemInfoApi distributedSystemInfoApi =  new DistributedSystemInfoApi();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_SYSTEM_INFO_API, distributedSystemInfoApi);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_SYSTEM_INFO_API, SystemInfoApi.class, false);
	}

	@Bean
	@Scope("prototype")
	public TakFigClient takFigClient(MissionDisruptionManager missionDisruptionManager) {
		return new TakFigClient(missionDisruptionManager);
	}
	
	@Bean
	public MissionDisruptionManager missionDisruptionManager(FederationManager federationManager, MissionService missionService, GroupManager groupManager, EnterpriseSyncService syncService, MissionActionROLConverter missionActionROLConverter) {
		return new MissionDisruptionManager(federationManager, missionService, groupManager, syncService, missionActionROLConverter);
	}

	@Bean
	public MissionActionROLConverter marc(RemoteUtil remoteUtil, ObjectMapper mapper) {
		return new MissionActionROLConverter(remoteUtil, mapper);
	}
	
	@Bean
	public FederationServer federationServer() {
		return new FederationServer();
	}

	@Bean(Constants.DISTRIBUTED_COT_MESSENGER)
	public Messenger<CotEventContainer> distributedCotMessenger(Ignite ignite, SubscriptionStore subscriptionStore, ServerInfo serverInfo, MessageConverter messageConverter, SubmissionService submissionService) {
		return new DistributedCotMessenger(ignite, subscriptionStore, serverInfo,  messageConverter, submissionService);
	}

	@Bean(Constants.DISTRIBUTED_TAK_MESSENGER)
	public Messenger<TakMessage> takMessenger(Ignite ignite, ServerInfo serverInfo) {
		return new DistributedTakMessenger(ignite, serverInfo);
	}

	@Bean
	public NioNettyBuilder nettyBuilder() {
		return new NioNettyBuilder();
	}

	@Bean
	public MessagingInitializer coreMessagingInitializer(SubmissionService submissionService) {
		return new MessagingInitializer();
	}

	@Bean
	public StreamingEndpointRewriteFilter streamingEnpointRewriteFilter(@Lazy MissionService missionService) {
		return new StreamingEndpointRewriteFilter(missionService);
	}
	
	@Bean
	public DataFeedFilter dataFeedFilter() {
		return new DataFeedFilter();
	}

	@Bean
	public VBMSASharingFilter vbmSASharingFilter() {
		return new VBMSASharingFilter();
	}
	
	@Bean
	public NioServer nioServer() throws IOException {
		return new NioServer();
	}

	@Bean
	@Primary
	@Profile("!" + Constants.CLUSTER_PROFILE_NAME)
	public SubscriptionManager distributedSubscriptionManager(Ignite ignite) throws RemoteException {
		DistributedSubscriptionManager distributedSubscriptionManager =  new DistributedSubscriptionManager();

		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, distributedSubscriptionManager);
		
		return distributedSubscriptionManager;
	}
	
	@Bean
	@Primary
	@Profile(Constants.CLUSTER_PROFILE_NAME)
	public SubscriptionManager distributedSubscriptionManagerCluster(Ignite ignite) throws RemoteException {
		DistributedSubscriptionManager distributedSubscriptionManager =  new DistributedSubscriptionManager();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, distributedSubscriptionManager);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, SubscriptionManager.class, false);
	}

	@Bean
	public GroupManager groupManager(Ignite ignite, GroupStore groupStore) {
		DistributedPersistentGroupManager distributedPersistentGroupManager = new DistributedPersistentGroupManager(groupStore);
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_GROUP_MANAGER, distributedPersistentGroupManager);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_GROUP_MANAGER, GroupManager.class, false);
	}

	@Bean
	public GroupDao groupDao() {
		return new PersistentGroupDao();
	}

	@Bean
	ActiveGroupCacheHelper getActiveGroupCacheHelper() {
		return new ActiveGroupCacheHelper();
	}

	@Bean
	ClassificationCacheHelper getClassificationCacheHelper() {
		return new ClassificationCacheHelper();
	}

	@Bean
	public MessagingDependencyInjectionProxy dependencyProxy() {
		return new MessagingDependencyInjectionProxy();
	}

	@Bean
	public GroupStore groupStore() {
		return new InMemoryGroupStore();
	}

	// no-op sub protocol handler to allow the application context to initialize without websocket support
	@Bean
	@Primary
	public SubProtocolHandler noOpWebSocketSubProtocolHandler() {
		return new SubProtocolHandler() {

			@Override
			public List<String> getSupportedProtocols() {
				return null;
			}

			@Override
			public void handleMessageFromClient(WebSocketSession session, WebSocketMessage<?> message, MessageChannel outputChannel) throws Exception { }

			@Override
			public void handleMessageToClient(WebSocketSession session, Message<?> message) throws Exception { }

			@Override
			public String resolveSessionId(Message<?> message) {
				return null;
			}

			@Override
			public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) throws Exception { }

			@Override
			public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel) throws Exception { }
		};
	}

	@Bean
	public MissionPackageExtractor missionPackageExtractor() {
		return new MissionPackageExtractor();
	}

	@Bean
	public ScrubInvalidValues scrubInvalidValues() {
		return new ScrubInvalidValues();
	}

	@Bean
	public com.bbn.marti.util.MessageConversionUtil coreMessagingUtil() {
		return new com.bbn.marti.util.MessageConversionUtil();
	}

	@Bean
	public UrlAddingFilter urlAddingFilter() {
		return new UrlAddingFilter();
	}

	@Bean
	public ContactManager contactManager(Ignite ignite) {
		DistributedContactManager distributedContactManager = new DistributedContactManager();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_CONTACT_MANAGER, distributedContactManager);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_CONTACT_MANAGER, ContactManager.class, false);
	}

	@Bean
	public FederationROLHandler federationROLHandler(MissionService missionService, EnterpriseSyncService syncService, RemoteUtil remoteUtil, DataFeedRepository dataFeedRepository) throws RemoteException {
		return new FederationROLHandler(missionService, syncService, remoteUtil, dataFeedRepository);
	}

	@Bean
	public DistributedRepeaterManager repeaterManager(Ignite ignite) {
		DistributedRepeaterManager distributedRepeaterManager =  new DistributedRepeaterManager();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_REPEATER_MANAGER, distributedRepeaterManager);

		return distributedRepeaterManager;
	}

	@Bean
	public RepeaterStore repeaterStore() {
		return new RepeaterStore();
	}

	// TODO: handle this for cluster
	@Bean
	@Primary
	public ServerInfo serverInfo(Ignite ignite) {
		DistributedServerInfo distributedServerInfo =  new DistributedServerInfo(ignite);
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_SERVER_INFO, distributedServerInfo);

		ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_SERVER_INFO, ServerInfo.class, false);
		
		return distributedServerInfo;
	}

	@Bean
	public InputManager inputManager(Ignite ignite) {
		DistributedInputManager distributedInputManager =  new DistributedInputManager();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_INPUT_MANAGER, distributedInputManager);

		ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class, false);
		
		return distributedInputManager;
	}
	

	@Bean
	public SecurityManager SecurityManager(Ignite ignite) {
		DistributedSecurityManager distributedSecurityManager =  new DistributedSecurityManager();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_SECURITY_MANAGER, distributedSecurityManager);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_SECURITY_MANAGER, SecurityManager.class, false);
	}

	@Bean
	public ActuatorMetricsService actuatorMetricsService(MetricsEndpoint metricsEndpoint) {
		return new ActuatorMetricsService(metricsEndpoint);
	}

	@Bean
	public DatabaseMetricsService databaseMetricsService(RepositoryService repositoryService) {
		return new DatabaseMetricsService(repositoryService);
	}

	@Bean
	public QueueMetricsService queueMetricsService(BrokerService brokerService, SubmissionService submissionService,
			RepositoryService repositoryService) {
		return new QueueMetricsService(brokerService, submissionService, repositoryService);
	}

	@Bean
	public NetworkMetricsService networkMetricsService() {
		return new NetworkMetricsService();
	}

	@Bean
	@Primary
	public MetricsCollector metricsCollector(Ignite ignite,
			DatabaseMetricsService databaseMetricsService, QueueMetricsService queueMetricsService, NetworkMetricsService networkMetricsService) {
		DistributedMetricsCollector metricsCollector = new DistributedMetricsCollector(ignite);
		metricsCollector.setMessagingDatabaseMetricsService(databaseMetricsService);
		metricsCollector.setNetworkMetricsService(networkMetricsService);

		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_METRICS_COLLECTOR, metricsCollector);

		ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_METRICS_COLLECTOR, MetricsCollector.class, false);
		
		return metricsCollector;
	}

	@Bean
	public FlowTagFilter flowTagFilter(ServerInfo serverInfo) {
		return new FlowTagFilter(serverInfo);
	}

	@Bean
	public MessagingUtilImpl messagingUtil() {
		return new MessagingUtilImpl();
	}

	@Bean
	InjectionManager injectionManager(GroupFederationUtil groupFederationUtil) {
		return new InjectionManager(groupFederationUtil);
	}
	
	@Bean
	public InjectionService injectionService(Ignite ignite, UidCotTagInjector uidCotTagInjector) {
		DistributedInjectionService distributedInjectionService =  new DistributedInjectionService();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_INJECTION_SERVICE, distributedInjectionService);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_INJECTION_SERVICE, InjectionService.class, false);
	}
	
	@Bean
	@Profile("!" + Constants.CLUSTER_PROFILE_NAME)
	UidCotTagInjector uidCotTagInjector() throws RemoteException {
		return new UidCotTagInjector();
	}
	
	@Bean
	@Profile(Constants.CLUSTER_PROFILE_NAME)
	UidCotTagInjector clusterUidCotTagInjector() throws RemoteException {
		return new ClusterUidCotTagInjector();
	}
	
	@Bean
	@Scope(scopeName = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public RequestHolderBean requestHolderBean() { 
		return new RequestHolderBean();
	}

	@Bean("missionCacheResolver")
	public MissionCacheResolver missionCacheResolver() { return new MissionCacheResolver(); }

	@Bean("missionLayerCacheResolver")
	public MissionLayerCacheResolver missionLayerCacheResolver() { return new MissionLayerCacheResolver(); }

	@Bean
	public MessageDeliveryStrategy mds() {
		return new MessageDeliveryStrategy();
	}

	@Bean
	public MessageReadStrategy mrs() {
		return new MessageReadStrategy();
	}
	
	@Bean
	public MessageDOSStrategy mdoss() {
		return new MessageDOSStrategy();
	}
		
	@Bean
	public QoSManager qosManager(Ignite ignite) {
		DistributedQoSManager qosManager = new DistributedQoSManager();
		
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_QOS_MANAGER, qosManager);
		ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_QOS_MANAGER, QoSManager.class, false);
		
		return qosManager;
	}
	

	@Bean
	public DataFeedFederationAspect DataFeedFederationAspect(FederationManager federationManager, MissionActionROLConverter malrc) {
		return new DataFeedFederationAspect(federationManager, malrc, null);
	}
	
	@Bean
	public PluginDataFeedApi distributedPluginDataFeedApi(Ignite ignite) {
		
		DistributedPluginDataFeedApi distributedPluginDataFeedApi =  new DistributedPluginDataFeedApi();
		
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_PLUGIN_DATA_FEED_API, distributedPluginDataFeedApi);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_DATA_FEED_API, PluginDataFeedApi.class, false);

	}
	
	@Bean
	public DatafeedCacheHelper pluginDatafeedCacheHelper() {
		return new DatafeedCacheHelper();
	}

	@Bean
	public PluginDataFeedJdbc pluginDataFeedJdbc() {
		return new PluginDataFeedJdbc();
	}
	
	@Bean
	public PluginStore pluginStore() {
		return new PluginStore();
	}
	
	@Bean
	public PluginApi pluginApi(Ignite ignite) {
		
		DistributedPluginApi distributedPluginApi =  new DistributedPluginApi();
		
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_PLUGIN_API, distributedPluginApi);

		return distributedPluginApi;

	}
	
	@Bean
	public PluginSelfStopApi pluginSelfStopApi(Ignite ignite) {
		
		DistributedPluginSelfStopApi distributedPluginSelfStopApi =  new DistributedPluginSelfStopApi();
		
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_PLUGIN_SELF_STOP_API, distributedPluginSelfStopApi);

		return ignite.services(ClusterGroupDefinition.getMessagingLocalClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_SELF_STOP_API, PluginSelfStopApi.class, false);
	}
	
	@Bean
	// this distributed services act as a proxy for modifying a local cache.
	// deploy a remote version to ignite, which will be used for cache gets (infrequent).
	// return a "local" non remote version as the bean, so that we can perform cache puts (frequent)
	// without making remote calls to ignite for no reason
	public DataFeedCotService distributedDataFeedCotService(Ignite ignite) {
		DistributedDataFeedCotService distributedDataFeedCotService =  new DistributedDataFeedCotService();
		ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_DATAFEED_COT_SERVICE, distributedDataFeedCotService);

		return new DistributedDataFeedCotService();
	}
	
	@Bean
	public DataFeedCotCacheHelper dataFeedCotCacheHelper() {
		return new DataFeedCotCacheHelper();
	}
}
