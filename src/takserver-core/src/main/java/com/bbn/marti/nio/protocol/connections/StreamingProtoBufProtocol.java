package com.bbn.marti.nio.protocol.connections;

import java.io.ByteArrayInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import com.bbn.marti.config.Network;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.nio.util.ConnectionState;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.google.protobuf.CodedOutputStream;

import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import io.micrometer.core.instrument.Metrics;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.proto.StreamingProtoBufHelper;

public class StreamingProtoBufProtocol extends AbstractBroadcastingProtocol<CotEventContainer> {

    private final static Logger log = Logger.getLogger(StreamingProtoBufProtocol.class);
    private final static int MAX_SIZE = 65536;
    private final static int MAX_LOG_SIZE = 1024;

    private volatile boolean outboundClosed = false;
    private ByteBuffer leftovers = null;
    private boolean gotMagic = false;
    private boolean gotSize = false;
    private int nextShift = 0;
    private int nextSize = 0;

    /**
     * A static, inner factory class that returns a new instance of the outer.
     */
    public final static ProtocolInstantiator<CotEventContainer> streamingProtoBufInstantiator = new ProtocolInstantiator<CotEventContainer>() {
        @Override
        public StreamingProtoBufProtocol newInstance(ChannelHandler handler) { return new StreamingProtoBufProtocol(); }
        public String toString() { return "ProtoBuf_streaming_protocol_builder"; }
    };

    /**
     * Called when the handler first connects.
     *
     * Instantiate our parser/message buffer.
     */
    @Override
    public void onConnect(ChannelHandler handler) {
        Assertion.notNull(handler);

        // notify our listeners
        super.broadcastOnConnect(handler, this);
    }

    private boolean readSize(ByteBuffer buffer) {
        while (buffer.remaining() > 0) {
            byte b = buffer.get();
            if ((b & 0x80) == 0) {
                nextSize = nextSize | (b << nextShift);
                return true;
            } else {
                nextSize |= (b & 0x7F) << nextShift;
                nextShift += 7;

                // TODO check for varint max size of 64 bits
            }
        }
        return false;
    }

    @Override
    public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
        Assertion.areNotNull(buffer, handler);
        Assertion.condition(buffer.hasRemaining(), "Received trivial data container from network");

        try {
            ByteBuffer fullBuf = null;
            if (leftovers == null) {
                // first time through leftovers is null, set fullbuf to new buffer
                fullBuf = buffer;
            } else {
                //  set fullbuf to leftovers + buffer
                int binaryLength = buffer.remaining();
                fullBuf = ByteBuffer.allocate(leftovers.remaining() + binaryLength);
                fullBuf.put(leftovers);
                fullBuf.put(buffer);
                ((Buffer)fullBuf).flip();
                leftovers = null;
            }

            // try to parse messages out of fullbuf
            while (fullBuf.remaining() > 0) {

                // have we read a size from the stream yet?
                if (nextSize == 0) {

                    // size is preceded by the magic byte
                    if (!gotMagic) {
                        byte nextMagic = fullBuf.get();
                        gotMagic = nextMagic == StreamingProtoBufHelper.MAGIC;
                        if (!gotMagic) {
                            log.error("Failed to find magic byte, instead found " + nextMagic);
                            break;
                        }
                    }
                }

                if (!gotSize) {
                    gotSize = readSize(fullBuf);
                    if (!gotSize) {
                        break;
                    }
                }

                // do we have enough left in the buffer to read out a full message?
                if (fullBuf.remaining() < nextSize) {
                    // haven't got enough for a message, stash the fullbuf in leftovers for next time around
                    leftovers = ByteBuffer.allocate(fullBuf.remaining());
                    leftovers.put(fullBuf);
                    ((Buffer)leftovers).flip();
                    break;
                }

                // copy bytes for next message into eventBytes
                byte[] eventBytes = new byte[nextSize];
                fullBuf.get(eventBytes);

                // parse and broadcast the message
                TakMessage takMessage = TakMessage.parseFrom(eventBytes);
                CotEventContainer cotEventContainer = StreamingProtoBufHelper.proto2cot(takMessage);
                super.broadcastDataReceived(cotEventContainer, handler);

                // reset parser state
                nextSize = 0;
                nextShift = 0;
                gotMagic = false;
                gotSize = false;
                leftovers = null;
            }
        } catch (Exception e) {
            log.error("Exception in onDataReceived!", e);
            handler.forceClose();
        }
    }

    private static TakMessage createFileTransferRequest(CotEventContainer data) {
        try {
            // set the download url
            Network network = DistributedConfiguration.getInstance().getNetwork();
            if (network == null || network.getTakServerHost() == null || network.getTakServerHost().length() == 0
                || network.getConnector() == null || network.getConnector().size() == 0) {
                log.error("createFileTransferRequest failed, need to set takServerHost and connector in CoreConfig <network/> ");
                return null;
            }

            Network.Connector connector = network.getConnector().get(0);
            String url = "https://" + network.getTakServerHost() + ":" + connector.getPort() + "/Marti/api/cot/xml/" + data.getUid();

            // compute the hash of the cot that will be downloaded
            String dataXml = Constants.XML_HEADER + data.toCotElement().toCotXml();
            String shaHash = CommonUtil.SHA256(dataXml.getBytes());

            String senderCallsign = "takserver";
            if (data.getContext(Constants.USER_KEY) != null) {
                senderCallsign = ((AuthenticatedUser)data.getContext(Constants.USER_KEY)).getName();
            }

            // build the fileTransfer message
            String fileTransferXml = CommonUtil.getFileTransferCotMessage(
                    shaHash, shaHash, senderCallsign,
                    data.getUid() + ".cot", url, dataXml.length(), new String[]{});

            // convert the fileTransfer message to protobuf
            SAXReader reader = new SAXReader();
            Document doc = reader.read(new ByteArrayInputStream(fileTransferXml.getBytes()));
            CotEventContainer container = new CotEventContainer(doc);
            TakMessage takMessage = StreamingProtoBufHelper.cot2protoBuf(container);
            return takMessage;

        } catch (Exception e) {
            log.error("Exception in createFileTransferRequest!", e);
            return null;
        }
    }

    /**
     * Called when the application wants to send data to the network
     *
     * Convert data to its byte format, hand data to the channel's handler.
     */
    @Override
    public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
        try {
            Assertion.condition(!outboundClosed, "!outboundClosed");
            Assertion.areNotNull(data, handler);

            ByteBuffer buffer = data.getProtoBufBytes();

            if (buffer == null) {
            	buffer = convertCotToProtoBufBytes(data);
            } else {
				Metrics.counter(Constants.METRIC_MESSAGE_PRECONVERT_COUNT, "takserver", "messaging").increment();
            }

            if(buffer == null) {
                return null;
            };

            return handler.write(buffer);

        } catch (Exception e) {
            log.error("Exception in write!", e);
            return null;
        }
    }

    public static ByteBuffer convertCotToProtoBufBytes(CotEventContainer data) {
        ByteBuffer buffer = null;
        try {
            //
            // Convert CotEventContainer to protobuf
            //
            TakMessage takMessage = StreamingProtoBufHelper.cot2protoBuf(data);
            if (takMessage == null) {
                log.error("cot2protoBuf failed to parse message!");
                return null;
            }

            //
            // allocate a buffer for the message
            //
            int takMessageSize = takMessage.getSerializedSize();
            if (takMessageSize > MAX_SIZE) {
                String xml = data.asXml();
                if (xml.length() > MAX_LOG_SIZE) {
                    xml = xml.substring(0, MAX_LOG_SIZE) + "...";
                }

                if (log.isDebugEnabled()) {
                	log.debug("Attempt to write message greater than max size : " + takMessageSize + ", " + xml);
                } else {
                	log.error("Attempt to write message greater than max size : " + takMessageSize);
                }

                // overwrite the failed message with a file transfer request, and write it out instead
                takMessage = createFileTransferRequest(data);
                if (takMessage == null) {
                    log.error("createFileTransferRequest failed!");
                    return null;
                }

                takMessageSize = takMessage.getSerializedSize();
            }

            int sizeOfSize = CodedOutputStream.computeUInt32SizeNoTag(takMessageSize);
            buffer = ByteBuffer.allocate(1 + sizeOfSize + takMessageSize);

            //
            // write out the message to the buffer
            //
            CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(buffer);
            codedOutputStream.write(StreamingProtoBufHelper.MAGIC);
            codedOutputStream.writeUInt32NoTag(takMessageSize);
            takMessage.writeTo(codedOutputStream);
            ((Buffer) buffer).rewind();
        }catch(Exception e) {
            log.error("Error converting cot to proto " + e);
        }

        return buffer;
    }

    /**
     * Called when the handler has finished propagating
     * data coming from the network, signifies an EOS
     *
     * void out our buffered structures
     */
    @Override
    public void onInboundClose(ChannelHandler handler) {

        if (leftovers != null && leftovers.remaining() > 0) {
            log.warn("Received EOS notification with partial message (" + leftovers.remaining());
        }

        // notify the listeners
        super.broadcastInboundClose(handler);
    }

    /**
     * Called when the handler would like to
     */
    @Override
    public void onOutboundClose(ChannelHandler handler) {
        Assertion.notNull(handler);

        this.outboundClosed = true;

        // notify listeners
        super.broadcastOutboundClose(handler);
    }

    /**
     * DO NOT put the channel handler in the string -- typically prints out its listener as
     * part of its toString method
     */
    @Override
    public String toString() {
        return "server_streaming_protobuf";
    }
}
