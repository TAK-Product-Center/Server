package tak.server.federation.hub.plugin.manager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tak.server.federation.hub.plugin.FederationHubPluginMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginManagerConfig implements Serializable {
    private Map<String, FederationHubPluginMetadata> plugins = new HashMap<>();

    public Map<String, FederationHubPluginMetadata> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, FederationHubPluginMetadata> plugins) {
        this.plugins = plugins;
    }
    
    @JsonAnySetter
    public void assignNamesFromKeys() {
        plugins.forEach((key, plugin) -> plugin.setName(key));
    }
}
