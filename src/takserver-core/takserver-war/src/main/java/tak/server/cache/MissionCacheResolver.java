package tak.server.cache;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

public class MissionCacheResolver implements CacheResolver {

    public static final String MISSION_CACHE_RESOLVER = "missionCacheResolver";
    private static final Logger logger = LoggerFactory.getLogger(MissionCacheResolver.class);

    @Autowired
    private CacheManager cacheManager;

    public MissionCacheResolver() { }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        try {
			String cacheName = ((String) context.getArgs()[0]).toLowerCase();
            List<Cache> caches = new CopyOnWriteArrayList<>();
            caches.add(cacheManager.getCache(cacheName));

            if ((CacheOperation)context.getOperation() instanceof CacheEvictOperation) {
                caches.add(cacheManager.getCache(Constants.ALL_MISSION_CACHE));
//                caches.add(cacheManager.getCache(cacheName + MissionChangeCacheResolver.SUFFIX));
            }

            return caches;
        } catch (Exception e) {
            logger.error("exception in resolveCaches!", e);
            return null;
        }
    }
}
