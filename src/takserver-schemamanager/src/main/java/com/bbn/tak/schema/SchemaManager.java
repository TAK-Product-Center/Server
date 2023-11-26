

package com.bbn.tak.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Configuration;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import tak.server.util.JavaVersionChecker;

/**
 * Command-line utility for managing TAK server database schema.
 * This tool lets a user backup a database, migrate to a new schema version, and more.
 *
 */
public class SchemaManager {

    public static final String DEFAULT_TAK_HOME = "/opt/tak";
    public static final String CORECONFIG_EXAMPLE = "CoreConfig.example.xml";

    public static int EXIT_OK = 0;
    public static int EXIT_BAD_INPUT = 1;
    public static int EXIT_RUNTIME_ERROR = 2;

    private static final Logger logger = LoggerFactory.getLogger(SchemaManager.class);

    Map<String, Command> commandMap = new HashMap<String, Command>();
    CommonOptions commonOptions = new CommonOptions();
    Flyway flyway;
    JCommander jcommander = null;

    private String dbNameOveride = null;
    private boolean isHelp = false;

    public static void main(String args[]) {
        JavaVersionChecker.check();
        disableAccessWarnings();
        int exitStatus = EXIT_OK;
        SchemaManager instance = null;
        try {
            instance = new SchemaManager();
            String parsedName = instance.processCommandLine(args);
            if (parsedName == null) {
                instance.jcommander.usage();
                exitStatus = EXIT_BAD_INPUT;
            } else {
                Command command = instance.commandMap.get(parsedName);
                logger.trace("executing " + parsedName);
                exitStatus = command.execute() ? EXIT_OK : EXIT_RUNTIME_ERROR;
            }
        } catch (ParameterException ex) {
            logger.error("Invalid command. " + ex.getMessage());
            exitStatus = EXIT_BAD_INPUT;
        } catch (RuntimeException ex) {
            StringBuilder builder = new StringBuilder();
            builder.append(ex.getMessage());
            Throwable cause = ex.getCause();
            if (cause != null) {
                builder.append(" ");
                builder.append(cause.getMessage());
            }
            logger.error(builder.toString());
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
            exitStatus = EXIT_RUNTIME_ERROR;
        } finally {
            try {
                if (instance != null && instance.getConnection() != null && !instance.getConnection().isClosed()) {
                    instance.getConnection().close();
                }
            } catch (IllegalStateException | SQLException ex) {
                logger.debug("Error closing connection: " + ex.getMessage());
            }
        }
        System.exit(exitStatus);
    }

    public SchemaManager() {
        jcommander = new JCommander(this.commonOptions);
        jcommander.setProgramName(SchemaManager.class.getSimpleName());

        commandMap.put(CloneCommand.name, new CloneCommand(this));
        commandMap.put(HelpCommand.name, new HelpCommand(this));
        commandMap.put(PurgeCommand.name, new PurgeCommand(this));
        commandMap.put(UpgradeCommand.name, new UpgradeCommand(this));
        commandMap.put(ValidateCommand.name, new ValidateCommand(this));
        commandMap.put(SetupPostresOnRDS.name, new SetupPostresOnRDS(this));
        commandMap.put(SetupPostresGeneric.name, new SetupPostresGeneric(this));

        for (String commandName : commandMap.keySet()) {
            jcommander.addCommand(commandName, commandMap.get(commandName));
            logger.trace("Added " + commandName + " to jcommander.");
        }
    }

    /**
     * Gets an <em>approximate</em> count of all rows in tables from schema version 12.
     * This is sufficient to determine if there is data from a schema that pre-dates this SchemaManager implementation.
     *
     * @return the approximate total count of all tables, or <code>null</code> if no tables count be read.
     */
    Long estimateLegacyRows() {
        Long totalRows = null;
        if (flyway.getConfiguration().getDataSource() == null) {
            throw new IllegalStateException("Data source is not initialized.");
        }

        // Tables in TAK server schema version 12
        String[] tables = {"cot_image", "cot_link", "cot_router", "cot_thumbnail", "mission", "mission_change",
                "mission_keyword", "mission_resource", "mission_uid", "resource", "subscriptions",
                "video_connections"};
        Statement statement = null;
        try (Connection connection = getConnection()) {
            // Unfortunately, you can't use a PreparedStatement when the variable is a table name.
            statement = connection.createStatement();
            for (String table : tables) {
                try {
                    String sql = "SELECT reltuples AS estimate FROM pg_class "
                            + " WHERE relname ='" + table + "' LIMIT " + (Integer.MAX_VALUE);
                    ResultSet rowCountResults = statement.executeQuery(sql);
                    logger.debug("Executed query '" + sql + "'");
                    if (rowCountResults.next()) {
                        int count = rowCountResults.getInt(1);
                        totalRows = (totalRows == null) ? count : totalRows + count;
                    } else {
                        logger.debug("Table '" + table + "' not found.");
                    }
                } catch (SQLException ex) {
                    logger.debug("Error estimating rows in '" + table + "': " + ex.getMessage());
                }
            }

        } catch (IllegalStateException | SQLException ex) {
            logger.error("Failed to connect to TAK server database.");
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ex) {
                    logger.debug(ex.getMessage());
                }
            }

        }
        return totalRows;
    }

    /**
     * Gets the database connection of the SchemaManager.
     *
     * @return a Connection to the database
     * @throws IllegalStateException if there is no valid connection, e.g. the database name or password was wrong.
     */
    Connection getConnection() throws IllegalStateException {
        Connection connection = null;
        try {
            connection = flyway.getConfiguration().getDataSource().getConnection();
        } catch (FlywayException | SQLException | NullPointerException ex) {
            String message = "Invalid connection to database '" + commonOptions.database + "'.";
            throw new IllegalStateException(message, ex);
        }
        return connection;
    }

    /**
     * Attempts to read the schema version from a TAK server database, schema version 7 through 12.
     * These databases were managed with the original SchemaManager implementation, which encoded the
     * schema version in the database as a function.
     *
     * @return the version of the schema if it could be read, or <code>null</code> otherwise.
     */
    String getLegacySchemaVersion() {
        String legacySchemaVersion = null;
        PreparedStatement martiSchemaQuery = null;
        ResultSet schemaVersionResult = null;
        try (Connection connection = getConnection()) {
            martiSchemaQuery = connection.prepareStatement("SELECT marti_schema_version();");
            schemaVersionResult = martiSchemaQuery.executeQuery();
            if (schemaVersionResult.next()) {
                legacySchemaVersion = Integer.toString(schemaVersionResult.getInt(1));
            }
        } catch (SQLException | NullPointerException ex) {
            logger.debug("Error executing query on database '" + commonOptions.database + "'. " + ex.getMessage());
        } finally {
            if (martiSchemaQuery != null) {
                try {
                    martiSchemaQuery.close();
                } catch (SQLException ex) {
                    logger.debug(ex.getMessage());
                }
            }
        }

        return legacySchemaVersion;
    }

    /**
     * Checks whether the current database schema has the Flyway metadata table.
     * This indicates the database is comprehensible to the current version of SchemaManager.
     *
     * @return <code>true</code> if the metadata table exists, even if it is empty.
     */
    boolean hasMetadata() {
        boolean tableFound = false;
        Statement countRows = null;
        try (Connection connection = getConnection()) {
            String tableName = flyway.getConfiguration().getTable();
            countRows = connection.createStatement();
            String sql = "SELECT COUNT(*) FROM " + tableName + ";";
            logger.trace("Executing '" + sql + "'");
            ResultSet rowCount = countRows.executeQuery(sql);
            tableFound = rowCount.next();
        } catch (IllegalStateException | SQLException ex) {
            logger.debug("Metadata table not found.");
        } finally {
            if (countRows != null) {
                try {
                    countRows.close();
                } catch (SQLException ex) {
                    logger.debug(ex.getMessage());
                }
            }
        }

        return tableFound;
    }

    /**
     * Parses command-line arguments and returns the command that was given
     *
     * @param args the arguments from the command line
     * @return the command that was found in the command line
     */
    private String processCommandLine(String[] args) {
        jcommander.parse(args);
        String command = jcommander.getParsedCommand();
        logger.trace("parsed command is " + command);
        if (command != null && command.compareToIgnoreCase(HelpCommand.name) != 0) {
            readCoreConfig();
            configure();
        }
        return command;
    }

    /**
     * Unconditionally and irrevocably destroys your database, including the PostGIS extensions.
     * This is needed because flyway.clean() doesn't remove extensions from a legacy database, and will
     * fail when there exists an extension that depends on a table it was trying to delete.
     */
    void purge() {
        // Delete PostGIS extensions, including unnecessary ones
        String[] dropExtensionCommands = {"DROP EXTENSION IF EXISTS fuzzystrmatch CASCADE;",
                "DROP EXTENSION IF EXISTS postgis_topology CASCADE;",
                "DROP EXTENSION IF EXISTS postgis CASCADE;"};
        String sql = null;
        Statement deleteStatement = null;
        String database = commonOptions.database;
        try (Connection connection = getConnection()) {
            deleteStatement = connection.createStatement();
            for (int index = 0; index < dropExtensionCommands.length; index++) {
                sql = dropExtensionCommands[index];
                logger.debug(sql);
                deleteStatement.execute(sql);
            }
            
            flyway.clean();
            logger.debug("Database '" + database + "' has been cleaned.");
            // Rebuild the metadata table
            flyway.repair();
            logger.debug("Metadata rebuilt for database '" + database + "'.");
        } catch (SQLException ex) {
            logger.error("Failure purging database '" + database + "'. " + ex.getMessage());
        } finally {
            if (deleteStatement != null) {
                try {
                    deleteStatement.close();
                } catch (SQLException ex) {
                    logger.debug(ex.getMessage());
                }
            }
        }
    }

    /**
     * Configures the SchemaManager's connection to a datasource.
     *
     * @return <code>true</code> if the connection was successful
     */
    private boolean configure() {
        HikariConfig config = new HikariConfig();
        HikariDataSource dataSource;

        logger.debug("Connecting datasource."
                + "\ndatabase =" + commonOptions.database
                + "\nusername =" + commonOptions.username
                + "\nurl = " + commonOptions.jdbcUrl);

        config.setJdbcUrl(commonOptions.jdbcUrl);
        config.setUsername(commonOptions.username);
        config.setPassword(commonOptions.password);
        dataSource = new HikariDataSource(config);

        flyway = Flyway.configure().cleanDisabled(false).dataSource(dataSource).table("schema_version").load();
        return true;
    }

    /**
     * The common options are null initially. The assumption is that command line arguments override
     * what is loaded from the CoreConfig.xml. If the values are not null, then command line arguments
     * were provided and should be preserved.
     */
    private void readCoreConfig() {

        Configuration configuration = null;
        com.bbn.marti.config.Connection connection = null;

        try {
            logger.info("trying to load configuration from file " + "CoreConfig.xml");
            configuration = loadJAXifiedXML(Configuration.class.getPackage().getName());
        } catch (JAXBException | FileNotFoundException ex) {
            logger.warn("Failure reading database configuration from file " + ex.getMessage() +
                    " trying command line options" + commonOptions.toString());
        }

        if (configuration != null) {
            connection = configuration.getRepository().getConnection();
            if (commonOptions.password == null) {
                commonOptions.setPassword(connection.getPassword());
            }
            if (commonOptions.username == null) {
                commonOptions.setUsername(connection.getUsername());
            }
            if (commonOptions.jdbcUrl == null) {
                commonOptions.setJdbcUrl(connection.getUrl());
            }
        }
        StringUtils.substringAfterLast(commonOptions.jdbcUrl, "/");
        commonOptions.setDatabase(StringUtils.substringAfterLast(commonOptions.jdbcUrl, "/"));

        if (logger.isDebugEnabled()) {
            logger.debug(" CommonOptions is " + commonOptions.toString());
        }
    }

    /**
     *  Parses an XML file to the specified packageName.  It assumes the packageName is valid and that is has been
     *  Parses an XML file to the specified packageName.  It assumes the packageName is valid and that is has been
     *  generated from the schema
     *
     *  @param packageName The name of the package that corresponds to the contents of the file
     *  @param <T>         The type corresponding to the packagename
     *  @return The object of type {@link T} if the file exists, null if it does not
     *  @throws JAXBException         If a parsing exception occurs
     *
     */
    @SuppressWarnings("unchecked")
	private <T> T loadJAXifiedXML(String packageName) throws JAXBException, FileNotFoundException {

        // first look for the CoreConfig in the standard takserver installation dir
        File f = new File(DEFAULT_TAK_HOME + File.separator + "CoreConfig.xml");
        logger.info("trying to load CoreConfig.xml in " + DEFAULT_TAK_HOME);

        if (!f.exists()) {
            // attempt to find it in the user dir where the jar was initiated
            String userDir = System.getProperty("user.dir");
            f = new File(userDir + File.separator + "CoreConfig.xml");
            logger.warn(" Trying current directory " + userDir);
        }

        // For initial install case, CoreConfig.xml won't exist yet, so use CoreConfig.example.xml instead.
        if (!f.exists()) {
            String coreConfigExamplePath = DEFAULT_TAK_HOME + File.separator + CORECONFIG_EXAMPLE;

            f = new File(coreConfigExamplePath);
            logger.warn(" Trying " + coreConfigExamplePath);
        }

        InputStream is = new FileInputStream(f);
        JAXBContext jc = JAXBContext.newInstance(packageName);
        Unmarshaller u = jc.createUnmarshaller();
        return (T) u.unmarshal(is);
    }

    @SuppressWarnings("unchecked")
    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }
}
