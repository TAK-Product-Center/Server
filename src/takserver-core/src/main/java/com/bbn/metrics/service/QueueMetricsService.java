package com.bbn.metrics.service;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.service.BrokerService;
import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.remote.util.SpringContextBeanForApi;


public final class QueueMetricsService implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(QueueMetricsService.class);
	
	private final BrokerService brokerService;
	private final SubmissionService submissionService;
	private final RepositoryService repositoryService;
	
	private static QueueMetricsService instance = null;

	public static QueueMetricsService getInstance() {
    	if (instance == null) {
    		synchronized(QueueMetricsService.class) {
    			if (instance == null) {
    				instance = SpringContextBeanForApi.getSpringContext().getBean(QueueMetricsService.class);
    			}
    		}
    	}
    	return instance;
    }
	
	public QueueMetricsService(BrokerService brokerService, SubmissionService submissionService, RepositoryService repositoryService) {
		this.brokerService = brokerService;
		this.submissionService = submissionService;
		this.repositoryService = repositoryService;
	}

	public AtomicLong getBrokerSize() {
		return brokerService.getQueueMetrics().currentSize;
	}

	public AtomicLong getBrokerCapacity() {
		return brokerService.getQueueMetrics().capacity;
	}

	public AtomicLong getSubmissionSize() {
		return submissionService.getQueueMetrics().currentSize;
	}

	public AtomicLong getSubmissionCapacity() {
		return submissionService.getQueueMetrics().capacity;
	}

	public AtomicLong getRepositorySize() {
		return repositoryService.getQueueMetrics().currentSize;
	}

	public AtomicLong getRepositoryCapacity() {
		return repositoryService.getQueueMetrics().capacity;
	}
}
