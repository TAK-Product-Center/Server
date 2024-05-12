package tak.server.cache;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.Lazy;

import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.service.MissionService;
import com.google.common.base.Strings;

import tak.server.cache.resolvers.AllCopMissionCacheResolver;
import tak.server.cache.resolvers.AllMissionCacheResolver;

public class MissionCacheHelper {

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    @Qualifier("caffineCacheManager")
    private CacheManager caffineCacheManager;

	@Autowired
	@Lazy // lazy is necessary due to circular dependency. Could be fixed by wrapping this around whole mission data layer, or combining them.
	private MissionService missionService;

	@Autowired
	private MissionRepository missionRepository;
	
	@Autowired
	AllMissionCacheResolver allMissionCacheResolver;
	
	@Autowired
	AllCopMissionCacheResolver allCopMissionCacheResolver;
	
	private static final Logger logger = LoggerFactory.getLogger(MissionCacheHelper.class);
	
	public Mission getMission(String missionName, boolean hydrateDetails, boolean skipCache) {

		if (Strings.isNullOrEmpty(missionName)) {
			throw new IllegalArgumentException("can't get cache for empty mission name");
		}

		if (skipCache) {
			return doMissionQuery(missionName, hydrateDetails);
		}

		String key = getKey(missionName, hydrateDetails);

		Mission mission = null;

		ValueWrapper wrapper = getCacheManager().getCache(missionName).get(key);
		mission = unwrapMission(wrapper);
		
		if (mission != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("cache hit for " + key);
			}
			return mission;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("cache miss for " + key);
		}
		
		Semaphore lock = null;
		try {
			// only lock on cache miss. block to acquire semaphore.
			lock = getMissionLock(key);
			lock.acquire();

			// double-checked cache get
			wrapper = getCacheManager().getCache(missionName).get(key);	
			mission = unwrapMission(wrapper);

			if (mission != null) {
				// cache hit - double-checked lock
				return mission;
			}

			mission = doMissionQuery(missionName, hydrateDetails);

			if (mission != null) {

				if (logger.isDebugEnabled()) {
					logger.debug("Unproxy ExternalMissionData and MapLayer");
				}
				UnproxyHelper.unproxyMission(mission);

				// cache the mission with the appropriate key
				getCacheManager().getCache(missionName).put(key, mission);
			}
		} catch (InterruptedException e) {
			logger.error("interrupted", e);
		} finally {
			try {
				// release lock and remove it from lock map
				lock.release();
			} finally {
				deleteLock(key);
			}
		}
		
		return mission;
	}
	
	public Mission getMissionByGuid(UUID guid, boolean hydrateDetails, boolean skipCache) {

		if (guid == null) {
			throw new IllegalArgumentException("empty guid parameter");
		}

		if (skipCache) {
			return doMissionQueryGuid(guid, hydrateDetails);
		}
		
		String key = getKeyGuid(guid, hydrateDetails);
		
		Mission mission = null;

		ValueWrapper wrapper = getCacheManager().getCache(guid.toString()).get(key);	
		mission = unwrapMission(wrapper);
		
		if (mission != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("cache hit for " + key);
			}
			return mission;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("cache miss for " + key);
		}
		
		Semaphore lock = null;
		try {
			// only lock on cache miss. block to acquire semaphore.
			lock = getMissionLock(key);
			lock.acquire();

			// double-checked cache get
			wrapper = getCacheManager().getCache(guid.toString()).get(key);	
			mission = unwrapMission(wrapper);

			if (mission != null) {
				// cache hit - double-checked lock
				return mission;
			}

			mission = doMissionQueryGuid(guid, hydrateDetails);

			if (mission != null) {

				if (logger.isDebugEnabled()) {
					logger.debug("Unproxy ExternalMissionData and MapLayer");
				}
				UnproxyHelper.unproxyMission(mission);

				// cache the mission with the appropriate key
				getCacheManager().getCache("mg-" + guid.toString()).put(key, mission);
			}
		} catch (InterruptedException e) {
			logger.error("interrupted", e);
		} finally {
			try {
				// release lock and remove it from lock map
				lock.release();
			} finally {
				deleteLock(key);
			}
		}

		return mission;
	}
	
	private Mission unwrapMission(ValueWrapper missionWrapper) {
		Mission mission = null;
		
		Object result = missionWrapper == null ? null : missionWrapper.get();

		if (result instanceof Mission) {
			mission = ((Mission) result);
		}
		
		return mission;
	}

	private Mission doMissionQuery(String missionName, boolean hydrateDetails) {

		Mission mission = missionRepository.getByNameNoCache(missionName);
		
		logger.trace("mission {} : {} ", missionName, mission);
		
		if (mission != null) {
			missionService.hydrate(mission, hydrateDetails);
			
			if (!hydrateDetails) {
				missionService.hydrateFeedNameForMission(mission);		
			}
		}

		return mission;
	}
	
	private Mission doMissionQueryGuid(UUID guid, boolean hydrateDetails) {

		Mission mission = missionRepository.getByGuidNoCache(guid);

		if (logger.isTraceEnabled()) {
			logger.trace("mission {} : {} ", guid, mission);
		}
		
		if (mission != null) {
			missionService.hydrate(mission, hydrateDetails);
			
			if (!hydrateDetails) {
				missionService.hydrateFeedNameForMission(mission);		
			}
		}
		
		return mission;
	}

	public static String getKey(String missionName, boolean hydrateDetails) {

		return "[getMission, " + missionName.toLowerCase() + ", " + (hydrateDetails ? "true, hydrated" : "false") + "]";
	}
	
	public static String getKeyGuid(UUID guid, boolean hydrateDetails) {

		return "[mg-" + guid + "_" + (hydrateDetails ? "true, hydrated" : "false") + "]";
	}
	
	public void clearAllMissionAndCopsCache() {
		if (logger.isDebugEnabled()) {
			logger.debug("Clear All Mission And Cops Cache");
		}		
		
		allMissionCacheResolver.invalidateCache();
		allCopMissionCacheResolver.invalidateCache();
	}
	
    public CacheManager getCacheManager() {
    	if (CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
    		return cacheManager;
    	} else {
    		return caffineCacheManager;
    	}
    }
	
    private final ConcurrentHashMap<String, Semaphore> missionAvailableMap = new ConcurrentHashMap<>();
	
    private Semaphore getMissionLock(String key) {
		Semaphore lock = missionAvailableMap.get(key);

		if (lock != null) {
			return lock;
		}

		lock = new Semaphore(1, true);

		missionAvailableMap.putIfAbsent(key, lock);

		return lock;
	}

	private void deleteLock(String key) {
		missionAvailableMap.remove(key);
	}
}
