package tak.server.cache.resolvers;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;

public class MissionChangeCacheResolver extends TakCacheManagerResolver {

    public static final String MISSION_CHANGE_CACHE_RESOLVER = "missionChangeCacheResolver";
    private static final Logger logger = LoggerFactory.getLogger(MissionChangeCacheResolver.class);
    
    public static final String SUFFIX = "-changes";

    public MissionChangeCacheResolver() { }

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
