package tak.server.cache.resolvers;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;

public class MissionLayerCacheResolver extends TakCacheManagerResolver {

    public static final String MISSION_LAYER_CACHE_RESOLVER = "missionLayerCacheResolver";
    private static final Logger logger = LoggerFactory.getLogger(MissionLayerCacheResolver.class);

    public static final String SUFFIX = "-layers";

    public MissionLayerCacheResolver() { }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        try {
            String cacheName = (String) context.getArgs()[0] + SUFFIX;
            List<Cache> caches = new CopyOnWriteArrayList<>();
            caches.add(getCacheManager().getCache(cacheName));

            return caches;
        } catch (Exception e) {
            logger.error("exception in resolveCaches!", e);
            return null;
        }
    }
}
