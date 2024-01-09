package tak.server.ignite.cache;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.util.RemoteUtil;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;

public final class IgniteCacheHolder {
	
	private static IgniteCache<String, RemoteSubscription>  igniteSubscriptionUidTrackerCache = null;
	private static IgniteCache<String, RemoteSubscription>  igniteSubscriptionClientUidTrackerCache = null;

	private static IgniteCache<String, String>  igniteLatestSAConnectionUidCache = null;
	
	private static IgniteCache<String, String>  iginteUserOutboundGroupCache = null;
	private static IgniteCache<String, String>  iginteUserInboundGroupCache = null;
	private static final Logger logger = LoggerFactory.getLogger(IgniteCacheHolder.class);
	
	 public static final int GROUPS_BIT_VECTOR_LEN = 32768;
	 
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
	
	public static IgniteCache<String, String> getIgniteLatestSAConnectionUidCache() {
    	if (igniteLatestSAConnectionUidCache == null) {
    		initGroupCaches();
    		CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(Constants.INGITE_LATEST_SA_CONNECTION_UID_CACHE);
    		cacheCfg.setIndexedTypes(String.class, String.class);
    		igniteLatestSAConnectionUidCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
		}
		
		return igniteLatestSAConnectionUidCache;
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
	
	public static Collection<String> getAllLatestSAsForGroupVector(String groupVector) {	
		Collection<String> sas = new ArrayList<>();

		SqlFieldsQuery qry = new SqlFieldsQuery("Select lsa._VAL from \"" + Constants.INGITE_LATEST_SA_CONNECTION_UID_CACHE + "\".String lsa, \"" + Constants.IGNITE_USER_OUTBOUND_GROUP_CACHE +  "\".String og where lsa._KEY = og._KEY "
		+" and bitand(cast(lpad(?," + GROUPS_BIT_VECTOR_LEN + ",'0') as binary), cast(lpad(og._VAL, " + GROUPS_BIT_VECTOR_LEN + ", '0') as binary)) <> cast(lpad('0000', " + GROUPS_BIT_VECTOR_LEN + " ,'0') as long)");
		qry.setArgs(groupVector);	
		SqlFieldsQuery saQry = qry;
		try (QueryCursor<List<?>> cursor = getIgniteLatestSAConnectionUidCache().query(saQry)) {
			for (List<?> row : cursor) {
				for (Object cotColumn : row) {
					sas.add(cotColumn.toString());
				}
			}
		}
		
		return sas;
	}
	
	private static void initGroupCaches() {
		getIgniteUserInboundGroupCache();
		getIgniteUserOutboundGroupCache();
	}
}
