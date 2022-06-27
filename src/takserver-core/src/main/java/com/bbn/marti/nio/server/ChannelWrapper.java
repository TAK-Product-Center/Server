

package com.bbn.marti.nio.server;

import java.nio.channels.SelectableChannel;
import java.util.EnumSet;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.util.IOEvent;
import com.bbn.marti.util.Assertion;

/**
* A simple wrapper for returning a channel/channel handler/selection interest for registration with a NioServer
*/
public class ChannelWrapper {
	private SelectableChannel channel;
	private ChannelHandler handler;
	private int interest = 0;

	public ChannelWrapper withChannel(SelectableChannel channel) {
        Assertion.notNull(channel);
        
		this.channel = channel;
		return this;
	}
	
	public ChannelWrapper withHandler(ChannelHandler handler) {
        Assertion.notNull(handler);
        
		this.handler = handler;
		return this;
	}
	
	public ChannelWrapper withInterest(EnumSet<IOEvent> interest) {
        Assertion.notNull(interest);
        
		this.interest = IOEvent.generateFlags(interest);
		return this;
	}
    
    public ChannelWrapper withInterest(IOEvent interest) {
        Assertion.notNull(interest);
        
        this.interest = interest.flag();
        return this;
    }
	
	public SelectableChannel channel() { return channel; }
	public ChannelHandler handler() { return handler; }
	public int interest() { return interest; }
}