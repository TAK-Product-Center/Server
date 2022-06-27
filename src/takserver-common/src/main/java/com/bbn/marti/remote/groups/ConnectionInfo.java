

package com.bbn.marti.remote.groups;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.NotNull;

import com.bbn.marti.config.Network.Input;

import tak.server.ignite.IgniteHolder;

/*
 * 
 * Value class storing connection-specific information
 * 
 */
public class ConnectionInfo implements Serializable {
    
    private static final long serialVersionUID = -4681053433717680556L;

    private static final AtomicLong counter = new AtomicLong();
    
    private String connectionId;
    private String nodeId;
    private int port;
    private String address;
    private boolean tls;
    private Input input;
    private X509Certificate cert;

    public X509Certificate getCaCert() {
        return caCert;
    }

    public void setCaCert(X509Certificate caCert) {
        this.caCert = caCert;
    }

    private X509Certificate caCert;
    private final long id;
    private boolean client;
    private transient Object handler;
    
    private transient Object emptyPipeline;
    
    public ConnectionInfo() { 
    	id = counter.getAndIncrement(); 
    	nodeId = IgniteHolder.getInstance().getIgniteStringId();
    }
    
    public String getNodeId() {
    	return nodeId;
    }
    
    private AtomicInteger readCount = new AtomicInteger();
    
    public AtomicInteger getReadCount() {
        return readCount;
    }

    public void setReadCount(AtomicInteger readCount) {
        this.readCount = readCount;
    }
    
    private AtomicInteger processedCount = new AtomicInteger();
    
    public AtomicInteger getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(AtomicInteger processedCount) {
        this.processedCount = processedCount;
    }

    public String getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public boolean isTls() {
        return tls;
    }

    public void setTls(boolean tls) {
        this.tls = tls;
    }
    
    public Input getInput() {
        return input;
    }

    public void setInput(@NotNull Input input) {
        this.input = input;
    }
    
    public X509Certificate getCert() {
        return cert;
    }

    public void setCert(X509Certificate cert) {
        this.cert = cert;
    }

    public boolean isClient() {
        return client;
    }

    public void setClient(boolean client) {
        this.client = client;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public Object getHandler() {
        return handler;
    }

    public void setHandler(Object handler) {
        this.handler = handler;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connectionId == null) ? 0 : connectionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConnectionInfo other = (ConnectionInfo) obj;
        if (connectionId == null) {
            if (other.connectionId != null)
                return false;
        } else if (!connectionId.equals(other.connectionId))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConnectionInfo [connectionId=");
        builder.append(connectionId);
        builder.append(", nodeId=");
        builder.append(nodeId);
        builder.append(", port=");
        builder.append(port);
        builder.append(", address=");
        builder.append(address);
        builder.append(", tls=");
        builder.append(tls);
        builder.append(", input=");
        builder.append(input);
        builder.append(", cert=");
        builder.append(cert);
        builder.append(", caCert=");
        builder.append(caCert);
        builder.append(", id=");
        builder.append(id);
        builder.append(", client=");
        builder.append(client);
        builder.append(", readCount=");
        builder.append(readCount);
        builder.append(", processedCount=");
        builder.append(processedCount);
        builder.append("]");
        return builder.toString();
    }
}
