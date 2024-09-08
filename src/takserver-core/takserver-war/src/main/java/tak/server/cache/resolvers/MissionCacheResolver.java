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

import tak.server.cache.MissionCacheHelper;

public class MissionCacheResolver extends TakCacheManagerResolver {

    public static final String MISSION_CACHE_RESOLVER = "missionCacheResolver";
    private static final Logger logger = LoggerFactory.getLogger(MissionCacheResolver.class);

    public MissionCacheResolver() { }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        try {        	
            logger.debug("resolveCaches args {} method {}", context.getArgs(), context.getMethod().getName());

        	// basic case for old behavior
        	if (context.getArgs()[0] instanceof String) { 
        		logger.debug("basic case");
        		String cacheName = ((String) context.getArgs()[0]);
        		
            	List<Cache> caches = new CopyOnWriteArrayList<>();
                caches.add(getCacheManager().getCache(cacheName.toLowerCase()));

                if ((CacheOperation)context.getOperation() instanceof CacheEvictOperation) {
                    caches.add(getCacheManager().getCache(cacheName + MissionLayerCacheResolver.SUFFIX));	
                }
                
                return caches;
        	}
               	
        	
            // can alter behavior here by method if there are issues with different parameters, using the method name
            
            String cacheNameMissionGuid = null;
        	String cacheNameMissionName = null;

            if ((context.getArgs()[0] != null) && context.getArgs()[0] instanceof UUID) {
            	// adding toString() for the UUID case - which can't be cast as a String.
            	cacheNameMissionGuid = "mg-" + ((String) context.getArgs()[0].toString());
            } else {
            	cacheNameMissionName = ((String) context.getArgs()[0].toString()); // name
            }
            
            if (cacheNameMissionName == null) {
            	if (context.getArgs().length > 1 && (context.getArgs()[1] != null)) {
            		cacheNameMissionName = ((String) context.getArgs()[1].toString());
            	}
            }

            if (cacheNameMissionName != null) {
                cacheNameMissionName = cacheNameMissionName.toLowerCase();
            }

            List<Cache> caches = new CopyOnWriteArrayList<>();
            
            if (cacheNameMissionGuid != null) {
            	caches.add(getCacheManager().getCache(cacheNameMissionGuid));
            }

            logger.debug("mission cache operation for cache name guid {} cache name name {} cache context {} args {}", cacheNameMissionGuid, cacheNameMissionName, context.getArgs());
            
            if ((CacheOperation)context.getOperation() instanceof CacheEvictOperation) {
                caches.add(getCacheManager().getCache(cacheNameMissionGuid + MissionLayerCacheResolver.SUFFIX));
                
                caches.add(getCacheManager().getCache(cacheNameMissionGuid));
                
                if (context.getArgs()[0] instanceof UUID) {
                	caches.add(getCacheManager().getCache(MissionCacheHelper.getKeyGuid(((UUID) context.getArgs()[0]), false)));
                	caches.add(getCacheManager().getCache(MissionCacheHelper.getKeyGuid(((UUID) context.getArgs()[0]), true)));
                }
                
                if (cacheNameMissionName != null) {
                    caches.add(getCacheManager().getCache(cacheNameMissionName + MissionLayerCacheResolver.SUFFIX));
                    caches.add(getCacheManager().getCache(cacheNameMissionName));
                    
                    caches.add(getCacheManager().getCache(MissionCacheHelper.getKey(cacheNameMissionName, false)));
                    caches.add(getCacheManager().getCache(MissionCacheHelper.getKey(cacheNameMissionName, true)));
                }
            }
            
            return caches;
        } catch (Exception e) {
            logger.error("exception in resolveCaches!", e);
            return null;
        }
    }
}
