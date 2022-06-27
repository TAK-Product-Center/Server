

package com.bbn.marti.nio.channel.connections;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.nio.util.ByteUtils;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.service.TransportCotEvent;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

/**
* A server socket channel for listening for Udp traffic on a fixed port. With each read
* event, this handler spins of a handler that encapsulates the data and the ip/port that
* it came from. 
*/
public class UdpServerChannelHandler extends AbstractBroadcastingChannelHandler {
	private static final Logger log = Logger.getLogger(UdpServerChannelHandler.class);
	private final static int READ_ALLOC = 20480;
	private Executor executor = null;
	private DatagramChannel channel;

	public final UdpServerChannelHandler withChannel(DatagramChannel channel) {
		Assertion.notNull(channel);
		Assertion.pre(channel.isOpen(), "Should only ever be instantiated with a connected channel");
		
		this.channel = channel;

        storeLocalAddress(this.channel);
		
		return this;
	}
	
	public final UdpServerChannelHandler withExecutor(Executor executor) {
		Assertion.notNull(executor);
	
		this.executor = executor;
		return this;
	}
    @Override
    public String netProtocolName() {
        return "udp";
    }

	/**
	* Handle IO event, spin off new handler for the data coming in
	*/
	@Override
	public boolean handleRead(SelectableChannel channel, Server server, final ByteBuffer buffer) {
        totalNumberOfReads.incrementAndGet();
	    try {
	        // update reads metric for this input
	        InputMetric metric = MessageConversionUtil.getInstance().getInputMetric(input);
	        metric.getReadsReceived().incrementAndGet();
	    } catch (Exception e) {
	        log.debug("exception writing metric", e);
	    }
	    
		boolean resubscribe = true;
        ((Buffer)buffer).clear()
            .limit(Math.min(buffer.capacity(), READ_ALLOC));

        InetSocketAddress clientAddress = null;
		try {
			// read data into buffer
			clientAddress = (InetSocketAddress) this.channel.receive(buffer);
        } catch (IOException e) {
            log.error("Exception encountered handling read IO for " + this, e);
            
            // I guess we want to read more -- should rebind, or something
            return true;
        }

        try {
            // flip the buffer for read processing
            ((Buffer)buffer).flip();
            if (clientAddress != null && buffer.remaining() > 0) {
                log.trace(this + " read " + buffer.remaining() + " bytes of data from the wire");
                totalBytesWritten.addAndGet(buffer.remaining());
                // copy data into local buffer
                ByteBuffer copy = ByteUtils.copy(buffer);
                
            	// build + submit IO processing job to executor
                Runnable dataCarryingRunnable = buildDataCarryingRunnable(copy, clientAddress, localPort());
                
            	executor.execute(dataCarryingRunnable);
            } else if (buffer.remaining() == 0) {
            	log.warn("handleRead receives spurious read call");
            } else {
            	// EOS? For UDP?
            	log.warn("Receives EOS for (udp) " + this + " -- unexpected outcome");
            }
        } catch (Exception e) {
            log.error("Error encountered handling io read for " + this, e);
        }
		
		return resubscribe;
	}

    private Runnable buildDataCarryingRunnable(
        final ByteBuffer data,
        final InetSocketAddress clientAddress, 
        final int localPort) 
    {
        return new Runnable() {
            public void run() {

                ConnectionInfo connectionInfo = new ConnectionInfo();
                try {
                    // If the input isn't streaming, the same connection information will be used for each message received, so set the connectionId to the inputId
                    if (input != null && !TransportCotEvent.isStreaming(input.getProtocol())) {
                        connectionInfo.setConnectionId(getInputId());
                    }
                    connectionInfo.setAddress(clientAddress.getAddress().toString());
                } catch (Exception e) {
                    log.debug("exception getting connection information", e);
                }


                // create channel handler for this packet arrival
                UdpDataChannelHandler clientHandler = (UdpDataChannelHandler) new UdpDataChannelHandler()
                        .withAddress(clientAddress)
                        .withLocalPort(UdpServerChannelHandler.this.localPort())
                        .withConnectionInfo(connectionInfo);

                if (input != null) {
                    clientHandler.withInput(input);
                    connectionInfo.setTls(TransportCotEvent.isTls(input.getProtocol()));
                }
                    
                // notify channel listener
                broadcastDataReceived(data, clientHandler);
            }        
        };
    }

    @Override
	public AsyncFuture<ChannelHandler> close() {
        Assertion.fail("Not yet implemented");
        return null;
	}

    @Override        
    public void forceClose() {
        Assertion.pre(this.channel != null, "Asked to close datagram server socket for " + this + ", but don't have a socket to close");
        
        log.warn(this + " received force close call -- shutting down the bound datagram listener");
        try {
            this.channel.close();
        } catch (IOException e) {
            log.warn("IO exception encountered close listening datagram socket for " + this, e);
        }
    }

    @Override
	public String toString() {
		return String.format("[UDP server :: local_port: %d stream_processor: %s]",
            localPort(),
            listener
        );
	}
}
