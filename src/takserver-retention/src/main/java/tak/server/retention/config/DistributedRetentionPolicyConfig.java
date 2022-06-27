package tak.server.retention.config;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;
import tak.server.retention.scheduler.ExpirationTaskService;
import tak.server.retention.scheduler.SingleTaskSchedulerService;

import com.bbn.marti.remote.service.RetentionPolicyConfig;

public class DistributedRetentionPolicyConfig implements RetentionPolicyConfig, Service {

    private static final Logger logger = LoggerFactory.getLogger(DistributedRetentionPolicyConfig.class);

    private static final String RETENTION_POLICY = "conf/retention/retention-policy.yml";

    private static final String RETENTION_SERVICE = "conf/retention/retention-service.yml";

    @Autowired
    CronConfig cronConfig;

    @Autowired
    RetentionPolicy retentionPolicy;

    @Autowired
    SingleTaskSchedulerService retentionTaskService;

    @Autowired
    ExpirationTaskService expirationTaskService;

    public Map<String, Integer> updateRetentionPolicyMap(Map<String, Integer> policyMap) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        try {
            File file = new File(RETENTION_POLICY);
            logger.info(" writing to app.home " + file.getAbsolutePath() );
            Map<String, Integer> map = retentionPolicy.getDataRetentionMap();
            map.putAll(policyMap);

            // make a copy of the object for writing
            mapper.writeValue(file, new RetentionPolicy(map));
            logger.info("writing new schedule " + file.getAbsolutePath());
        } catch (IOException e) {
            logger.error(" Exception saving retention policy for " + policyMap.toString());
        }
        return retentionPolicy.getDataRetentionMap();
    }

    @Override
    public Map<String, Integer> getRetentionPolicyMap() {
        logger.info(" get retention policy map " + retentionPolicy.getDataRetentionMap());
        return retentionPolicy.getDataRetentionMap();
    }

    @Override
    public String setRetentionServiceSchedule(String cronExpression) {

        // TODO we should allow the schedule to be disabled using "-"
        // verify that other strings are valid

        if (logger.isDebugEnabled()) {
            logger.debug(" what is the cron express " + cronExpression);
        }
        if (! CronSequenceGenerator.isValidExpression(cronExpression) && !cronExpression.equals("-")) {
            logger.error(" Invalid cron expression " + cronExpression + " schedule not changed");
            return null;
        }
        if (!cronExpression.equals("-")) {
            CronSequenceGenerator generator = new CronSequenceGenerator(cronExpression, TimeZone.getTimeZone("EDT"));
            Date nextRunDate = generator.next(new Date());

            logger.info(" what is the current next run time " + nextRunDate);
        }



        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        try {
            cronConfig.setCronExpression(cronExpression);
            File file = new File(RETENTION_SERVICE);

            // make a copy to write
            CronConfig newCronConfig = new CronConfig();
            newCronConfig.setCronExpression(cronExpression);
            mapper.writeValue(file, newCronConfig);
            if (logger.isDebugEnabled()){
                logger.debug("writing new schedule " + file.getAbsolutePath());
            }
            setNewSchedule(cronExpression);
        } catch (IOException e) {
            logger.error(" Exception saving retention service schedule for " + cronExpression);
        }
        return cronConfig.getCronExpression();
    }


    public void setNewSchedule(String cronExpression) {
        retentionTaskService.scheduleRetentionServiceTask(cronExpression);

        if (cronExpression.equals("-")) {
            logger.info("the new cron trigger is \"-\" and the next execution time is never");
            return;
        }
        CronTrigger cronTrigger = new CronTrigger(cronExpression);
        CronSequenceGenerator generator = new CronSequenceGenerator(cronExpression);
        Date nextRunDate = generator.next(new Date());
        logger.info( "what is the new cron trigger"  + cronTrigger.getExpression() + " what is the next execution time " + nextRunDate);

    }

    @Override
    public void setMissionExpiryTask(String taskName, Long seconds) {

        logger.info(" setting mission expiration for " + taskName + " expiration in seconds" + seconds);

        expirationTaskService.cancelFutureScheduledTask(taskName);
        if (seconds != null && seconds > -1) {
            logger.info(" set mission task " + taskName + " seconds " + seconds);
            expirationTaskService.scheduleMissionExpiryTask(taskName, seconds);
        }
    }

    @Override  // delete this no longer used
    public void setMissionPackageExpiryTask(String taskName, Long seconds) {
        logger.info(" setting mission package expiration for " + taskName + " expiration in seconds" + seconds);

        expirationTaskService.cancelFutureScheduledTask(taskName);
        if (seconds != null && seconds > -1) {
            logger.info(" set mission package task " + taskName + " seconds " + seconds);
            expirationTaskService.scheduleMissionPackageExpiryTask(taskName, seconds);
        }
    }

    @Override
    public void setResourceExpiryTask(String taskName, Long seconds) {
        logger.info(" setting resource expiration for " + taskName + " expiration in seconds" + seconds);

        expirationTaskService.cancelFutureScheduledTask(taskName);
        if (seconds != null && seconds > -1) {
            logger.info(" set resource task " + taskName + " seconds " + seconds);
            expirationTaskService.scheduleResourceExpiryTask(taskName, seconds);
        }
    }

    @Override
    public String getRetentionServiceSchedule() {
        return cronConfig.getCronExpression();
    }

    @Override
    public void cancel(ServiceContext ctx) {
        if (logger.isDebugEnabled()) {
            logger.debug(getClass().getSimpleName() + " service cancelled");
        }
    }

    @Override
    public void init(ServiceContext ctx) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(" init method " + getClass().getSimpleName());
        }
    }

    @Override
    public void execute(ServiceContext ctx) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(" execute method " + getClass().getSimpleName());
        }
    }
}
