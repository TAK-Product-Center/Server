

package com.bbn.marti.nio.protocol;

import com.bbn.marti.nio.channel.ChannelHandler;

/**
* A factory for building a ChannelListener in response to the 
* creation of a new ChannelHandler.
*
* The returned listener is registered as the handler's listener
*/
public interface ChannelListenerInstantiator {
	public ChannelListener newInstance(ChannelHandler handler);
}