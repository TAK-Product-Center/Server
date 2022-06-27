

package com.bbn.marti.nio.channel;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

import com.bbn.marti.config.Network;
import com.bbn.marti.nio.protocol.ChannelListener;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

/**
* Interface for a handler that receives notifications of socket layer events being published by the server 
* (see the handle_ class of calls), and receives application-side calls for high-level io operations and queries.
*
* The Server (NioServer) is responsible for maintaining the selector, and calls into a handler when io events
* that the handler has subscribed to (on its registered selectable channel)
*
* Channel handlers are guaranteed that:
*
* -- Only one thread (the server thread) will be entrant in a single handle_ call at a given time, such that each
* call is subject to a happens-before relationship. As long as in handling the io call, the channel handler emits
* an event into any processing framework that guarantees linearity (subject to linearity of submission), then 
* connection-oriented protocols can ensure in-order processing of data.
*
* -- The methods corresponding to Read/Write/Connect/Accept will be called only if a channel is registered for that 
* interest op, and a selection key is triggered for that interest op, ie. the OS sends a notification that a given
* interest can be acted upon. The offending channel/server pair are passed into the handle_ call.
*
* -- (that said,) ChannelHandlers should be made resilient to spurious calls for events after they have been unsubscribed or removed from the server,
* as submission and processing of registration/interest op modifications are asynchronous.
*
*
* <p> ChannelHandlers are responsible for internally tracking their own channel ownership, as the Server does not maintain a map
* of handlers to the channels they own. The Server can only modify the interest ops or deregister a channel when given a direct reference 
* to a specific channel. A static ChannelHandler can be used for a group of channels, as any downstream
* functionality is not specified by this interface. In practice, current implementations always construct a chain of 
*
*/
public interface ChannelHandler {
	/**
	* Server side calls -- occur when the corresponding interestOp is triggered
	* for the SelectableChannel's registration. See the Server interface for 
	* facitilies for modifying a channel's interest
	*/
	public boolean handleRead(SelectableChannel channel, Server server, ByteBuffer buff);
	public boolean handleWrite(SelectableChannel channel, Server server, ByteBuffer buff);
	public boolean handleConnect(SelectableChannel channel, Server server);
	public boolean handleAccept(SelectableChannel channel, Server server);

	/**
	* Begins the connection process for the contained channel. Can only 
	* be called on handler types where the application is acting as a 
	* client.
	*
	* If the socket is already connected, nothing happens, and an
	* immediate future is returned. 
	*
	* Otherwise, the socket is opened and registered with the Server with a
	* CONNECT interest. When the connect process is finished (see finishConnect)
	* after the server calls into the channel, the returned future is triggered.
	*/
	public AsyncFuture<ChannelHandler> connect();
	
	/**
	* Schedules a write for the given data. The implementor guarantees that:
	* - the given buffer will map to a contiguous chunk of data written out to the wire
	* - all write calls
	*
	* The returned future is triggered when the write for the given data completes, 
	* and set to the number of bytes written out on account of the given buffer,
	* or is set to an exceptional state if the write does not complete.
	*/
	public AsyncFuture<Integer> write(ByteBuffer buffer);
	
	/**
	* Initiates the close process. Unless an exceptional state is encountered,
	* clients can expect that all writes preceeding the close call will be 
	* written to the wire before the close call is applied.
	*/
	public AsyncFuture<ChannelHandler> close();

    /**
    * Initiates and completes the close process. All pending writes will be dropped.
    * 
    * All pending reads (those already pulled from the network) will be reported 
    * to the channel listeners, followed by an EOS (onInboundClose) notification.
    * This may happen asynchronously.
    */
    public void forceClose();

	// general properties queries
	public InetAddress host();
	public int port();
	public int localPort();
	public String netProtocolName(); // e.g. "TCP"

	// constructor/specialization args
	public void listener(ChannelListener listener);

	// property queries
	public ChannelListener listener();

	public boolean isMatchingInput(Network.Input input);
	
	public String identityHash();
}