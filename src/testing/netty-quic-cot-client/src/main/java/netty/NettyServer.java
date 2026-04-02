package netty;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicTokenHandler;


public class NettyServer {
	private static final int NUM_AVAIL_CORES = Runtime.getRuntime().availableProcessors();
	public static int highMark;
	public static int lowMark;
	public static int flushThreshold;
	public static int maxOptimalMessagesPerMinute;

	public static class ConnectionMeta {
		public int port = 9010;
		public String address = "localhost";
		
		String keyManager = "SunX509";	    
	    String keystoreType = "JKS";	
	    // takserver.jks
		String keystoreFile = "/opt/tak/certs/files/takserver.jks" ;
		String keystorePassword = "atakatak";
		 
		String truststoreType = "JKS";
		// truststore-root.jks
		String truststoreFile = "/opt/tak/certs/files/truststore-root.jks" ;
		String truststorePassword = "atakatak";
	}
	
	ConnectionMeta cm = new ConnectionMeta();
	
	
	@FunctionalInterface
	public interface ConnectionLostCallback {
		void connectionLost();
	}
	
	public NettyServer() {

	}
	
	public void buildQuicServer() {
		new Thread(() -> {
			try {
				QuicSslContext context = buildServerSslContext();
						
				NioEventLoopGroup group = new NioEventLoopGroup(1);
				ChannelHandler codec = new QuicServerCodecBuilder().sslContext(context)
						.initialMaxData(10000000)
						.initialMaxStreamDataBidirectionalLocal(1000000)
						.initialMaxStreamDataBidirectionalRemote(1000000)
						.initialMaxStreamsBidirectional(100)
						.initialMaxStreamsUnidirectional(100)
						.handler(new ChannelInboundHandlerAdapter() {
							 @Override
								public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
									super.userEventTriggered(ctx, evt);
									System.out.println("Connection event: " + evt);
								}
							
							@Override
							public void channelActive(ChannelHandlerContext ctx) {
								QuicChannel channel = (QuicChannel) ctx.channel();
								System.out.println("Channel active " + channel.id().asLongText());

								
							}

							@Override
							public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
								super.channelUnregistered(ctx);
								System.out.println("Channel active " + ctx.channel().id().asLongText());
							}

							@Override
							public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
								super.exceptionCaught(ctx, cause);
								System.out.println("Connection Error: " + cause);
							}

							@Override
							public boolean isSharable() {
								return true;
							}
							
							
						})
						
						.streamHandler(new ChannelInitializer<QuicStreamChannel>() {
							@Override
		                    protected void initChannel(QuicStreamChannel ch)  {
		                        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

								SimpleChannelInboundHandler<byte[]> handler = new SimpleChannelInboundHandler<byte[]>() {
	                        
									public void d() {
										
									}
									
									@Override
									protected void channelRead0(ChannelHandlerContext ctx, byte[] msg)
											throws Exception {
										System.out.println("Stream Read: " + new String(msg));						
									}
									
									@Override
									public void channelActive(ChannelHandlerContext ctx) {
										System.out.println("stream active " +  ctx.channel().parent().id().asLongText());
									}

									@Override
									public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
										super.channelUnregistered(ctx);
										System.out.println("stream active " + ctx.channel().parent().id().asLongText());
									}
									
								};
								
		                        // Add a LineBasedFrameDecoder here as we just want to do some simple HTTP 0.9 handling.
		                        ch.pipeline()
		                        	.addLast(new ByteArrayDecoder())
		                        	.addLast(new ByteArrayEncoder())
		                        	.addLast(handler);
		                        
		                       
		                    }
		                }).build();
				try {
					System.out.println("Starting Server...");
					Bootstrap bs = new Bootstrap();
					Channel channel = bs.group(group)
							.channel(NioDatagramChannel.class)
							.handler(codec)
							.bind(new InetSocketAddress(cm.port)).sync().channel();
					channel.closeFuture().sync();
				} finally {
					group.shutdownGracefully();
				}
			}
			catch (Exception e) {
				System.out.println("quic server error " + e);
			}
		}).start();
	}
	
	private QuicSslContext sslContext;
	private TrustManagerFactory trustMgrFactory;
	private KeyManagerFactory keyMgrFactory;
	
	private QuicSslContext buildServerSslContext() {
		try {
			initTrust();
		} catch (Exception e) {
			System.out.println("Could not init trust " + e);
		}
		
		sslContext = QuicSslContextBuilder.forServer(keyMgrFactory, cm.keystorePassword)
			.trustManager(trustMgrFactory)
			.clientAuth(ClientAuth.REQUIRE)
			.applicationProtocols("takstream")
			.earlyData(true)
			.build();

		return sslContext;
	}
	
	private void initTrust() throws Exception {
		String keyManager = cm.keyManager;
	    keyMgrFactory = KeyManagerFactory.getInstance(keyManager);
	    
	    String keystoreType = cm.keystoreType;
	    
	    if (keystoreType == null || keystoreType == "") {
	        throw new IllegalArgumentException("empty keystore type");
	    }
	    
	    KeyStore self = KeyStore.getInstance(keystoreType);
		
		String keystoreFile = cm.keystoreFile;
		
		if (keystoreFile == null || keystoreFile == "") {
		    throw new IllegalArgumentException("keystore file name empty");
		}
		
		String keystorePassword = cm.keystorePassword;
		
		if (keystorePassword == null || keystorePassword == "") {
		    throw new IllegalArgumentException("empty keystore password");
		}

		try(FileInputStream fis = new FileInputStream(keystoreFile)) {
			// Filename of the keystore file
			self.load(fis, keystorePassword.toCharArray());
		}

		// Password of the keystore file
		keyMgrFactory.init(self, keystorePassword.toCharArray());

		// Trust Manager Factory type (e.g., ??)
		trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		// initialize trust store
		initTrust(trustMgrFactory);
	}

	public KeyStore initTrust(TrustManagerFactory trustMgrFactory) {
	    
	    KeyStore trust = null;
	    
	    try {
	        String truststoreType = cm.truststoreType;

	        if (truststoreType == null || truststoreType == "") {
	            throw new IllegalArgumentException("empty truststore type");
	        }

	        // Truststore type (same as keystore types - e.g., "JKS")
	        trust = KeyStore.getInstance(truststoreType);


	        String truststoreFile = cm.truststoreFile;

	        if (truststoreFile == null || truststoreFile == "") {
	            throw new IllegalArgumentException("empty truststore file name");
	        }

	        String truststorePassword = cm.truststorePassword;

	        if (truststorePassword == null || truststorePassword == "") {
	            throw new IllegalArgumentException("empty truststore password");
	        }

	        try(FileInputStream fis = new FileInputStream(truststoreFile)) {
				// Filename of the truststore file
				trust.load(fis, truststorePassword.toCharArray());
			}
	        
	        // NOTE we are not adding any cert revocations like TAK Server does	        
	        trustMgrFactory.init(trust);

        
	    } catch (Exception e) {
            System.out.println("exception initializing trust store " + e);
        }
	   
	    
	    return trust;
	    
	}
}
