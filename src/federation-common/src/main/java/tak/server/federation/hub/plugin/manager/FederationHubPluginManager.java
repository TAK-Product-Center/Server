package tak.server.federation.hub.plugin.manager;

import tak.server.federation.FederationException;
import tak.server.federation.hub.plugin.FederationHubPluginMetadata;

public interface FederationHubPluginManager {
	FederationException registerPlugin(FederationHubPluginMetadata metadata);
}
