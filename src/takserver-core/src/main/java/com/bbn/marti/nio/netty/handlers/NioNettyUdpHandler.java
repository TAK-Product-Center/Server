package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.bbn.marti.config.Input;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.connections.UdpDataChannelHandler;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.nio.protocol.connections.SingleProtobufOrCotProtocol;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

import io.micrometer.core.instrument.Metrics;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;

public class NioNettyUdpHandler extends MessageToMessageDecoder<DatagramPacket> {
	private final static Logger log = Logger.getLogger(NioNettyUdpHandler.class);
	protected MessagingUtilImpl messagingUtil;
	protected Protocol<CotEventContainer> protocol;
	protected Input input;
	protected ConcurrentLinkedQueue<ProtocolListener<CotEventContainer>> protocolListeners;

	public NioNettyUdpHandler(Input input) {
		this.input = input;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {

		ReferenceCountUtil.retain(packet.content());

		ByteBuffer buffer = packet.content().nioBuffer();
		
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
	        
	        AbstractBroadcastingProtocol<CotEventContainer> anp = createAdaptedNettyProtocol(handler);

			Resources.udpReadParseProcessor.submit(() -> {
				try {
					final CotEventContainer cot =
							SingleProtobufOrCotProtocol.byteBufToCot(buffer, handler);
					if (cot != null) {
						if (anp != null) {
							Resources.udpReadDataReceivedProcessor.submit(() -> {
								anp.getProtocolListeners().forEach(listener -> listener.onDataReceived(cot, handler, protocol));
								Metrics.counter(Constants.METRIC_MESSAGE_READ_COUNT_UDP, "takserver", "messaging").increment();
							});
						}
					}
				} catch (Exception e) {
					log.error("exception in udpReadParseProcessor", e);
				} finally {
					ReferenceCountUtil.release(packet.content());
				}
			});

		} catch (Exception e) {
			log.error("cot error", e);
			ReferenceCountUtil.release(packet.content());
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

		if (input.isFederateOnly()) {
			protocol.addProtocolListener(
					SubmissionService.InputListenerAuxillaryRouter.onFederateOnlyDataReceivedCallback
							.newInstance(channelHandler, protocol));
		}

		protocol.addProtocolListener(SubmissionService.getInstance().onDataReceivedCallback.newInstance(channelHandler, protocol));
		
		return protocol;
	}
}
