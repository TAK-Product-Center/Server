package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;

import com.bbn.marti.config.Network.Input;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;

import io.netty.channel.ChannelHandlerContext;

public class NioNettyTcpServerHandler extends NioNettyStcpServerHandler {

	public NioNettyTcpServerHandler(Input input) {
		super(input);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		localSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
		nettyContext = ctx;
		createConnectionInfo();
		createAdaptedNettyProtocol();
		createAdaptedNettyHandler(connectionInfo);
		((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyTCP");
		buildCallbacks();
		createAuthenticationCodecs();
		setReader();
		setWriter();
		setNegotiator();
	}
	
	
	@Override
	protected void setWriter() {
		writer = (data) -> {};
	}
	
	@Override
	protected void setNegotiator() {
		negotiator = () -> {};
	}
}
