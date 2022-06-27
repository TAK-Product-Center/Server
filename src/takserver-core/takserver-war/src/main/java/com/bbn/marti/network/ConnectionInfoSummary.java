package com.bbn.marti.network;

import java.util.Set;

import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.ConnectionStatusValue;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;

public class ConnectionInfoSummary {

	public ConnectionInfoSummary(Federate federate, ConnectionStatus connectionStatus, ConnectionStatusValue connectionStatusValue) {

		if (federate != null) {
			this.federateId = federate.getId();
			this.federateName = federate.getName();
			this.federateConfig = federate;
		}
		
		if (connectionStatus != null) {
			this.lastError = connectionStatus.getLastError();
			if (connectionStatus.getConnection() != null) {
				this.remoteAddress = connectionStatus.getConnection().getAddress();
				this.remotePort = connectionStatus.getConnection().getPort();
				this.client = connectionStatus.getConnection().isClient();
				this.readCount = connectionStatus.getConnection().getReadCount().get();
				this.processedCount = connectionStatus.getConnection().getProcessedCount().get();
			}
			
			this.user = connectionStatus.getUser();
		}
		
		
		this.connectionStatusValue = connectionStatusValue;
	}
		
    public String getFederateName() {
		return federateName;
	}

    public void setFederateName(String federateName) {
		this.federateName = federateName;
	}

    public String getRemoteAddress() {
		return remoteAddress;
	}
    
	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public int getRemotePort() { return remotePort; }

	public void setRemotePort(int remotePort) {	this.remotePort = remotePort; }

	public boolean isClient() {
		return client;
	}
	
	public void setClient(boolean client) {
		this.client = client;
	}
	
	public int getReadCount() {
		return readCount;
	}
	
	public void setReadCount(int readCount) {
		this.readCount = readCount;
	}
	
	public int getProcessedCount() {
		return processedCount;
	}
	
	public void setProcessedCount(int processedCount) {
		this.processedCount = processedCount;
	}

	public ConnectionStatusValue getConnectionStatusValue() {
		return connectionStatusValue;
	}

	public void setConnectionStatusValue(ConnectionStatusValue connectionStatusValue) {
		this.connectionStatusValue = connectionStatusValue;
	}

	public String getFederateId() {
		return federateId;
	}

	public void setFederateId(String federateId) {
		this.federateId = federateId;
	}
	
	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	private String federateId;
	private String federateName;
	private String remoteAddress;
	private int remotePort;
	private boolean client;
	private int readCount;
	private int processedCount;
	private ConnectionStatusValue connectionStatusValue;
	private String lastError;
	
	private Federate federateConfig;

	public Federate getFederateConfig() {
		return federateConfig;
	}

	public void setFederateConfig(Federate federateConfig) {
		this.federateConfig = federateConfig;
	}
	
	private User user;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	private Set<Group> groups;

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}
	
}
