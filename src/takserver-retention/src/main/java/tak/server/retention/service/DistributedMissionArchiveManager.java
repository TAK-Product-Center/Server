package tak.server.retention.service;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronSequenceGenerator;
import tak.server.retention.SpringContextBeanForRetention;
import tak.server.retention.config.MissionArchivingCronConfig;
import tak.server.retention.scheduler.SingleTaskSchedulerService;

import com.bbn.marti.remote.MissionArchiveConfig;
import com.bbn.marti.remote.service.MissionArchiveManager;

public class DistributedMissionArchiveManager implements MissionArchiveManager, Service {
	
	private static final String MISSION_ARCHIVE_CONFIG = "conf/retention/mission-archiving-config.yml";

	MissionArchiveHelper missionArchiveHelper;

	MissionArchivingCronConfig missionArchivingCronConfig;

	SingleTaskSchedulerService retentionTaskService;

	private static final Logger logger = LoggerFactory.getLogger(DistributedMissionArchiveManager.class);

	private MissionArchivingCronConfig missionArchivingCronConfig() {
		return SpringContextBeanForRetention.getSpringContext().getBean(MissionArchivingCronConfig.class);
	}

	private MissionArchiveHelper missionArchiveHelper() {
		return SpringContextBeanForRetention.getSpringContext().getBean(MissionArchiveHelper.class);
	}

	private SingleTaskSchedulerService retentionTaskService() {
		return SpringContextBeanForRetention.getSpringContext().getBean(SingleTaskSchedulerService.class);
	}
	@Override
	public void cancel(ServiceContext ctx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public String getMissionArchive() {
		return missionArchiveHelper().getMissionStoreJson();
	}

	@Override
	public String restoreMissionFromArchive(int id) {
		return missionArchiveHelper().restoreMissionFromArchive(id);
	}

	@Override
	public MissionArchiveConfig getMissionArchiveConfig() {
		MissionArchiveConfig missionArchiveConfig = new MissionArchiveConfig();
		missionArchiveConfig.setArchiveMissionByNoContentActivity(missionArchivingCronConfig().isArchiveMissionByNoContentActivity());
		missionArchiveConfig.setArchiveMissionByNoSubscriptionActivity(missionArchivingCronConfig().isArchiveMissionByNoSubscriptionActivity());
		missionArchiveConfig.setCronExpression(missionArchivingCronConfig().getMissionCronExpression());
		missionArchiveConfig.setRemoveFromArchiveAfterDays(missionArchivingCronConfig().getRemoveFromArchiveAfterDays());
		missionArchiveConfig.setTimeToArchiveAfterNoActivityDays(missionArchivingCronConfig().getTimeToArchiveAfterNoActivityDays());
		return missionArchiveConfig;
	}

	@Override
	public synchronized void updateMissionArchiveConfig(MissionArchiveConfig missionArchiveConfig) {
		// make a copy to write
		MissionArchivingCronConfig newCronConfig = new MissionArchivingCronConfig();
		
		String cronExpression = missionArchiveConfig.getCronExpression();

		if (!CronSequenceGenerator.isValidExpression(cronExpression) && !cronExpression.equals("-")) {
			logger.error(" Invalid cron expression " + cronExpression + " schedule not changed");
		} else {
			newCronConfig.setMissionCronExpression(cronExpression);
			missionArchivingCronConfig().setMissionCronExpression(cronExpression);
		}
		
		newCronConfig.setArchiveMissionByNoSubscriptionActivity(missionArchiveConfig.getArchiveMissionByNoSubscriptionActivity());
		newCronConfig.setArchiveMissionByNoContentActivity(missionArchiveConfig.getArchiveMissionByNoContentActivity());
		newCronConfig.setRemoveFromArchiveAfterDays(missionArchiveConfig.getRemoveFromArchiveAfterDays());
		newCronConfig.setTimeToArchiveAfterNoActivityDays(missionArchiveConfig.getTimeToArchiveAfterNoActivityDays());
		
		missionArchivingCronConfig().setArchiveMissionByNoSubscriptionActivity(missionArchiveConfig.getArchiveMissionByNoSubscriptionActivity());
		missionArchivingCronConfig().setArchiveMissionByNoContentActivity(missionArchiveConfig.getArchiveMissionByNoContentActivity());
		missionArchivingCronConfig().setRemoveFromArchiveAfterDays(missionArchiveConfig.getRemoveFromArchiveAfterDays());
		missionArchivingCronConfig().setTimeToArchiveAfterNoActivityDays(missionArchiveConfig.getTimeToArchiveAfterNoActivityDays());
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
		try {
			File file = new File(MISSION_ARCHIVE_CONFIG);

			mapper.writeValue(file, newCronConfig);

			retentionTaskService().scheduleMissionArchivingTask(cronExpression);
		} catch (IOException e) {
			logger.error(" Exception saving mission archive config for " + missionArchiveConfig);
		}
	}

}
