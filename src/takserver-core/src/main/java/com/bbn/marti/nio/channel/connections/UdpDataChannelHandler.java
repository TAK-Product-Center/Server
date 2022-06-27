

package com.bbn.marti.nio.channel.connections;

import java.net.InetSocketAddress;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

import org.jetbrains.annotations.Nullable;

/**
* A channel handler for representing the receipt of a single datagram -- the source address/port are recorded internally. 
*
* This handler cannot be connected or closed, and does not subscribe to any IO Events.
*/
public class UdpDataChannelHandler extends AbstractBroadcastingChannelHandler {
	public final UdpDataChannelHandler withAddress(InetSocketAddress address) {
		Assertion.notNull(address);
        
        storeRemoteAddress(address);

		return this;
	}
	
	public final UdpDataChannelHandler withLocalPort(int localPort) {
		Assertion.pre(MessageConversionUtil.isValidPort(localPort));
        
        super.localPort = localPort;
        
		return this;
	}

	@Override
	public String netProtocolName() {
		return "udp";
	}

    @Override
	public String toString() {
		return String.format("[UDP server datagram :: remote_addr: %s:%d local_port: %d stream_processor: %s]",
            host(),
            port(),
            localPort(),
            listener);
	}

    @Override
	public AsyncFuture<ChannelHandler> close() {
		Assertion.fail("Close not applicable for " + this);
		return null;
	}

    @Override
    public void forceClose() {
        Assertion.fail("Not yet implemented");
    }
}
