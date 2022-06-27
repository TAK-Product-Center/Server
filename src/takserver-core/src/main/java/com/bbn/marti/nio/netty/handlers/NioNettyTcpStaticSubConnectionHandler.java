package com.bbn.marti.nio.netty.handlers;

import org.apache.log4j.Logger;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufProtocol;

import io.micrometer.core.instrument.Metrics;
import io.netty.channel.ChannelHandlerContext;

/*
 * 
 * This class is used for making network writes to the TCP Static subscription remote address
 * when a write is triggered on the {@NioNettyTcpStaticSubHandler}. Will close after the
 * write is fully delivered
 * 
 */
public class NioNettyTcpStaticSubConnectionHandler extends NioNettyHandlerBase {
	private final static Logger log = Logger.getLogger(NioNettyTcpStaticSubConnectionHandler.class);
	
	private final CotEventContainer cot;
	
	public NioNettyTcpStaticSubConnectionHandler(CotEventContainer cot, boolean isProto) {
		this.cot = cot;
		this.protobufSupported.set(isProto);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		byte[] bytesToWrite = null;
		if (!protobufSupported.get()) {
			bytesToWrite = cot.getOrInstantiateEncoding();
		} else {
			bytesToWrite = StreamingProtoBufProtocol.convertCotToProtoBufBytes(cot).array();
		}
					
		Metrics.counter(Constants.METRIC_MESSAGE_WRITE_COUNT, "takserver", "messaging").increment();

		ctx.writeAndFlush(bytesToWrite).addListener((future) -> {
			ctx.close();
		});
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.info("NioNettyTcpStaticSubConnectionHandler error", cause);
		ctx.close();
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		ctx.close();
	}

	@Override
	protected void createConnectionInfo() {}
}
