

package com.bbn.marti.nio.protocol;

import java.nio.ByteBuffer;

import com.bbn.marti.nio.channel.ChannelHandler;

/**
* An interface for a listener receiving channel events. 
* 
* This interface is presented to the channel handler as its outgoing listener.
*
* A typed subinterface, Protocol, is used as a decorator for providing 
* encoding/decoding facilities to transform a byte stream to its generic 
* type. Data coming from the network is decoded to that type, and then 
* passed to protocol listeners of the same or super type. Data coming 
* from the application is of the given type, and is transformed to 
* a byte stream.
*
* As far as reentrancy goes, a channel listener implementation is guaranteed that:
* -- only one thread is servicing an on_ call at a time.
*
* A channel listener implementation must guarantee that:
* -- all on_ calls are synchronous.
*
* The onInboundClose call signals that the channelListener will no longer receive
* network -> application data
*
* The onOutboundClose call signals that the channel handler can no longer receive
* application -> network traffic.
*
* A channel listener can expect that all onDataReceived calls, if any, come after
* an onConnect call, and that no further onDataReceived calls will be received
* after an onInboundClose call. 
*
* There are no ordering constraints on onInbound/OutboundClose calls.
*
*/
public interface ChannelListener {
	/**
	* Called when the handler has finished the connection process
	*
	* Signals that the listener may now receive onDataReceived calls,
	* and can now write data to the handler.
	*/
	public void onConnect(ChannelHandler handler);

	/**
	* Called when data is received from the network
	*/
	public void onDataReceived(ByteBuffer buffer, ChannelHandler handler);

	/**
	* Called to mark the EOS for network -> application traffic
	*/
	public void onInboundClose(ChannelHandler handler);	
	
	/**
	* Called to mark the end of application -> network traffic. Further
	* attempts to write to the handler may throw an exception.
	*/
	public void onOutboundClose(ChannelHandler handler);
}