package com.bbn.marti.nio.grpc;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.ClientSubscription;
import com.atakmap.Tak.ClientVersion;
import com.atakmap.Tak.InputChannelGrpc;
import com.atakmap.Tak.ServerVersion;
import com.bbn.marti.config.Input;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.service.Resources;
import com.google.common.primitives.Longs;

import atakmap.commoncommo.protobuf.v1.Takmessage;
import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import tak.server.Constants;

/*
 */
public class GrpcStreamingServer {
	private static final Logger logger = LoggerFactory.getLogger(GrpcStreamingServer.class);
	private final Map<String, NioGrpcChannelHandler> clientHandlerMap = new ConcurrentHashMap<>();
	private Server server;
	private Input input;

	public void start(Input input, WriteBufferWaterMark waterMark, SslContext sslContext) {
		this.input = input;
		
		NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(input.getPort())
				.sslContext(sslContext)
				.executor(Resources.grpcInputExecutor)
				.workerEventLoopGroup(Resources.grpcInputWorkerEventLoopGroup)
				.bossEventLoopGroup(Resources.grpcInputWorkerEventLoopGroup)
				.withChildOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark)
				.channelType(NioServerSocketChannel.class);
				
		GrpcStreamingService service = new GrpcStreamingService();
		
		server = serverBuilder
				.addService(ServerInterceptors.intercept(service, tlsInterceptor()))
				.build();

		try {
			server.start();
			logger.info("Started GrpcStreamingServer on port " + input.getPort());
		} catch (IOException e) {
			logger.info("Could not start GrpcStreamingServer " + input);
		}
	}

	private class GrpcStreamingService extends InputChannelGrpc.InputChannelImplBase {
		private final GrpcStreamingServer grpcStreamingServer = GrpcStreamingServer.this;

		// sender
		@Override
		public void clientTakMessageStream(ClientSubscription request, StreamObserver<TakMessage> responseObserver) {
			NioGrpcChannelHandler handler = new NioGrpcChannelHandler(input, responseObserver, getCurrentClientCert(),
					getCurrentLocalSocketAddress(), getCurrentRemoteSocketAddress());
			handler.channelActive(null);
			grpcStreamingServer.clientHandlerMap.put(getCurrentSessionId(), handler);
		}

		// receiver
		@Override
		public StreamObserver<TakMessage> serverTakMessageStream(StreamObserver<ClientSubscription> responseObserver) {
			return new StreamObserver<Takmessage.TakMessage>() {

				@Override
				public void onNext(TakMessage message) {
					try {
						grpcStreamingServer.clientHandlerMap.get(getCurrentSessionId()).submitTakMessage(message);
					} catch (Exception e) {
						logger.debug("Could not submit cot to NioGrpcChannelHandler");
					}
				}

				@Override
				public void onError(Throwable t) {
					NioGrpcChannelHandler handler = grpcStreamingServer.clientHandlerMap.remove(getCurrentSessionId());
					if (handler != null) {
						handler.channelUnregistered(null);
					}
				}

				@Override
				public void onCompleted() {
					NioGrpcChannelHandler handler = grpcStreamingServer.clientHandlerMap.remove(getCurrentSessionId());
					if (handler != null) {
						handler.channelUnregistered(null);
					}
				}
			};
		}

		@Override
		public void versionCheck(ClientVersion request, StreamObserver<ServerVersion> responseObserver) {
			// TODO determine compatibility based on server + client versions
			responseObserver.onNext(ServerVersion.newBuilder()
					.setVersion(Constants.GRPC_INPUT_CHANNEL_VERSION)
					.setCompatible(true)
					.build());
			responseObserver.onCompleted();
		}
	}

	public void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

	final static private Context.Key<SSLSession> sslSessionKey = Context.key("SSLSession");
	final static private Context.Key<SocketAddress> localSocketAddressKey = Context.key("LocalSocketAddressKey");
	final static private Context.Key<SocketAddress> remoteSocketAddressKey = Context.key("RemoteSocketAddressKey");
	final static private Context.Key<Long> sslSessionIdKey = Context.key("SSLSessionId");


	public static ServerInterceptor tlsInterceptor() {

		return new ServerInterceptor() {

			@Override
			public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
					final Metadata requestHeaders, ServerCallHandler<ReqT, RespT> next) {

				SSLSession sslSession = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
				if (sslSession == null) {
					return next.startCall(call, requestHeaders);
				}

				Context context = Context.current().withValue(sslSessionKey, sslSession)
						.withValue(sslSessionIdKey, Longs.fromByteArray(sslSession.getId()))
						.withValue(remoteSocketAddressKey, call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR))
						.withValue(localSocketAddressKey, call.getAttributes().get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR));
				
				return Contexts.interceptCall(context, call, requestHeaders, next);
			}
		};
	}

	private String getCurrentSessionId() {
		try {
			return sslSessionIdKey.get(Context.current()).toString();
		} catch (Exception e) {
			throw new TakException(e);
		}
	}

	private InetSocketAddress getCurrentRemoteSocketAddress() {
		try {
			return (InetSocketAddress) remoteSocketAddressKey.get(Context.current());
		} catch (Exception e) {
			throw new TakException(e);
		}
	}

	private InetSocketAddress getCurrentLocalSocketAddress() {
		try {
			return (InetSocketAddress) localSocketAddressKey.get(Context.current());
		} catch (Exception e) {
			throw new TakException(e);
		}
	}

	private X509Certificate getCurrentClientCert() {
		try {

			SSLSession session = sslSessionKey.get(Context.current());

			Certificate[] clientCertArray = requireNonNull(requireNonNull(session, "SSL Session").getPeerCertificates(),
					"SSL peer certs array");

			if (clientCertArray == null || clientCertArray.length == 0) {
				throw new IllegalArgumentException("invalid client cert array");
			}

			Certificate clientCert = requireNonNull(clientCertArray[0], "v2 fed client cert");

			return (X509Certificate) clientCert;

		} catch (Exception e) {
			throw new TakException(e);
		}
	}

}
