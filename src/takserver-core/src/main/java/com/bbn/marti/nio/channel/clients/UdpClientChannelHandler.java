

package com.bbn.marti.nio.channel.clients;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.protocol.StreamInstantiator;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.nio.util.DatagramState;
import com.bbn.marti.nio.util.IOEvent;
import com.bbn.marti.nio.util.NetUtils;
import com.bbn.marti.nio.util.WriteData;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.Transitions;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;
import com.bbn.marti.util.concurrent.future.SettableAsyncFuture;

/**
* A channel handler that is bound to a specific remote inet address/port, and writes
* data only to that remote destination.
*
* Does not receive reads, and immediately rejects writes that are larger than
* WRITE_ALLOC.
*
* Should not be modified to point to a different address
*/
public class UdpClientChannelHandler extends AbstractBroadcastingChannelHandler {
	private final static Logger log = Logger.getLogger(UdpClientChannelHandler.class);
    
    /**
    * A static factory that will open an outgoing udp channel, and encapsulate it in a 
    * udp client channel handler.
    */
    public final static ClientFactory udpClientFactory = new ClientFactory() {
        public ChannelHandler openClientChannel(
            String iface,
            InetSocketAddress address, 
            Server server,
            StreamInstantiator streamInstantiator) throws IOException 
        {
            ConnectionInfo connection = new ConnectionInfo();
            
            // for client udp channel, assign a random id to itself
            connection.setConnectionId(UUID.randomUUID().toString().replace("-", ""));
            
            ChannelHandler clientHandler = new UdpClientChannelHandler()
                .withAddress(address)
                .withIface(iface)
                .withStreamInstantiator(streamInstantiator)
                .withServer(server)
                .withConnectionInfo(connection);
            
            return clientHandler;
        }
    };    
    
	private final static int WRITE_ALLOC = 20480;
    private final static IllegalStateException envelopeFailureException = new IllegalStateException("Couldn't write entire envelope out to the network in one write -- discarding");

    private final AtomicReference<DatagramState> state; // state of the state
    private final Queue<WriteData> writes; // pending application -> network writes

    private InetSocketAddress remoteSockAddress;
    private String iface;

	private DatagramChannel channel;
    private SettableAsyncFuture<ChannelHandler> connectFuture;
    private SettableAsyncFuture<ChannelHandler> closeFuture;

    private StreamInstantiator streamInstantiator;

	public UdpClientChannelHandler() {
		super();

		this.state = new AtomicReference<DatagramState>(DatagramState.INIT);                
        this.writes = new ConcurrentLinkedQueue<WriteData>();
        
        refreshFutures();
	}
    
    private void refreshFutures() {
        this.connectFuture = SettableAsyncFuture.create();
        this.closeFuture = SettableAsyncFuture.create();
    }
	
	public final UdpClientChannelHandler withAddress(InetSocketAddress remoteSockAddress) {
		Assertion.notNull(remoteSockAddress);

        // store remote addr/port into super
        super.remoteAddress = remoteSockAddress.getAddress();
        super.remotePort = remoteSockAddress.getPort();
        
        // save sock addr
        this.remoteSockAddress = remoteSockAddress;
        
		return this;
	}

	private final UdpClientChannelHandler withIface(String iface) {
	    this.iface = iface;
	    return this;
    }

    public final UdpClientChannelHandler withStreamInstantiator(StreamInstantiator instantiator) {
        Assertion.notNull(instantiator);
        
        this.streamInstantiator = instantiator;
        return this;
    }

    @Override
    public AsyncFuture<ChannelHandler> connect() {
        // not open -- contend for INIT/CLOSED -> OPEN transition
        switch (Transitions.doWhileSetTransition(state, DatagramState.OPEN, DatagramState.connectReceiveStates)) {
            case INIT:
            case CLOSED:
                doConnect();
                break;
            case OPEN:
                // will return already set connect future
                break;
            case CLOSING:
                log.warn("Called connect on " + this + " but handler is currently closing");
                return AsyncFutures.immediateFailedFuture(new IllegalStateException("Called connect on closing channel"));
            default:
                Assertion.fail("not yet implemented");
        }
            
        return this.connectFuture;
    }


    private void doConnect() {
        // refresh futures
        refreshFutures();
        
        try {
            // open channel
            this.channel = NetUtils.openUdpChannel(this.remoteSockAddress, this.iface);
        } catch (IOException e) {
            log.warn("Exception opening connection for " + this, e);
            
            // set state to closed
            this.state.set(DatagramState.CLOSED);
            
            // set the connect future to be exceptional
            this.connectFuture.setException(e);

            return;
        }
        
        // register the handler
        server().registerChannel(this.channel, this, IOEvent.NONE);
        
        // notify listeners of connect
        broadcastConnectComplete();
    }

    @Override
    public boolean handleWrite(SelectableChannel channel, Server server, ByteBuffer buffer) {
        Assertion.areNotNull(channel, server, buffer);
        Assertion.pre(DatagramState.writeHandleStates.contains(state.get()));
        totalNumberOfWrites.getAndIncrement();

        boolean resubscribe = false;    
        final WriteData toWrite = writes.poll();
        if (toWrite != null) {
            Assertion.pre(toWrite.data.hasRemaining(), "Write at the head of the queue should always be nonempty");
            final ByteBuffer data = toWrite.data;
            int bytesWritten = -1;
            
            // do IO on the channel
            try {
                bytesWritten = this.channel.write(data);
                totalBytesWritten.addAndGet(bytesWritten > 0 ? bytesWritten : 0);
            } catch (PortUnreachableException pue) { // not to be confused with the datacenter metric
                log.warn("Port unreachable exception -- giddily ignoring", pue);
            } catch (IOException e) {
                log.error("Exception encountered handling write IO for " + this + " -- shutting down the connection", e);
                
                // do force close
                forceClose();

                // break out
                return false;
            }
            
            log.trace(this + " wrote " + bytesWritten + " bytes of data to the wire");
            
            // Successfully did IO -- process results
            // main difference is that we only allow 
            try {
                if (data.hasRemaining()) {
                    // didn't write entire envelope out
                    log.warn(String.format(
                        "%s couldn't write entire packet out to the wire (was %d bytes, wrote %d bytes) -- discarding", 
                        this, 
                        toWrite.originalLength, 
                        bytesWritten
                    ));
                    
                    toWrite.setException(this.envelopeFailureException);
                } else {
                    toWrite.setFuture();
                }

                resubscribe = ( writes.peek() != null);
                
                // TODO: offload check onto separate executor
                if (!resubscribe && this.state.get() == DatagramState.CLOSING) {
                    // no more data on the queue (resubscribe == false) and we have a pending close
                    tryClosingTransition();
                }
            } catch (Exception e) {
                log.error("Exception encountered handling write IO data for " + this + " -- shutting down the connection", e);
                forceClose();
            }
        } else {
            log.warn(this + " received spurious handle io write call (not necessarily a problem)");
        }

        return resubscribe;        
    }

    @Override
	public AsyncFuture<Integer> write(ByteBuffer buffer) {
        Assertion.pre(buffer != null, "Buffer must be nonnull");
        Assertion.pre(buffer.hasRemaining(), "Tried to write empty data");
		Assertion.pre(buffer.remaining() <= WRITE_ALLOC, "Message sent must be less than configured UDP size limit");
		
		WriteData writeData = new WriteData(buffer, SettableAsyncFuture.<Integer>create());

        boolean accepted = writes.offer(writeData);
        if (accepted) {
            WriteData head = writes.peek();
            if (head != null) {
                server().addInterestOp((SelectableChannel) this.channel, IOEvent.WRITE);
            }
        } else {
			writeData.future.setException(new IllegalStateException("Couldn't enqueue write event for " + this + " -- not enough room in the outbound queue"));
		}
		
		return writeData.future;
	}

    @Override    	
	public AsyncFuture<ChannelHandler> close() {
		log.debug("Closing connection for: " + this);

        if (state.compareAndSet(DatagramState.OPEN, DatagramState.CLOSING)) {
            // notify listeners of outbound traffic end
            broadcastOutboundCloseComplete();
            
            scheduleClose();
        } else {
            switch (state.get()) {
                case INIT:
                    Assertion.fail("Should never close an init'ed channel");
                    break;
                case OPEN:
                    log.warn("Call race to close " + this + " with a connect call");
                    break;
                case CLOSING:
                case CLOSED:
                    log.warn("Called close on an already closing/closed channel: " + this);
                    break;
                default:
            }
        }
        
		return closeFuture;
	}

    @Override
    public void forceClose() {
        log.debug("Force closing connection for: " + this);
        
        DatagramState contention = Transitions.doUntilSetTransition(state, DatagramState.CLOSED, DatagramState.forceClosedStates);

        switch (contention) {
            case INIT:
                log.warn("Trying to call force close on INIT'ed channel for " + this);
                
                // release IO resources
                NetUtils.guardedClose(this.channel);

                // set future
                closeFuture.setResult((ChannelHandler) this);
                
                break;
            case OPEN:
                log.trace(this + " doing OPEN -> CLOSED transition (force close)");
            
                // was open -- notify the listeners
                broadcastOutboundCloseComplete();
            case CLOSING:
                log.trace(this + " doing CLOSING -> CLOSED transition (force close)");
            
                // closing in progress -- clear the writes and close the channel
                NetUtils.guardedClose(this.channel);
                
                // clear writes
                writes.clear();

                // set future
                closeFuture.setResult((ChannelHandler) this);
                
                break;
            case CLOSED:
                log.warn("Race to close (probably not a problem) for " + this);
                break;
            default:
                Assertion.fail("Not yet implemented");
        }
    }

    private void scheduleClose() {
        if (writes.isEmpty()) {
            tryClosingTransition();
        } else {
            log.trace("schedule close for " + this + " finding writes in progress -- close will finish at tail end of writes");
        }
    }

    private void tryClosingTransition() {
        if (state.compareAndSet(DatagramState.CLOSING, DatagramState.CLOSED)) {
            log.debug("Finishing close for: " + this);
        
            // do IO close
            NetUtils.guardedClose(this.channel);
            
            // set future
            closeFuture.setResult((ChannelHandler) this);
        } else {
            log.debug("Pending close finding writes complete, but channel already fully closed");
        }
    }

    @Override
    public String netProtocolName() {
        return "udp";
    }

    @Override    	
    public String toString() {
        return new String("UDP sender to: " + host() + ":" + port());
//		return String.format("[UDP client datagram :: remote_addr: %s:%d local_port: %d stream_processor: %s]",
//            host(),
//            port(),
//            localPort(),
//            listener());
	}
}
