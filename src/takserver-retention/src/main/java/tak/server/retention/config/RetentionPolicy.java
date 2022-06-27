package tak.server.retention.config;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.bbn.marti.remote.service.RetentionQueryService;

@Configuration
@ConfigurationProperties
@PropertySource(name="retention-policy", factory= YamlPropertySourceFactory.class, value="file:conf/retention/retention-policy.yml")
public class RetentionPolicy implements Serializable {

    private static final long serialVersionUID = -2842031725284479523L;

    Map<String, Integer> dataRetentionMap = new HashMap<>();

    public RetentionPolicy() {
    }

    public RetentionPolicy(Map<String, Integer> dataRetentionMap) {
        this.dataRetentionMap = dataRetentionMap;
    }

    public Map<String, Integer> getDataRetentionMap() {
        return dataRetentionMap;
    }

    public void setDataRetentionMap(Map<String, Integer> dataRetentionMap) {
        this.dataRetentionMap = dataRetentionMap;
    }

    @Override
    public String toString() {
        return "RetentionPolicy{" +
                "dataRetentionMap=" + dataRetentionMap +
                '}';
    }
}
