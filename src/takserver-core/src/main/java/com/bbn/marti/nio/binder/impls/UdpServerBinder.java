

package com.bbn.marti.nio.binder.impls;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.List;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.binder.BinderFactory;
import com.bbn.marti.nio.binder.ServerBinder;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.connections.UdpServerChannelHandler;
import com.bbn.marti.nio.protocol.StreamInstantiator;
import com.bbn.marti.nio.server.ChannelWrapper;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.nio.util.IOEvent;
import com.bbn.marti.nio.util.NetUtils;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;

/**
* A binder for opening and binding a udp server datagram channel to a given port.
*
* The given listeners are copied into the server handler, and the channel is initially registered
* for OP_READs.
*/
public class UdpServerBinder extends AbstractServerBinder {
	private final static Logger log = Logger.getLogger(UdpServerBinder.class);

    /**
    * Returns a UDP data listener. Will bind to the given port, and push any accepted data into a channel handler that 
    * has no active connection, but contains a record of where the traffic came from (the source ip/port that were
    * given when the packet was unloaded). 
    *
    * @note the network interface and the inet address group are ignored
    */      
    public final static BinderFactory udpBinderFactory = new BinderFactory() {
        public ServerBinder instance(
            int port,
            OrderedExecutor boundExecutor,
            StreamInstantiator streamInstantiator, 
            List<NetworkInterface> interfs,
            InetAddress group) 
        {
            return new UdpServerBinder()
                .withPort(port)
                .withInstantiator(streamInstantiator)
                .withBoundExecutor(boundExecutor);
        }
    };

    protected DatagramChannel doBind() throws IOException {
        // instantiate datagram server channel
        DatagramChannel serverChannel = null;
        
        try {
            // Force IPV4 (INET) since it is left up to the platform otherwise
            serverChannel = DatagramChannel.open(StandardProtocolFamily.INET);

            serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
            serverChannel.configureBlocking(false);
            serverChannel.bind(bindAddress());
        } catch (IOException e) {
            // drop resources on error
            NetUtils.guardedClose(serverChannel);
            
            // throw error at the binder
            throw e;
        }

        return serverChannel;
    }

    @Override
	public ChannelWrapper handleBind(Server server) throws IOException {
		Assertion.notNull(server);

        DatagramChannel serverChannel = doBind();
		
        if (serverChannel != null) {
            // build handler for server datagram channel
            ChannelHandler serverHandler = new UdpServerChannelHandler()
            	.withChannel(serverChannel)
                .withExecutor(boundExecutor())
            	.withServer(server); // one COULD put an ordered view here, that would limit inputs from a specific udp input
            
            // build chain around handler
            instantiator().instantiate(serverHandler);
            
            // wrap and register with the server
            return new ChannelWrapper()
            	.withChannel(serverChannel)
            	.withHandler(serverHandler)
            	.withInterest(IOEvent.READ);
        } else {
            return null;
        }
	}
    
	@Override
    public String toString() {
        return new String("UDP local port: " + port());
//        return String.format(
//            "[Udp server instantiator :: local_port: %d %s]",
//            port(),
//            pipelineAndStreamString()
//        );
    }
}
