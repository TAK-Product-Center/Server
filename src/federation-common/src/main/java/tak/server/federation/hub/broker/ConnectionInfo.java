package tak.server.federation.hub.broker;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/*
 *
 * Value class storing connection-specific information
 *
 */
public class ConnectionInfo implements Serializable {

	public enum ConnectionType {
		OUTGOING, INCOMING
	}
	
    private static final long serialVersionUID = -4681053433717680556L;

    private static final AtomicLong counter = new AtomicLong();

    private String connectionId;
    private int port;
    private String address;
    private boolean tls;
    private X509Certificate cert;
    private String remoteServerId;
    private ConnectionType type;
    
    public X509Certificate getClientCert() {
        return clientCert;
    }

    public void setClientCert(X509Certificate clientCert) {
        this.clientCert = clientCert;
    }

    private X509Certificate clientCert;
    private final long id;
    private boolean client;
    private transient Object handler;

    private transient Object emptyPipeline;

    public ConnectionInfo(ConnectionType type, String remoteServerId) {
    	this.remoteServerId = remoteServerId;
    	this.type = type;
        id = counter.getAndIncrement();
    }
    

	public String getRemoteServerId() {
		return remoteServerId;
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

    public Object getEmptyPipeline() {
        return emptyPipeline;
    }

    public void setEmptyPipeline(Object emptyPipeline) {
        this.emptyPipeline = emptyPipeline;
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
		result = prime * result + ((remoteServerId == null) ? 0 : remoteServerId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		if (remoteServerId == null) {
			if (other.remoteServerId != null)
				return false;
		} else if (!remoteServerId.equals(other.remoteServerId))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

    @Override
	public String toString() {
		return "ConnectionInfo [connectionId=" + connectionId + ", port=" + port + ", address=" + address + ", tls="
				+ tls + ", cert=" + cert + ", remoteServerId=" + remoteServerId + ", type=" + type + ", clientCert="
				+ clientCert + ", id=" + id + ", client=" + client + ", readCount=" + readCount + ", processedCount="
				+ processedCount + "]";
	}
}
