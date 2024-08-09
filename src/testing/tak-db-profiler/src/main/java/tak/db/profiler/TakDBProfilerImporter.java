package tak.db.profiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import tak.db.profiler.table.CotRouterTable;
import tak.db.profiler.table.GroupsTable;
import tak.db.profiler.table.MissionResourceTable;
import tak.db.profiler.table.MissionTable;
import tak.db.profiler.table.MissionUidTable;
import tak.db.profiler.table.ResourceTable;
import tak.db.profiler.table.DatabaseStatistics;

public class TakDBProfilerImporter {
	private static final Logger logger = LoggerFactory.getLogger(TakDBProfilerImporter.class);
	private static final TakDBProfilerParams profilerParams = new TakDBProfilerParams();
	
	public static void main(String[] args) throws Exception {
		logger.info("IMPORTER");

		Options options = new Options();
	    options.addOption("username", true, "Database username");
	    options.addOption("password", true, "Database password");
	    options.addOption("host", true, "Database host");
	    options.addOption("port", true, "Database port");
	    options.addOption("configDir", true, "Config directory to use");

	    CommandLineParser parser = new DefaultParser();
	    try {
	        CommandLine cmd = parser.parse(options, args);
	        
	        logger.info("-USERNAME");
	        if (cmd.hasOption("username")) {
	        	profilerParams.setUsername(cmd.getOptionValue("username"));
	        	logger.info("\tUsername provided: " + profilerParams.getUsername());
	        } else {
	        	logger.info("\tNo username provided. Using default: " + profilerParams.getUsername());
			}
	        
	        logger.info("-PASSWORD");
	        if (cmd.hasOption("password")) {
	        	profilerParams.setPassword(cmd.getOptionValue("password"));
	        	logger.info("\tPassword provided: " + profilerParams.getPassword());
	        } else {
	        	logger.info("\tNo password provided. Using default: " + profilerParams.getPassword());
			}
	        
	        logger.info("-HOST");
	        if (cmd.hasOption("host")) {
	        	profilerParams.setHost(cmd.getOptionValue("host"));
	        	logger.info("\tHost provided: " + profilerParams.getHost());
	        } else {
	        	logger.info("\tNo host provided. Using default: " + profilerParams.getHost());
			}
	        
	        logger.info("-PORT");
	        if (cmd.hasOption("port")) {
	        	profilerParams.setPort(Integer.parseInt(cmd.getOptionValue("port")));
	        	logger.info("\tPort provided: " + profilerParams.getPort());
	        } else {
	        	logger.info("\tNo port provided. Using default: " + profilerParams.getPort());
			}
	        
	        logger.info("-CONFIG DIR");
	        if (cmd.hasOption("configDir")) {
	        	profilerParams.setConfigDir(cmd.getOptionValue("configDir"));
	        	logger.info("\tConfig directory provided: " + profilerParams.getConfigDir());
	        	new File(profilerParams.getConfigDir()).mkdirs();
	        } else {
	        	logger.info("\tNo config directory provided. Using default: " + profilerParams.getConfigDir());
	        	new File(profilerParams.getConfigDir()).mkdirs();
			}

	    } catch (ParseException e) {
	        System.err.println("Error parsing params: " + e.getMessage());
	        System.exit(1);
	    }
	    
	    SpringApplication application = new SpringApplication(TakDBProfilerImporter.class);
	    application.setWebApplicationType(WebApplicationType.NONE);	    
	    ApplicationContext context = application.run(args);
	}
	
	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public  DatabaseImportProcedure databaseImportProcedure() {
		return new DatabaseImportProcedure();
	}
	
	@Bean
	@Qualifier("databaseStatisticsDB")
	public DatabaseStatistics databaseStatisticsDB(DataSource dataSource) {
		DatabaseStatistics databaseStatistics = new DatabaseStatistics();
		databaseStatistics.initializeTable(dataSource);
		return databaseStatistics;
	}
	
	@Bean
	@Qualifier("databaseStatisticsFile")
	public DatabaseStatistics databaseStatisticsFile(DataSource dataSource) {
		return loadFile(profilerParams.getConfigDir() + "/" + TakDBProfilerConstants.DATABASE_STATISTICS_FILE_NAME, DatabaseStatistics.class);
	}
	
	@Bean
	@Qualifier("groupsTableFile")
	public GroupsTable groupsTableFile(DataSource dataSource) {
		return loadFile(profilerParams.getConfigDir() + "/" + TakDBProfilerConstants.GROUPS_FILE_NAME, GroupsTable.class);
	}
	
	@Bean
	@Qualifier("cotRouterTableFile")
	public CotRouterTable cotRouterTableFile(DataSource dataSource) {
		return loadFile(profilerParams.getConfigDir() + "/" + TakDBProfilerConstants.COT_ROUTER_FILE_NAME, CotRouterTable.class);
	}
	
	@Bean
	@Qualifier("resourceTableFile")
	public ResourceTable resourceTableFile(DataSource dataSource) {
		return loadFile(profilerParams.getConfigDir() + "/" + TakDBProfilerConstants.RESOURCES_FILE_NAME, ResourceTable.class);
	}
	
	@Bean
	@Qualifier("missionTableFile")
	public MissionTable missionTableFile(DataSource dataSource) {
		return loadFile(profilerParams.getConfigDir() + "/" + TakDBProfilerConstants.MISSIONS_FILE_NAME, MissionTable.class);
	}
	
	@Bean
	@Qualifier("missionResourceTableFile")
	public MissionResourceTable missionResourceTableFile(DataSource dataSource) {
		return loadFile(profilerParams.getConfigDir() + "/" + TakDBProfilerConstants.MISSION_RESOURCES_FILE_NAME, MissionResourceTable.class);
	}
	
	@Bean
	@Qualifier("missionUidTableFile")
	public MissionUidTable missionUidTableFile(DataSource dataSource) {
		return loadFile(profilerParams.getConfigDir() + "/" + TakDBProfilerConstants.MISSION_UIDS_FILE_NAME, MissionUidTable.class);
	}

	@Bean
	public HikariDataSource dataSource() {

		String jdbcUrl = "jdbc:postgresql://" + profilerParams.getHost() + ":" + profilerParams.getPort() + "/"
				+ profilerParams.getDatabase();
				
		Properties props = new Properties();
		props.setProperty("user", profilerParams.getUsername());
		props.setProperty("password", profilerParams.getPassword());

		HikariConfig hikariConfig = new HikariConfig();

		hikariConfig.setUsername(profilerParams.getUsername());
		hikariConfig.setPassword(profilerParams.getPassword());
		hikariConfig.setJdbcUrl(jdbcUrl);
		hikariConfig.setMaxLifetime(600000);
		hikariConfig.setIdleTimeout(10000);
		hikariConfig.setMaximumPoolSize(50);
		hikariConfig.setConnectionTimeout(30000);
		hikariConfig.setAllowPoolSuspension(true);
		hikariConfig.setInitializationFailTimeout(-1);
		hikariConfig.setMinimumIdle(1);

		return new HikariDataSource(hikariConfig);
	}
	
	public static <T> T loadFile(String file, Class<T> clazz) {
		try {
			return new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(file), clazz);
		} catch (IOException e) {
			logger.error("Error loading " + clazz, e);
			return null;
		}
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
