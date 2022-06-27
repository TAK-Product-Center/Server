package com.bbn.metrics.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import com.bbn.marti.service.SubscriptionManager;
import com.bbn.metrics.MetricsCollector;
import com.bbn.metrics.dto.MetricSubscription;
import com.bbn.metrics.dto.NetworkMetrics;

import tak.server.cluster.ClusterManager;

@Endpoint(id = "custom-network-metrics")
public class NetworkMetricsEndpoint {
    @SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(NetworkMetricsEndpoint.class);
    private MetricsCollector metricsCollector;
    private SubscriptionManager subscriptionManager;
    
    @Autowired(required = false)
    private ClusterManager clusterStateManager;

    public NetworkMetricsEndpoint(MetricsCollector metricsCollector, SubscriptionManager subscriptionManager) {
    	this.metricsCollector = metricsCollector;
    	this.subscriptionManager = subscriptionManager;
	}
    
    @ReadOperation
    public NetworkMetrics getMetrics() {
        NetworkMetrics networkMetrics = new NetworkMetrics();
        networkMetrics.setBytesRead(metricsCollector.getTotalNetworkBytesRead());
        networkMetrics.setBytesWritten(metricsCollector.getTotalNetworkBytesWritten());                
        networkMetrics.setNumClients(clusterStateManager != null ? clusterStateManager.getSubscriptionCount() : subscriptionManager.getLocalSubscriptionCount());
        networkMetrics.setNumReads(metricsCollector.getTotalNumNetworkReads());
        networkMetrics.setNumWrites(metricsCollector.getTotalNumNetworkWrites());
       
        return networkMetrics;
    }
    
    @WriteOperation
    public MetricSubscription getIndividualMetrics(@Selector String uidParam, String uid) {
    	MetricSubscription subscription = subscriptionManager.getMetricSubscription(uid);

        return subscription;
    }
}
