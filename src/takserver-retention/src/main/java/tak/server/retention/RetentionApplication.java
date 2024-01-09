package tak.server.retention;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.bbn.marti.config.TAKIgniteConfiguration;
import com.bbn.marti.remote.exception.TakException;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
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
import tak.server.retention.config.DistributedRetentionPolicyConfig;
import tak.server.retention.scheduler.ExpirationTaskService;
import tak.server.retention.scheduler.SingleTaskSchedulerService;
import tak.server.retention.service.DistributedMissionArchiveManager;
import tak.server.retention.service.MissionArchiveHelper;
import tak.server.util.ActiveProfiles;
import tak.server.util.DataSourceUtils;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.service.MissionArchiveManager;
import com.bbn.marti.remote.service.RetentionPolicyConfig;
import com.bbn.marti.remote.service.RetentionQueryService;
import com.bbn.marti.remote.util.LoggingConfigPropertiesSetupUtil;
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
        System.setProperty("spring.profiles.active",  Constants.RETENTION_PROFILE_NAME);

        SpringApplication application = new SpringApplication();
        LoggingConfigPropertiesSetupUtil.getInstance().setupLoggingConfiguration();
        ApplicationContext ctx = application.run(RetentionApplication.class, args);
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
        // setup the IgniteConfigurationHolder here
        TAKIgniteConfiguration takIgniteConfiguration =
                IgniteConfigurationHolder.getInstance().getTAKIgniteConfiguration();
        IgniteConfiguration ic = IgniteConfigurationHolder.getInstance().getIgniteConfiguration(
                Constants.RETENTION_PROFILE_NAME, takIgniteConfiguration);
        IgniteConfigurationHolder.getInstance().setIgniteConfiguration(ic);
        return Ignition.getOrStart(ic);
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
    public MissionArchiveHelper missionArchiveHelper() {
       return new MissionArchiveHelper();
    }

    @Bean
    public HikariDataSource dataSource() {
        HikariDataSource hikariDataSource = null;
        hikariDataSource = DataSourceUtils.setupDataSourceFromCoreConfig();
        hikariDataSource.setMaximumPoolSize(2);
        return hikariDataSource;
    }
}
