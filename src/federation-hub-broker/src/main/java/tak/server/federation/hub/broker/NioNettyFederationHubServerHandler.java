package tak.server.federation.hub.broker;

import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.Identity;
import com.bbn.roger.fig.FederationUtils;
import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;

public class NioNettyFederationHubServerHandler extends SimpleChannelInboundHandler<byte[]> {
    private final static Logger logger = LoggerFactory.getLogger(NioNettyFederationHubServerHandler.class);
    private static final int INTBYTES = Integer.SIZE / Byte.SIZE;
    private AtomicBoolean alreadyClosed = new AtomicBoolean(true);
    private ByteBuffer leftovers = null;
    private int nextSize = -1;

    protected ConnectionInfo connectionInfo;
    protected Reader reader;
    private ChannelHandlerContext nettyContext;
    private Certificate[] certArray;
    private FederateIdentity federateIdentity;

    private final Set<FederatedEvent> cache;
    
    private final String sessionId;

    public Set<FederatedEvent> getCache() {
        return cache;
    }

    private FederationHubBrokerService brokerService;

    public NioNettyFederationHubServerHandler(String sessionId, FederationHubBrokerService brokerService,
            Comparator<FederatedEvent> comp) {
        super();
        this.sessionId = sessionId;
        this.brokerService = brokerService;
        this.cache = new ConcurrentSkipListSet<FederatedEvent>(comp);
    }

    @FunctionalInterface
    private interface Reader {
        void read(byte[] msg);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        /* If we somehow fill the buffer without flushing, force the flush now. */
        if (!ctx.channel().isWritable()) {
            nettyContext.flush();
        }
    }

    public FederateIdentity getFederateIdentity() {
        return federateIdentity;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.pipeline()
            .get(SslHandler.class)
            .handshakeFuture()
            .addListener(new GenericFutureListener<Future<Channel>>() {
                @Override
                public void operationComplete(Future<Channel> future) throws Exception {
                    if (!future.isSuccess()) {
                        ctx.close();
                        return;
                    }
                    nettyContext = ctx;
                    InetSocketAddress remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    try {
                        SslHandler sslhandler = (SslHandler)nettyContext.channel().pipeline().get("ssl");
                        certArray = sslhandler.engine().getSession().getPeerCertificates();
                    } catch (SSLPeerUnverifiedException e) {
                        logger.error("Could not get certificate chain", e);
                        ctx.close();
                        return;
                    }

                    createConnectionInfo();
                    String fedId = FederationUtils.getBytesSHA256(certArray[0].getEncoded()) + "-" + ctx.hashCode();
                    federateIdentity = new FederateIdentity(fedId);
                    brokerService.addCaFederateToPolicyGraph(federateIdentity, certArray);
                    setReader();
                    alreadyClosed.set(false);
                    HubConnectionInfo hubConnectionInfo = new HubConnectionInfo();
                    hubConnectionInfo.setConnectionId(fedId);
                    hubConnectionInfo.setRemoteConnectionType(Identity.ConnectionType.FEDERATION_TAK_CLIENT.toString());
                    hubConnectionInfo.setLocalConnectionType(Identity.ConnectionType.FEDERATION_HUB_SERVER.toString());
                    hubConnectionInfo.setFederationProtocolVersion(1);
                    hubConnectionInfo.setRemoteAddress(remoteSocketAddress.getHostString() + ":" + remoteSocketAddress.getPort());
                   
                    boolean success = brokerService.addV1ConnectionInfo(sessionId, hubConnectionInfo);
                    if (!success) {
                    	forceClose();
                    }
                    
                    brokerService.sendContactMessagesV1(NioNettyFederationHubServerHandler.this);
                }
            });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("NioNettyFederationHubServerHandler error -- closing connection ", cause);
        channelUnregistered(ctx);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        connectionInfo.getReadCount().getAndIncrement();
        connectionInfo.getProcessedCount().getAndIncrement();
        reader.read(msg);
    }

    private static String getConnectionId(SocketChannel socket) {
        String id = null;
        try {
            id = Integer.valueOf(socket.hashCode()).toString();
        } catch (Exception e) {
            logger.error("Exception getting connection ID", e);
        }
        return id;
    }

    private void createConnectionInfo() {
        connectionInfo = new ConnectionInfo(ConnectionInfo.ConnectionType.INCOMING, null);
        X509Certificate cert = (X509Certificate)certArray[0];
        connectionInfo.setCert(cert);
        connectionInfo.setConnectionId(getConnectionId((SocketChannel)nettyContext.channel()));
        if (nettyContext.channel().remoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress addr = (InetSocketAddress)nettyContext.channel().remoteAddress();
            connectionInfo.setAddress(addr.getHostString());
            connectionInfo.setPort(addr.getPort());
        } else {
            connectionInfo.setAddress(nettyContext.channel().remoteAddress().toString());
        }
    }

    public void send(Message message) {
        if (alreadyClosed.get()) {
            return;
        }

        if (nettyContext.channel().isWritable()) {
            byte[] bytes = message.getPayload().getBytes();
            ByteBuffer binaryData = ByteBuffer.allocate(INTBYTES + bytes.length);
            binaryData.putInt(bytes.length);
            binaryData.put(bytes);
            ((Buffer)binaryData).rewind();
            connectionInfo.getProcessedCount().getAndIncrement();
            nettyContext.writeAndFlush(binaryData.array());
        } else {
            logger.error("Netty channel is not writable");
        }
    }

    private void setReader() {
        reader = (msg) -> {
            ByteBuffer buffer = ByteBuffer.wrap(msg);
            ByteBuffer fullBuf = null;

            if (leftovers == null) {
                fullBuf = buffer;
            } else {
                int binaryLength = buffer.remaining();
                fullBuf = ByteBuffer.allocate(leftovers.remaining() + binaryLength);
                fullBuf.put(leftovers);
                fullBuf.put(buffer);
                ((Buffer) fullBuf).flip();
                leftovers = null;
            }

            boolean breakout = false;
            while (fullBuf.remaining() > 0 && !breakout) {
                if (nextSize == -1) {
                    if (fullBuf.remaining() > INTBYTES) {
                        nextSize = fullBuf.getInt();
                    } else {
                        leftovers = ByteBuffer.allocate(fullBuf.remaining());
                        leftovers.put(fullBuf);
                        ((Buffer) leftovers).flip();
                        breakout = true;
                        break;
                    }
                }

                if (fullBuf.remaining() < nextSize) {
                    leftovers = ByteBuffer.allocate(fullBuf.remaining());
                    leftovers.put(fullBuf);
                    ((Buffer) leftovers).flip();
                    breakout = true;
                    break;
                }

                byte[] eventBytes = new byte[nextSize];
                nextSize = -1;
                fullBuf.get(eventBytes);
                if (fullBuf.remaining() == 0) {
                    leftovers = null;
                }

                try {
                    FederatedEvent event = FederatedEvent.parseFrom(eventBytes);
                    nextSize = -1;

                    // Add federate to group in case policy was updated during connection.
                    brokerService.addFederateToGroupPolicyIfMissingV1(certArray,
                         federateIdentity);

                    Message federatedMessage = new Message(new HashMap<>(),
                        new FederatedEventPayload(event));
                    try {
                        brokerService.assignGroupFilteredMessageSourceAndDestinationsFromPolicy(federatedMessage, null,
                            federateIdentity);
                        Set<AddressableEntity<?>> dests = federatedMessage.getDestinations();
                    } catch (FederationException e) {
                        logger.error("Could not get destinations from policy graph", e);
                        forceClose();
                        return;
                    }

                    brokerService.sendFederatedEventV1(federatedMessage);

                    if (event != null && event.hasContact()) {
                        cache.add(event);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Caching " + event +
                                "  for " + federateIdentity.getFedId());
                        }
                    }
                } catch (InvalidProtocolBufferException e) {
                    logger.error("Parsing problem with FederatedEvent", e);
                    forceClose();
                }
            }
        };
    }

    public void forceClose() {
        if (nettyContext != null && nettyContext.channel().isActive()) {
            nettyContext.close();
        }
        brokerService.removeV1Connection(sessionId);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        if (connectionInfo != null && alreadyClosed.compareAndSet(false, true) == true) {
        	
        }
        brokerService.removeV1Connection(sessionId);
        ctx.close();
    }
}
