package tak.server.cache.resolvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheResolver;

import com.bbn.marti.remote.config.CoreConfigFacade;

import tak.server.cache.SpringCacheOperationUpdater;

public abstract class TakCacheManagerResolver implements CacheResolver {
	
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    @Qualifier("caffineCacheManager")
    private CacheManager caffineCacheManager;
    
    @Autowired
    protected SpringCacheOperationUpdater springCacheOperationUpdater;
    
    protected CacheManager getCacheManager() {
    	if (CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
    		return cacheManager;
    	} else {
    		return caffineCacheManager;
    	}
    }
}
