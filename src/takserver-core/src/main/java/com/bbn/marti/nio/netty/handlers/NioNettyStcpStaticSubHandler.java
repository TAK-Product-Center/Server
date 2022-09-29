package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.bbn.marti.config.Filter;
import com.bbn.marti.config.Input;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufProtocol;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.TransportCotEvent;
import com.bbn.marti.util.MessageConversionUtil;

import io.micrometer.core.instrument.Metrics;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import tak.server.Constants;
import tak.server.ignite.IgniteHolder;


/*
 * 
 */
public class NioNettyStcpStaticSubHandler extends NioNettyHandlerBase {
	private final static Logger log = Logger.getLogger(NioNettyStcpStaticSubHandler.class);
	private String uid;
	private String xpath;
	private User user;
	private Filter filter;
	private Subscription subscription;
	
	public NioNettyStcpStaticSubHandler(String uid, String protocolStr, String xpath, String name, User user, Filter filter) {
		
		this.input = new Input();
		
		this.uid = uid;
		this.xpath = xpath;
		this.user = user;
		this.filter = filter;
		
		TransportCotEvent transport = TransportCotEvent.findByID(protocolStr);
		
		// proto only channel so go right to proto without negotiation 		
		if (transport == TransportCotEvent.PROTOTLS) {
			protobufSupported.set(true);
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		localSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
		nettyContext = ctx;
		createConnectionInfo();
		createAdaptedNettyProtocol();
		createAdaptedNettyHandler(connectionInfo);
		((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyStcpStaticSubHandler");
		setWriter();
		setupFlushHandler();
		createSubscription();
		subscriptionManager().addFilterToSub(subscription, filter);
	}
	
	private void setWriter() {
		writer = (data) -> {

			if (nettyContext.channel().isWritable()) {
				
				byte[] bytesToWrite = null;
				if (!protobufSupported.get()) {
					bytesToWrite = data.getOrInstantiateEncoding();
				} else {
					if (data.getProtoBufBytes() != null) {
						
						if (log.isTraceEnabled()) {
							log.trace("preconverted proto array length: " + data.getProtoBufBytes().array().length);
						}
						bytesToWrite = data.getProtoBufBytes().array(); // use pre-converted message if possible
						
						Metrics.counter(Constants.METRIC_MESSAGE_PRECONVERT_COUNT, "takserver", "messaging").increment();
					} else {
						bytesToWrite = StreamingProtoBufProtocol.convertCotToProtoBufBytes(data).array();
					}
				}
				
				currentMessageCount.getAndIncrement();
				
				Metrics.counter(Constants.METRIC_MESSAGE_WRITE_COUNT, "takserver", "messaging").increment();
				
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
				
			} else {
				// do not broker - watermark skip
				Metrics.counter(Constants.METRIC_MESSAGE_WATERMARK_SKIP_COUNT, "takserver", "messaging").increment();
			}
		};
	}
	
	@Override
	protected void createSubscription() {		
		subscription = subscriptionManager().addSubscription(uid, protocol, channelHandler, xpath, user);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.info("NioNettyStcpStaticSubHandler error", cause);
		ctx.close();
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		log.info("Closing NioNettyStcpStaticSubHandler: " + remoteSocketAddress);
		
		if (connectionInfo != null) {
			if (connectionInfo.getConnectionId() != null) {
				AtomicBoolean cancelFlag = GroupFederationUtil.getInstance().updateCancelMap.get(connectionInfo.getConnectionId());

				if (cancelFlag != null) {
					cancelFlag.set(true);
				}
			}
		}

		if(channelHandler != null) {
			submissionService().handleChannelDisconnect(channelHandler);
			protocolListeners.forEach(listener -> listener.onOutboundClose(channelHandler, protocol));
		}
		
		ctx.close();
		
		super.channelUnregistered(ctx);

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

}
