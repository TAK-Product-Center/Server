package tak.server.ignite.cache;

import java.math.BigInteger;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.annotations.QuerySqlFunction;
import org.apache.ignite.configuration.CacheConfiguration;

import com.bbn.marti.remote.RemoteSubscription;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;

public final class IgniteCacheHolder {
	
	private static IgniteCache<String, RemoteSubscription>  igniteSubscriptionUidTrackerCache = null;
	private static IgniteCache<String, RemoteSubscription>  igniteSubscriptionClientUidTrackerCache = null;

	private static IgniteCache<String, String>  iginteUserOutboundGroupCache = null;
	private static IgniteCache<String, String>  iginteUserInboundGroupCache = null;
	
	public static void cacheRemoteSubscription(RemoteSubscription sub) {
		sub.prepareForSerialization();
		
		getIgniteSubscriptionUidTackerCache().put(sub.uid, sub);
		getIgniteSubscriptionClientUidTackerCache().put(sub.clientUid, sub);
	}
	
	public static void removeCachedRemoteSubscription(RemoteSubscription sub) {
		getIgniteSubscriptionUidTackerCache().remove(sub.uid);
		getIgniteSubscriptionClientUidTackerCache().remove(sub.clientUid);
	}
	
	public static IgniteCache<String, RemoteSubscription> getIgniteSubscriptionUidTackerCache() {
    	if (igniteSubscriptionUidTrackerCache == null) {
    		initGroupCaches();
    		CacheConfiguration<String, RemoteSubscription> cacheCfg = new CacheConfiguration<>(Constants.IGNITE_SUBSCRIPTION_UID_TRACKER_CACHE);
    		cacheCfg.setIndexedTypes(String.class, RemoteSubscription.class);
//    		cacheCfg.setSqlFunctionClasses(SqlHelper.class);
    		igniteSubscriptionUidTrackerCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
		}
		
		return igniteSubscriptionUidTrackerCache;
	}
	
	public static IgniteCache<String, RemoteSubscription> getIgniteSubscriptionClientUidTackerCache() {
    	if (igniteSubscriptionClientUidTrackerCache == null) {
    		initGroupCaches();
    		CacheConfiguration<String, RemoteSubscription> cacheCfg = new CacheConfiguration<>(Constants.IGNITE_SUBSCRIPTION_CLIENTUID_TRACKER_CACHE);
    		cacheCfg.setIndexedTypes(String.class, RemoteSubscription.class);
//    		cacheCfg.setSqlFunctionClasses(SqlHelper.class);
    		igniteSubscriptionClientUidTrackerCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
		}
		
		return igniteSubscriptionClientUidTrackerCache;
	}
	
	public static IgniteCache<String, String> getIgniteUserOutboundGroupCache() {
    	if (iginteUserOutboundGroupCache == null) {
    		CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(Constants.IGNITE_USER_OUTBOUND_GROUP_CACHE);
    		cacheCfg.setIndexedTypes(String.class, String.class);
    		iginteUserOutboundGroupCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
		}
		
		return iginteUserOutboundGroupCache;
	}
	
	public static IgniteCache<String, String> getIgniteUserInboundGroupCache() {
    	if (iginteUserInboundGroupCache == null) {
    		CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(Constants.IGNITE_USER_INBOUND_GROUP_CACHE);
    		cacheCfg.setIndexedTypes(String.class, String.class);
    		iginteUserInboundGroupCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
		}
		
		return iginteUserInboundGroupCache;
	}

	public static class SqlHelper {
	    @QuerySqlFunction
	    public static int groupVectorAnd(String fromCache, String toMatch) {
	    	// one of the vectors was empty, return 0 for no match
	    	if (fromCache.isEmpty() || toMatch.isEmpty()) {
	    		return 0;
	    	}
	    	
	        return new BigInteger(fromCache,2).and(new BigInteger(toMatch,2)).compareTo(new BigInteger("0"));
	    }
	}
	
	private static void initGroupCaches() {
		getIgniteUserInboundGroupCache();
		getIgniteUserOutboundGroupCache();
	}
}
