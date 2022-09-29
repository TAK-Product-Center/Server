package tak.server.config;

import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.file.FileConfigurationApi;
import com.bbn.locate.LocateApi;
import com.bbn.marti.CotImageBean;
import com.bbn.marti.CreateSubscriptionServlet;
import com.bbn.marti.DBAdminServlet;
import com.bbn.marti.EditSubscriptionServlet;
import com.bbn.marti.GetCotDataServlet;
import com.bbn.marti.GetServerTimeServlet;
import com.bbn.marti.KmlMasterSaServlet;
import com.bbn.marti.LatestKMLServlet;
import com.bbn.marti.MissionKMLServlet;
import com.bbn.marti.ResubscribeServlet;
import com.bbn.marti.TracksKMLServlet;
import com.bbn.marti.citrap.CITrapReportAPI;
import com.bbn.marti.citrap.CITrapReportNotifications;
import com.bbn.marti.citrap.CITrapReportService;
import com.bbn.marti.config.ConfigAPI;
import com.bbn.marti.cot.search.CotSearchQueryMap;
import com.bbn.marti.cot.search.CotSearchQueryQueue;
import com.bbn.marti.cot.search.api.CotQueryApi;
import com.bbn.marti.cot.search.api.CotQueryServlet;
import com.bbn.marti.cot.search.service.CotSearchService;
import com.bbn.marti.cot.search.service.CotSearchServiceImpl;
import com.bbn.marti.device.profile.api.ProfileAPI;
import com.bbn.marti.device.profile.api.ProfileAdminAPI;
import com.bbn.marti.device.profile.service.ProfileService;
import com.bbn.marti.excheck.ExCheckAPI;
import com.bbn.marti.excheck.ExCheckService;
import com.bbn.marti.feeds.DataFeedApi;
import com.bbn.marti.groups.CustomExceptionHandler;
import com.bbn.marti.groups.GroupsApi;
import com.bbn.marti.injector.InjectionApi;
import com.bbn.marti.kml.icon.api.IconsetIconApi;
import com.bbn.marti.kml.icon.service.IconsetUploadProcessor;
import com.bbn.marti.kml.icon.service.IconsetUploadProcessorImpl;
import com.bbn.marti.logs.LogServlet;
import com.bbn.marti.maplayer.MapLayerService;
import com.bbn.marti.maplayer.api.MapLayersApi;
import com.bbn.marti.network.ContactManagerApi;
import com.bbn.marti.network.ContactManagerService;
import com.bbn.marti.network.FederationApi;
import com.bbn.marti.network.FederationConfigApi;
import com.bbn.marti.network.HomeApi;
import com.bbn.marti.network.LDAPApi;
import com.bbn.marti.network.SecurityAuthenticationApi;
import com.bbn.marti.network.SubmissionApi;
import com.bbn.marti.network.UIDSearchApi;
import com.bbn.marti.oauth.admin.TokenApi;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.FederationConfigInterface;
import com.bbn.marti.remote.groups.FileUserManagementInterface;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.service.RetentionQueryService;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.repeater.RepeaterApi;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.DistributedFederationHttpConnectorManager;
import com.bbn.marti.service.DistributedRetentionQueryManager;
import com.bbn.marti.service.FederationHttpConnectorManager;
import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.sync.ContentServlet;
import com.bbn.marti.sync.DeleteServlet;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.FileList;
import com.bbn.marti.sync.JDBCEnterpriseSyncService;
import com.bbn.marti.sync.MetadataApi;
import com.bbn.marti.sync.MissionPackageCreatorServlet;
import com.bbn.marti.sync.MissionPackageQueryServlet;
import com.bbn.marti.sync.MissionPackageUploadServlet;
import com.bbn.marti.sync.SearchServlet;
import com.bbn.marti.sync.UploadServlet;
import com.bbn.marti.sync.api.ClassificationApi;
import com.bbn.marti.sync.api.ContactsApi;
import com.bbn.marti.sync.api.CopViewApi;
import com.bbn.marti.sync.api.CotApi;
import com.bbn.marti.sync.api.MissionApi;
import com.bbn.marti.sync.api.PropertiesApi;
import com.bbn.marti.sync.api.SequenceApi;
import com.bbn.marti.sync.api.SubscriptionApi;
import com.bbn.marti.sync.federation.DataFeedFederationAspect;
import com.bbn.marti.sync.federation.EnterpriseSyncFederationAspect;
import com.bbn.marti.sync.federation.MissionActionROLConverter;
import com.bbn.marti.sync.federation.MissionFederationAspect;
import com.bbn.marti.sync.federation.MissionFederationManager;
import com.bbn.marti.sync.federation.MissionFederationManagerROL;
import com.bbn.marti.util.IconsetDirWatcher;
import com.bbn.marti.util.VersionApi;
import com.bbn.marti.util.spring.HttpSessionCreatedEventListener;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.bbn.marti.util.spring.TakAuthSessionDestructionListener;
import com.bbn.marti.video.VideoConnectionManager;
import com.bbn.marti.video.VideoConnectionManagerV2;
import com.bbn.marti.video.VideoConnectionSender;
import com.bbn.marti.video.VideoConnectionUploader;
import com.bbn.marti.video.VideoManagerService;
import com.bbn.marti.xmpp.XmppAPI;
import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.endpoint.ClusterMetricsEndpoint;
import com.bbn.metrics.endpoint.CpuMetricsEndpoint;
import com.bbn.metrics.endpoint.DatabaseMetricsEndpoint;
import com.bbn.metrics.endpoint.MemoryMetricsEndpoint;
import com.bbn.metrics.endpoint.QueueMetricsEndpoint;
import com.bbn.metrics.service.ActuatorMetricsService;
import com.bbn.metrics.service.DatabaseMetricsService;
import com.bbn.tak.tls.CertManager;
import com.bbn.tak.tls.CertManagerAdminApi;
import com.bbn.tak.tls.CertManagerApi;
import com.bbn.tak.tls.Service.CertManagerService;
import com.bbn.user.registration.RegistrationApi;
import com.bbn.user.registration.service.UserRegistrationService;
import com.bbn.useraccountmanagement.FileUserAccountManagementApi;
import com.bbn.vbm.VBMConfigurationApi;
import com.fasterxml.jackson.databind.ObjectMapper;

import tak.server.Constants;
import tak.server.api.DistributedPluginFileApi;
import tak.server.api.DistributedPluginMissionApi;
import tak.server.cache.ContactCacheHelper;
import tak.server.cache.MissionCacheResolver;
import tak.server.federation.FederationConfigManager;
import tak.server.grid.MissionArchiveManagerProxyFactory;
import tak.server.grid.PluginManagerProxyFactory;
import tak.server.grid.RetentionPolicyConfigProxyFactory;
import tak.server.plugins.PluginDataApi;
import tak.server.plugins.PluginFileApi;
import tak.server.plugins.PluginManagerApi;
import tak.server.plugins.PluginMissionApi;
import tak.server.qos.QoSApi;
import tak.server.qos.QoSManager;
import tak.server.retention.RetentionApi;
import tak.server.system.ApiDependencyProxy;
import tak.server.util.ExecutorSource;
import tak.server.util.LoginAccessController;

/*
 * services that are only used in separate API process
 */
@Configuration
@Profile({Constants.API_PROFILE_NAME, Constants.MONOLITH_PROFILE_NAME})
@Import({WebSocketConfiguration.class, WebSocketSecurityConfig.class, WebMvcAutoConfiguration.class, RequestContextConfig.class})
@EnableWebSecurity
@EnableAspectJAutoProxy
public class ApiConfiguration implements WebMvcConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(ApiConfiguration.class);

	@Value("${takserver.compat.context-path}")
	private String compatServletPath;

	@Bean
	public FederationHttpConnectorManager distributedFederationHttpConnectorManager(Ignite ignite, CoreConfig config) {
		DistributedFederationHttpConnectorManager distributedFederationHttpConnectorManager =  new DistributedFederationHttpConnectorManager();
		ignite.services(ClusterGroupDefinition.getApiClusterDeploymentGroup(ignite))
			.deployNodeSingleton(Constants.DISTRIBUTED_FEDERATION_HTTP_CONNECTOR_SERVICE, distributedFederationHttpConnectorManager);

		return ignite.services(ClusterGroupDefinition.getApiLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_FEDERATION_HTTP_CONNECTOR_SERVICE, FederationHttpConnectorManager.class, false);
	}

	@Bean
	protected HttpSessionListener httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	protected ServletContextListener listener() {
		return new ServletContextListener() {

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				if (logger.isDebugEnabled()) {
					logger.debug("ServletContext initialized");
				}
			}

			@Override
			public void contextDestroyed(ServletContextEvent sce) {
				if (logger.isDebugEnabled()) {
					logger.debug("ServletContext destroyed");
				}
			}
		};
	}

	@Bean
	public MultipartConfigElement multipartConfigElement() {

		DistributedConfiguration config = DistributedConfiguration.getInstance();

		MultipartConfigFactory factory = new MultipartConfigFactory();

		factory.setMaxFileSize(DataSize.ofBytes(config.getNetwork().getEnterpriseSyncSizeLimitMB() * 1024 * 1024));
		factory.setMaxRequestSize(DataSize.ofBytes(config.getNetwork().getEnterpriseSyncSizeLimitMB() * 1024 * 1024));

		return factory.createMultipartConfig();
	}

	@Bean
	public ServletRegistrationBean<SearchServlet> searchServlet() {
		ServletRegistrationBean<SearchServlet> bean = new ServletRegistrationBean<>(
				new SearchServlet(), compatServletPath + "/sync/search/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<DeleteServlet> deleteServlet() {
		ServletRegistrationBean<DeleteServlet> bean = new ServletRegistrationBean<>(
				new DeleteServlet(), compatServletPath + "/sync/delete/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<ContentServlet> contentServlet() {
		ServletRegistrationBean<ContentServlet> bean = new ServletRegistrationBean<>(
				new ContentServlet(), compatServletPath + "/sync/content/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<UploadServlet> uploadServlet() {
		ServletRegistrationBean<UploadServlet> bean = new ServletRegistrationBean<>(
				new UploadServlet(), compatServletPath + "/sync/upload/*");
		bean.setLoadOnStartup(1);
		bean.setMultipartConfig(multipartConfigElement());
		return bean;
	}

	@Bean
	public ServletRegistrationBean<MissionPackageCreatorServlet> mpCreateServlet() {
		ServletRegistrationBean<MissionPackageCreatorServlet> bean = new ServletRegistrationBean<>(
				new MissionPackageCreatorServlet(), compatServletPath + "/sync/missioncreate/*");
		bean.setLoadOnStartup(1);
		bean.setMultipartConfig(multipartConfigElement());
		return bean;
	}

	@Bean
	public ServletRegistrationBean<MissionPackageQueryServlet> mpQueryServlet() {
		ServletRegistrationBean<MissionPackageQueryServlet> bean = new ServletRegistrationBean<>(
				new MissionPackageQueryServlet(), compatServletPath + "/sync/missionquery/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<MissionPackageUploadServlet> mpUploadServlet() {
		ServletRegistrationBean<MissionPackageUploadServlet> bean = new ServletRegistrationBean<>(
				new MissionPackageUploadServlet(), compatServletPath + "/sync/missionupload/*");
		bean.setLoadOnStartup(1);
		bean.setMultipartConfig(multipartConfigElement());
		return bean;
	}

	@Bean
	public ServletRegistrationBean<MissionKMLServlet> missionKMLServlet() {
		ServletRegistrationBean<MissionKMLServlet> bean = new ServletRegistrationBean<>(
				new MissionKMLServlet(), compatServletPath + "/ExportMissionKML/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<LatestKMLServlet> latestKMLServlet() {
		ServletRegistrationBean<LatestKMLServlet> bean = new ServletRegistrationBean<>(
				new LatestKMLServlet(), compatServletPath + "/LatestKML/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<KmlMasterSaServlet> kmlMasterServlet() {
		ServletRegistrationBean<KmlMasterSaServlet> bean = new ServletRegistrationBean<>(
				new KmlMasterSaServlet(), compatServletPath + "/KmlMasterSA/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<TracksKMLServlet> tracksKmlServlet() {
		ServletRegistrationBean<TracksKMLServlet> bean = new ServletRegistrationBean<>(
				new TracksKMLServlet(), compatServletPath + "/TracksKML/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<ResubscribeServlet> resubscribeServlet() {
		ServletRegistrationBean<ResubscribeServlet> bean = new ServletRegistrationBean<>(
				new ResubscribeServlet(), compatServletPath + "/ResubscribeServlet/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<GetServerTimeServlet> serverTimeServlet() {
		ServletRegistrationBean<GetServerTimeServlet> bean = new ServletRegistrationBean<>(
				new GetServerTimeServlet(), compatServletPath + "/GetTime/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<GetCotDataServlet> cotDataServlet() {
		ServletRegistrationBean<GetCotDataServlet> bean = new ServletRegistrationBean<>(
				new GetCotDataServlet(), compatServletPath + "/GetCotData/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<CreateSubscriptionServlet> createSubscriptionServlet() {
		ServletRegistrationBean<CreateSubscriptionServlet> bean = new ServletRegistrationBean<>(
				new CreateSubscriptionServlet(), compatServletPath + "/CreateSubscriptionServlet/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<EditSubscriptionServlet> editSubscriptionServlet() {
		ServletRegistrationBean<EditSubscriptionServlet> bean = new ServletRegistrationBean<>(
				new EditSubscriptionServlet(), compatServletPath + "/EditSubscriptionServlet/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<DBAdminServlet> dbAdminServlet() {
		ServletRegistrationBean<DBAdminServlet> bean = new ServletRegistrationBean<>(
				new DBAdminServlet(), compatServletPath + "/DBAdmin/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<VideoConnectionManager> videoConnectionManager() {
		ServletRegistrationBean<VideoConnectionManager> bean = new ServletRegistrationBean<>(
				new VideoConnectionManager(), compatServletPath + "/vcm/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<VideoConnectionUploader> videoConnectionUploader() {
		ServletRegistrationBean<VideoConnectionUploader> bean = new ServletRegistrationBean<>(
				new VideoConnectionUploader(), compatServletPath + "/vcu/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<VideoConnectionSender> videoConnectionSender() {
		ServletRegistrationBean<VideoConnectionSender> bean = new ServletRegistrationBean<>(
				new VideoConnectionSender(), compatServletPath + "/vcs/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<CotQueryServlet> cotQueryServlet() {
		ServletRegistrationBean<CotQueryServlet> bean = new ServletRegistrationBean<>(
				new CotQueryServlet(), compatServletPath + "/CotQueryServlet/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public ServletRegistrationBean<LogServlet> errorLogServlet() {
		ServletRegistrationBean<LogServlet> bean = new ServletRegistrationBean<>(
				new LogServlet(), compatServletPath + "/ErrorLog/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addRedirectViewController("webtak", "/webtak/index.html");
		registry.addRedirectViewController("webtak/", "/webtak/index.html");
		registry.addRedirectViewController("Marti/webtak", "/webtak/index.html");
		registry.addRedirectViewController("Marti/webtak/", "/webtak/index.html");
		registry.addRedirectViewController("Marti/", "/index.html");
		registry.addRedirectViewController("Marti", "/index.html");
		registry.addRedirectViewController("Marti/index.html", "/index.html");
		registry.addRedirectViewController("/", "/index.html");
		registry.addRedirectViewController("/setup", "/setup/index.html");
		registry.addRedirectViewController("/setup/", "/setup/index.html");
		registry.addRedirectViewController("/register", "/register/index.html");
		registry.addRedirectViewController("/register/", "/register/index.html");
		registry.addRedirectViewController("/locate", "/locate/index.html");
		registry.addRedirectViewController("/locate/", "/locate/index.html");
		registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
	}
	
	@Bean
	CoreConfig coreConfig() {
		return DistributedConfiguration.getInstance();
	}

	@Bean
	CertManagerService certManagerService() {
		return new CertManagerService();
	}

	@Bean
	CertManager certManager() throws NoSuchProviderException, NoSuchAlgorithmException {
		return new CertManager();
	}

	// CI Trap
	@Bean("ciTrapPersistenceStore")
	com.bbn.marti.citrap.PersistenceStore ciTrapPersistenceStore() {
		return new com.bbn.marti.citrap.PersistenceStore();
	}

	@Bean
	CITrapReportNotifications cITrapReportNotifications() {
		return new CITrapReportNotifications();
	}

	@Bean
	CITrapReportService cITrapReportService() {
		return new CITrapReportService();
	}

	@Bean
	VideoManagerService videoManagerService() {
		return new VideoManagerService();
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SpringContextBeanForApi springContextBeanForApi() {
		return new SpringContextBeanForApi();
	}

	@Bean
	HttpSessionCreatedEventListener httpSessionCreatedEventListener() {
		return new HttpSessionCreatedEventListener();
	}

	@Bean
	TakAuthSessionDestructionListener takAuthSessionDestructionListener() {
		return new TakAuthSessionDestructionListener();
	}

	@Bean
	ContactManagerService contactManagerService() {
		return new ContactManagerService();
	}

	@Bean
	public CotSearchQueryQueue cotSearchQueryQueue() {
		return new CotSearchQueryQueue();
	}

	@Bean
	public CotSearchService cotSearchService() {
		return new CotSearchServiceImpl();
	}

	@Bean
	public CotSearchQueryMap cotSearchQueryMap() {
		return new CotSearchQueryMap();
	}

	@Bean
	public ExCheckService exCheckService() {
		return new ExCheckService();
	}

	@Bean
	public MapLayerService mapLayerService() {
		return new MapLayerService();
	}

	@Bean("errorLogPersistenceStore")
	public com.bbn.marti.logs.PersistenceStore errorLogPersistenceStore() {
		return new com.bbn.marti.logs.PersistenceStore();
	}

	@Bean
	public CotImageBean cotImageBean() {
		return new CotImageBean();
	}

	@Bean
	public ProfileService profileService() {
		return new ProfileService();
	}

	@Bean
	public FileList fileList() {
		return new FileList();
	}

	@Bean
	public EnterpriseSyncService enterpriseSyncService() {
		return new JDBCEnterpriseSyncService();
	}

	@Bean
	public MissionFederationManager missionFederationManager(MissionActionROLConverter malrc) {
		return new MissionFederationManagerROL(malrc);
	}

	@Bean
	public MissionActionROLConverter malrc(RemoteUtil remoteUtil, ObjectMapper mapper) {
		return new MissionActionROLConverter(remoteUtil, mapper);
	}

	// Metrics Services
	@Bean
	public ActuatorMetricsService actuatorMetricsService(MetricsEndpoint metricsEndpoint) {
		return new ActuatorMetricsService(metricsEndpoint);
	}

	@Bean
	public DatabaseMetricsService databaseMetricsService(RepositoryService repositoryService) {
		return new DatabaseMetricsService(repositoryService);
	}
	
	@Bean
	public DatabaseMetricsEndpoint databaseMetricsEndpoint(@Lazy MetricsCollector metricsCollector) {
		return new DatabaseMetricsEndpoint(metricsCollector);
	}

	@Bean
	public MemoryMetricsEndpoint memoryMetricsEndpoint(@Lazy MetricsCollector metricsCollector) {
		return new MemoryMetricsEndpoint(metricsCollector);
	}

	@Bean
	public ClusterMetricsEndpoint clusterMetricsEndpoint() {
		return new ClusterMetricsEndpoint();
	}
	
	@Bean
	public CpuMetricsEndpoint cpuMetricsEndpoint(@Lazy MetricsCollector metricsCollector) {
		return new CpuMetricsEndpoint(metricsCollector);
	}

	@Bean
	public QueueMetricsEndpoint queueMetricsEndpoint(@Lazy MetricsCollector metricsCollector) {
		return new QueueMetricsEndpoint(metricsCollector);
	}

	@Bean
	public SecurityAuthenticationApi securityAuthenticationApi() {
		return new SecurityAuthenticationApi();
	}

	@Bean
	public FederationConfigInterface federationConfigManager(CoreConfig coreConfig) throws RemoteException {
		return new FederationConfigManager(coreConfig);
	}

	@Bean
	public MissionFederationAspect missionFederationAspect() {
		return new MissionFederationAspect();
	}
	
	@Bean
	public EnterpriseSyncFederationAspect enterpriseSyncFederationAspect() {
		return new EnterpriseSyncFederationAspect();
	}

	@Bean
	public IconsetDirWatcher iconsetDirWatcher() {
		return new IconsetDirWatcher();
	}

	@Bean
	public IconsetUploadProcessor iconsetUploadProcessorImpl() {
		return new IconsetUploadProcessorImpl();
	}

	// APIs

	@Bean
	public HomeApi homeApi() {
		return new HomeApi();
	}

	@Bean
	public CertManagerApi certManagerApi() {
		return new CertManagerApi();
	}

	@Bean
	public CertManagerAdminApi certManagerAdminApi() {
		return new CertManagerAdminApi();
	}

	@Bean
	public CITrapReportAPI ciTrapReportApi() {
		return new CITrapReportAPI();
	}

	@Bean
	public VersionApi versionApi() {
		return new VersionApi();
	}

	@Bean
	public ConfigAPI configAPI() {
		return new ConfigAPI();
	}

	@Bean
	public LDAPApi ldapAPI() {
		return new LDAPApi();
	}

	@Bean
	public SubmissionApi submissionApi() {
		return new SubmissionApi();
	}
	
	@Bean
	public DataFeedApi dataFeedApi() {
		return new DataFeedApi();
	}

	@Bean
	public FederationApi federationApi() {
		return new FederationApi();
	}

	@Bean
	public ContactManagerApi contactManagerApi() {
		return new ContactManagerApi();
	}

	@Bean
	public UIDSearchApi uidSearchApi() {
		return new UIDSearchApi();
	}

	@Bean
	public FederationConfigApi federationConfigApi() {
		return new FederationConfigApi();
	}

	@Bean
	public GroupsApi groupApi() {
		return new GroupsApi();
	}

	@Bean
	public CotQueryApi cotQueryApi() {
		return new CotQueryApi();
	}

	@Bean
	public ExCheckAPI exCheckApi() {
		return new ExCheckAPI();
	}

	@Bean
	public MapLayersApi mapLayersApi() {
		return new MapLayersApi();
	}

	@Bean
	public IconsetIconApi iconsetIconApi() {
		return new IconsetIconApi();
	}

	@Bean
	public XmppAPI xmppApi() {
		return new XmppAPI();
	}

	@Bean
	public ProfileAPI profileAPI() {
		return new ProfileAPI();
	}

	@Bean
	public ProfileAdminAPI profileAdminAPI() {
		return new ProfileAdminAPI();
	}

	@Bean
	public SubscriptionApi subscriptionApi() {
		return new SubscriptionApi();
	}

	@Bean
	public CotApi cotApi() {
		return new CotApi();
	}

	@Bean
	public ContactsApi contactsApi() {
		return new ContactsApi();
	}

	@Bean
	public ClassificationApi classificationApi() {
		return new ClassificationApi();
	}

	@Bean
	public SequenceApi sequenceApi() {
		return new SequenceApi();
	}

	@Bean
	public MissionApi missionApi() {
		return new MissionApi();
	}

	@Bean
	public CopViewApi copViewApi() {
		return new CopViewApi();
	}

	@Bean
	public PropertiesApi propertiesApi() {
		return new PropertiesApi();
	}

	@Bean
	public MetadataApi metadataApi() {
		return new MetadataApi();
	}

	@Bean
	public RepeaterApi repeaterApi() {
		return new RepeaterApi();
	}

	@Bean
	public InjectionApi injectionApi() {
		return new InjectionApi();
	}

	@Bean
	public TokenApi oAuth2AdminApi() {
		return new TokenApi();
	}

	@Bean
	public CustomExceptionHandler exceptionHandler() {
		return new CustomExceptionHandler();
	}

	@Bean
	public ApiDependencyProxy apiDependencyProxy() {
		return new ApiDependencyProxy();
	}

	@Bean
	public RegistrationApi registrationApi() {
		return new RegistrationApi();
	}

	@Bean
	public LocateApi locateApi() {
		return new LocateApi();
	}

	@Bean
	public UserRegistrationService userRegistrationService() {
		return new UserRegistrationService();
	}

	@Bean
	public PluginManagerApi pluginManagerApi() {
		return new PluginManagerApi();
	}
	
	@Bean
	public PluginDataApi pluginDataApi() {
		return new PluginDataApi();
	}

	@Bean
	public RetentionApi retentionApi() {
		return new RetentionApi();
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		try {
			if (coreConfig().getRemoteConfiguration().getNetwork().isAllowAllOrigins()) {
				registry.addMapping("/**").allowedMethods("OPTIONS", "HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
			}
		} catch (Exception e) {
			logger.error("exception in addCorsMappings!", e);
		}
	}

	@Bean
	public PluginManagerProxyFactory pluginManagerProxyFactory() {
		return new PluginManagerProxyFactory();
	}

	@Bean
	public RetentionPolicyConfigProxyFactory retentionPolicyConfigurationProxyFactory() {
		return new RetentionPolicyConfigProxyFactory();
	}
	
	@Bean
	public MissionArchiveManagerProxyFactory missionArchiveManagerProxyFactory() {
		return new MissionArchiveManagerProxyFactory();
	}

	@Bean
	public RetentionQueryService retentionQueryService(Ignite ignite, GroupManager groupManager) {
		DistributedRetentionQueryManager distributedRetentionQueryManager =  new DistributedRetentionQueryManager(ignite, groupManager);
		ignite.services(ClusterGroupDefinition.getApiClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_RETENTION_QUERY_MANAGER,
				distributedRetentionQueryManager);

		return ignite.services(ClusterGroupDefinition.getApiLocalClusterDeploymentGroup(ignite))
				.serviceProxy(Constants.DISTRIBUTED_RETENTION_QUERY_MANAGER, RetentionQueryService.class, false);
	}

	@Bean("missionCacheResolver")
	public MissionCacheResolver missionCacheResolver() { return new MissionCacheResolver(); }
	
	@Bean
	public QoSManager qosManager(Ignite ignite) {
		
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_QOS_MANAGER, QoSManager.class, false);
		
	}
	
	@Bean
	public QoSApi qosApi()  {
		return new QoSApi();
	}

	
	@Bean
	public LoginAccessController loginAcccessController() {
		return new LoginAccessController();
	}
	
	@Bean
	public FileUserAccountManagementApi fileUserAccountManagementApi() {
		return new FileUserAccountManagementApi();
	}
	
	@Bean("myFileUserManagementInterface")
	public FileUserManagementInterface distributedUserManager(Ignite ignite) {
		
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_USER_FILE_MANAGER, FileUserManagementInterface.class, false);
		
	}
	
	@Bean
	public VBMConfigurationApi vbmConfigurationApi() {
		return new VBMConfigurationApi();
	}
	
	@Bean
	public FileConfigurationApi fileConfigurationApi() {
		return new FileConfigurationApi();
	}
	
	@Bean
	public VideoConnectionManagerV2 videoConnectionManagerV2()  {
		return new VideoConnectionManagerV2();
	}
	
	@Bean
	public ContactCacheHelper contactCacheHelper(CoreConfig conf) {
		return new ContactCacheHelper();
	}
	
	@Bean
	public ExecutorSource executorSource(CoreConfig conf) {
		return new ExecutorSource(conf);
	}

	@Bean
	public PluginMissionApi pluginMissionApi(Ignite ignite) {
		
		DistributedPluginMissionApi distributedPluginMissionApi =  new DistributedPluginMissionApi();
		
		ignite.services(ClusterGroupDefinition.getApiClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_PLUGIN_MISSION_API, distributedPluginMissionApi);

		return ignite.services(ClusterGroupDefinition.getApiLocalClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_MISSION_API, PluginMissionApi.class, false);

	}
	
	public DataFeedFederationAspect DataFeedFederationAspect(MissionFederationManager mfm) {
		return new DataFeedFederationAspect(null, null, mfm);
	}
	
	@Bean
	public PluginFileApi pluginFileApi(Ignite ignite) {
		
		DistributedPluginFileApi distributedPluginFileApi =  new DistributedPluginFileApi();
		
		ignite.services(ClusterGroupDefinition.getApiClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_PLUGIN_FILE_API, distributedPluginFileApi);

		return ignite.services(ClusterGroupDefinition.getApiLocalClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_FILE_API, PluginFileApi.class, false);

	}

}