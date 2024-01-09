

package com.bbn.marti.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import com.bbn.marti.nio.protocol.connections.SingleCotProtocol;
import com.bbn.marti.nio.protocol.connections.SingleProtobufOrCotProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.nio.binder.BinderFactory;
import com.bbn.marti.nio.binder.ServerBinder;
import com.bbn.marti.nio.binder.impls.MulticastServerBinder;
import com.bbn.marti.nio.binder.impls.UdpServerBinder;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.clients.ClientFactory;
import com.bbn.marti.nio.channel.clients.UdpClientChannelHandler;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.listener.ProtocolListenerInstantiator;
import com.bbn.marti.nio.protocol.DecoratedStreamInstantiator;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.StreamInstantiator;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.Tuple;
import com.bbn.marti.util.concurrent.executor.OrderedExecutor;

import tak.server.cot.CotEventContainer;
/**
* A set of per-TransportCotEvent utilities and factories for creating server binders, and opening client channels with default server/client protocols.
*/
public enum TransportCotEvent {
	TCP   ("tcp"),

	STCP  ("stcp"),

	TLS   ("cottls"),

	SSL   ("ssl"),

	UDP   ("udp",
        UdpServerBinder.udpBinderFactory, UdpClientChannelHandler.udpClientFactory,
        Resources.udpProcessor, Resources.udpProcessor, // UDP doesnt' have a pipeline executor
        SingleCotProtocol.singleCotInstantiator, SingleCotProtocol.singleCotInstantiator),

	MUDP  ("cotmcast",
        MulticastServerBinder.mudpBinderFactory, UdpClientChannelHandler.udpClientFactory,
        Resources.udpProcessor, Resources.udpProcessor, // UDP doesn't have a pipeline executor
        SingleCotProtocol.singleCotInstantiator, SingleCotProtocol.singleCotInstantiator),

	COTPROTOMUDP  ("mcast",
		MulticastServerBinder.mudpBinderFactory, UdpClientChannelHandler.udpClientFactory,
		Resources.udpProcessor, Resources.udpProcessor, // UDP doesn't have a pipeline executor
		SingleProtobufOrCotProtocol.singleProtobufOrCotInstantiator, SingleProtobufOrCotProtocol.singleProtobufOrCotInstantiator),

	PROTOTLS("prototls"),

	COTPROTOTLS("tls");


	public final String configID; // the "street" name of the transport type, in CoreConfig speak
	final BinderFactory binderFactory; // the default factory for building a binder of this type
	final ClientFactory clientFactory; // the default factory for building a client of this type

	final ProtocolInstantiator<CotEventContainer> serverCotEncodingFactory; // the default factory for building a server CoT protocol for this type
	final ProtocolInstantiator<CotEventContainer> clientCotEncodingFactory; // the default factory for building a client CoT protocol for this type

    final OrderedExecutor serverExecutor;
    final OrderedExecutor connectionExecutor;

	private static final Logger logger = LoggerFactory.getLogger(TransportCotEvent.class);

	TransportCotEvent(String configID) {
		this.configID = configID;

        this.binderFactory = null;
        this.clientFactory = null;
		this.serverCotEncodingFactory = null;
		this.clientCotEncodingFactory = null;
        this.serverExecutor = null;
        this.connectionExecutor = null;
	}
	
	TransportCotEvent(
            String configID,
            BinderFactory binderFactory,
            ClientFactory clientFactory,
            OrderedExecutor serverExecutor,
            OrderedExecutor connectionExecutor,
            ProtocolInstantiator<CotEventContainer> serverCotEncodingFactory,
            ProtocolInstantiator<CotEventContainer> clientCotEncodingFactory)
    {
		this.configID = configID;

        this.binderFactory = binderFactory;
        this.clientFactory = clientFactory;

		this.serverCotEncodingFactory = serverCotEncodingFactory;
		this.clientCotEncodingFactory = clientCotEncodingFactory;

        this.serverExecutor = serverExecutor;
        this.connectionExecutor = connectionExecutor;
  }

	public static TransportCotEvent findByID(String protocolString) {
		for (TransportCotEvent transport : TransportCotEvent.values()) {
			if (protocolString.equalsIgnoreCase(transport.id())) {
				return transport;
			}
		}

		return null;
	}

	public static boolean isTls(String protocolString) {
	    if (protocolString.equalsIgnoreCase(TransportCotEvent.SSL.id())
				|| protocolString.equalsIgnoreCase(TransportCotEvent.TLS.id())
				|| protocolString.equalsIgnoreCase(TransportCotEvent.PROTOTLS.id())
				|| protocolString.equalsIgnoreCase(TransportCotEvent.COTPROTOTLS.id())) {
	        return true;
	    }

	    return false;
	}

	public static boolean isStreaming(String protocolString) {

	    // if it's ssl, it's streaming
	    if (isTls(protocolString)) {
	        return true;
	    }

        if (protocolString.equalsIgnoreCase(TransportCotEvent.STCP.id())) {
            return true;
        }
        
        return false;
    }

	public String id() {
		return this.configID;
	}

    public ServerBinder binder(
        int localPort,
		LinkedBlockingQueue<ProtocolListenerInstantiator<CotEventContainer>> listenerInstantiators,
        List<CodecSource> codecSources,
        List<NetworkInterface> interfs,
        InetAddress group) 
    {
        
        logger.debug("codecSources: " + codecSources);
        
        return binder(
            localPort,
            new DecoratedStreamInstantiator<CotEventContainer>()
                .withProtocolInstantiator(serverCotEncodingFactory)
                .withProtocolListenerInstantiators(listenerInstantiators),
            codecSources,
            interfs,
            group);
    }

    /**
	* Creates a binder for this transport type -- the binder 
	* will produce a server listener (either socket or data oriented)
	* when called, later. The binder will bind the server listener
	* to the given port. For non Multicast types, the network interface
	* and the inet address group are ignored.
	*/
	public ServerBinder binder(
		int localPort,
		StreamInstantiator instantiator,
        List<CodecSource> codecSources,
		List<NetworkInterface> interfs,
		InetAddress group) 
	{
	    
	    return this.binderFactory.instance(
			localPort,
            serverExecutor,
			instantiator, 
			interfs,
			group
		);
	}
    
    public Tuple<ChannelHandler,Protocol<CotEventContainer>> client(
        InetAddress remoteAddress,
        int remotePort,
        Server server,
        List<CodecSource> codecSources) throws IOException
    {
        return client(
            null,
            remoteAddress,
            remotePort,
            server,
            codecSources);
    }

	public Tuple<ChannelHandler,Protocol<CotEventContainer>> client(
			String iface,
			InetAddress remoteAddress,
			int remotePort,
			Server server,
			List<CodecSource> codecSources) throws IOException
	{
		return client(
				iface,
				remoteAddress,
				remotePort,
				server,
				codecSources,
				clientCotEncodingFactory,
				new LinkedBlockingQueue<ProtocolListenerInstantiator<CotEventContainer>>());
	}

	/**
    * Opens a client channel of this transport type (see clientHandler),
    * instantiates and attaches the returned Protocol to the ChannelHandler,
    * and instantiates and attaches a list of ProtocolListeners to the Protocol
    *
    * Connects after the full instantiation/chain is built
    */
    public <T> Tuple<ChannelHandler,Protocol<T>> client(
    	String iface,
        InetAddress remoteAddress,
        int remotePort,
        Server server,
        List<CodecSource> codecSources,
        ProtocolInstantiator<T> protocolInstantiator,
		LinkedBlockingQueue<ProtocolListenerInstantiator<T>> listenerInstantiators) throws IOException
    {
        Assertion.areNotNull(remoteAddress, server, codecSources, protocolInstantiator, listenerInstantiators);
    
        // build a stream instantiator
        DecoratedStreamInstantiator<T> streamInstantiator = new DecoratedStreamInstantiator()
            .withProtocolInstantiator(protocolInstantiator)
            .withProtocolListenerInstantiators(listenerInstantiators);
    
        // build le client handler
        ChannelHandler clientHandler = makeClientHandler(
			iface,
            new InetSocketAddress(remoteAddress, remotePort),
            server,
            codecSources,
            streamInstantiator);
        
        // build+attach the protocol
        Protocol<T> protocol = DecoratedStreamInstantiator.instantiateProtocol(clientHandler, protocolInstantiator);

        // build the listeners, attach the listeners
        List<ProtocolListener<T>> protocolListeners = DecoratedStreamInstantiator.instantiateListeners(clientHandler, protocol, listenerInstantiators);
        DecoratedStreamInstantiator.attachListeners(protocol, protocolListeners);
        
        // do connect
        clientHandler.connect();
        
        return Tuple.create(clientHandler, protocol);
    }

    /**
    * Simple construction
    */
    private ChannelHandler makeClientHandler(
		String iface,
        InetSocketAddress address, 
        Server server,
        List<CodecSource> codecSources,
        StreamInstantiator streamInstantiator) throws IOException 
    {
        Assertion.areNotNull(address, server, codecSources, streamInstantiator);
        
        return clientFactory.openClientChannel(
			iface,
            address, 
            server,
            streamInstantiator);
    }    
}
