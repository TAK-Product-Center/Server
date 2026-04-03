package tak.server.federation.hub.ui.graph;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Holds settings from the UI. Arbitrary fields (like zoom, canvas_x, canvas_y) 
 * will be stored in the settings map.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FederationUISettingsPolicyModel {

    // Catch-all for any fields inside "settings"
    private Map<String, Object> settings = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getSettings() {
        return settings;
    }

    @JsonAnySetter
    public void setSetting(String key, Object value) {
        this.settings.put(key, value);
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return "FederationUISettingsPolicyModel{" +
                    "settings=" + mapper.writeValueAsString(settings) +
                    '}';
        } catch (JsonProcessingException e) {
            return "FederationUISettingsPolicyModel{settings=" + settings + "}";
        }
    }
}
