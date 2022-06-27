package tak.server.federation.hub.broker;

import static java.util.Objects.requireNonNull;

import com.atakmap.Tak.*;
import tak.server.federation.GuardedStreamHolder;
import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;
import tak.server.federation.FederationPolicyGraph;
import com.bbn.roger.fig.FederationUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import mil.af.rl.rol.*;
import mil.af.rl.rol.value.Parameters;
import mil.af.rl.rol.value.ResourceDetails;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.FederationHubUtils;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.policy.FederationHubPolicyManagerProxyFactory;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map.Entry;

@SpringBootApplication
public class FederationHubBrokerService implements CommandLineRunner, ApplicationListener<RestartServerEvent> {

    private static final String DEFAULT_CONFIG_FILE = "/opt/tak/federation-hub/configs/federation-hub-broker.yml";

    private static final String SSL_SESSION_ID = "sslSessionId";
    private static final String FEDERATED_ID_KEY = "federatedIdentity";

    private static final Logger logger = LoggerFactory.getLogger(FederationHubBrokerService.class);

    private static Ignite ignite = null;

    private static String configFile;

    @Autowired
    private FederationHubServerConfig fedHubConfig;

    /* v1 variables. */
    private final Map<String, NioNettyFederationHubServerHandler> v1ClientStreamMap = new ConcurrentHashMap<>();
    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;
    private ChannelFuture channelFuture;

    /* v2 variables. */
    private Server server;

    @Autowired
    private SSLConfig sslConfig;

    private final Map<String, GuardedStreamHolder<FederatedEvent>> clientStreamMap = new ConcurrentHashMap<>();
    private final Map<String, GuardedStreamHolder<ROL>> clientROLStreamMap = new ConcurrentHashMap<>();
    private final Map<String, String> clientROLStreamNames = new ConcurrentHashMap<>();

    private SyncService syncService = new FileCacheSyncService();

    private final AtomicBoolean keepRunning = new AtomicBoolean(true);

    private static ServerHealth serving;
    private static ServerHealth notConnected;

    @Autowired
    private FederationHubPolicyManager fedHubPolicyManager;

    // Count messages from all clients, over the lifetime of the server.
    private static final AtomicLong clientMessageCounter = new AtomicLong();
    private static final AtomicLong clientByteAccumulator = new AtomicLong();

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -jar federation-hub-broker.jar [CONFIG_FILE_PATH]");
            return;
        } else if (args.length == 1) {
            configFile = args[0];
        } else {
            configFile = DEFAULT_CONFIG_FILE;
        }

        SpringApplication application = new SpringApplication(FederationHubBrokerService.class);

        ignite = Ignition.getOrStart(FederationHubUtils.getIgniteConfiguration(
           FederationHubConstants.FEDERATION_HUB_BROKER_IGNITE_PROFILE,
           true));
        if (ignite == null) {
            System.exit(1);
        }

        serving = ServerHealth
            .newBuilder()
            .setStatus(ServerHealth.ServingStatus.SERVING)
            .build();
        notConnected = ServerHealth
            .newBuilder()
            .setStatus(ServerHealth.ServingStatus.NOT_CONNECTED)
            .build();

        ApplicationContext context = application.run(args);
    }

    @Bean
    public FederationHubDependencyInjectionProxy dependencyProxy() {
        return new FederationHubDependencyInjectionProxy();
    }

    @Override
    public void run(String... args) throws Exception {
        FederationHubBrokerImpl hb = new FederationHubBrokerImpl();
        ClusterGroup cg = ignite.cluster().forAttribute(
            FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
            FederationHubConstants.FEDERATION_HUB_BROKER_IGNITE_PROFILE);
        ignite.services(cg).deployClusterSingleton(
            FederationHubConstants.FED_HUB_BROKER_SERVICE, hb);
    }

    @Bean
    public Ignite getIgnite() {
        return ignite;
    }

    @Bean
    public FederationHubPolicyManagerProxyFactory fedHubPolicyManagerProxyFactory() {
        return new FederationHubPolicyManagerProxyFactory();
    }

    public FederationHubPolicyManager fedHubPolicyManager() throws Exception {
        return fedHubPolicyManagerProxyFactory().getObject();
    }

    private FederationHubServerConfig loadConfig(String configFile)
            throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
        if (getClass().getResource(configFile) != null) {
            // It's a resource.
            return new ObjectMapper(new YAMLFactory()).readValue(getClass().getResourceAsStream(configFile),
                FederationHubServerConfig.class);
        }

        // It's a file.
        return new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(configFile),
            FederationHubServerConfig.class);
    }

    private void removeInactiveClientStreams() {
        if (logger.isDebugEnabled()) {
            logger.debug("Running inactivity check for {} client streams", clientStreamMap.size());
        }

        for (Map.Entry<String, GuardedStreamHolder<FederatedEvent>> clientStreamEntry :
                clientStreamMap.entrySet()) {
            if (!clientStreamEntry.getValue().isClientHealthy(fedHubConfig.getClientTimeoutTime())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Detected FederatedEvent client stream {} inactivity",
                        clientStreamEntry.getValue().getFederateIdentity());
                }

                clientStreamEntry.getValue().throwDeadlineExceptionToClient();
                clientStreamMap.remove(clientStreamEntry.getKey());
            }
        }

        for (Map.Entry<String, GuardedStreamHolder<ROL>> rolStreamEntry :
                clientROLStreamMap.entrySet()) {
            if (!rolStreamEntry.getValue().isClientHealthy(fedHubConfig.getClientTimeoutTime())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Detected ROL client stream {} inactivity",
                        rolStreamEntry.getValue().getFederateIdentity());
                }

                try {
                    rolStreamEntry.getValue();
                } catch (Exception e) { }

                rolStreamEntry.getValue().throwDeadlineExceptionToClient();
                clientROLStreamMap.remove(rolStreamEntry.getKey());
            }
        }
    }

    /* TODO find place to call this. */
    public void stopV1Server() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        if (channelFuture != null) {
            channelFuture.channel().close();
            channelFuture = null;
        }
    }

    private SslContext buildServerSslContext(FederationHubServerConfig fedHubConfig) throws Exception {
        KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance(fedHubConfig.getKeyManagerType());
        TrustManagerFactory trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        try {
            String keyManager = fedHubConfig.getKeyManagerType();
            if (Strings.isNullOrEmpty(keyManager)) {
                throw new IllegalArgumentException("Empty key manager configuration");
            }

            String keystoreType = fedHubConfig.getKeystoreType();
            if (Strings.isNullOrEmpty(keystoreType)) {
                throw new IllegalArgumentException("Empty keystore type");
            }

            String keystoreFile = fedHubConfig.getKeystoreFile();
            if (Strings.isNullOrEmpty(keystoreFile)) {
                throw new IllegalArgumentException("Keystore file name empty");
            }

            String keystorePassword = fedHubConfig.getKeystorePassword();
            if (Strings.isNullOrEmpty(keystorePassword)) {
                throw new IllegalArgumentException("Empty keystore password");
            }

            KeyStore self = KeyStore.getInstance(keystoreType);
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                // Filename of the keystore file.
                self.load(fis, keystorePassword.toCharArray());
            }

            // Password of the keystore file.
            keyMgrFactory.init(self, fedHubConfig.getKeystorePassword().toCharArray());

            // Initialize trust store.
            SSLConfig.initTrust(fedHubConfig, trustMgrFactory);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not initialize truststore", e);
            }
        }

        try {
            return SslContextBuilder.forServer(keyMgrFactory)
                    .trustManager(trustMgrFactory)
                    .clientAuth(ClientAuth.REQUIRE)
                    .sslProvider(SslProvider.OPENSSL)
                    .build();
        } catch (SSLException e) {
            logger.error("Could not build server SSL context", e);
            return null;
        }
    }

    public void sendContactMessagesV1(NioNettyFederationHubServerHandler handler) {
        FederationPolicyGraph fpg = fedHubPolicyManager.getPolicyGraph();
        if (fpg == null) {
            logger.error("Cannot send contact messages; policy manager is null");
            return;
        }

        String fedId = handler.getFederateIdentity().getFedId();
        Federate clientNode = fpg.getFederate(fedId);
        if (clientNode == null) {
            logger.error("Cannot send contact messages; client node is null");
            return;
        }

        // Send contact messages from other clients back to this new client.
        for (NioNettyFederationHubServerHandler otherClient :
                v1ClientStreamMap.values()) {

            Federate otherClientNode = fpg
                .getFederate(otherClient.getFederateIdentity().getFedId());

            if (logger.isDebugEnabled()) {
                logger.debug("Looking for cached contact messages from other client " +
                    otherClientNode.getFederateIdentity().getFedId());
            }

            // Send cached contact messages, iff there is a federated edge
            // between the two federates.
            if (otherClientNode != null &&
                    fpg.getEdge(otherClientNode, clientNode) != null) {
                for (FederatedEvent event : otherClient.getCache()) {
                    Message federatedMessage = new Message(new HashMap<>(),
                        new FederatedEventPayload(event));
                    handler.send(federatedMessage);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Sending v1 cached " + event +
                            " from " + otherClientNode.getFederateIdentity().getFedId() +
                            " to " + clientNode.getFederateIdentity().getFedId());
                    }
                }
            }
        }
    }

    /* Mirrors NioNettyBuilder. */
    private void setupFederationV1Server() {
        boolean useEpoll = Epoll.isAvailable() && fedHubConfig.isUseEpoll();
        bossGroup = useEpoll ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        workerGroup = useEpoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();

        new Thread(() -> {
            try {
                final int NUM_AVAIL_CORES = Runtime.getRuntime().availableProcessors();
                int highMark = 4096 * NUM_AVAIL_CORES;
                int lowMark = highMark / 2;
                WriteBufferWaterMark waterMark = new WriteBufferWaterMark(lowMark, highMark);

                /* TODO: set up other fields from xsd of federation-server elements. */
                SslContext sslContext = buildServerSslContext(fedHubConfig);

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                SslHandler sslHandler = sslContext.newHandler(channel.alloc());
                                sslHandler.engine().setEnabledProtocols(
                                    fedHubConfig.getTlsVersions().toArray(String[]::new));
                                String sessionId = new String(sslHandler.engine().getSession().getId(),
                                    Charsets.UTF_8);

                                NioNettyFederationHubServerHandler handler =
                                    new NioNettyFederationHubServerHandler(FederationHubBrokerService.this,
                                        new Comparator<FederatedEvent>() {
                                            @Override
                                            public int compare(FederatedEvent a, FederatedEvent b) {
                                                return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                                            }
                                        }
                                );

                                v1ClientStreamMap.put(sessionId, handler);

                                channel.pipeline()
                                    .addLast("ssl", sslHandler)
                                    .addLast(new ByteArrayDecoder())
                                    .addLast(new ByteArrayEncoder())
                                    .addLast(handler);
                            }

                            @Override
                            public String toString() {
                                return "FederationHubServerInitializer";
                            }
                        })
                        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark)
                        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

                channelFuture = bootstrap.bind(fedHubConfig.getV1Port()).sync().channel().closeFuture();
                logger.info("Successfully started Federation Hub v1 server on port " + fedHubConfig.getV1Port());
            } catch (Exception e) {
                logger.error("Error initializing Federation Hub v1 server", e);
            }
        }).start();
    }

    private void setupFederationV2Server() {
        /* TODO Create and configure analog to lambda filter. */

        try {
            sendCaGroupsToFedManager(sslConfig.getTrust());
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        if (fedHubConfig.isEnableHealthCheck()) {
            // Health check thread. Schedule metrics sending every K seconds.
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    removeInactiveClientStreams();
                    if (!keepRunning.get()) {
                        // Cancel this scheduled job by throwing an exception.
                        throw new RuntimeException("Stopping server");
                    }
                }
            }, fedHubConfig.getClientRefreshTime(), fedHubConfig.getClientRefreshTime(), TimeUnit.SECONDS);
        }

        NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(fedHubConfig.getV2Port())
            .maxInboundMessageSize(fedHubConfig.getMaxMessageSizeBytes())
            .sslContext(sslConfig.getSslContext());

        if (fedHubConfig.getMaxConcurrentCallsPerConnection() != null &&
                fedHubConfig.getMaxConcurrentCallsPerConnection() > 0) {
            serverBuilder.maxConcurrentCallsPerConnection(fedHubConfig.getMaxConcurrentCallsPerConnection());
        }

        FederatedChannelService service = new FederatedChannelService();

        server = serverBuilder
                .addService(ServerInterceptors.intercept(service, tlsInterceptor()))
                .build();

        /* TODO What is the purpose of this? */
        service.binaryMessageStream(new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {}

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        });

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                requireNonNull(fedHubConfig, "Federation Hub configuration object");

                try {
                    server.start();
                    logger.info("Federation Hub (v2 protocol) started, listening on port " +
                            fedHubConfig.getV2Port());

                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            System.err.println("*** shutting down gRPC server since JVM is shutting down");
                            FederationHubBrokerService.this.stop();
                            keepRunning.set(false);
                            System.err.println("*** server shut down");
                        }
                    });
                } catch (Exception e) {
                    logger.error("Exception starting v2 Federation Hub server", e);
                }
            }
        });
    }

    @Override
    public void onApplicationEvent(RestartServerEvent event) {
        if (fedHubConfig.isV2Enabled()) {
            logger.info("Restarting V2 federation server after truststore update");
            server.shutdown();
            try {
                server.awaitTermination();
            } catch (InterruptedException e) { }
            keepRunning.set(false);
            clientStreamMap.clear();
            clientMessageCounter.set(0);
            clientByteAccumulator.set(0);
            keepRunning.set(true);
            sslConfig.initSslContext(fedHubConfig);
            setupFederationV2Server();
        }
    }

    @Bean
    public SSLConfig getSslConfig() {
        return new SSLConfig();
    }

    @Bean
    public FederationHubServerConfig getFedHubConfig()
            throws JsonParseException, JsonMappingException, IOException {
        return loadConfig(configFile);
    }

    @Bean
    public void setupFederationServers() {
        sslConfig.initSslContext(fedHubConfig);

        if (fedHubConfig.isV2Enabled()) {
            setupFederationV2Server();
        }

        if (fedHubConfig.isV1Enabled()) {
            setupFederationV1Server();
        }
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void sendCaGroupsToFedManager(KeyStore keyStore) throws KeyStoreException {
        for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements();) {
            String alias = e.nextElement();
            X509Certificate cert = (X509Certificate)keyStore.getCertificate(alias);
            FederationHubBrokerUtils.sendCaGroupToFedManager(fedHubPolicyManager, cert);
        }
    }

    public void addCaFederateToPolicyGraph(FederateIdentity federateIdentity, Certificate[] caCertArray) {
        List<String> caCertNames = new LinkedList<>();

        /*
         * The cert array returned by gRPC's SSLSession is padded
         * with null entries.  This loop adds all certs in the
         * array from index 1 (index 0 is the peer's cert, index 1+
         * are CA certs) til the first null entry (the start of the
         * padding) to a list of CA certs.
         */
        for (int i = 1; i < caCertArray.length; i++) {
            if (caCertArray[i] == null) {
                break;
            }
            try {
                String issuerName = ((X509Certificate)caCertArray[i])
                    .getIssuerX500Principal().getName();

                if (logger.isDebugEnabled()) {
                    logger.debug("addCaFederateToPolicyGraph issuerName: " + issuerName);
                }

                String groupName = issuerName + "-" +
                    FederationUtils.getBytesSHA256(caCertArray[i].getEncoded());
                caCertNames.add(groupName);
            } catch (CertificateEncodingException e) {
                logger.error("Could not encode certificate", e);
            }
        }

        Federate federate = new Federate(federateIdentity);
        fedHubPolicyManager.addCaFederate(federate, caCertNames);
    }

    public void addFederateToGroupPolicyIfMissingV1(Certificate[] certArray,
            FederateIdentity federateIdentity) {
        String fedId = federateIdentity.getFedId();
        if (fedHubPolicyManager.getPolicyGraph().getNode(fedId) == null) {
            addCaFederateToPolicyGraph(federateIdentity, certArray);
        }
    }

    private class FederatedChannelService extends FederatedChannelGrpc.FederatedChannelImplBase {

        private final FederationHubBrokerService broker = FederationHubBrokerService.this;
        private final FederationProcessorFactory federationProcessorFactory = new FederationProcessorFactory();
        AtomicReference<Long> start = new AtomicReference<>();

        @Override
        public void sendOneEvent(FederatedEvent clientEvent, io.grpc.stub.StreamObserver<Empty> emptyReseponse) {
            if (logger.isDebugEnabled()) {
                logger.debug("Received single event from client: " + clientEvent.getEvent().getUid());
            }
            clientMessageCounter.incrementAndGet();
            clientByteAccumulator.addAndGet(clientEvent.getSerializedSize());
        }

        private void addFederateToGroupPolicyIfMissingV2(SSLSession session,
                GuardedStreamHolder holder) {
            String fedId = holder.getFederateIdentity().getFedId();
            if (fedHubPolicyManager.getPolicyGraph().getNode(fedId) == null) {
                try {
                    Certificate[] certArray = session.getPeerCertificates();
                    addCaFederateToPolicyGraph(holder.getFederateIdentity(), certArray);
                } catch (SSLPeerUnverifiedException e) {
                    logger.error("Could not get peer certificates from the SSL session", e);
                }
            }
        }

        @Override
        public StreamObserver<BinaryBlob> binaryMessageStream(StreamObserver<Empty> responseObserver) {
            return new StreamObserver<BinaryBlob>() {

                @Override
                public void onNext(BinaryBlob value) {
                    long latency = new Date().getTime() - value.getTimestamp();
                    SSLSession session = (SSLSession)sslSessionKey.get(Context.current());
                    logger.info("binaryMessageStream received binary file from client: " +
                        value.getDescription() + " " + new Date(value.getTimestamp()) + " " +
                        value.getSerializedSize() + " bytes (serialized) latency: " +
                        latency + " ms");

                    if (fedHubConfig.isUseCaGroups()) {
                        addFederateToGroupPolicyIfMissingV2(session,
                            clientStreamMap.get(new String(session.getId(),
                                Charsets.UTF_8))
                        );
                    }

                    FederationHubBrokerService.this.handleRead(value, session.getId());
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Exception in binary message stream", t);
                }

                @Override
                public void onCompleted() {
                    logger.info("Binary message stream complete");
                }
            };
        }

        @Override
        public void sendOneBlob(BinaryBlob request, StreamObserver<Empty> resp) {
            start.compareAndSet(null, System.currentTimeMillis());

            SSLSession session = (SSLSession)sslSessionKey.get(Context.current());

            long bytesPerSecond = -1;

            long elapsed = System.currentTimeMillis() - start.get();
            long latency = -1;
            double mps = -1;
            double exectime = elapsed / 1000D;

            if ((long) exectime > 0) {
                mps = clientMessageCounter.get() / exectime;
                bytesPerSecond = clientByteAccumulator.get() / (long) exectime;

                latency = new Date().getTime() - request.getTimestamp();
            }

            if (logger.isTraceEnabled()) {
                logger.trace("sendOneBlob received binary file from client: " +
                    request.getDescription() + " " +
                    new Date(request.getTimestamp()) + " " +
                    request.getSerializedSize() + " bytes (serialized)  latency: " +
                    latency + " ms " + mps + " messages per second," +
                    bytesPerSecond + " bytes per second");
            }

            clientMessageCounter.incrementAndGet();
            clientByteAccumulator.addAndGet(request.getSerializedSize());

            FederationHubBrokerService.this.handleRead(request, session.getId());
        }

        private final AtomicInteger clientEventStreamCounter = new AtomicInteger();

        @Override
        public void clientEventStream(Subscription subscription,
                StreamObserver<FederatedEvent> clientStream) {
            requireNonNull(subscription, "client-specified subscription");
            requireNonNull(subscription.getIdentity(), "client-specified identity");

            String clientName = subscription.getIdentity().getName();

            if (Strings.isNullOrEmpty(clientName)) {
                throw new IllegalArgumentException("Invalid clientEventStream request from client - null or empty name was provided");
            }

            int streamCount = clientEventStreamCounter.incrementAndGet();

            SSLSession session = (SSLSession) sslSessionKey.get(Context.current());

            GuardedStreamHolder<FederatedEvent> streamHolder = null;

            if (!Strings.isNullOrEmpty(subscription.getFilter())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Preparing subscription filter for " +
                        subscription.getIdentity() + " " +
                        subscription.getFilter());
                }
                /* TODO Use lambda filter. */
                //federateLambdaFilter.prepare(subscription.getFilter());
            }

            try {
                Certificate[] clientCertArray = requireNonNull(
                    requireNonNull(session, "SSL Session").getPeerCertificates(),
                    "SSL peer certs array");

                if (clientCertArray.length == 0) {
                    throw new IllegalArgumentException("Client certificate not available");
                }

                streamHolder = new GuardedStreamHolder<FederatedEvent>(clientStream,
                    clientName, FederationUtils.getBytesSHA256(clientCertArray[0].getEncoded()),
                    subscription, new Comparator<FederatedEvent>() {
                        @Override
                        public int compare(FederatedEvent a, FederatedEvent b) {
                            return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                        }
                    }
                );

                if (fedHubConfig.isUseCaGroups()) {
                    addCaFederateToPolicyGraph(streamHolder.getFederateIdentity(),
                        clientCertArray);
                }

                FederationPolicyGraph fpg = fedHubPolicyManager.getPolicyGraph();
                requireNonNull(fpg, "federation policy graph object");

                String fedId = streamHolder.getFederateIdentity().getFedId();
                Federate clientNode = fpg.getFederate(fedId);
                if (logger.isDebugEnabled()) {
                    logger.debug("New client federated event stream client node id: " +
                        streamHolder.getFederateIdentity().getFedId() + " " +
                        clientNode);
                }

                requireNonNull(clientNode,
                    "federation policy node for newly connected client with FedId: " +
                    streamHolder.getFederateIdentity().getFedId());

                // Send contact messages from other clients back to this new client.
                for (GuardedStreamHolder<FederatedEvent> otherClient :
                        clientStreamMap.values()) {

                    Federate otherClientNode = fpg
                        .getFederate(otherClient.getFederateIdentity().getFedId());

                    if (logger.isDebugEnabled()) {
                        logger.debug("Looking for cached contact messages from other client " +
                            otherClientNode.getFederateIdentity().getFedId());
                    }

                    // Send cached contact messages, iff there is a federated edge between the two federates.
                    if (otherClientNode != null &&
                            fpg.getEdge(otherClientNode, clientNode) != null) {
                        for (FederatedEvent event : otherClient.getCache()) {
                            streamHolder.send(event);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Sending v2 cached " + event +
                                    " from " + otherClientNode.getFederateIdentity().getFedId() +
                                    " to " + clientNode.getFederateIdentity().getFedId());
                            }
                        }
                    }
                }
            } catch (SSLPeerUnverifiedException | CertificateEncodingException e) {
               throw new RuntimeException("Error obtaining federate client certficate", e);
            }

            try {
                /*
                 * If no authentication errors, keep track of clientStream
                 * and its associated federate identity.
                 *
                 * If we add the client to the map before authenticating,
                 * we keep re-adding the same client trying to connect with
                 * a new session id.
                 */
                String id = new String(session.getId(), Charsets.UTF_8);
                broker.clientStreamMap.put(id, streamHolder);

                if (logger.isDebugEnabled()) {
                    logger.debug("Registering FederateIdentity: " + streamHolder.getFederateIdentity());
                }

                /* TODO Is there an equivalent to the ROGER network plugin registry? */
                //FederationHubBrokerService.this.registry.register(
                //    FederationHubBrokerService.this,
                //    streamHolder.getFederateIdentity());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            logger.info("Client stream added. Count: " + streamCount);
        }

        @Override
        public void clientROLStream(Subscription subscription, StreamObserver<ROL> clientStream) {
            requireNonNull(subscription, "client-specified subscription");
            requireNonNull(subscription.getIdentity(), "client-specified identity");

            String clientName = subscription.getIdentity().getName();

            if (Strings.isNullOrEmpty(clientName)) {
                throw new IllegalArgumentException("Invalid clientEventStream request from client - null or empty name was provided");
            }

            try {
                SSLSession session = (SSLSession)sslSessionKey.get(Context.current());
                Certificate[] clientCertArray = requireNonNull(requireNonNull(session, "SSL Session")
                    .getPeerCertificates(), "SSL peer certs array");

                if (clientCertArray.length == 0) {
                    throw new IllegalArgumentException("Client certificate not available");
                }

                String fedCertHash = FederationUtils.getBytesSHA256(clientCertArray[0].getEncoded());

                if (logger.isDebugEnabled()) {
                    logger.debug("Certificate hash of federate sending ROL: {}, clientName: {}", fedCertHash, clientName);
                }

                GuardedStreamHolder<ROL> rolStreamHolder = new GuardedStreamHolder<>(clientStream,
                    clientName, fedCertHash, subscription, new Comparator<ROL>() {
                        @Override
                        public int compare(ROL a, ROL b) {
                            return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                        }
                    }
                );

                /* Keep track of client stream and its associated federate identity. */
                String id = new String(session.getId(), Charsets.UTF_8);
                broker.clientROLStreamMap.put(id, rolStreamHolder);
                broker.clientROLStreamNames.put(id, clientName);

                logger.info("Client ROL stream added. Count: " + broker.clientROLStreamMap.size());

            } catch (SSLPeerUnverifiedException | CertificateEncodingException e) {
                throw new RuntimeException("Error obtaining federate client certificate", e);
            }
        }

        private void dispersePackageBytes(String sessionId, ResourceDetails details, byte[] packageBytes) throws IOException {
            for (Map.Entry<String, GuardedStreamHolder<ROL>> stream :
                    clientROLStreamMap.entrySet()) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Local (server) session " + sessionId + " stream session: " + stream.getKey());
                    }
                    /* Compute the SHA-256 hash. */
                    MessageDigest msgDigest = null;
                    try {
                        msgDigest = MessageDigest.getInstance("SHA-256");
                        msgDigest.update(packageBytes);
                        byte[] mdbytes = msgDigest.digest();
                        StringBuffer hash = new StringBuffer();
                        for (int i = 0; i < mdbytes.length; i++) {
                            hash.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("Calculated hash: " + hash);
                        }
                        details.setSha256(hash.toString());
                    } catch (NoSuchAlgorithmException e) { }

                    ROL.Builder rolBuilder = ROL.newBuilder()
                        .setProgram("disperse package\n" + new ObjectMapper().writeValueAsString(details) + ";");

                    BinaryBlob file = BinaryBlob
                        .newBuilder()
                        .setData(ByteString.readFrom(new ByteArrayInputStream(packageBytes)))
                        .build();

                    rolBuilder.addPayload(file);

                    ROL rol = rolBuilder.build();

                    /* Don't self-send. */
                    if (!stream.getKey().equals(sessionId)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("dispersing package " + packageBytes.length + " bytes to federate " + sessionId);
                        }
                        handleRead(rol, sessionId);
                    }
                } catch (io.grpc.StatusRuntimeException e1){
                    logger.error("Exception dispersing package: gRPC StatusRuntimeException " + details, e1);
                } catch (Exception e) {
                    logger.error("Exception dispersing package: " + details, e);
                }

                /* TODO Need to close stream here, or gRPC will do it? */
            }
        }

        private void requestPackage(String sessionId, ResourceDetails details) {
            if (Strings.isNullOrEmpty(details.getSha256())) {
                logger.warn("mission package announce contains no hash - ignoring");
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Requesting package " + details + " from announcing federate");
            }

            GuardedStreamHolder<ROL> announcer = clientROLStreamMap.get(sessionId);

            if (announcer == null) {
                logger.warn("Announcing federate session not found for " + details);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Announcing federate session found");
            }

            ROL rol;
            try {
                rol = ROL.newBuilder().setProgram("request package\n" + new ObjectMapper().writeValueAsString(details) + ";").build();
            } catch (JsonProcessingException e) {
                logger.warn("Unparseable JSON in ROL announce: " + details);
                return;
            }

            try {
                /* Tell the announcing federate to provide us the mission package binary. */
                handleRead(rol, sessionId);

                /* Track message sends for metrics. */
                clientMessageCounter.incrementAndGet();
                clientByteAccumulator.addAndGet(rol.getSerializedSize());
            } catch (Exception ex) {
                logger.error("Exception sending message to ROL stream", ex);
//              clientROLStreamMap.remove(announcer);
            }
        }

        @Override
        public StreamObserver<ROL> serverROLStream(StreamObserver<Subscription> responseObserver) {
            return new StreamObserver<ROL>() {

                @Override
                public void onNext(ROL clientROL) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ROL from client: " + clientROL.getProgram());
                        }

                        requireNonNull(clientROL, "ROL message from client");
                        requireNonNull(clientROL.getProgram(), "ROL program from client");

                        /* Interpret and execute the ROL program. */
                        RolLexer lexer = new RolLexer(new ANTLRInputStream(clientROL.getProgram()));

                        CommonTokenStream tokens = new CommonTokenStream(lexer);

                        RolParser parser = new RolParser(tokens);
                        parser.setErrorHandler(new BailErrorStrategy());

                        /* Parse the ROL program. */
                        ParseTree rolParseTree = parser.program();

                        requireNonNull(rolParseTree, "parsed ROL program");

                        final AtomicReference<String> resource = new AtomicReference<>();
                        final AtomicReference<String> operation = new AtomicReference<>();
                        final AtomicReference<Parameters> parameters = new AtomicReference<>();

                        new MissionRolVisitor(new ResourceOperationParameterEvaluator<Parameters, String>() {
                            @Override
                            public String evaluate(String res, String op, Parameters params) {
                                logger.error("Evaluating " + op + " on " + resource + " given " + params);

                                resource.set(res);
                                operation.set(op);
                                parameters.set(params);

                                return res;
                            }
                        }).visit(rolParseTree);

                        requireNonNull(resource.get(), "resource");
                        requireNonNull(operation.get(), "operation");

                        SSLSession session = (SSLSession) sslSessionKey.get(Context.current());

                        String sessionId = new String(session.getId(), Charsets.UTF_8);
                        if (fedHubConfig.isUseCaGroups()) {
                            addFederateToGroupPolicyIfMissingV2(session, clientStreamMap.get(
                                    new String(session.getId(), Charsets.UTF_8))
                            );
                        }

                        /* Create a federation processor for this ROL type, and process the ROL program. */
                        federationProcessorFactory.newProcessor(resource.get(), operation.get(),
                            parameters.get(), sessionId).process(clientROL);

                    } catch (Exception e) {

                        if (e instanceof io.grpc.StatusException) {
                            if (((io.grpc.StatusException) e).getStatus().equals(Status.CANCELLED)) {
                                //SSLSession session = (SSLSession) sslSessionKey.get(Context.current());
                                // could look up which connection it is. But the name is gibberish!

                                logger.info("Federate disconnected or connectivity lost");
                            }
                        } else {
//                              logger.error("exception in server event stream call", t);
                            logger.error("Exception in ROL stream " + e.getClass().getName(), e);
                        }
                        // need to remove for
                        // io.grpc.StatusRuntimeException:
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Status status = Status.fromThrowable(t);
                    SSLSession session = (SSLSession)sslSessionKey.get(Context.current());
                    String id = new String(session.getId(), Charsets.UTF_8);
                    broker.clientROLStreamMap.remove(id);
                    String clientName = broker.clientROLStreamNames.remove(id);


                    if (t instanceof StatusRuntimeException) {
                        logger.error("gRPC StatusRuntimeException. Removed {} from clientRolStreams."
                                + " Status: {}", clientName, status, t);
                    } else {
                        logger.error("Error in ROL stream. Removed {} from clientROLStreams."
                                + " Status: {}", clientName, status, t);
                    }
                }

                @Override
                public void onCompleted() {
                    if (logger.isDebugEnabled()) {
                        logger.debug("ROL stream complete");
                    }
                }
            };
        }

        @Override
        public StreamObserver<FederatedEvent> serverEventStream(StreamObserver<Subscription> responseObserver) {
            return new StreamObserver<FederatedEvent>() {

                @Override
                public void onNext(FederatedEvent fe) {
                    clientMessageCounter.incrementAndGet();
                    clientByteAccumulator.addAndGet(fe.getSerializedSize());

                    SSLSession session = (SSLSession)sslSessionKey.get(Context.current());

                    // Add federate to group in case policy was updated during connection
                    String id = new String(session.getId(), Charsets.UTF_8);
                    if (fedHubConfig.isUseCaGroups()) {
                        addFederateToGroupPolicyIfMissingV2(session, clientStreamMap.get(
                                new String(session.getId(), Charsets.UTF_8))
                        );
                    }

                    // submit to orchestrator
                    FederationHubBrokerService.this.handleRead(fe, session.getId());
                }

                @Override
                public void onError(Throwable t) {
                    if (t instanceof io.grpc.StatusException) {
                        if (((io.grpc.StatusException) t).getStatus().equals(Status.CANCELLED)) {
                            //SSLSession session = (SSLSession) sslSessionKey.get(Context.current());
                            // could look up which connection it is. But the name is gibberish!

                            logger.info("Federate disconnected or connectivity lost");
                        }
                    } else {
                        logger.error("Exception in server event stream call", t);
                    }
                }

                @Override
                public void onCompleted() {
                    Map<String, Object> metric = new HashMap<>();

                    metric.put("message", "received a 'complete' notification from a client");

                    logger.info(new Gson().toJson(metric));
                }
            };
        }

        @Override
        public void healthCheck(ClientHealth request, StreamObserver<ServerHealth> responseObserver) {
            if (fedHubConfig.isEnableHealthCheck()) {
                String sessionId = new String(((SSLSession)sslSessionKey.get(
                     Context.current())).getId(), Charsets.UTF_8);

                if (clientStreamMap.containsKey(sessionId)) {
                    clientStreamMap.get(sessionId).updateClientHealth(request);
                    responseObserver.onNext(serving);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No session existed for sessionID: {}", sessionId);
                    }
                    responseObserver.onNext(notConnected);
                }

                if (clientROLStreamMap.containsKey(sessionId)) {
                    clientROLStreamMap.get(sessionId).updateClientHealth(request);
                    /* Uncommenting this and/or the one below causes "Too many responses gRPC error. */
                    //responseObserver.onNext(serving);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No ROL client session existed for sessionID: {}", sessionId);
                    }
                    //responseObserver.onNext(notConnected);
                }
            } else {
                logger.warn("Not sending FIG health check - disabled in config");
            }
        }

        private class FederationMissionPackageProcessor implements FederationProcessor<ROL> {

            private final String res;
            private final String op;
            private final ResourceDetails dt;
            private final String sessionId;

            FederationMissionPackageProcessor(String res, String op, ResourceDetails dt, String sessionId) {
                this.res = res;
                this.op = op;
                this.dt = dt;
                this.sessionId = sessionId;
            }

            @Override
            public void process(ROL clientROL) {
                if (!res.toLowerCase().equals("package")) {
                    logger.warn("Ignoring unexpected ROL resource from client: " + res);
                    return;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Processing op " + op);
                }

                switch (op.toLowerCase()) {
                case "announce":
                    logger.info("Received 'announce package' ROL command from client.");

                    if (Strings.isNullOrEmpty(dt.getSha256())) {
                        logger.warn("'announce package' ROL from client contains no resource hash - ignoring.");
                        return;
                    }

                    SyncResultBytes packageResult = null;

                    try {
                        packageResult = syncService.retrieveBytes(dt.getSha256());
                        if (logger.isDebugEnabled()) {
                            logger.debug("dispersing locally cached package " + dt);
                        }
                        // NotFoundException will be thrown if the package was not cached locally.
                        // If it was found, disperse it to clients.
                        dispersePackageBytes(sessionId, dt, packageResult.getBytes());
                    } catch (NotFoundException nfe) {
                        requestPackage(sessionId, dt);
                    } catch (IOException e) {
                        logger.warn("Exception dispersing binary package", e);
                        clientROLStreamMap.remove(sessionId);
                    }

                    break;

                case "disperse":
                    logger.info("Received 'disperse package' ROL command from client.");

                    if (Strings.isNullOrEmpty(dt.getSha256())) {
                        logger.warn("'announce package' ROL from client contains no resource hash - ignoring.");
                        return;
                    }

                    if (clientROL.getPayloadList().isEmpty()) {
                        logger.warn("ROL disperse message from client contained no payload. ignoring.");
                        return;
                    }

                    BinaryBlob packageBlob = clientROL.getPayload(0);

                    byte[] packageBytes = packageBlob.getData().toByteArray();

                    if (packageBytes == null || packageBytes.length == 0) {
                        logger.warn("empty binary payload in 'disperse package' command. Ignoring. " + dt);
                        return;
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Saving package " + dt);
                    }

                    // cache the package
                    syncService.save(packageBytes, dt);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Package saved " + dt);
                    }

                    /*
                     * TODO In the FIG implementation, there
                     * is an option to skip the gateway that
                     * was used here. Do we need the analogous
                     * operation?
                     */
                    if (logger.isDebugEnabled()) {
                        logger.debug("Sending ROL to gateway");
                    }
                    handleRead(clientROL, sessionId);
                    break;
                default:
                    logger.warn("Unexpected ROL operation received from client: " + op);
                }
            }
        }

        private class FederationMissionProcessor implements FederationProcessor<ROL> {

            private final String res;
            private final String op;
            private final String sessionId;
            private final Parameters parameters;

            FederationMissionProcessor(String res, String op, Parameters parameters, String sessionId) {
                this.res = res;
                this.op = op;
                this.sessionId = sessionId;
                this.parameters = parameters;
            }

            @Override
            public void process(ROL rol) {
                processAllResourceOperations(rol);
            }

            private void processAllResourceOperations(ROL rol) {

                GuardedStreamHolder<ROL> announcer = clientROLStreamMap.get(sessionId);

                for (Map.Entry<String, GuardedStreamHolder<ROL>> stream : clientROLStreamMap.entrySet()) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Local (server) session " + sessionId + " stream session: " + stream.getKey());
                        }

                        // Don't self-send.
                        if (!stream.getKey().equals(sessionId)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Federating mission create to federate " + sessionId);
                            }
                            handleRead(rol, sessionId);
                        }
                    } catch (io.grpc.StatusRuntimeException e1) {
                        logger.error("Exception sending mission ROL: gRPC StatusRuntimeException", e1);
                    } catch (Exception e) {
                        logger.error("Exception sending mission ROL", e);
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Announcing federate session found");
                    }

                    try {
                        // Track message sends for metrics.
                        clientMessageCounter.incrementAndGet();
                        clientByteAccumulator.addAndGet(rol.getSerializedSize());
                    } catch (Exception ex) {
                        logger.error("Exception sending message to ROL stream", ex);
                        clientROLStreamMap.remove(announcer);
                    }
                }
            }
        }

        private class FederationDefaultProcessor implements FederationProcessor<ROL> {

            private final String res;
            private final String op;
            private final String sessionId;

            FederationDefaultProcessor(String res, String op, String sessionId) {
                this.res = res;
                this.op = op;
                this.sessionId = sessionId;
            }

            @Override
            public void process(ROL rol) {
                processAllResourceOperations(rol);
            }

            private void processAllResourceOperations(ROL rol) {

                GuardedStreamHolder<ROL> announcer = clientROLStreamMap.get(sessionId);

                for (Map.Entry<String, GuardedStreamHolder<ROL>> stream : clientROLStreamMap.entrySet()) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("local (server) session " + sessionId + " stream session: " + stream.getKey());
                        }

                        // Don't self-send.
                        if (!stream.getKey().equals(sessionId)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Federating mission create to federate " + sessionId);
                            }
                            handleRead(rol, sessionId);
                        }
                    } catch (io.grpc.StatusRuntimeException e1){
                        logger.error("Exception sending ROL: gRPC StatusRuntimeException", e1);
                    } catch (Exception e) {
                        logger.error("Exception sending ROL", e);

                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("sending federate session matched");
                    }

                    try {
                        // Track message sends for metrics.
                        clientMessageCounter.incrementAndGet();
                        clientByteAccumulator.addAndGet(rol.getSerializedSize());
                    } catch (Exception ex) {
                        logger.error("Exception sending message to ROL stream", ex);
                        clientROLStreamMap.remove(announcer);
                    }
                }
            }
        }

        class FederationProcessorFactory {

            FederationProcessor<ROL> newProcessor(String resource, String operation, Parameters parameters, String sessionId) {
                switch (Resource.valueOf(resource.toUpperCase())) {
                case PACKAGE:
                    if (!(parameters instanceof ResourceDetails)) {
                        throw new IllegalArgumentException("Invalid ReourceDetails object for mission package processing");
                    }
                    return new FederationMissionPackageProcessor(resource, operation, (ResourceDetails) parameters, sessionId);
                case MISSION:
                    return new FederationMissionProcessor(resource, operation, parameters, sessionId);
                default:
                    return new FederationDefaultProcessor(resource, operation, sessionId);
                }
            }
        }
    }

    final static private Context.Key<SSLSession> sslSessionKey = Context.key("SSLSession");

    public static ServerInterceptor tlsInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    final Metadata requestHeaders,
                    ServerCallHandler<ReqT, RespT> next) {

                SSLSession sslSession = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
                Context context = Context.current().withValue(sslSessionKey, sslSession);
                return Contexts.interceptCall(context, call, requestHeaders, next);
            }
        };
    }

    public void assignMessageSourceAndDestinationsFromPolicy(Message message,
            FederateIdentity federateIdentity)
            throws FederationException {
        assignMessageSourceAndDestinationsFromPolicy(message, federateIdentity,
            fedHubPolicyManager.getPolicyGraph());
    }

    private void assignMessageSourceAndDestinationsFromPolicy(Message message,
            FederateIdentity federateIdentity, FederationPolicyGraph policyGraph)
            throws FederationException {
        message.setSource(new AddressableEntity<FederateIdentity>(federateIdentity));
        Set<Federate> destinationNodes = policyGraph.allReachableFederates(federateIdentity.getFedId());
        destinationNodes.stream().forEach(node ->
            message.getDestinations().add(new AddressableEntity<>(node.getFederateIdentity()))
        );
    }

    private void deliverRol(Message message, FederateIdentity src, FederateIdentity dest) {

        String originSslId = null;
        if (message.containsMetadataKey(SSL_SESSION_ID) &&
                message.getMetadataValue(SSL_SESSION_ID) instanceof String) {
            originSslId = (String)message.getMetadataValue(SSL_SESSION_ID);
        }

        for (Map.Entry<String, GuardedStreamHolder<ROL>> stream : clientROLStreamMap.entrySet()) {
            String destinationSsl = stream.getKey();
            if (!destinationSsl.equals(originSslId)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("clientROLStream session: " + destinationSsl);
                    logger.debug("clientROLStream name: " + clientROLStreamNames.get(destinationSsl));
                }
                try {
                    stream.getValue().send((ROL)message.getPayload().getContent());
                } catch (Exception e) {
                    logger.error("Caught exception", e);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Not sending ROL to originator: {}", clientROLStreamNames.get(destinationSsl));
                }
            }
        }
    }

    private void sendRolMessage(Message message) {
        for (AddressableEntity<?> entity : message.getDestinations()) {
            if (entity.getEntity() instanceof FederateIdentity) {
                FederateIdentity src = (FederateIdentity)message.getSource().getEntity();
                FederateIdentity dest = (FederateIdentity)entity.getEntity();

                FederationPolicyGraph policyGraph = fedHubPolicyManager.getPolicyGraph();

                Federate srcNode = policyGraph.getFederate(src.getFedId());
                Federate destNode = policyGraph.getFederate(dest.getFedId());

                /* Validate src/dest and nodes. */
                FederateEdge edge = policyGraph.getEdge(srcNode, destNode);

                if (edge != null && !Strings.isNullOrEmpty(edge.getFilterExpression())) {

                    /*
                     * Important note: filters need to be responsible for
                     * mutating the message. It's recommended that they
                     * follow an immutable approach, and return a new
                     * message instead of mutating.
                     */

                    /* TODO Apply the lambda filter. */
                    //message = federateLambdaFilter.filter(message, edge.getFilterExpression());
                }

                if (message == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Message delivery denied by edge filter");
                    }
                    continue;
                }

                deliverRol(message, src, dest);
            }
        }
    }

    private void deliverV1(Message message, FederateIdentity src, FederateIdentity dest) {
        for (Entry<String, NioNettyFederationHubServerHandler> entry : v1ClientStreamMap.entrySet()) {
            if (entry.getValue().getFederateIdentity().equals(dest)) {
                Message filteredMessage = message;

                /* TODO use filter
                if (!Strings.isNullOrEmpty(entry.getValue().getSubscription().getFilter())) {
                    filteredMessage = federateLambdaFilter.filter(message, entry.getValue().getSubscription().getFilter());
                }

                if (filteredMessage == null) {
                    logger.debug("message delivery denied by subscription filter");
                    continue;
                }
                */

                if (logger.isTraceEnabled()) {
                    logger.trace("Sending message {} from {} to {}", message.toString(), src, dest);
                }

                try {
                    entry.getValue().send(filteredMessage);

                    /* Track message sends for metrics. */
                    clientMessageCounter.incrementAndGet();
                    clientByteAccumulator.addAndGet(
                        ((FederatedEvent)filteredMessage.getPayload().getContent()).getSerializedSize());
                } catch (Exception ex) {
                    logger.error("Exception sending message to client stream", ex);
                    v1ClientStreamMap.remove(entry.getKey());
                }
            }
        }
    }

    private void deliver(Message message, FederateIdentity src, FederateIdentity dest) {
        for (Entry<String, GuardedStreamHolder<FederatedEvent>> entry : clientStreamMap.entrySet()) {
            if (entry.getValue().getFederateIdentity().equals(dest)) {
                Message filteredMessage = message;

                /* TODO use filter
                if (!Strings.isNullOrEmpty(entry.getValue().getSubscription().getFilter())) {
                    filteredMessage = federateLambdaFilter.filter(message, entry.getValue().getSubscription().getFilter());
                }

                if (filteredMessage == null) {
                    logger.debug("message delivery denied by subscription filter");
                    continue;
                }
                */

                FederatedEvent event = requireNonNull((FederatedEvent)filteredMessage.getPayload().getContent(),
                    "federated event message payload");

                if (logger.isTraceEnabled()) {
                    logger.trace("Sending message {} from {} to {}", message.toString(), src, dest);
                }

                try {
                    entry.getValue().send(event);

                    /* Track message sends for metrics. */
                    clientMessageCounter.incrementAndGet();
                    clientByteAccumulator.addAndGet(event.getSerializedSize());
                } catch (Exception ex) {
                    logger.error("Exception sending message to client stream", ex);
                    GuardedStreamHolder<FederatedEvent> streamHolder = clientStreamMap.remove(entry.getKey());
                }
            }
        }
    }

    public void sendFederatedEventV1(Message message) {
        for (AddressableEntity<?> entity : message.getDestinations()) {
            if (entity.getEntity() instanceof FederateIdentity) {
                FederateIdentity src = (FederateIdentity)message.getSource().getEntity();
                FederateIdentity dest = (FederateIdentity)entity.getEntity();

                FederationPolicyGraph policyGraph = fedHubPolicyManager.getPolicyGraph();

                Federate srcNode = policyGraph.getFederate(src.getFedId());
                Federate destNode = policyGraph.getFederate(dest.getFedId());

                /* Validate src/dest and nodes. */
                FederateEdge edge = policyGraph.getEdge(srcNode, destNode);

                if (edge != null && !Strings.isNullOrEmpty(edge.getFilterExpression())) {

                    /*
                     * Important note: filters need to be responsible for
                     * mutating the message. It's recommended that they
                     * follow an immutable approach, and return a new
                     * message instead of mutating.
                     */

                    /* TODO Apply the lambda filter. */
                    //message = federateLambdaFilter.filter(message, edge.getFilterExpression());
                }

                if (message == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Message delivery denied by edge filter");
                    }
                    continue;
                }

                deliverV1(message, src, dest);
            }
        }
    }

    private void sendFederatedEvent(Message message) {
        /* Use FederateIdentity as the connection key. */

        /* TODO: bring back SSL_SESSION_ID self-send check. */
//      if (message.getMetadataValue(SSL_SESSION_ID) != null) { // prefer session id
        for (AddressableEntity<?> entity : message.getDestinations()) {
            if (entity.getEntity() instanceof FederateIdentity) {
                FederateIdentity src = (FederateIdentity)message.getSource().getEntity();
                FederateIdentity dest = (FederateIdentity)entity.getEntity();

                FederationPolicyGraph policyGraph = fedHubPolicyManager.getPolicyGraph();

                Federate srcNode = policyGraph.getFederate(src.getFedId());
                Federate destNode = policyGraph.getFederate(dest.getFedId());

                /* Validate src/dest and nodes. */
                FederateEdge edge = policyGraph.getEdge(srcNode, destNode);

                if (edge != null && !Strings.isNullOrEmpty(edge.getFilterExpression())) {

                    /*
                     * Important note: filters need to be responsible for
                     * mutating the message. It's recommended that they
                     * follow an immutable approach, and return a new
                     * message instead of mutating.
                     */

                    /* TODO Apply the lambda filter. */
                    //message = federateLambdaFilter.filter(message, edge.getFilterExpression());
                }

                if (message == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Message delivery denied by edge filter");
                    }
                    continue;
                }

                deliver(message, src, dest);
            }
        }
//      }
    }

    private void sendMessage(Message message) {
        if (!(message.getPayload().getContent() instanceof FederatedEvent ||
                message.getPayload().getContent() instanceof BinaryBlob ||
                message.getPayload().getContent() instanceof ROL)) {
            throw new IllegalArgumentException("Unsupported payload type " +
                message.getPayload().getClass().getName());
        }

        if (message.getPayload().getContent() instanceof FederatedEvent) {
            sendFederatedEvent(message);
        } else if (message.getPayload().getContent() instanceof ROL) {
            sendRolMessage(message);
        } else {
            logger.info("Not handling send to client of " +
                message.getPayload().getContent().getClass().getSimpleName() +
                " yet.");
        }
    }

    private void handleRead(BinaryBlob event, byte[] sessionId) {
        Message federatedMessage = new Message(new HashMap<>(),
            new BinaryBlobPayload(event));
        federatedMessage.setMetadataValue(SSL_SESSION_ID,
            new String(sessionId, Charsets.UTF_8));

        String streamKey = new String(sessionId, Charsets.UTF_8);

        federatedMessage.setMetadataValue(SSL_SESSION_ID, streamKey);

        GuardedStreamHolder<FederatedEvent> streamHolder =
            clientStreamMap.get(streamKey);

        if (streamHolder != null) {
            federatedMessage.setMetadataValue(FEDERATED_ID_KEY,
                streamHolder.getFederateIdentity());
            try {
                assignMessageSourceAndDestinationsFromPolicy(federatedMessage,
                    streamHolder.getFederateIdentity(),
                    fedHubPolicyManager.getPolicyGraph());
                sendMessage(federatedMessage);
            } catch (FederationException e) {
                logger.error("Could not get destinations from policy graph", e);
            }
        }
    }

    private void handleRead(ROL event, String streamKey) {
        Message federatedMessage = new Message(new HashMap<>(),
            new ROLPayload(event));
        federatedMessage.setMetadataValue(SSL_SESSION_ID, streamKey);
        GuardedStreamHolder<ROL> streamHolder =
            clientROLStreamMap.get(streamKey);
        if (streamHolder != null) {
            federatedMessage.setMetadataValue(FEDERATED_ID_KEY,
                streamHolder.getFederateIdentity());
            try {
                assignMessageSourceAndDestinationsFromPolicy(federatedMessage,
                    streamHolder.getFederateIdentity(),
                    fedHubPolicyManager.getPolicyGraph());
                sendMessage(federatedMessage);
            } catch (FederationException e) {
                logger.error("Could not get destinations from policy graph", e);
            }
        } else {
            logger.error("Could not find stream holder for streamkey: {}", streamKey);
        }
    }

    private void handleRead(FederatedEvent event, byte[] sessionId) {
        Message federatedMessage = new Message(new HashMap<>(),
            new FederatedEventPayload(event));
        String streamKey = new String(sessionId, Charsets.UTF_8);
        federatedMessage.setMetadataValue(SSL_SESSION_ID, streamKey);

        GuardedStreamHolder<FederatedEvent> streamHolder = clientStreamMap.get(streamKey);
        if (streamHolder != null) {
            federatedMessage.setMetadataValue(FEDERATED_ID_KEY,
                streamHolder.getFederateIdentity());
            try {
                assignMessageSourceAndDestinationsFromPolicy(federatedMessage,
                    streamHolder.getFederateIdentity(),
                    fedHubPolicyManager.getPolicyGraph());
                sendMessage(federatedMessage);
                if (event != null && event.hasContact()) {
                    streamHolder.getCache().add(event);
                    if (logger.isDebugEnabled()) {
                        logger.debug("caching " + event +
                            "  for " + streamHolder.getFederateIdentity().getFedId());
                    }
                }
            } catch (FederationException e) {
                logger.error("Could not get destinations from policy graph", e);
            } catch (Exception e) {
                logger.error("Exception sending message", e);
            }
        } else {
            logger.error("StreamHolder is null");
        }
    }
}
