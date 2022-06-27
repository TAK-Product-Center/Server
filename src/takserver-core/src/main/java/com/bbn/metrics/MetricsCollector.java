package com.bbn.metrics;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.metrics.service.DatabaseMetricsService;
import com.bbn.metrics.service.NetworkMetricsService;

/**
 *
 */
public interface MetricsCollector {
	
	public static final String metricsLoggerName = "metrics-logger";
	
	public static final Logger metricsLogger = LoggerFactory.getLogger(metricsLoggerName);
	
	void setNetworkMetricsService(NetworkMetricsService networkMetricsService);
	NetworkMetricsService getNetworkMetricsService();	
	
	DatabaseMetricsService getApiDatabaseMetricsService();
	void setApiDatabaseMetricsService(DatabaseMetricsService databaseMetricsService);

	DatabaseMetricsService getMessagingDatabaseMetricsService();
	void setMessagingDatabaseMetricsService(DatabaseMetricsService databaseMetricsService);

	AtomicLong getTotalNetworkBytesWritten();
	
	AtomicLong getTotalNetworkBytesRead();
	
	AtomicLong getTotalNumNetworkWrites();
	
	AtomicLong getTotalNumNetworkReads();
	
	AtomicLong getBrokerSize();

	AtomicLong getBrokerCapacity();

	AtomicLong getSubmissionSize();
	
	AtomicLong getSubmissionCapacity();

	AtomicLong getRepositorySize();

	AtomicLong getRepositoryCapacity();
	
}
