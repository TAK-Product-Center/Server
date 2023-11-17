package tak.server.qos;

import java.util.List;
import java.util.Map;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.Qos;
import com.bbn.marti.config.RateLimitRule;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;

// This class is stateless, so that either the ignite service or spring bean may be used.
public class DistributedQoSManager implements QoSManager, org.apache.ignite.services.Service {

	private static final long serialVersionUID = 6710047799516448875L;

	private static final Logger logger = LoggerFactory.getLogger(DistributedQoSManager.class);

	@EventListener({ContextRefreshedEvent.class})
	public void init() {
		try {

			@SuppressWarnings({"rawtypes"})
			Map<String, MessageDeliveryStrategy> deliveryStrategies = MessagingDependencyInjectionProxy.getSpringContext().getBeansOfType(MessageDeliveryStrategy.class);

			if (logger.isDebugEnabled()) {
				logger.debug("delivery strategies: " + deliveryStrategies);
			}

		} catch (Exception e) {
			logger.error("error enumerating delivery strategies", e);
		}

	}


	@Override
	public void setReadLimiterEnabled(boolean enable) {
		Qos qos = qosConf();
		qos.getReadRateLimiter().setEnabled(enable);
		mdip().coreConfig().setAndSaveQos(qos);
		if (enable) {			
			mdip().mrs().enable();
		} else {			
			mdip().mrs().disable();
		}
		
	}
	
	@Override
	public void setReadRules(List<RateLimitRule> rateLimitRule) {
		Qos qos = qosConf();
		qos.getReadRateLimiter().getRateLimitRule().clear();
		for (RateLimitRule rule : rateLimitRule) {
			qos.getReadRateLimiter().getRateLimitRule().add(rule);
		}
		mdip().coreConfig().setAndSaveQos(qos);
		mdip().eventPublisher().publishEvent(new QosRefreshedEvent(mdip().getSpringContext()));
	}


	@Override
	public void setDeliveryLimiterEnabled(boolean enable) {
		Qos qos = qosConf();
		qos.getDeliveryRateLimiter().setEnabled(enable);
		mdip().coreConfig().setAndSaveQos(qos);
		if (enable) {			
			mdip().mds().enable();
		} else {			
			mdip().mds().disable();
		}
	}
	
	@Override
	public void setDeliveryRules(List<RateLimitRule> rateLimitRule) {
		Qos qos = qosConf();
		qos.getDeliveryRateLimiter().getRateLimitRule().clear();
		for (RateLimitRule rule : rateLimitRule) {
			qos.getDeliveryRateLimiter().getRateLimitRule().add(rule);
		}
		mdip().coreConfig().setAndSaveQos(qos);
		mdip().eventPublisher().publishEvent(new QosRefreshedEvent(mdip().getSpringContext()));
	}
	
	
	@Override
	public void setDOSLimiterEnabled(boolean enable) {
		Qos qos = qosConf();
		qos.getDosRateLimiter().setEnabled(enable);
		mdip().coreConfig().setAndSaveQos(qos);
		if (enable) {			
			mdip().mdoss().enable();
		} else {			
			mdip().mdoss().disable();
		}
	}

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
    	
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}
	
	MessagingDependencyInjectionProxy mdip() {
		return MessagingDependencyInjectionProxy.getInstance();
	}
	
	Configuration conf() {
		return mdip().coreConfig().getRemoteConfiguration();
	}
	
	public Qos qosConf() {
		return conf().getFilter().getQos();
	}

	
	@Override
	public Map.Entry<Integer, Integer> getActiveDeliveryRateThresholdAndLimit() {
		return mdip().mds().getActiveRateThresholdAndLimit();
	}
	
	@Override
	public Map.Entry<Integer, Integer> getActiveReadRateThresholdAndLimit() {
		return mdip().mrs().getActiveRateThresholdAndLimit();
	}
	
	@Override
	public Map.Entry<Integer, Integer> getActiveDOSRateThresholdAndLimit() {
		return mdip().mdoss().getActiveRateThresholdAndLimit();
	}
}
