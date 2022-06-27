

package com.bbn.marti.network;

import java.io.Serializable;

import com.bbn.marti.config.Federation;

public class OutgoingConnectionSummary implements Serializable {
	
	private static final long serialVersionUID = -7047484514896133408L;

	public OutgoingConnectionSummary() {}
	
	public OutgoingConnectionSummary(Federation.FederationOutgoing outgoingConnection, ConnectionInfoSummary connectionInfoSummary) {
		this.outgoingConnection = outgoingConnection;
		this.connectionInfoSummary = connectionInfoSummary;
	}

    public String getDisplayName() {
        return outgoingConnection.getDisplayName();
    }

    public void setDisplayName(String value) {
        this.outgoingConnection.setDisplayName(value);
    }

    public String getAddress() {
        return this.outgoingConnection.getAddress();
    }

    public void setAddress(String value) {
        this.outgoingConnection.setAddress(value);
    }

    public Integer getPort() {
        return this.outgoingConnection.getPort();
    }

    public void setPort(Integer value) {
        this.outgoingConnection.setPort(value);
    }

    public boolean isEnabled() {
        return this.outgoingConnection.isEnabled();
    }

    public void setEnabled(Boolean value) {
	this.outgoingConnection.setEnabled(value);
    }

    public String getFallback() {
	return this.outgoingConnection.getFallback();
    }

    public void setFallback(String value) {
	this.outgoingConnection.setFallback(value);
    }

    public Integer getMaxRetries() {
	return this.outgoingConnection.getMaxRetries();
    }

    public void setMaxRetries(Integer value) {
	this.outgoingConnection.setMaxRetries(value);
    }

    public boolean isUnlimitedRetries() {
	return this.outgoingConnection.isUnlimitedRetries();
    }

    public void setUnlimitedRetries(boolean value) {
	this.outgoingConnection.setUnlimitedRetries(value);
    }

    public Integer getProtocolVersion() {
        return this.outgoingConnection.getProtocolVersion();
    }

    public void setProtocolVersion(Integer value) {
        this.outgoingConnection.setProtocolVersion(value);
    }

    public int getReconnectInterval() {
    	return this.outgoingConnection.getReconnectInterval();
    }

    public void setReconnectInterval(Integer value) {
        this.outgoingConnection.setReconnectInterval(value);
    }

    public ConnectionInfoSummary getConnectionInfoSummary() {
	return connectionInfoSummary;
    }

    public void setConnectionInfoSummary(ConnectionInfoSummary connectionInfoSummary) {
	this.connectionInfoSummary = connectionInfoSummary;
    }

    private Federation.FederationOutgoing outgoingConnection;
    private ConnectionInfoSummary connectionInfoSummary;

}
