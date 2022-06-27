package com.bbn.metrics.service;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.service.Resources;

public final class DatabaseMetricsService implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseMetricsService.class);
	private static AtomicBoolean isDatabaseConnected = new AtomicBoolean(false);

	private static AtomicInteger maxConnections = new AtomicInteger(1);
	private static AtomicReference<String> serverVersion = new AtomicReference<String>();

	public DatabaseMetricsService(RepositoryService repositoryService) {
	    try {
	        maxConnections.set(Math.max(1, (repositoryService.getMaxConnections() / 2) - 1));
	        serverVersion.set(repositoryService.getServerVersion());
	    } catch (Exception e) {
	    }
    	Resources.dbHealthThreadPool.scheduleWithFixedDelay(() -> {
			try {
	            repositoryService.testDatabaseConnection();
				isDatabaseConnected.set(true);

				int tempMaxConnections = Math.max(1, (repositoryService.getMaxConnections() / 2) - 1);

				if (tempMaxConnections != maxConnections.get()) {
					maxConnections.set(tempMaxConnections);
					serverVersion.set(repositoryService.getServerVersion());
					repositoryService.reinitializeConnectionPool();
				}
			} catch (Exception e) {
				isDatabaseConnected.set(false);
			}
		}, 5, 5, TimeUnit.SECONDS);
	}

    public AtomicBoolean isDatabaseConnected() {
    	return isDatabaseConnected;
    }

    public AtomicInteger getMaxConnections() {
        return maxConnections;
    }

    public AtomicReference<String> getServerVersion() {
        return serverVersion;
    }

}
