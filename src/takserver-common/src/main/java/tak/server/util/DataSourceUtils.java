package tak.server.util;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Repository;
import com.bbn.marti.config.Connection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;


public class DataSourceUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(DataSourceUtils.class);

	public static HikariDataSource setupDataSourceFromCoreConfig() {
		
	    Repository repository = CoreConfigFacade.getInstance().getRemoteConfiguration().getRepository();
	    Connection coreDbConnection = repository.getConnection();

        int max_connections = 0;
	    if (repository.isEnable()) {
	    	
	        Properties props = new Properties();
	        props.setProperty("user", coreDbConnection.getUsername());
	        if (!coreDbConnection.isSslEnabled()) {
	        	props.setProperty("password", coreDbConnection.getPassword());	        	
	        }else {
	        	props.setProperty("sslmode", coreDbConnection.getSslMode());
	        	props.setProperty("sslcert", coreDbConnection.getSslCert());
	        	props.setProperty("sslkey", coreDbConnection.getSslKey());
	        	props.setProperty("sslrootcert", coreDbConnection.getSslRootCert());
	        }
	        
	        try(java.sql.Connection conn = DriverManager.getConnection(coreDbConnection.getUrl(), props); ResultSet res = conn.createStatement().executeQuery("show max_connections")) {
	            
	            res.next();
	            max_connections = res.getInt(1);
	        } catch (Exception ee) {
	            logger.error("error connecting to database", ee);
	        }
	    }

        // this will set a min of 200 connections for 4 cpus (c5.xlarge) to 1045 for 96
        // cpus (c5.24xlarge) (max is 1045 regardless)
        int numDbConnections;
        if (repository.isConnectionPoolAutoSize()) {
            numDbConnections = repository.getPoolScaleFactor() + (int) Math.min(845, (Runtime.getRuntime().availableProcessors() - 4) * 9.2);
            numDbConnections = Math.min(Math.max(1, (max_connections - 2) / 2), numDbConnections);
        } else {
            numDbConnections = repository.getNumDbConnections();
        }
        
        logger.info(Runtime.getRuntime().availableProcessors() + " CPU cores detected. Postgres server maximum allowed connections value is " + max_connections
                    + ". The computed connection pool size is: " + numDbConnections);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(coreDbConnection.getUsername());
        hikariConfig.setPassword(coreDbConnection.getPassword());
        hikariConfig.setJdbcUrl(coreDbConnection.getUrl());
        hikariConfig.setMaxLifetime(repository.getDbConnectionMaxLifetimeMs());
        hikariConfig.setIdleTimeout(repository.getDbConnectionMaxIdleMs());
        hikariConfig.setMaximumPoolSize(numDbConnections);
        hikariConfig.setConnectionTimeout(repository.getDbTimeoutMs());
        hikariConfig.setAllowPoolSuspension(true);
        hikariConfig.setInitializationFailTimeout(-1);
        hikariConfig.setMinimumIdle(1);
        if (coreDbConnection.isSslEnabled()) {
    	    logger.info("SSL connection to database is enabled, client user: {}", coreDbConnection.getUsername());
        	hikariConfig.addDataSourceProperty("user", coreDbConnection.getUsername());
        	hikariConfig.addDataSourceProperty("sslmode", coreDbConnection.getSslMode());
        	hikariConfig.addDataSourceProperty("sslcert", coreDbConnection.getSslCert());
        	hikariConfig.addDataSourceProperty("sslkey", coreDbConnection.getSslKey());
        	hikariConfig.addDataSourceProperty("sslrootcert", coreDbConnection.getSslRootCert());
        }

        if (!repository.isEnable()) {
                // Zero would be ideal.... But an exception is thrown if less than 250 is chosen
        	hikariConfig.setConnectionTimeout(250);
        }

        return new HikariDataSource(hikariConfig);
        
	}
	
}
