package tak.server.qos;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.config.CoreConfigFacade;
import org.apache.log4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.config.DosLimitRule;
import com.bbn.marti.config.RateLimitRule;
import com.bbn.marti.service.Resources;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.google.common.collect.Maps;

import io.micrometer.core.instrument.Metrics;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;

public class MessageDOSStrategy extends MessageBaseStrategy<CotEventContainer> {

	private final static Logger log = Logger.getLogger(MessageDOSStrategy.class);

	private static final String TAK_REQUEST_TYPE = "t-x-takp-q";
	private static final String TAK_PING_TYPE = "t-x-c-t";
	
	private static final String DASH = "-";
	private static final String NEGOTIATION_POSTFIX = "-negotiation";
	private static final String PING_POSTFIX = "-ping";
		
	@EventListener({ContextRefreshedEvent.class})
	private void init() {
		currentRateLimit = Metrics.gauge(Constants.METRIC_DOS_ACTIVE_RATE_LIMIT, new AtomicInteger(-1)); // default to no rate limit
		currentThreshold = Metrics.gauge(Constants.METRIC_DOS_ACTIVE_RATE_LIMIT_THRESHOLD, new AtomicInteger(-1)); // default to no rate limit threshold

		CoreConfig config = CoreConfigFacade.getInstance();

		maxRate = config.getRemoteConfiguration().getFilter().getQos().getDosRateLimiter().getIntervalSeconds();
		
		// should be small enough for reads that we dont need a limit
		maxCacheSize = -1;
		
		// populate rate limit table
		// this table is used for rapid lookup of rate limits when client counts change
		for (DosLimitRule rule : config.getRemoteConfiguration().getFilter().getQos().getDosRateLimiter().getDosLimitRule()) {
			rateLimits.put(rule.getClientThresholdCount(), rule.getMessageLimitPerInterval());
			rateThresholds.add(rule.getClientThresholdCount());
			
			if (rule.getMessageLimitPerInterval() > maxRate) {
				maxRate = rule.getMessageLimitPerInterval();
			}
		}

		// sort rate threshold in descending order, because we will use the first rate that matches client count
		rateThresholds.sort(Collections.reverseOrder());
		
		enabled.set(config.getRemoteConfiguration().getFilter().getQos().getDosRateLimiter().isEnabled());
        
        isInit.set(true);
	}
	
	@Override
	public boolean isAllowed(CotEventContainer message, String ip) {
		if (message == null) {
			throw new IllegalArgumentException("null message");
		}
		
		return isAllowed(message.getType(), message.getUid(), ip);
	}
	
	@Override
	public boolean isAllowed(String messageType, String messageId, String ip) {
		// if this rate limited is disabled, broker the message.
		if (!enabled.get()) {
			return true;
		}

		// do not limit if rate limit is off
		if (currentRateLimit.get() < 1) {
			return true;
		}
		
		Long now = new Date().getTime();
		
		// allow one negotiation message per client through so we don't interfere with proto negotiation		
		if (messageType.compareTo(TAK_REQUEST_TYPE) == 0) {
			String negotiation = messageId + DASH + ip + NEGOTIATION_POSTFIX;
			Long negotiationMessageTimestamp = cache().getIfPresent(negotiation);
			
			if (negotiationMessageTimestamp == null) {
				put(negotiation, now);
				return true;
			} else {
				Metrics.counter(Constants.METRIC_MESSAGE_QOS_DOS_SKIP_COUNT, "takserver", "messaging").increment();
				return false;
			}
		}

		// only allow one ping per 5 seconds per client		
		if (messageType.compareTo(TAK_PING_TYPE) == 0) {
			String ping = messageId + DASH + ip + PING_POSTFIX;
			Long pingMessageTimestamp = cache().getIfPresent(ping);
			
			if (pingMessageTimestamp == null || now - pingMessageTimestamp >= 5000) {
				put(ping, now);
				return true;
			} else {
				Metrics.counter(Constants.METRIC_MESSAGE_QOS_DOS_SKIP_COUNT, "takserver", "messaging").increment();
				return false;
			}
		}

		Long lastestCount = cache().getIfPresent(ip);

		// there was no recorded count for message from this ip, so broker
		if (lastestCount == null) {
			put(ip, 1l);
			return true;
		}
		
		lastestCount++;
				
		// if interval has not passed, check if under message limit		
		if (lastestCount <= currentRateLimit.get()) {
			put(ip, lastestCount);
			return true;
		} else {
			return false;
		}
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
	
	@Override
	public void changeRateLimitIfRequired(int clientCount) {
		
		if (!isInit.get()) {
			return;
		}
		
		for (Integer threshold : rateThresholds) {
			if (clientCount >= threshold) {

				if (threshold != currentThreshold.get()) {
					int rateLimit = rateLimits.get(threshold);
					log.info("Hit client threshold of " + threshold + ". Applying dos rate limit of " + rateLimit + " messages per " + maxRate + " seconds.");
					currentRateLimit.set(rateLimit);
					currentThreshold.set(threshold);
					
					if (currentRateLimit.get() > 0) {
						cache(true);
					}
				}  else {

					log.info("DOS rate limit of " + currentRateLimit.get() + " messages already applied (unchanged)");
				}

				return;

			}
		}

		// limit did not apply, do not limit
		currentRateLimit.set(-1);
		currentThreshold.set(-1);	
	}
	
	private void put(String key, Long value) {
		Resources.qosCacheProcessor.execute(() -> {
			if (key != null) {
				cache().put(key, value);
			}
		});
	}
	
}
