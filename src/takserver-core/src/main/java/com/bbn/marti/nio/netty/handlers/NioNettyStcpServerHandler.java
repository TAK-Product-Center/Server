package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;

import com.bbn.cot.filter.DataFeedFilter;
import com.bbn.marti.config.AuthType;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Input;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.codec.impls.AbstractAuthCodec;
import com.bbn.marti.nio.codec.impls.AnonymousAuthCodec;
import com.bbn.marti.nio.codec.impls.FileAuthCodec;
import com.bbn.marti.nio.codec.impls.LdapAuthCodec;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.nio.protocol.connections.StreamingCotProtocol;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.util.MessageConversionUtil;
import com.google.common.base.Charsets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import tak.server.ignite.IgniteHolder;

/*
 */
public class NioNettyStcpServerHandler extends NioNettyHandlerBase {
	private AbstractAuthCodec authCodec;
	private AuthType authenticationType;
	protected ScheduledFuture<?> flushFuture;

	public NioNettyStcpServerHandler(Input input) {
		this.input = input;
		this.authenticationType = input.getAuth();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		localSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
		nettyContext = ctx;
		createConnectionInfo();
		createAdaptedNettyProtocol();
		createAdaptedNettyHandler(connectionInfo);
		((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettySTCP");
		buildCallbacks();
		createAuthenticationCodecs();
		setReader();
		setWriter();
		setNegotiator();
		setupFlushHandler();
		createSubscription();
	}

	@Override
	protected void createConnectionInfo() {
		connectionInfo = new ConnectionInfo();
		connectionInfo.setAddress(remoteSocketAddress.getHostString());
		connectionInfo.setConnectionId(IgniteHolder.getInstance().getIgniteStringId() + MessageConversionUtil.getConnectionId((SocketChannel) nettyContext.channel()));
		connectionInfo.setPort(remoteSocketAddress.getPort());
		connectionInfo.setClient(true);
		connectionInfo.setInput(input);
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		if(channelHandler != null) {			
			submissionService.handleChannelDisconnect(channelHandler);
			protocolListeners.forEach(listener -> listener.onOutboundClose(channelHandler, protocol));
		}
		
		if (authCodec != null) {
			authCodec.onInboundClose();
			authCodec.onOutboundClose();
		}
		
		ctx.close();
		
		super.channelUnregistered(ctx);

	}

	protected void setReader() {
		reader = (msg) -> {

			ByteBuffer msgBuf = ByteBuffer.wrap(msg);

			if (authCodec != null) {
				msgBuf = authCodec.decode(msgBuf);
			}
			
			if(msgBuf == null || msgBuf.remaining() == 0) return;
			
			StreamingCotProtocol
				.add(builder, Charsets.UTF_8.decode(msgBuf), cotParser(), channelHandler)
				.forEach(c -> {
					if (isNotDOSLimited(c)  && isNotReadLimited(c)) {
						if (isDataFeedInput()) {
							DataFeedFilter.getInstance().filter(c, (DataFeed) input);
						}
 						protocolListeners.forEach(listener -> listener.onDataReceived(c, channelHandler, protocol));
					}
				});
		};
	}

	protected void setWriter() {
		writer = (data) -> {
			if (nettyContext.channel().isWritable()) {
				byte[] bytesToWrite = data.getOrInstantiateEncoding();
				
				currentMessageCount.getAndIncrement();
				
				// flush if instant flush is set or if queued bytes > flushThreshold
				if (isInstantFlush.get() || (NioNettyBuilder.highMark - nettyContext.channel().bytesBeforeUnwritable()) > NioNettyBuilder.flushThreshold) {
					nettyContext.writeAndFlush(bytesToWrite);
				} else {
					nettyContext.write(bytesToWrite);
				}	
				
				AbstractBroadcastingChannelHandler.totalBytesWritten.getAndAdd(bytesToWrite.length);
				AbstractBroadcastingChannelHandler.totalNumberOfWrites.getAndIncrement();
				((TcpChannelHandler) channelHandler).totalTcpBytesWritten.getAndAdd(bytesToWrite.length);
				((TcpChannelHandler) channelHandler).totalTcpNumberOfWrites.getAndIncrement();
			}
		};
	}

	protected void setNegotiator() {
		negotiator = () -> {};
	}
	
	protected void createAuthenticationCodecs() {
		if (authenticationType == AuthType.FILE) {
			authCodec = new FileAuthCodec(createAdaptedNettyPipelineContext());
			authCodec.setConnectionInfo(connectionInfo);
			authCodec.onConnect();
		} else if (authenticationType == AuthType.LDAP) {
			authCodec = new LdapAuthCodec(createAdaptedNettyPipelineContext());
		 	authCodec.setConnectionInfo(connectionInfo);
			authCodec.onConnect();
		} else if(authenticationType == AuthType.ANONYMOUS) {
			authCodec = new AnonymousAuthCodec(createAdaptedNettyPipelineContext(), input);
			authCodec.setConnectionInfo(connectionInfo);
			authCodec.onConnect();
		}
	}
	
}
