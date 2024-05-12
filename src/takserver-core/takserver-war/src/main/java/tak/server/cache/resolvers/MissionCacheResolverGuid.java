package tak.server.cache.resolvers;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;

public class MissionCacheResolverGuid extends TakCacheManagerResolver {

    public static final String MISSION_CACHE_RESOLVER_GUID = "missionCacheResolverGuid";
    private static final Logger logger = LoggerFactory.getLogger(MissionCacheResolverGuid.class);

    public MissionCacheResolverGuid() { }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        try {
			UUID cacheName = ((UUID) context.getArgs()[0]);
            List<Cache> caches = new CopyOnWriteArrayList<>();
            caches.add(getCacheManager().getCache("mg-" + cacheName.toString()));

            if ((CacheOperation)context.getOperation() instanceof CacheEvictOperation) {
                caches.add(getCacheManager().getCache("mg-" + cacheName + MissionLayerCacheResolver.SUFFIX));	
            }

            return caches;
        } catch (Exception e) {
            logger.error("exception in resolveCaches!", e);
            return null;
        }
    }
}
