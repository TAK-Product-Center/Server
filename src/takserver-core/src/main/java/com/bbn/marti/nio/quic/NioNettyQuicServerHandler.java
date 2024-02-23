package com.bbn.marti.nio.quic;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.bbn.marti.config.Input;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyTlsServerHandler;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufOrCoTProtocol;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.ConnectionInfo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.quic.QuicChannel;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;

public class NioNettyQuicServerHandler extends NioNettyTlsServerHandler {
	private final static Logger log = Logger.getLogger(NioNettyQuicServerHandler.class);
	
	private final Map<String, InetSocketAddress> clientAddressMap;
	
	public NioNettyQuicServerHandler(Input input, Map<String, InetSocketAddress> clientAddressMap) {
		super(input);
		this.clientAddressMap = clientAddressMap;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		try {			
			remoteSocketAddress = clientAddressMap.get(ctx.channel().parent().id().asLongText());
			localSocketAddress = new InetSocketAddress("localhost", input.getPort());
						
			nettyContext = ctx;						
			createConnectionInfo();
			createAdaptedNettyProtocol();
			createAdaptedNettyHandler(connectionInfo);
			((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyQuic");
			createAuthenticationCodecs();
			setReader();
			setWriter();
			setNegotiator();
			buildCallbacks();
			setupFlushHandler();
			createSubscription();
		} catch (Exception e) {
			log.error("exception processing channel activation", e);
		}

	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		super.channelUnregistered(ctx);
		// if the stream closes, force close the entire connection
		ctx.channel().parent().close();
	}

	@Override
	protected void createConnectionInfo() {
		connectionInfo = new ConnectionInfo();
		connectionInfo.setAddress(remoteSocketAddress.getHostString());
		connectionInfo.setConnectionId(IgniteHolder.getInstance().getIgniteStringId() + new Integer(nettyContext.channel().parent().hashCode()).toString());
		connectionInfo.setPort(remoteSocketAddress.getPort());
		connectionInfo.setTls(true);
		connectionInfo.setClient(true);
		connectionInfo.setInput(input);
		connectionInfo.setCert(getCertFromSslChain(0));
	}

	
	@Override
	protected void setNegotiator() {
		negotiator = () -> {			
			if (authCodec != null && authCodec.getAuthStatus().get() != AuthStatus.SUCCESS) return;

			try {
				CotEventContainer announcement = StreamingProtoBufOrCoTProtocol
						.buildProtocolAnnouncement(negotiationUuid = UUID.randomUUID().toString());

				nettyContext.writeAndFlush(announcement.getOrInstantiateEncoding());
			} catch (DocumentException e) {
				log.error(e);
			}
		};
	}
	
	@Override
	protected X509Certificate getCertFromSslChain(int index) {
		X509Certificate cert = null;
		try {
			QuicChannel channel = (QuicChannel) nettyContext.channel().parent();
			cert = (X509Certificate) channel.sslEngine().getSession().getPeerCertificates()[index];
		} catch (SSLPeerUnverifiedException e) {
			if(log.isDebugEnabled()) {
				log.debug("Could not get cert at from chain at index " + index, e);
			}
		}
		return cert;
	}
	
	public String getQuicChannelId() {
		return nettyContext.channel().parent().id().asLongText();
	}
	
	public void close() {
		try {
			nettyContext.close();
		} catch (Exception e) {
			log.error("Error closing stream channel", e);
		}
		
		try {
			nettyContext.channel().parent().close();
		} catch (Exception e) {
			log.error("Error closing connection channel", e);
		}
	}
}
