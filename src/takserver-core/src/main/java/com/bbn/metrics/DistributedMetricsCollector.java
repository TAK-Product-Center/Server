package com.bbn.metrics;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ignite.Ignite;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.metrics.service.DatabaseMetricsService;
import com.bbn.metrics.service.NetworkMetricsService;
import com.bbn.metrics.service.QueueMetricsService;

/**
 *
 */
public class DistributedMetricsCollector implements MetricsCollector, Service, Serializable {
	private static final long serialVersionUID = 8781994478252101817L;
	private static final Logger logger = LoggerFactory.getLogger(DistributedMetricsCollector.class);
	private static AtomicLong totalBytesWritten = AbstractBroadcastingChannelHandler.totalBytesWritten;
	private static AtomicLong totalBytesRead = AbstractBroadcastingChannelHandler.totalBytesRead;
	private static AtomicLong totalNumberOfWrites = AbstractBroadcastingChannelHandler.totalNumberOfWrites;
	private static AtomicLong totalNumberOfReads = AbstractBroadcastingChannelHandler.totalNumberOfReads;

	private DatabaseMetricsService apiDatabaseMetricsService;
	private DatabaseMetricsService messagingDatabaseMetricsService;
	private NetworkMetricsService networkMetricsService;

	public DistributedMetricsCollector(Ignite ignite) { }

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

	@Override
	public DatabaseMetricsService getApiDatabaseMetricsService() {
		return apiDatabaseMetricsService;
	}

	@Override
	public void setApiDatabaseMetricsService(DatabaseMetricsService databaseMetricsService) {
		this.apiDatabaseMetricsService = databaseMetricsService;
	}

	@Override
	public DatabaseMetricsService getMessagingDatabaseMetricsService() {
		return messagingDatabaseMetricsService;
	}

	@Override
	public void setMessagingDatabaseMetricsService(DatabaseMetricsService databaseMetricsService) {
		this.messagingDatabaseMetricsService = databaseMetricsService;
	}

	@Override
	public AtomicLong getTotalNetworkBytesWritten() {
		return totalBytesWritten;
	}

	@Override
	public AtomicLong getTotalNetworkBytesRead() {
		return totalBytesRead;
	}

	@Override
	public AtomicLong getTotalNumNetworkWrites() {
		return totalNumberOfWrites;
	}

	@Override
	public AtomicLong getTotalNumNetworkReads() {
		return totalNumberOfReads;
	}

	@Override
	public void setNetworkMetricsService(NetworkMetricsService networkMetricsService) {
		this.networkMetricsService = networkMetricsService;
	}

	@Override
	public NetworkMetricsService getNetworkMetricsService() {
		return networkMetricsService;
	}

	@Override
	public AtomicLong getBrokerCapacity() {
		return QueueMetricsService.getInstance().getBrokerCapacity();
	}

	@Override
	public AtomicLong getBrokerSize() {
		return QueueMetricsService.getInstance().getBrokerSize();
	}

	@Override
	public AtomicLong getRepositoryCapacity() {
		return QueueMetricsService.getInstance().getRepositoryCapacity();
	}

	@Override
	public AtomicLong getRepositorySize() {
		return QueueMetricsService.getInstance().getRepositorySize();
	}

	@Override
	public AtomicLong getSubmissionCapacity() {
		return QueueMetricsService.getInstance().getSubmissionCapacity();
	}

	@Override
	public AtomicLong getSubmissionSize() {
		return QueueMetricsService.getInstance().getSubmissionSize();
	}
}
