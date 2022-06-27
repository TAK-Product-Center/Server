package com.bbn.metrics.service;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;

import com.google.common.util.concurrent.AtomicDouble;

public class ActuatorMetricsService {
	private static final Logger logger = LoggerFactory.getLogger(ActuatorMetricsService.class);
	private AtomicInteger cpuCount = new AtomicInteger();
	private AtomicDouble heapUsed = new AtomicDouble();
	private AtomicDouble heapCommitted = new AtomicDouble();
	private AtomicDouble cpuUsed = new AtomicDouble();	
	private MetricsEndpoint metricsEndpoint;
	
	public ActuatorMetricsService(MetricsEndpoint metricsEndpoint) {
		this.metricsEndpoint = metricsEndpoint;
	}

	public AtomicDouble getHeapUsed() {
		heapUsed.set(metricsEndpoint
				.metric("jvm.memory.used", Arrays.asList("area:heap"))
				.getMeasurements()
				.get(0)
				.getValue());
		
		return heapUsed;
	}

	public AtomicDouble getHeapCommitted() {
		heapCommitted.set(metricsEndpoint
				.metric("jvm.memory.committed", Arrays.asList("area:heap"))
				.getMeasurements()
				.get(0)
				.getValue());
		
		return heapCommitted;
	}
	
	public AtomicDouble getCpuUsage() {
		cpuUsed.set(metricsEndpoint
				.metric("system.cpu.usage", null)
				.getMeasurements()
				.get(0)
				.getValue());
		
		return cpuUsed;
	}
	
	public AtomicInteger getCpuCount() {
		cpuCount.set(metricsEndpoint
				.metric("system.cpu.count", null)
				.getMeasurements()
				.get(0)
				.getValue().intValue());
		
		return cpuCount;
	}
}
