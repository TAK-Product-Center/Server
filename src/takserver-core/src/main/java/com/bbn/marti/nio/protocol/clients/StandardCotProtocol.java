

package com.bbn.marti.nio.protocol.clients;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.connections.StreamingCotProtocol;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.future.AsyncCallback;
import com.bbn.marti.util.concurrent.future.AsyncFunction;
import com.bbn.marti.util.concurrent.future.AsyncFunctionAdapter;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;
import com.google.common.base.Function;

import tak.server.cot.CotEventContainer;

/**
* A cot protocol that queues write events, and interleaves open/writes/closes with each event callback.
*
*/
public class StandardCotProtocol extends StreamingCotProtocol implements AsyncCallback<Integer> {
	public final static ProtocolInstantiator<CotEventContainer> standardCotInstantiator = new ProtocolInstantiator<CotEventContainer>() {
		public Protocol<CotEventContainer> newInstance(ChannelHandler handler) { return new StandardCotProtocol(); }
        public String toString() { return "CoT_standard_protocol_builder"; }
	};

	/**
	* Struct for holding open-write-close function chaining logic, the data to write, and metadata
	* about what we wrote.
	*/
	private class WriteUnit {
		WriteData data;
		AsyncCallback<ChannelHandler> writeHead;
		AsyncFuture<Integer> writeTail;
	}
	
	/**
	* Struct for holding data for write. BytesWritten should be assigned after the write complete
	* function returns
	*/
	private static class WriteData {
		ChannelHandler handler;
		CotEventContainer data;
		int bytesWritten = -1;
	}
	
	/**
	* A function that receives a channel handler (presumably after a successful connect), and chains 
	* a write event to it, returning the settable future created by the handler to signal the write's completion.
	*/		
	private class WriteDataFunction implements AsyncFunction<ChannelHandler,Integer> {
		private WriteData toWrite;
		public AsyncFuture<Integer> apply(ChannelHandler handler) {
			log.trace("StandardCotProtocol applying write function");
			return handler.write(toWrite.data.getOrInstantiateBufferEncoding());
		}
	}

	/**
	* A function that receives an integer (the number of bytes written after a send), and chains a channel close 
	* to it, returning the settable future created by the handler to signal the close's completion.
	*/
	private class CloseChannelFunction implements AsyncFunction<Integer,ChannelHandler> {
		private WriteData toWrite;
		public AsyncFuture<ChannelHandler> apply(Integer bytesSent) {
			log.trace("StandardCotProtocol applying close function");
			toWrite.bytesWritten = bytesSent;
			return toWrite.handler.close();
		}
	}

	private class PostCloseChannelCallback implements Function<ChannelHandler,Integer> {
		private WriteData toWrite;
		public Integer apply(ChannelHandler handler) {
			log.trace("StandardCotProtocol applying post close function");
			Assertion.condition(toWrite.bytesWritten != -1);
			return toWrite.bytesWritten;
		}
	}
	
	private static final Logger log = Logger.getLogger(StandardCotProtocol.class);
	private Queue<WriteUnit> outgoing;
	
	public StandardCotProtocol() {
		super();
		this.outgoing = new ConcurrentLinkedQueue<WriteUnit>();
	}
	
	public void onOutboundClose(ChannelHandler handler) {
		outgoing.clear();
		super.onOutboundClose(handler);
	}
	
	/**
	* Failure method for handling a write unit gone wrong
	*
	* TODO: logic for implementing a kind-of wait/scheduled reconnect/subscription removal
	*/
	@Override
	public void onFailure(Exception t) {
		log.trace("tcp client protocol receiving scheduling (error) callback");
		scheduleNext();
	}
	
	/**
	* Success method for handling a write unit success
	*/
	@Override
	public void onSuccess(Integer result) {
		log.trace("tcp client protocol receiving scheduling callback");
		scheduleNext();
	}

	/**
	* Removes the write unit that we just completed off the queue, and schedules the unit
	* after it if there is one
	*/
	private void scheduleNext() {
		WriteUnit writeUnit = scheduleWriteUnit();
		if (writeUnit != null) {
			log.trace("tcp client protocol dequeueing next write");
			chainWriteUnit(writeUnit, writeUnit.data.handler);
		}
	}
	
	private synchronized WriteUnit scheduleWriteUnit() {
		// pop write unit we just worked on off the queue
		WriteUnit writeUnit = outgoing.poll();
		
		// DEBUG -- shouldn't be null, we are just removing the write unit we just worked on
		Assertion.notNull(writeUnit);
		
		return outgoing.peek();
	}
	
	/**
	* Attempts to enqueue the write unit. If the queue empty before we enqueue,
	* then we know that write unit will not be scheduled by the tail end of a 
	*
	*/
	private synchronized boolean scheduleWriteUnit(WriteUnit unit) {
		boolean wasEmpty = (outgoing.peek() == null);
		boolean accepted = outgoing.offer(unit);
		
		return wasEmpty && accepted;
	}
	
	/**
	* enqueue new write, chain its scheduling to the execution of the current future
	*/
	@Override
	public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
		log.debug("Writing CoT: " + data.asXml());
		
		// build write unit
		WriteUnit writeUnit = buildWriteUnit(data, handler);
		
		// schedule -- adds to the pending queue if there is an active write, or executes it if there is not
		if (scheduleWriteUnit(writeUnit)) {
			log.trace("StandardCotProtocol write being scheduled");
			// nothing was on the queue, will not be scheduled by the return of a
			// pending write unit
			chainWriteUnit(writeUnit, writeUnit.data.handler);
		} else {
			log.trace("StandardCotProtocol write being enqueued");
		}
		
		// return future for the write unit
		return writeUnit.writeTail;
	}
	
	/**
	* builds a write unit
	*/
	private WriteUnit buildWriteUnit(CotEventContainer data, ChannelHandler handler) {
		// build write data struct
		WriteData newWrite = new WriteData();
		newWrite.data = data;
		newWrite.handler = handler;

		// build write data function
		WriteDataFunction writeFunction = new WriteDataFunction();
		writeFunction.toWrite = newWrite;

		// build close channel/set result function
		CloseChannelFunction closeFunction = new CloseChannelFunction();
		closeFunction.toWrite = newWrite;
		
		// build post close callback
		PostCloseChannelCallback postCloseFunction = new PostCloseChannelCallback();
		postCloseFunction.toWrite = newWrite;
		
		// build to-run function chain for later
		AsyncFunctionAdapter<ChannelHandler,Integer> connectWriteJunction = AsyncFunctionAdapter.create(writeFunction);
		AsyncFuture<ChannelHandler> closeFuture = AsyncFutures.transform((AsyncFuture<Integer>) connectWriteJunction, closeFunction);
		AsyncFuture<Integer> aggregateFunction = AsyncFutures.transform(closeFuture, postCloseFunction);
				
		// build struct to hold write unit functions until they can be scheduled
		WriteUnit writeUnit = new WriteUnit();

		// store data, head, tail into struct
		writeUnit.data = newWrite;
		writeUnit.writeHead = (AsyncCallback<ChannelHandler>) connectWriteJunction;
		writeUnit.writeTail = aggregateFunction;
		
		return writeUnit;
	}
	
	private void chainWriteUnit(WriteUnit unit, ChannelHandler handler) {
		log.trace("StandardCotProtocol building write unit for single write, handler: " + handler);
		AsyncFutures.addCallback(handler.connect(), unit.writeHead);
		AsyncFutures.addCallback(unit.writeTail, this);
	}

	@Override
	public String toString() {
		return "[Client CoT message processor]";
	}
}