

package com.bbn.tak.schema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "delete all records from the database", separators = "=")
public class PurgeCommand extends Command {
	public static final String name = "purge";

	PurgeCommand(SchemaManager manager) {
		super(manager, LoggerFactory.getLogger(PurgeCommand.class));
	}

	@Parameter(names = {"-s", "-safety"}, description = "prompt the user to confirm the operation if DB is not empty")
	private boolean safety = true;
	
	/**
	 * Clears the database schema.
	 * @return true if the command executed successfully
	 */
	@Override
	public boolean execute() {
		boolean reallyPurge = false;
		String databaseName = schemaManager.commonOptions.database;
		Long existingRows = schemaManager.estimateLegacyRows();
		if (existingRows == null) {
			logger.info("Didn't find any TAK server tables in database '" + databaseName + "'");
			existingRows = 0l;
		}
		if (safety && existingRows > 0) {
			logger.info("This will DESTROY all data in database '" + databaseName + "'!");
			logger.info("Please type 'erase' to confirm you want to do this: ");

			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String userInput;
			try {
				userInput = reader.readLine();
				if (userInput.compareToIgnoreCase("erase") == 0) {
					reallyPurge = true;
				} else {
					logger.info("Cancelled.\n");
				}
			} catch (IOException ex) {
				logger.error("Error reading user response. Cancelling.");
				if (logger.isDebugEnabled()) {
					ex.printStackTrace();
				}
			}
		} else {
			// No data in DB, or user is reckless -- go nuts!
			reallyPurge = true;
		}
		if (reallyPurge) {
			schemaManager.purge();
			logger.info("Database '" + databaseName + "' has been erased.");
		}

		return reallyPurge;
	}
}
