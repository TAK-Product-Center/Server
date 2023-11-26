

package com.bbn.tak.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "upgrades the database schema to the latest version")
public class UpgradeCommand extends Command {
	public static final String name = "upgrade";

	UpgradeCommand(SchemaManager manager) {
		super(manager, LoggerFactory.getLogger(UpgradeCommand.class));
	}
	
	/**
	 * Migrates the database to the latest schema version.
	 * If the database is non-empty and has an old, unsupported, or corrupt schema, the upgrade will not occur.
	 */
	@Override
	public boolean execute() {
		boolean migrationIsValid = true;

		// Check whether the DB is empty
		Long numberOfRows = schemaManager.estimateLegacyRows();
		if (numberOfRows != null && numberOfRows > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append("Database is not empty.");
			if (!schemaManager.hasMetadata()) {
				builder.append(" It has a legacy schema and contains approximately ");
				builder.append(numberOfRows);
				builder.append(" rows." );
			}
			logger.info(builder.toString());
			if (!schemaManager.hasMetadata()) {
				String schemaVersion = schemaManager.getLegacySchemaVersion();
				if (schemaVersion != null) {
					logger.info("Detected legacy schema version " + schemaVersion + ".");
					if (schemaManager.flyway.getConfiguration() instanceof ClassicConfiguration) {
					    ((ClassicConfiguration)schemaManager.flyway.getConfiguration()).setBaselineVersion(MigrationVersion.fromVersion(schemaVersion.toString()));
					} else if (schemaManager.flyway.getConfiguration() instanceof FluentConfiguration) {
					    ((FluentConfiguration)schemaManager.flyway.getConfiguration()).baselineVersion(MigrationVersion.fromVersion(schemaVersion.toString()));
					}
					schemaManager.flyway.baseline();
				} else {
					logger.error("Database is not empty and schema version could not be detected. " 
							+ "Cowardly refusing to upgrade.");
					migrationIsValid = false;
				}
			}
		} else if (!schemaManager.hasMetadata() ) {
			// Database is empty but there's no Flyway metadata. Blow it away to avoid future problems with random
			// cruft.
			logger.debug("No TAK server data found. Rebuilding database from scratch." );
			schemaManager.purge();
			if (schemaManager.flyway.getConfiguration() instanceof ClassicConfiguration) {
			    ((ClassicConfiguration)schemaManager.flyway.getConfiguration()).setBaselineOnMigrate(true);
			} else if (schemaManager.flyway.getConfiguration() instanceof FluentConfiguration) {
			    ((FluentConfiguration)schemaManager.flyway.getConfiguration()).baselineOnMigrate(true);
			}
		}
		
		if (migrationIsValid) {
			int numberOfUpgrades = 0;
			try {
				numberOfUpgrades = schemaManager.flyway.migrate().migrationsExecuted;
				if (numberOfUpgrades > 0) {
					logger.info("Successfully applied " + numberOfUpgrades + " update(s).");
				}
				logger.info("TAK server database schema is up to date.");
			} catch (FlywayException e) {
				
				try {
					schemaManager.flyway.repair();
					logger.info("Checksums updated. Continuing with schema update.");
				} catch (FlywayException ee) {
					throw new FlywayException("checksum  repair failed", ee);
				}
				
				numberOfUpgrades = schemaManager.flyway.migrate().migrationsExecuted;
				if (numberOfUpgrades > 0) {
					logger.info("Successfully applied " + numberOfUpgrades + " update(s).");
				}
				logger.info("TAK server database schema is up to date.");
			}
		}
	
		return migrationIsValid;
	}
}
