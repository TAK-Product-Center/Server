package tak.server.federation.hub.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryUpdatedListener;

import org.apache.ignite.Ignite;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;

import tak.server.federation.hub.FederationHubConstants;

public class PluginRegistrySyncService {

	private Ignite ignite;

	private Map<String, FederationHubPluginMetadata> registeredPlugins = new HashMap<>();
	
	private AtomicBoolean isPluginManagerActive = new AtomicBoolean(false);

	public PluginRegistrySyncService(Ignite ignite) {
		this.ignite = ignite;
		
		registeredPlugins = FederationHubPluginRegistry.registrationCacheAsMap(ignite);
		
		registerNodeEventListener();
		registerCacheListenerQuery();
	}

	// we cannot afford to hit the cache on every message.
	// this query will track the plugin cache and keep an updated local copy
	// for quick lookup
	private void registerCacheListenerQuery() {
		ContinuousQuery<String, FederationHubPluginMetadata> registeredCacheQuery = new ContinuousQuery<>();

		registeredCacheQuery.setLocalListener((CacheEntryUpdatedListener<String, FederationHubPluginMetadata>) events -> {
			for (CacheEntryEvent<? extends String, ? extends FederationHubPluginMetadata> e : events) {
				switch (e.getEventType()) {
				case CREATED:
					registeredPlugins.put(e.getKey(), e.getValue());
					break;
				case UPDATED:
					registeredPlugins.put(e.getKey(), e.getValue());
					break;
				case REMOVED:
					registeredPlugins.remove(e.getKey());
					break;
				case EXPIRED:
					registeredPlugins.remove(e.getKey());
					break;
				}
			}
		});

		FederationHubPluginRegistry.registrationCache(ignite).query(registeredCacheQuery);
	}

	// we need to know if plugin manager is active. use this listener to keep track
	// of the plugin managers node status within the cluster
	private void registerNodeEventListener() {
		isPluginManagerActive.set(ignite.cluster().nodes().stream()
				.anyMatch(node -> FederationHubConstants.FEDERATION_HUB_PLUGIN_MANAGER_IGNITE_PROFILE
						.equals(node.attribute(FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY))));
		
		IgnitePredicate<DiscoveryEvent> ignitePredicate = new IgnitePredicate<DiscoveryEvent>() {
		    @Override
		    public boolean apply(DiscoveryEvent event) {
		    	System.out.println("event.type()1 " + event.type());
		    	
		        String profile = event.eventNode().attribute(FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY);
		        // we only need to track events from plugin manager node
				if (!FederationHubConstants.FEDERATION_HUB_PLUGIN_MANAGER_IGNITE_PROFILE.equals(profile)) {
					return true;
				}
		    	System.out.println("event.type()2 " + event.type());

		        switch (event.type()) {
		        	case EventType.EVT_NODE_JOINED:
		        		isPluginManagerActive.set(true);
		        		break;
		            case EventType.EVT_NODE_LEFT:
		            	isPluginManagerActive.set(false);
		            	registeredPlugins.clear();
		                break;
		            case EventType.EVT_NODE_FAILED:
		            	isPluginManagerActive.set(false);
		            	registeredPlugins.clear();
		                break;
		            default:
		                break;
		        }

		        return true;
		    }
		};
		ignite.events().localListen(ignitePredicate, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED, EventType.EVT_NODE_JOINED);
	}
	
	public boolean isPluginManagerActive() {
		return isPluginManagerActive.get();
	}
	
	public Collection<FederationHubPluginMetadata> getRegisteredPlugins() {
		return registeredPlugins.values();
	}
}
