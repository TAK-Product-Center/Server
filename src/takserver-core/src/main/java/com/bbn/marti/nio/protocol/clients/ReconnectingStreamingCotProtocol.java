

package com.bbn.marti.nio.protocol.clients;

import org.apache.log4j.Logger;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.connections.StreamingCotProtocol;
import com.bbn.marti.util.concurrent.future.AsyncFunction;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;

import tak.server.cot.CotEventContainer;

/**
* A cot protocol for streaming connections that calls connect before every message send. Any
* exceptional output from a connect call is forwarded to the write.
*/
public class ReconnectingStreamingCotProtocol extends StreamingCotProtocol {
	private final static Logger log = Logger.getLogger(ReconnectingStreamingCotProtocol.class);
	
	public final static ProtocolInstantiator<CotEventContainer> reconnectingCotInstantiator = new ProtocolInstantiator<CotEventContainer>() {
		public ReconnectingStreamingCotProtocol newInstance(ChannelHandler handler) { return new ReconnectingStreamingCotProtocol(); }
        public String toString() { return "CoT_outbound_streaming_protocol_protocol"; }
	};
	
	@Override
	public AsyncFuture<Integer> write(final CotEventContainer data, final ChannelHandler handler) {
		log.debug("writing a cot message out to an stcp client");
		
		// connect channel -- future will be an immediate future if already connected
		AsyncFuture<ChannelHandler> connectFuture = handler.connect();
		
		AsyncFuture<Integer> writeFuture = AsyncFutures.transform(connectFuture, new AsyncFunction<ChannelHandler,Integer>() {
			public AsyncFuture<Integer> apply(ChannelHandler handler) {
				return ReconnectingStreamingCotProtocol.super.write(data, handler);
			}
		});

		return writeFuture;
	}

	public String toString() {
		return "[Client CoT stream processor]";
	}
}