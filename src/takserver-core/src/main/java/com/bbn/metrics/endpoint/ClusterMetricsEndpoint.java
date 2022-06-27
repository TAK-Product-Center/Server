package com.bbn.metrics.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Profile;

import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.metrics.dto.ClusterMetrics;

import tak.server.Constants;
import tak.server.cluster.ClusterManager;

@Profile(Constants.CLUSTER_PROFILE_NAME)
@Endpoint(id = "takserver-cluster")
public class ClusterMetricsEndpoint {
	
    @SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(MemoryMetricsEndpoint.class);
    
    @Autowired(required = false)
    private ClusterManager clusterStateManager; 
	
	@ReadOperation
	public ClusterMetrics getMetrics() {
		
		ClusterMetrics response = new ClusterMetrics();
		
		if (clusterStateManager != null) {
			response.setSubscriptionCount(clusterStateManager.getSubscriptionCount());
			response.setTotalMessagesReceivedCount(clusterStateManager.getMessagesReceivedCount());
			response.setTotalMessagesClusteredSentCount(clusterStateManager.getClusterMessagesSentCount());
			response.setTotalMessagesClusteredReceivedCount(clusterStateManager.getClusterMessagesReceivedCount());
			response.setTotalMessagesSentCount(clusterStateManager.getMessagesSentCount());
		}
		
		return response;
	}
}
	
