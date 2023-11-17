package tak.server.cache;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.eviction.fifo.FifoEvictionPolicyFactory;
import org.apache.ignite.cache.spring.SpringCacheManager;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.event.ContextRefreshedEvent;

import com.bbn.marti.remote.CoreConfig;

import tak.server.Constants;

public class TakIgniteSpringCacheManager extends SpringCacheManager {

	@Autowired
	CoreConfig config;

	private static final Logger logger = LoggerFactory.getLogger(TakIgniteSpringCacheManager.class);

	private Ignite visibleIgnite = null;
	
	private boolean messagingProfileActive = false;
	
	/** Caches map. */
	private final ConcurrentMap<String, SpringCache> caches = new ConcurrentHashMap<>();

	public TakIgniteSpringCacheManager() {
		throw new IllegalArgumentException("ignite instance must be passed in constructor");
	}

	// require ignite instance to be passed in
	public TakIgniteSpringCacheManager(Ignite ignite) {
		
		String profilesActive = System.getProperty("spring.profiles.active");

		if (profilesActive != null) {
			// Due to initialization order, can't use spring environment and therefore
			// ProfileTracker class yet, so look at the expected system property.
			if (profilesActive.toLowerCase().contains(Constants.MESSAGING_PROFILE_NAME)) {
				messagingProfileActive = true;
			}
		}

		// Force set SpringCacheManager.ignite private field 
		try {
			Field f = SpringCacheManager.class.getDeclaredField("ignite");
			f.setAccessible(true);
			f.set(this, ignite);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		visibleIgnite = ignite;
	}

	@Override public void onApplicationEvent(ContextRefreshedEvent event) { 
		// don't do anything because we required the ignite instance to be passed in.
		// In the original implementation, this is where the ignite instance was created.
	}

	/** {@inheritDoc} */
	@Override public Cache getCache(String name) {

		SpringCache cache = caches.get(name);

		if (cache == null) {

			CacheConfiguration<Object, Object> cacheConfig = new CacheConfiguration<>(name);

			if (config.getRemoteConfiguration().getBuffer().getQueue().isEnableCacheGroup()) {
				cacheConfig.setGroupName("takserver-cache-group");
			}

			if (config.getRemoteConfiguration().getNetwork().isCloudwatchEnable()) {
				cacheConfig.setStatisticsEnabled(true);
			}
			
			cacheConfig.setAtomicityMode(CacheAtomicityMode.ATOMIC);

			IgniteCache<Object, Object> igniteCache = null;

			// use on-heap memory
			boolean onHeapEnabled = config.getRemoteConfiguration().getBuffer().getQueue().isOnHeapEnabled();
			cacheConfig.setOnheapCacheEnabled(onHeapEnabled);
			if (onHeapEnabled) {
				int springCacheMaxSize = config.getRemoteConfiguration().getBuffer().getQueue().getSpringCacheMaxSize();

				if (springCacheMaxSize < 1) {
					// autodetect based on number of cores
					springCacheMaxSize = Runtime.getRuntime().availableProcessors() * config.getRemoteConfiguration().getBuffer().getQueue().getSpringCacheSizeScalingFactor();

					if (logger.isDebugEnabled()) {
						logger.debug("spring cache max size: " + springCacheMaxSize);
					}
				}

				int springCacheBatchSize = config.getRemoteConfiguration().getBuffer().getQueue().getSpringCacheBatchSize();
				int springCacheMaxMemorySize = config.getRemoteConfiguration().getBuffer().getQueue().getSpringCacheMaxMemorySize();

				if (springCacheBatchSize == -1 || springCacheMaxMemorySize == -1) {
					cacheConfig.setEvictionPolicyFactory(new FifoEvictionPolicyFactory<>(
							springCacheMaxSize));
				} else {
					cacheConfig.setEvictionPolicyFactory(new FifoEvictionPolicyFactory<>(
							springCacheMaxSize, springCacheBatchSize, springCacheMaxMemorySize));
				}
			}

			int cacheLastTouchedExpiryMinutes = config.getRemoteConfiguration().getBuffer().getQueue().getCacheLastTouchedExpiryMinutes();
			if (cacheLastTouchedExpiryMinutes != -1) {
				cacheConfig.setExpiryPolicyFactory(
						TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, cacheLastTouchedExpiryMinutes)));
			}

			// near cache defaults to off but can be configured
			if (!messagingProfileActive && config.getRemoteConfiguration().getBuffer().getQueue().getNearCacheMaxSize() > 0) {
				
				NearCacheConfiguration<Object, Object> nearCfg = new NearCacheConfiguration<>();

				nearCfg.setNearEvictionPolicyFactory(new FifoEvictionPolicyFactory<>(config.getRemoteConfiguration().getBuffer().getQueue().getNearCacheMaxSize()));

				igniteCache = visibleIgnite.getOrCreateCache(cacheConfig, nearCfg);

			} else {
				igniteCache = visibleIgnite.getOrCreateCache(cacheConfig);
			}


			if (logger.isDebugEnabled()) {
				logger.debug("create cache name: " + name + " atomicity mode: " + cacheConfig.getAtomicityMode() + " mode: " + cacheConfig.getCacheMode());
			}
			
			
			if (config.getRemoteConfiguration().getNetwork().isCloudwatchEnable()) {
				final IgniteCache<Object, Object> finalIgniteCache = igniteCache;
				
				// FIXME
//
//				Metrics.counter(name + "-CACHE", "CacheHits", () -> finalIgniteCache.metrics().getCacheHits(), StandardUnit.Count);
//				CloudWatchPublisher.addMetric(name + "-CACHE", "CacheMisses", () -> finalIgniteCache.metrics().getCacheMisses(), StandardUnit.Count);
//				CloudWatchPublisher.addMetric(name + "-CACHE", "CacheMisses", () -> finalIgniteCache.metrics().getCacheMisses(), StandardUnit.Count);
//				CloudWatchPublisher.addMetric(name + "-CACHE", "CachePuts", () -> finalIgniteCache.metrics().getCachePuts(), StandardUnit.Count);
//				CloudWatchPublisher.addMetric(name + "-CACHE", "CacheGets", () -> finalIgniteCache.metrics().getCacheGets(), StandardUnit.Count);
//				CloudWatchPublisher.addMetric(name + "-CACHE", "CacheEvicts", () -> finalIgniteCache.metrics().getCacheEvictions(), StandardUnit.Count);
//				CloudWatchPublisher.addMetric(name + "-CACHE", "CacheRemovals", () -> finalIgniteCache.metrics().getCacheRemovals(), StandardUnit.Count);
//				CloudWatchPublisher.addMetric(name + "-CACHE", "CacheHitPercent", () -> finalIgniteCache.metrics().getCacheHitPercentage(), StandardUnit.Percent);
//				CloudWatchPublisher.addMetric(name + "-CACHE", "CacheMissPercent", () -> finalIgniteCache.metrics().getCacheMissPercentage(), StandardUnit.Percent);	
			}

			cache = new SpringCache(igniteCache, this);
			SpringCache old = caches.putIfAbsent(name, cache);

			if (old != null) {
				cache = old;
			}
		}

		return cache;
	}	
}

class SpringCache implements Cache {
	private static final Object NULL = new NullValue();

	private final IgniteCache<Object, Object> cache;

	/**
	 * @param cache Cache.
	 * @param mgr Manager
	 */
	SpringCache(IgniteCache<Object, Object> cache, SpringCacheManager mgr) {
		assert cache != null;

		this.cache = cache;
		//            this.mgr = mgr;
	}

	/** {@inheritDoc} */
	@Override public String getName() {
		return cache.getName();
	}

	/** {@inheritDoc} */
	@Override public Object getNativeCache() {
		return cache;
	}

	/** {@inheritDoc} */
	@Override public ValueWrapper get(Object key) {
		Object val = cache.get(key);

		return val != null ? fromValue(val) : null;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override public <T> T get(Object key, Class<T> type) {
		Object val = cache.get(key);

		if (NULL.equals(val))
			val = null;

		if (val != null && type != null && !type.isInstance(val))
			throw new IllegalStateException("Cached value is not of required type [cacheName=" + cache.getName() +
					", key=" + key + ", val=" + val + ", requiredType=" + type + ']');

		return (T)val;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override public <T> T get(final Object key, final Callable<T> valLdr) {
		Object val = cache.get(key);

		if (val == null) {
			// original implementation had locks here. 
			//                IgniteLock lock = mgr.getSyncLock(cache.getName(), key);
			//
			//                lock.lock();

			try {
				val = cache.get(key);

				if (val == null) {
					try {
						T retVal = valLdr.call();

						val = wrapNull(retVal);

						cache.putAsync(key, val);
					}
					catch (Exception e) {
						throw new ValueRetrievalException(key, valLdr, e);
					}
				}
			}
			finally {
				//                    lock.unlock();
			}
		}

		return (T)unwrapNull(val);
	}

	/** {@inheritDoc} */
	@Override public void put(Object key, Object val) {
		if (val == null)
			cache.withSkipStore().putAsync(key, NULL);
		else
			cache.putAsync(key, val);
	}

	/** {@inheritDoc} */
	@Override public ValueWrapper putIfAbsent(Object key, Object val) {
		Object old;

		if (val == null)
			old = cache.withSkipStore().getAndPutIfAbsentAsync(key, NULL);
		else
			old = cache.getAndPutIfAbsentAsync(key, val);

		return old != null ? fromValue(old) : null;
	}

	/** {@inheritDoc} */
	@Override public void evict(Object key) {
		cache.removeAsync(key);
	}

	/** {@inheritDoc} */
	@Override public void clear() {
		cache.clearAsync();
	}

	/**
	 * @param val Cache value.
	 * @return Wrapped value.
	 */
	private static ValueWrapper fromValue(Object val) {
		assert val != null;

		return new SimpleValueWrapper(unwrapNull(val));
	}

	private static Object unwrapNull(Object val) {
		return NULL.equals(val) ? null : val;
	}

	private <T> Object wrapNull(T val) {
		return val == null ? NULL : val;
	}

	private static class NullValue implements Serializable {
		private static final long serialVersionUID = 87578647641L;

		/** {@inheritDoc} */
		@Override public boolean equals(Object o) {
			return this == o || (o != null && getClass() == o.getClass());
		}
	}


}
