package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.atakmap.Tak.FederatedEvent;
import tak.server.federation.FederateSslPreAuthCodec;
import com.bbn.marti.groups.DummyAuthenticator;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import tak.server.ignite.IgniteHolder;

/*
 */
public class NioNettyFederationServerHandler extends NioNettyHandlerBase {
	private final static Logger log = Logger.getLogger(NioNettyHandlerBase.class);
	protected Protocol<FederatedEvent> fedProto;
	private static final int INTBYTES = Integer.SIZE / Byte.SIZE;
	private AtomicBoolean alreadyClosed = new AtomicBoolean(true);
	private ByteBuffer leftovers = null;
	private int nextSize = -1;

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.pipeline()
				.get(SslHandler.class)
				.handshakeFuture()
				.addListener(new GenericFutureListener<Future<Channel>>() {
					@Override
					public void operationComplete(Future<Channel> future) throws Exception {
						if (future.isSuccess()) {							
							remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
							localSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
							nettyContext = ctx;				
							createConnectionInfo();
							createAdaptedNettyProtocol();
							createAdaptedNettyHandler(connectionInfo);
							((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyFederationServer");
							setReader();
							new FederateSslPreAuthCodec(null, DummyAuthenticator.getInstance()).handleOnConnect(connectionInfo);
							alreadyClosed.set(false);
							federationManager().handleOnConnect(channelHandler, fedProto);
						}else {
							ctx.close();
						}
					}
				});
	}

	@Override
	protected void createConnectionInfo() {
		connectionInfo = new ConnectionInfo();
		X509Certificate cert = (X509Certificate) getCertFromSslChain(0);
		X509Certificate caCert = (X509Certificate) getCertFromSslChain(1);
		connectionInfo.setCert(cert);
		connectionInfo.setCaCert(caCert);
		connectionInfo.setConnectionId(IgniteHolder.getInstance().getIgniteStringId() + MessageConversionUtil.getConnectionId((SocketChannel) nettyContext.channel()));
		if (nettyContext.channel().remoteAddress() instanceof InetSocketAddress) {
			InetSocketAddress addr = (InetSocketAddress) nettyContext.channel().remoteAddress();
			connectionInfo.setAddress(addr.getHostString());
			connectionInfo.setPort(addr.getPort());
		} else {
			connectionInfo.setAddress(nettyContext.channel().remoteAddress().toString());
		}
	}
	
	@Override
	protected void createAdaptedNettyProtocol() {
		fedProto = new AbstractBroadcastingProtocol<FederatedEvent>() {

			@Override
			public void negotiate() {
			}

			@Override
			public void onConnect(ChannelHandler handler) {
			}

			@Override
			public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
			}

			@Override
			public void onInboundClose(ChannelHandler handler) {
			}

			@Override
			public void onOutboundClose(ChannelHandler handler) {
			}

			@Override
			public AsyncFuture<Integer> write(FederatedEvent data, ChannelHandler handler) {
				if (nettyContext.channel().isWritable()) {
					byte[] eventBytes = data.toByteArray();
					ByteBuffer binaryData = ByteBuffer.allocate(INTBYTES + eventBytes.length);
					binaryData.putInt(eventBytes.length);
					binaryData.put(eventBytes);
					((Buffer) binaryData).rewind();
					AbstractBroadcastingChannelHandler.totalBytesWritten.getAndAdd(binaryData.array().length);
					AbstractBroadcastingChannelHandler.totalNumberOfWrites.getAndIncrement();
					((TcpChannelHandler) channelHandler).totalTcpBytesWritten.getAndAdd(binaryData.array().length);
					((TcpChannelHandler) channelHandler).totalTcpNumberOfWrites.getAndIncrement();
					connectionInfo.getProcessedCount().getAndIncrement();
					nettyContext.writeAndFlush(binaryData.array());
				}
				return null;
			}

		};
	}
	
	protected void setReader() {
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
					federationManager().handleOnDataReceived(event, channelHandler, fedProto);
				} catch (InvalidProtocolBufferException e) {
					log.error("parsing problem with Federated Event: " + e.getMessage());
					channelHandler.forceClose();
				}
			}
		};
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		if (connectionInfo != null && channelHandler != null && alreadyClosed.compareAndSet(false, true) == true) {
			messagingUtil().processFederateClose(connectionInfo, channelHandler, SubscriptionStore.getInstance().getByHandler(channelHandler));
		}
		ctx.close();
		super.channelUnregistered(ctx);

	}
}
