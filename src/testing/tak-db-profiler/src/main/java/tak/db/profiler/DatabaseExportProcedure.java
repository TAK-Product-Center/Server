package tak.db.profiler;

import java.io.File;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import jakarta.annotation.PostConstruct;
import tak.db.profiler.table.CotRouterTable;
import tak.db.profiler.table.GroupsTable;
import tak.db.profiler.table.MissionResourceTable;
import tak.db.profiler.table.MissionTable;
import tak.db.profiler.table.MissionUidTable;
import tak.db.profiler.table.ResourceTable;
import tak.db.profiler.table.DatabaseStatistics;

public class DatabaseExportProcedure {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseExportProcedure.class);
	private TakDBProfilerParams profilerParams;
	
	@Autowired
	DataSource dataSource;
	
	@Autowired
	DatabaseStatistics databaseStatistics;
	
	@Autowired
	@Qualifier("groupsTableDb")
	GroupsTable groupsTableDb;
	
	@Autowired
	@Qualifier("cotRouterTableDb")
	CotRouterTable cotRouterTableDb;
	
	@Autowired
	@Qualifier("resourceTableDb")
	ResourceTable resourceTableDb;
	
	@Autowired
	@Qualifier("missionTableDb")
	MissionTable missionTableDb;
	
	@Autowired
	@Qualifier("missionResourceTableDb")
	MissionResourceTable missionResourceTableDb;

	@Autowired
	@Qualifier("missionUidTableDb")
	MissionUidTable missionUidTableDb;

	public DatabaseExportProcedure(TakDBProfilerParams profilerParams) {
		this.profilerParams = profilerParams;
	}

	@PostConstruct
	public void init() {
		String outputDir = profilerParams.getConfigDir();
	    
	    saveToFile(groupsTableDb, outputDir + "/" + TakDBProfilerConstants.GROUPS_FILE_NAME);
	    saveToFile(missionTableDb, outputDir + "/" + TakDBProfilerConstants.MISSIONS_FILE_NAME);
	    saveToFile(missionUidTableDb, outputDir + "/" + TakDBProfilerConstants.MISSION_UIDS_FILE_NAME);
	    saveToFile(missionResourceTableDb, outputDir + "/" + TakDBProfilerConstants.MISSION_RESOURCES_FILE_NAME);
	    saveToFile(resourceTableDb, outputDir + "/" + TakDBProfilerConstants.RESOURCES_FILE_NAME);
	    saveToFile(cotRouterTableDb, outputDir + "/" + TakDBProfilerConstants.COT_ROUTER_FILE_NAME);
	    saveToFile(databaseStatistics, outputDir + "/" + TakDBProfilerConstants.DATABASE_STATISTICS_FILE_NAME);	
	}
	
	public static void saveToFile(Object o, String path) {
		try {
			ObjectMapper om = new ObjectMapper(new YAMLFactory());
			om.writeValue(new File(path), o);
			logger.info("Successfully wrote configuration " + path);
		} catch (Exception e) {
			logger.error("Error writing " + path, e);
		}
		
	}
}
