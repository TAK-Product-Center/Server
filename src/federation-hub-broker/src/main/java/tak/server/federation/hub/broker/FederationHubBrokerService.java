package tak.server.federation.hub.broker;

import static java.util.Objects.requireNonNull;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.cache.event.CacheEntryEvent;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.Empty;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederateHops;
import com.atakmap.Tak.FederateProvenance;
import com.atakmap.Tak.FederatedChannelGrpc;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.Identity;
import com.atakmap.Tak.ROL;
import com.atakmap.Tak.ServerHealth;
import com.atakmap.Tak.Subscription;
import com.bbn.roger.fig.FederationUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
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
import mil.af.rl.rol.FederationProcessor;
import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import mil.af.rl.rol.value.Parameters;
import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;
import tak.server.federation.FederationNode;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.GuardedStreamHolder;
import tak.server.federation.hub.FederationHubCache;
import tak.server.federation.hub.broker.db.FederationHubMissionDisruptionManager;
import tak.server.federation.hub.broker.events.BrokerServerEvent;
import tak.server.federation.hub.broker.events.HubClientDisconnectEvent;
import tak.server.federation.hub.broker.events.RestartServerEvent;
import tak.server.federation.hub.broker.events.StreamReadyEvent;
import tak.server.federation.hub.broker.events.UpdatePolicy;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.ui.graph.FederationOutgoingCell;
import tak.server.federation.hub.ui.graph.PolicyObjectCell;
import tak.server.federation.rol.MissionRolVisitor;

public class FederationHubBrokerService implements ApplicationListener<BrokerServerEvent> {

	private final Ignite ignite;

    private static final String SSL_SESSION_ID = "sslSessionId";
    private static final String FEDERATED_ID_KEY = "federatedIdentity";

    private static final Logger logger = LoggerFactory.getLogger(FederationHubBrokerService.class);

    private FederationHubBrokerMetrics fedHubBrokerMetrics;
    private HubConnectionStore hubConnectionStore;
    private FederationHubServerConfig fedHubConfig;
    private FederationHubPolicyManager fedHubPolicyManager;
    private FederationHubMissionDisruptionManager federationHubMissionDisruptionManager;
    private SSLConfig sslConfig;

    private int numThreads = Runtime.getRuntime().availableProcessors() + 1;
	private ScheduledExecutorService mfdtScheduler = Executors.newScheduledThreadPool(1);

    /* v1 variables. */
    private final Map<String, NioNettyFederationHubServerHandler> v1ClientStreamMap = new ConcurrentHashMap<>();
    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;
    private ChannelFuture channelFuture;

    /* v2 variables. */
    private Server server;

    private SyncService syncService = new FileCacheSyncService();
    private final FederationProcessorFactory federationProcessorFactory = new FederationProcessorFactory();
    private final FederationHubROLHandler federationHubROLHandler;

    private final AtomicBoolean keepRunning = new AtomicBoolean(true);

    private static ServerHealth serving = ServerHealth
            .newBuilder()
            .setStatus(ServerHealth.ServingStatus.SERVING)
            .build();

    private static ServerHealth notConnected = ServerHealth
            .newBuilder()
            .setStatus(ServerHealth.ServingStatus.NOT_CONNECTED)
            .build();

    // Count messages from all clients, over the lifetime of the server.
    private static final AtomicLong clientMessageCounter = new AtomicLong();
    private static final AtomicLong clientByteAccumulator = new AtomicLong();

    private static FederationHubBrokerService instance = null;

    public static FederationHubBrokerService getInstance() {
    	return instance;
    }

    public ScheduledExecutorService getMfdtScheduler() {
		return mfdtScheduler;
	}
    
	private ContinuousQuery<String, FederationPolicyGraph> continuousConfigurationQuery = new ContinuousQuery<>();

    public FederationHubBrokerService(Ignite ignite, SSLConfig sslConfig, FederationHubServerConfig fedHubConfig, FederationHubPolicyManager fedHubPolicyManager, HubConnectionStore hubConnectionStore, FederationHubMissionDisruptionManager federationHubMissionDisruptionManager,
                                      FederationHubBrokerMetrics fedHubBrokerMetrics) {
    	instance = this;
    	this.ignite = ignite;
    	this.sslConfig = sslConfig;
    	this.fedHubConfig = fedHubConfig;
    	this.fedHubPolicyManager = fedHubPolicyManager;
    	this.hubConnectionStore = hubConnectionStore;
        this.fedHubBrokerMetrics = fedHubBrokerMetrics;
    	this.federationHubMissionDisruptionManager = federationHubMissionDisruptionManager;

    	federationHubROLHandler = new FederationHubROLHandler(federationHubMissionDisruptionManager);

    	setupFederationServers();

    	// rather than hitting ignite every time we need the policy graph,
    	// use event driven approach to always have the updated graph
    	// available here for instant access
    	continuousConfigurationQuery.setLocalListener((evts) -> {
	     	for (CacheEntryEvent<? extends String, ? extends FederationPolicyGraph> e : evts) {
	     		federationPolicyGraph = e.getValue();
	     	}
    	});

    	FederationHubCache.getFederationHubPolicyStoreCache(ignite).query(continuousConfigurationQuery);
    }

    private FederationPolicyGraph federationPolicyGraph;

    public FederationPolicyGraph getFederationPolicyGraph() {
    	if (federationPolicyGraph == null)
    		federationPolicyGraph = fedHubPolicyManager.getPolicyGraph();

    	return federationPolicyGraph;
    }

    private void removeInactiveClientStreams() {
        if (logger.isDebugEnabled()) {
            logger.debug("Running inactivity check for {} client streams", hubConnectionStore.getClientStreamMap().size());
        }

        for (Map.Entry<String, GuardedStreamHolder<FederatedEvent>> clientStreamEntry : hubConnectionStore.getClientStreamMap().entrySet()) {
            if (!clientStreamEntry.getValue().isClientHealthy(fedHubConfig.getClientTimeoutTime())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Detected FederatedEvent client stream {} inactivity",
                        clientStreamEntry.getValue().getFederateIdentity());
                }

                clientStreamEntry.getValue().throwDeadlineExceptionToClient();
                hubConnectionStore.clearIdFromAllStores(clientStreamEntry.getKey());
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

    public boolean addV1ConnectionInfo(String sessionId, HubConnectionInfo info) {
    	Federate clientNode = getFederationPolicyGraph().getFederate(info.getConnectionId());
        if (clientNode == null) {
        	logger.info("Permission Denied. Federate/CA Group not found in the policy graph: " + info.getConnectionId());
        	return false;
        }

    	info.setGroupIdentities(getFederationPolicyGraph().getFederate(info.getConnectionId()).getGroupIdentities());
        hubConnectionStore.addConnectionInfo(sessionId, info);

        return true;
    }

    public void removeV1Connection(String sessionId) {
    	v1ClientStreamMap.remove(sessionId);
        hubConnectionStore.clearIdFromAllStores(sessionId);
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
        FederationPolicyGraph fpg = getFederationPolicyGraph();
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
        for (NioNettyFederationHubServerHandler otherClient : v1ClientStreamMap.values()) {

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
                                String sessionId = new BigInteger(sslHandler.engine().getSession().getId()).toString();

                                NioNettyFederationHubServerHandler handler =
                                    new NioNettyFederationHubServerHandler(sessionId, FederationHubBrokerService.this,
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
    public void onApplicationEvent(BrokerServerEvent event) {
    	if (event instanceof HubClientDisconnectEvent) {
    		outgoingClientMap.remove(((HubClientDisconnectEvent) event).getHubId());
    	}

    	if (event instanceof RestartServerEvent) {
    		if (fedHubConfig.isV2Enabled()) {
                logger.info("Restarting V2 federation server after truststore update");
                server.shutdown();
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) { }
                keepRunning.set(false);

                hubConnectionStore.clearStreamMaps();
                clientMessageCounter.set(0);
                clientByteAccumulator.set(0);
                keepRunning.set(true);
                sslConfig.initSslContext(fedHubConfig);
                setupFederationV2Server();
            }

    		Collection<PolicyObjectCell> cells = fedHubPolicyManager.getPolicyCells();

    		List<FederationOutgoingCell> outgoings = new ArrayList<>();
    		if (cells != null) {
    			cells.stream().filter(cell -> cell instanceof FederationOutgoingCell).forEach( cell -> {
    				outgoings.add((FederationOutgoingCell) cell);
    	        });
       		}

    		updateOutgoingConnections(outgoings);
    	}

        if (event instanceof UpdatePolicy) {
        	// when the policy gets updated, the federate nodes from currently connected spokes will be cleared from the graph
        	// they will not get re-added to the graph until a messages comes over the federate. to get around this,
        	// we can re-add them based on their active sessions

        	hubConnectionStore.getClientStreamMap().entrySet().forEach(entry -> {
        		SSLSession session = hubConnectionStore.getSessionMap().get(entry.getKey());
        		if (session != null && session.isValid()) {
        			addFederateToGroupPolicyIfMissingV2(hubConnectionStore.getSessionMap().get(entry.getKey()) ,entry.getValue());

        			// check if the currently connected spoke is still allowed to be connected after the policy change
        			FederationPolicyGraph fpg = getFederationPolicyGraph();
                    Federate clientNode = checkFederateExistsInPolicy(entry.getValue(), session, fpg);
                    if (clientNode == null) {
                     	logger.info("Permission Denied. Federate/CA Group not found in the policy graph: " + entry.getValue().getFederateIdentity());
                     	entry.getValue().throwPermissionDeniedToClient();

                     	HubFigClient client = outgoingClientMap.get(entry.getKey());
                     	if (client != null)
                     		client.processDisconnect();

                     	return;
                    }

        		} else {
        			hubConnectionStore.removeSession(entry.getKey());
        		}
        	});

        	v1ClientStreamMap.entrySet().forEach(entry -> {
    			// check if the currently connected spoke is still allowed to be connected after the policy change
    			FederationPolicyGraph fpg = getFederationPolicyGraph();
                Federate clientNode = getFederationPolicyGraph().getFederate(entry.getValue().getFederateIdentity());
                if (clientNode == null) {
                 	logger.info("Permission Denied. Federate/CA Group not found in the policy graph: " + entry.getValue().getFederateIdentity());
                 	entry.getValue().forceClose();
                }
        	});

        	updateOutgoingConnections(((UpdatePolicy) event).getOutgoings());
        }

        if (event instanceof StreamReadyEvent) {
        	@SuppressWarnings("rawtypes")
			StreamReadyEvent streamReadyEvent = (StreamReadyEvent) event;

        	switch (streamReadyEvent.getType()) {
			case EVENT:
			{
				break;
			}
			case GROUPS:
			{
				break;
			}
			case ROL:
			{
				@SuppressWarnings("unchecked")
				List<ROL> rolEvents = streamReadyEvent.getEvents();
				rolEvents.forEach(r -> this.parseRol(r, streamReadyEvent.getStreamKey()));
				break;
			}
			default:
				break;
			}
        }
    }

    Map<String, FederationOutgoingCell> outgoingConfigMap = new HashMap<>();
    Map<String, HubFigClient> outgoingClientMap = new HashMap<>();
    Map<String, ScheduledFuture<?>> outgoingClientRetryMap = new HashMap<>();
    ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(1);
    private synchronized void updateOutgoingConnections(List<FederationOutgoingCell> outgoings) {
    	try {
    		// cancel and clear all the current retries
    		outgoingClientRetryMap
    			.values()
    			.stream()
    			.filter(future -> future != null)
    			.forEach(future -> future.cancel(true));

    		outgoingClientRetryMap.clear();

    		// create a temp map for the updated outgoings
    		Map<String, FederationOutgoingCell> updatedOutgoing = new HashMap<>();
    		outgoings.forEach(outgoing -> {
    			updatedOutgoing.put(outgoing.getProperties().getOutgoingName(), outgoing);
    		});


            // check existing outgoings against updated outgoings for ones that got deleted
            Iterator<Entry<String, FederationOutgoingCell>> itr = outgoingConfigMap.entrySet().iterator();
            while(itr.hasNext()) {
            	Entry<String, FederationOutgoingCell> outgoing = itr.next();

            	if (updatedOutgoing.get(outgoing.getKey()) == null) {
            		HubFigClient client = outgoingClientMap.get(outgoing.getKey());
            		outgoingClientMap.remove(outgoing.getKey());

            		if (client != null) {
            			client.processDisconnect();
            		}
            	}
            }

            // replace the old outgoings configs with the new ones
            outgoingConfigMap.clear();
            outgoingConfigMap.putAll(updatedOutgoing);

            // if a new outgoing already has a client, lets stop it so it can be restarted with the updated config
            for (String key: outgoingConfigMap.keySet()) {
            	if (outgoingClientMap.containsKey(key)) {
            		outgoingClientMap.get(key).processDisconnectWithoutRetry();
            	}
            }

            // start all the outgoings defined in the policy
            outgoingConfigMap.entrySet().forEach(e -> {
            	try {
            		if (e.getValue().getProperties().isOutgoingEnabled()) {
            			HubFigClient client = new HubFigClient(fedHubConfig, federationHubMissionDisruptionManager, e.getValue());
                		client.start();
                		outgoingClientMap.put(e.getKey(), client);
            		}
				} catch (Exception e1) {
					logger.error("Error creating hub client", e);
					scheduleRetry(e.getKey(), e.getValue());
				}
            });
		} catch (Exception e) {
			logger.error("Error trying to update outgoing connections", e);
		}
    }

    public void scheduleRetry(String name) {
    	fedHubPolicyManager.getPolicyCells().forEach(cell -> {
    		if (cell instanceof FederationOutgoingCell) {
    			FederationOutgoingCell outgoing = (FederationOutgoingCell) cell;
    			if (outgoing.getProperties().getOutgoingName().equals(name) && outgoing.getProperties().isOutgoingEnabled()) {
    				scheduleRetry(name, outgoing);
    			}
    		}
    	});
    }

    public void scheduleRetry(String name, FederationOutgoingCell outgoing) {
    	if (outgoingClientRetryMap.get(name) != null)
    		outgoingClientRetryMap.get(name).cancel(true);

    	if (fedHubConfig.getOutgoingReconnectSeconds() > 0 && outgoing.getProperties().isOutgoingEnabled()) {
    		logger.info("Connection for {} failed. Trying again in {} seconds.", name, fedHubConfig.getOutgoingReconnectSeconds());
    		ScheduledFuture<?> future = retryScheduler.scheduleAtFixedRate(() -> {
        		attemptRetry(name, outgoing);
    		}, fedHubConfig.getOutgoingReconnectSeconds(), fedHubConfig.getOutgoingReconnectSeconds(), TimeUnit.SECONDS);

    		outgoingClientRetryMap.put(name, future);
    	} else {
    		logger.info("Not attempting a retry for {} because the Outgoing Reconnect is not > 0", name);
    	}
    }

    private void attemptRetry(String name, FederationOutgoingCell outgoing) {
    	try {
			HubFigClient client = new HubFigClient(fedHubConfig, federationHubMissionDisruptionManager, outgoing);
    		client.start();
    		outgoingClientMap.put(name, client);
    		outgoingClientRetryMap.get(name).cancel(true);
    		outgoingClientRetryMap.remove(name);
		} catch (Exception e) {
			logger.error("", e);
			attemptRetry(name, outgoing);
		}
    }

    public void setupFederationServers() {
        sslConfig.initSslContext(fedHubConfig);

        if (fedHubConfig.isV2Enabled()) {
            setupFederationV2Server();
        }

        if (fedHubConfig.isV1Enabled()) {
            setupFederationV1Server();
        }

        retryScheduler.schedule(() -> {
        	 // try to initialize outgoing connections from the saved policy
            List<FederationOutgoingCell> outgoings = fedHubPolicyManager.getPolicyCells()
            		.stream()
            		.filter(c -> c instanceof FederationOutgoingCell)
            		.map(c -> (FederationOutgoingCell) c)
            		.collect(Collectors.toList());

            this.updateOutgoingConnections(outgoings);
        }, 5, TimeUnit.SECONDS);
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

                String groupName = issuerName + "-" + FederationUtils.getBytesSHA256(caCertArray[i].getEncoded());
                caCertNames.add(groupName);
            } catch (CertificateEncodingException e) {
                logger.error("Could not encode certificate", e);
            }
        }

        Federate federate = new Federate(federateIdentity);
        synchronized (federationPolicyGraph) {
        	federationPolicyGraph = fedHubPolicyManager.addCaFederate(federate, caCertNames);
		}
    }

    public void addFederateToGroupPolicyIfMissingV1(Certificate[] certArray,
            FederateIdentity federateIdentity) {
        String fedId = federateIdentity.getFedId();
        if (getFederationPolicyGraph().getNode(fedId) == null) {
            addCaFederateToPolicyGraph(federateIdentity, certArray);
        }
    }

    public void addFederateToGroupPolicyIfMissingV2(SSLSession session, GuardedStreamHolder holder) {
    	hubConnectionStore.addSession(new BigInteger(session.getId()).toString(), session);
        String fedId = holder.getFederateIdentity().getFedId();

        FederationPolicyGraph fpg = getFederationPolicyGraph();

        if (fpg.getNode(fedId) == null) {
            try {
                Certificate[] certArray = session.getPeerCertificates();
                addCaFederateToPolicyGraph(holder.getFederateIdentity(), certArray);
            } catch (SSLPeerUnverifiedException e) {
                logger.error("Could not get peer certificates from the SSL session", e);
            }
        }
    }

    // collect all groups for federates connected to the hub that can reach the given federate
    public FederateGroups getFederationHubGroups(String selfId) {
    	try {
    		// find receivable federates for the given federate
			Collection<String> receivableFederates =
					getFederationPolicyGraph()
						.allReceivableFederates(selfId)
						.stream()
						.map(f -> f.getFederateIdentity().getFedId())
						.collect(Collectors.toCollection(HashSet::new));

	    	List<FederateGroups> federatedGroups = hubConnectionStore.getClientGroupStreamMap().entrySet()
	    			.stream()
	    			// ignore self groups
	    			.filter(e -> !e.getKey().equals(selfId))
	    			// filter out connections that aren't receivable
	    			.filter(e -> receivableFederates.contains(e.getValue().getFederateIdentity().getFedId()))
	    			// filter out receivable connections that don't have groups
	    			.filter(e -> hubConnectionStore.getClientToGroupsMap().get(e.getKey()) != null)
	    			.map(e -> {
	    				FederateGroups groups = hubConnectionStore.getClientToGroupsMap().get(e.getKey());
	    				// return the top level group if there are no nested ones
	    				// otherwise ignore the top level group, and return the list of nested ones
	    				if (groups.getNestedGroupsList().isEmpty()) {
	    					return Arrays.asList(groups);
	    				} else {
	    					return groups.getNestedGroupsList();
	    				}
	    			})
	    			// flatmap the lists of FederateGroups
	    			.flatMap(list -> list.stream())
	    			// filter out FederateGroups that have reached their hop limit
	    			.filter(g -> {
	    				FederateHops federateHops = g.getFederateHops();
	    				if (federateHops != null) {
	    					long maxHops = federateHops.getMaxHops();
	    		    		long currentHops = federateHops.getCurrentHops() + 1;

	    		    		if (currentHops >= maxHops && maxHops != -1) {
	    		    			return false;
	    		    		}
	    				}
	    				return true;
	    			})
	    			// increment the hop count for each FederateGroups
	    			.map(g -> {
	    				FederateGroups.Builder builder = g.toBuilder();
	    				FederateHops hops = builder.getFederateHops()
	    						.toBuilder()
	    						.setCurrentHops(g.getFederateHops().getCurrentHops() + 1)
	    						.build();

	    				builder.setFederateHops(hops);
	    				return builder.build();
	    			})
	    			.collect(Collectors.toList());

	    		// the reason we must use nested groups here is to maintain the hop limit of each individual FederateGroups object.
	    	    // all of the group name strings will still be added to getFederateGroupsList() as usual. TAK Servers will still only look at
	    	    // the getFederateGroupsList(). but federation hubs will look at getNestedGroupsList() before sending to ensure the
	    	    // hop limit has not been reached. (TAK Servers are end of line, so they don't need to check)
	    		FederateGroups.Builder builder = FederateGroups.newBuilder();

	    		federatedGroups.forEach(g -> builder.addNestedGroups(g));

	    		Set<String> federatedGroupsNameSet =
	    				federatedGroups
	    					.stream()
	    					.map(g->g.getFederateGroupsList())
	    					.flatMap(list -> list.stream())
	    					.collect(Collectors.toCollection(HashSet::new));

	    		builder.addAllFederateGroups(federatedGroupsNameSet);

	    		return builder.build();
		} catch (FederationException e) {
			logger.error("Could not get Federation Hub Groups", e);
			return FederateGroups.newBuilder().build();
		}
    }

    private class FederatedChannelService extends FederatedChannelGrpc.FederatedChannelImplBase {

		private final FederationHubBrokerService broker = FederationHubBrokerService.this;
        AtomicReference<Long> start = new AtomicReference<>();



		@Override
		public void serverFederateGroupsStream(Subscription request, StreamObserver<FederateGroups> responseObserver) {
			try {
				SSLSession session = (SSLSession) sslSessionKey.get(Context.current());
				String id = new BigInteger(session.getId()).toString();
				Certificate[] clientCertArray = requireNonNull(
						requireNonNull(session, "SSL Session").getPeerCertificates(), "SSL peer certs array");

				if (clientCertArray.length == 0) {
					throw new IllegalArgumentException("Client certificate not available");
				}

				GuardedStreamHolder<FederateGroups> streamHolder = new GuardedStreamHolder<FederateGroups>(
						responseObserver, request.getIdentity().getName(),
						FederationUtils.getBytesSHA256(clientCertArray[0].getEncoded()), session, request,
						new Comparator<FederateGroups>() {
							@Override
							public int compare(FederateGroups a, FederateGroups b) {
								return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
							}
						}, true);

				addFederateToGroupPolicyIfMissingV2(session, streamHolder);

                FederationPolicyGraph fpg = getFederationPolicyGraph();
	            requireNonNull(fpg, "federation policy graph object");

	            Federate clientNode = checkFederateExistsInPolicy(streamHolder, session, fpg);
                if (clientNode == null) {
                	responseObserver.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
                	return;
                }

				hubConnectionStore.addGroupStream(id, streamHolder);

				// when a client connects to the hub, indicate a serving status and send it the
				// current list of groups
				FederateGroups federateGroups =
						 getFederationHubGroups(streamHolder.getFederateIdentity().getFedId()).toBuilder()
						.setStreamUpdate(ServerHealth.newBuilder().setStatus(ServerHealth.ServingStatus.SERVING).build())
						.build();

				// do not use the stream holder to send the initial groups.
				// since this is a handshake, we are expecting groups back,
				// and the stream holder will attach the provenance, causing
				// it to be dropped on the way back
				responseObserver.onNext(federateGroups);
			} catch (SSLPeerUnverifiedException | CertificateEncodingException e) {
				throw new RuntimeException("Error in serverFederateGroupsStream", e);
			}
		}

		@Override
		public StreamObserver<FederateGroups> clientFederateGroupsStream(StreamObserver<Subscription> responseObserver) {
			return new StreamObserver<FederateGroups>() {
				@Override
				public void onNext(FederateGroups fedGroups) {
					SSLSession session = (SSLSession)sslSessionKey.get(Context.current());
					String id = new BigInteger(session.getId()).toString();

					GuardedStreamHolder<FederatedEvent> holder = hubConnectionStore.getClientStreamMap().get(id);
                	if (holder != null) {
                		addFederateToGroupPolicyIfMissingV2(session, hubConnectionStore.getClientStreamMap().get(id));
                	}

                	GuardedStreamHolder<FederateGroups> groupHolder = hubConnectionStore.getClientGroupStreamMap().get(id);
                	if (groupHolder != null) {
                		addFederateToGroupPolicyIfMissingV2(session, hubConnectionStore.getClientGroupStreamMap().get(id));
                	}

					addFederateGroups(id, fedGroups);
				}

				@Override
				public void onError(Throwable t) {
					logger.error("clientFederateGroupsStream ",t);
					SSLSession session = (SSLSession)sslSessionKey.get(Context.current());
					String id = new BigInteger(session.getId()).toString();
					hubConnectionStore.clearIdFromAllStores(id);
                    responseObserver.onError(t);
				}

				@Override
				public void onCompleted() { }

			};
		}

        @Override
        public void sendOneEvent(FederatedEvent clientEvent, io.grpc.stub.StreamObserver<Empty> emptyReseponse) {
            if (logger.isDebugEnabled()) {
                logger.debug("Received single event from client: " + clientEvent.getEvent().getUid());
            }
            clientMessageCounter.incrementAndGet();
            clientByteAccumulator.addAndGet(clientEvent.getSerializedSize());
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

                    addFederateToGroupPolicyIfMissingV2(session, hubConnectionStore.getClientStreamMap().get(new BigInteger(session.getId()).toString()));

                    FederationHubBrokerService.this.handleRead(value, new BigInteger(session.getId()).toString());
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

            FederationHubBrokerService.this.handleRead(request, new BigInteger(session.getId()).toString());
        }

        private final AtomicInteger clientEventStreamCounter = new AtomicInteger();

        @Override
        public void clientEventStream(Subscription subscription, StreamObserver<FederatedEvent> clientStream) {
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
                    session, subscription, new Comparator<FederatedEvent>() {
                        @Override
                        public int compare(FederatedEvent a, FederatedEvent b) {
                            return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                        }
                    }, true
                );

				addFederateToGroupPolicyIfMissingV2(session, streamHolder);

                FederationPolicyGraph fpg = getFederationPolicyGraph();
	            requireNonNull(fpg, "federation policy graph object");

                Federate clientNode = checkFederateExistsInPolicy(streamHolder, session, fpg);
                if (clientNode == null) {
                	logger.info("Permission Denied. Federate/CA Group not found in the policy graph: " + streamHolder.getFederateIdentity());
                	clientStream.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
                	return;
                }

                // Send contact messages from other clients back to this new client.
                for (GuardedStreamHolder<FederatedEvent> otherClient : hubConnectionStore.getClientStreamMap().values()) {

                    Federate otherClientNode = fpg
                        .getFederate(otherClient.getFederateIdentity().getFedId());

                    if (logger.isDebugEnabled()) {
                        logger.debug("Looking for cached contact messages from other client " +
                            otherClientNode.getFederateIdentity().getFedId());
                    }

                    // Send cached contact messages, iff there is a federated edge between the two federates.
                    FederateEdge edge = fpg.getEdge(otherClientNode, clientNode);
                    if (otherClientNode != null && edge != null) {
                        for (FederatedEvent event : otherClient.getCache()) {
                        	if (isDestinationEdgeReachableByGroupFilter(edge, event.getFederateGroupsList())) {
	                            streamHolder.send(event);
	                            if (logger.isDebugEnabled()) {
	                                logger.debug("Sending v2 cached " + event +
	                                    " from " + otherClientNode.getFederateIdentity().getFedId() +
	                                    " to " + clientNode.getFederateIdentity().getFedId());
	                            }
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
            	String sessionId = new BigInteger(session.getId()).toString();

            	HubConnectionInfo info = new HubConnectionInfo();
            	info.setConnectionId(streamHolder.getFederateIdentity().getFedId());
            	info.setRemoteServerId(subscription.getIdentity().getServerId());
            	info.setRemoteConnectionType(subscription.getIdentity().getType().toString());
            	info.setLocalConnectionType(Identity.ConnectionType.FEDERATION_HUB_SERVER.toString());
            	info.setFederationProtocolVersion(2);
            	info.setGroupIdentities(getFederationPolicyGraph().getFederate(streamHolder.getFederateIdentity().getFedId()).getGroupIdentities());

            	SocketAddress socketAddress = getCurrentSocketAddress();

				if (socketAddress != null) {
					info.setRemoteAddress(socketAddress.toString().replace("/", ""));
				} else {
					info.setRemoteAddress("");
				}

                hubConnectionStore.addConnectionInfo(sessionId, info);
            	hubConnectionStore.addClientStreamHolder(sessionId, streamHolder);

                // send a dummy message to make sure things are initialized
                streamHolder.send(FederatedEvent.newBuilder().build());

                // if groups for this connection exist, send them here as well incase it failed
                if (hubConnectionStore.getClientToGroupsMap().get(sessionId) != null) {
                    addFederateGroups(sessionId, hubConnectionStore.getClientToGroupsMap().get(sessionId));
                }

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
                    clientName, fedCertHash, session, subscription, new Comparator<ROL>() {
                        @Override
                        public int compare(ROL a, ROL b) {
                            return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                        }
                    }, true
                );

				addFederateToGroupPolicyIfMissingV2(session, rolStreamHolder);

                FederationPolicyGraph fpg = getFederationPolicyGraph();
	            requireNonNull(fpg, "federation policy graph object");

	            Federate clientNode = checkFederateExistsInPolicy(rolStreamHolder, session, fpg);
                if (clientNode == null) {
                	clientStream.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
                	return;
                }

                // get the changes, but don't send till we add the rolStream because the stream will get used
                // down the line for getting the session id
                FederationHubMissionDisruptionManager.OfflineMissionChanges changes = null;

                if (fedHubConfig.isMissionFederationDisruptionEnabled()) {
                	changes = federationHubMissionDisruptionManager.getMissionChangesAndTrackConnectEvent(
                    		rolStreamHolder.getFederateIdentity().getFedId(), session.getPeerCertificates());
                }

                /* Keep track of client stream and its associated federate identity. */
                String id = new BigInteger(session.getId()).toString();
                hubConnectionStore.addRolStream(id, rolStreamHolder);

                AtomicLong delayMs = new AtomicLong(0L);
                if (changes != null) {
        			
        			for(final Entry<ObjectId, ROL.Builder> entry: changes.getResourceRols().entrySet()) {
        				mfdtScheduler.schedule(() -> {
        					ROL rol = federationHubMissionDisruptionManager.hydrateResourceROL(entry.getKey(), entry.getValue());
        					rolStreamHolder.send(rol);
        				}, delayMs.getAndAdd(500), TimeUnit.MILLISECONDS);
        			}
        			for(final ROL rol: changes.getRols()) {
        				mfdtScheduler.schedule(() -> {
        					rolStreamHolder.send(rol);
        				}, delayMs.getAndAdd(100), TimeUnit.MILLISECONDS);
        			}
        		}

                logger.info("Client ROL stream added. Count: " + broker.hubConnectionStore.getClientROLStreamMap().size());

            } catch (SSLPeerUnverifiedException | CertificateEncodingException e) {
                throw new RuntimeException("Error obtaining federate client certificate", e);
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

                        SSLSession session = (SSLSession) sslSessionKey.get(Context.current());

                        String sessionId = new BigInteger(session.getId()).toString();
                        addFederateToGroupPolicyIfMissingV2(session, hubConnectionStore.getClientStreamMap().get(sessionId));

                        GuardedStreamHolder<ROL> streamHolder = hubConnectionStore.getClientROLStreamMap().get(sessionId);
                        // sometimes rol comes in before we're completely ready.
                        // temporarily cache rol until the stream holder gets added
                        if (streamHolder == null) {
                        	hubConnectionStore.cacheRol(clientROL, sessionId);
                        } else {
                        	parseRol(clientROL, sessionId);
                        }
                	} catch (Exception e) {
						logger.error("Error with Rol read",e);
					}
                }

                @Override
                public void onError(Throwable t) {
                    Status status = Status.fromThrowable(t);
                    SSLSession session = (SSLSession)sslSessionKey.get(Context.current());
                    String id = new BigInteger(session.getId()).toString();
                    hubConnectionStore.clearIdFromAllStores(id);
                    responseObserver.onError(t);
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
        	SSLSession session = (SSLSession)sslSessionKey.get(Context.current());
            String id = new BigInteger(session.getId()).toString();

        	Subscription subscription = Subscription.newBuilder()
        			.setFilter("")
        			.setIdentity(Identity.newBuilder().setServerId(fedHubConfig.getFullId()).setType(Identity.ConnectionType.FEDERATION_HUB_SERVER).setName(id).setUid(id).build())
        			.build();

        	responseObserver.onNext(subscription);

            return new StreamObserver<FederatedEvent>() {

                @Override
                public void onNext(FederatedEvent fe) {
                    clientMessageCounter.incrementAndGet();
                    clientByteAccumulator.addAndGet(fe.getSerializedSize());

                    // Add federate to group in case policy was updated during connection
                    GuardedStreamHolder<FederatedEvent> holder = hubConnectionStore.getClientStreamMap().get(id);
                	if (holder != null) {
                		addFederateToGroupPolicyIfMissingV2(session, hubConnectionStore.getClientStreamMap().get(id));
                	}
                    // submit to orchestrator
                    FederationHubBrokerService.this.handleRead(fe, new BigInteger(session.getId()).toString());
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
                    hubConnectionStore.clearIdFromAllStores(id);
                    responseObserver.onError(t);
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
            	SSLSession session = (SSLSession)sslSessionKey.get(Context.current());
                String sessionId = new BigInteger(session.getId()).toString();

                if (hubConnectionStore.getClientStreamMap().containsKey(sessionId)) {
                    hubConnectionStore.getClientStreamMap().get(sessionId).updateClientHealth(request);
                    responseObserver.onNext(serving);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No session existed for sessionID: {}", sessionId);
                    }
                    responseObserver.onNext(notConnected);
                }

                if (hubConnectionStore.getClientROLStreamMap().containsKey(sessionId)) {
                    hubConnectionStore.getClientROLStreamMap().get(sessionId).updateClientHealth(request);
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
    }

    public Federate checkFederateExistsInPolicy(GuardedStreamHolder<?> streamHolder, SSLSession session, FederationPolicyGraph fpg) {
    	Federate clientNode = null;
        try {
            String fedId = streamHolder.getFederateIdentity().getFedId();

            clientNode = fpg.getFederate(fedId);

            if (logger.isDebugEnabled()) {
                logger.debug("New client federated event stream client node id: " +
                    streamHolder.getFederateIdentity().getFedId() + " " +
                    clientNode);
            }
		} catch (Exception e) {
			logger.error("could not check federate status",e);
			return null;
		}
        return clientNode;
    }

    final static private Context.Key<SSLSession> sslSessionKey = Context.key("SSLSession");
	final static private Context.Key<SocketAddress> remoteAddressKey = Context.key("RemoteAddress");

    public static ServerInterceptor tlsInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    final Metadata requestHeaders,
                    ServerCallHandler<ReqT, RespT> next) {

                SSLSession sslSession = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
                SocketAddress socketAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);

                Context context = Context.current()
                		.withValue(sslSessionKey, sslSession)
                		.withValue(remoteAddressKey, socketAddress);

                return Contexts.interceptCall(context, call, requestHeaders, next);
            }
        };
    }

    private SocketAddress getCurrentSocketAddress() {
    	return remoteAddressKey.get(Context.current());
	}

    // this ignores group filtering
    public void assignMessageSourceAndDestinationsFromPolicy(Message message,
            FederateIdentity federateIdentity, FederationPolicyGraph policyGraph)
            throws FederationException {
        message.setSource(new AddressableEntity<FederateIdentity>(federateIdentity));
        Set<Federate> destinationNodes = policyGraph.allReachableFederates(federateIdentity.getFedId()).federates;
        destinationNodes.stream().forEach(node ->
            message.getDestinations().add(new AddressableEntity<>(node.getFederateIdentity()))
        );
    }

    public void assignGroupFilteredMessageSourceAndDestinationsFromPolicy(Message message, List<String> groups,
            FederateIdentity federateIdentity) throws FederationException{
    	assignGroupFilteredMessageSourceAndDestinationsFromPolicy(message, groups, federateIdentity,
                getFederationPolicyGraph());
    }

    private void assignGroupFilteredMessageSourceAndDestinationsFromPolicy(Message message, List<String> groups,
            FederateIdentity federateIdentity, FederationPolicyGraph policyGraph)
            throws FederationException {

        message.setSource(new AddressableEntity<FederateIdentity>(federateIdentity));

        getGroupFilteredDestinations(groups, federateIdentity, policyGraph).forEach(node -> {
        	message.getDestinations().add(new AddressableEntity<>(node.getFederateIdentity()));
        });

    }

    public static Set<Federate> getGroupFilteredDestinations(List<String> groups,
            FederateIdentity federateIdentity, FederationPolicyGraph policyGraph)
            throws FederationException {

    	Set<Federate> reachableGroupFilteredFederates = new HashSet<>();

        Set<Federate> destinationNodes = policyGraph.allReachableFederates(federateIdentity.getFedId()).federates;
        Map<FederationNode, FederateEdge> destinationToFederateEdges = policyGraph.allReachableFederates(federateIdentity.getFedId()).getDestinationToFederateEdgeMappings();
        Map<FederationNode, Set<FederateEdge>> destinationToFederateGroupEdges = policyGraph.allReachableFederates(federateIdentity.getFedId()).getDestinationToFederateGroupEdgeMappings();

        destinationNodes.stream().forEach(node -> {
        	// direct edge between two nodes takes top priority for group filtering
        	if (destinationToFederateEdges.containsKey(node)) {
        		FederateEdge edge = destinationToFederateEdges.get(node);
        		if (!isDestinationEdgeReachableByGroupFilter(edge, groups))
        			return;
        	}

        	// edge between nodes where the source is a CA group takes second priority
        	// if there are multiple CA groups for some reason (intermediate certs?)
        	// make sure all the edges pass
        	else if (destinationToFederateGroupEdges.containsKey(node)) {
        		// if any edges are false, fail the check
        		boolean isNotReachableByGroupsForAllEdges = destinationToFederateGroupEdges.get(node)
        			.stream()
        			.map(edge -> isDestinationEdgeReachableByGroupFilter(edge, groups))
        			.anyMatch(isDestinationReachableByGroupFilter -> !isDestinationReachableByGroupFilter);

        		if (isNotReachableByGroupsForAllEdges)
        			return;
        	}

        	// if no edges, they must be interconnected, so skip group filtering
        	reachableGroupFilteredFederates.add(node);
        });

        return reachableGroupFilteredFederates;
    }

    public static boolean isDestinationEdgeReachableByGroupFilter(FederateEdge edge, List<String> groups) {
    	// if no groups are attached, and the group filtering isn't set to
    	// allow all messages through, drop the message
    	if (groups == null || groups.isEmpty()) {
    		if (edge.getFilterType() == FederateEdge.GroupFilterType.ALL) {
    			return true;
    		} else {
    			return false;
    		}
    	}

    	Set<String> groupSet = new HashSet<>(groups);

		switch (edge.getFilterType()) {
		case ALL: {
			return true;
		}
		case ALLOWED: {
			return !Sets.intersection(groupSet, edge.getAllowedGroups()).isEmpty();
		}
		case DISALLOWED: {
			return Sets.intersection(groupSet, edge.getDisallowedGroups()).isEmpty();
		}
		case ALLOWED_AND_DISALLOWED: {
			return !Sets.intersection(groupSet, edge.getAllowedGroups()).isEmpty() &&
					Sets.intersection(groupSet, edge.getDisallowedGroups()).isEmpty();
		}
		default:
			return true;
		}
    }

    private void deliverRol(Message message, FederateIdentity src, FederateIdentity dest) {
        for (Entry<String, GuardedStreamHolder<ROL>> entry : hubConnectionStore.getClientROLStreamMap().entrySet()) {
            if (entry.getValue().getFederateIdentity().equals(dest)) {
                Message filteredMessage = message;

                ROL rol = requireNonNull((ROL)filteredMessage.getPayload().getContent(), "federated rol message payload");

                if (logger.isTraceEnabled()) {
                    logger.trace("Sending message {} from {} to {}", message.toString(), src, dest);
                }

                try {
                    entry.getValue().send(rol);

                    /* Track message sends for metrics. */
                    clientMessageCounter.incrementAndGet();
                    clientByteAccumulator.addAndGet(rol.getSerializedSize());
                } catch (Exception ex) {
                    logger.error("Exception sending message to rol stream", ex);
                    hubConnectionStore.removeRolStream(entry.getKey());
                }
            }
        }
    }

    private void sendRolMessage(Message message) {
        for (AddressableEntity<?> entity : message.getDestinations()) {
            if (entity.getEntity() instanceof FederateIdentity) {
                FederateIdentity src = (FederateIdentity)message.getSource().getEntity();
                FederateIdentity dest = (FederateIdentity)entity.getEntity();

                if (src == dest) continue;

                FederationPolicyGraph policyGraph = getFederationPolicyGraph();

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
        for (Entry<String, GuardedStreamHolder<FederatedEvent>> entry : hubConnectionStore.getClientStreamMap().entrySet()) {
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
                    recordBrokerMetrics(src, dest, message);

                    clientByteAccumulator.addAndGet(event.getSerializedSize());
                } catch (Exception ex) {
                    logger.error("Exception sending message to client stream", ex);
                    hubConnectionStore.clearIdFromAllStores(entry.getKey());
                }
            }
        }
    }

    public void sendFederatedEventV1(Message message) {
        for (AddressableEntity<?> entity : message.getDestinations()) {
            if (entity.getEntity() instanceof FederateIdentity) {
                FederateIdentity src = (FederateIdentity)message.getSource().getEntity();
                FederateIdentity dest = (FederateIdentity)entity.getEntity();

                FederationPolicyGraph policyGraph = getFederationPolicyGraph();

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

                FederationPolicyGraph policyGraph = getFederationPolicyGraph();

                Federate srcNode = policyGraph.getFederate(src.getFedId());
                Federate destNode = policyGraph.getFederate(dest.getFedId());

                /* Validate src/dest and nodes. */
                FederateEdge edge = policyGraph.getEdge(srcNode, destNode);


                deliver(message, src, dest);
            }
        }
    }

    private void deliverGroup(Message message, FederateIdentity src, FederateIdentity dest) {
        for (Entry<String, GuardedStreamHolder<FederateGroups>> entry : hubConnectionStore.getClientGroupStreamMap().entrySet()) {
            if (entry.getValue().getFederateIdentity().equals(dest)) {
                Message filteredMessage = message;

                FederateGroups event = requireNonNull((FederateGroups)filteredMessage.getPayload().getContent(),
                    "federated group message payload");
                if (logger.isTraceEnabled()) {
                    logger.trace("Sending message {} from {} to {}", message.toString(), src, dest);
                }

                try {
                    entry.getValue().send(event);
                } catch (Exception ex) {
                    logger.error("Exception sending message to group stream", ex);
                }
            }
        }
    }

    private void sendFederatedGroup(Message message) {
        for (AddressableEntity<?> entity : message.getDestinations()) {
            if (entity.getEntity() instanceof FederateIdentity) {
                FederateIdentity src = (FederateIdentity)message.getSource().getEntity();
                FederateIdentity dest = (FederateIdentity)entity.getEntity();
                FederateGroups groups = (FederateGroups)message.getPayload().getContent();

                FederationPolicyGraph policyGraph = getFederationPolicyGraph();

                Federate srcNode = policyGraph.getFederate(src.getFedId());
                Federate destNode = policyGraph.getFederate(dest.getFedId());

                /* Validate src/dest and nodes. */
                FederateEdge edge = policyGraph.getEdge(srcNode, destNode);

				FederateGroups updatedGroups = getFederationHubGroups(dest.getFedId()).toBuilder()
						.addAllFederateProvenance(groups.getFederateProvenanceList())
						.build();

				deliverGroup(new Message(new HashMap<>(), new FederatedGroupPayload(updatedGroups)), src, dest);
            }
        }
    }

    private void sendMessage(Message message) {
        if (!(message.getPayload().getContent() instanceof FederatedEvent ||
        		message.getPayload().getContent() instanceof FederateGroups ||
                message.getPayload().getContent() instanceof BinaryBlob ||
                message.getPayload().getContent() instanceof ROL)) {
            throw new IllegalArgumentException("Unsupported payload type " +
                message.getPayload().getClass().getName());
        }

        if (message.getPayload().getContent() instanceof FederatedEvent) {
            sendFederatedEvent(message);
        } else if (message.getPayload().getContent() instanceof ROL) {
            sendRolMessage(message);
        } else if (message.getPayload().getContent() instanceof FederateGroups) {
        	sendFederatedGroup(message);
        }
        else {
            logger.info("Not handling send to client of " +
                message.getPayload().getContent().getClass().getSimpleName() +
                " yet.");
        }
    }

    public void handleRead(BinaryBlob event, String streamKey) {
    	if (hasAlreadySeenMessage(event)) {
    		if (logger.isDebugEnabled()) {
        		logger.debug("Stopping circular event " + event);
        	}
    		return;
        }

        Message federatedMessage = new Message(new HashMap<>(),
            new BinaryBlobPayload(event));
        federatedMessage.setMetadataValue(SSL_SESSION_ID, streamKey);

        federatedMessage.setMetadataValue(SSL_SESSION_ID, streamKey);

        GuardedStreamHolder<FederatedEvent> streamHolder = hubConnectionStore.getClientStreamMap().get(streamKey);

        if (streamHolder != null) {
            federatedMessage.setMetadataValue(FEDERATED_ID_KEY,
                streamHolder.getFederateIdentity());
            try {
            	assignGroupFilteredMessageSourceAndDestinationsFromPolicy(federatedMessage, null,
                    streamHolder.getFederateIdentity(),
                    getFederationPolicyGraph());

                sendMessage(federatedMessage);

            } catch (FederationException e) {
                logger.error("Could not get destinations from policy graph", e);
            }
        }
    }

    public void handleRead(ROL event, String streamKey) {
        Message federatedMessage = new Message(new HashMap<>(),
            new ROLPayload(event));
        federatedMessage.setMetadataValue(SSL_SESSION_ID, streamKey);
        GuardedStreamHolder<ROL> streamHolder = hubConnectionStore.getClientROLStreamMap().get(streamKey);
        if (streamHolder != null) {
            federatedMessage.setMetadataValue(FEDERATED_ID_KEY, streamHolder.getFederateIdentity());
            try {
            	assignGroupFilteredMessageSourceAndDestinationsFromPolicy(federatedMessage, event.getFederateGroupsList(),
                    streamHolder.getFederateIdentity(),
                    getFederationPolicyGraph());
                sendMessage(federatedMessage);
            } catch (FederationException e) {
                logger.error("Could not get destinations from policy graph", e);
            }
        } else {
            logger.error("Could not find stream holder for streamkey: {}", streamKey);
        }
    }

    public void handleRead(FederatedEvent event, String streamKey) {
    	if (hasAlreadySeenMessage(event)) {
    		if (logger.isDebugEnabled()) {
        		logger.debug("Stopping circular event " + event);
        	}
    		return;
        }

        Message federatedMessage = new Message(new HashMap<>(),
            new FederatedEventPayload(event));

        federatedMessage.setMetadataValue(SSL_SESSION_ID, streamKey);

        GuardedStreamHolder<FederatedEvent> streamHolder = hubConnectionStore.getClientStreamMap().get(streamKey);
        if (streamHolder != null) {
            federatedMessage.setMetadataValue(FEDERATED_ID_KEY,
                streamHolder.getFederateIdentity());
            try {
            	// don't group filter contact messages
                if (event != null && event.hasContact()) {
                	assignMessageSourceAndDestinationsFromPolicy(federatedMessage,
                            streamHolder.getFederateIdentity(),
                            getFederationPolicyGraph());

                    sendMessage(federatedMessage);

                    streamHolder.getCache().add(event);
                    if (logger.isDebugEnabled()) {
                        logger.debug("caching " + event +
                            "  for " + streamHolder.getFederateIdentity().getFedId());
                    }
                } else {
                	assignGroupFilteredMessageSourceAndDestinationsFromPolicy(federatedMessage, event.getFederateGroupsList(),
                            streamHolder.getFederateIdentity(),
                            getFederationPolicyGraph());

                    sendMessage(federatedMessage);
                }
            } catch (FederationException e) {
                logger.error("Could not get destinations from policy graph", e);
            } catch (Exception e) {
                logger.error("Exception sending message", e);
            }
        } else {
        	if (logger.isDebugEnabled())
        		logger.debug("StreamHolder is null");
        }
    }

    private boolean hasAlreadySeenMessage(Object event) {
    	try {
    		List<FederateProvenance> federateProvenances = null;


        	if (event instanceof FederatedEvent) {
        		federateProvenances = ((FederatedEvent) event).getFederateProvenanceList();
        	}

        	if (event instanceof FederateGroups) {
        		federateProvenances = ((FederateGroups) event).getFederateProvenanceList();
        	}

        	if (event instanceof BinaryBlob) {
        		federateProvenances = ((BinaryBlob) event).getFederateProvenanceList();
        	}

        	if (event instanceof ROL) {
        		federateProvenances = ((ROL) event).getFederateProvenanceList();
        	}

        	if (federateProvenances == null) return false;

        	Set<String> visitedHubs = federateProvenances
            		.stream()
            		.map(prov -> prov.getFederationServerId())
            		.collect(Collectors.toSet());

           	return visitedHubs.contains(fedHubConfig.getFullId());
    	} catch (Exception e) {
			return false;
		}
    }

    public void addFederateGroups(String sourceId, FederateGroups groups) {
    	if (hasAlreadySeenMessage(groups)) {
    		if (logger.isDebugEnabled()) {
        		logger.debug("Stopping circular event " + groups);
        	}

    		return;
        }

    	hubConnectionStore.putFederateGroups(sourceId, groups);

        Message federatedMessage = new Message(new HashMap<>(),new FederatedGroupPayload(groups));
        federatedMessage.setMetadataValue(SSL_SESSION_ID, sourceId);

        GuardedStreamHolder<?> holder = hubConnectionStore.getClientGroupStreamMap().containsKey(sourceId) ?
        		hubConnectionStore.getClientGroupStreamMap().get(sourceId) : hubConnectionStore.getClientStreamMap().get(sourceId);
        FederateIdentity ident = holder.getFederateIdentity();
        federatedMessage.setMetadataValue(FEDERATED_ID_KEY, ident);
        try {
            assignMessageSourceAndDestinationsFromPolicy(federatedMessage,
                ident,
                getFederationPolicyGraph());
            sendMessage(federatedMessage);
        } catch (FederationException e) {
            logger.error("Could not get destinations from policy graph", e);
        }
    }

    public void parseRol(ROL clientROL, String streamKey) {
    	if (hasAlreadySeenMessage(clientROL)) {
    		if (logger.isDebugEnabled()) {
        		logger.debug("Stopping circular event " + clientROL);
        	}

    		return;
        }

    	handleRead(clientROL, streamKey);

    	// no need to process rol any further if mfd is disabled
    	if (!fedHubConfig.isMissionFederationDisruptionEnabled())
    		return;

		try {
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
                    resource.set(res);
                    operation.set(op);
                    parameters.set(params);

                    logger.info("Evaluating " + op + " on " + resource);

                    return res;
                }
            }).visit(rolParseTree);

            requireNonNull(resource.get(), "resource");
            requireNonNull(operation.get(), "operation");

            GuardedStreamHolder<ROL> streamHolder = hubConnectionStore.getClientROLStreamMap().get(streamKey);

            /* Create a federation processor for this ROL type, and process the ROL program. */
            federationProcessorFactory.newProcessor(resource.get(), operation.get(),
                parameters.get(), streamKey, streamHolder.getFederateIdentity().getFedId()).process(clientROL);

        } catch (Exception e) {
        	logger.error("Exception in ROL parsing " + e.getClass().getName(), e);
        }
	}

    private class FederationProcessorFactory {
		FederationProcessor<ROL> newProcessor(String resource, String operation, Parameters parameters, String streamKey, String federateServerId) {
			return new FederationMissionProcessor(streamKey, federateServerId);
		}
	}

    private class FederationMissionProcessor implements FederationProcessor<ROL> {
    	private final String streamKey;
		private final String federateServerId;

		FederationMissionProcessor(String streamKey, String federateServerId) {
			this.streamKey = streamKey;
			this.federateServerId = federateServerId;
		}

		@Override
		public void process(ROL rol) {
			try {

				if (federationHubROLHandler != null) {
					federationHubROLHandler.onNewEvent(rol, streamKey, federateServerId);
				}

			} catch (RemoteException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception submitting ROL", e);
				}
			}
		}
	}

    private void recordBrokerMetrics(FederateIdentity src, FederateIdentity dest, Message message) {
        FederationPolicyGraph fpg = getFederationPolicyGraph();
        Federate srcFederate = fpg.getFederate(src.getFedId());
        Federate destFederate = fpg.getFederate(dest.getFedId());
        Set<FederateIdentity> srcFederateIdentities = srcFederate.getGroupIdentities();
        Set<FederateIdentity> destFederateIdentities = destFederate.getGroupIdentities();
        String srcCert = null;
        if (!srcFederateIdentities.isEmpty()) {
            Iterator<FederateIdentity> it = srcFederateIdentities.iterator();
            srcCert = it.next().getFedId();
        } else {
            logger.warn("Failed to get cert for src.");
        }
        String destCert = null;
        if (!destFederateIdentities.isEmpty()) {
            Iterator<FederateIdentity> it = destFederateIdentities.iterator();
            destCert = it.next().getFedId();
        } else {
            logger.warn("Failed to get cert for dest.");
        }
        Payload<?> messagePayload = message.getPayload();
        long messageLength = -1;
        if (messagePayload != null) {
            byte[] messageBytes = messagePayload.getBytes();
            if (messageBytes != null) {
                messageLength = messageBytes.length;
            }
        }
        if (srcCert != null && destCert != null && messageLength != -1) {
            fedHubBrokerMetrics.incrementChannelWrite(src.getFedId(), srcCert, dest.getFedId(), destCert, messageLength);
            fedHubBrokerMetrics.incrementChannelRead(src.getFedId(), srcCert, dest.getFedId(), destCert, messageLength);
        }
    }
}