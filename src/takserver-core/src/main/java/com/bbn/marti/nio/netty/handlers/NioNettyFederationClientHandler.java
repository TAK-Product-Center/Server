package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.bbn.marti.config.Federation.FederationOutgoing;

import tak.server.federation.DistributedFederationManager;
import tak.server.federation.FederateSslPreAuthCodec;
import com.bbn.marti.groups.DummyAuthenticator;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.ConnectionStatusValue;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.SubscriptionStore;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/*
 */
public class NioNettyFederationClientHandler extends NioNettyFederationServerHandler {
	private final static Logger log = Logger.getLogger(NioNettyFederationClientHandler.class);
	private AtomicBoolean alreadyClosed = new AtomicBoolean(true);
	private String outgoingName;
	private ConnectionStatus status;
	private String federationError = "";
	private AtomicBoolean duplicateActiveConnection = new AtomicBoolean(false);

	
	public NioNettyFederationClientHandler(FederationOutgoing outgoing, ConnectionStatus status) {
		this.outgoingName = outgoing.getDisplayName();
		this.status = status;
	}
	
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.pipeline()
				.get(SslHandler.class)
				.handshakeFuture()
				.addListener(new GenericFutureListener<Future<Channel>>() {
					@Override
					public void operationComplete(Future<Channel> future) throws Exception {
						if(future.isSuccess()) {
							nettyContext = ctx;	
							remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
							localSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
							
							String fingerprint = RemoteUtil.getInstance().getCertSHA256Fingerprint((X509Certificate) getCertFromSslChain(0));
							
							SubscriptionStore.getInstanceFederatedSubscriptionManager()
								.getFederateSubscriptions()
								.forEach(federateSubscription ->{
									if (federateSubscription.getUser() instanceof FederateUser) {
										FederateUser fedUser = (FederateUser) federateSubscription.getUser();
										// there is an active connection from the same cert, mark this connection as duplicate 										
										if (fedUser.getFederateConfig().getId().equals(fingerprint)) {
											duplicateActiveConnection.set(true);
										}
									}
								});
							
							if (duplicateActiveConnection.get()) {
								ctx.channel().close();
								DistributedFederationManager.getInstance().disableOutgoing(outgoingName);
								status.setConnectionStatusValue(ConnectionStatusValue.DISABLED);
								status.setLastError("duplicate federation connection");
								SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoingName, status);
								return;
							}
														
							status.setConnectionStatusValue(ConnectionStatusValue.CONNECTED);
							status.setLastError("");
		
							SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoingName, status);
							createConnectionInfo();
							connectionInfo.setClient(true);
							createAdaptedNettyProtocol();
							createAdaptedNettyHandler(connectionInfo);
							((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyFederationClient");
							setReader();
							new FederateSslPreAuthCodec(null, DummyAuthenticator.getInstance()).handleOnConnect(connectionInfo);
							alreadyClosed.set(false);
							federationManager.handleOnConnect(channelHandler, fedProto);
						}else {
							federationError = future.cause().getMessage();
							log.info("NioNettyFederationHandler error connecting to federate " + federationError);
							ctx.close();
						}
					}
				});
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		federationError = cause.getMessage();
		log.error("NioNettyFederationHandler error connecting to federate", cause);
		ctx.close();
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		if (connectionInfo != null && channelHandler != null && alreadyClosed.compareAndSet(false, true) == true) {
			messagingUtil.processFederateClose(connectionInfo, channelHandler, SubscriptionStore.getInstance().getByHandler(channelHandler));
		}
		
		if (status.getConnectionStatusValue() != ConnectionStatusValue.RETRY_SCHEDULED && !duplicateActiveConnection.get()) {
			federationManager.checkAndSetReconnectStatus(federationManager.getOutgoingConnection(outgoingName), federationError);
		}
				
		ctx.close();
		
		super.channelUnregistered(ctx);

	}
}
