package com.bbn.metrics.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.dto.QueueMetrics;

@Endpoint(id = "custom-queue-metrics")
public class QueueMetricsEndpoint {
	private static final Logger logger = LoggerFactory.getLogger(QueueMetricsEndpoint.class);

	MetricsCollector metricsCollector;
	
	public QueueMetricsEndpoint(MetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}

	@ReadOperation
	public QueueMetrics getMetrics() {
		QueueMetrics queueMetrics = new QueueMetrics();
		try {
			queueMetrics.setBrokerCapacity(metricsCollector.getBrokerCapacity());
			queueMetrics.setBrokerSize(metricsCollector.getBrokerSize());
			queueMetrics.setRepositoryCapacity(metricsCollector.getRepositoryCapacity());
			queueMetrics.setRepositorySize(metricsCollector.getRepositorySize());
			queueMetrics.setSubmissionCapacity(metricsCollector.getSubmissionCapacity());
			queueMetrics.setSubmissionSize(metricsCollector.getSubmissionSize());
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		
		
		return queueMetrics;
	}
}
