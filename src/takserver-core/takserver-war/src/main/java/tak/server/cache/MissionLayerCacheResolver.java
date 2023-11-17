package tak.server.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import tak.server.Constants;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MissionLayerCacheResolver implements CacheResolver {

    public static final String MISSION_LAYER_CACHE_RESOLVER = "missionLayerCacheResolver";
    private static final Logger logger = LoggerFactory.getLogger(MissionLayerCacheResolver.class);

    public static final String SUFFIX = "-layers";

    @Autowired
    private CacheManager cacheManager;

    public MissionLayerCacheResolver() { }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        try {
            String cacheName = (String) context.getArgs()[0] + SUFFIX;
            List<Cache> caches = new CopyOnWriteArrayList<>();
            caches.add(cacheManager.getCache(cacheName));

            return caches;
        } catch (Exception e) {
            logger.error("exception in resolveCaches!", e);
            return null;
        }
    }
}
