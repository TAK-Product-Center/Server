package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.bbn.cot.CotParserCreator;
import com.bbn.marti.config.Input;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.connections.UdpDataChannelHandler;
import com.bbn.marti.nio.channel.connections.UdpServerChannelHandler;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.nio.protocol.connections.SingleCotProtocol;
import com.bbn.marti.nio.protocol.connections.SingleProtobufOrCotProtocol;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.service.TransportCotEvent;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.google.common.base.Strings;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.federation.DistributedFederationManager;
import tak.server.qos.MessageDeliveryStrategy;

public class NioNettyUdpHandler extends MessageToMessageDecoder<DatagramPacket> {
	private final static Logger log = Logger.getLogger(NioNettyUdpHandler.class);
	protected MessagingUtilImpl messagingUtil;
	protected DistributedConfiguration config;
	protected Protocol<CotEventContainer> protocol;
	protected volatile CotParser parser;
	protected Input input;
	protected ConcurrentLinkedQueue<ProtocolListener<CotEventContainer>> protocolListeners;
	protected AtomicBoolean protoSupported = new AtomicBoolean(false);
	
	public NioNettyUdpHandler(Input input) {
		this.input = input;
		this.parser = CotParserCreator.newInstance();
		
		TransportCotEvent transport = TransportCotEvent.findByID(input.getProtocol());
		if (transport == TransportCotEvent.COTPROTOMUDP)
			this.protoSupported.set(true);
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
		try {
			InetSocketAddress sender = packet.sender();

			ConnectionInfo connectionInfo = new ConnectionInfo();
          
            connectionInfo.setConnectionId(MessageConversionUtil.getConnectionId(input));
            connectionInfo.setAddress(sender.getAddress().toString());
            connectionInfo.setTls(false);

            UdpDataChannelHandler handler = (UdpDataChannelHandler) new UdpDataChannelHandler()
                    .withAddress(sender)
                    .withLocalPort(input.getPort())
                    .withConnectionInfo(connectionInfo);

            handler.withInput(input);
            
            InputMetric inputMetric = SubmissionService.getInstance().getInputMetric(input.getName());
            if (inputMetric != null) {
                inputMetric.getMessagesReceived().incrementAndGet();
            }
                        
            if (protoSupported.get()) {
            	CotEventContainer cot = SingleProtobufOrCotProtocol.byteBufToCot(packet.content().nioBuffer(), handler, parser);
            	if (cot != null)
            		 createAdaptedNettyProtocol(handler).getProtocolListeners().forEach(listener -> listener.onDataReceived(cot, handler, protocol));
            } else {
            	CotEventContainer cot = SingleCotProtocol.byteBufToCot(packet.content().nioBuffer(), handler, parser);
            	if (cot != null)
            		createAdaptedNettyProtocol(handler).getProtocolListeners().forEach(listener -> listener.onDataReceived(cot, handler, protocol));
            }
   		} catch (Exception e) {
			log.error("cot error",e);
		}
	}
	
	protected AbstractBroadcastingProtocol<CotEventContainer> createAdaptedNettyProtocol(ChannelHandler channelHandler) {
		AbstractBroadcastingProtocol<CotEventContainer> protocol = new AbstractBroadcastingProtocol<CotEventContainer>() {
			@Override
			public void negotiate() {}

			@Override
			public void onConnect(ChannelHandler handler) {}

			@Override
			public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {}

			@Override
			public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
				return null;
			}

			@Override
			public void onInboundClose(ChannelHandler handler) {}

			@Override
			public void onOutboundClose(ChannelHandler handler) {}

		};
		
		if (input.isArchiveOnly()) {
			protocol.addProtocolListener(
					SubmissionService.InputListenerAuxillaryRouter.onArchiveOnlyDataReceivedCallback
							.newInstance(channelHandler, protocol));
		}

		if (!input.isArchive()) {
			protocol.addProtocolListener(SubmissionService.InputListenerAuxillaryRouter.onNoArchiveDataReceivedCallback
					.newInstance(channelHandler, protocol));
		}

		protocol.addProtocolListener(SubmissionService.getInstance().onDataReceivedCallback.newInstance(channelHandler, protocol));
		
		return protocol;
	}
}
