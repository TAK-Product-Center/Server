package tak.server.ignite;

import java.util.HashSet;
import java.util.UUID;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;

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
	
	private IgniteHolder() {
		ignite = Ignition.getOrStart(IgniteConfigurationHolder.getInstance().getConfiguration());

		ignite.cluster().active(true);
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
		
		
		return messagingAddr.equals(apiAddr);
	}

}
