

package com.bbn.marti.nio.protocol;

import com.bbn.marti.nio.channel.ChannelHandler;

public interface ProtocolInstantiator<T> extends ChannelListenerInstantiator {
	public Protocol<T> newInstance(ChannelHandler handler);
}