package com.bbn.metrics.dto;

public class ClusterMetrics {
	private long subscriptionCount;
	private long totalMessagesReceivedCount;
	private long totalMessagesClusteredReceivedCount;
	private long totalMessagesClusteredSentCount;
	private long totalMessagesSentCount;
	public long getSubscriptionCount() {
		return subscriptionCount;
	}
	public void setSubscriptionCount(long subscriptionCount) {
		this.subscriptionCount = subscriptionCount;
	}
	public long getTotalMessagesReceivedCount() {
		return totalMessagesReceivedCount;
	}
	public void setTotalMessagesReceivedCount(long totalMessagesReceivedCount) {
		this.totalMessagesReceivedCount = totalMessagesReceivedCount;
	}
	public long getTotalMessagesClusteredReceivedCount() {
		return totalMessagesClusteredReceivedCount;
	}
	public void setTotalMessagesClusteredReceivedCount(long totalMessagesClusteredReceivedCount) {
		this.totalMessagesClusteredReceivedCount = totalMessagesClusteredReceivedCount;
	}
	public long getTotalMessagesClusteredSentCount() {
		return totalMessagesClusteredSentCount;
	}
	public void setTotalMessagesClusteredSentCount(long totalMessagesClusteredSentCount) {
		this.totalMessagesClusteredSentCount = totalMessagesClusteredSentCount;
	}
	public long getTotalMessagesSentCount() {
		return totalMessagesSentCount;
	}
	public void setTotalMessagesSentCount(long totalMessagesSentCount) {
		this.totalMessagesSentCount = totalMessagesSentCount;
	}
	@Override
	public String toString() {
		return "ClusterMetrics [subscriptionCount=" + subscriptionCount + ", totalMessagesReceivedCount="
				+ totalMessagesReceivedCount + ", totalMessagesClusteredReceivedCount="
				+ totalMessagesClusteredReceivedCount + ", totalMessagesClusteredSentCount="
				+ totalMessagesClusteredSentCount + ", totalMessagesSentCount=" + totalMessagesSentCount + "]";
	}

	
	
}
