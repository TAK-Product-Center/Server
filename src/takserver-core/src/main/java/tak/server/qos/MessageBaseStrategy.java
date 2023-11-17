package tak.server.qos;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.service.AddSubscriptionEvent;
import com.bbn.marti.service.RemoveSubscriptionEvent;
import com.bbn.metrics.endpoint.NetworkMetricsEndpoint;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;
//import com.github.benmanes.caffeine.cache.CacheBuilder;


//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;

public abstract class MessageBaseStrategy<T> implements MessageStrategy<T> {
	
	@Autowired
	protected CoreConfig config;

	private Cache<String, Long> cache;

	protected long maxCacheSize;

	protected AtomicInteger currentRateLimit = null; // default to no rate limit
	protected AtomicInteger currentThreshold = null; // default to no rate limit
	
	protected AtomicBoolean enabled = new AtomicBoolean(false);

	@Autowired
	protected NetworkMetricsEndpoint metrics;

	private static final Logger logger = LoggerFactory.getLogger(MessageBaseStrategy.class);

	protected Map<Integer, Integer> rateLimits = new HashMap<>();

	protected List<Integer> rateThresholds = new LinkedList<>();
	
	protected Integer maxRate = 0;
	
	protected AtomicBoolean isInit = new AtomicBoolean(false);

	protected Cache<String, Long> cache() {

		return cache(false);
	}

	protected Cache<String, Long> cache(boolean refresh) {

		if (cache == null) {
			synchronized (this) {
				if (cache == null) {

					Caffeine<Object, Object> builder = Caffeine.newBuilder();

					if (maxCacheSize > 0) {
						builder.maximumSize(maxCacheSize);
					}

					cache = builder.expireAfterWrite(maxRate, TimeUnit.SECONDS).build(); // retain enough cache to support the max reporting rate
				}
			}
		}

		return cache;
	}

	@Override
	public String toString() {
		return "MessageBaseStrategy [config=" + config + ", cache=" + cache + ", maxCacheSize=" + maxCacheSize + "]";
	}
	
	@EventListener
	private void handleAddSubscriptionEvent(AddSubscriptionEvent event) {
		if (logger.isDebugEnabled()) {
			logger.debug("Add subscription: " + event);
		}
		
		// handle init race conditions on metrics
		if (enabled.get() && metrics != null && metrics.getMetrics() != null) {
			changeRateLimitIfRequired((int) metrics.getMetrics().getNumClients()); // truncate
		}
	}

	@EventListener
	private void handleRemoveSubscriptionEvent(RemoveSubscriptionEvent event) {
		if (logger.isDebugEnabled()) {
			logger.debug("Remove subscription: " + event);
		}
		
		// handle init race conditions on metrics
		if (enabled.get() && metrics != null && metrics.getMetrics() != null) {
			changeRateLimitIfRequired((int) metrics.getMetrics().getNumClients()); // truncate
		}
	}

	public void changeRateLimitIfRequired(int clientCount) {
		
		if (!isInit.get()) {
			return;
		}
		
		for (Integer threshold : rateThresholds) {
			if (clientCount >= threshold) {

				if (threshold != currentThreshold.get()) {
					int rateLimit = rateLimits.get(threshold);
					logger.info("Hit client threshold of " + threshold + ". Applying reporting rate limit of " + rateLimit + " seconds.");
					currentRateLimit.set(rateLimit);
					currentThreshold.set(threshold);
					
					if (currentRateLimit.get() > 0) {
						cache(true);
					}
				}  else if (rateLimits.get(threshold) != currentRateLimit.get()){
					int rateLimit = rateLimits.get(threshold);
					logger.info("Applying new rate limit of " + rateLimit + " seconds for client threshold of " + threshold);
					currentRateLimit.set(rateLimit);
					
					if (currentRateLimit.get() > 0) {
						cache(true);
					}
					
				} else {
					logger.info("Reporting rate limit of " + currentRateLimit.get() + " seconds already applied (unchanged)");
				}

				return;

			}
		}

		// limit did not apply, do not limit
		currentRateLimit.set(-1);
		currentThreshold.set(-1);	
	}
	
}
