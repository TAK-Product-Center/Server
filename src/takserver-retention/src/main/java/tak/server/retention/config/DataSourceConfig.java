package tak.server.retention.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ignite.IgniteException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bbn.marti.config.Connection;
import com.bbn.marti.config.Repository;
import com.bbn.marti.remote.CoreConfig;

import static java.util.Objects.requireNonNull;

@Configuration
public class DataSourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Autowired
    CoreConfig coreConfig;

    @Bean
    public HikariDataSource getHikariDataSource() {
        HikariDataSource hikariDataSource = null;
        int retry = 0;
        while (hikariDataSource == null && retry < 5) {
            try {
                hikariDataSource = loadHikariDataSource();
                if (retry > 0) {
                    logger.warn("Retrying in 20 seconds, retry count " + retry);
                }
                Thread.sleep(20000);
            } catch (InterruptedException ie) {
                logger.error("Retry interrupted: " + ie.getMessage());
            }
            retry++;
        }
        return hikariDataSource;
    }

    private HikariDataSource loadHikariDataSource() {
        HikariDataSource hikariDataSource = null;
        try {
            Repository repository = coreConfig.getRemoteConfiguration().getRepository();
            Connection coreDbConnection = coreConfig.getRemoteConfiguration().getRepository().getConnection();
            requireNonNull(requireNonNull(coreDbConnection, "CoreConfig db connection").getUsername(), "CoreConfig db username");
            requireNonNull(coreDbConnection.getPassword(), "CoreConfig db password");
            requireNonNull(coreDbConnection.getUrl(), "CoreConfig db url");

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setUsername(coreDbConnection.getUsername());
            hikariConfig.setPassword(coreDbConnection.getPassword());
            hikariConfig.setJdbcUrl(coreDbConnection.getUrl());
            hikariConfig.setMaxLifetime(repository.getDbConnectionMaxLifetimeMs());
            hikariConfig.setMaximumPoolSize(2);
            hikariDataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            logger.warn(" unable to access the core config");
        }
        return hikariDataSource;
    }
}
