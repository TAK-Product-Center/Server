package tak.server.federation.hub.broker;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.FederateGroupHopLimits;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederateProvenance;
import com.atakmap.Tak.FederateTokenResponse;
import com.atakmap.Tak.FederatedChannelGrpc;
import com.atakmap.Tak.FederatedChannelGrpc.FederatedChannelBlockingStub;
import com.atakmap.Tak.FederatedChannelGrpc.FederatedChannelStub;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.Identity;
import com.atakmap.Tak.ROL;
import com.atakmap.Tak.ServerHealth;
import com.atakmap.Tak.ServerHealth.ServingStatus;
import com.atakmap.Tak.Subscription;
import com.bbn.roger.fig.FederationUtils;
import com.bbn.roger.fig.FigProtocolNegotiator;
import com.bbn.roger.fig.Propagator;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.protobuf.ByteString;

import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.FedhubGuardedStreamHolder;
import tak.server.federation.TokenAuthCredential;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.FederationHubResources;
import tak.server.federation.hub.FederationHubUtils;
import tak.server.federation.hub.broker.db.FederationHubMissionDisruptionManager;
import tak.server.federation.hub.broker.events.HubClientDisconnectEvent;
import tak.server.federation.hub.ui.graph.FederationOutgoingCell;

/*
 *
 * Handler for v2 federation
 *
 */
public class HubFigClient implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(HubFigClient.class);

	private FederationHubServerConfigManager fedHubConfigManager;

	private String host;
	private int port;
	
	private String connectionToken;
	private boolean useToken = false;
	private String tokenType = "";
	
	private X509Certificate[] sessionCerts;
	private String clientFingerprint;
	private List<String> clientGroups;
	private String fedName;
	private String clientUid = UUID.randomUUID().toString().replace("-", "");

	private ManagedChannel channel = null;

	private ClientCall<FederatedEvent, Subscription> clientCall;
	private ClientCall<FederateGroups, Subscription> groupsCall;
	private ClientCall<ROL, Subscription> rolCall;

	private FederatedChannelBlockingStub blockingFederatedChannel;
	private FederatedChannelStub asyncFederatedChannel;
	private FederatedChannelStub asyncNoAuthChannel;
	
	private AtomicBoolean initSenders = new AtomicBoolean(false);

	private final long maxMem = Runtime.getRuntime().maxMemory();
	private final WriteBufferWaterMark waterMark;

	private SSLConfig sslConfig = new SSLConfig();

	private FedhubGuardedStreamHolder<FederatedEvent> eventStreamHolder;
	private FedhubGuardedStreamHolder<FederateGroups> groupStreamHolder;
	private FedhubGuardedStreamHolder<ROL> rolStreamHolder;

	private ScheduledFuture<?> healthScheduler;

	private Federate hubClientFederate;
	private List<String> hubClientGroups = new ArrayList<>();

	private Subscription serverSubscription;
	private HubConnectionInfo info;

	private FederateProvenance provenance;

	private FederationHubMissionDisruptionManager federationHubMissionDisruptionManager;

	public HubFigClient(FederationHubServerConfigManager fedHubConfigManager,
			FederationHubMissionDisruptionManager federationHubMissionDisruptionManager,
			FederationOutgoingCell federationOutgoingCell) {
		this.fedHubConfigManager = fedHubConfigManager;
		this.federationHubMissionDisruptionManager = federationHubMissionDisruptionManager;
		this.host = federationOutgoingCell.getProperties().getHost();
		this.port = federationOutgoingCell.getProperties().getPort();
		this.fedName = federationOutgoingCell.getProperties().getOutgoingName();
		this.connectionToken = federationOutgoingCell.getProperties().getToken();
		this.useToken = federationOutgoingCell.getProperties().isUseToken();
		this.tokenType = federationOutgoingCell.getProperties().getTokenType();

		
		this.info = new HubConnectionInfo();

		long computedHighMark = (long) ((maxMem * 0.75)
				/ fedHubConfigManager.getConfig().getMaxExpectedConnectedFederates());
		computedHighMark = Math.min(computedHighMark, Integer.MAX_VALUE);

		int highMark = (int) computedHighMark;
		int lowMark = highMark / 2;

		waterMark = new WriteBufferWaterMark(lowMark, highMark);

		provenance = FederateProvenance.newBuilder().setFederationServerId(fedHubConfigManager.getConfig().getFullId())
				.setFederationServerName(fedHubConfigManager.getConfig().getFullId()).build();
	}

	public ManagedChannel getChannel() {
		return channel;
	}

	public FederatedChannelStub getAsyncFederatedChannel() {
		return asyncFederatedChannel;
	}

	public void start() throws Exception {		
		sslConfig.initSslContext(fedHubConfigManager.getConfig());
		
		SslContextBuilder sslContextBuilder;
		// don't use a key manager if we are using token auth
		if (useToken) {
			sslContextBuilder = GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
					.protocols("TLSv1.2", "TLSv1.3")
					.trustManager(sslConfig.getTrustMgrFactory());
		} else {
			sslContextBuilder = GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
					.protocols("TLSv1.2", "TLSv1.3")
					.keyManager(sslConfig.getKeyMgrFactory())
					.trustManager(sslConfig.getTrustMgrFactory());
		}

		channel = openFigConnection(host, port, sslContextBuilder.build());
		
		if (useToken && !Strings.isNullOrEmpty(tokenType)) {
			if ("automatic".equals(tokenType.toLowerCase())) {
				asyncNoAuthChannel = FederatedChannelGrpc.newStub(channel).withCallCredentials(null);
                
                X509Certificate clientCert = null;
                BinaryBlob certPayload = null;
                clientCert = FederationUtils.loadX509CertFromJKSFile(fedHubConfigManager.getConfig().getKeystoreFile(), fedHubConfigManager.getConfig().getKeystorePassword());
                certPayload = BinaryBlob.newBuilder().setData(ByteString.readFrom(new ByteArrayInputStream(clientCert.getEncoded()))).build();

                asyncNoAuthChannel.getAuthTokenByX509(certPayload, new StreamObserver<com.atakmap.Tak.FederateTokenResponse>() {
					@Override
					public void onNext(FederateTokenResponse value) {
						TokenAuthCredential credential = new TokenAuthCredential(value.getToken());
						blockingFederatedChannel = FederatedChannelGrpc.newBlockingStub(channel).withCallCredentials(credential);
						asyncFederatedChannel = FederatedChannelGrpc.newStub(channel).withCallCredentials(credential);
						
						initReceivers();

						if (initSenders.getAndSet(true)) {
							setupGroupStreamSender();
							setupRolStreamSender();
						}
					}

					@Override
					public void onError(Throwable t) {
						processDisconnect(t);
						if (shouldRetry.get()) {
							FederationHubBrokerService.getInstance().scheduleRetry(fedName);
						}
					}

					@Override
					public void onCompleted() {}
                	
                });
			} else {
				TokenAuthCredential credential = new TokenAuthCredential(connectionToken);
				blockingFederatedChannel = FederatedChannelGrpc.newBlockingStub(channel).withCallCredentials(credential);
				asyncFederatedChannel = FederatedChannelGrpc.newStub(channel).withCallCredentials(credential);
				initReceivers();
			}
		} else {
			blockingFederatedChannel = FederatedChannelGrpc.newBlockingStub(channel);
			asyncFederatedChannel = FederatedChannelGrpc.newStub(channel);
			initReceivers();
		}
	}

	private void initReceivers() {
		if (logger.isDebugEnabled()) {
			logger.debug("init HubFigClient");
		}
		// Send a subscription request, get back a stream of messages from server
		asyncFederatedChannel.clientEventStream(Subscription.newBuilder().setFilter("")
				.setIdentity(Identity.newBuilder().setType(Identity.ConnectionType.FEDERATION_HUB_CLIENT)
						.setServerId(fedHubConfigManager.getConfig().getFullId()).setName(fedName).setUid(clientUid)
						.build())
				.build(), new StreamObserver<FederatedEvent>() {

					@Override
					public void onNext(FederatedEvent fedEvent) {
						FederationHubBrokerService.getInstance().handleRead(fedEvent, fedName);
					}

					@Override
					public void onError(Throwable t) {
						processDisconnect(t);
						if (shouldRetry.get()) {
							FederationHubBrokerService.getInstance().scheduleRetry(fedName);
						}
					}

					@Override
					public void onCompleted() {
					}
				});

		asyncFederatedChannel.serverFederateGroupsStream(
				Subscription.newBuilder().setFilter("")
						.setIdentity(Identity.newBuilder().setName(fedName).setUid(clientUid).build()).build(),
				new StreamObserver<FederateGroups>() {

					@Override
					public void onNext(FederateGroups value) {
						// once the group stream is established, we are ready to setup event streaming
						if (value.getStreamUpdate() != null
								&& value.getStreamUpdate().getStatus() == ServingStatus.SERVING) {
							setupEventStreamSender();

							FederateGroups federateGroups = FederationHubBrokerService.getInstance()
									.getFederationHubGroups(fedName).toBuilder().setStreamUpdate(ServerHealth
											.newBuilder().setStatus(ServerHealth.ServingStatus.SERVING).build())
									.build();

							groupStreamHolder.send(federateGroups);
						}

						FederationHubBrokerService.getInstance().addFederateGroups(fedName, value);
					}

					@Override
					public void onError(Throwable t) {
						if (t instanceof StatusRuntimeException) {
							StatusRuntimeException sre = (StatusRuntimeException) t;
							if (sre.getStatus().getCode().equals(Status.Code.UNIMPLEMENTED)) {
								setupEventStreamSender();
							}
						} else {
							processDisconnect(t);
						}
					}

					@Override
					public void onCompleted() {
					}
				});

		final AtomicBoolean initROLStream = new AtomicBoolean(false);
		healthScheduler = FederationHubResources.healthCheckScheduler.scheduleWithFixedDelay(() -> {
			ClientHealth clientHealth = ClientHealth.newBuilder().setStatus(ClientHealth.ServingStatus.SERVING).build();

			// set the server health to a timestamp only when we are sending a health check
			// message
			asyncFederatedChannel.healthCheck(clientHealth, new StreamObserver<ServerHealth>() {
				@Override
				public void onNext(ServerHealth value) {
					if (logger.isDebugEnabled()) {
						logger.debug("received federated health check message from server " + value);
					}

					if (value.getStatus().equals(ServerHealth.ServingStatus.SERVING)) {
						ClientHealth.ServingStatus servingStatus = ClientHealth.ServingStatus
								.valueOf(value.getStatus().toString());
						eventStreamHolder
								.updateClientHealth(ClientHealth.newBuilder().setStatus(servingStatus).build());
					} else {
						RuntimeException e = new RuntimeException("Not Healthy");
						processDisconnect(e);
						throw e;
					}

					if (initROLStream.compareAndSet(false, true)) {
						// open the client ROL stream only after getting a health check back. This will
						// trigger transmission of federated mission changes.
						// Subscription / Stream to receive ROL messages from server
						asyncFederatedChannel.clientROLStream(Subscription.newBuilder().setFilter("")
								.setIdentity(Identity.newBuilder().setName(fedName).setUid(clientUid).build()).build(),
								new StreamObserver<ROL>() {

									@Override
									public void onNext(ROL value) {
										FederationHubBrokerService.getInstance().parseRol(value, fedName);
									}

									@Override
									public void onError(Throwable t) {
										processDisconnect(t);
									}

									@Override
									public void onCompleted() {
									}
								});
					}
				}

				@Override
				public void onError(Throwable t) {
					processDisconnect(t);
				}

				@Override
				public void onCompleted() {
				}
			});
		}, 3, 3, TimeUnit.SECONDS);
	}

	private ManagedChannel openFigConnection(final String host, final int port, SslContext sslContext)
			throws IOException {
		return NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.TLS).sslContext(sslContext)
				.withOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark)
				.maxInboundMessageSize(fedHubConfigManager.getConfig().getMaxMessageSizeBytes())
				.channelType(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
				.executor(FederationHubResources.federationGrpcExecutor)
				.eventLoopGroup(FederationHubResources.federationGrpcWorkerEventLoopGroup)
				.protocolNegotiator(new FigProtocolNegotiator(new Propagator<X509Certificate[]>() {
					@Override
					public X509Certificate[] propogate(X509Certificate[] certs) {
						try {
							sessionCerts = certs;
							X509Certificate clientCert = certs[0];
							X509Certificate caCert = certs[1];

							clientFingerprint = FederationUtils
									.getBytesSHA256(((X509Certificate) clientCert).getEncoded());
							clientGroups = FederationHubUtils.getCaGroupIdsFromCerts(certs);

							logger.info("Received remote fingerprint {} for {}", clientFingerprint, fedName);

							if (fedHubConfigManager.getConfig().isUseCaGroups()) {
								try {
									hubClientFederate = new Federate(new FederateIdentity(fedName));

									for (String clientGroup : clientGroups) {
										hubClientFederate.addGroupIdentity(new FederateIdentity(clientGroup));
										hubClientGroups.add(clientGroup);
									}

									FederationHubDependencyInjectionProxy.getInstance().fedHubPolicyManager()
											.addCaFederate(hubClientFederate, hubClientGroups);
								} catch (Exception e) {
									logger.error("error updating federate node", e);
									processDisconnect(e);
									return null;
								}
							}

							FederationPolicyGraph fpg = FederationHubBrokerService.getInstance()
									.getFederationPolicyGraph();
							requireNonNull(fpg, "federation policy graph object");

							Federate clientNode = FederationHubBrokerService.getInstance().getFederationPolicyGraph()
									.getFederate(new FederateIdentity(fedName));

							requireNonNull(clientNode, "federation policy node for newly connected client");

							if (asyncFederatedChannel != null && !initSenders.get()) {
								initSenders.set(true);
								setupGroupStreamSender();
								setupRolStreamSender();
							}
						} catch (Exception e) {
							logger.error("Error parsing cert", e);
							processDisconnect(e);
							return null;
						}

						return certs;
					}
				}).figTlsProtocolNegotiator(sslContext, FederationUtils.authorityFromHostAndPort(host, port))).build();
	}

	private void setupGroupStreamSender() {
		MethodDescriptor<FederateGroups, Subscription> methodDescripton = MethodDescriptor
				.<FederateGroups, Subscription>newBuilder().setType(MethodDescriptor.MethodType.CLIENT_STREAMING)
				.setFullMethodName(generateFullMethodName("com.atakmap.FederatedChannel", "ClientFederateGroupsStream"))
				.setRequestMarshaller(
						io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.FederateGroups.getDefaultInstance()))
				.setResponseMarshaller(
						io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.Subscription.getDefaultInstance()))
				.build();

		groupsCall = channel.newCall(methodDescripton, asyncFederatedChannel.getCallOptions());
		// use listener to respect flow control, and send messages to the server when it
		// is ready
		groupsCall.start(new ClientCall.Listener<Subscription>() {

			@Override
			public void onMessage(Subscription response) {
				// Notify gRPC to receive one additional response.
				groupsCall.request(1);

			}

			@Override
			public void onReady() {
			}

		}, new Metadata());

		// Notify gRPC to receive one response. Without this line, onMessage() would
		// never be called.
		groupsCall.request(1);

		groupStreamHolder = new FedhubGuardedStreamHolder<FederateGroups>(groupsCall, fedName, clientFingerprint,
				clientGroups, provenance, new Comparator<FederateGroups>() {
					@Override
					public int compare(FederateGroups a, FederateGroups b) {
						return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
					}
				});

		FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().addGroupStream(fedName,
				groupStreamHolder);
	}

	private void setServerSubscriptionForConnection() {
		if (eventStreamHolder != null && serverSubscription != null) {
			eventStreamHolder.setSubscription(serverSubscription);

			List<HubConnectionInfo> existingConnectionsFromRemoteServer = FederationHubDependencyInjectionProxy
					.getInstance().hubConnectionStore().getConnectionInfos().stream()
					.filter(i -> i.getRemoteServerId().equals(serverSubscription.getIdentity().getServerId()))
					.collect(Collectors.toList());

			// if we already have a connection to/from this server, don't allow another.
			// force close without reconnect.
			if (existingConnectionsFromRemoteServer.size() > 0) {
				logger.info("Error: Connection to/from " + fedName + " already exists. Disallowing duplicate");
				processDisconnectWithoutRetry(
						new Throwable("Connection to/from " + fedName + " already exists. Disallowing duplicate"));
			} else {
				info.setConnectionId(fedName);
				info.setRemoteConnectionType(serverSubscription.getIdentity().getType().toString());
				info.setLocalConnectionType(Identity.ConnectionType.FEDERATION_HUB_CLIENT.toString());
				info.setRemoteServerId(serverSubscription.getIdentity().getServerId());
				info.setFederationProtocolVersion(2);
				info.setGroupIdentities(FederationHubBrokerService.getInstance().getFederationPolicyGraph()
						.getFederate(fedName).getGroupIdentities());
				info.setRemoteAddress(host);

				FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().addConnectionInfo(fedName,
						info);
			}
			logger.info("Outgoing connection for {} established ", fedName);
		}
	}

	public void setupEventStreamSender() {
		MethodDescriptor<FederatedEvent, Subscription> methodDescripton = MethodDescriptor.<FederatedEvent, Subscription>newBuilder()
				.setType(MethodDescriptor.MethodType.CLIENT_STREAMING)
				.setFullMethodName(generateFullMethodName("com.atakmap.FederatedChannel", "ServerEventStream"))
				.setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.FederatedEvent.getDefaultInstance()))
				.setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.Subscription.getDefaultInstance()))
				.build();
		
		clientCall = channel.newCall(methodDescripton, asyncFederatedChannel.getCallOptions());

		// use listener to respect flow control, and send messages to the server when it
		// is ready
		clientCall.start(new ClientCall.Listener<Subscription>() {

			@Override
			public void onMessage(Subscription response) {
				serverSubscription = response;
				setServerSubscriptionForConnection();
				// Notify gRPC to receive one additional response.
				clientCall.request(1);
			}

			@Override
			public void onReady() {
			}
		}, new Metadata());

		// Notify gRPC to receive one response. Without this line, onMessage() would
		// never be called.
		clientCall.request(1);
		
		eventStreamHolder = new FedhubGuardedStreamHolder<FederatedEvent>(clientCall,
                fedName, clientFingerprint, clientGroups, provenance, new Comparator<FederatedEvent>() {
                    @Override
                    public int compare(FederatedEvent a, FederatedEvent b) {
                        return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                    }
                }
            );
        
		setServerSubscriptionForConnection();
		
		eventStreamHolder.send(FederatedEvent.newBuilder().build());
		FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().addClientStreamHolder(fedName, eventStreamHolder);
		
		FederationPolicyGraph fpg = FederationHubBrokerService.getInstance().getFederationPolicyGraph();                        
        String fedId = eventStreamHolder.getFederateIdentity().getFedId();
        Federate clientNode = fpg.getFederate(fedId);
		
        // Send contact messages from other clients back to this new client.
        for (FedhubGuardedStreamHolder<FederatedEvent> otherClient : FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().getClientStreamMap().values()) {

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
                	if (FederationHubBrokerService.isDestinationEdgeReachableByGroupFilter(edge, event.getFederateGroupsList())) {
                		FederatedEvent.Builder builder = event.toBuilder();
                		
                		FederateGroupHopLimits groupHopLimits =  event.getFederateGroupHopLimits();
    					if (groupHopLimits != null && groupHopLimits.getLimitsList().size() > 0) {
    						groupHopLimits = FederationHubBrokerService.removeEdgeFilteredHopLimitedGroupsFromList(groupHopLimits, edge);
    						builder.setFederateGroupHopLimits(groupHopLimits);
    					}
    					
    					List<String> federateGroups =  builder.getFederateGroupsList();
    					if (federateGroups != null && federateGroups.size() > 0) {
    						List<String> filteredFederateGroups = FederationHubBrokerService.removeFilteredGroups(federateGroups, edge);
    						builder.clearFederateGroups().addAllFederateGroups(filteredFederateGroups);			
    					}
    					
    					event = builder.build();
	                	
    					eventStreamHolder.send(event);
	                    if (logger.isDebugEnabled()) {
	                        logger.debug("Sending v2 cached " + event +
	                            " from " + otherClientNode.getFederateIdentity().getFedId() +
	                            " to " + clientNode.getFederateIdentity().getFedId());
	                    }
                	}
                }
            }
        }
	}

	public void setupRolStreamSender() {
		MethodDescriptor<ROL, Subscription> methodDescripton = MethodDescriptor.<ROL, Subscription>newBuilder()
				.setType(MethodDescriptor.MethodType.CLIENT_STREAMING)
				.setFullMethodName(generateFullMethodName("com.atakmap.FederatedChannel", "ServerROLStream"))
				.setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.ROL.getDefaultInstance()))
				.setResponseMarshaller(
						io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.Subscription.getDefaultInstance()))
				.build();

		rolCall = channel.newCall(methodDescripton, asyncFederatedChannel.getCallOptions());

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

		// Notify gRPC to receive one response. Without this line, onMessage() would
		// never be called.
		rolCall.request(1);

		rolStreamHolder = new FedhubGuardedStreamHolder<ROL>(rolCall, fedName, clientFingerprint, clientGroups,
				provenance, new Comparator<ROL>() {
					@Override
					public int compare(ROL a, ROL b) {
						return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
					}
				});

		// get the changes, but don't send till we add the rolStream because the stream
		// will get used
		// down the line for getting the session id
		FederationHubMissionDisruptionManager.OfflineMissionChanges changes = null;
		if (fedHubConfigManager.getConfig().isMissionFederationDisruptionEnabled()) {
			changes = federationHubMissionDisruptionManager.getMissionChangesAndTrackConnectEvent(
					rolStreamHolder.getFederateIdentity().getFedId(), clientGroups);
		}

		FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().addRolStream(fedName, rolStreamHolder);

		AtomicLong delayMs = new AtomicLong(5000l);

		if (changes != null) {
			for (final Entry<ObjectId, ROL.Builder> entry : changes.getResourceRols().entrySet()) {
				FederationHubResources.mfdtScheduler.schedule(() -> {
					ROL rol = federationHubMissionDisruptionManager.hydrateResourceROL(entry.getKey(),
							entry.getValue());
					rolStreamHolder.send(rol);
				}, delayMs.getAndAdd(500), TimeUnit.MILLISECONDS);
			}
			for (final ROL rol : changes.getRols()) {
				FederationHubResources.mfdtScheduler.schedule(() -> {
					rolStreamHolder.send(rol);
				}, delayMs.getAndAdd(100), TimeUnit.MILLISECONDS);
			}
		}
	}

	private final AtomicBoolean hasDisconnected = new AtomicBoolean(false);
	private final AtomicBoolean shouldRetry = new AtomicBoolean(true);

	// force close the connection, don't retry to connect since this was
	// intentional.
	public void processDisconnectWithoutRetry(Throwable cause) {
		shouldRetry.set(false);
		processDisconnect(cause);
	}

	public void processDisconnect(Throwable cause) {
		if (hasDisconnected.getAndSet(true))
			return;

		String rootCauseMsg = FederationUtils.getHumanReadableErrorMsg(cause);

		logger.info("Process Federate Disconnect : " + fedName + " due to " + rootCauseMsg);

		if (healthScheduler != null && !healthScheduler.isCancelled()) {
			healthScheduler.cancel(true);
		}

		try {
			if (groupsCall != null)
				groupsCall.cancel("Close group stream", new Exception("Graceful close of group channel", cause));
		} catch (Exception e) {
			logger.error("error closing group call", e);
		}

		try {
			if (clientCall != null)
				clientCall.cancel("Close client stream", new Exception("Graceful close of client channel", cause));
		} catch (Exception e) {
			logger.error("error closing clientCall", e);
		}

		try {
			if (rolCall != null)
				rolCall.cancel("Close rol stream", new Exception("Graceful close of rol channel", cause));
		} catch (Exception e) {
			logger.error("error closing rolCall", e);
		}

		try {
			channel.shutdownNow();
		} catch (Exception e) {
			logger.warn("error terminating federated channel", e);
		}

		FederationHubDependencyInjectionProxy.getSpringContext()
				.publishEvent(new HubClientDisconnectEvent(this, fedName));
		FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().clearIdFromAllStores(fedName);
	}

	public String getClientFingerprint() {
		return clientFingerprint;
	}

	public String getFedName() {
		return fedName;
	}
}
