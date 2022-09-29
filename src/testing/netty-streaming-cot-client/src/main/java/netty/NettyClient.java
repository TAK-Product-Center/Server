package netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/*
 * 
 * This is an example class of how to interface with a gRPC CoT streaming input on TAK Server.
 * The client in this example acts as an echo client. It will send back any messages it receives.
 * Any clients on TAK Server in the same groups as this client should received the echoed message.
 * 
 * Before Running, make sure to edit @port, @host, @keystoreFile, @keystorePassword, @truststoreFile, @truststorePassword
 * The keystore should contain the X509 client cert you want to used for authentication and group assignment
 * 
 */
public class NettyClient {
	private static final int NUM_AVAIL_CORES = Runtime.getRuntime().availableProcessors();
	public static int highMark;
	public static int lowMark;
	public static int flushThreshold;
	public static int maxOptimalMessagesPerMinute;
	private final boolean isEpoll;
	private EventLoopGroup workerGroup;
	private EventLoopGroup bossGroup;
	
	public static class ConnectionMeta {
		public int port = 8089;
		public String address = "localhost";
		
		String keyManager = "SunX509";	    
	    String keystoreType = "JKS";	
	    // takserver.jks
		String keystoreFile = "takserver.jks" ;
		String keystorePassword = "atakatak";
		 
		String truststoreType = "JKS";
		// truststore-root.jks
		String truststoreFile = "truststore-root.jks" ;
		String truststorePassword = "atakatak";
	}
	
	
	@FunctionalInterface
	public interface ConnectionLostCallback {
		void connectionLost();
	}
	
	public NettyClient() {
		isEpoll = Epoll.isAvailable();
		
		int baselineHighMark = 4096;
		int baselineMaxOptimalMessagesPerMinute = 250;
		
		highMark = baselineHighMark * NUM_AVAIL_CORES;
		lowMark = highMark / 2;
		flushThreshold = lowMark;
		maxOptimalMessagesPerMinute = baselineMaxOptimalMessagesPerMinute * NUM_AVAIL_CORES;
	}

	public void startClient(ConnectionMeta cm, NettyInitializer initializer) throws UnknownHostException {
		checkAndCreateEventLoopGroups();
		
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(cm.address), cm.port);

		new Thread(() -> {
			try {
				Bootstrap clientBootstrap = new Bootstrap();
				clientBootstrap.group(bossGroup)
						.channel(isEpoll ? EpollSocketChannel.class : NioSocketChannel.class)
						.remoteAddress(address)
						.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(lowMark, highMark))
						.handler(initializer);
				ChannelFuture channelFuture = clientBootstrap.connect().sync();
				channelFuture.channel().closeFuture().sync();
			} catch (Exception e) {
				System.out.println("error initializing netty client " + e);
			}
		}).start();
	}

	private void checkAndCreateEventLoopGroups() {
		if (bossGroup == null) {
            bossGroup = isEpoll ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        }
		if (workerGroup == null) {
            workerGroup = isEpoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        }
	}
}
