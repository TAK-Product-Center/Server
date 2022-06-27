

package com.bbn.marti.nio.server;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.EnumSet;

import com.bbn.marti.config.Network;
import com.bbn.marti.nio.binder.ServerBinder;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.util.IOEvent;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

/**
* Interface for a nio server that is controlled through a bind/listen/stop sequence as a persistent service, 
* and through register/deregister/modify change operations as an event publisher on the channels it regulates.
*
* <p> The server is responsible for handling scheduling and guaranteeing ordering of calls. See {@link SchedulingStrategy},
* where the server makes different guarantees about different scheduling strategies in terms of the number of threads servicing
* a type of request at a given time.
*
* <p> Channel registration/deregistration and modification of interest is done asynchronously, with the listenable future triggered
* when the operation is completed, or when an exception is generated. ChannelHandlers should be made resilient to unwanted calls that 
* occur between the time when the change  request is submitted, and the time when the server processes it. 
* The Server does guarantee that InterestOp modifications are applied in the order they were received (subject to a happens before 
* relation with the entry and exit of the server's submit method call.
*
*/
public interface Server {
	public void bind(ServerBinder binder, Network.Input input) throws IOException;

	// called to start the server listening
	public void listen();
	
	// called to stop the server listening and release all resources
	public void stop();
	
	// application side interface for adding/removing channels from the selection set
    public AsyncFuture<ChannelHandler> registerChannel(SelectableChannel channel, ChannelHandler handler, EnumSet<IOEvent> events);

    public AsyncFuture<ChannelHandler> registerChannel(SelectableChannel channel, ChannelHandler handler, IOEvent event);

    public AsyncFuture<ChannelHandler> deregisterChannel(SelectableChannel channel, ChannelHandler handler);

	// application side interface for changing the selection key registration for a specific channel (ie, what we want the channel handler to receive 
	// notifications about)

    // absolute set -- blows away current interest sets
    public AsyncFuture<ChannelHandler> setInterestOps(SelectableChannel channel, EnumSet<IOEvent> events);

    public AsyncFuture<ChannelHandler> setInterestOp(SelectableChannel channel, IOEvent event);    

    // relative add -- ORs in given event. Idempotent if already registered for that IO flag
    public AsyncFuture<ChannelHandler> addInterestOp(SelectableChannel channel, IOEvent event);
    
    public AsyncFuture<ChannelHandler> addInterestOps(SelectableChannel channel, EnumSet<IOEvent> events);

    public AsyncFuture<ChannelHandler> removeIterestOp(SelectableChannel channel, IOEvent event);
    
    public AsyncFuture<ChannelHandler> removeIterestOps(SelectableChannel channel, EnumSet<IOEvent> events);
    // relative subtract -- ANDs out given event. Idempotent if already not registered for that IO flag    
}