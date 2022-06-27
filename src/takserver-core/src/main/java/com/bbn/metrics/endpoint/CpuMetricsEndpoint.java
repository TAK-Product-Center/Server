package com.bbn.metrics.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.dto.CpuMetrics;
import com.bbn.metrics.service.ActuatorMetricsService;

@Endpoint(id = "custom-cpu-metrics")
public class CpuMetricsEndpoint {
	private static final Logger logger = LoggerFactory.getLogger(ActuatorMetricsService.class);
	private MetricsCollector metricsCollector;
	@Autowired
	ActuatorMetricsService actuatorMetricsService;
	
	public CpuMetricsEndpoint(MetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}

	@ReadOperation
	public CpuMetrics getCpuUsage() {
		CpuMetrics cpuMetrics = new CpuMetrics();
		cpuMetrics.setCpuCount(actuatorMetricsService.getCpuCount());
		cpuMetrics.setCpuUsage(actuatorMetricsService.getCpuUsage());
	
		return cpuMetrics;
	}
}
