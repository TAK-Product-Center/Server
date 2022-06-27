package com.bbn.metrics.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Lazy;

import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.dto.DatabaseMetrics;
import com.bbn.metrics.service.DatabaseMetricsService;

@Endpoint(id = "takserver-database")
public class DatabaseMetricsEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetricsEndpoint.class);
	private MetricsCollector metricsCollector;

	public DatabaseMetricsEndpoint(@Lazy MetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}

	@ReadOperation
	public DatabaseMetrics getMetrics() {
		DatabaseMetrics response = new DatabaseMetrics();
		response.setApiConnected(metricsCollector.getApiDatabaseMetricsService().isDatabaseConnected().get());
		response.setMessagingConnected(metricsCollector.getMessagingDatabaseMetricsService().isDatabaseConnected().get());
		response.setMaxConnections(metricsCollector.getMessagingDatabaseMetricsService().getMaxConnections().get());
		response.setServerVersion(metricsCollector.getMessagingDatabaseMetricsService().getServerVersion().get());
		return response;
	}

}
