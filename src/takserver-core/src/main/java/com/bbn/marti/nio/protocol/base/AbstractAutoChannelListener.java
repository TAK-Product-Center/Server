

package com.bbn.marti.nio.protocol.base;

import java.nio.ByteBuffer;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.ChannelListener;

/**
* An abstract channel listener that does nothing on notification receipt, and returns itself on new instance.
*
* Intended for clutter-free, static extenders who wish to override one or two methods
*/
public class AbstractAutoChannelListener implements ChannelListener { 
	public void onConnect(ChannelHandler handler) {
		;
	}

	public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
		;
	}

	public void onInboundClose(ChannelHandler handler) {
		;
	}

	public void onOutboundClose(ChannelHandler handler) {
		;
	}
}