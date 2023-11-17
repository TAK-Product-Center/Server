package tak.server.cache.classification;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import tak.server.Constants;


public class ClassificationCacheHelper {

    @Autowired
    private Ignite ignite;

    @Autowired
    private CacheManager cacheManager;


    public IgniteCache<Object, Object> getClassificationCache() {
        Object springNativeCache = cacheManager.getCache(Constants.CLASSIFICATION_CACHE).getNativeCache();

        if (!(springNativeCache instanceof IgniteCache)) {
            throw new IllegalArgumentException("invalid cache type " + springNativeCache.getClass().getTypeName());
        }

        return ((IgniteCache<Object, Object>) springNativeCache);
    }
}