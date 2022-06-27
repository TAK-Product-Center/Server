

package com.bbn.marti.nio.channel.connections;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.util.concurrent.future.AsyncFuture;


public class TcpChannelHandler extends AbstractBroadcastingChannelHandler {
	private final static Logger log = LoggerFactory.getLogger(TcpChannelHandler.class);
	
	public AtomicLong totalTcpBytesWritten = new AtomicLong();
	public AtomicLong totalTcpBytesRead = new AtomicLong();
	public AtomicLong totalTcpNumberOfWrites = new AtomicLong();
	public AtomicLong totalTcpNumberOfReads = new AtomicLong();

	@Override
	public boolean handleRead(SelectableChannel channel, Server server, ByteBuffer buffer) {
		return false;
	}


	@Override
	public boolean handleWrite(SelectableChannel channel, Server server, ByteBuffer buffer) {
		return false;
	}

	@Override
	public AsyncFuture<ChannelHandler> connect() {
		log.info("connect");
		return null;
	}
   

	@Override
	public AsyncFuture<Integer> write(ByteBuffer buffer) {
		log.info("write");
		return null;
	}

	@Override
	public AsyncFuture<ChannelHandler> close() {
		log.info("close");
		return null;
	}


	@Override
	public void forceClose() {
		log.info("forceClose");
	}


	@Override
	public String netProtocolName() {
		if (connectionInfo.isTls()) {
			return "tls";
		} else {
			return "tcp";
		}
	}

	@Override
	public String toString() {
		return "TCP Channel server on local port " + localPort() + " client: " + host() + ":" + port();
	}

}
