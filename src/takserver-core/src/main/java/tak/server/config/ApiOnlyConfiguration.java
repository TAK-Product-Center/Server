package tak.server.config;

import java.util.concurrent.Executor;

import org.apache.ignite.Ignite;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bbn.marti.config.Auth;
import com.bbn.marti.groups.DistributedPersistentGroupManager;
import com.bbn.marti.groups.GroupDao;
import com.bbn.marti.groups.GroupStore;
import com.bbn.marti.groups.InMemoryGroupStore;
import com.bbn.marti.groups.LdapAuthenticator;
import com.bbn.marti.groups.PersistentGroupDao;
import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.RepeaterManager;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.service.MissionCacheWarmer;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.spring.RequestHolderBean;
import com.bbn.marti.util.spring.RequestHolderFilterBean;
import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.service.DatabaseMetricsService;

import tak.server.CommonConstants;
import tak.server.Constants;
import tak.server.cache.ActiveGroupCacheHelper;
import tak.server.cot.CotEventContainer;
import tak.server.grid.ContactManagerProxyFactory;
import tak.server.grid.CoreConfigProxyFactoryForAPI;
import tak.server.grid.DistributedDatafeedCotServiceProxyFactory;
import tak.server.grid.FederationManagerProxyFactory;
import tak.server.grid.GroupManagerProxyFactory;
import tak.server.grid.InjectionServiceProxyFactory;
import tak.server.grid.InputManagerProxyFactory;
import tak.server.grid.MetricsCollectorProxyFactory;
import tak.server.grid.RepeaterManagerProxyFactory;
import tak.server.grid.SecurityManagerProxyFactory;
import tak.server.grid.ServerInfoProxyFactory;
import tak.server.grid.SubscriptionManagerProxyFactory;
import tak.server.messaging.DistributedCotMessengerForApi;
import tak.server.messaging.MessageConverter;
import tak.server.messaging.Messenger;
import tak.server.profile.DistributedServerInfo;
import tak.server.qos.MessageDeliveryStrategy;

/*
 * services that are only used in separate API process
 */
@Configuration
@Profile({Constants.API_PROFILE_NAME})
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, ErrorMvcAutoConfiguration.class, SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
public class ApiOnlyConfiguration implements AsyncConfigurer, WebMvcConfigurer {

	@Bean
	public FederationManagerProxyFactory federationManagerProxyFactory() {
		return new FederationManagerProxyFactory();
	}

	@Bean
	public SubscriptionManagerProxyFactory subscriptionManagerProxyFactory() {
		return new SubscriptionManagerProxyFactory();
	}

	@Bean
	@Primary
	public SubscriptionManager subscriptionManager() throws Exception {
		return subscriptionManagerProxyFactory().getObject();
	}

	@Bean
	public GroupStore groupStore() {
		return new InMemoryGroupStore();
	}

	@Bean
	ActiveGroupCacheHelper getActiveGroupCacheHelper() {
		return new ActiveGroupCacheHelper();
	}

	@Bean
	@Primary
	public GroupManager groupManager(GroupStore groupStore) {
		return new DistributedPersistentGroupManager(groupStore);
	}

	@Bean(CommonConstants.MESSENGER_GROUPMANAGER_NAME)
	public GroupManagerProxyFactory groupManagerProxyFactory() {
		return new GroupManagerProxyFactory();
	}

	@Bean
	public MessagingDependencyInjectionProxy dependencyProxy() {
		return new MessagingDependencyInjectionProxy();
	}

	@Bean
	public GroupDao groupDao() {
		return new PersistentGroupDao();
	}

	@Bean(Constants.DISTRIBUTED_COT_MESSENGER)
	public Messenger<CotEventContainer> messenger(Ignite ignite, SubscriptionStore subscriptionStore, ServerInfo serverInfo, MessageConverter messageConverter, CoreConfig config) {
		return new DistributedCotMessengerForApi(ignite, subscriptionStore, serverInfo,  messageConverter, config);
	}

	@Bean
	@Primary
	public ContactManager contactManager() throws Exception {
		return contactManagerProxyFactory().getObject();
	}

	@Bean
	public ContactManagerProxyFactory contactManagerProxyFactory() {
		return new ContactManagerProxyFactory();
	}

	// dependency injection to address Spring bean instantiation
	@Bean
	@Primary
	public CoreConfig coreConfig(CoreConfigProxyFactoryForAPI coreConfigProxyFactoryForAPI) throws Exception {
		return coreConfigProxyFactory().getObject();
	}

	@Bean
	public CoreConfigProxyFactoryForAPI coreConfigProxyFactory() {
		return new CoreConfigProxyFactoryForAPI();
	}

	@Bean
	@Primary
	public RepeaterManager repeaterManager() throws Exception {
		return repeaterManagerProxyFactory().getObject();
	}

	@Bean
	public RepeaterManagerProxyFactory repeaterManagerProxyFactory() {
		return new RepeaterManagerProxyFactory();
	}

//	@Bean
//	@Primary
//	public ServerInfo serverInfo() throws Exception {
//		return serverInfoProxyFactory().getObject();
//	}


	@Bean
	@Primary
	public ServerInfo serverInfo(Ignite ignite, CoreConfig config) throws Exception {
		return new DistributedServerInfo(ignite, config);
	}

	@Bean
	public ServerInfoProxyFactory serverInfoProxyFactory() {
		return new ServerInfoProxyFactory();
	}

	@Bean
	public InputManagerProxyFactory inputManagerProxyFactory() {
		return new InputManagerProxyFactory();
	}

	@Bean
	public InjectionServiceProxyFactory injectionServiceProxyFactory() {
		return new InjectionServiceProxyFactory();
	}

	@Bean
	public SecurityManagerProxyFactory securityManagerProxyFactory() {
		return new SecurityManagerProxyFactory();
	}
	
	@Bean
	public DistributedDatafeedCotServiceProxyFactory distributedDatafeedCotServiceProxyFactory() {
		return new DistributedDatafeedCotServiceProxyFactory();
	}

	@Bean
	@Primary
	public MetricsCollector metricsCollector(DatabaseMetricsService databaseMetricsService) throws Exception {
		MetricsCollector metricsCollector = metricsCollectorProxyFactory().getObject();
		metricsCollector.setApiDatabaseMetricsService(databaseMetricsService);
		return metricsCollector;
	}

	@Bean
	public MetricsCollectorProxyFactory metricsCollectorProxyFactory() {
		return new MetricsCollectorProxyFactory();
	}

	@Bean
	public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
	    return new BeanFactoryPostProcessor() {
	        @Override
	        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	            beanFactory.registerScope("thread", new SimpleThreadScope());
	        }
	    };
	}

	@Bean
	@Scope(scopeName = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public RequestHolderBean requestHolderBean() {
		return new RequestHolderBean();
	}

	@Bean
	public RequestHolderFilterBean requestHolderFilterBean() {
		return new RequestHolderFilterBean();
	}

    @Bean
    public LdapAuthenticator ldapAuthenticator(CoreConfig config, GroupManager groupManager) {
        Auth.Ldap ldapConfig = config.getRemoteConfiguration().getAuth().getLdap();

        if (ldapConfig == null) {
            return null;
        }

        return LdapAuthenticator.getInstance(ldapConfig, groupManager);
    }

    @Bean
	public AsyncTaskExecutor asyncExecutor() {

		int numProc = Runtime.getRuntime().availableProcessors();

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(numProc * 3);
		executor.setMaxPoolSize(numProc * 32);
		executor.setQueueCapacity(1024 * 32);
		executor.setThreadNamePrefix("takserver-api-async-executor-");
		executor.initialize();

		return executor;

	}

    @Bean
    public Executor getAsyncExecutor() {
		return asyncExecutor();
	}

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    	configurer.setTaskExecutor(asyncExecutor());
    	configurer.setDefaultTimeout(-1);  // five minute timeout
    }

    @Bean
    public MissionCacheWarmer cacheWarmer(MissionService missionService) {
    	return new MissionCacheWarmer();
    }
    
    @Bean
	public MessageDeliveryStrategy mds() {
		return new MessageDeliveryStrategy();
	}

}
