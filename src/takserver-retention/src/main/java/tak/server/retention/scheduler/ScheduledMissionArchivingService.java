package tak.server.retention.scheduler;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import tak.server.retention.config.MissionArchivingCronConfig;
import tak.server.retention.service.LocalQueryService;
import tak.server.retention.service.MissionArchiveHelper;

import com.bbn.marti.remote.service.RetentionQueryService;

@Service
@EnableScheduling
public class ScheduledMissionArchivingService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledMissionArchivingService.class);


    @Autowired
    RetentionQueryService retentionQueryService;

    @Autowired
    LocalQueryService localQueryService;
    
    @Autowired 
    MissionArchiveHelper missionArchiveHelper;
    
    @Autowired
    MissionArchivingCronConfig missionArchivingCronConfig;

    @Override
    public void run() {
        try {

            // this is the remote mission service
            if (retentionQueryService == null) {
                logger.info(" Retention Query Service is not ready ");
                return;
            }
            if (localQueryService == null) {
                logger.info(" Data Query Service is not ready ");
                return;
            }
            logger.info(" Mission Archiving Service Running ");
            
            Set<Map<String,Object>> missionsToArchive = new HashSet<>();
            if (missionArchivingCronConfig.isArchiveMissionByNoContentActivity()) {
                localQueryService
            		.getMissionExpirationForArchivalByLatestMissionChanges()
        			.entrySet()
        			.stream()
        			.filter(missionChange-> ((float)(new Date().getTime() - missionChange.getValue().getTime()) / 1000. / 60. / 60. / 24.) > missionArchivingCronConfig.getTimeToArchiveAfterNoActivityDays())
        			.forEach(missionChange->missionsToArchive.add(missionChange.getKey()));
            }
            
            if (missionArchivingCronConfig.isArchiveMissionByNoSubscriptionActivity()) {
            	localQueryService
            		.getMissionExpirationForArchivalByLatestSubscribe()
        			.entrySet()
        			.stream()
        			.filter(missionSub-> ((float)(new Date().getTime() - missionSub.getValue().getTime()) / 1000. / 60. / 60. / 24.) > missionArchivingCronConfig.getTimeToArchiveAfterNoActivityDays())
        			.forEach(missionSub->missionsToArchive.add(missionSub.getKey()));
            }
            
            missionsToArchive.forEach(missionToArchive->missionArchiveHelper.archiveMissionAndDelete(missionToArchive));
            
            missionArchiveHelper.removeExpiredArchiveEntries(missionArchivingCronConfig.getRemoveFromArchiveAfterDays());

        } catch (Exception e) {
            logger.error("Error accessing retention query service ", e);
        }
    }
}
