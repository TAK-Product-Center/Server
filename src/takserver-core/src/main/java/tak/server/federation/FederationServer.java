package tak.server.federation;

import static java.util.Objects.requireNonNull;

import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.config.Federation.FederateCA;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.CRUD;
import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.ContactListEntry;
import com.atakmap.Tak.Empty;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedChannelGrpc;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.Identity;
import com.atakmap.Tak.ROL;
import com.atakmap.Tak.ServerHealth;
import com.atakmap.Tak.Subscription;
import com.bbn.cot.filter.DataFeedFilter;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Input;
import com.bbn.marti.config.Tls;
import com.bbn.marti.groups.CommonGroupDirectedReachability;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.exception.DuplicateFederateException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.Reachability;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.FederatedSubscriptionManager;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.federation.FederationROLHandler;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.roger.fig.FederationUtils;
import com.bbn.roger.fig.model.FigServerConfig;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;

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
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Metrics;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import mil.af.rl.rol.FederationProcessor;
import mil.af.rl.rol.MissionRolVisitor;
import mil.af.rl.rol.Resource;
import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import mil.af.rl.rol.value.Parameters;
import mil.af.rl.rol.value.ResourceDetails;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.federation.message.AddressableEntity;
import tak.server.federation.message.Message;
import tak.server.messaging.Messenger;


public class FederationServer {
	private static final Logger logger = LoggerFactory.getLogger(FederationServer.class);
	private static final Logger rolLogger = LoggerFactory.getLogger("rol");
	private static final Logger fedHealthLogger = LoggerFactory.getLogger("fedhealth");

	private static FederationServer fedServer;
	private FigServerConfig config = null;
	private SSLConfig sslConfig = null;
	private Server server;

	private static final AtomicLong clientMessageCounter = new AtomicLong(); // Count messages from all clients, over the lifetime of the server
	private static final AtomicLong clientByteAccumulator = new AtomicLong(); // Count messages from all the clients, over the lifetime of the server

	private final AtomicBoolean keepRunning = new AtomicBoolean(true);

	private static final String SSL_SESSION_ID = "sslSessionId";
	private static final String FEDERATED_ID_KEY = "federatedIdentity";

	// Server Health Responses
	private static final ServerHealth serving;
	private static final ServerHealth notConnected;

	private final Map<String, GuardedStreamHolder<FederatedEvent>> clientStreamMap = new ConcurrentHashMap<>();
	private final Map<String, GuardedStreamHolder<ROL>> clientROLStreamMap = new ConcurrentHashMap<>();
	private final Map<String, String> clientROLStreamNames = new ConcurrentHashMap<>();
	private final Map<String, String> serverFederateMap = new ConcurrentHashMap<>();
	private final Map<String, StreamObserver<FederateGroups>> serverFederateGroupStreamMap = new ConcurrentHashMap<>();
	
	@Autowired
	private DistributedFederationManager federationManager;

	@Autowired
	private SubmissionInterface submission;

	@Autowired
	private Messenger<CotEventContainer> cotMessenger;

	@Autowired
	private GroupFederationUtil groupFederationUtil;

	@Autowired
	private GroupManager groupManager;

	@Autowired
	private SubscriptionManager subscriptionManager;

	@Autowired(required = false)
	private FederationROLHandler federationROLHandler;

	@Autowired
	private MessagingUtilImpl messagingUtil;

	@Autowired
	private FederatedSubscriptionManager federatedSubscriptionManager;

	@Autowired
	private SubscriptionStore subscriptionStore;

	@Autowired
	private MissionDisruptionManager mdm;

	private Configuration coreConfig() {
		return DistributedConfiguration.getInstance().getRemoteConfiguration();
	}

	private Federation fedConfig() {
		return coreConfig().getFederation();
	}

	static {
		serving = ServerHealth.newBuilder().setStatus(ServerHealth.ServingStatus.SERVING).build();
		notConnected = ServerHealth.newBuilder().setStatus(ServerHealth.ServingStatus.NOT_CONNECTED).build();
	}

	public FigServerConfig getConfig() {
		return config;
	}

	public void setConfig(FigServerConfig config) {
		this.config = config;
	}

	public FederationServer() { }

	public static void refreshServer() {
		fedServer.stop();
		fedServer.refreshConfig();
		fedServer.start();
	}

	public static void stopServer() {
	    fedServer.stop();
	}

	@EventListener({ContextRefreshedEvent.class})
	private void init() {
		fedServer = this;

		try {
			// if the federation truststore is pointing to the root truststore - undo it and set to fed truststore.
			if (fedConfig() != null) {

				com.bbn.marti.config.Federation.FederationServer fedServerConfig = fedConfig().getFederationServer();

				if (fedServerConfig != null) {
					Tls fedTls = fedServerConfig.getTls();

					if (fedTls != null) {
						String fedTruststore = fedTls.getTruststoreFile();

						if (fedTruststore != null && fedTruststore.equals("certs/files/truststore-root.jks")) {
							fedTls.setTruststoreFile(DistributedConfiguration.DEFAULT_TRUSTSTORE);
							DistributedConfiguration.getInstance().saveChanges();
						}
					}
				}
			}
		} catch (Exception e) {
			logger.warn("Error checking federation truststore configuration.", e);
		}

		// set up the v2 fed server configuration
		try {

			com.bbn.marti.config.Federation.FederationServer fedServerConfig = fedConfig().getFederationServer();

			if (!fedConfig().isEnableFederation() || fedConfig().getFederationServer().getV2Port() < 1) {
				logger.info("Federation disabled in config. Not starting V2 federation server.");

				return;
			}

			FigServerConfig serverConfig = new FigServerConfig();

			serverConfig.setPort(fedServerConfig.getV2Port());
			serverConfig.setKeystoreFile(fedServerConfig.getTls().getKeystoreFile());
			serverConfig.setKeystorePassword(fedServerConfig.getTls().getKeystorePass());
			serverConfig.setTruststoreFile(fedServerConfig.getTls().getTruststoreFile());
			serverConfig.setTruststorePass(fedServerConfig.getTls().getTruststorePass());
			serverConfig.setSkipGateway(true); // eliminate this
			serverConfig.setMaxMessageSizeBytes(fedConfig().getFederationServer().getMaxMessageSizeBytes()); // put in coreconfig
			serverConfig.setMetricsLogIntervalSeconds(60); // put in coreconfig
			serverConfig.setClientTimeoutTime(15); // put in coreconfig
			serverConfig.setClientRefreshTime(5); // put in coreconfig

			if (logger.isDebugEnabled()) {
				logger.debug("v2 federation config", serverConfig);
			}

			setConfig(serverConfig);

			// start the server
			if (fedServerConfig.isV2Enabled()) {

				if (logger.isDebugEnabled()) {
					logger.debug("scheduling v2 federation server start in " + fedConfig().getFederationServer().getInitializationDelaySeconds() + " seconds");
				}

				start();
			} else {
				logger.info("not starting disabled v2 fed server.");
			}

		} catch (Exception e) {
			logger.warn("Problem starting v2 federation server", e);
		}
	}

	private void refreshConfig() {
	    logger.info("refreshing config");

	    com.bbn.marti.config.Federation.FederationServer fedServerConfig = fedConfig().getFederationServer();


	    FigServerConfig serverConfig = new FigServerConfig();

	    serverConfig.setPort(fedServerConfig.getV2Port());
	    serverConfig.setKeystoreFile(fedServerConfig.getTls().getKeystoreFile());
	    serverConfig.setKeystorePassword(fedServerConfig.getTls().getKeystorePass());
	    serverConfig.setTruststoreFile(fedServerConfig.getTls().getTruststoreFile());
	    serverConfig.setTruststorePass(fedServerConfig.getTls().getTruststorePass());
	    serverConfig.setSkipGateway(true); // eliminate this
	    serverConfig.setMaxMessageSizeBytes(134217728); // put in coreconfig
	    serverConfig.setMetricsLogIntervalSeconds(60); // put in coreconfig
	    serverConfig.setClientTimeoutTime(15); // put in coreconfig
	    serverConfig.setClientRefreshTime(5); // put in coreconfig

	    if (logger.isDebugEnabled()) {
	        logger.debug("v2 federation config", serverConfig);
	    }

	    setConfig(serverConfig);
	}

	private void start() {

		try {
			requireNonNull(config, "v2 fed configuration object");

			sslConfig = new SSLConfig();

			if (config.isEnableHealthCheck()) {
				// Health check thread. Schedule metrics sending every K seconds.
				Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						removeInactiveClientStreams();
						if (!keepRunning.get()) {
							throw new RuntimeException("stopping server"); // cancel further executions of this scheduled job by throwing an exception
						}
					}
				}, config.getClientRefreshTime(), config.getClientRefreshTime(), TimeUnit.SECONDS);
			} else {
				logger.info("health check disabled in config");
			}

			sslConfig.initSslContext(config);

			try {
				sendCaGroupsToFedManager(sslConfig.getTrust());
			} catch (KeyStoreException e) {
				throw new TakException(e);
			}

			NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(config.getPort())
					.maxMessageSize(config.getMaxMessageSizeBytes()) // max message size. If not specified, defaults to 4MB
					.sslContext(sslConfig.getSslContext())
					.executor(Resources.federationGrpcExecutor)
					.workerEventLoopGroup(Resources.federationGrpcWorkerEventLoopGroup)
					.bossEventLoopGroup(Resources.federationGrpcWorkerEventLoopGroup)
					.channelType(NioServerSocketChannel.class);

			if (config.getMaxConcurrentCallsPerConnection() != null && config.getMaxConcurrentCallsPerConnection() > 0) {
				serverBuilder.maxConcurrentCallsPerConnection(config.getMaxConcurrentCallsPerConnection());
			}

			FederatedChannelService service = new FederatedChannelService();

			server = serverBuilder
					.addService(ServerInterceptors.intercept(service, tlsInterceptor()))
					.build();

			service.binaryMessageStream(new StreamObserver<Empty>() {

				@Override
				public void onNext(Empty value) {}

				@Override
				public void onError(Throwable t) {}

				@Override
				public void onCompleted() {}});

			Executors.newSingleThreadExecutor().submit(new Runnable() {

				@Override
				public void run() {

					requireNonNull(config, "FIG configuration object");

					try {

						if (logger.isDebugEnabled()) {
							logger.debug("in v2 fed start executor");
						}

						server.start();

						logger.info("Federation server (v2) started, listening on port " + config.getPort());

						Runtime.getRuntime().addShutdownHook(new Thread() {
							@Override
							public void run() {
								// Use stderr here since the logger may has been reset by its JVM shutdown hook.
								System.err.println("*** shutting down gRPC server since JVM is shutting down");
								FederationServer.this.stop();
								keepRunning.set(false);
								System.err.println("*** server shut down");
							}
						});
					} catch (Exception e) {
						logger.error("exception starting v2 fed server (inner)", e);
					}
				}
			});

		} catch (Exception e) {
			logger.error("Exception starting v2 fed server (outer)", e);
		}
	}

	public void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

	private class FederatedChannelService extends FederatedChannelGrpc.FederatedChannelImplBase {

		private final FederationServer fs = FederationServer.this;

		private final FederationProcessorFactory federationProcessorFactory = new FederationProcessorFactory();

		AtomicReference<Long> start = new AtomicReference<>();

		@Override
		public void sendOneEvent(FederatedEvent clientEvent, io.grpc.stub.StreamObserver<Empty> emptyReseponse) {
			if (logger.isDebugEnabled()) {
				logger.debug("received single event from client: " + clientEvent.getEvent().getUid());
			}
			clientMessageCounter.incrementAndGet();
			clientByteAccumulator.addAndGet(clientEvent.getSerializedSize());
		}

		@Override
		public StreamObserver<BinaryBlob> binaryMessageStream(StreamObserver<Empty> responseObserver) {

			return new StreamObserver<BinaryBlob>() {

				@Override
				public void onNext(BinaryBlob value) {

					SSLSession session = (SSLSession) sslSessionKey.get(Context.current());

					// submit message
					FederationServer.this.handleRead(value, session.getId());
				}

				@Override
				public void onError(Throwable t) {
					logger.error("exception in binary message stream", t);
				}

				@Override
				public void onCompleted() {
					logger.info("binary message stream complete");
				}
			};
		}

		@Override
		public void sendOneBlob(BinaryBlob request, StreamObserver<Empty> resp) {
			start.compareAndSet(null, System.currentTimeMillis());

			SSLSession session = (SSLSession) sslSessionKey.get(Context.current());

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
				logger.trace("sendOneBlob received binary file from client: " + request.getDescription() + " " + new Date(request.getTimestamp()) + " " + request.getSerializedSize() + " bytes (serialized)  latency: " + latency + " ms " + mps + " messages per second, " + bytesPerSecond + " bytes per second");
			}

			clientMessageCounter.incrementAndGet();
			clientByteAccumulator.addAndGet(request.getSerializedSize());

			// submit to orchestrator
			FederationServer.this.handleRead(request, session.getId());
		}

		private final AtomicInteger clientEventStreamCounter = new AtomicInteger();

		/*
		 * Handle federated event stream subscription request from clients (other v2 fed takservers)
		 *
		 */
		@Override
		public void clientEventStream(Subscription subscription, StreamObserver<FederatedEvent> clientStream) {
			requireNonNull(subscription, "client-specified subscription");
			requireNonNull(subscription.getIdentity(), "client-specified identity");
			FigServerFederateSubscription fedSub = null;
			String fedName = subscription.getIdentity().getName();

			if (Strings.isNullOrEmpty(fedName)) {
				throw new IllegalArgumentException("invalid clientEventStream request from client - null or empty name was provided");
			}

			int streamCount = clientEventStreamCounter.incrementAndGet();

			SSLSession session = (SSLSession) sslSessionKey.get(Context.current());

			GuardedStreamHolder<FederatedEvent> streamHolder = null;
			
			if (fedHealthLogger.isDebugEnabled()) {
				fedHealthLogger.debug("open federation clientEventStream " + subscription);
			}

			try {
				Certificate[] clientCertArray = requireNonNull(requireNonNull(session, "SSL Session").getPeerCertificates(), "SSL peer certs array");

				if (clientCertArray.length == 0) {
					throw new IllegalArgumentException("client cert not available");
				}

				streamHolder = new GuardedStreamHolder<FederatedEvent>(clientStream, fedName, FederationUtils.getBytesSHA256(clientCertArray[0].getEncoded()), session, subscription, new Comparator<FederatedEvent>() {

					@Override
					public int compare(FederatedEvent a, FederatedEvent b) {
						return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
					}
				}, false);

				ConnectionInfo connection = new ConnectionInfo();
				connection.setConnectionId(getCurrentSessionId());

				fedSub = (FigServerFederateSubscription) federatedSubscriptionManager.getFederateSubscription(connection);

				if (logger.isDebugEnabled()) {
					logger.debug("fedSub: " + fedSub);
				}

				federatedSubscriptionManager.putClientStreamToSession(getCurrentSessionId(), streamHolder);

				if (logger.isDebugEnabled()) {
					logger.debug("set client event stream holder for fed sub in clientEventStream");
				}

				if (config.isUseCaGroups()) {
					addCaFederateToPolicyGraph(streamHolder, clientCertArray);
				}

				// keep track of clientStream and its associated federate identity
				String id = new String(session.getId(), Charsets.UTF_8);
				fs.clientStreamMap.put(id, streamHolder);

			} catch (SSLPeerUnverifiedException | CertificateEncodingException e) {
				throw new RuntimeException("error obtaining federate client cert", e);
			}

			try {
				if (logger.isDebugEnabled()) {
					logger.debug("registering FederateIdentity: " + streamHolder.getFederateIdentity());
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("client stream added. count: " + streamCount);
			}

			if (fedSub != null) {
				sendLocalContacts(fedSub);
				sendLatestSAs(fedSub);
			} else if (logger.isDebugEnabled()){
				logger.debug("Could not find v2 federate subscription for client stream. SA and local contacts not being sent.");
			}
		}

		private void sendLocalContacts(FigServerFederateSubscription fedSub) {
			try {
				// send a current snapshot of the contact list (current subscriptions) to this federate
				if (logger.isDebugEnabled()) {
					logger.debug("send latest contacts");
				}
				groupFederationUtil.sendLatestContactsToFederate(fedSub);
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception sending latest contacts as v2 federate server", e);
				}
			}
		}

		private void sendLatestSAs(FigServerFederateSubscription fedSub) {
			try {
				if (coreConfig().getBuffer().getLatestSA().isEnable()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Sending latest SA as v2 federate server");
					}
					messagingUtil.sendLatestReachableSA(fedSub.getUser());
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception sending latest SA as v2 federate server", e);
				}
			}
		}

		/*
		 * Establish a stream for sending ROL to the client (client-initiated)
		 */
		@Override
		public void clientROLStream(Subscription subscription, StreamObserver<ROL> clientStream) {
			requireNonNull(subscription, "client-specified subscription");
			requireNonNull(subscription.getIdentity(), "client-specified identity");

			String clientName = subscription.getIdentity().getName();

			if (Strings.isNullOrEmpty(clientName)) {
				throw new IllegalArgumentException("invalid clientEventStream request from client - null or empty name was provided");
			}
			
			if (fedHealthLogger.isDebugEnabled()) {
				fedHealthLogger.debug("open federation clientROLStream " + subscription);
			}

			try {
				
				if (fedHealthLogger.isDebugEnabled()) {
					fedHealthLogger.debug("open federation clientROLStream " + subscription);
				}
				
				SSLSession session = (SSLSession) sslSessionKey.get(Context.current());

				Certificate[] clientCertArray = requireNonNull(requireNonNull(session, "SSL Session").getPeerCertificates(), "SSL peer certs array");

				if (clientCertArray.length == 0) {
					throw new IllegalArgumentException("client cert not available");
				}

				String fedCertHash = FederationUtils.getBytesSHA256(clientCertArray[0].getEncoded());

				if (rolLogger.isDebugEnabled()) {
					rolLogger.debug("cert hash of federate sending ROL: " + fedCertHash);
				}

				GuardedStreamHolder<ROL> rolStreamHolder = new GuardedStreamHolder<>(
						clientStream,
						clientName,
						fedCertHash,
						session,
						subscription,
						(ROL a, ROL b) -> ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result(), false);

				federatedSubscriptionManager.putClientROLStreamToSession(getCurrentSessionId(), rolStreamHolder);

				// keep track of clientStream and its associated federate identity
				String id = new String(session.getId(), Charsets.UTF_8);
				fs.clientROLStreamMap.put(id, rolStreamHolder);
				fs.clientROLStreamNames.put(id, clientName);

				if (rolLogger.isDebugEnabled()) {
					rolLogger.debug("client ROL stream added. count: " + fs.clientROLStreamMap.size());
				}

				ConnectionInfo connection = new ConnectionInfo();
				connection.setConnectionId(getCurrentSessionId());

				try {
					// send out data feeds to federate
					if (DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
						List<ROL> feedMessages = mdm.getDataFeedEvents();
						
						AtomicLong delayMs = new AtomicLong(100L);										
						for (final ROL feedMessage : feedMessages) {
							Resources.scheduledClusterStateExecutor.schedule(() -> {
								try {
									clientStream.onNext(feedMessage);
								} catch (Exception e) {
									logger.error("exception federating data feed", e);
								}
							}, delayMs.getAndAdd(100), TimeUnit.MILLISECONDS);
						}
					}
				} catch (Exception e) {
					logger.error("exception federating data feeds", e);
				}

				// send missions from disruption period
				// use fed sub to send ROL. get groups from federate.
				if (fedConfig().isEnableMissionFederationDisruptionTolerance()) {

					try {

						if (logger.isDebugEnabled()) {
							logger.debug("mission federation disruption tolerance enabled");
						}
						
						final String sessionId = getCurrentSessionId();
						
						if (logger.isDebugEnabled()) {
							logger.debug("FederationServer sessionId: " + sessionId);
						}

						com.bbn.marti.config.Federation.Federate federate = federationManager.getFederate(serverFederateMap.get(sessionId));
						
						if (federate == null) {
							if (logger.isDebugEnabled()) {
								logger.debug("can't send federation changes - null federate");
								return;
							}
						}
						
						if (Strings.isNullOrEmpty(federate.getId())) {
							if (logger.isDebugEnabled()) {
								logger.debug("can't send federation changes - empty federate id");
								return;
							}
						}

						Resources.fedReconnectThreadPool.schedule(() -> {

							try {

								ConnectionInfo connectionInfo = new ConnectionInfo();
								FigServerFederateSubscription fedSubscription = null;

								try {
									connectionInfo.setConnectionId(sessionId);
									fedSubscription = (FigServerFederateSubscription) federatedSubscriptionManager.getFederateSubscription(connectionInfo);
								} catch (Exception e) {
									if (logger.isDebugEnabled()) {
										logger.debug("exception getting subscription", e);
									}
								}
								List<ROL> missionChanges = mdm.getMissionChangesAndTrackConnectEvent(federate, federate.getName(), fedSubscription);

								if (logger.isTraceEnabled()) {
									logger.trace("mission disruption changes to send: " + missionChanges);
								}

								final AtomicInteger changeCount = new AtomicInteger(0);

								Set<String> outGroups = null;
								if (federate.isFederatedGroupMapping()) {
									NavigableSet<Group> groups = groupManager.getGroups(fedSubscription.getUser());
									outGroups = GroupFederationUtil.getInstance().filterFedOutboundGroups(federate.getOutboundGroup(), groups, federate.getId());
								}

								final Set<String> fOutGroups = outGroups;

								final List<ROL> changeMessages = new CopyOnWriteArrayList<>();

								missionChanges.forEach((change) -> {

									if (federate.isFederatedGroupMapping() && fOutGroups != null && fOutGroups.size() > 0) {
										ROL.Builder changeBuilder = change.toBuilder();
										changeBuilder.addAllFederateGroups(fOutGroups);
										changeMessages.add(changeBuilder.build());
									}

									changeCount.incrementAndGet();
								});

								AtomicLong delayMs = new AtomicLong(0L);

								if (rolLogger.isDebugEnabled()) {
									rolLogger.debug(changeMessages.size() + " mission disruption changes to federate");
								}

								// stagger sending mission changes
								for (final ROL fedChange : changeMessages) {
									Resources.scheduledClusterStateExecutor.schedule(() -> {
										try {
											clientStream.onNext(fedChange);
										} catch (Exception e) {
											logger.error("exception federating mission disruption change", e);
										}
									}, delayMs.getAndAdd(100), TimeUnit.MILLISECONDS);
								}

								logger.info(changeCount.get() + " mission changes federated");

								if (logger.isDebugEnabled()) {
									logger.debug("mission changes sent to federate");
								}
							} catch (Exception e) {
								logger.warn("error sending mission disruption changes to federate client", e);
							}

						}, DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getHealthCheckIntervalSeconds(), TimeUnit.SECONDS);
					} catch (Exception e) {
						logger.warn("exception getting or sending federation changes", e);
					}
				}

			} catch (SSLPeerUnverifiedException | CertificateEncodingException e) {
				throw new RuntimeException("error obtaining federate client cert", e);
			}
		}

		/*
		 * Handle ROL sent from client to server
		 */
		@Override
		public StreamObserver<ROL> serverROLStream(StreamObserver<Subscription> responseObserver) {
			return new StreamObserver<ROL>() {
				
				{
					
					if (fedHealthLogger.isDebugEnabled()) {
						fedHealthLogger.debug("open federation serverROLStream ");
					}
				}

				@Override
				public void onNext(ROL clientROL) {
					try {
						if (rolLogger.isDebugEnabled()) {
							rolLogger.debug("ROL from client: " + clientROL.getProgram());
						}

						if (clientROL == null || clientROL.getProgram() == null || clientROL.getProgram().isEmpty()) {
							return;
						}

						// interpret and execute the ROL program
						RolLexer lexer = new RolLexer(new ANTLRInputStream(clientROL.getProgram()));

						CommonTokenStream tokens = new CommonTokenStream(lexer);

						RolParser parser = new RolParser(tokens);
						parser.setErrorHandler(new BailErrorStrategy());

						// parse the ROL program
						ParseTree rolParseTree = parser.program();

						requireNonNull(rolParseTree, "parsed ROL program");

						final AtomicReference<String> resource = new AtomicReference<>();
						final AtomicReference<String> operation = new AtomicReference<>();
						final AtomicReference<Parameters> parameters = new AtomicReference<>();

						new MissionRolVisitor(new ResourceOperationParameterEvaluator<Parameters, String>() {
							@Override
							public String evaluate(String res, String op, Parameters params) {
								if (rolLogger.isDebugEnabled()) {
									rolLogger.debug(" evaluating " + op + " on " + resource + " given " + params);
								}

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

						// create a federation processor for this ROL type, and process the ROL program
						federationProcessorFactory.newProcessor(resource.get(), operation.get(), parameters.get(), sessionId).process(clientROL);

					} catch (Exception e) {
						
						if (logger.isDebugEnabled()) {
							logger.debug("exception in ROL processing", e);
						}

						ConnectionInfo connectionInfo = new ConnectionInfo();
				        FigServerFederateSubscription subscription = null;
				        AbstractBroadcastingChannelHandler handler = null;

				        try {
				        	connectionInfo.setConnectionId(getCurrentSessionId());
				        	subscription = (FigServerFederateSubscription) federatedSubscriptionManager.getFederateSubscription(connectionInfo);
				        	handler = (AbstractBroadcastingChannelHandler) subscription.getHandler();
				        } catch (Exception eee) { }

						String msg = "ROL stream disconnection";

						if (e instanceof io.grpc.StatusException) {
							if (((io.grpc.StatusException) e).getStatus().getCode().equals(Status.CANCELLED.getCode())) {
								//SSLSession session = (SSLSession) sslSessionKey.get(Context.current());
								// could look up which connection it is. But the name is gibberish!

								if (logger.isDebugEnabled()) {
									logger.debug(msg);
								}

								try {
									messagingUtil.processFederateClose(connectionInfo, handler, subscription);
								} catch (Exception ee) {
									logger.warn("exception cleaning up federated subscription", e);
								}

							}
						} else if (e instanceof io.grpc.StatusRuntimeException) {
							if (((io.grpc.StatusRuntimeException) e).getStatus().getCode().equals(Status.CANCELLED.getCode())) {
								//SSLSession session = (SSLSession) sslSessionKey.get(Context.current());
								// could look up which connection it is. But the name is gibberish!

								if (logger.isDebugEnabled()) {
									logger.debug(msg);
								}

								try {
									messagingUtil.processFederateClose(connectionInfo, handler, subscription);
								} catch (Exception ee) {
									logger.warn("exception cleaning up federated subscription", ee);
								}

							}
						} else {
							logger.error("exception in ROL event stream call", e);
						}
					}
				}

				@Override
				public void onError(Throwable t) {
					
					if (logger.isDebugEnabled()) {
		        		logger.debug("ROL stream onError", t);
		        	}

			        ConnectionInfo connectionInfo = new ConnectionInfo();
			        FigServerFederateSubscription subscription = null;
			        AbstractBroadcastingChannelHandler handler = null;

			        try {
			        	connectionInfo.setConnectionId(getCurrentSessionId());
			        	subscription = (FigServerFederateSubscription) federatedSubscriptionManager.getFederateSubscription(connectionInfo);
			        	handler = (AbstractBroadcastingChannelHandler) subscription.getHandler();
			        } catch (Exception e) { }

			        if (logger.isDebugEnabled()) {
			        	logger.debug("fed sub on ROL stream error: " + subscription);
			        }

					String msg = "ROL stream disconnection";

					if (t instanceof io.grpc.StatusException) {
						if (((io.grpc.StatusException) t).getStatus().getCode().equals(Status.CANCELLED.getCode())) {

							if (logger.isDebugEnabled()) {
								logger.debug(msg);
							}

							try {
								messagingUtil.processFederateClose(connectionInfo, handler, subscription);
							} catch (Exception e) {
								logger.warn("exception cleaning up federated subscription", e);
							}
						}
					} else if (t instanceof io.grpc.StatusRuntimeException) {
						if (((io.grpc.StatusRuntimeException) t).getStatus().getCode().equals(Status.CANCELLED.getCode())) {

							if (logger.isDebugEnabled()) {
								logger.debug(msg);
							}

							try {
								messagingUtil.processFederateClose(connectionInfo, handler, subscription);
							} catch (Exception e) {
								logger.warn("exception cleaning up federated subscription", e);
							}
						}
					} else {
						logger.error("exception in ROL event stream call", t);
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

		// handle subscription requests from other takservers and open a stream of messages to send back to them
		@Override
		public StreamObserver<FederatedEvent> serverEventStream(StreamObserver<Subscription> responseObserver) {
			String sessionId = getCurrentSessionId();
                        
        	Subscription subscription = Subscription.newBuilder()
        			.setFilter("")
        			.setIdentity(Identity.newBuilder().setType(Identity.ConnectionType.FEDERATION_TAK_SERVER).setServerId(DistributedConfiguration.getInstance().getRemoteConfiguration().getNetwork().getServerId()).setName(sessionId).setUid(sessionId).build())
        			.build();
        	
        	responseObserver.onNext(subscription);
			return new StreamObserver<FederatedEvent>() {
				
				FigServerFederateSubscription federateSubscription = null;
				AbstractBroadcastingChannelHandler channelHandler = null;
				// create user and subscription for v2 fed connection
				{
					String sessionId = getCurrentSessionId();
					ConnectionInfo connectionInfo = null;
					try {
						
						if (fedHealthLogger.isDebugEnabled()) {
							fedHealthLogger.debug("open federation serverEventStream ");
						}
						
						X509Certificate cert = (X509Certificate) getCurrentClientCert();
						X509Certificate caCert = (X509Certificate) getCurrentCaCert();
						String principalDN = cert.getSubjectX500Principal().getName();
						String issuerDN = cert.getIssuerX500Principal().getName();
						String fingerprint = RemoteUtil.getInstance().getCertSHA256Fingerprint(cert); // Get the cert fingerprint

						boolean matchingCA = GroupFederationUtil.getInstance().isRemoteCASelfCA(caCert);

						if (logger.isDebugEnabled()) {
							logger.debug("fed cert principalDN: " + principalDN + " issuerDN: " + issuerDN + " fingerprint: " + fingerprint);
						}

						// this will throw an exception if the principal or issuer dn can't be obtained
						String certName = MessageConversionUtil.getCN(principalDN) + ":" + MessageConversionUtil.getCN(issuerDN);

						if (logger.isDebugEnabled()) {
							logger.debug("federate user name from DNs: " + certName);
						}

						// Look for a configured federate with this fingerprint.
						List<ConnectionStatus> dupeFederates = new ArrayList<>();

						// serialize federate config get/set operations
						Federate federate = federationManager.getFederate(fingerprint);

						if (federate == null) {
							if (logger.isDebugEnabled()) {
								logger.debug("CoreConfig federate not found for fingerprint / id: " + fingerprint);
							}
							// put an empty federate with the name and id in the in-memory CoreConfig. Don't save the coreConfig().xml yet, let that be up to the front-end.
							federate = new Federate();
							federate.setId(fingerprint);
							federate.setName(certName);
							federate.setFederatedGroupMapping(DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isFederatedGroupMapping());
							federate.setAutomaticGroupMapping(DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isAutomaticGroupMapping());

		                    for (FederateCA ca : fedConfig().getFederateCA()) {
		                        if (ca.getFingerprint().compareTo(RemoteUtil.getInstance().getCertSHA256Fingerprint(caCert)) == 0) {
		                            for (String groupname : ca.getInboundGroup()) {
		                                federate.getInboundGroup().add(groupname);
		                            }
		                            for (String groupname : ca.getOutboundGroup()) {
		                                federate.getOutboundGroup().add(groupname);
		                            }
		                            break;
		                        }
		                    }

							federationManager.addFederateToConfig(federate);

							if (logger.isDebugEnabled()) {
								logger.debug("federate added to config for id / fingerprint " + fingerprint);
							}
						} else {

							if (logger.isDebugEnabled()) {
								logger.debug("matched existing federate by fingerprint: " + fingerprint + " " + federate.getName());
							}

							// if we matched a federate, there may be an existing connection for it.
							for (ConnectionStatus status : federationManager.getActiveConnectionInfo()) {
								if (status.getFederate() != null && status.getFederate().equals(federate)) {

									dupeFederates.add(status);

									logger.info("Disallowing duplicate federate connection for federate " + federate.getName() + " " + federate.getId() + " " + new SecureRandom().nextInt());
								}
							}
						}

						
						serverFederateMap.put(getCurrentSessionId(), federate.getId());
						
						if (logger.isDebugEnabled()) {
							logger.debug("put session id " + getCurrentSessionId() + " in map for federate id " + federate.getId());
						}
						
						connectionInfo = new ConnectionInfo();

						SocketAddress socketAddress = getCurrentSocketAddress();
						
						if (socketAddress != null) {
							connectionInfo.setAddress(socketAddress.toString().replace("/", ""));
						} else {
							connectionInfo.setAddress("");
						}
						
						connectionInfo.setConnectionId(sessionId);
						connectionInfo.setPort(fedConfig().getFederationServer().getV2Port());
						connectionInfo.setTls(true);

						FederateUser user = new FederateUser(fingerprint, connectionInfo.getConnectionId(), principalDN, "", cert, new X509Certificate[0], federate);

						groupManager.addUser(user);

						// setup federate groups. For a new federate config object, these lists will be empty.
						for (String groupName : federate.getInboundGroup()) {
							groupManager.addUserToGroup(user, new Group(groupName, Direction.IN));
						}
						List<String> outboundGroups = federate.getOutboundGroup();
						for (String groupName : outboundGroups) {
							groupManager.addUserToGroup(user, new Group(groupName, Direction.OUT));
						}

						for (String groupName : federationManager.getInboundGroupMap(federate.getId()).values()) {
							groupManager.hydrateGroup(new Group(groupName, Direction.IN));
						}

						final ConnectionInfo ciFinal = connectionInfo;

						try {

							channelHandler = new AbstractBroadcastingChannelHandler() {

								{
									withHandlerType("V2FederationServer");
									this.connectionInfo = ciFinal; // In certain cases, like when handling point to point messages, this AbstractBroadcastingChannelHandler will be used to
								}

								private final String notImplementedMessage = "No-op v2 fed client ChannelHandler";

								@Override
								public AsyncFuture<ChannelHandler> connect() {
									throw new UnsupportedOperationException(notImplementedMessage);
								}

								@Override
								public AsyncFuture<Integer> write(ByteBuffer buffer) {
									throw new UnsupportedOperationException(notImplementedMessage);
								}

								@Override
								public AsyncFuture<ChannelHandler> close() {
									throw new UnsupportedOperationException(notImplementedMessage);
								}

								@Override
								public void forceClose() {
									try {

										federatedSubscriptionManager.getClientStreamBySession(sessionId).throwDeadlineExceptionToClient();;
									}catch (Exception e) {
										if (logger.isInfoEnabled()) {
											logger.info("Could not forceClose V2 federation server handler " + e);
										}
									}
								}

								@Override
								public InetAddress host() {
									throw new UnsupportedOperationException(notImplementedMessage);
								}

								@Override
								public int port() {
									throw new UnsupportedOperationException(notImplementedMessage);
								}

								@Override
								public String netProtocolName() {
									throw new UnsupportedOperationException(notImplementedMessage);
								}

								@Override
								public boolean isMatchingInput(Input input) {
									return false;
								}

								@Override
								public String toString() {
									return "v2 fed channel handler " + connectionInfo;
								}
							};

							connectionInfo.setHandler(channelHandler);

							// always use the default reachability
							Reachability<User> reachability = new CommonGroupDirectedReachability(groupManager);

							String sessionHash = Hashing.sha256().hashBytes(sessionId.getBytes()).toString();

							String subscriptionUid = "FedClient_v2_" + sessionHash;

							// Create the subscription for the FIG federate, so that messages can be delivered
							federateSubscription = federationManager.addFigFederateSubscriptionV2(subscriptionUid, null, channelHandler, null, null, user.getFederateConfig().isShareAlerts(), connectionInfo, federate);

							logger.info("created v2 federate subscription " + user.getId());

							if (federate.isFederatedGroupMapping() && serverFederateGroupStreamMap.get(getCurrentSessionId()) != null) {
								List<String> outgoingGroups =  federate.getOutboundGroup();
								if (outgoingGroups != null) {
									if (logger.isDebugEnabled()) {
										logger.debug("preparing to send server federate groups = " + federate.getOutboundGroup());
									}
									serverFederateGroupStreamMap.get(getCurrentSessionId()).onNext(FederateGroups.newBuilder().addAllFederateGroups(outgoingGroups).build());
								}
							}

							// set user on subscription, so that message brokering will be able to find the user
							federateSubscription.setUser(user);
							federateSubscription.callsign = subscriptionUid; // using subscription UID
							federateSubscription.setReachability(reachability);
							federateSubscription.setHandler(channelHandler);
							federateSubscription.setIsAutoMapped((matchingCA && federate.isFederatedGroupMapping() && federate.isAutomaticGroupMapping()));


							if (logger.isDebugEnabled()) {
								logger.debug("adding federate subscription: " + federateSubscription);
							}
							// track this subscription generally
							subscriptionManager.addRawSubscription(federateSubscription);
							federationManager.updateFederationSubscriptionCache(connectionInfo, user.getFederateConfig());

							if (federate.isFederatedGroupMapping() && serverFederateGroupStreamMap.get(getCurrentSessionId()) != null) {
								List<String> outgoingGroups =  federate.getOutboundGroup();
								if (outgoingGroups != null) {
									if (logger.isDebugEnabled()) {
										logger.debug("preparing to send server federate groups = " + federate.getOutboundGroup());
									}
									serverFederateGroupStreamMap.get(getCurrentSessionId()).onNext(FederateGroups.newBuilder().addAllFederateGroups(outgoingGroups).build());
								}
							}


							if (federateSubscription != null) {
								sendLocalContacts(federateSubscription);
								sendLatestSAs(federateSubscription);
							}

		        			if (logger.isDebugEnabled()) {
		        				logger.debug("ServerEventStreamclientCall ready.");
		        			}

						} catch (Exception e) {
							logger.info("exception setting up fig federate groups " + e.getMessage(), e);
						}

					} catch (DuplicateFederateException e) {
						// let this propagate
						throw e;
					} catch (Exception e) {
						logger.warn("exception creating federate user: " + e.getMessage(), e);
					}
				}

				@Override
				public void onNext(FederatedEvent fe) {
					if (logger.isDebugEnabled()) {
						logger.debug("v2 fed message received from sessionId: " + getCurrentSessionId() + " " + fe);
					}

					clientMessageCounter.incrementAndGet();
					clientByteAccumulator.addAndGet(fe.getSerializedSize());

					FederationServer.this.handleRead(fe, getCurrentSessionId(), federateSubscription, channelHandler);
				}

				@Override
				public void onError(Throwable t) {
					if (logger.isDebugEnabled()) {
						logger.debug("serverEventStream onError: " + t);
					}

					String msg = "federate disconnected or connectivity lost";
					
					try {
						serverFederateGroupStreamMap.get(getCurrentSessionId()).onError(t);
					} catch (Exception e) {}

					try {
						serverFederateGroupStreamMap.get(getCurrentSessionId()).onError(t);
					} catch (Exception e) {}

					if (t instanceof io.grpc.StatusException) {
						if (((io.grpc.StatusException) t).getStatus().getCode().equals(Status.CANCELLED.getCode())) {

							logger.info(msg);

							try {
								messagingUtil.processFederateClose(channelHandler.getConnectionInfo(), channelHandler, federatedSubscriptionManager.getFederateSubscription(channelHandler.getConnectionInfo()));
							} catch (Exception e) {
								logger.warn("exception processing federate close", e);
							}
						}
					} else if (t instanceof io.grpc.StatusRuntimeException) {
						if (((io.grpc.StatusRuntimeException) t).getStatus().getCode().equals(Status.CANCELLED.getCode())) {

							logger.info(msg);

							try {
								messagingUtil.processFederateClose(channelHandler.getConnectionInfo(), channelHandler, federatedSubscriptionManager.getFederateSubscription(channelHandler.getConnectionInfo()));
							} catch (Exception e) {
								logger.warn("exception processing federate close", e);
							}
						}
					} else {
						logger.error("exception in server event stream call", t);
					}
				}

				@Override
				public void onCompleted() {
					if (logger.isDebugEnabled()) {
						logger.debug("FederatedEvent stream complete");
					}
				}
			};
		}

		public StreamObserver<FederateGroups> clientFederateGroupsStream(StreamObserver<Subscription> responseObserver) {
			return new StreamObserver<FederateGroups>() {
				@Override
				public void onNext(FederateGroups value) {
					if (logger.isDebugEnabled()) {
						logger.debug("Collecting client federate groups: " + value);
					}
					groupFederationUtil.collectRemoteFederateGroups(new HashSet<String>(value.getFederateGroupsList()),  federationManager.getFederate(serverFederateMap.get(getCurrentSessionId())));
				}

				@Override
				public void onError(Throwable t) {

				}

				@Override
				public void onCompleted() {
					logger.debug(" clientFederateGroups on completed  ");
				}
			};
		}

		@Override
		public void serverFederateGroupsStream(Subscription request, StreamObserver<FederateGroups> responseObserver) {
			serverFederateGroupStreamMap.put(getCurrentSessionId(), responseObserver);
			federatedSubscriptionManager.putServerGroupStreamToSession(getCurrentSessionId(), responseObserver);
			// let the client know the group stream is ready
			responseObserver.onNext(FederateGroups.newBuilder().setStreamUpdate(ServerHealth.newBuilder().setStatus(ServerHealth.ServingStatus.SERVING).build()).build());
			if (logger.isDebugEnabled()) {
				logger.debug("Setting the serverFederateGroupsStreamObserver");
			}
		}

		
		@Override
		public void healthCheck(ClientHealth request, StreamObserver<ServerHealth> responseObserver) {
			
			GuardedStreamHolder<FederatedEvent> fedEventStream = null;

			if (config.isEnableHealthCheck()) {

				String sessionId = new String(((SSLSession) sslSessionKey.get(Context.current())).getId(), Charsets.UTF_8);

				if (clientStreamMap.containsKey(sessionId)) {
					
					fedEventStream = clientStreamMap.get(sessionId);
					
					fedEventStream.updateClientHealth(request);
					responseObserver.onNext(serving);
				} else {
					logger.debug("No session existed for sessionID: {}", sessionId);
					responseObserver.onNext(notConnected);
				}

				if (clientROLStreamMap.containsKey(sessionId)) {
					clientROLStreamMap.get(sessionId).updateClientHealth(request);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("No ROL client session existed for sessionID: {}", sessionId);
					}
				}

			} else {
				logger.warn("not sending federation health check - disabled in config");
			}
			
			// This check ensures that the client stream is TAK Server 4.3 or higher. The reponse observer can only be safely closed in 3.4 or higher, becuase closing it
			// in earlier version of TAK Server will cause the federated client TAK Server to disconnect.
			
			if (fedEventStream != null && fedEventStream.getSubscription()!= null && fedEventStream.getSubscription().getVersion() != null && fedEventStream.getSubscription().getVersion().getMajor() > 0) {
				if (fedHealthLogger.isTraceEnabled()) {
					fedHealthLogger.trace("closing client fed health check stream - fed subscriber version: " + fedEventStream.getSubscription().getVersion());
				}
				
				responseObserver.onCompleted(); 
			}

		}

		private class FederationMissionPackageProcessor implements FederationProcessor<ROL> {

			private final String res;
			private final String op;
			private final ResourceDetails dt;

			FederationMissionPackageProcessor(String res, String op, ResourceDetails dt, String sessionId) {
				this.res = res;
				this.op = op;
				this.dt = dt;
			}

			@Override
			public void process(ROL clientROL) {
				if (!res.toLowerCase(Locale.ENGLISH).equals("package")) {
					logger.warn("ignoring unexpected ROL resource from client: " + res);
					return;
				}

				logger.debug("processing op " + op);

				switch (op.toLowerCase(Locale.ENGLISH)) {
				case "announce":
					logger.info("processing 'announce package' ROL command from client.");
					if (logger.isDebugEnabled()) {
						logger.debug("package details: "  + dt);
					}

					if (Strings.isNullOrEmpty(dt.getSha256())) {
						logger.warn("'announce package' ROL from client contains no resource hash - ignoring.");
						return;
					}

					try {

						ConnectionInfo connection = new ConnectionInfo();
						connection.setConnectionId(getCurrentSessionId());

						FigServerFederateSubscription sub = (FigServerFederateSubscription) federatedSubscriptionManager.getFederateSubscription(connection);

						NavigableSet<Group> groups = null;

						Federate federate = federationManager.getFederate(serverFederateMap.get(getCurrentSessionId()));
						if (federate.isFederatedGroupMapping()) {
							if (sub.getIsAutoMapped()) {
								groups = groupFederationUtil.autoMapGroups(clientROL.getFederateGroupsList());
							} else {
								groups = groupFederationUtil.addFederateGroupMapping(federationManager.getInboundGroupMap(federate.getId()),
										clientROL.getFederateGroupsList());
							}
						}

						// fall back if no matches on the mapping
						if (groups == null || groups.isEmpty()) {
							NavigableSet<Group> allGroups = groupManager.getGroups(sub.getUser());
							groups = groupFederationUtil.filterGroupDirection(Direction.IN, allGroups);
						}

						MissionPackageAnnounce announceCotResult = groupFederationUtil.createPackageAnnounceCot(dt);

						if (logger.isDebugEnabled()) {
							logger.debug("submitting federated mission package announcement: " + announceCotResult + " groups " + groups + " to submission service.");
						}

						submission.submitCot(announceCotResult.getAnnoucement(), announceCotResult.getUids(), announceCotResult.getCallsigns(), groups, false, false);

					} catch (Exception e) {
						logger.warn("exception processing federated mission package announcement.", e);
					}

					break;

				case "disperse":
					if (logger.isDebugEnabled()) {
						logger.info("received ignorable 'disperse package' ROL command from client - not relevant for pair-wise federation.");
					}

					break;
				default:
					logger.warn("unexpected ROL operation received from client: " + op);
				}
			}
		}

		private class FederationMissionProcessor implements FederationProcessor<ROL> {

			FederationMissionProcessor(String sessionId) {
				this.sessionId = sessionId;
			}

			private final String sessionId;

			@Override
			public void process(ROL rol) {

				if (logger.isDebugEnabled()) {
					logger.debug("recieved ROL message from FIG - submitting for processing " + rol.getProgram());
				}

				try {

					GuardedStreamHolder<ROL> rolStreamHolder = FederationServer.this.clientROLStreamMap.get(sessionId);

					if (rolStreamHolder == null) {
						logger.warn("null rolStreamHolder when processing client ROL");
						return;
					}

					ConnectionInfo connection = new ConnectionInfo();
					connection.setConnectionId(getCurrentSessionId());

					FigServerFederateSubscription sub = (FigServerFederateSubscription) federatedSubscriptionManager.getFederateSubscription(connection);
					if (sub == null) {
						logger.warn("null FigServerFederateSubscription when processing incoming federate ROL");
						return;
					}

					NavigableSet<Group> groups = null;

					Federate federate = federationManager.getFederate(serverFederateMap.get(getCurrentSessionId()));
					if (federate.isFederatedGroupMapping()) {
						if (sub.getIsAutoMapped()) {
							groups = groupFederationUtil.autoMapGroups(rol.getFederateGroupsList());
						} else {
							groups = groupFederationUtil.addFederateGroupMapping(federationManager.getInboundGroupMap(federate.getId()),
									rol.getFederateGroupsList());
						}
					}

					// fall back if no matches on the mapping
					if (groups == null || groups.isEmpty()) {
						NavigableSet<Group> allGroups = groupManager.getGroups(sub.getUser());
						groups = groupFederationUtil.filterGroupDirection(Direction.IN, allGroups);
					}

					if (logger.isDebugEnabled()) {
						logger.debug("group set for incoming client ROL: " + groups);
					}

					if (federationROLHandler != null) {
						federationROLHandler.onNewEvent(rol, groups);
					}

				} catch (RemoteException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("exception submitting ROL", e);
					}
				}
			}
		}

		class FederationProcessorFactory {
			FederationProcessor<ROL> newProcessor(String resource, String operation, Parameters parameters, String sessionId) {
				switch (Resource.valueOf(resource.toUpperCase())) {
				case PACKAGE:
					if (!(parameters instanceof ResourceDetails)) {
						throw new IllegalArgumentException("invalid reourcedetails object for mission package processing");
					}

					if (logger.isDebugEnabled()) {
						logger.debug("creating FederationPackageMissionProcessor");
					}

					return new FederationMissionPackageProcessor(resource, operation, (ResourceDetails) parameters, sessionId);

				default:

					if (logger.isDebugEnabled()) {
						logger.debug("creating FederationMissionProcessorProcessor");
					}

					return new FederationMissionProcessor(sessionId);
				}
			}
		}
	}

	private void addCaFederateToPolicyGraph(GuardedStreamHolder<?> streamHolder, Certificate[] caCertArray) {
		List<String> caCertNames = new LinkedList<>();

		// The cert array returned by gRPC's SSLSession is padded with null entries.  This loop adds all certs in the
		// array from index 1 (index 0 is the peer's cert, index 1+ are CA certs) til the first null entry (the
		// start of the padding) to a list of Ca certs.
		for(int i = 1; i < caCertArray.length; i++) {
			if (caCertArray[i] == null) {
				break;
			}
			try {
				String issuerName = ((X509Certificate) caCertArray[i]).getIssuerX500Principal().getName();

				if (logger.isDebugEnabled()) {
					logger.debug("addCaFederateToPolicyGraph issuerName: " + issuerName);
				}

				String groupName = issuerName + "-" + FederationUtils.getBytesSHA256(caCertArray[i].getEncoded());
				caCertNames.add(groupName);
			} catch (CertificateEncodingException e) {
				logger.error("Could not encode certificate", e);
			}
		}

	}

	private void sendCaGroupsToFedManager(KeyStore keyStore) throws KeyStoreException {
		for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements();) {
			String alias = e.nextElement();
			X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
			sendCaGroupToFedManager(cert);
		}
	}

	private void sendCaGroupToFedManager(X509Certificate cert) throws KeyStoreException {
		try {
			String issuerName = cert.getIssuerX500Principal().getName();
			String groupName = issuerName + "-" + FederationUtils.getBytesSHA256(cert.getEncoded());
			FederateGroup group = new FederateGroup(new FederateIdentity(groupName));
		} catch (CertificateEncodingException cee) {
			logger.error("Could not encode certificate", cee);
		}
	}

	public boolean addGroupCa(X509Certificate ca) {
		try {
			String dn = ca.getSubjectX500Principal().getName();
			String alias = getCN(dn);
			sslConfig.getTrust().setEntry(alias, new KeyStore.TrustedCertificateEntry(ca), null);
			saveTruststoreFile();
			sslConfig.refresh();
			sendCaGroupToFedManager(ca);
		} catch (KeyStoreException e) {
			logger.debug("exception adding ca", e);
		}
		return false;
	}

	// attempt to get the CN in a robust way
	private static String getCN(String dn) {
		if (Strings.isNullOrEmpty(dn)) {
			throw new IllegalArgumentException("empty DN");
		}

		try {
			LdapName ldapName = new LdapName(dn);

			for(Rdn rdn : ldapName.getRdns()) {
				if(rdn.getType().equalsIgnoreCase("CN")) {

					return rdn.getValue().toString();
				}
			}

			throw new TakException("No CN found in DN: " + dn);

		} catch (InvalidNameException e) {
			throw new TakException(e);
		}
	}

	private synchronized void saveTruststoreFile() {
		try {
			sslConfig.getTrust().store(new FileOutputStream(config.getTruststoreFile()), config.getTruststorePassword().toCharArray());
			if (logger.isTraceEnabled()) {
				logger.trace("federation truststore file save complete");
			}
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.warn("exception saving truststore file", e);
		}
	}

	// Send a message
	public void send(Message message) {
		// make a decision about which connected clients to write this message back to
		requireNonNull(message, "null message object");

		if (!(message.getPayload().getContent() instanceof FederatedEvent || message.getPayload().getContent() instanceof BinaryBlob || message.getPayload().getContent() instanceof ROL)) {
			throw new IllegalArgumentException("unsupported payload type " + message.getPayload().getClass().getName());
		}

		// decide what to do based on payload type
		if (message.getPayload().getContent() instanceof FederatedEvent) {
			sendFederatedEvent(message);
		} else if (message.getPayload().getContent() instanceof ROL) {
			sendRolMessage(message);
		} else {
			logger.info("not handling send to client of " + message.getPayload().getContent().getClass().getSimpleName() + " yet.");
		}
	}

	private void sendFederatedEvent(Message message) {
		for (AddressableEntity<?> entity : message.getDestinations()) {

			if (entity.getEntity() instanceof FederateIdentity) {

				FederateIdentity src = (FederateIdentity) message.getSource().getPeerId().getEntity();
				FederateIdentity dest = (FederateIdentity) entity.getEntity();

				deliver(message, src, dest);
			}
		}
	}



	private void sendRolMessage(Message message) {
		for (Map.Entry<String, GuardedStreamHolder<ROL>> stream : clientROLStreamMap.entrySet()) {
			
			if (rolLogger.isDebugEnabled()) {
				rolLogger.debug("sending ROL message: " + message);
			}
			
			logger.debug("clientROLStream session: " + stream.getKey());
			logger.debug("clientROLStream name: " + clientROLStreamNames.get(stream.getKey()));
			try {
				stream.getValue().send((ROL) message.getPayload().getContent());
			} catch (Exception e) {
				logger.error("Caught exception", e);
			}
		}
	}

	private void deliver(Message message, FederateIdentity src, FederateIdentity dest) {
		for (Entry<String, GuardedStreamHolder<FederatedEvent>> entry : clientStreamMap.entrySet()) {
			if (entry.getValue().getFederateIdentity().equals(dest)) {

				Message filteredMessage = message;

				FederatedEvent event = requireNonNull((FederatedEvent) filteredMessage.getPayload().getContent(), "federated event message payload");

				if (logger.isTraceEnabled()) {
					logger.trace("sending message from " + src + " to " + dest);
				}

				try {
					entry.getValue().send(event);

					// track message sends for metrics
					clientMessageCounter.incrementAndGet();
					clientByteAccumulator.addAndGet(event.getSerializedSize());
				} catch (Exception ex) {
					logger.error("Exception sending message to client stream", ex);
					clientStreamMap.remove(entry.getKey());
				}
			}
		}
	}

	private void handleRead(FederatedEvent fedEvent, String sessionId, FederateSubscription federateSubscription, ChannelHandler handler) {
		
		if (fedEvent == null) {

			logger.warn("null read in FederationServer handleRead");

			return;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("message received from v2 fed client: " + fedEvent);
		}
		
		// this needs to be checked before fedEvent.getEvent()
		if (fedEvent.getFederateGroupsList() != null) {
			groupFederationUtil.collectRemoteFederateGroups(new HashSet<String>(fedEvent.getFederateGroupsList()), federationManager.getFederate(serverFederateMap.get(getCurrentSessionId())));
		}

		// covert to a CoT message and submit to broker
		if (fedEvent.hasEvent()) {
			
			((AbstractBroadcastingChannelHandler) federateSubscription.getHandler()).getConnectionInfo().getReadCount().getAndIncrement();
			
			try {
				CotEventContainer cot = ProtoBufHelper.getInstance().proto2cot(fedEvent.getEvent());
				
				try {
		        	Metrics.counter(Constants.METRIC_FED_DATA_MESSAGE_READ_COUNT, "takserver", "messaging").increment();
		        } catch (Exception ex) {
		        	if (logger.isDebugEnabled()) {
		        		logger.debug("error recording fed message read metric", ex);
		        	}
		        }

				if (DistributedConfiguration.getInstance().getRemoteConfiguration().getSubmission().isIgnoreStaleMessages()) {
					if (MessageConversionUtil.isStale(cot)) {
						if (logger.isDebugEnabled()) {
							logger.debug("ignoring stale FIG federated message: " + cot);
						}
						return;
					}
				}

				cot.setContext(GroupFederationUtil.FEDERATE_ID_KEY, getCurrentSessionId());
	            cot.setContext(Constants.SOURCE_HASH_KEY, handler.identityHash());
				cot.setContext(Constants.NOFEDV2_KEY, true);
				
				// if this message was from a data feed, pass it through the data feed filter
				// for mission filtering						
				String feedUuid = (String) cot.getContextValue(Constants.DATA_FEED_UUID_KEY);
				if (!Strings.isNullOrEmpty(feedUuid)) {
					if (!DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation())
						return;
					DataFeedFilter.getInstance().filterFederatedDataFeed(cot);
				}

				if (federateSubscription != null && federateSubscription.getUser() != null) {
					User user = federateSubscription.getUser();
					cot.setContext(Constants.USER_KEY, user);
					NavigableSet<Group> groups = null;
					if (federationManager.getFederate(serverFederateMap.get(getCurrentSessionId())).isFederatedGroupMapping()) {
						// update and add the correct parameter
						if (((FigFederateSubscription) federateSubscription).getIsAutoMapped()) {
							groups = groupFederationUtil.autoMapGroups(fedEvent.getFederateGroupsList());
						} else {
							groups = groupFederationUtil.addFederateGroupMapping(federationManager.getInboundGroupMap(user.getId()),
									fedEvent.getFederateGroupsList());
						}
					}
                    // fall back if no matches on the mapping
					if (groups == null || groups.isEmpty()) {
						NavigableSet<Group> allGroups = groupManager.getGroups(federateSubscription.getUser());
						groups = groupFederationUtil.filterGroupDirection(Direction.IN, allGroups);
					}
					if (groups != null) {
						if (logger.isTraceEnabled()) {
							logger.trace("marking groups in FIG federated message: " + groups);
						}

						cot.setContext(Constants.GROUPS_KEY, groups);
					}

					//                            cot.setContextValue(RepositoryService.ARCHIVE_EVENT_KEY, config.isArchive()); // TODO: make archive configurable per FIG federate
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("user not found for federate subscription for FIG message");
					}
				}

				if (cot != null && !cot.getType().toLowerCase(Locale.ENGLISH).startsWith("t-x-m")) {

					// submit message to marti core for processing
					if (logger.isDebugEnabled()) {
						logger.debug("submitting v2 fed client CoT event ", cot);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("ignoring mission change message");
					}
				}

				// ignore federated mission changes
				if (cot != null && !cot.getType().toLowerCase(Locale.ENGLISH).startsWith("t-x-m")) {

					cotMessenger.send(cot);
				}
				federateSubscription.incHit(new Date().getTime());  // for debugging, uncomment this line to count incoming messages (plus the outgoing message count) for FIG federates in "Num Processed" in the UI

				// store for latestSA
				ConcurrentHashMap<String, RemoteContactWithSA> remoteContacts = federatedSubscriptionManager.getRemoteContactsMapByChannelHandler(handler);
				if(remoteContacts != null && remoteContacts.containsKey(cot.getUid())) {
					remoteContacts.get(cot.getUid()).setLastSA(cot);
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception getting federate DN", e);
				}
			}
		}

		if (fedEvent.hasContact() && fedEvent.getContact().getOperation() != CRUD.INVALID) {

			if (logger.isDebugEnabled()) {
				logger.debug("received contact message from FIG federate: " + fedEvent.getContact());
			}
			
			try {
	        	Metrics.counter(Constants.METRIC_FED_CONTACT_MESSAGE_READ_COUNT, "takserver", "messaging").increment();
	        } catch (Exception ex) {
	        	if (logger.isDebugEnabled()) {
	        		logger.debug("error recording fed message read metric", ex);
	        	}
	        }

			try {
				ContactListEntry contact = fedEvent.getContact();
				if(contact.getOperation() == CRUD.DELETE) {
					CotEventContainer e = ProtoBufHelper.getInstance().protoBuf2delContact(contact);
					Collection<com.bbn.marti.service.Subscription> reachable = groupFederationUtil.getReachableSubscriptions(federateSubscription);
					if(reachable.contains(federateSubscription)) {
						logger.warn("reachable contained self!");
						reachable.remove(federateSubscription);
					}
					e.setContext(Constants.NOFEDV2_KEY, true);
					e.setContext(GroupFederationUtil.FEDERATE_ID_KEY, getCurrentSessionId());
					e.setContextValue(Constants.SUBSCRIBER_HITS_KEY, subscriptionStore.subscriptionCollectionToConnectionIdSet(reachable));
					e.setContextValue(Constants.USER_KEY, federateSubscription.getUser());
					cotMessenger.send(e);
				}
				federationManager.addRemoteContact(fedEvent.getContact(), handler);
			} catch (Exception e) {
				logger.warn("exception processing FIG federate contact message", e);
			}
		}
	}

	private void handleRead(BinaryBlob event, byte[] sessionId) {
		// do a read
		// create a message
		Message federatedMessage = new Message(new HashMap<>(), new BinaryBlobPayload(event));
		long startTime = System.currentTimeMillis();
		//        setMessageDestinations(federatedMessage);
		federatedMessage.setMetadataValue(SSL_SESSION_ID, new String(sessionId, Charsets.UTF_8));

		String streamKey = new String(sessionId, Charsets.UTF_8);

		federatedMessage.setMetadataValue(SSL_SESSION_ID, streamKey);

		GuardedStreamHolder<FederatedEvent> streamHolder = clientStreamMap.get(streamKey);

		if (streamHolder != null) {
			federatedMessage.setMetadataValue(FEDERATED_ID_KEY, streamHolder.getFederateIdentity());
			try {
				//                assignMessageSourceAndDestinationsFromPolicy(federatedMessage, streamHolder.getFederateIdentity(), federationManager.getPolicyGraph());
				sendMessage(federatedMessage, startTime);
			} catch (Exception e) {
				logger.error("Exception sending federated message", e);
			}
		}
	}

	private void sendMessage(Message federatedMessage, long startTime) {
		try {
			send(federatedMessage);
		} catch (Exception e) {
			logger.error("exception sending message", e);
		}
		//        }
	}

	final static private Context.Key<SSLSession> sslSessionKey = Context.key("SSLSession");
	final static private Context.Key<Long> sslSessionIdKey = Context.key("SSLSessionId");
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
				
				if (sslSession == null && socketAddress == null) {
					return next.startCall(call, requestHeaders);
				}
				
				Context context = Context.current()
						.withValue(sslSessionKey, sslSession)
						.withValue(sslSessionIdKey, Longs.fromByteArray(sslSession.getId()))
						.withValue(remoteAddressKey, socketAddress);
				
				
				return Contexts.interceptCall(context, call, requestHeaders, next);
			}
		};
	}

	public FigServerConfig loadConfig(String configFilename) throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		if (getClass().getResource(configFilename) != null) {
			// it's a resource
			return new ObjectMapper(new YAMLFactory()).readValue(getClass().getResourceAsStream(configFilename), FigServerConfig.class);
		}

		// it's a file
		return new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(configFilename), FigServerConfig.class);
	}

	private void removeInactiveClientStreams() {
		if (logger.isDebugEnabled()) {
			logger.debug("Running inactivity check for {} client streams", clientStreamMap.size());
		}
		for (Map.Entry<String, GuardedStreamHolder<FederatedEvent>> clientStreamEntry : clientStreamMap.entrySet()) {

			if(!clientStreamEntry.getValue().isClientHealthy(config.getClientTimeoutTime())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Detected FederatedEvent client stream {} inactivity", clientStreamEntry.getValue().getFederateIdentity());
				}
				clientStreamEntry.getValue().throwDeadlineExceptionToClient();

				clientStreamMap.remove(clientStreamEntry.getKey());
			}
		}

		for (Map.Entry<String, GuardedStreamHolder<ROL>> rolStreamEntry : clientROLStreamMap.entrySet()) {

			if(!rolStreamEntry.getValue().isClientHealthy(config.getClientTimeoutTime())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Detected ROL client stream {} inactivity", rolStreamEntry.getValue().getFederateIdentity());
				}

				try {
					rolStreamEntry.getValue();
				} catch (Exception e) {

				}

				rolStreamEntry.getValue().throwDeadlineExceptionToClient();

				clientROLStreamMap.remove(rolStreamEntry.getKey());
			}
		}
	}

	private String getCurrentSessionId() {
		try {		
			return sslSessionIdKey.get(Context.current()).toString();
		} catch (Exception e) {
			throw new TakException(e);
		}
	}
	
	private Certificate getCurrentClientCert() {
		try {

			SSLSession session = sslSessionKey.get(Context.current());
	
			Certificate[] clientCertArray = requireNonNull(requireNonNull(session, "SSL Session").getPeerCertificates(), "SSL peer certs array");

			if (clientCertArray == null) {
				throw new IllegalArgumentException("null v2 fed client cert array");
			}

			if (clientCertArray.length == 0) {
				throw new IllegalArgumentException("emmpty v2 fed client cert array");
			}

			Certificate clientCert = requireNonNull(clientCertArray[0], "v2 fed client cert");

			return clientCert;

		} catch (Exception e) {
			throw new TakException(e);
		}
	}
	
	private SocketAddress getCurrentSocketAddress() {
		try {
			
			return remoteAddressKey.get(Context.current());


		} catch (Exception e) {
			throw new TakException(e);
		}
	}

	private Certificate getCurrentCaCert() {
		try {

			SSLSession session = sslSessionKey.get(Context.current());

			Certificate[] clientCertArray = requireNonNull(requireNonNull(session, "SSL Session").getPeerCertificates(), "SSL peer certs array");

			if (clientCertArray == null) {
				throw new IllegalArgumentException("null v2 fed client cert array");
			}

			if (clientCertArray.length == 0) {
				throw new IllegalArgumentException("emmpty v2 fed client cert array");
			}

			Certificate caCert = requireNonNull(clientCertArray[1], "v2 fed ca cert");

			return caCert;

		} catch (Exception e) {
			throw new TakException(e);
		}
	}
}
