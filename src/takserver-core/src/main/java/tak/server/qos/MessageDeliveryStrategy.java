package tak.server.qos;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.config.RateLimitRule;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SubmissionService;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;

public class MessageDeliveryStrategy extends MessageBaseStrategy<CotEventContainer> {

	private static final Logger logger = LoggerFactory.getLogger(MessageDeliveryStrategy.class);
	
	private Counter qosNoTimestampCounter = null;
	private Counter qosDeliverySkipCounter = null;
	private Counter qosCachePutSkipCounter = null;
	
	public MessageDeliveryStrategy() {
		qosNoTimestampCounter = Metrics.counter(Constants.METRIC_MESSAGE_QOS_NO_TIMESTAMP_COUNT);
		qosDeliverySkipCounter = Metrics.counter(Constants.METRIC_MESSAGE_QOS_DELIVERY_SKIP_COUNT, "takserver", "messaging");
		qosCachePutSkipCounter = Metrics.counter(Constants.METRIC_QOS_DELIVERY_CACHE_PUT_SKIP);

	}
	
	@EventListener({ContextRefreshedEvent.class})
	private void init() {
		
		currentRateLimit = Metrics.gauge(Constants.METRIC_WRITE_ACTIVE_RATE_LIMIT, new AtomicInteger(-1)); // default to no rate limit
		currentThreshold = Metrics.gauge(Constants.METRIC_WRITE_ACTIVE_RATE_LIMIT_THRESHOLD, new AtomicInteger(-1)); // default to no rate limit threshold

		// TODO make this adaptive based on memory size
		maxCacheSize = config.getRemoteConfiguration().getBuffer().getQueue().getMessageTimestampCacheSizeItems();
		
		if (config.getRemoteConfiguration().getFilter() == null) {
			throw new TakException("filter config not found");
		}

		if (config.getRemoteConfiguration().getFilter().getQos() == null) {
			throw new TakException("qos config not found");
		}

		if (config.getRemoteConfiguration().getFilter().getQos() == null) {
			throw new TakException("qos config not found");
		}

		if (config.getRemoteConfiguration().getFilter().getQos().getDeliveryRateLimiter() == null ||
				config.getRemoteConfiguration().getFilter().getQos().getDeliveryRateLimiter() == null ||
				config.getRemoteConfiguration().getFilter().getQos().getDeliveryRateLimiter().getRateLimitRule()  == null ||
				config.getRemoteConfiguration().getFilter().getQos().getDeliveryRateLimiter().getRateLimitRule().isEmpty()) {
			throw new TakException("no rate limit rules found in config");
		}

		// populate rate limit table
		// this table is used for rapid lookup of rate limits when client counts change
		for (RateLimitRule rule : config.getRemoteConfiguration().getFilter().getQos().getDeliveryRateLimiter().getRateLimitRule()) {
			rateLimits.put(rule.getClientThresholdCount(), rule.getReportingRateLimitSeconds());
			rateThresholds.add(rule.getClientThresholdCount());
			
			if (rule.getReportingRateLimitSeconds() > maxRate) {
				maxRate = rule.getReportingRateLimitSeconds();
			}
		}

		// sort rate threshold in descending order, because we will use the first rate that matches client count
		rateThresholds.sort(Collections.reverseOrder());
		
		enabled.set(config.getRemoteConfiguration().getFilter().getQos().getDeliveryRateLimiter().isEnabled());
        
        isInit.set(true);
	}

	@Override
	public boolean isAllowed(CotEventContainer message, String clientId) {
		if (message == null) {
			throw new IllegalArgumentException("null message");
		}
		
		return isAllowed(message.getType(), message.getUid(), clientId);
	}

	@Override
	public boolean isAllowed(String messageType, String messageId, String clientId) {
		// if this rate limited is disabled, broker the message.
		if (!enabled.get()) {
			return true;
		}
				
		// do not limit if rate limit is off
		if (currentRateLimit.get() < 1) {
			return true;
		}

		if (Strings.isNullOrEmpty(messageId) || Strings.isNullOrEmpty(clientId)) {
			return true;
		}
		
		// Don't limit control messages		
		if (SubmissionService.getInstance().isControlMessage(messageType))
			return true;
		
		// Don't limit messages when the publisher is the receiver	
		if (messageId.equals(clientId)) 
			return true;

		// concat ids for simple key that doesn't need multimap
		final String key = messageId + "-" + clientId;

		final Long now = new Date().getTime();

		Long latestMessageTimestamp = cache().getIfPresent(key);

		if (latestMessageTimestamp == null) {

			qosNoTimestampCounter.increment();

			put(key, now);

			// there was no recorded timestamp for message from this client, so broker
			return true;
		}
		
		// skip this message, rate limit applies
		if (now - latestMessageTimestamp <= (currentRateLimit.get() * 1000)) {

			if (logger.isDebugEnabled()) {
				logger.debug("skipping message from " + messageId + " to " + clientId);
			}
			
			qosDeliverySkipCounter.increment();
			
			return false;
		}

		// deliver the message and update the timestamp
		put(key, now);
		
		return true;
	}

	@Override
	public String toString() {
		return "MessageDeliveryStrategy [config=" + config + ", cache=" + cache() + ", maxCacheSize=" + maxCacheSize + "]";
	}

	@Override
	public void enable() {
		enabled.set(true);
		changeRateLimitIfRequired((int) metrics.getMetrics().getNumClients());
		logger.info(getClass().getSimpleName() + " enabled");
	}

	@Override
	public void disable() {
		enabled.set(false);
		logger.info(getClass().getSimpleName() + " disabled");
		currentRateLimit.set(-1);
		currentThreshold.set(-1);
	}
	
	@Override
	public Map.Entry<Integer, Integer> getActiveRateThresholdAndLimit() {
		return Maps.immutableEntry(currentThreshold.get(), currentRateLimit.get());
	}
	
	private void put(String key, Long value) {
		try {
			Resources.qosCacheProcessor.execute(() -> {
				if (key != null) {
					cache().put(key, value);
				}
			});
		} catch (RejectedExecutionException ree) {
			// count how often full queue has blocked message send
			qosCachePutSkipCounter.increment();
		}
	}
}
