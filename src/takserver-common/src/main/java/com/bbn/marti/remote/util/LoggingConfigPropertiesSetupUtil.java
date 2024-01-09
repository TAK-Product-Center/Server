package com.bbn.marti.remote.util;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.config.CoreConfigFacade;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.Constants;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/*
 *  Utility class that sets System Properties based on Logging Configurations from CoreConfig
 */
public class LoggingConfigPropertiesSetupUtil {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfigPropertiesSetupUtil.class);
    private final CoreConfig coreConfig = CoreConfigFacade.getInstance();

    private static LoggingConfigPropertiesSetupUtil instance = new LoggingConfigPropertiesSetupUtil();

    public static LoggingConfigPropertiesSetupUtil getInstance() {
        return instance;
    }

    public LoggingConfigPropertiesSetupUtil() {}

    public void setupLoggingConfiguration() {
        Configuration config = coreConfig.getRemoteConfiguration();
        setupLoggingProperties(config);
    }

    public void setupLoggingProperties(Configuration config) {
        if (config.getLogging() != null) {
            System.setProperty("logging.json.enabled", String.valueOf(config.getLogging().isJsonFormatEnabled()));
            System.setProperty("logging.audit.enabled", String.valueOf(config.getLogging().isAuditLoggingEnabled()));
            System.setProperty("logging.pretty.enabled", String.valueOf(config.getLogging().isPrettyLoggingEnabled()));
        }
    }

}
