

package com.bbn.marti.nio.channel.clients;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.StreamInstantiator;
import com.bbn.marti.nio.server.Server;

/**
* A factory for instantiating client handlers
*/
public interface ClientFactory {
    public ChannelHandler openClientChannel(
        String iface,
        InetSocketAddress address, 
        Server server,
        StreamInstantiator streamInstantiator) throws IOException;
}