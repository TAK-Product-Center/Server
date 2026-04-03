package tak.server.retention.config;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private Map<String, RetentionSettings> retentionSettings = new HashMap<>();

    public static class RetentionSettings implements Serializable {
        private List<String> exemptKeywords;

        public List<String> getExemptKeywords() {
            return exemptKeywords;
        }

        public void setExemptKeywords(List<String> exemptKeywords) {
            this.exemptKeywords = exemptKeywords;
        }

        @Override
        public String toString() {
            return "RetentionSettings{" +
                    "exemptKeywords=" + exemptKeywords +
                    '}';
        }
    }

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

    public Map<String, RetentionSettings> getRetentionSettings() {
        return retentionSettings;
    }

    public void setRetentionSettings(Map<String, RetentionSettings> retentionSettings) {
        this.retentionSettings = retentionSettings;
    }

    @Override
    public String toString() {
        return "RetentionPolicy{" +
                "dataRetentionMap=" + dataRetentionMap +
                ", retentionSettings=" + retentionSettings +
                '}';
    }
}
