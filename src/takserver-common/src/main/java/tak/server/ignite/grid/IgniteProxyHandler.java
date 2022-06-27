package tak.server.ignite.grid;

import org.apache.ignite.IgniteCache;

import com.bbn.cluster.NoOpIgniteCache;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.groups.User;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;
import tak.server.ignite.cache.IgniteCacheHolder;

public class IgniteProxyHandler {
	
	protected CoreConfig coreConfig;
	
	IgniteProxyHandler(CoreConfig coreConfig) {
		this.coreConfig = coreConfig;
	}

    protected IgniteCache<String, RemoteSubscription> getIgniteSubscriptionUidTackerCache() {
    	return IgniteCacheHolder.getIgniteSubscriptionUidTackerCache();
	}
    
    protected IgniteCache<String, RemoteSubscription> getIgniteSubscriptionClientUidTackerCache() {
    	return IgniteCacheHolder.getIgniteSubscriptionClientUidTackerCache();
	}
}
