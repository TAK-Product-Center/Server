package tak.db.profiler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection {
	private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
	private Connection connection;
	
	public DBConnection(TakDBProfilerParams params) {
		String jdbcUrl = "jdbc:postgresql://" + params.getHost() + ":" + params.getPort() + "/" + params.getDatabase();
    	try {
			connection = DriverManager.getConnection(jdbcUrl, params.getUsername(), params.getPassword());
			logger.info("Connected to database: " + params);
		} catch (SQLException e) {
			logger.error("Error connecting to Postgres on " + jdbcUrl + " with username: " + params.getUsername()
					+ " and password " + params.getPassword(), e);
			System.exit(1);
		}
	}
	
	public Connection getConnection() throws IllegalStateException {
        return connection;
    }
}
