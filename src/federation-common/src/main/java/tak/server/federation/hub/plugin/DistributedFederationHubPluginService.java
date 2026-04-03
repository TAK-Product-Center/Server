package tak.server.federation.hub.plugin;

import org.apache.ignite.Ignite;

public class DistributedFederationHubPluginService implements FederationHubPluginService,  org.apache.ignite.services.Service {
	
	public DistributedFederationHubPluginService(Ignite ignite) {
		
	}
	
	@Override
	public void stopIgnite() {
		
	}
}
