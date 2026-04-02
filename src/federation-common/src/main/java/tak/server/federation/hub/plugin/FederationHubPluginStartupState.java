package tak.server.federation.hub.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public enum FederationHubPluginStartupState {
	Active, Disabled
}
