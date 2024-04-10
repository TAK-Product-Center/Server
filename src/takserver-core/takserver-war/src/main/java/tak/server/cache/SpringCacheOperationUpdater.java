package tak.server.cache;

import java.util.List;
import java.util.UUID;

import org.apache.ignite.lang.IgniteBiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;

import com.bbn.marti.remote.config.CoreConfigFacade;

import tak.server.ignite.IgniteHolder;

// this class is responsible for syncing caffine cache events between messaging and api processes
// during a non cluster deployment
public final class SpringCacheOperationUpdater {
	private static final Logger logger = LoggerFactory.getLogger(SpringCacheOperationUpdater.class);
	public static String CAFFINE_CACHE_UPDATE_LISTENER = "caffine.cache.update.listener";

	@Autowired
	@Qualifier("caffineCacheManager")
	private CacheManager caffineCacheManager;
	
	private final boolean isCluster;

	public SpringCacheOperationUpdater() {
		isCluster = CoreConfigFacade.getInstance().getCachedConfiguration().getCluster().isEnabled();
		
		IgniteBiPredicate<UUID, SpringCacheOperationUpdate> ignitePredicate = (nodeId, cacheUpdate) -> {
			if (logger.isDebugEnabled()) {
				logger.debug("SpringCacheOperationUpdater update received from: " + nodeId + " of contents: " + cacheUpdate);
			}

			if (cacheUpdate.getOperation() instanceof CacheEvictOperation) {
				CacheEvictOperation evictOperation = (CacheEvictOperation) cacheUpdate.getOperation();
				
				// if cache wide, invalide the entire cache. otherwise, just evict the key
				if (evictOperation.isCacheWide()) {
					cacheUpdate.getCaches().forEach(cacheName -> {
						Cache cache = caffineCacheManager.getCache(cacheName);
						
						if (cache != null) {
							cache.invalidate();
						}
					});
				} else {
					cacheUpdate.getCaches().forEach(cacheName -> {
						Cache cache = caffineCacheManager.getCache(cacheName);
						
						if (cache != null) {
							cache.evictIfPresent(evictOperation.getKey());
						}
					});
				}
			}

			return true;
		};
		
		// only need listener for non cluster deployment
		if (!isCluster) 
			IgniteHolder.getInstance().getIgnite().message().localListen(CAFFINE_CACHE_UPDATE_LISTENER, ignitePredicate);
	}

	public void publishCacheUpdate(CacheOperation operation, List<String> caches) {
		// no need to proceed if we are in cluster
		if (isCluster) return;
		
		IgniteHolder.getInstance().getIgnite().message(IgniteHolder.getInstance().getIgnite().cluster().forRemotes())
			.send(CAFFINE_CACHE_UPDATE_LISTENER, new SpringCacheOperationUpdate(operation, caches));
	}
}
