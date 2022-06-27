package tak.server.retention;

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
import tak.server.retention.config.DistributedRetentionPolicyConfig;
import tak.server.retention.scheduler.ExpirationTaskService;
import tak.server.retention.scheduler.SingleTaskSchedulerService;
import tak.server.retention.service.DistributedMissionArchiveManager;
import tak.server.retention.service.MissionArchiveHelper;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.service.MissionArchiveManager;
import com.bbn.marti.remote.service.RetentionPolicyConfig;
import com.bbn.marti.remote.service.RetentionQueryService;

@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
public class RetentionApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(RetentionApplication.class);

    @Override
    public void run(String... args) throws Exception { }

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(RetentionApplication.class, args);

        SingleTaskSchedulerService  singleTaskSchedulerService = ctx.getBean(SingleTaskSchedulerService.class);
        singleTaskSchedulerService.loadRetentionServiceTask();
        singleTaskSchedulerService.loadMissionArchivingTask();

        ExpirationTaskService expirationTaskService = ctx.getBean(ExpirationTaskService.class);
        expirationTaskService.scheduleAllTasks();

        logger.info(" Retention Application started  " );
    }

    @Bean
    Ignite ignite() {
        return Ignition.getOrStart(IgniteConfigurationHolder.getInstance().getIgniteConfiguration(Constants.RETENTION_PROFILE_NAME,
                "127.0.0.1",
                false, false, false, false,
                47500, 100,
                47100, 100, 512,
                600000, 52428800, 52428800));
    }

    @Bean
    public RetentionQueryService dataQueryManager(Ignite ignite) {
        return ignite.services(ClusterGroupDefinition.getApiClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_RETENTION_QUERY_MANAGER,
                RetentionQueryService.class, false);
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
       return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_CONFIGURATION, CoreConfig.class, false);
    }
    
    @Bean
    public MissionArchiveHelper missionArchiveHelper() {
       return new MissionArchiveHelper();
    }

}
