package tak.server.ignite.cache;

import com.bbn.marti.config.Cluster;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.Constants;
import tak.server.ignite.IgniteHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public final class IgniteCacheHolder {

	public static final int GROUPS_BIT_VECTOR_LEN = 32768;

	private static final Logger logger = LoggerFactory.getLogger(IgniteCacheHolder.class);

	private static ScheduledExecutorService svc;

	/**
	 * Buffers subscriptions if in cluster mode, sends them straight through if not.
	 */
	private static class RemoteSubscriptionSubmissionBuffer {

		private static final ConcurrentLinkedQueue<RemoteSubscriptionContainer> pendingSubscriptionCache = new ConcurrentLinkedQueue<>();

		private static class RemoteSubscriptionContainer {
			public final RemoteSubscription subscriptionToAdd;
			public final String uidToRemove;
			public final String clientUidToRemove;

			public RemoteSubscriptionContainer(RemoteSubscription subscriptionToAdd, String uidToRemove, String clientUidToRemove) {
				this.subscriptionToAdd = subscriptionToAdd;
				this.uidToRemove = uidToRemove;
				this.clientUidToRemove = clientUidToRemove;
			}
		}

		private static synchronized void initScheduledExecution() {
			if (svc == null) {
				Cluster config = CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster();
				ThreadFactory threadFactory =
					new ThreadFactoryBuilder()
						.setNameFormat("subscriptionCacheUpdater" + "-%1$d")
						.setUncaughtExceptionHandler((thread, t) -> logger.error("Uncaught exception", t))
						.build();
				svc = new ScheduledThreadPoolExecutor(1, threadFactory);
				svc.scheduleWithFixedDelay(RemoteSubscriptionSubmissionBuffer::drainBufferIntoCache,
					config.getMetricsIntervalDelaySeconds(), config.getMetricsIntervalSeconds(), TimeUnit.SECONDS);
			}
		}


		public static void addToCache(RemoteSubscription subscription) {
			if (CoreConfigFacade.getInstance().isCluster()) {
				initScheduledExecution();
				pendingSubscriptionCache.add(new RemoteSubscriptionContainer(subscription, null, null));
			} else {
				getIgniteSubscriptionUidTrackerCache().put(subscription.uid, subscription);
				getIgniteSubscriptionClientUidTrackerCache().put(subscription.clientUid, subscription);
			}
		}

		public static void removeFromCache(@Nullable String uidToRemove, @Nullable String clientUidToRemove) {
			if (CoreConfigFacade.getInstance().isCluster()) {
				initScheduledExecution();
				pendingSubscriptionCache.add(new RemoteSubscriptionContainer(null, uidToRemove, clientUidToRemove));
			} else {
				if (uidToRemove != null) {
					getIgniteSubscriptionUidTrackerCache().remove(uidToRemove);
				}
				if (clientUidToRemove != null) {
					getIgniteSubscriptionClientUidTrackerCache().remove(clientUidToRemove);
				}
			}
		}

		public static void drainBufferIntoCache() {
			RemoteSubscriptionContainer subscriptionContainer = pendingSubscriptionCache.poll();
			while (subscriptionContainer != null) {
				if (subscriptionContainer.subscriptionToAdd != null) {
					RemoteSubscription sub = subscriptionContainer.subscriptionToAdd;
					getIgniteSubscriptionUidTrackerCache().put(sub.uid, sub);
					getIgniteSubscriptionClientUidTrackerCache().put(sub.clientUid, sub);
				}
				if (subscriptionContainer.uidToRemove != null) {
					getIgniteSubscriptionUidTrackerCache().remove(subscriptionContainer.uidToRemove);
				}
				if (subscriptionContainer.clientUidToRemove != null) {
					getIgniteSubscriptionClientUidTrackerCache().remove(subscriptionContainer.clientUidToRemove);
				}
				subscriptionContainer = pendingSubscriptionCache.poll();
			}
		}
	}

	public static void cacheRemoteSubscription(RemoteSubscription sub) {
		sub.prepareForSerialization();
		RemoteSubscriptionSubmissionBuffer.addToCache(sub);
	}

	public static void removeCachedRemoteSubscription(RemoteSubscription sub) {
		RemoteSubscriptionSubmissionBuffer.removeFromCache(sub.uid, sub.clientUid);
	}

	private static IgniteCache<String, RemoteSubscription> getIgniteSubscriptionUidTrackerCache() {
		initGroupCaches();
		CacheConfiguration<String, RemoteSubscription> cacheCfg = new CacheConfiguration<>(Constants.IGNITE_SUBSCRIPTION_UID_TRACKER_CACHE);
		cacheCfg.setIndexedTypes(String.class, RemoteSubscription.class);
		return IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
	}

	public static FieldsQueryCursor<List<?>> queryIgniteSubscriptionUidTrackerCache(SqlFieldsQuery qry) {
		return getIgniteSubscriptionUidTrackerCache().query(qry);
	}

	private static IgniteCache<String, RemoteSubscription> getIgniteSubscriptionClientUidTrackerCache() {
		initGroupCaches();
		CacheConfiguration<String, RemoteSubscription> cacheCfg = new CacheConfiguration<>(Constants.IGNITE_SUBSCRIPTION_CLIENTUID_TRACKER_CACHE);
		cacheCfg.setIndexedTypes(String.class, RemoteSubscription.class);
		return IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
	}

	public static RemoteSubscription getSubscriptionFromIgniteSubscriptionClientUidTrackerCache(String uid) {
		return getIgniteSubscriptionClientUidTrackerCache().get(uid);
	}

	public static void removeFromIgniteSubscriptionClientUidTrackerCache(String str) {
		RemoteSubscriptionSubmissionBuffer.removeFromCache(null, str);
	}

	public static IgniteCache<String, String> getIgniteLatestSAConnectionUidCache() {
		initGroupCaches();

		// for some reason caches with setSqlFunctionClasses cannot be fetched using getOrCreateCache because if it already exists
		// ignite thinks you are trying to make a dynamic cache configuration change. to avoid this add an extra check
		IgniteCache<String, String> cache = IgniteHolder.getInstance().getIgnite().cache(Constants.INGITE_LATEST_SA_CONNECTION_UID_CACHE);
		if (cache != null) return cache;

		CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(Constants.INGITE_LATEST_SA_CONNECTION_UID_CACHE);
		cacheCfg.setIndexedTypes(String.class, String.class);
		cacheCfg.setSqlFunctionClasses(IgniteCacheSqlHelperFunctions.class);

		return IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
	}

	public static IgniteCache<String, String> getIgniteUserOutboundGroupCache() {
		CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(Constants.IGNITE_USER_OUTBOUND_GROUP_CACHE);
		cacheCfg.setIndexedTypes(String.class, String.class);
		return IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
	}

	public static IgniteCache<String, String> getIgniteUserInboundGroupCache() {
		CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(Constants.IGNITE_USER_INBOUND_GROUP_CACHE);
		cacheCfg.setIndexedTypes(String.class, String.class);
		return IgniteHolder.getInstance().getIgnite().getOrCreateCache(cacheCfg);
	}

	public static Collection<String> getAllLatestSAsForGroupVector(String groupVector) {
		Collection<String> sas = new ArrayList<>();

		String latestSAQuery = "SELECT LSA._VAL " +
			"FROM \"" + Constants.INGITE_LATEST_SA_CONNECTION_UID_CACHE + "\".String LSA, \"" +
			Constants.IGNITE_USER_OUTBOUND_GROUP_CACHE + "\".String OG " +
			"WHERE LSA._KEY = OG._KEY " +
			"  AND groupsOverlap(?,OG._VAL);";

		logger.debug("cluster latest SA query: {}", latestSAQuery);

		SqlFieldsQuery qry = new SqlFieldsQuery(latestSAQuery);
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
