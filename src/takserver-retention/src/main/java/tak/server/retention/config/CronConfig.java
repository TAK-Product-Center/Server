package tak.server.retention.config;


import java.io.Serializable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties
@PropertySource(name="cron-sched", factory=YamlPropertySourceFactory.class, value="file:conf/retention/retention-service.yml")
public class CronConfig implements Serializable {

    private static final long serialVersionUID = 7617163979278779733L;

    private String cronExpression;

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

}
