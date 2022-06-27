package tak.server.retention.scheduler;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import tak.server.retention.service.LocalQueryService;

import com.bbn.marti.remote.service.RetentionQueryService;

@Service
public class ExpirationTaskService {
    private static final Logger logger = LoggerFactory.getLogger(ExpirationTaskService.class);

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private LocalQueryService localQueryService;

    @Autowired
    private RetentionQueryService retentionQueryService;

    // A map for keeping scheduled tasks
    public Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    public void scheduleAllTasks() {
        scheduleAllResourceTasks();
        scheduleAllMissionTasks();
    }

    public void scheduleAllResourceTasks() {
        List<Map<String, Object>> results = localQueryService.getResourceExpirations();
// todo add the id to hash to form a unique name.
        for (Map m : results) {
            String hash = (String) m.get("hash");
            Long expiration = (Long) m.get("expiration");
            logger.info(" hash " + hash + " expiration " + expiration.toString());
            Date date = new Date(expiration * 1000);
            logger.info(" tasks is scheduled for " + date);
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(new ResourceExpiryTask(hash, expiration), date);
            scheduledTasks.put(hash, scheduledTask);
        }
    }

    public void scheduleAllMissionTasks() {
        List<Map<String, Object>> results = localQueryService.getMissionExpirations();
        for (Map m : results) {
            String name = (String) m.get("name");
            Long expiration = (Long) m.get("expiration");
            logger.info(" name " + name + " expiration " + expiration.toString());
            String finalName = name;
            Date date = new Date(expiration * 1000);
            logger.info(" tasks is scheduled for " + date);
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(new MissionExpiryTask(name, expiration), date);
            scheduledTasks.put(name, scheduledTask);
        }
    }

    // Remove scheduled task
    public void cancelFutureScheduledTask(String id) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(id);
        if(scheduledTask != null) {
            scheduledTask.cancel(true);
            logger.info("scheduledTask is being cancelled = " + scheduledTask + " the id is " + id);

            scheduledTasks.put(id, null);
        } else {
            logger.info("scheduledTask is null? " + id);
        }
    }

    public void scheduleMissionExpiryTask(String taskName, Long seconds) {
        Date date = new Date(Long.valueOf(seconds) * 1000);
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(new MissionExpiryTask(taskName, seconds), date);
        scheduledTasks.put(taskName, scheduledTask);
        logger.info(" Schedule new mission task: " + taskName + " at time " + date);

    }

    public void scheduleMissionPackageExpiryTask(String taskName, Long seconds) {
        Date date = new Date(Long.valueOf(seconds) * 1000);
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(new MissionPackageExpiryTask(taskName, seconds), date);
        scheduledTasks.put(taskName, scheduledTask);
        logger.info(" Schedule new mission task: " + taskName + " at time " + date);
    }

    public void scheduleResourceExpiryTask(String taskName, Long seconds) {
        Date date = new Date(Long.valueOf(seconds) * 1000);
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(new ResourceExpiryTask(taskName, seconds), date);
        scheduledTasks.put(taskName, scheduledTask);
        logger.info(" Schedule new  resource task: " + taskName + " at time " + date);
    }

    class ResourceExpiryTask implements Runnable {
        private String name;
        private Long expiration;

        public ResourceExpiryTask(String name, Long expiration) {
            this.name = name;
            this.expiration = expiration;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getExpiration() {
            return expiration;
        }

        public void setExpiration(Long expiration) {
            this.expiration = expiration;
        }

        @Override
        public void run() {
            logger.info("Running task " + name);
            localQueryService.deleteFilesByExpiration(expiration);
        }
    }

    class MissionExpiryTask implements Runnable {
        private String name;
        private Long expiration;

        public MissionExpiryTask(String name, Long expiration) {
            this.name = name;
            this.expiration = expiration;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getExpiration() {
            return expiration;
        }

        public void setExpiration(Long expiration) {
            this.expiration = expiration;
        }

        @Override
        public void run() {
            logger.info("Running task " + name);
            // delete using the mission service
            retentionQueryService.deleteMissionByExpiration(expiration);
        }
    }

    class MissionPackageExpiryTask implements Runnable {
        private String name;
        private Long expiration;

        public MissionPackageExpiryTask(String name, Long expiration) {
            this.name = name;
            this.expiration = expiration;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getExpiration() {
            return expiration;
        }

        public void setExpiration(Long expiration) {
            this.expiration = expiration;
        }

        @Override
        public void run() {
            logger.info("Running task " + name);
            localQueryService.deleteMissionPackageByExpiration(expiration);
        }
    }
}
