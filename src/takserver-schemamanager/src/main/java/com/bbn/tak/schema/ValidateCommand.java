package com.bbn.tak.schema;

import com.beust.jcommander.Parameter;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.internal.info.MigrationInfoDumper;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "report on the current schema version and its update history")
public class ValidateCommand extends Command {
	public static final String name = "validate";

	ValidateCommand(SchemaManager manager) {
		super(manager, LoggerFactory.getLogger(ValidateCommand.class));
	}

	@Parameter(names = {"-url" }, description="the database url, example: jdbc:postgresql://127.0.0.1:5432/cot")
	String jdbcUrl;

	@Override
	public boolean execute() throws IllegalStateException {
		String url = schemaManager.commonOptions.jdbcUrl;
		if (url == null) {
			throw new IllegalStateException("Not connected to data source.");
		} else {
			try {
				MigrationInfoService infoService = schemaManager.flyway.info();
				MigrationInfo[] allMigrations = infoService.all();
				String asciiTable = MigrationInfoDumper.dumpToAsciiTable(allMigrations);
				MigrationInfo[] appliedMigrations = infoService.applied();
				if (appliedMigrations.length == 0 ) {
					logger.info("No migration history available.");
					Long existingRows = schemaManager.estimateLegacyRows();
					if (existingRows == null ) {
						logger.info("No data found in legacy tables.");
					} else if (existingRows == 0 ) {
						logger.info("Legacy tables found, and they are empty.");
					} else {
						logger.info("Found approximately " + existingRows
								+ " total rows of data in legacy tables.");
					}
					String legacyVersion = schemaManager.getLegacySchemaVersion();
					if (legacyVersion == null) {
						logger.info("No legacy version information.");
						if (existingRows == null) {
							logger.info("*** Database '" + schemaManager.commonOptions.database
									+ "' does not appear to be initialized. ***");
						}
					} else {
						logger.info("Detected legacy schema version " + legacyVersion + ".");
					}
				} 
				logger.info(asciiTable);
			} catch (FlywayException ex) {
				logger.error("Failed to connect to database '" + schemaManager.commonOptions.database + "'. "
						+ex.getMessage());	
			}
		}
		return true;
	}

}
