

package com.bbn.marti.nio.protocol;

import com.bbn.marti.nio.channel.ChannelHandler;

public interface StreamInstantiator {
	public void instantiate(ChannelHandler handler);
}