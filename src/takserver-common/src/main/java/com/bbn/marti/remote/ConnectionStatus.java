

package com.bbn.marti.remote;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;

import tak.server.ignite.IgniteHolder;

public class ConnectionStatus implements Serializable {
    
	private static final long serialVersionUID = -7402936954278885462L;
	
	private final String nodeId;
	private ConnectionInfo connection;
	
	// link to the federate that matches this connection
    private Federate federate;
	
    public Federate getFederate() {
        return federate;
    }

    public void setFederate(Federate federate) {
        this.federate = federate;
    }
    
    public String getFederateName() {
        if (federate == null) {
            return "";
        }
        
        return federate.getName();
    }

    public ConnectionStatus(ConnectionStatusValue connectionStatus) {
		this(connectionStatus, null);
	}

	public ConnectionStatus(ConnectionStatusValue connectionStatus, String message) {
		this.connectionStatusValue.set(connectionStatus);
		this.lastError = message;
		this.nodeId = IgniteHolder.getInstance().getIgniteStringId();
	}
	
	public ConnectionStatus(ConnectionStatusValue connectionStatus, String message, Federate federate, ConnectionInfo connection) {
		this.connectionStatusValue.set(connectionStatus);
		this.lastError = message;
		this.nodeId = connection.getNodeId();
		this.federate = federate;
		this.connection = connection;
	}
	
	public String getNodeId() {
		return nodeId;
	}
		
    public String getLastError() {
		return lastError;
	}

	public void setLastError(String message) {
		this.lastError = message;
	}
	
	public ConnectionStatusValue getConnectionStatusValue() {
	    return connectionStatusValue.get();
	}
	
	public boolean compareAndSetConnectionStatusValue(ConnectionStatusValue expect, ConnectionStatusValue update) {
        return connectionStatusValue.compareAndSet(expect, update);
    }

	public void setConnectionStatusValue(ConnectionStatusValue connectionStatusValue) {
		this.connectionStatusValue.set(connectionStatusValue);
	}

	private String lastError;
	
	private final AtomicReference<ConnectionStatusValue> connectionStatusValue = new AtomicReference<>();

    public ConnectionInfo getConnection() {
        return connection;
    }

    public void setConnection(ConnectionInfo connection) {
        this.connection = connection;
    }
    
    private User user;
    
    public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	Set<Group> groups;
	
	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}

	@Override
    public String toString() {
        return "ConnectionStatus [connection=" + connection + ", federate="
                + (federate == null ? "" : federate.getName()) + ", lastError=" + lastError
                + ", connectionStatusValue=" + connectionStatusValue + "]";
    }
}
