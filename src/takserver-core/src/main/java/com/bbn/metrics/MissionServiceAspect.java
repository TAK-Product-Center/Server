package com.bbn.metrics;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

import io.micrometer.core.instrument.Metrics;

@Aspect
@Configurable
public class MissionServiceAspect {
	
	private static final Logger logger = LoggerFactory.getLogger(MissionServiceAspect.class);
	
	private static final Map<Integer, Long> joinpointBeforeTimestampMap = new ConcurrentHashMap<>();
	
	@Before("execution(* com.bbn.marti.sync.service.MissionService.**(..))")
	public void missionServiceBefore(JoinPoint jp) {

		joinpointBeforeTimestampMap.put(jp.hashCode(), System.currentTimeMillis());
	}

	@After("execution(* com.bbn.marti.sync.service.MissionService.**(..))")
	public void missionServiceAfter(JoinPoint jp) {

		try {

			long start = joinpointBeforeTimestampMap.get(jp.hashCode());

			long execMs = System.currentTimeMillis() - start;

			joinpointBeforeTimestampMap.remove(jp.hashCode());
			
			afterMethod(jp, execMs, "MissionService");

		} catch (Throwable t) {
			logger.error("missionService metrics joinpoint error", t);
		}
	}
	
	@Before("execution(* com.bbn.marti.sync.api.MissionApi.**(..))")
	public void missionApiBefore(JoinPoint jp) {

		joinpointBeforeTimestampMap.put(jp.hashCode(), System.currentTimeMillis());
		
	}

	@After("execution(* com.bbn.marti.sync.api.MissionApi.**(..))")
	public void missionApiAfter(JoinPoint jp) {

		try {

			long start = joinpointBeforeTimestampMap.get(jp.hashCode());

			long execMs = System.currentTimeMillis() - start;

			joinpointBeforeTimestampMap.remove(jp.hashCode());
			
			afterMethod(jp, execMs, "MissionApi");

		} catch (Throwable t) {
			logger.error("missionService metrics joinpoint error", t);
		}
	}
	
	@Before("execution(* com.bbn.marti.sync.repository..*.*(..))")
	public void repositoryBefore(JoinPoint jp) {

		joinpointBeforeTimestampMap.put(jp.hashCode(), System.currentTimeMillis());
	}

	@After("execution(* com.bbn.marti.sync.repository..*.*(..))")
	public void repositoryAfter(JoinPoint jp) {

		try {
			long start = joinpointBeforeTimestampMap.get(jp.hashCode());

			long execMs = System.currentTimeMillis() - start;

			joinpointBeforeTimestampMap.remove(jp.hashCode());
			
			afterMethod(jp, execMs, "repository");

		} catch (Throwable t) {
			logger.error("missionService metrics joinpoint error", t);
		}
	}

	private void afterMethod(JoinPoint jp, long execMs, String tag) {
		
		String className = jp.getTarget().getClass().getSimpleName();

		String trimSignature = jp.getSignature().toLongString().split("\\(")[1];
		String fixedSignature = jp.getSignature().getName() + "(" + trimSignature;

		try {
			
			// individual metrics
			Metrics.counter("method-exec-counter-" + className + "." + fixedSignature, "takserver", tag).increment();
			Metrics.timer("method-exec-timer-" + className + "." + fixedSignature, "takserver", tag).record(Duration.ofMillis(execMs));
			
			// aggregate metrics (required because CloudWatch does not support aggregation across custom metric dimensions)
			Metrics.counter("method-exec-counter-" + tag, "takserver-aggregate", tag + "-aggregate").increment();
			Metrics.timer("method-exec-timer-" + tag, "takserver-aggregate", tag + "-aggregate").record(Duration.ofMillis(execMs));

		} catch (Exception e) {
			logger.error("error recording duration metric ",e);
		}
	}
}
