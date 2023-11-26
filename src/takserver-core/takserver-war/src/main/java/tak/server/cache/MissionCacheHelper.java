package tak.server.cache;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ignite.IgniteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;

import com.bbn.marti.sync.model.Mission;
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
	
	private static final AtomicBoolean isInvalidateAllMissionCache = new AtomicBoolean(false);

	private static final Logger logger = LoggerFactory.getLogger(MissionCacheHelper.class);

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
	
	public Mission getMissionByGuid(UUID guid, boolean hydrateDetails, boolean skipCache) {

		if (guid == null) {
			throw new IllegalArgumentException("empty guid parameter");
		}

		if (skipCache) {
			return doMissionQueryGuid(guid, hydrateDetails);
		}
		
		String key = getKeyGuid(guid, hydrateDetails);

		logger.debug("getMissionByGuid cache key {} ", key);
		
		Mission mission = null;

		Object result = getCache(guid).get(key);

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

			mission = doMissionQueryGuid(guid, hydrateDetails);

			if (mission != null) {

				// cache the mission with the appropriate key
				getCache(guid).put(key, mission);
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
			logger.trace("mission {} : {} ", missionName, mission);
		}

		if (mission != null && hydrateDetails) {
			missionService.hydrate(mission, hydrateDetails);
		}

		return mission;
	}
	
	private Mission doMissionQueryGuid(UUID guid, boolean hydrateDetails) {

		Mission mission = missionRepository.getByGuidNoCache(guid);

		logger.trace("mission {} : {} ", guid, mission);
		
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

		Object springNativeCache = cacheManager.getCache(cacheName.toLowerCase()).getNativeCache();

		if (!(springNativeCache instanceof IgniteCache)) {
			throw new IllegalArgumentException("invalid cache type " + springNativeCache.getClass().getTypeName()); 
		}

		return ((IgniteCache<Object, Object>) springNativeCache); 

	}
	
	/*
	 * Get the Ignite cache created by the Spring cache manager so that the options will be the same and ensure that it's the same one
	 */
	@SuppressWarnings("unchecked")
	public IgniteCache<Object, Object> getCache(UUID cacheUUID) {

		Object springNativeCache = cacheManager.getCache(cacheUUID.toString()).getNativeCache();

		if (!(springNativeCache instanceof IgniteCache)) {
			throw new IllegalArgumentException("invalid cache type " + springNativeCache.getClass().getTypeName()); 
		}

		return ((IgniteCache<Object, Object>) springNativeCache); 

	}

	public String getKey(String missionName, boolean hydrateDetails) {

		return "[getMission, " + missionName.toLowerCase() + ", " + (hydrateDetails ? "true, hydrated" : "false") + "]";
	}
	
	public String getKeyGuid(UUID guid, boolean hydrateDetails) {

		return "[missionguid_" + guid + "_" + (hydrateDetails ? "true, hydrated" : "false") + "]";
	}
	
	public void clearAllMissionAndCopsCache() {

		// don't allow concurrent clears of these caches.
		if (isInvalidateAllMissionCache.compareAndSet(false, true)) {
			try {
				
				try {
					Cache allMissionCache = cacheManager.getCache(Constants.ALL_MISSION_CACHE);

					if (allMissionCache != null) {
						allMissionCache.invalidate();
					}
				} catch (Exception e) {
					logger.error("error clearing all mission cache.", e);
				}

				try {
					Cache allCopsCache = cacheManager.getCache(Constants.ALL_COPS_MISSION_CACHE);

					if (allCopsCache != null) {
						allCopsCache.invalidate();
					}
				} catch (Exception e) {
					logger.error("error clearing all mission cache.", e);
				}
				
				logger.debug("cleared all mission cache and all cops cache.");
			} catch (Exception e) {
				logger.error("error clearing all mission cache and all cops cache.", e);
			} finally {
				isInvalidateAllMissionCache.set(false);
			}
		}

		return;
	}
}
