package com.bbn.marti.nio.protocol.connections;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.bbn.cot.CotParserCreator;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

import atakmap.commoncommo.protobuf.v1.Takmessage;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.proto.StreamingProtoBufHelper;


public class SingleProtobufOrCotProtocol extends AbstractBroadcastingProtocol<CotEventContainer> {
	
	private ThreadLocal<CotParser> cotParser = new ThreadLocal<>(); 
	
    private final static Logger log = Logger.getLogger(SingleProtobufOrCotProtocol.class);

    public final static ProtocolInstantiator<CotEventContainer> singleProtobufOrCotInstantiator = new ProtocolInstantiator<CotEventContainer>() {
        public SingleProtobufOrCotProtocol newInstance(ChannelHandler handler) { return new SingleProtobufOrCotProtocol(); }
        public String toString() { return "SingleProtobufOrCotProtocolInstantiator"; }
    };

    private final static Charset charset = Charset.forName("UTF-8");

    @Override
    public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
        Assertion.areNotNull(buffer, handler);
        Assertion.condition(buffer.hasRemaining(), "Received trivial data container from network");

        try {

            Byte firstByte = buffer.get();
            CotEventContainer cotEventContainer;

            if (firstByte == StreamingProtoBufHelper.MAGIC) {

                int version = StreamingProtoBufHelper.getInstance().readVarint(buffer);
                if (Integer.toString(version).compareTo(StreamingProtoBufHelper.TAK_PROTO_VERSION) != 0) {
                    log.error("Failed to find supported protocol version ! : " + version);
                    return;
                }

                if (buffer.get() != StreamingProtoBufHelper.MAGIC) {
                    log.error("Failed to find magic byte!");
                    return;
                }

                byte[] eventBytes = new byte[buffer.remaining()];
                buffer.get(eventBytes);

                // parse the protobuf
                Takmessage.TakMessage takMessage = Takmessage.TakMessage.parseFrom(eventBytes);
                cotEventContainer = StreamingProtoBufHelper.getInstance().proto2cot(takMessage);

            } else if (firstByte == 0x3C) {

                StringBuilder cotBuilder = new StringBuilder("<");
                cotBuilder.append(charset.decode(buffer).toString());
                
                if (cotParser.get() == null) {
                	cotParser.set(CotParserCreator.newInstance());
                }

                // parse the cot
                Document doc = cotParser.get().parse(cotBuilder.toString());
                cotEventContainer = new CotEventContainer(doc);

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Found invalid first byte! : 0x" + String.format("%02X ", firstByte));
                }

                if (log.isTraceEnabled()) {
                    log.trace("Found invalid content! : " + charset.decode(buffer).toString());
                }
                return;
            }

            // broadcast the message
            broadcastDataReceived(cotEventContainer, handler);

        } catch (Exception e) {
            log.error("exception in onDataReceived!", e);
        }
    }

    @Override
    public AsyncFuture<Integer> write(CotEventContainer cot, ChannelHandler handler) {
        return handler.write(cot.getOrInstantiateBufferEncoding(this.charset));
    }

    @Override
    public void onConnect(ChannelHandler handler) {
        log.trace(String.format(
                "%s received connect signal -- handler: %s",
                this,
                handler
        ));

        broadcastOnConnect(handler);
    }

    @Override
    public void onInboundClose(ChannelHandler handler) {
        log.trace(String.format(
                "%s received network close signal -- handler: %s",
                this,
                handler
        ));

        broadcastInboundClose(handler);
    }

    @Override
    public void onOutboundClose(ChannelHandler handler) {
        log.trace(String.format(
                "%s received application close signal -- handler: %s",
                this,
                handler
        ));

        broadcastOutboundClose(handler);
    }

    @Override
    public String toString() {
        return "SingleProtobufOrCotProtocol";
    }
}
