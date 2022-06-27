

package com.bbn.marti.nio.protocol.clients;

import org.apache.log4j.Logger;

import com.atakmap.Tak.FederatedEvent;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.connections.TakProtoBufProtocol;
import com.bbn.marti.util.concurrent.future.AsyncFunction;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;

/**
 * Created on 10/29/15.
 */
public class TakProtoBufSendProtocol extends TakProtoBufProtocol {
   	private final static Logger log = Logger.getLogger(TakProtoBufSendProtocol.class);

	public final static ProtocolInstantiator<FederatedEvent> TakProtoBufSendProtocolInstantiator = new ProtocolInstantiator<FederatedEvent>() {
		public TakProtoBufSendProtocol newInstance(ChannelHandler handler) { return new TakProtoBufSendProtocol(); }
        public String toString() { return "Tak_ProtoBuf_outbound_protocol"; }
	};

	@Override
	public AsyncFuture<Integer> write(final FederatedEvent data, final ChannelHandler handler) {
		log.trace("writing a protobuf message out to an stcp client");

		// connect channel -- future will be an immediate future if already connected
		AsyncFuture<ChannelHandler> connectFuture = handler.connect();

		AsyncFuture<Integer> writeFuture = AsyncFutures.transform(connectFuture, new AsyncFunction<ChannelHandler, Integer>() {
            public AsyncFuture<Integer> apply(ChannelHandler handler) {
                return TakProtoBufSendProtocol.super.write(data, handler);
            }
        });

		return writeFuture;
	}

	public String toString() {
		return "[TakProtolBufSend stream processor]";
	}
}
