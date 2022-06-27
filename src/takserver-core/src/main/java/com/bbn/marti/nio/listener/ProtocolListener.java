

package com.bbn.marti.nio.listener;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.Protocol;

/**
 * Callback interface that, once implemented, describes how to handle data from a transport.
 * @param <T> The type of data being received
 */
public interface ProtocolListener<T> {
	/**
	 * Called when the handler first connects
	 * @param handler The source channel handler
	 * @param protocol The source datatype parser/deparser
	 */
	public void onConnect(ChannelHandler handler, Protocol<T> protocol);
	
	/**
	 * Called when data is received by transport
	 * @param data The data received
	 * @param transport The transport over which data was received
	 * @param protocol The protocol over which data was received
	 */
	public void onDataReceived(T data, ChannelHandler handler, Protocol<T> protocol);
	
	/**
	 * Called with the handler is closed
	 *
	 * onInboundClose marks the end of the read stream -- once this is called,
	 * we can expect no more data from the given handler. 
	 *
	 * onOutboundClose signals that the protocol/handler will no longer 
	 * accept outbound data. Writing to the protocol after this call has been
	 * received may produce a runtime exception
	 *
	 * @param handler The handler being closed
	 * @param protocol Protocol being used to encode to/from the handler
	 */
	public void onInboundClose(ChannelHandler handler, Protocol<T> protocol);
	public void onOutboundClose(ChannelHandler handler, Protocol<T> protocol);
}
