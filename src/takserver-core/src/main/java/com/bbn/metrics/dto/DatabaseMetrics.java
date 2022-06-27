package com.bbn.metrics.dto;

public class DatabaseMetrics {

	private boolean isApiConnected;
	private boolean isMessagingConnected;

	private int maxConnections;
	private String serverVersion;

	public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String server_version) {
        this.serverVersion = server_version;
    }

    public boolean isApiConnected() {
		return isApiConnected;
	}

	public void setApiConnected(boolean isApiConnected) {
		this.isApiConnected = isApiConnected;
	}

	public boolean isMessagingConnected() {
		return isMessagingConnected;
	}

	public void setMessagingConnected(boolean isMessagingConnected) {
		this.isMessagingConnected = isMessagingConnected;
	}

	@Override
	public String toString() {
		return "DatabaseMetrics [isApiConnected=" + isApiConnected + ", isMessagingConnected=" + isMessagingConnected
				+ "]";
	}
}
