package tak.server.federation.hub.broker;

import com.atakmap.Tak.FederatedEvent;

import com.bbn.roger.fig.FederationUtils;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.HashMap;
import java.util.Set;

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

    public Set<FederatedEvent> getCache() {
        return cache;
    }

    private FederationHubBrokerService brokerService;

    public NioNettyFederationHubServerHandler(FederationHubBrokerService brokerService,
            Comparator<FederatedEvent> comp) {
        super();
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
                    try {
                        SslHandler sslhandler = (SslHandler)nettyContext.channel().pipeline().get("ssl");
                        certArray = sslhandler.engine().getSession().getPeerCertificates();
                    } catch (SSLPeerUnverifiedException e) {
                        logger.error("Could not get certificate chain", e);
                        ctx.close();
                        return;
                    }

                    createConnectionInfo();
                    String fedId = ((X509Certificate)certArray[0]).getSubjectDN().toString() +
                        "-" + FederationUtils.getBytesSHA256(certArray[0].getEncoded());
                    federateIdentity = new FederateIdentity(fedId);
                    brokerService.addCaFederateToPolicyGraph(federateIdentity,
                        certArray);
                    setReader();
                    alreadyClosed.set(false);
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
                        brokerService.assignMessageSourceAndDestinationsFromPolicy(federatedMessage,
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
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        if (connectionInfo != null && alreadyClosed.compareAndSet(false, true) == true) {
            /* TODO Perform federate close. */
            //messagingUtil.processFederateClose(connectionInfo, channelHandler, SubscriptionStore.getInstance().getByHandler(channelHandler));
        }
        ctx.close();
    }
}
