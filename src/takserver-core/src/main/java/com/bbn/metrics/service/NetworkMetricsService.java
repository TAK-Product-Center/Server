package com.bbn.metrics.service;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;

public final class NetworkMetricsService implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(NetworkMetricsService.class);
	private AtomicLong totalBytesWritten;
	private AtomicLong totalBytesRead;
	private AtomicLong totalNumberOfWrites = AbstractBroadcastingChannelHandler.totalNumberOfWrites;
	private AtomicLong totalNumberOfReads = AbstractBroadcastingChannelHandler.totalNumberOfReads;


	public AtomicLong getTotalBytesWritten() {
		return totalBytesWritten;
	}

	public AtomicLong getTotalBytesRead() {
		return totalBytesRead;
	}

	public AtomicLong getTotalNumberOfWrites() {
		return totalNumberOfWrites;
	}

	public AtomicLong getTotalNumberOfReads() {
		return totalNumberOfReads;
	}
}
