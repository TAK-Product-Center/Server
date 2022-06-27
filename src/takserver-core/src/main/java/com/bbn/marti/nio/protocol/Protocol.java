

package com.bbn.marti.nio.protocol;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

/**
 * A typed decorator interface that, once implemented, provides the encoding of the data type T to the transport.
 *
 * @param <T>
 */
public interface Protocol<T> extends ChannelListener {
	/**
	* Writes the given data to the transport, returning an 
	* asynchronous integer that will be set with the number of 
	* bytes written out to the wire on account of the given data
	* 
	* If the data could not be sent, the async integer may be 
	* triggered with an exceptional status (see AsyncCallback onFailure)
	*/
	public AsyncFuture<Integer> write(T data, ChannelHandler handler);

	/**
	* add/removes the given protocol listener from this protocol
	*
	* @note Should not be called from the same thread context as a 
	* protocol listener on_ call originating from this protocol.
	* May deadlock due to lock read/write non-reentrancy in some implementations.
	*/
	public boolean addProtocolListener(ProtocolListener<T> listener);

	public boolean removeProtocolListener(ProtocolListener<T> listener);

	public boolean containsProtocolListener(ProtocolListener<T> listener);

	public void negotiate();
}
