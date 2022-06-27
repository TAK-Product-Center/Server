package tak.server.retention.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import tak.server.retention.config.RetentionPolicy;
import tak.server.retention.service.LocalQueryService;

import com.bbn.marti.remote.service.RetentionQueryService;

@Service
@EnableScheduling
public class ScheduledRetentionService implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(ScheduledRetentionService.class);

    public static final String COT = "cot";
    public static final String FILES = "files";
    public static final String GEO_CHAT = "geochat";
    public static final String MISSIONS = "missions";
    public static final String MISSION_PACKAGES = "missionpackages";

    @Autowired
    RetentionPolicy retentionPolicy;

    @Autowired
    RetentionQueryService retentionQueryService;

    @Autowired
    LocalQueryService localQueryService;

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
            logger.info(" Reaper Service Running ");
            if (logger.isDebugEnabled()) {
                logger.debug(" retention map  " + retentionPolicy.getDataRetentionMap());
            }
// TODO this is the mission service -> need to rename for clarity
            retentionQueryService.deleteMissionByTtl(retentionPolicy.getDataRetentionMap().get(MISSIONS));

            localQueryService.deleteFilesByTtl(retentionPolicy.getDataRetentionMap().get(FILES));
            localQueryService.deleteCotByTtl(retentionPolicy.getDataRetentionMap().get(COT));
            localQueryService.deleteGeoChatByTtl(retentionPolicy.getDataRetentionMap().get(GEO_CHAT));
            // delete geo chat messages from legacy cot_router table
            localQueryService.deleteLegacyGeoChatByTtl(retentionPolicy.getDataRetentionMap().get(GEO_CHAT));

            // I don't think I need this anymore since we are adding the policy to all files
//            localQueryService.deleteMissionPackageByTtl(retentionPolicy.getDataRetentionMap().get(MISSION_PACKAGES));

        } catch (Exception e) {
            logger.error("Error accessing retention query service ", e);
        }
    }


}
