

package com.bbn.marti.nio.codec;

import java.nio.ByteBuffer;

import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

/**
* A codec that handles network -> application, and application -> network traffic conversion.
*
* Codecs are guaranteed that:
*
* -- only one thread is entrant in a read/connect/inboundClose call at a time
*
* -- likewise, one thread is entrant in a write/outboundClose call at a time
*
* -- any unread data in the buffer passed to decode will be reclaimed by the outer
* context, and recycled, such that it will form the beginning of the next data segment
* passed to decode when more data arrives
*
* -- the order of any data emitted from encode/decode will be kept in the same
* order w.r.t. a happens-before relation of encode/decode calls. (within an encode, or a 
* decode, scope. encode and decode are themselves disjoint.)
*
* Codecs must guarantee that:
*
* -- within the scope of a single decode call, all progress that can be made on the 
* given data *is* made. ie, if a codec does not process all of the data passed to 
* a decode call, then an arbitrary number of decode calls with that remaining data 
* will always emit the empty buffer, and will always the passed in buffer marked
* as completely unread
*
* -- a call to encode with nonempty data *always* completely consumes the input,
* producing meaningful output.
*/
public interface ByteCodec {
    /**
    * Called by the pipeline on init.
    *
    * After the call returns, the codec can expect that:
    *
    * -- all calls to its PipelineContext will be serviced
    *
    * -- it can write to the network
    *
    *
    * The Pipeline expects that, until the returned future is triggered:
    * 
    * -- no read traffic is propagated beyond the given codec, ie. a decode
    * call does not produce a nonempty ByteBuffer (Pipelines can handle
    * a future being triggered *during* a read call)
    */
    AsyncFuture<ByteCodec> onConnect();	

    /**
	* Receives incoming, network side traffic and decodes it into another byte buffer
    *
    * A synchronous call from the callee pipeline, ie. no side effects are expected to take place outside 
    * the scope of this call
    *
    * Pipeline implementors guarantee that this call will not be made until the onConnect call has
    * returned
	*/
	ByteBuffer decode(ByteBuffer buffer);
	
	/**
	* Receives outgoing, application side traffic and encodes it into another byte buffer
    *
    * Similar to the decode call--no calls will be made until onConnect completes. Will
    * possibly be serviced concurrently with decode/inboundClose.
	*/
	ByteBuffer encode(ByteBuffer buffer);

	/**
    * Inbound close is propagated in a network-to-application order
    *
    * After this call is made, no decode calls will be placed.
    *
    * After this call, encode calls may be placed, but can be discarded.
	*/
	void onInboundClose();
    
    /**
    * Outbound close is propagated in an application-to-network order
    *
    * After this call is made, no encode calls will be placed.
    *
    * After this calls, decode calls may be placed, and should be serviced.
    */
	void onOutboundClose();
	
	void setConnectionInfo(ConnectionInfo connectionInfo);
	
	ConnectionInfo getConnectionInfo();
}