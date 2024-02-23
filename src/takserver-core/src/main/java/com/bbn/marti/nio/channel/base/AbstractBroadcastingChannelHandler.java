

package com.bbn.marti.nio.channel.base;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.bbn.marti.config.Input;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.ChannelListener;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.nio.util.NetUtils;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.google.common.base.Strings;

/**
* Abstract Channel Handler for delegating all of the IO operations to an extending class, while
* maintaining a default assertion fail for unexpected IO ops
*
*/
public abstract class AbstractBroadcastingChannelHandler implements ChannelHandler {
	private final static Logger log = Logger.getLogger(AbstractBroadcastingChannelHandler.class);
	public static AtomicLong totalBytesWritten = new AtomicLong();
    public static AtomicLong totalBytesRead = new AtomicLong();
    public static AtomicLong totalNumberOfWrites = new AtomicLong();
    public static AtomicLong totalNumberOfReads = new AtomicLong();
    
	private Server server = null;
	protected ChannelListener listener = null;

    protected InetAddress remoteAddress = null;
    protected int remotePort = -1;
    protected int localPort = -1;

    protected boolean propagateConnectionIdToChildren = false;
    protected String connectionId;
    
    protected String handlerType = "";
    
    protected ConnectionInfo connectionInfo;
    
    protected Input input;
    private String inputId;
    
    public Input getInput() {
        return input;
    }

    public final String getInputId() {
        if (inputId == null && input != null) {
            inputId = MessageConversionUtil.getConnectionId(input);
        }
        return inputId;
    }

    public final ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }
    
    public final String getHandlerType() {
        return handlerType;
    }
    
    public final AbstractBroadcastingChannelHandler withHandlerType(String handlerType) {
        this.handlerType = handlerType;
        return this;
    }

    public final AbstractBroadcastingChannelHandler withServer(Server server) {
		Assertion.notNull(server);

		this.server = server;
		return this;
	}


	public final AbstractBroadcastingChannelHandler withConnectionInfo(ConnectionInfo connectionInfo) {

	    if (connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId())) {
	        throw new IllegalArgumentException("invalid connectionInfo: " + connectionInfo);
	    }

	    this.connectionInfo = connectionInfo;

        return this;
    }


    public final void propagateConnectionIdToChildren(boolean value) {
        propagateConnectionIdToChildren = value;
    }
    
    @Override
	public final void listener(ChannelListener listener) {
		Assertion.notNull(listener, "Given listener must be nonnull");

		this.listener = listener;
	}
	
	protected final Server server() {
		Assertion.notNull(server);
		return this.server;
	}
	
    @Override
	public final ChannelListener listener() {
		return this.listener;
	}
	
	// server side calls -- called when we get a selection key hit for one of these interest ops
	// defaults here are implemented as failed assertions - extenders need only override ones they want to succeed/subscribe to
	@Override
    public boolean handleRead(SelectableChannel channel, Server server, ByteBuffer buffer) {
		Assertion.fail("Method not applicable for " + this);
		return false;
	}
	
	@Override
    public boolean handleWrite(SelectableChannel channel, Server server, ByteBuffer buffer) {
		Assertion.fail("Method not applicable for " + this);
		return false;
	}
	
	@Override
    public boolean handleConnect(SelectableChannel channel, Server server) {
		Assertion.fail("Method not applicable for " + this);
		return false;
	}
	
	@Override
    public boolean handleAccept(SelectableChannel channel, Server server) {
		Assertion.fail("Method not applicable for " + this);
		return false;
	}
	
	@Override
    public AsyncFuture<Integer> write(ByteBuffer buffer) {
		Assertion.fail("Method not applicable for " + this);
		return null;
	}

	@Override
    public AsyncFuture<ChannelHandler> connect() {
		Assertion.fail("Method not applicable for " + this);
		return null;
	}

    @Override
    public InetAddress host() {
        return remoteAddress;
    }
    
	@Override
    public int port() {
        return remotePort;
    }
    
	@Override
    public final int localPort() {
        return localPort;
    }

    @Nullable
    public final String getConnectionId() {
        if (connectionInfo != null) {
            return connectionInfo.getConnectionId();
        }
        return null;
    }
    
    public void storeLocalPort(int localport) {
        this.localPort = localport; 
    }
    
    protected final void storeAddresses(InetSocketAddress remote, NetworkChannel channel) {
        storeRemoteAddress(remote);
        storeLocalAddress(channel);
    }

    protected final void storeRemoteAddress(InetSocketAddress remote) {
        if (remote != null) {
            this.remoteAddress = remote.getAddress();
            this.remotePort = remote.getPort();
        }        
    }

    protected final void storeLocalAddress(NetworkChannel channel) {
        this.localPort = NetUtils.localPortOrNegativeOne(channel);
    }

    protected final void broadcastConnectComplete() {
        if (this.listener != null) {
        	if (log.isTraceEnabled()) {          		
        		log.trace(this + " notifying channel listener of connect complete");
        	}
            try {
                this.listener.onConnect(this);
            } catch (RuntimeException e) {
                log.error(this + " encountered exception notifying listener of connect event -- shutting down the connection", e);
                this.forceClose();
            }
        }
    }
    protected final void broadcastDataReceived(ByteBuffer buffer) {
        broadcastDataReceived(buffer, this);
    }

    protected final void broadcastDataReceived(ByteBuffer buffer, ChannelHandler handler) {
        if (this.listener != null) {
        	if (log.isTraceEnabled()) {        		
        		log.trace(this + " notifying channel listener of data received (" + buffer.remaining() + " bytes)");
        	}
            try {
                this.listener.onDataReceived(buffer, handler);
            } catch (RuntimeException e) {
                log.error(handler + " encountered exception notifying listener of data received event -- shutting down the connection", e);
                handler.forceClose();
            }
        }
    }

    protected final void broadcastInboundCloseComplete() {
        if (this.listener != null) {
        	if (log.isTraceEnabled()) {            		
        		log.trace(this + " notifying channel listener of inbound close complete");
        	}
            try {
                this.listener.onInboundClose(this);
            } catch (RuntimeException e) {
                log.error(this + " encountered exception notifying listener of EOS event -- shutting down the connection", e);
                this.forceClose();
            }
        }
    }
    
    protected final void broadcastOutboundCloseComplete() {
        if (this.listener != null) {
        	if (log.isTraceEnabled()) {            		
        		log.trace(this + " notifying channel listener of outbound close complete");
        	}
            try {
                this.listener.onOutboundClose(this);
            } catch (RuntimeException e) {
                log.error(this + " encountered exception notifying listener of close event -- shutting down the connection", e);
                this.forceClose();
            }
        }
    }

    public ChannelHandler withInput(Input input) {
        
        if (input == null) {
            throw new IllegalArgumentException("null input");
        }
        
        if (log.isTraceEnabled()) {
        	log.trace("set input " + input.getName() + " for channel handler " + this.getClass().getName() + " " + this + " " + this.hashCode());
        }
        
        this.input = input;
        
        return this;
    }

    @Override
    public boolean isMatchingInput(@NotNull Input input) {
        return (this.input == input);
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((connectionId == null) ? 0 : connectionId.hashCode());
		result = prime * result + ((connectionInfo == null) ? 0 : connectionInfo.hashCode());
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
		AbstractBroadcastingChannelHandler other = (AbstractBroadcastingChannelHandler) obj;
		if (connectionId == null) {
			if (other.connectionId != null)
				return false;
		} else if (!connectionId.equals(other.connectionId))
			return false;
		if (connectionInfo == null) {
			if (other.connectionInfo != null)
				return false;
		} else if (!connectionInfo.equals(other.connectionInfo))
			return false;
		return true;
	}
	
	@Override
	public String identityHash() {
		return Integer.valueOf(System.identityHashCode(this)).toString();
	}
    
    
}
