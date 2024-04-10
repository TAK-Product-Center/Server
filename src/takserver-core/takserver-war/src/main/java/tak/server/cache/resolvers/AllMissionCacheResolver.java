package tak.server.cache.resolvers;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheEvictOperation.Builder;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

import tak.server.Constants;

public class AllMissionCacheResolver extends TakCacheManagerResolver implements CacheResolver {

    public static final String ALL_MISSION_CACHE_RESOLVER = "allMissionCacheResolver";
    private static final Logger logger = LoggerFactory.getLogger(AllMissionCacheResolver.class);
    
	private List<Cache> getCaches() {
		List<Cache> caches = new CopyOnWriteArrayList<>();
		                        
		Cache cache = getCacheManager().getCache(Constants.ALL_MISSION_CACHE);
		caches.add(cache);
		return caches;
	}

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        try {
            List<Cache> caches = getCaches();
        	
            return caches;
        } catch (Exception e) {
            logger.error("exception in resolveCafffineCache!", e);
            return null;
        }
    }
    
    public void invalidateCache() {
    	// get caches
    	List<Cache> caches = getCaches();
    	
    	// invalidate caches
    	caches.stream()
    		.filter(cache -> cache != null)
    		.forEach(cache -> cache.invalidate());
    	
    	Builder operation = new CacheEvictOperation.Builder();
    	operation.setCacheWide(true);
    	
    	// publish cache invalidation
		List<String> cacheNames = caches.stream().map(c -> c.getName()).collect(Collectors.toList());
		springCacheOperationUpdater.publishCacheUpdate(operation.build(), cacheNames);
    }
}
