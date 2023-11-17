package tak.server.qos;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.config.RateLimitRule;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.google.common.collect.Maps;

import io.micrometer.core.instrument.Metrics;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;

public class MessageReadStrategy extends MessageBaseStrategy<CotEventContainer> {

	private final static Logger log = Logger.getLogger(MessageReadStrategy.class);
	
	@EventListener({ContextRefreshedEvent.class})
	private void init() {
		currentRateLimit = Metrics.gauge(Constants.METRIC_READ_ACTIVE_RATE_LIMIT, new AtomicInteger(-1)); // default to no rate limit
		currentThreshold = Metrics.gauge(Constants.METRIC_READ_ACTIVE_RATE_LIMIT_THRESHOLD, new AtomicInteger(-1)); // default to no rate limit threshold

		// should be small enough for reads that we dont need a limit
		maxCacheSize = -1;
		
		// populate rate limit table
		// this table is used for rapid lookup of rate limits when client counts change
		for (RateLimitRule rule : config.getRemoteConfiguration().getFilter().getQos().getReadRateLimiter().getRateLimitRule()) {
			rateLimits.put(rule.getClientThresholdCount(), rule.getReportingRateLimitSeconds());
			rateThresholds.add(rule.getClientThresholdCount());
			
			if (rule.getReportingRateLimitSeconds() > maxRate) {
				maxRate = rule.getReportingRateLimitSeconds();
			}
		}

		// sort rate threshold in descending order, because we will use the first rate that matches client count
		rateThresholds.sort(Collections.reverseOrder());
		
		enabled.set(config.getRemoteConfiguration().getFilter().getQos().getReadRateLimiter().isEnabled());
        
        isInit.set(true);
	}
	
	@EventListener({QosRefreshedEvent.class})
	private void refreshQosRead() {
		rateLimits = new HashMap<>();
		rateThresholds = new LinkedList<>();
		
		// populate rate limit table
		// this table is used for rapid lookup of rate limits when client counts change
		for (RateLimitRule rule : config.getRemoteConfiguration().getFilter().getQos().getReadRateLimiter().getRateLimitRule()) {
			rateLimits.put(rule.getClientThresholdCount(), rule.getReportingRateLimitSeconds());
			rateThresholds.add(rule.getClientThresholdCount());
			
			if (rule.getReportingRateLimitSeconds() > maxRate) {
				maxRate = rule.getReportingRateLimitSeconds();
			}
		}

		// sort rate threshold in descending order, because we will use the first rate that matches client count
		rateThresholds.sort(Collections.reverseOrder());
		if (enabled.get() && metrics != null && metrics.getMetrics() != null) {
			changeRateLimitIfRequired((int) metrics.getMetrics().getNumClients());
		}

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

		// skip control messages		
		if (SubmissionService.getInstance().isControlMessage(messageType))
			return true;
		
		final String key = messageId + "-" + clientId;

		Long now = new Date().getTime();
		

		Long latestMessageTimestamp = cache().getIfPresent(key);

		if (latestMessageTimestamp == null) {
			put(key, now);
			return true;
		}

		// skip this message, rate limit applies
		if (now - latestMessageTimestamp <= (currentRateLimit.get() * 1000)) {
			Metrics.counter(Constants.METRIC_MESSAGE_QOS_READ_SKIP_COUNT, "takserver", "messaging").increment();
			return false;
		}

		// deliver the message and update the timestamp
		put(key, now);

		return true;
	}

	@Override
	public void enable() {
		enabled.set(true);
		changeRateLimitIfRequired((int) metrics.getMetrics().getNumClients());
		log.info(getClass().getSimpleName() + " enabled");
	}

	@Override
	public void disable() {
		enabled.set(false);
		log.info(getClass().getSimpleName() + " disabled");
		currentRateLimit.set(-1);
		currentThreshold.set(-1);
	}
	
	@Override
	public Map.Entry<Integer, Integer> getActiveRateThresholdAndLimit() {
		return Maps.immutableEntry(currentThreshold.get(), currentRateLimit.get());
	}
	
	private void put(String key, Long value) {
		Resources.qosCacheProcessor.execute(() -> {
			if (key != null) {
				cache().put(key, value);
			}
		});
	}
}
