package grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jetty.util.BlockingArrayQueue;

import com.atakmap.Tak.ClientSubscription;
import com.atakmap.Tak.InputChannelGrpc;
import com.atakmap.Tak.InputChannelGrpc.InputChannelStub;
import com.google.common.base.Strings;

import atakmap.commoncommo.protobuf.v1.Takmessage;
import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.concurrent.DefaultThreadFactory;


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
public class GrpcClientInputTest {

	private TrustManagerFactory trustMgrFactory;
	private KeyManagerFactory keyMgrFactory;
	private ManagedChannel channel;
	private InputChannelStub asyncChannel;
	private ClientCall<TakMessage, ClientSubscription> clientCall;

	private final String keystoreType = "JKS";
	private final String truststoreType = "JKS";
	private final String keyManager = "SunX509";

	// TODO EDIT THESE TO FIT YOUR ENV
	private final String host = "localhost";
	private final int port = 30001;
	private final String[] tlsVersions = { "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3" };
	// path to user cert to use for x509 auth: <usercert.jks>
	private final String keystoreFile = "";
	private final String keystorePassword = "";
	// path to trust store: <truststore.jks>
	private final String truststoreFile = "";
	private final String truststorePassword = "";

	// Bounded Executor pool for grpc input server and channel builders
	private final BlockingQueue<Runnable> workQueue = new BlockingArrayQueue<>(0, 1, 1000);
	public final ExecutorService grpcInputExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, workQueue, new DefaultThreadFactory("grpc-input-executor", true));
	// Bounded worker pool for grpc input server and channel builders
	public final EventLoopGroup grpcInputWorkerEventLoopGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("grpc-input-worker", true));

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
		GrpcClientInputTest client = new GrpcClientInputTest();
		try {
			client.testGrpc();
		} catch (Exception e) {
			System.out.println("" + e);
		}

		while (true) {
		}
	}

	public void testGrpc() throws Exception {
		SslContext sslContext = buildClientSslContext();

		channel = NettyChannelBuilder.forAddress(host, port)
				.negotiationType(NegotiationType.TLS)
				.executor(grpcInputExecutor)
				.eventLoopGroup(grpcInputWorkerEventLoopGroup)
				.sslContext(sslContext)
				.channelType(NioSocketChannel.class)
				.build();

		asyncChannel = InputChannelGrpc.newStub(channel);

		openSendStream();
		openReceiveStream();

		while (true) {
		}
	}

	public void openSendStream() {
		clientCall = channel.newCall(
				io.grpc.MethodDescriptor.create(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING,
						generateFullMethodName("com.atakmap.InputChannel", "ServerTakMessageStream"),
						io.grpc.protobuf.ProtoUtils
								.marshaller(atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage.getDefaultInstance()),
						io.grpc.protobuf.ProtoUtils
								.marshaller(com.atakmap.Tak.ClientSubscription.getDefaultInstance())),
				asyncChannel.getCallOptions());

		// use listener to respect flow control, and send messages to the server when it
		// is ready
		clientCall.start(new ClientCall.Listener<ClientSubscription>() {

			@Override
			public void onMessage(ClientSubscription message) {
				clientCall.request(1);
			}

			@Override
			public void onReady() {
			}

		}, new Metadata());

		// Notify gRPC to receive one response. Without this line, onMessage() would
		// never be called.
		clientCall.request(1);
	}

	public void openReceiveStream() {
		asyncChannel.clientTakMessageStream(null, new StreamObserver<Takmessage.TakMessage>() {

			@Override
			public void onNext(Takmessage.TakMessage value) {
				// ECHO
				clientCall.sendMessage(value);
			}

			@Override
			public void onError(Throwable t) {
				t.printStackTrace();
			}

			@Override
			public void onCompleted() {
			}
		});
	}

	private SslContext buildClientSslContext() throws Exception {
		if (Strings.isNullOrEmpty(keyManager)) {
			throw new IllegalArgumentException("empty key manager configuration");
		}

		keyMgrFactory = KeyManagerFactory.getInstance(keyManager);

		if (Strings.isNullOrEmpty(keystoreType)) {
			throw new IllegalArgumentException("empty keystore type");
		}

		KeyStore self = KeyStore.getInstance(keystoreType);

		if (Strings.isNullOrEmpty(keystoreFile)) {
			throw new IllegalArgumentException("keystore file name empty");
		}

		if (Strings.isNullOrEmpty(keystorePassword)) {
			throw new IllegalArgumentException("empty keystore password");
		}

		try (FileInputStream fis = new FileInputStream(keystoreFile)) {
			// Filename of the keystore file
			self.load(fis, keystorePassword.toCharArray());
		}

		// Password of the keystore file
		keyMgrFactory.init(self, keystorePassword.toCharArray());

		// Trust Manager Factory type (e.g., ??)
		trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		KeyStore trust = KeyStore.getInstance(truststoreType);
		trust.load(new FileInputStream(truststoreFile), truststorePassword.toCharArray());
		trustMgrFactory.init(trust);

		return GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
				.protocols(tlsVersions)
				.keyManager(keyMgrFactory)
				.trustManager(trustMgrFactory)
				.build();
	}
}
