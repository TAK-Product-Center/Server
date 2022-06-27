

package com.bbn.marti.nio.listener;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.Protocol;
import com.google.common.collect.ComparisonChain;

/**
* A protocol listener that returns itself when new instance gets called
*
* Intended for clutter-free extensions for single static listeners that are
* used across the system to pipe data to different locations, and can handle
* multiple calls being entrant at a single time
*
*/
public abstract class AbstractAutoProtocolListener<T> implements ProtocolListener<T>, ProtocolListenerInstantiator<T>, Comparable<AbstractAutoProtocolListener<T>>, Serializable {
    private final static Logger log = Logger.getLogger(AbstractAutoProtocolListener.class);

	public void onConnect(ChannelHandler handler, Protocol<T> protocol) {
		;
	}

	public void onDataReceived(T data, ChannelHandler handler, Protocol<T> protocol) {
		;
	}

	public void onInboundClose(ChannelHandler handler, Protocol<T> protocol) {
		;
	}

	public void onOutboundClose(ChannelHandler handler, Protocol<T> protocol) {
		;
	}

	/**
	* Return ourselves on new instance
	*/
	public ProtocolListener<T> newInstance(ChannelHandler handler, Protocol<T> protocol) {
        log.trace(String.format(
            "Instantiating new protocol listener for %s -- handler: %s protocol: %s",
            this,
            handler,
            protocol
        ));

		return this;
	}

	@Override
	public int compareTo(AbstractAutoProtocolListener<T> o) {
		return ComparisonChain.start().compare(this.hashCode(), o.hashCode()).result();
	}

}