package tak.server.ignite;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;

import com.bbn.marti.remote.RemoteSubscription;

import tak.server.Constants;

/*
 * 
 * 
 */
public class IgniteHolder {
	
	private static IgniteHolder instance;
	private Ignite ignite;
	private UUID igniteId = null;
	private String igniteStringId = null;
	private AtomicBoolean isConnected = new AtomicBoolean();
	
	private IgniteHolder() {
		ignite = Ignition.getOrStart(IgniteConfigurationHolder.getInstance().getIgniteConfiguration());
		ignite.cluster().active(true);
		
		// set that ignite is connected after we re-init the services
		IgniteReconnectEventHandler.registerPostAction(() -> {
			isConnected.set(true);
		});
		
		// listen for reconnect events. this is messy but can be simplified with
		// IgniteClientDisconnectedEvent once we upgrade ignite to 2.8+ 
		ignite.events().localListen((Event event) -> {
		    if (event.type() == EventType.EVT_CLIENT_NODE_RECONNECTED) {		       
		    	IgniteReconnectEventHandler.reconnect();
		    }

		    return true;
		}, EventType.EVT_CLIENT_NODE_RECONNECTED);
	}
	
	CacheConfiguration<String, RemoteSubscription> cacheCfg = new CacheConfiguration<>("tak-ignite-heartbeat");
	public boolean isConnected() {
		// if false, no need to check - IgniteReconnectEventHandler will set it true when it's ready
		if (!isConnected.get()) return false;
		
		try {		
			// will err if servers are down
			ignite.getOrCreateCache(cacheCfg).get("heartbeat");
			isConnected.set(true);
		} catch (Exception e) {
			e.printStackTrace();
			isConnected.set(false);
		}
		
		return isConnected.get();
	}
	
	public static IgniteHolder getInstance() {
		if (instance == null) {
			synchronized(IgniteHolder.class) {
				if (instance == null) {
					instance = new IgniteHolder();
				}
			}
		}
		
		return instance;
	}
	
	public Ignite getIgnite() {
		return ignite;
	}
	
	public UUID getIgniteId() {
		if (igniteId == null) {
			igniteId = ignite.cluster().localNode().id();
		}
		
		return igniteId;
	}
	
	public String getIgniteStringId() {
		if (igniteStringId == null) {
			igniteStringId = ignite.cluster().localNode().id().toString();
		}
		
		return igniteStringId;
	}
	
	// if TAK Server processes are operating on the same host
	public boolean areTakserverIgnitesLocal() {
		HashSet<String> messagingAddr = new HashSet<String>();
		IgniteHolder.getInstance().getIgnite().cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME).nodes().forEach(messaging -> {
			messagingAddr.addAll(messaging.addresses());
		});
		
		HashSet<String> apiAddr = new HashSet<String>();
		IgniteHolder.getInstance().getIgnite().cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.API_PROFILE_NAME).nodes().forEach(api -> {				
			apiAddr.addAll(api.addresses());
		});
		
		return messagingAddr.equals(apiAddr) || apiAddr.isEmpty();
	}

}
