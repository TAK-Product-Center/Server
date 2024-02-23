

package com.bbn.marti.nio.protocol.connections;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.atakmap.Tak.FederatedEvent;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Created on 10/29/15.
 */
public class TakProtoBufProtocol extends AbstractBroadcastingProtocol<FederatedEvent> {
    private final static Logger log = Logger.getLogger(TakProtoBufProtocol.class);
    private volatile boolean outboundClosed = false;

    private static final int INTBYTES = Integer.SIZE / Byte.SIZE;

    ByteBuffer leftovers = null;
    int nextSize = -1;

  /**
	* A static, inner factory class that returns a new instance of the outer.
	*/
	public final static ProtocolInstantiator<FederatedEvent> TakProtoBufInstantiator = new ProtocolInstantiator<FederatedEvent>() {
        @Override
        public TakProtoBufProtocol newInstance(ChannelHandler handler) { return new TakProtoBufProtocol(); }
        public String toString() { return "Tak_ProtoBuf_protocol_builder"; }
    };

    @Override
    public void onConnect(ChannelHandler handler) {
    	Assertion.notNull(handler);

        log.trace(String.format(
            "%s received connect signal -- handler: %s",
            this,
            handler
        ));

		// notify our listeners
		super.broadcastOnConnect(handler, this);
    }

    @Override
    public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
        ByteBuffer fullBuf = null;
        //long start = System.currentTimeMillis();
        //log.warn("new read: " + buffer.remaining() + " bytes");

        if (leftovers == null) {
            fullBuf = buffer;
        } else {
            int binaryLength = buffer.remaining();
            fullBuf = ByteBuffer.allocate(leftovers.remaining() + binaryLength);
            //log.warn("making new array from leftovers: " + leftovers.remaining() + " new array: " + fullBuf.capacity());
            fullBuf.put(leftovers);
            fullBuf.put(buffer);
            ((Buffer)fullBuf).flip();
            leftovers = null;
        }

        boolean breakout = false;
        while(fullBuf.remaining() > 0 && !breakout) {
            //log.warn("looping: " + nextSize);
            if (nextSize == -1) {
                if (fullBuf.remaining() > INTBYTES) {
                    nextSize = fullBuf.getInt();
                    //log.warn("getting new event: " + nextSize);
                } else {
                    //log.warn("not even enough bytes to read an int, waiting for more data");
                    leftovers = ByteBuffer.allocate(fullBuf.remaining());
                    leftovers.put(fullBuf);
                    ((Buffer)leftovers).flip();
                    breakout = true;
                    break;
                }
            }

            if (fullBuf.remaining() < nextSize) {
                //log.warn("not a full event, waiting for more data.  remaining: " + fullBuf.remaining() + " needed: " + nextSize);
                leftovers = ByteBuffer.allocate(fullBuf.remaining());
                leftovers.put(fullBuf);
                ((Buffer)leftovers).flip();
                breakout = true;
                break;
            }

            byte [] eventBytes = new byte[nextSize];
            nextSize = -1;
            fullBuf.get(eventBytes);
            if (fullBuf.remaining() == 0) {
                leftovers = null; // just to be sure
            }
            //log.warn(" leftover bytes: " + fullBuf.remaining());

            try {
                FederatedEvent event = FederatedEvent.parseFrom(eventBytes);
                nextSize = -1;
                super.broadcastDataReceived(event, handler);
            } catch (InvalidProtocolBufferException e) {
                log.error("parsing problem with Federated Event: " + e.getMessage());
                //probably unrecoverable
                handler.forceClose();
            }
        }

        //log.trace("TakProtoBufProtocol onDataReceived took " + (System.currentTimeMillis() - start) + " ms");
    }

    @Override
    public AsyncFuture<Integer> write(FederatedEvent data, ChannelHandler handler) {

        byte [] eventBytes = data.toByteArray();
        ByteBuffer binaryData = ByteBuffer.allocate(INTBYTES + eventBytes.length);
        binaryData.putInt(eventBytes.length);
        binaryData.put(eventBytes);
        ((Buffer)binaryData).rewind();

        log.trace(String.format(
            "%s writing application data -- handler: %s data_length: %d",
            this,
            handler,
            binaryData.remaining()
        ));

		return handler.write(binaryData);
    }

    @Override
    public void onInboundClose(ChannelHandler handler) {
        log.trace(String.format(
            "%s received network close signal -- handler: %s",
            this,
            handler
        ));

		// notify the listeners
		super.broadcastInboundClose(handler);
    }

    @Override
    public void onOutboundClose(ChannelHandler handler) {
        Assertion.notNull(handler);

        this.outboundClosed = true;

        log.trace(String.format(
                "%s received application close signal -- handler: %s",
                this,
                handler
        ));

        // notify listeners
        super.broadcastOutboundClose(handler);
    }


}
