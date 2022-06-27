package tak.server.cache;

import java.util.ArrayList;
import java.util.List;

import org.apache.ignite.IgniteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;

import com.bbn.marti.remote.sync.MissionChangeType;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.service.MissionService;
import com.google.common.base.Strings;

import tak.server.Constants;

public class MissionCacheHelper {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    @Lazy // lazy is necessary due to circular dependency. Could be fixed by wrapping this around whole mission data layer, or combining them.
    private MissionService missionService;
    
    @Autowired
    private MissionRepository missionRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(MissionCacheHelper.class);
    
    /*
     * // get the latest mission change for each item in the mission
		List<MissionChange> changes = null;
		if (mission.getUids().size() > 0 && mission.getContents().size() > 0) {
			changes = missionChangeRepository.findLatest(// db
					mission.getName(), new ArrayList<>(mission.getUids()),
					new ArrayList<>(resourceMap.keySet()), MissionChangeType.ADD_CONTENT.ordinal());
		} else if (mission.getUids().size() > 0) {
			changes = missionChangeRepository.findLatestForUids(// db
					mission.getName(), new ArrayList<>(mission.getUids()), MissionChangeType.ADD_CONTENT.ordinal());
		} else if (mission.getContents().size() > 0) {
			changes = missionChangeRepository.findLatestForHashes( // db
					mission.getName(), new ArrayList<>(resourceMap.keySet()), MissionChangeType.ADD_CONTENT.ordinal());
		} else {
			return mission;
		}
     */
    
    public Mission getMission(String missionName, boolean hydrateDetails, boolean skipCache) {
        
        if (Strings.isNullOrEmpty(missionName)) {
            throw new IllegalArgumentException("can't get cache for empty mission name");
        }
        
        if (skipCache) {
            return doMissionQuery(missionName, hydrateDetails);
        }
        
        String key = getKey(missionName, hydrateDetails);
        
        if (logger.isDebugEnabled()) {
            logger.debug("getMission cache key: " + key);
        }
        
        Mission mission = null;
        
        Object result = getCache(missionName).get(key);
        
        if (result == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("no cache entry for key: " + key);
            }
        }
        
        if (result instanceof Mission) {
            mission = ((Mission) result);
        }
        
        if (mission == null) {
            
            if (logger.isDebugEnabled()) {
                logger.debug("cache miss for " + key);
            }
            
            mission = doMissionQuery(missionName, hydrateDetails);
            
            if (mission != null) {
                
                // cache the mission with the appropriate key
                getCache(missionName).put(key, mission);
            }
            
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("cache hit for " + key);
            }
        }
        
        return mission;
    }
    
    private Mission doMissionQuery(String missionName, boolean hydrateDetails) {
        Mission mission = missionRepository.getByNameNoCache(missionName);
        
        if (logger.isTraceEnabled()) {
            logger.trace("mission name " + missionName + " " + mission);
        }
        
        if (mission != null && hydrateDetails) {
            missionService.hydrate(mission, hydrateDetails);
        }
        
        return mission;
    }
    
    public void putMission(Mission mission, boolean isDetailHydrated) {
        
        if (mission == null) {
            throw new IllegalArgumentException("null mission");
        }
        
        
        if (Strings.isNullOrEmpty(mission.getName())) {
            throw new IllegalArgumentException("empty mission name");
        }
        
        String key = getKey(mission.getName(), isDetailHydrated);
        
        if (logger.isDebugEnabled()) {
            logger.debug("put mission key: " + key + " isHydrated: " + isDetailHydrated + " mission: " + mission);
        }
        
        getCache(mission.getName()).put(key, mission);
    }
    
    public void putMissionIfDirty(Mission mission, boolean isDetailHydrated) {
        
        if (mission == null) {
            throw new IllegalArgumentException("null mission");
        }
        
        
        if (Strings.isNullOrEmpty(mission.getName())) {
            throw new IllegalArgumentException("empty mission name");
        }
        
        String key = getKey(mission.getName(), isDetailHydrated);
        
        if (logger.isDebugEnabled()) {
            logger.debug("putIfDirty cache key: " + key);
        }
        
        Object result = getCache(mission.getName()).get(key);
        
        if (result == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("no cache entry for key: " + key + " - putting");
            }
            
            getCache(mission.getName()).put(key, mission);
        }
    }
    
    public void clear(Mission mission, boolean isDetailHydrated) {
        
        if (mission == null) {
            throw new IllegalArgumentException("null mission - can't remove");
        }
        
        if (Strings.isNullOrEmpty(mission.getName())) {
            throw new IllegalArgumentException("empty mission name");
        }
        
        String key = getKey(mission.getName(), isDetailHydrated);
        
        if (logger.isDebugEnabled()) {
            logger.debug("clear mission key: " + key + " isHydrated: " + isDetailHydrated);
        }
        
        //		getMissionCache(mission.getName()).put(key, "null");
        getCache(mission.getName()).removeAsync(key);
    }
    
    /*
     * Get the Ignite cache created by the Spring cache manager so that the options will be the same and ensure that it's the same one
     */
    @SuppressWarnings("unchecked")
    public IgniteCache<Object, Object> getCache(String cacheName) {
        
        Object springNativeCache = cacheManager.getCache(cacheName).getNativeCache();
        
        if (!(springNativeCache instanceof IgniteCache)) {
            throw new IllegalArgumentException("invalid cache type " + springNativeCache.getClass().getTypeName()); 
        }
        
        return ((IgniteCache<Object, Object>) springNativeCache); 
        
    }
    
    public String getKey(String missionName, boolean hydrateDetails) {
        
        return "[getMission, " + missionName + ", " + (hydrateDetails ? "true, hydrated" : "false") + "]";
    }
    
    public void clearAllMissionCache() {
        getCache(Constants.ALL_MISSION_CACHE).clear();
    }
}
