package com.bbn.marti.nio.netty;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;

import com.bbn.marti.config.Filter;
import com.bbn.marti.config.Federation.FederationOutgoing;
import com.bbn.marti.config.Federation.FederationServer;
import com.bbn.marti.config.Network.Input;
import com.bbn.marti.nio.grpc.GrpcStreamingServer;
import com.bbn.marti.nio.netty.handlers.NioNettyTcpStaticSubHandler;
import com.bbn.marti.nio.netty.initializers.NioNettyInitializer;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.ConnectionStatusValue;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.Resources;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

import io.micrometer.core.lang.NonNull;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import tak.server.cot.CotEventContainer;
import tak.server.federation.DistributedFederationManager;

/*
 */
public class NioNettyBuilder implements Serializable {
	private static final long serialVersionUID = 602601050590817673L;
	private final static Logger log = Logger.getLogger(NioNettyBuilder.class);
	private static final int NUM_AVAIL_CORES = Runtime.getRuntime().availableProcessors();
	public static int highMark;
	public static int lowMark;
	public static int flushThreshold;
	public static int maxOptimalMessagesPerMinute;
	private final boolean isEpoll;
	private EventLoopGroup workerGroup;
	private EventLoopGroup bossGroup;
	private final DistributedConfiguration config = DistributedConfiguration.getInstance();
	private final FederationServer federationServer = config.getRemoteConfiguration().getFederation().getFederationServer();
	private final Map<Integer, ChannelFuture> portToNettyServer = new ConcurrentHashMap<>();
	private final Map<Integer, GrpcStreamingServer> portToGrpcServer = new ConcurrentHashMap<>();
	private final Map<Integer, Input> portToInput = new ConcurrentHashMap<>();

	private static NioNettyBuilder instance = null;

	public static synchronized NioNettyBuilder getInstance() {
		if (instance == null) {
			synchronized (NioNettyBuilder.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(NioNettyBuilder.class);
				}
			}
		}

		return instance;
	}
	
	public NioNettyBuilder() {
		isEpoll = Epoll.isAvailable() && DistributedConfiguration.getInstance().getRemoteConfiguration().getNetwork().isUseLinuxEpoll();
		
		int baselineHighMark = 4096;
		int baselineMaxOptimalMessagesPerMinute = 250;
		
		highMark = baselineHighMark * NUM_AVAIL_CORES;
		lowMark = highMark / 2;
		flushThreshold = lowMark;
		maxOptimalMessagesPerMinute = baselineMaxOptimalMessagesPerMinute * NUM_AVAIL_CORES;
	}

	public void buildFederationServer() {
		startServer(NioNettyInitializer.Pipeline.initializer.federationServer(federationServer), federationServer.getPort());
	}
	
	public void buildTcpServer(@NonNull Input input) {
		startServer(NioNettyInitializer.Pipeline.initializer.tcpServer(input), input.getPort());
	}

	public void buildStcpServer(@NonNull Input input) {
		startServer(NioNettyInitializer.Pipeline.initializer.stcpServer(input), input.getPort());
	}

	public void buildTlsServer(@NonNull Input input) {
		startServer(NioNettyInitializer.Pipeline.initializer.tlsServer(input, config.getSecurity().getTls()), input.getPort());
	}
	
	public void buildGrpcServer(@NonNull Input input) {
		WriteBufferWaterMark waterMark = new WriteBufferWaterMark(lowMark, highMark);
		SslContext sslContext = NioNettyInitializer.Pipeline.initializer.grpcServer(input, config.getSecurity().getTls()).getSslContext();
		GrpcStreamingServer grpcStreamingServer = new GrpcStreamingServer();
		grpcStreamingServer.start(input, waterMark, sslContext);
		portToGrpcServer.put(input.getPort(), grpcStreamingServer);
	}

	public void stopServer(int port) {
		Optional.ofNullable(portToNettyServer.get(port)).ifPresent(future -> future.channel().close());
		Optional.ofNullable(portToGrpcServer.get(port)).ifPresent(grpcServer -> grpcServer.stop());
		portToInput.remove(port);
		portToGrpcServer.remove(port);
	}

	private void startServer(NioNettyInitializer initializer, int port) {
		checkAndCreateEventLoopGroups();

		new Thread(() -> {
			try {
				ServerBootstrap bootstrap = new ServerBootstrap();
				bootstrap.group(bossGroup, workerGroup)
						.channel(isEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
						.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
						.childHandler(initializer)
						.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(lowMark, highMark))
						.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

				portToNettyServer.put(port, bootstrap.bind(port).sync().channel().closeFuture());

				if (initializer.getInput() != null) {
					portToInput.put(port, initializer.getInput());
				}

				log.info("Successfully Started Netty Server for " + initializer + " on Port " + port + " with watermark " + new WriteBufferWaterMark(lowMark, highMark));

			} catch (Exception e) {
				log.error("Error initializing netty server ", e);
			}
		}).start();
	}

	public void buildFederationClient(FederationOutgoing outgoing, ConnectionStatus status) throws UnknownHostException {
		startClient(NioNettyInitializer.Pipeline.initializer.federationClient(federationServer, outgoing, status),
				new InetSocketAddress(InetAddress.getByName(outgoing.getAddress()), outgoing.getPort()),
				(e) -> {
					if(status.getConnectionStatusValue() != ConnectionStatusValue.RETRY_SCHEDULED) {
						DistributedFederationManager.getInstance().checkAndSetReconnectStatus(outgoing, e.getMessage());
					}
				});
	}
	

	public void modifyServerInput(Input input) {
		Input oldInput = portToInput.get(input.getPort());
		if (input != null && oldInput != null) {
			oldInput = input;
		}
	}
	
	public void buildStcpStaticSubClient(String host, int port, String uid, String protocolStr, String xpath, String name, User user, Filter filter) throws UnknownHostException {
		startClient(NioNettyInitializer.Pipeline.initializer.stcpStaticSubClient(uid, protocolStr, xpath, name, user, filter),
				new InetSocketAddress(InetAddress.getByName(host), port),
				(e) -> {
					log.info("Unable to create stcp static sub client connection to " + host + ":" + port);
				});
	}

	
	// maintain a connectionless handler that stores static sub meta data, so that when its time to write, we can create a network connection 
	// to the remote address
	public void buildTcpStaticSubClient(String host, int port, String uid, String protocolStr, String xpath, String name, User user, Filter filter) throws UnknownHostException {
		new NioNettyTcpStaticSubHandler(host, port, uid, protocolStr, xpath, name, user, filter);
	}
	
	public void submitCotToTcpStaticSubClient(CotEventContainer cot, boolean isProto, InetSocketAddress remoteAddress) {
		checkAndCreateEventLoopGroups();
		
		Resources.tcpStaticSubProcessor.execute(() -> {
			try {
				Bootstrap clientBootstrap = new Bootstrap();
				clientBootstrap.group(bossGroup)
						.channel(isEpoll ? EpollSocketChannel.class : NioSocketChannel.class)
						.remoteAddress(remoteAddress)
						.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(lowMark, highMark))
						.handler( NioNettyInitializer.Pipeline.initializer.tcpStaticSubClient(cot, isProto));
				ChannelFuture channelFuture = clientBootstrap.connect().sync();
				channelFuture.channel().closeFuture().sync();
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.error("error initializing netty static sub client ", e);
				}
			}
		});
	}

	private void startClient(NioNettyInitializer initializer, InetSocketAddress remoteAddress, ErrorHandler errorHandler) {
		checkAndCreateEventLoopGroups();

		new Thread(() -> {
			try {
				Bootstrap clientBootstrap = new Bootstrap();
				clientBootstrap.group(bossGroup)
						.channel(isEpoll ? EpollSocketChannel.class : NioSocketChannel.class)
						.remoteAddress(remoteAddress)
						.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(lowMark, highMark))
						.handler(initializer);
				ChannelFuture channelFuture = clientBootstrap.connect().sync();
				channelFuture.channel().closeFuture().sync();
			} catch (Exception e) {
				log.error("error initializing netty client ", e);
				errorHandler.handle(e);
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

	@FunctionalInterface
	private interface ErrorHandler {
		public void handle(Exception e);
	}

	@PreDestroy
	private void preDestroy() {
		if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
		if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
	}
}
