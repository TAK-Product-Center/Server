

package com.bbn.tak.schema;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "create a copy of the database under a new name")
public class CloneCommand extends Command {
	public static final String name = "clone";

	CloneCommand(SchemaManager manager) {
		super(manager, LoggerFactory.getLogger(CloneCommand.class));
	}

	@Parameter(names = {"-name" }, description="name of the database clone", required = true)
	String cloneName;

	/**
	 * Creates a clone of the database.
	 */
	@Override
	public boolean execute() {
		boolean cloneSucceeded = false;
		String database = schemaManager.commonOptions.database;
		try(Connection connection = schemaManager.getConnection()) {
			Statement cloneStatement = null;
			if (cloneName.compareToIgnoreCase(database) == 0) {
				logger.error("Clone name cannot be the same as the original database name (case insensitive).");
			} else {
				try {
					String sql = "CREATE DATABASE " + cloneName + " TEMPLATE " + database
							+ " OWNER " + schemaManager.commonOptions.username;
					cloneStatement = connection.createStatement();
					logger.debug(sql);
					cloneStatement.execute(sql);
					logger.info("SUCCESS. Created new database '" + cloneName + "', copied from '"
							+ database + "'.");
					cloneSucceeded = true;
				} catch (SQLException ex) {
					logger.error("Failed to clone database '" + database + "'. "
							+ ex.getMessage());
					if (logger.isDebugEnabled()) {
						ex.printStackTrace();
					}
				} finally {
					if (cloneStatement != null) {
						try {
							cloneStatement.close();
						} catch (SQLException ex) {
							logger.debug(ex.getMessage());
						}
					}
				}
			}
		}
		catch (SQLException ex){
			logger.error("Error trying to open SQL connection ", ex);
		}
		return cloneSucceeded;
	}
}
