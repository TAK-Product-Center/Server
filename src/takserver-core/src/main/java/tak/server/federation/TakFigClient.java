package tak.server.federation;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.rmi.RemoteException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.NamingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.bbn.marti.config.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.protobuf.ByteString;

import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Metrics;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.CRUD;
import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.ContactListEntry;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedChannelGrpc;
import com.atakmap.Tak.FederatedChannelGrpc.FederatedChannelBlockingStub;
import com.atakmap.Tak.FederatedChannelGrpc.FederatedChannelStub;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.Identity;
import com.atakmap.Tak.ROL;
import com.atakmap.Tak.ROL.Builder;
import com.atakmap.Tak.ServerHealth;
import com.atakmap.Tak.ServerHealth.ServingStatus;
import com.atakmap.Tak.Subscription;
import com.atakmap.Tak.TakServerVersion;
import com.bbn.cot.filter.DataFeedFilter;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.config.Federation.FederationOutgoing;
import com.bbn.marti.config.Input;
import com.bbn.marti.config.Tls;
import com.bbn.marti.groups.CommonGroupDirectedReachability;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.groups.PeriodicUpdateCancellationException;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.ConnectionStatusValue;
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
import com.bbn.marti.service.FederatedSubscriptionManager;
import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SSLConfig;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.federation.FederationROLHandler;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.VersionBean;
import com.bbn.marti.util.concurrent.future.AsyncCallback;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.bbn.roger.fig.FederationUtils;
import com.bbn.roger.fig.FigProtocolNegotiator;
import com.bbn.roger.fig.Propagator;

import mil.af.rl.rol.FederationProcessor;
import mil.af.rl.rol.Resource;
import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import mil.af.rl.rol.value.Parameters;
import mil.af.rl.rol.value.ResourceDetails;

import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.federation.rol.MissionRolVisitor;
import tak.server.messaging.Messenger;

/*
 *
 * Handler for v2 federation
 *
 */
public class TakFigClient implements Serializable {


	private static final Logger fedHealthLogger = LoggerFactory.getLogger("fedhealth");

	private MissionDisruptionManager mdm;

	public TakFigClient(MissionDisruptionManager mdm) {
		this.mdm = mdm;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TakFigClient [clientName=");
		builder.append(fedName);
		builder.append(", clientUid=");
		builder.append(clientUid);
		builder.append(", federateSubscription=");
		builder.append(federateSubscription);
		builder.append(", config=");
		builder.append(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation());
		builder.append(", serverLastHealth=");
		builder.append(serverLastHealth);
		builder.append("]");
		return builder.toString();
	}
	private static final long serialVersionUID = 4598790097877439984L;

	private static final Logger logger = LoggerFactory.getLogger(TakFigClient.class);

	private static final Logger rolLogger = LoggerFactory.getLogger("rol");

	private ManagedChannel channel = null;

	public ManagedChannel getChannel() {
		return channel;
	}

	@SuppressWarnings("unused")
	private FederatedChannelBlockingStub blockingFederatedChannel;
	private FederatedChannelStub asyncFederatedChannel;

	public FederatedChannelStub getAsyncFederatedChannel() {
		return asyncFederatedChannel;
	}

	private String fedName = "";

	public String getClientName() {
		return fedName;
	}

	private String clientUid = "";
	
	private String connectionToken = "";

	private FigFederateSubscription federateSubscription = null;

	private ConnectionInfo connectionInfo;

	private AbstractBroadcastingChannelHandler figDummyChannelHandler;

	private String outgoingName;

	private ConnectionStatus status;

	private DistributedFederationManager fedManager;

	private final AtomicBoolean hasSentGroups = new AtomicBoolean(false);

	@Autowired
	private GroupManager groupManager;

	@Autowired
	private SubmissionInterface submission;

	@Autowired
	private Messenger<CotEventContainer> cotMessenger;

	@Autowired
	private SubscriptionManager subscriptionManager;

	@Autowired
	private GroupFederationUtil groupFederationUtil;

	@Autowired
	private MessagingUtilImpl messagingUtil;

	@Autowired
	private FederatedSubscriptionManager federatedSubscriptionManager;

	@Autowired
	private SubscriptionStore subscriptionStore;

	@Autowired
	private FederationROLHandler federationROLHandler;

	@Autowired
	private VersionBean versionBean;

	private GuardedStreamHolder<ROL> rolHolder = null;

	// track last server health check message
	private final AtomicReference<DateTime> serverLastHealth = new AtomicReference<>(null);

	private final AtomicBoolean running = new AtomicBoolean(true);

	private String federateId;

	private int federateMaxHops;

	private ClientCall<ROL, Subscription> rolCall;


	public GuardedStreamHolder<ROL> getRolCall() {
		return rolHolder;
	}

	public void start(FederationOutgoing outgoing, ConnectionStatus status) {
		requireNonNull(outgoing, "FederationOutgoing");
		requireNonNull(status, "ConnectionStatus");

		this.outgoingName = outgoing.getDisplayName();
		this.status = status;
		this.connectionToken = outgoing.getConnectionToken();
		
		Tls figTls = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getTls();

		// use the client name from the outgoing configuration
		fedName = outgoing.getDisplayName();

		clientUid = UUID.randomUUID().toString().replace("-", "");

		logger.info("client name: " + fedName);

		TrustManagerFactory trustMgrFactory = null;

		try {

			String keyManager = figTls.getKeymanager();

			if (Strings.isNullOrEmpty(keyManager)) {
				throw new IllegalArgumentException("empty key manager configuration");
			}

			KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance(keyManager);


			String keystoreType = figTls.getKeystore();

			if (Strings.isNullOrEmpty(keystoreType)) {
				throw new IllegalArgumentException("empty keystore type");
			}

			// Keystore type (e.g., "JKS")
			KeyStore self = KeyStore.getInstance(keystoreType);

			String keystoreFile = figTls.getKeystoreFile();

			if (Strings.isNullOrEmpty(keystoreFile)) {
				throw new IllegalArgumentException("keystore file name empty");
			}

			String keystorePassword = figTls.getKeystorePass();

			if (Strings.isNullOrEmpty(keystorePassword)) {
				throw new IllegalArgumentException("empty keystore password");
			}

			try(FileInputStream fis = new FileInputStream(keystoreFile)) {
				// Filename of the keystore file
				self.load(fis, keystorePassword.toCharArray());
			}
			// Password of the keystore file
			keyMgrFactory.init(self, figTls.getKeystorePass().toCharArray());

			// Trust Manager Factory type (e.g., ??)
			trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

			SSLConfig.initTrust(figTls, trustMgrFactory);
			try {

				if (logger.isDebugEnabled()) {
					logger.debug("Trust managers: " + ((trustMgrFactory == null) ? "Trust All" : trustMgrFactory.getTrustManagers().length));
				}

			} catch (Exception e) {
				logger.warn("exception initializing trust store", e);
			}
			
			SslContextBuilder sslContextBuilder;
			// don't use a key manager if we are using token auth
			if (!Strings.isNullOrEmpty(connectionToken)) {
				sslContextBuilder = GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)  // this ensures that we are using OpenSSL, not JRE SSL
						.trustManager(trustMgrFactory);
			} else {
				sslContextBuilder = GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)  // this ensures that we are using OpenSSL, not JRE SSL
						.keyManager(keyMgrFactory)
						.trustManager(trustMgrFactory);
			}

			String context = "TLSv1.2,TLSv1.3";

			String ciphers = figTls.getCiphers();
			if (!Strings.isNullOrEmpty(ciphers)) {
				sslContextBuilder = sslContextBuilder.ciphers(Arrays.asList(ciphers.split(",")));
				// only set context from config if cipher is also present
				if (!Strings.isNullOrEmpty(figTls.getContext())) {
					context = figTls.getContext();
				}
			}

			channel = openFigConnection(outgoing.getAddress(), outgoing.getPort(),
					sslContextBuilder.protocols(Arrays.asList(context.split(","))).build());

		} catch (Exception e) {
			logger.error("exception setting up TLS config", e);
		}

		blockingFederatedChannel = FederatedChannelGrpc.newBlockingStub(channel);
				
		if (!Strings.isNullOrEmpty(connectionToken)) {
			TokenAuthCredential credential = new TokenAuthCredential(connectionToken);
			asyncFederatedChannel = FederatedChannelGrpc.newStub(channel).withCallCredentials(credential);
		} else {
			asyncFederatedChannel = FederatedChannelGrpc.newStub(channel);
		}

		connectionInfo = new ConnectionInfo();

		connectionInfo.setAddress(outgoing.getAddress());
		connectionInfo.setConnectionId(clientUid);
		connectionInfo.setPort(outgoing.getPort());
		connectionInfo.setTls(true);
		connectionInfo.setClient(true);
		connectionInfo.setHandler(this);

		final ConnectionInfo ciFinal = connectionInfo;

		status.setConnection(connectionInfo);
		// Empty ChannelHandler placeholder - to represent this FIG client in places that are expecting an AbstractBroadcastingChannelHandler
		figDummyChannelHandler = new AbstractBroadcastingChannelHandler() {

			{
				withHandlerType("TakFigClient");
				this.connectionInfo = ciFinal; // In certain cases, like when handling point to point messages, this AbstractBroadcastingChannelHandler will be used to
				// get the connection info
			}

			private final String notImplementedMessage = "No-op FIG ChannelHandler";

			@Override
			public boolean handleRead(SelectableChannel channel, Server server, ByteBuffer buff) {
				return false;
			}

			@Override
			public boolean handleWrite(SelectableChannel channel, Server server, ByteBuffer buff) {
				return false;
			}

			@Override
			public boolean handleConnect(SelectableChannel channel, Server server) {
				return false;
			}

			@Override
			public boolean handleAccept(SelectableChannel channel, Server server) {
				return false;
			}

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

				try {
					TakFigClient.this.shutdown();
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("exception closing federation client", e);
					}
				}

				return new AsyncFuture<ChannelHandler>() {

					@Override
					public Outcome getStatus() {
						return null;
					}

					@Override
					public ChannelHandler getResult() {
						return null;
					}

					@Override
					public Exception getException() {
						return null;
					}

					@Override
					public void addJob(Runnable runnable) {

					}

					@Override
					public void addJob(Runnable runnable, Executor executor) { }

					@Override
					public void addCallback(AsyncCallback<ChannelHandler> callback) { }

					@Override
					public void addCallback(AsyncCallback<ChannelHandler> callback, Executor executor) { }
				};
			}

			@Override
			public void forceClose() {
				try {
					TakFigClient.this.shutdown();
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("exception closing federation client", e);
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
				return "FIG channel handler " + connectionInfo;
			}
		};

		// connect to server, and start receiving messages.
		init();
		SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
	}

	private NavigableSet<Group> getGroupsForActiveSubscription() throws RemoteException {
		Objects.requireNonNull(federateSubscription);
		Objects.requireNonNull(federateSubscription.getUser());
        // this returns a filtered list, else empty list
		NavigableSet<Group> groups = groupManager.getGroups(federateSubscription.getUser());
		return groupFederationUtil.filterGroupDirection(Direction.IN, groups);
	}

	@SuppressWarnings("deprecation")
	private void init() {
		FederationOutgoing outgoing = fedManager().getOutgoingConnection(outgoingName);

		// setup group stream
		serverFederateGroups();

		if (logger.isDebugEnabled()) {
			logger.debug("init TakFigClient");
		}


		// Send a subscription request, get back a stream of messages from server
		asyncFederatedChannel.clientEventStream(Subscription.newBuilder()
				.setFilter(Strings.isNullOrEmpty(outgoing.getFilter()) ? "" : outgoing.getFilter())
				.setIdentity(Identity.newBuilder()
						.setName(fedName)
						.setUid(clientUid)
						.setServerId(CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getServerId())
						.setType(Identity.ConnectionType.FEDERATION_TAK_CLIENT)
						.build()).setVersion(TakServerVersion.newBuilder() // TAK Server 4.3 and higher declares the federation version here. The server uses this information to tailor the federation interaction.
								.setMajor(versionBean.getVersionInfo().getMajor())
								.setMinor(versionBean.getVersionInfo().getMinor())
								.setPatch(versionBean.getVersionInfo().getPatch())
								.setBranch(versionBean.getVersionInfo().getBranch())
								.setVariant(versionBean.getVersionInfo().getVariant())
								.build())
							.build(),
							new StreamObserver<FederatedEvent>() {

			/*
			 * This anonymous inner class receives events from the FIG server.
			 */
			AtomicReference<Long> start = new AtomicReference<>();

			AtomicLong serverMessageCounter = new AtomicLong(); // Count all the messages from the server

			@Override
			public void onNext(FederatedEvent fedEvent) {
				connectionInfo.getReadCount().getAndIncrement();

				start.compareAndSet(null, System.currentTimeMillis());

				if (fedEvent == null) {
					throw new IllegalArgumentException("null federated event");
				}

				if (logger.isDebugEnabled()) {
					logger.debug("message received from federated server: " + fedEvent);
				}

				serverMessageCounter.incrementAndGet();

				// this needs to be checked before fedEvent.getEvent()
				if (fedEvent.getFederateGroupsList() != null) {
					groupFederationUtil.collectRemoteFederateGroups(new HashSet<String>(fedEvent.getFederateGroupsList()), getFederate());
				}

				if (fedEvent.hasEvent()) {
					try {
						CotEventContainer cot = ProtoBufHelper.getInstance().proto2cot(fedEvent.getEvent());

						try {
				        	Metrics.counter(Constants.METRIC_FED_DATA_MESSAGE_READ_COUNT, "takserver", "messaging").increment();
				        } catch (Exception ex) {
				        	if (logger.isDebugEnabled()) {
				        		logger.debug("error recording fed message read metric", ex);
				        	}
				        }

						if (CoreConfigFacade.getInstance().getRemoteConfiguration().getSubmission().isIgnoreStaleMessages()) {
							if (MessageConversionUtil.isStale(cot)) {
								if (logger.isDebugEnabled()) {
									logger.debug("ignoring stale federated message: " + cot);
								}
								return;
							}
						}

						cot.setContext(GroupFederationUtil.FEDERATE_ID_KEY, clientUid);
			            cot.setContext(Constants.SOURCE_HASH_KEY, figDummyChannelHandler.identityHash());
						cot.setContext(Constants.NOFEDV2_KEY, true);

						cot.setContext(Constants.REMOTE_FEDERATE_SOURCE_GROUPS_KEY, (List<String>) fedEvent.getFederateGroupsList());

						// if this message was from a data feed, pass it through the data feed filter
						// for mission filtering
						String feedUuid = (String) cot.getContextValue(Constants.DATA_FEED_UUID_KEY);
						if (!Strings.isNullOrEmpty(feedUuid)) {
							if (!CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation())
								return;
							DataFeedFilter.getInstance().filterFederatedDataFeed(cot);
						}

						if (federateSubscription != null && federateSubscription.getUser() != null) {
							User user = federateSubscription.getUser();
							cot.setContext(Constants.USER_KEY, user);

							NavigableSet<Group> groups = null;

							if (getFederate().isFederatedGroupMapping()) {
								if (federateSubscription.getIsAutoMapped()) {
									groups = groupFederationUtil.autoMapGroups(fedEvent.getFederateGroupsList());
								} else {
									groups = groupFederationUtil.addFederateGroupMapping(fedManager.getInboundGroupMap(user.getId()),
											fedEvent.getFederateGroupsList());
								}

								if ((groups == null || groups.isEmpty()) && getFederate().isFallbackWhenNoGroupMappings()) {
									groups = getGroupsForActiveSubscription();
								}
							} else  {
								groups = getGroupsForActiveSubscription();
							}

							if (groups != null) {
								if (logger.isTraceEnabled()) {
									logger.trace("marking groups in FIG federated message: " + groups);
								}
								cot.setContext(Constants.GROUPS_KEY, groups);
							}

							if (!getFederate().isArchive()) {
								cot.setContextValue(Constants.ARCHIVE_EVENT_KEY, Boolean.FALSE);
							}

						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("user not found for federate subscription for federated message");
							}
						}

						if (cot != null && !cot.getType().toLowerCase(Locale.ENGLISH).startsWith("t-x-m")) {

							// submit message to marti core for processing
							if (logger.isDebugEnabled()) {
								logger.debug("submitting federated message CoT event ", cot);
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
						ConcurrentHashMap<String, RemoteContactWithSA> remoteContacts = federatedSubscriptionManager.getRemoteContactsMapByChannelHandler(figDummyChannelHandler);
						if (remoteContacts != null && remoteContacts.containsKey(cot.getUid())) {
							remoteContacts.get(cot.getUid()).setLastSA(cot);
						}
					} catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug("exception getting federate DN", e);
						}
						logger.info("exception getting federate DN", e);
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
						if (contact.getOperation() == CRUD.DELETE) {
							CotEventContainer e = ProtoBufHelper.getInstance().protoBuf2delContact(contact);
							Collection<com.bbn.marti.service.Subscription> reachable = groupFederationUtil.getReachableSubscriptions(federateSubscription);
							if (reachable.contains(federateSubscription)) {
								logger.warn("reachable contained self!");
								reachable.remove(federateSubscription);
							}
							e.setContext(Constants.NOFEDV2_KEY, true);
							e.setContext(GroupFederationUtil.FEDERATE_ID_KEY, clientUid);
							e.setContextValue(Constants.SUBSCRIBER_HITS_KEY, subscriptionStore.subscriptionCollectionToConnectionIdSet(reachable));
							e.setContextValue(Constants.USER_KEY, federateSubscription.getUser());
							cotMessenger.send(e);
						}
						fedManager().addRemoteContact(fedEvent.getContact(), figDummyChannelHandler);
					} catch (Exception e) {
						logger.warn("exception processing FIG federate contact message", e);
					}
				}
			}

			@Override
			public void onError(Throwable t) {
				String rootCauseMsg = FederationUtils.getHumanReadableErrorMsg(t);
				if (logger.isDebugEnabled()) {
					logger.debug("received error notification from server" + " cause " + rootCauseMsg, t);

					if (t instanceof io.grpc.StatusRuntimeException) {
						io.grpc.StatusRuntimeException tg = (io.grpc.StatusRuntimeException) t;
						Status s = tg.getStatus();
						if (logger.isDebugEnabled()) {
							logger.debug("status: " + s + " " + s.getDescription());
						}
					}
				}
				status.setLastError(rootCauseMsg);
				SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
				processDisconnect(t);
			}

			@Override
			public void onCompleted() {

				start.compareAndSet(null, System.currentTimeMillis());

				long elapsed = System.currentTimeMillis() - start.get();
				double exectime = elapsed / 1000D;
				double mps = serverMessageCounter.get() / exectime;

				logger.info("received server onCompleted. Message count from server: " + serverMessageCounter.get() + " messages received in " + exectime + " seconds " + " - " + mps + " messages per second");
			}


		});

		final AtomicBoolean initROLStream = new AtomicBoolean(false);

		// send health check messages according to the configuration
		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getHealthCheckIntervalSeconds() > 0) {
			Resources.repeaterPool.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {

					if (!running.get()) {
						throw new PeriodicUpdateCancellationException("cancelling updates - not running");
					}

					ClientHealth clientHealth = ClientHealth.newBuilder().setStatus(ClientHealth.ServingStatus.SERVING).build();

					// set the server health to a timestamp only when we are sending a health check message
					serverLastHealth.compareAndSet(null, new DateTime());
					asyncFederatedChannel.healthCheck(clientHealth, new StreamObserver<ServerHealth>() {
						@Override
						public void onNext(ServerHealth value) {
							if (logger.isDebugEnabled()) {
								logger.debug("received federated health check message from server " + value);
							}

							if (value.getStatus().equals(ServerHealth.ServingStatus.SERVING)) {
								if (logger.isDebugEnabled()) {
									logger.debug("federated server server healthy");
								}
								serverLastHealth.set(new DateTime());
								federateSubscription.setLastProcTime(new Date().getTime());
							} else {
								processDisconnect(new RuntimeException("Server disconnected from our connection"));
								throw new PeriodicUpdateCancellationException("cancelling updates - not running");
							}

							if (initROLStream.compareAndSet(false, true)) {

								// open the client ROL stream only after getting a health check back. This will trigger transmission of federated mission changes.
								// Subscription / Stream to receive ROL messages from server
								asyncFederatedChannel.clientROLStream(Subscription.newBuilder()
										.setFilter(Strings.isNullOrEmpty(outgoing.getFilter()) ? "" : outgoing.getFilter())
										.setIdentity(Identity.newBuilder()
												.setName(fedName)
												.setServerId(MessagingDependencyInjectionProxy.getInstance().serverInfo().getServerId())
												.setType(Identity.ConnectionType.FEDERATION_TAK_CLIENT)
												.setUid(clientUid)
												.build()).build(), new StreamObserver<ROL>() {

									@Override
									public void onNext(final ROL rol) {
										if (rol == null) {
											if (logger.isDebugEnabled()) {
												logger.debug("skipping null ROL message");
											}
											return;
										}

										if (rolLogger.isDebugEnabled()) {
											rolLogger.debug("received ROL from server: " + rol.getProgram());
										}

										// interpret and execute the ROL program
										RolLexer lexer = new RolLexer(new ANTLRInputStream(rol.getProgram()));

										CommonTokenStream tokens = new CommonTokenStream(lexer);

										RolParser parser = new RolParser(tokens);
										parser.setErrorHandler(new BailErrorStrategy());

										// parse the ROL program
										ParseTree rolParseTree = parser.program();

										requireNonNull(rolParseTree, "parsed ROL program");

										final AtomicReference<String> res = new AtomicReference<>();
										final AtomicReference<String> op = new AtomicReference<>();
										final AtomicReference<Parameters> parameters = new AtomicReference<>();

										new MissionRolVisitor(new ResourceOperationParameterEvaluator<Parameters, String>() {
											@Override
											public String evaluate(String resource, String operation, Parameters params) {
												if (logger.isDebugEnabled()) {
													logger.debug(" evaluating " + operation + " on " + resource + " given " + params);
												}

												res.set(resource);
												op.set(operation);
												parameters.set(params);

												return resource;
											}
										}).visit(rolParseTree);

										try {
											new FederationProcessorFactory().newProcessor(res.get(), op.get(), parameters.get(), rolHolder).process(rol);
										} catch (Exception e) {
											logger.warn("exception in core processing incoming ROL", e);
										}
									}

									@Override
									public void onError(Throwable t) {
										if (logger.isDebugEnabled()) {
											logger.debug("error processing ROL event from server" + " cause " + t.getMessage());
										}

										processDisconnect(t);
									}

									@Override
									public void onCompleted() {
										logger.info("received server onCompleted");
									}
								});
								if (logger.isDebugEnabled()) {
									logger.debug("opened client ROL stream");
								}

								if (federateId == null) {
									if (logger.isDebugEnabled()) {
										logger.debug("can't send mission changes - federate id not set");
										return;
									}
								}

								Federate federate = fedManager().getFederate(federateId);

								if (federate == null) {
									if (logger.isDebugEnabled()) {
										logger.debug("can't send mission changes - federate " + federateId + " not found");
										return;
									}
								}

								if (Strings.isNullOrEmpty(federate.getId())) {
									if (logger.isDebugEnabled()) {
										logger.debug("can't send federation changes - empty federate id");
										return;
									}
								}


								Set<String> outGroups = null;
								if (federate.isFederatedGroupMapping()) {
									NavigableSet<Group> groups = groupManager.getGroups(federateSubscription.getUser());
									outGroups = GroupFederationUtil.getInstance().filterFedOutboundGroups(federate.getOutboundGroup(), groups, federate.getId());
								}

								final Set<String> fOutGroups = outGroups;

								try {
									// send out data feeds to federate
									if (CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
										List<ROL> feedMessages = mdm.getDataFeedEventsForFederatedDataFeedOnly(federate);

										AtomicLong delayMs = new AtomicLong(100L);

										for (final ROL feedMessage : feedMessages) {
											Resources.scheduledClusterStateExecutor.schedule(() -> {
												try {
													ROL modifiedFeedMessage = feedMessage;
													if (federate.isFederatedGroupMapping() && fOutGroups != null && fOutGroups.size() > 0) {
														ROL.Builder changeBuilder = modifiedFeedMessage.toBuilder();
														changeBuilder.addAllFederateGroups(fOutGroups);
														modifiedFeedMessage = changeBuilder.build();
													}
													rolHolder.send(modifiedFeedMessage);
												} catch (Exception e) {
													logger.error("exception federating data feed", e);
												}
											}, delayMs.getAndAdd(100), TimeUnit.MILLISECONDS);
										}
									}
								} catch (Exception e) {
									logger.error("exception federating data feeds", e);
								}

								if (CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isEnableMissionFederationDisruptionTolerance()) {

									try {


										if (logger.isDebugEnabled()) {
											logger.debug("mission federation disruption tolerance enabled");
										}

										if (rolHolder == null) {
											if (logger.isDebugEnabled()) {
												logger.debug("can't send mission changes - rolCall not open");
												return;
											}
										}

										List<ROL> missionChanges = mdm.getMissionChangesAndTrackConnectEvent(federate, federate.getName(), federateSubscription);

										if (logger.isTraceEnabled()) {
											logger.trace("mission changes to send: " + missionChanges);
										}

										final AtomicInteger changeCount = new AtomicInteger(0);

										final List<ROL> changeMessages = new CopyOnWriteArrayList<>();

										missionChanges.forEach((change) -> {

											if (federate.isFederatedGroupMapping() && fOutGroups != null && fOutGroups.size() > 0) {
												ROL.Builder changeBuilder = change.toBuilder();
												changeBuilder.addAllFederateGroups(fOutGroups);
												change = changeBuilder.build();
											}

											changeMessages.add(change);
											changeCount.incrementAndGet();
										});

										logger.info(changeCount.get() + " federating " + changeMessages.size() + " mission changes.");

										AtomicLong delayMs = new AtomicLong(0L);

										// stagger sending mission changes
										for (final ROL fedChange : changeMessages) {
											Resources.scheduledClusterStateExecutor.schedule(() -> {
												try {
													rolHolder.send(fedChange);
												} catch (Exception e) {
													logger.error("exception federating mission disruption change", e);
												}
											}, delayMs.getAndAdd(100), TimeUnit.MILLISECONDS);
										}


									} catch (Exception e) {
										logger.warn("exception federating mission disruption changes", e);
									}
								}
							}
						};

						@Override
						public void onError(Throwable t) {
							status.setLastError(FederationUtils.getHumanReadableErrorMsg(t));
							SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
							processDisconnect(t);
							throw new PeriodicUpdateCancellationException("cancelling updates - not running");
						}

						@Override
						public void onCompleted() {
							if (fedHealthLogger.isTraceEnabled()) {
								fedHealthLogger.trace("client health check stream closed by server");
							}

						}
					});
				}
			}, CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getHealthCheckIntervalSeconds(),
					CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getHealthCheckIntervalSeconds(), TimeUnit.SECONDS);

			// send health check messages according to the configuration
			Resources.repeaterPool.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {

					if (!running.get()) {
						throw new PeriodicUpdateCancellationException("cancelling updates - not running");
					}
				}
			}, CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getHealthCheckIntervalSeconds(),
					CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getHealthCheckIntervalSeconds(), TimeUnit.SECONDS);
		} else {
			logger.warn("FIG health check disabled in config (health check interval seconds < 1)");
		}

		// open a channel to the FIG server for the purpose of sending ROL messages
		rolCall = channel.newCall(io.grpc.MethodDescriptor.create(
				io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING,
				generateFullMethodName("com.atakmap.FederatedChannel", "ServerROLStream"),
				io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.ROL.getDefaultInstance()),
				io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.Subscription.getDefaultInstance())), asyncFederatedChannel.getCallOptions());

		rolCall.start(new ClientCall.Listener<Subscription>() {

			@Override
			public void onMessage(Subscription response) {
				// Notify gRPC to receive one additional response.
				rolCall.request(1);
			}

			@Override
			public void onReady() {
				if (logger.isDebugEnabled()) {
					logger.debug("ROL channel ready");
				}
			}
		}, new Metadata());

		// Notify gRPC to receive one response. Without this line, onMessage() would never be called.
		rolCall.request(1);

		rolHolder = new GuardedStreamHolder<ROL>(rolCall, getClientName(),
				new Comparator<ROL>() {
					@Override
					public int compare(ROL a, ROL b) {
						return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
					}
				}, false);

		rolHolder.setMaxFederateHops(getFederate().getMaxHops());

		if (logger.isDebugEnabled()) {
			logger.debug("TakFigClient ROL call " + rolCall + " started");
		}
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);

	}

	/*
	 * Open a connection to the server, and set up the callback that will look for the right federate, and set up the subscription, once the connection is complete and we have the server's client cert.
	 */
	private ManagedChannel openFigConnection(final String host, final int port, SslContext sslContext) throws IOException {
		FederationOutgoing outgoing = fedManager().getOutgoingConnection(outgoingName);
		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();
		return NettyChannelBuilder.forAddress(host, port)
				.negotiationType(NegotiationType.TLS)
				.sslContext(sslContext)
				.maxInboundMessageSize(outgoing.getMaxFrameSize())
				.executor(Resources.federationGrpcExecutor)
				.eventLoopGroup(Resources.federationGrpcWorkerEventLoopGroup)
				.channelType(NioSocketChannel.class)
				.protocolNegotiator(new FigProtocolNegotiator(new Propagator<X509Certificate[]>() {
					@Override
					public X509Certificate[] propogate(X509Certificate[] certChain) {

						X509Certificate figServerClientCert = certChain[0];
						X509Certificate caCert = certChain[1];

						if (logger.isDebugEnabled()) {
							logger.debug("Received server client cert: " + figServerClientCert);
							logger.debug("Received server ca cert: " + caCert);
						}
						connectionInfo.setCert(figServerClientCert);
						try {
							if (logger.isDebugEnabled()) {
								logger.debug("Federate connection client cert: " + connectionInfo.getCert() + " connectionInfo " + connectionInfo);
							}
							X509Certificate cert = figServerClientCert;//connectionInfo.getCert();
							String principalDN = cert.getSubjectX500Principal().getName();
							String issuerDN = cert.getIssuerX500Principal().getName();
							String fingerprint = RemoteUtil.getInstance().getCertSHA256Fingerprint(cert); // Get the cert fingerprint
							// this will throw an exception if the principal or issuer dn can't be obtained
							String certName = MessageConversionUtil.getCN(principalDN) + ":" + MessageConversionUtil.getCN(issuerDN);

							AtomicBoolean duplicateActiveConnection = new AtomicBoolean(false);
							SubscriptionStore.getInstanceFederatedSubscriptionManager()
								.getFederateSubscriptions()
								.forEach(federateSubscription ->{
									if (federateSubscription.getUser() instanceof FederateUser) {
										FederateUser fedUser = (FederateUser) federateSubscription.getUser();
										// there is an active connection from the same cert, mark this connection as duplicate
										if (fedUser.getFederateConfig().getId().equals(fingerprint)) {
											duplicateActiveConnection.set(true);
										}
									}
								});

							if (duplicateActiveConnection.get()) {
								channel.shutdown();
								DistributedFederationManager.getInstance().disableOutgoing(outgoing);
								processDisconnect(new TakException("duplicate federation connection"));
								return null;
							}

							// Look for a configured federate with this fingerprint.
							List<ConnectionStatus> dupeFederates = new ArrayList<>();

							String dupeMsg = "";

							// serialize federate config get/set operations
							Federate federate = fedManager().getFederate(fingerprint);
							federateId = fingerprint;

							String caFingerprint = Optional.ofNullable(caCert).map(ca->RemoteUtil.getInstance().getCertSHA256Fingerprint(ca)).orElse("");

							boolean matchingCA = GroupFederationUtil.getInstance().isRemoteCASelfCA(caCert);

							if (federate == null) {

								if (logger.isDebugEnabled()) {
									logger.debug("CoreConfig federate not found for fingerprint / id: " + fingerprint);
								}

								// put an empty federate with the name and id in the in-memory CoreConfig. Don't save the CoreConfig.xml yet, let that be up to the front-end.
								federate = new Federate();
								federate.setId(fingerprint);
								federate.setName(certName);

								for (Federation.FederateCA ca : config.getFederation().getFederateCA()) {
			                        if (ca.getFingerprint().compareTo(caFingerprint) == 0) {
			                            for (String groupname : ca.getInboundGroup()) {
			                                federate.getInboundGroup().add(groupname);
			                            }
			                            for (String groupname : ca.getOutboundGroup()) {
			                                federate.getOutboundGroup().add(groupname);
			                            }
										// set the new federate max hops based on pre-configured CA settings
			                            federate.setMaxHops(ca.getMaxHops());
			                            break;
			                        }
			                    }

								fedManager().addFederateToConfig(federate);

								if (logger.isDebugEnabled()) {
									logger.debug("federate added to config for id / fingerprint " + fingerprint);
								}
							} else {

								if (logger.isDebugEnabled()) {
									logger.debug("matched existing federate by fingerprint: " + fingerprint + " " + federate.getName());
								}

								// if we matched a federate, there may be an existing connection for it.

								for (ConnectionStatus status : fedManager().getActiveConnectionInfo()) {
									if (status.getFederate() != null && status.getFederate().equals(federate)) {
										dupeFederates.add(status);

										dupeMsg = "Disallowing duplicate federate connection for federate " + federate.getName() + " " + federate.getId() + " " + new SecureRandom().nextInt();
									}
								}
							}

							federateMaxHops = federate.getMaxHops();

							try {

								// match this federate with an outgoing connection.

								List<FederationOutgoing> outgoings = fedManager().getOutgoingConnections(host, port);

								if (outgoings.isEmpty()) {
									throw new TakException("no matching outgoing connection found");
								}

								if (!dupeFederates.isEmpty()) {
									if (!config.getFederation().isAllowDuplicate()) {
										for (FederationOutgoing outgoing : outgoings) {
											fedManager().disableOutgoing(outgoing);
										}

										TakFigClient.this.shutdown();

										throw new DuplicateFederateException(dupeMsg + " " + outgoings.size() + " duplicate outgoing connections found");
									} else {
										logger.warn("allowing duplicate federate connection " + federate.getName() + " " + federate.getId());
									}
								}

								status.setConnection(connectionInfo);

								// put a reference to the matched federate in the connection status
								status.setFederate(federate);
								status.setConnectionStatusValue(ConnectionStatusValue.CONNECTED);
								status.setLastError(""); //reset the last error msg
								SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
								// TODO: Could put logic here for "zombie" federates

							} catch (DuplicateFederateException e) {
								throw e; // only let this type of exception propagate
							} catch (Exception e) {
								logger.warn("exception setting outgoing connection status " + e.getMessage() , e);
							}

							FederateUser user = new FederateUser(fingerprint, connectionInfo.getConnectionId(), fedName, connectionInfo.getAddress(), cert, new X509Certificate[0], federate);

							groupManager.addUser(user);

							// setup federate groups. For a new federate config object, these lists will be empty.
							for (String groupName : federate.getInboundGroup()) {
								groupManager.addUserToGroup(user, new Group(groupName, Direction.IN));
							}

							for (String groupName : federate.getOutboundGroup()) {
								groupManager.addUserToGroup(user, new Group(groupName, Direction.OUT));
							}

							for (String groupName : fedManager.getInboundGroupMap(federateId).values()) {
								groupManager.hydrateGroup(new Group(groupName, Direction.IN));
							}

							try {

								// always use the default reachability
								Reachability<User> reachability = new CommonGroupDirectedReachability(groupManager);

								// Create the subscription for the FIG federate, including a reference back to this TakFigClient, so that messages can be delivered
								federateSubscription = DistributedFederationManager.getInstance().addFigFederateSubscription("FIGFed_" + fedName + "_" + connectionInfo.getConnectionId(), null, null, null, null, user.getFederateConfig().isShareAlerts(), connectionInfo, TakFigClient.this);

								logger.info("created v2 federate subscription " + fedName);

								// set user on subscription, so that message brokering will be able to find the user
								federateSubscription.setUser(user);
								federateSubscription.callsign = fedName;
								federateSubscription.setReachability(reachability);
								federateSubscription.setHandler(figDummyChannelHandler);
								federateSubscription.setIsAutoMapped((matchingCA && federate.isFederatedGroupMapping() && federate.isAutomaticGroupMapping()));
								// track this subscription generally
								subscriptionManager.addRawSubscription(federateSubscription);
								DistributedFederationManager.getInstance().updateFederationSubscriptionCache(connectionInfo, user.getFederateConfig());

							} catch (Exception e) {
								logger.info("exception setting up v2 federate groups " + e.getMessage(), e);
							}
						} catch (DuplicateFederateException e) {
							// let this propagate
							throw e;
						} catch (Exception e) {
							logger.warn("exception creating federate user: " + e.getMessage(), e);
						}

						return certChain;
					}

				}).figTlsProtocolNegotiator(sslContext, FederationUtils.authorityFromHostAndPort(host, port)))
				.build();
		}

	public String getClientUid() {
		return clientUid;
	}

	public Federate getFederate() {
		return fedManager.getFederate(federateId);
	}

	public FederateSubscription getFederateSubscription() {
		return federateSubscription;
	}

	public void setFederateSubscription(FigFederateSubscription federateSubscription) {
		this.federateSubscription = federateSubscription;
	}

	public void processDisconnect(Throwable cause) {
		FederationOutgoing outgoing = fedManager().getOutgoingConnection(outgoingName);
        String rootCauseMsg = FederationUtils.getHumanReadableErrorMsg(cause);

		if (logger.isDebugEnabled()) {
			logger.debug("processing disconnect for FIG federate " + this + " cause " + rootCauseMsg);
		}
		if (federateSubscription != null) {
			federateSubscription.closeClientCall(cause);
			federateSubscription.closeGroupStream(cause);
		}
		running.set(false);

		try {
			channel.shutdownNow();
		} catch (Exception e) {
			logger.warn("error terminating federated channel", e);
		}

		messagingUtil.processFederateClose(connectionInfo, figDummyChannelHandler, SubscriptionStore.getInstance().getSubscriptionByConnectionInfo(connectionInfo));

		// Will be null if not connected yet
		if (federateSubscription != null) {
			subscriptionManager.deleteSubscription(federateSubscription.uid);
			groupManager.removeUser(federateSubscription.getUser());
		}

		boolean shouldReconnect = outgoing.getReconnectInterval() > 0 && outgoing.isEnabled();
		if (logger.isDebugEnabled()) {
			logger.debug("Connection failure to v2 federate " + outgoing.getDisplayName() + " - schedule retry: " + shouldReconnect + " - status: "
					+ status + " cause " + rootCauseMsg);

		}

		fedManager().checkAndSetReconnectStatus(outgoing, rootCauseMsg);

//		trackDisconnectEsvent(fedName);
	}

//	private void trackDisconnectEvent(String fedName) {
//
//		if (getFederate() != null && getFederate().getId() != null) {
//			if (trackedDisconnect.compareAndSet(false, true)) {
//				fedManager.trackDisconnectEventForFederate(getFederate().getId(), fedName, false);
//			}
//		}
//	}

	public void sendPackageAnnounce(CotEventContainer toSend, Set<String> groups) {

		try {
			ROL rol = groupFederationUtil.generatePackageAnnounceROL(toSend);

			if (groups != null) {
				ROL.Builder builder = rol.toBuilder();
				builder.addAllFederateGroups(groups);
				rol = builder.build();
			}

			rolHolder.send(rol);
		} catch (Exception e) {
			logger.warn("exception sending federated mission package announce ROL", e);
		}
	}

	private void serverFederateGroups() {
		// Send a subscription request, get back a stream of messages from server
		asyncFederatedChannel.serverFederateGroupsStream(Subscription.newBuilder()
				.setFilter(Strings.isNullOrEmpty(fedManager().getOutgoingConnection(outgoingName).getFilter()) ? "" : fedManager().getOutgoingConnection(outgoingName).getFilter())
				.setIdentity(Identity.newBuilder()
						.setName(getClientName())
						.setServerId(MessagingDependencyInjectionProxy.getInstance().serverInfo().getServerId())
						.setType(Identity.ConnectionType.FEDERATION_TAK_CLIENT)
						.setUid(clientUid)
						.build()).build(), new StreamObserver<FederateGroups>() {

			@Override
			public void onNext(FederateGroups value) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received remote federate groups = " + value);
				}

				logger.debug("Received remote federate groups = " + value);

				// once the group stream is established, we are ready to setup event streaming
				if (value.getStreamUpdate() != null && value.getStreamUpdate().getStatus() == ServingStatus.SERVING) {
					federateSubscription.setupEventStream();
				}

				// send the server our groups if we havent yet
				if (getFederate().isFederatedGroupMapping() && !hasSentGroups.getAndSet(true)) {
					((FigFederateSubscription) getFederateSubscription()).submitFederateGroups(new HashSet<>(getFederate().getOutboundGroup()));
				}

				groupFederationUtil.collectRemoteFederateGroups(new HashSet<String>(value.getFederateGroupsList()), getFederate());
			}

			@Override
			public void onError(Throwable t) {
				if (t instanceof StatusRuntimeException) {
					StatusRuntimeException sre = (StatusRuntimeException) t;
					if (sre.getStatus().getCode().equals(Status.Code.UNIMPLEMENTED)) {
						federateSubscription.setupEventStream();
					}
				}
			}

			@Override
			public void onCompleted() {
				logger.debug("received  onCompleted");

			}
		});
	}


	class FederationProcessorFactory {

		FederationProcessor<ROL> newProcessor(String resource, String operation, Object parameters, GuardedStreamHolder<ROL> rolHolder) {
			switch (Resource.valueOf(resource.toUpperCase())) {
			case PACKAGE:
				if (!(parameters instanceof ResourceDetails)) {
					throw new IllegalArgumentException("invalid reourcedetails object for mission package processing");
				}
				return new FederationMissionPackageProcessor(resource, operation, (ResourceDetails) parameters, rolHolder);
			default:
				return new FederationMissionProcessor(resource, operation, parameters, rolHolder);
			}
		}
	}

	private class FederationMissionPackageProcessor implements FederationProcessor<ROL> {

		private final String res;
		private final String op;
		private final ResourceDetails dt;

		final GuardedStreamHolder<ROL> rolHolder;

		FederationMissionPackageProcessor(String res, String op, ResourceDetails dt, GuardedStreamHolder<ROL> rolHolder) {
			this.res = res;
			this.op = op;
			this.dt = dt;
			this.rolHolder = rolHolder;
		}

		@Override
		public void process(ROL rol) {

			requireNonNull(res, "resource");
			requireNonNull(op, "operation");
			requireNonNull(dt, "details");

			if (!res.toLowerCase(Locale.ENGLISH).equals("package")) {
				logger.warn("ignoring unexpected ROL resource from client: " + res);
				return;
			}

			switch (op.toLowerCase()) {
			case "announce":
				if (logger.isDebugEnabled()) {
					logger.debug("package details: "  + dt);
				}

				if (Strings.isNullOrEmpty(dt.getSha256())) {
					logger.warn("'announce package' ROL from client contains no resource hash - ignoring.");
					return;
				}

				try {

					NavigableSet<Group> groups = null;

					if (getFederate().isFederatedGroupMapping()) {
						if (federateSubscription.getIsAutoMapped()) {
							groups = groupFederationUtil.autoMapGroups(rol.getFederateGroupsList());
						} else {
							groups = groupFederationUtil.addFederateGroupMapping(fedManager.getInboundGroupMap(getFederate().getId()),
									rol.getFederateGroupsList());
						}

						if ((groups == null || groups.isEmpty()) && getFederate().isFallbackWhenNoGroupMappings()) {
							NavigableSet<Group> allGroups = groupManager.getGroups(federateSubscription.getUser());
							groups = groupFederationUtil.filterGroupDirection(Direction.IN, allGroups);
						}
					} else {
						NavigableSet<Group> allGroups = groupManager.getGroups(federateSubscription.getUser());
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
			case "request":
				if (Strings.isNullOrEmpty(dt.getSha256())) {
					logger.warn("'request package' ROL from server contains no resource hash - ignoring.");
					return;
				}

				try {

					byte[] pbytes = RepositoryService.getInstance().getContentByHash(dt.getSha256());

					if (pbytes != null && pbytes.length > 0) {
						if (logger.isDebugEnabled()) {
							logger.debug("package for hash " + dt.getSha256() + " retrived from repository. " + dt + " dispersing to FIG ");
						}
						dispersePackage(dt, pbytes);
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("null or empty mission package - nothing to disperse.");
						}
					}

				} catch (IOException | SQLException | NamingException e) {
					logger.warn("exception dispersing binary package", e);
				}
				break;
			case "disperse":
				logger.info("received 'disperse package' ROL command from server. ignoring.");
				break;
			default:
				logger.warn("unexpected ROL operation received from server: " + op);
			}
		}

		private void dispersePackage(ResourceDetails details, byte[] bytes) throws IOException {

			if (bytes.length > CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getMaxMessageSizeBytes()) {
				logger.info("File payload size " + bytes.length + " in dispersePackage exceeds the max size! Not sending " + details);
			} else {
				String detailsJson = new ObjectMapper().writeValueAsString(details);

				Builder rol = ROL.newBuilder().setProgram("disperse package\n" + detailsJson + ";");

				if (logger.isDebugEnabled()) {
					logger.debug("bytes to disperse: " + bytes.length);
				}

				BinaryBlob file = BinaryBlob.newBuilder().setData(ByteString.readFrom(new ByteArrayInputStream(bytes))).build();

				rol.addPayload(file);

				if (logger.isDebugEnabled()) {
					logger.debug("dispersing package to server");
				}
				rolHolder.send(rol.build());
			}
		};
	}

	private class FederationMissionProcessor implements FederationProcessor<ROL> {

		FederationMissionProcessor(String res, String op, Object parameters, GuardedStreamHolder<ROL> rolHolder) { }

		@Override
		public void process(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL message sending to handler to process " + rol.getProgram());
			}

			try {

				NavigableSet<Group> groups = null;

				if (getFederate().isFederatedGroupMapping()) {
					if (federateSubscription.getIsAutoMapped()) {
						groups = groupFederationUtil.autoMapGroups(rol.getFederateGroupsList());
					} else {
						groups = groupFederationUtil.addFederateGroupMapping(fedManager.getInboundGroupMap(getFederate().getId()),
								rol.getFederateGroupsList());
					}

					if ((groups == null || groups.isEmpty()) && getFederate().isFallbackWhenNoGroupMappings()) {
						NavigableSet<Group> allGroups = TakFigClient.this.getGroupsForActiveSubscription();
						groups = groupFederationUtil.filterGroupDirection(Direction.IN, allGroups);
					}
				} else {
					NavigableSet<Group> allGroups = TakFigClient.this.getGroupsForActiveSubscription();
					groups = groupFederationUtil.filterGroupDirection(Direction.IN, allGroups);
				}

				try {
					federationROLHandler.onNewEvent(rol, groups);
				} catch (Exception e) {
					logger.error("exception handling federated ROL event", e);
				}

			} catch (RemoteException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception submitting ROL to web", e);
				}
			}
		}
	}

	private DistributedFederationManager fedManager() {
		if (fedManager == null) {
			fedManager = SpringContextBeanForApi.getSpringContext().getBean(DistributedFederationManager.class);
		}
		return fedManager;
	}

	private synchronized void rolSendSync(ROL rol) {
		rolHolder.send(rol);
	}

	public int getFederateMaxHops() {
		return federateMaxHops;
	}
}