package tak.server.retention.scheduler;

import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import tak.server.retention.config.CronConfig;
import tak.server.retention.config.MissionArchivingCronConfig;

@Service
@EnableScheduling
public class SingleTaskSchedulerService {


    private static final Logger logger = LoggerFactory.getLogger(SingleTaskSchedulerService.class);

    // Task Scheduler
    private TaskScheduler scheduler;
    private static final String CRON_DISABLED = "-";

    @Autowired
    CronConfig cronConfig;
    
    @Autowired
    MissionArchivingCronConfig missionArchivingCronConfig;

    @Autowired
    ScheduledRetentionService scheduledRetentionService;
    
    @Autowired
    ScheduledMissionArchivingService scheduledMissionArchivingService;

    ScheduledFuture<?> retentionTask;
    
    ScheduledFuture<?> missionArchiveTask;

    public SingleTaskSchedulerService(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    // Schedule Task to be executed every night at 00 or 12 am
    public ScheduledFuture<?> addTaskToScheduler(String id, Runnable task, CronTrigger cronTrigger) {
       logger.info(" adding task to scheduler " + id);
       return scheduler.schedule(task, cronTrigger);
    }

    // load task schedule from the configuration file
    public void loadRetentionServiceTask() {
        logger.info(" schedule reaper task from configuration file: " + cronConfig.getCronExpression());
        if (cronConfig.getCronExpression().equals(CRON_DISABLED)) {
            logger.info(" Reaper Service is not scheduled " );
        } else {
        	retentionTask = addTaskToScheduler("ReaperService", scheduledRetentionService, new CronTrigger(cronConfig.getCronExpression()));
        }
    }
    
    public void loadMissionArchivingTask() {
        logger.info(" schedule mission archiving task from configuration file: " + missionArchivingCronConfig);
        if (missionArchivingCronConfig.getMissionCronExpression().equals(CRON_DISABLED)) {
            logger.info(" Mission Archiver is not scheduled " );
        } else {
        	missionArchiveTask = addTaskToScheduler("MissionArchivingService", scheduledMissionArchivingService, new CronTrigger(missionArchivingCronConfig.getMissionCronExpression()));
        }
    }

    public void scheduleRetentionServiceTask(String cronExpression) {
    	if (retentionTask != null) {
    		retentionTask.cancel(false);
    	}
    	
        logger.info(" new reaper task schedule: " + cronExpression);
        if (cronConfig.getCronExpression().equals(CRON_DISABLED)) {
            logger.info(" Reaper Service is not scheduled " );
        } else {
        	retentionTask = addTaskToScheduler("ReaperService", scheduledRetentionService, new CronTrigger(cronExpression));
        }
    }
    
    public void scheduleMissionArchivingTask(String cronExpression) {
      	if (missionArchiveTask != null) {
      		missionArchiveTask.cancel(false);
    	}
    	
        logger.info(" new mission archiving task schedule: " + cronExpression);
        if (missionArchivingCronConfig.getMissionCronExpression().equals(CRON_DISABLED)) {
            logger.info(" Mission Archiving Service is not scheduled " );
        } else {
        	missionArchiveTask = addTaskToScheduler("MissionArchivingService", scheduledMissionArchivingService, new CronTrigger(cronExpression));
        }
    }
}
