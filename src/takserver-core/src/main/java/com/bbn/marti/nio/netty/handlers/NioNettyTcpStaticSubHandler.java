package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.bbn.marti.config.Filter;

import tak.server.ignite.IgniteHolder;

import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.TransportCotEvent;

import io.netty.channel.ChannelHandlerContext;

/*
 * 
 * This class acts as a holder for TCP static subscription state.
 * TCP Static Subscriptions are open-send-close, so we keep the subscription alive
 * here until deleted from the UI, and open a temporary connection to send
 * data from in {@NioNettyTcpStaticSubConnectionHandler}
 * 
 */
public class NioNettyTcpStaticSubHandler extends NioNettyHandlerBase {
	private final static Logger log = Logger.getLogger(NioNettyTcpStaticSubHandler.class);
	private String uid;
	private String xpath;
	private User user;
	private Subscription subscription;
	
	public NioNettyTcpStaticSubHandler(String host, int port, String uid, String protocolStr, String xpath, String name, User user, Filter filter) {
		this.uid = uid;
		this.xpath = xpath;
		this.user = user;
		
		TransportCotEvent transport = TransportCotEvent.findByID(protocolStr);
		
		// proto only channel so go right to proto without negotiation 		
		if (transport == TransportCotEvent.PROTOTLS) {
			protobufSupported.set(true);
		}
		
		remoteSocketAddress = new InetSocketAddress(host, port);

		createConnectionInfo();
		createAdaptedNettyProtocol();
		createAdaptedNettyHandler(connectionInfo);
		((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyTCPClient");
		setWriter();
		createSubscription();
		subscriptionManager().addFilterToSub(subscription, filter);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {}
	
	private void setWriter() {
		writer = (data) -> {
			NioNettyBuilder.getInstance().submitCotToTcpStaticSubClient(data, protobufSupported.get(), remoteSocketAddress);
		};
	}
	
	@Override
	protected void createSubscription() {		
		subscription = subscriptionManager().addSubscription(uid, protocol, channelHandler, xpath, user);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.info("NioNettyTcpClientHandler error", cause);
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {}

	@Override
	protected void createConnectionInfo() {
		connectionInfo = new ConnectionInfo();
		connectionInfo.setAddress(remoteSocketAddress.getHostString());
		connectionInfo.setConnectionId(IgniteHolder.getInstance().getIgniteStringId() + System.identityHashCode(this));
		connectionInfo.setPort(remoteSocketAddress.getPort());
		connectionInfo.setClient(true);
		connectionInfo.setInput(input);
	}
}
