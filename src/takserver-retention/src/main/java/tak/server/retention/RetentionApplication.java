package tak.server.retention;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import tak.server.Constants;
import tak.server.ignite.IgniteConfigurationHolder;
import tak.server.plugins.PluginManagerConstants;
import tak.server.retention.config.DistributedRetentionPolicyConfig;
import tak.server.retention.scheduler.ExpirationTaskService;
import tak.server.retention.scheduler.SingleTaskSchedulerService;
import tak.server.retention.service.DistributedMissionArchiveManager;
import tak.server.retention.service.MissionArchiveHelper;
import tak.server.util.DataSourceUtils;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.service.MissionArchiveManager;
import com.bbn.marti.remote.service.RetentionPolicyConfig;
import com.bbn.marti.remote.service.RetentionQueryService;
import tak.server.util.JavaVersionChecker;

@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
public class RetentionApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(RetentionApplication.class);

    @Override
    public void run(String... args) throws Exception { }

    public static void main(String[] args) {
        JavaVersionChecker.check();
        ApplicationContext ctx = SpringApplication.run(RetentionApplication.class, args);
        JavaVersionChecker.check(logger);

        SingleTaskSchedulerService  singleTaskSchedulerService = ctx.getBean(SingleTaskSchedulerService.class);
        singleTaskSchedulerService.loadRetentionServiceTask();
        singleTaskSchedulerService.loadMissionArchivingTask();

        ExpirationTaskService expirationTaskService = ctx.getBean(ExpirationTaskService.class);
        expirationTaskService.scheduleAllTasks();

        logger.info(" Retention Application started  " );
    }

    @Bean
    SpringContextBeanForRetention SpringContextBeanForRetention() {
        return new SpringContextBeanForRetention();
    }

    @Bean
    Ignite ignite() {
    	return Ignition.getOrStart(IgniteConfigurationHolder.getInstance().getIgniteConfiguration
				(Constants.RETENTION_PROFILE_NAME, "127.0.0.1", false, false, false, false, 47500, 100, 47100, 100, 512, 600000, 52428800, 52428800, -1, false, -1.f, false, false, -1, false, 300000, 300000, 600000));
    }

    @Bean
    public RetentionQueryService dataQueryManager(Ignite ignite) {
        final RetentionQueryService retentionQueryService = ignite.services(ClusterGroupDefinition.getApiClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_RETENTION_QUERY_MANAGER,
                RetentionQueryService.class, false);

         boolean isRetentionQueryServiceAvailable = false;

        // block and wait for Retention Query Service (aka MissionService) to become available
        try {
            isRetentionQueryServiceAvailable = canAccessRetentionQueryService(retentionQueryService).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("interrupted checking retention query service availability", e);
        }
        logger.info("retention query service available: {}",isRetentionQueryServiceAvailable);
        return retentionQueryService;
    }

    private CompletableFuture<Boolean> canAccessRetentionQueryService(RetentionQueryService retentionQueryService) {
        try {
            logger.info("Waiting for the Retention Query process... ");
            return CompletableFuture.completedFuture(accessRetentionQueryService(retentionQueryService));
        } catch (Exception e) {
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e1) {
                logger.error("interrupted sleep", e1);
            }
            return canAccessRetentionQueryService(retentionQueryService);
        }
    }

    private boolean accessRetentionQueryService(RetentionQueryService retentionQueryService) throws Exception {
        retentionQueryService.getAllMissions(true, true, "public"); // just test the service
        return true;
    }

    @Bean
    public RetentionPolicyConfig dataRetentionPolicyService(Ignite ignite) {
        DistributedRetentionPolicyConfig dpc = new DistributedRetentionPolicyConfig();
        ignite.services(ClusterGroupDefinition.getRetentionClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_RETENTION_POLICY_CONFIGURATION, dpc);

        return ignite.services(ClusterGroupDefinition.getRetentionClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_RETENTION_POLICY_CONFIGURATION,
                RetentionPolicyConfig.class, false);
    }
    
    @Bean
    public MissionArchiveManager missionArchiveManager(Ignite ignite) {
    	DistributedMissionArchiveManager mas = new DistributedMissionArchiveManager();
        ignite.services(ClusterGroupDefinition.getRetentionClusterDeploymentGroup(ignite)).deployNodeSingleton(Constants.DISTRIBUTED_MISSION_ARCHIVE_MANAGER, mas);

        return ignite.services(ClusterGroupDefinition.getRetentionClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_MISSION_ARCHIVE_MANAGER,
        		MissionArchiveManager.class, false);
    }

    @Bean
    public CoreConfig coreConfig(Ignite ignite) {
       final CoreConfig coreConfig = ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_CONFIGURATION,
                CoreConfig.class, false);

        boolean isCoreConfigAvailable = false;

        // block and wait for CoreConfig to become available in messaging process
        try {
            isCoreConfigAvailable = canAccessCoreConfig(coreConfig).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("interrupted checking CoreConfig availability", e);
        }

        logger.info("CoreConfig available: {}", isCoreConfigAvailable);

        return coreConfig;
    }

    @Bean
    public MissionArchiveHelper missionArchiveHelper() {
       return new MissionArchiveHelper();
    }

    private CompletableFuture<Boolean> canAccessCoreConfig(final CoreConfig coreConfig) {

        try {
            logger.info("Waiting for the Core Config process...");
            return CompletableFuture.completedFuture(accessCoreConfig(coreConfig));
        } catch (Exception e) {
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e1) {
                logger.error("interrupted sleep", e1);
            }
            return canAccessCoreConfig(coreConfig);
        }
    }

    private boolean accessCoreConfig(CoreConfig coreConfig) throws Exception {
        coreConfig.getRemoteConfiguration().getRepository().getConnection(); // check for config
        return true;
    }

    @Bean
    public HikariDataSource dataSource(CoreConfig coreConfig) {
        HikariDataSource hikariDataSource = null;
        hikariDataSource = DataSourceUtils.setupDataSourceFromCoreConfig(coreConfig);
        hikariDataSource.setMaximumPoolSize(2);
        return hikariDataSource;
    }
}
