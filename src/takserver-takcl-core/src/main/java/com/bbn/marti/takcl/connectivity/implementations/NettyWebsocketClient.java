//package com.bbn.marti.takcl.connectivity.implementations;
//
//import atakmap.commoncommo.protobuf.v1.Takmessage;
//import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
//import com.bbn.marti.takcl.Util;
//import com.bbn.marti.test.shared.data.users.AbstractUser;
//import com.google.protobuf.CodedOutputStream;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.handler.codec.bytes.ByteArrayDecoder;
//import io.netty.handler.codec.bytes.ByteArrayEncoder;
//import io.netty.handler.codec.http.DefaultHttpHeaders;
//import io.netty.handler.codec.http.FullHttpResponse;
//import io.netty.handler.codec.http.HttpHeaders;
//import io.netty.handler.codec.http.websocketx.*;
//import io.netty.handler.ssl.*;
//import io.netty.util.CharsetUtil;
//import org.dom4j.DocumentException;
//import org.dom4j.DocumentHelper;
//import org.jetbrains.annotations.NotNull;
//import tak.server.cot.CotEventContainer;
//import tak.server.proto.StreamingProtoBufHelper;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.nio.Buffer;
//import java.nio.ByteBuffer;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.security.GeneralSecurityException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Consumer;
//
//import static io.netty.handler.codec.http.websocketx.WebSocketVersion.*;
//
//public class NettyWebsocketClient implements ConnectibleTakprotoClient.TakWebsocketClientInterface {
//
//	public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
//
//		private final WebSocketClientHandshaker handshaker;
//		private ChannelPromise handshakeFuture;
//
//		public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
//			this.handshaker = handshaker;
//		}
//
//		public ChannelFuture handshakeFuture() {
//			return handshakeFuture;
//		}
//
//		@Override
//		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//			handshakeFuture = ctx.newPromise();
//		}
//
//		@Override
//		public void channelActive(ChannelHandlerContext ctx) throws Exception {
//			handshaker.handshake(ctx.channel());
//		}
//
//		@Override
//		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//			System.out.println("WebSocket Client disconnected!");
//		}
//
//		@Override
//		public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
//			Channel ch = ctx.channel();
//			if (!handshaker.isHandshakeComplete()) {
//				handshaker.finishHandshake(ch, (FullHttpResponse) msg);
//				System.out.println("WebSocket Client connected!");
//				handshakeFuture.setSuccess();
//				return;
//			}
//
//			if (msg instanceof FullHttpResponse) {
//				FullHttpResponse response = (FullHttpResponse) msg;
//				throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.getStatus() + ", content="
//						+ response.content().toString(CharsetUtil.UTF_8) + ')');
//			}
//
//			WebSocketFrame frame = (WebSocketFrame) msg;
//			System.err.println("FRAME TYPE: " + frame.getClass().toString());
//			if (frame instanceof TextWebSocketFrame) {
//				TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
//				System.out.println("WebSocket Client received message: " + textFrame.text());
//			} else if (frame instanceof PongWebSocketFrame) {
//				System.out.println("WebSocket Client received pong");
//			} else if (frame instanceof CloseWebSocketFrame) {
//				System.out.println("WebSocket Client received closing");
//				ch.close();
//			}
//		}
//
//		@Override
//		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//			cause.printStackTrace();
//
//			if (!handshakeFuture.isDone()) {
//				handshakeFuture.setFailure(cause);
//			}
//
//			ctx.close();
//		}
//	}
//
//
//	/**
//	 * Creates a new {@link WebSocketClientHandshaker} of desired protocol version.
//	 */
//	public static final class WebSocketClientHandshakerFactory {
//
//		/**
//		 * Private constructor so this static class cannot be instanced.
//		 */
//		private WebSocketClientHandshakerFactory() {
//		}
//
//		/**
//		 * Creates a new handshaker.
//		 *
//		 * @param webSocketURL    URL for web socket communications. e.g "ws://myhost.com/mypath".
//		 *                        Subsequent web socket frames will be sent to this URL.
//		 * @param version         Version of web socket specification to use to connect to the server
//		 * @param subprotocol     Sub protocol request sent to the server. Null if no sub-protocol support is required.
//		 * @param allowExtensions Allow extensions to be used in the reserved bits of the web socket frame
//		 * @param customHeaders   Custom HTTP headers to send during the handshake
//		 */
//		public static WebSocketClientHandshaker newHandshaker(
//				URI webSocketURL, WebSocketVersion version, String subprotocol,
//				boolean allowExtensions, HttpHeaders customHeaders) {
//			return newHandshaker(webSocketURL, version, subprotocol, allowExtensions, customHeaders, 65536);
//		}
//
//		/**
//		 * Creates a new handshaker.
//		 *
//		 * @param webSocketURL          URL for web socket communications. e.g "ws://myhost.com/mypath".
//		 *                              Subsequent web socket frames will be sent to this URL.
//		 * @param version               Version of web socket specification to use to connect to the server
//		 * @param subprotocol           Sub protocol request sent to the server. Null if no sub-protocol support is required.
//		 * @param allowExtensions       Allow extensions to be used in the reserved bits of the web socket frame
//		 * @param customHeaders         Custom HTTP headers to send during the handshake
//		 * @param maxFramePayloadLength Maximum allowable frame payload length. Setting this value to your application's
//		 *                              requirement may reduce denial of service attacks using long data frames.
//		 */
//		public static WebSocketClientHandshaker newHandshaker(
//				URI webSocketURL, WebSocketVersion version, String subprotocol,
//				boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength) {
//			if (version == V13) {
//				return new WebSocketClientHandshaker13(
//						webSocketURL, V13, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
//			}
//			if (version == V08) {
//				return new WebSocketClientHandshaker08(
//						webSocketURL, V08, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
//			}
//			if (version == V07) {
//				return new WebSocketClientHandshaker07(
//						webSocketURL, V07, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
//			}
//			if (version == V00) {
//				return new WebSocketClientHandshaker00(
//						webSocketURL, V00, subprotocol, customHeaders, maxFramePayloadLength);
//			}
//
//			throw new WebSocketHandshakeException("Protocol version " + version.toString() + " not supported.");
//		}
//	}
//
//	//		private final String serverUrl;
//	private final URI serverUri;
//	private final String baseUrl;
//	//		private final int serverPort;
//	private final Path truststoreJksPath;
//	private final Path userCertPrivateJksPath;
//	private final Consumer<String> responseListener;
//	private final Util.DurationLogger dl;
//	private EventLoopGroup group;
//	private Channel ch;
//
//	private ByteBuffer convertStringToProtoBufBuffer(String xml) {
//		ByteBuffer buffer;
//		try {
//			dl.begin("String to ByteBuffer");
//			CotEventContainer data = new CotEventContainer(DocumentHelper.parseText(xml));
//
//			//
//			// Convert CotEventContainer to protobuf
//			//
//			Takmessage.TakMessage takMessage = StreamingProtoBufHelper.getInstance().cot2protoBuf(data);
//			if (takMessage == null) {
//				System.err.println("cot2protoBuf failed to parse message!");
//				return null;
//			}
//
//			//
//			// allocate a buffer for the message
//			//
//			int takMessageSize = takMessage.getSerializedSize();
//			if (takMessageSize > ConnectibleTakprotoClient.SessionIdFetcher.MAX_SIZE) {
//				throw new RuntimeException(("TOO BIG!"));
//			}
//
//			int sizeOfSize = CodedOutputStream.computeUInt32SizeNoTag(takMessageSize);
//			buffer = ByteBuffer.allocate(1 + sizeOfSize + takMessageSize);
//
//			//
//			// write out the message to the buffer
//			//
//			CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(buffer);
//			codedOutputStream.write(StreamingProtoBufHelper.MAGIC);
//			codedOutputStream.writeUInt32NoTag(takMessageSize);
//			takMessage.writeTo(codedOutputStream);
//			((Buffer) buffer).rewind();
//			dl.end("String to ByteBuffer");
//
//		} catch (DocumentException | IOException e) {
//			throw new RuntimeException(e);
//		}
//		return buffer;
//	}
//
//	private NettyWebsocketClient(String serverUrl, int serverPort, Path truststoreJksPath,
//	                             Path userCertPrivateJksPath, Consumer<String> responseListener, Util.DurationLogger dl) {
//		this.baseUrl = "https://" + serverUrl + ":" + serverPort;
//		try {
//			this.serverUri = new URI(baseUrl + "/takproto/1");
//		} catch (URISyntaxException e) {
//			throw new RuntimeException(e);
//		}
//		this.truststoreJksPath = truststoreJksPath;
//		this.userCertPrivateJksPath = userCertPrivateJksPath;
//		this.responseListener = responseListener;
//		this.dl = dl;
//	}
//
//	private void connect() {
//
//		try {
////			String baseUrl = "https://" + serverUrl + ":" + serverPort;
////			URI uri = new URI(baseUrl);
//			ConnectibleTakprotoClient.TakClientSslContext tcsc = new ConnectibleTakprotoClient.TakClientSslContext(truststoreJksPath.toFile(), userCertPrivateJksPath.toFile());
//
////					KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
////					KeyStore ks = KeyStore.getInstance("JKS");
////					ks.load(new FileInputStream(jks), "atakatak".toCharArray());
////
////					keyManagerFactory.init(ks, "atakatak".toCharArray());
////					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
////					KeyStore ts = KeyStore.getInstance("JKS");
////					ts.load(new FileInputStream(tsr), "atakatak".toCharArray());
////
////					SslContext ctx = SslContextBuilder.forClient()
////							.keyManager(keyManagerFactory)
////							.trustManager(trustManagerFactory)
////							.clientAuth(ClientAuth.REQUIRE)
////							.sslProvider(SslProvider.OPENSSL)
////							.build();
//
//
//			SslContext ctx = SslContextBuilder.forClient()
//					.trustManager(tcsc.getTrustManagerFactory())
////					.trustManager(tsr)
////					.trustManager(truststoreJksPath.toFile())
////					.keyManager(tcsc.keyManager)
////					.sslContextProvider(tcsc.getKeyManagerFactory().getProvider())
////					.keyManager(tcsc.getKeyManagerFactory())
//					.keyManager(tcsc.getKeyManagerFactory())
//					.clientAuth(ClientAuth.REQUIRE)
//					.sslProvider(SslProvider.OPENSSL)
////					.keyManager(null, userCertPrivateJksPath.toFile())
//					.build();
//
//
//			ConnectibleTakprotoClient.SessionIdFetcher sif = new ConnectibleTakprotoClient.SessionIdFetcher(tcsc, baseUrl);
//			String cookie = sif.getSessionId();
//
//			Map<String, String> headers = new HashMap<>();
//			headers.put("Cookie", cookie);
////			TakWebsocketClient client = new TakWebsocketClient(new URI(baseUrl + TAKPROTO_PATH), headers, responseListener, dl);
////			client.setTcpNoDelay(true);
////			client.setSocketFactory(tcsc.getSslSocketFactory());
//
//
//			group = new NioEventLoopGroup();
//			Bootstrap b = new Bootstrap();
////				String protocol = uri.getScheme();
////				if (!"ws".equals(protocol)) {
////					throw new IllegalArgumentException("Unsupported protocol: " + protocol);
////				}
//
//
//			HttpHeaders customHeaders = new DefaultHttpHeaders();
//			customHeaders.add("Cookie", cookie);
//
//			// Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
//			// If you change it to V00, ping is not supported and remember to change
//			// HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
//			final WebSocketClientHandler handler =
//					new WebSocketClientHandler(
//							WebSocketClientHandshakerFactory.newHandshaker(
//									serverUri, WebSocketVersion.V13, null, false, customHeaders));
//
//			b.group(group)
//					.channel(NioSocketChannel.class)
//					.handler(new ChannelInitializer<SocketChannel>() {
//						@Override
//						public void initChannel(SocketChannel ch) throws Exception {
//							SslHandler sslHandler = ctx.newHandler(ch.alloc());
////								SslHandler sslHandler = ctx.newHandler(ch.alloc(), serverUri.getHost() + "/takproto/1", serverUri.getPort());
//
////								String[] cipherSuites = {"TLSv1.2"};
////								sslHandler.engine()
////										.setEnabledCipherSuites(cipherSuites);
//
//							ChannelPipeline pipeline = ch.pipeline();
//							pipeline.addLast("ssl", sslHandler)
//									.addLast(new ByteArrayDecoder())
//									.addLast(new ByteArrayEncoder());
////								pipeline.addLast(ctx.newHandler(ch.alloc(), serverUri.getHost(), serverUri.getPort()));
////								pipeline.addLast(
////										new HttpClientCodec(),
////										new HttpObjectAggregator(8192),
////										                              WebSocketClientCompressionHandler.INSTANCE,
////										handler);
//
////								pipeline.addLast("http-codec", new HttpClientCodec());
////								pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
////								pipeline.addLast("ws-handler", handler);
//						}
//					});
//
//			System.out.println("WebSocket Client connecting");
////				ch = b.connect(serverUri.getHost() + ":8443/takproto/1", serverUri.getPort()).sync().channel();
//
//			ChannelFuture cf = b.connect(serverUri.getHost(), serverUri.getPort());
////				cf.wait(10000);
////				cf.await();
////				cf.wait();
//			ch = cf.sync().channel();
//
//
////						.sync().channel();
////				ChannelFuture hf = handler.handshakeFuture();
////				hf.await();
//			handler.handshakeFuture().sync();
//
//			// Send 10 messages and wait for responses
//			System.out.println("WebSocket Client sending message");
//			for (int i = 0; i < 10; i++) {
//				ch.writeAndFlush(new TextWebSocketFrame("Message #" + i));
//			}
//
//			// Ping
////				System.out.println("WebSocket Client sending ping");
////				ch.writeAndFlush(new PingWebSocketFrame(Unpooled.copiedBuffer(new byte[]{1, 2, 3, 4, 5, 6})));
//
//			// Close
////				System.out.println("WebSocket Client sending close");
////				ch.writeAndFlush(new CloseWebSocketFrame());
//
//			// WebSocketClientHandler will close the connection when the server
//			// responds to the CloseWebSocketFrame.
////				ch.closeFuture().sync();
//		} catch (InterruptedException | IOException e) {
//			throw new RuntimeException(e);
////			} finally {
////				group.shutdownGracefully();
//		}
//	}
//
//	protected static NettyWebsocketClient buildAndConnectWebsocketClient(AbstractUser user, Consumer<String> responseListener, Util.DurationLogger dl) throws GeneralSecurityException, IOException, URISyntaxException {
//		return buildAndConnectWebsocketClient(user.getServer().getUrl(), user.getServer().getCertHttpsPort(),
//				Paths.get(TAKCLConfigModule.getInstance().getTruststoreJKSFilepath()),
//				user.getCertPrivateJksPath(),
//				responseListener, dl);
//	}
//
//	protected static NettyWebsocketClient buildAndConnectWebsocketClient(String serverUrl, int serverPort,
//	                                                                     Path truststoreJksPath, Path userCertPrivateJksPath, Consumer<String> responseListener, Util.DurationLogger dl) throws GeneralSecurityException, IOException, URISyntaxException {
//		NettyWebsocketClient client = new NettyWebsocketClient(serverUrl, serverPort, truststoreJksPath, userCertPrivateJksPath, responseListener, dl);
//		client.connect();
//		return client;
//	}
//
//	public void send(@NotNull String xmlData) {
//		ByteBuffer bb = convertStringToProtoBufBuffer(xmlData);
//		ch.writeAndFlush(bb);
//	}
//
//	@Override
//	public boolean isOpen() {
//		return ch.isOpen();
//	}
//
//	@Override
//	public boolean isClosed() {
//		return !isOpen();
//	}
//
//	@Override
//	public void close() {
//		try {
//			ch.closeFuture().sync();
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		} finally {
//			group.shutdownGracefully();
//		}
//	}
//
//	@Override
//	public void closeBlocking() throws InterruptedException {
//		try {
//			ch.closeFuture().sync();
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		} finally {
//			group.shutdownGracefully();
//		}
//	}
//}
