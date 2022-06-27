package com.bbn.metrics.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.dto.MemoryMetrics;
import com.bbn.metrics.service.ActuatorMetricsService;

@Endpoint(id = "custom-memory-metrics")
public class MemoryMetricsEndpoint {
	private static final Logger logger = LoggerFactory.getLogger(MemoryMetricsEndpoint.class);
	private MetricsCollector metricsCollector;
	@Autowired
	ActuatorMetricsService actuatorMetricsService;
	
	public MemoryMetricsEndpoint(MetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}
	

	@ReadOperation
	public MemoryMetrics getMetrics() {
		MemoryMetrics memoryMetrics = new MemoryMetrics();
		memoryMetrics.setHeapCommitted(actuatorMetricsService.getHeapCommitted());
		memoryMetrics.setHeapUsed(actuatorMetricsService.getHeapUsed());
		
		return memoryMetrics;
	}
}
	
