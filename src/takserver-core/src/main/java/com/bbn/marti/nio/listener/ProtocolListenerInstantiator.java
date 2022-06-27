

package com.bbn.marti.nio.listener;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.Protocol;

/**
* An interface for a factory that produces a protocol 
* listener, in response to newInstance being called
* with a ChannelHandler/Protocol<T> pair
*
* @note If the factory does not wish to register a ProtocolListener<T>
* for the given Handler/Protocol<T>, it may return null
*/
public interface ProtocolListenerInstantiator<T> {
	public ProtocolListener<T> newInstance(ChannelHandler handler, Protocol<T> protocol);
}