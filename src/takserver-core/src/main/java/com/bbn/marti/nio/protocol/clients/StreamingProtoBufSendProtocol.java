package com.bbn.marti.nio.protocol.clients;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufProtocol;
import com.bbn.marti.util.concurrent.future.AsyncFunction;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.concurrent.future.AsyncFutures;

import tak.server.cot.CotEventContainer;

import org.apache.log4j.Logger;


/**
 * Created on 5/31/2018.
 */
public class StreamingProtoBufSendProtocol extends StreamingProtoBufProtocol {
    private final static Logger log = Logger.getLogger(StreamingProtoBufSendProtocol.class);

    public final static ProtocolInstantiator<CotEventContainer> streamingProtoBufSendProtocolInstantiator = new ProtocolInstantiator<CotEventContainer>() {
        public StreamingProtoBufSendProtocol newInstance(ChannelHandler handler) { return new StreamingProtoBufSendProtocol(); }
        public String toString() { return "ProtoBuf_streaming_outbound_protocol_builder"; }
    };

    @Override
    public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
        log.trace("writing a protobuf message out to an stcp client");

        // connect channel -- future will be an immediate future if already connected
        AsyncFuture<ChannelHandler> connectFuture = handler.connect();

        AsyncFuture<Integer> writeFuture = AsyncFutures.transform(connectFuture, new AsyncFunction<ChannelHandler, Integer>() {
            public AsyncFuture<Integer> apply(ChannelHandler handler) {
                return StreamingProtoBufSendProtocol.super.write(data, handler);
            }
        });

        return writeFuture;
    }

    public String toString() {
        return "[StreamingProtoBufSend stream processor]";
    }
}
