package tak.server.federation.hub.broker;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.ClientHealth;
import com.atakmap.Tak.FederateGroups;
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
import com.google.common.collect.ComparisonChain;

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
import tak.server.federation.GuardedStreamHolder;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.FederationHubResources;
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

	private FederationHubServerConfig fedHubConfig;
	private String host;
	private int port;
	private X509Certificate[] sessionCerts;
	private String fedName;
	private String clientUid = UUID.randomUUID().toString().replace("-", "");

	private ManagedChannel channel = null;

	private ClientCall<FederatedEvent, Subscription> clientCall;
	private ClientCall<FederateGroups, Subscription> groupsCall;
	private ClientCall<ROL, Subscription> rolCall;

	private FederatedChannelBlockingStub blockingFederatedChannel;
	private FederatedChannelStub asyncFederatedChannel;

	private SSLConfig sslConfig = new SSLConfig();
	
	GuardedStreamHolder<FederatedEvent> eventStreamHolder;
	GuardedStreamHolder<FederateGroups> groupStreamHolder;
	GuardedStreamHolder<ROL> rolStreamHolder;

	private ScheduledFuture<?> healthScheduler;
	
	private Federate hubClientFederate;
	private List<String> hubClientGroups = new ArrayList<>();
	
	private Subscription serverSubscription;
	private HubConnectionInfo info;
	
	private FederationHubMissionDisruptionManager federationHubMissionDisruptionManager;
	
	public HubFigClient(FederationHubServerConfig fedHubConfig, FederationHubMissionDisruptionManager federationHubMissionDisruptionManager, FederationOutgoingCell federationOutgoingCell) {
		this.fedHubConfig = fedHubConfig;
		this.federationHubMissionDisruptionManager = federationHubMissionDisruptionManager;
		this.host = federationOutgoingCell.getProperties().getHost();
		this.port = federationOutgoingCell.getProperties().getPort();
		this.fedName = federationOutgoingCell.getProperties().getOutgoingName();
		this.info = new HubConnectionInfo();
	}

	public ManagedChannel getChannel() {
		return channel;
	}

	public FederatedChannelStub getAsyncFederatedChannel() {
		return asyncFederatedChannel;
	}

	public void start() throws Exception {
		sslConfig.initSslContext(fedHubConfig);

		channel = openFigConnection(host, port,
				GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
						.protocols("TLSv1.2", "TLSv1.3").keyManager(sslConfig.getKeyMgrFactory())
						.trustManager(sslConfig.getTrustMgrFactory()).build());

		blockingFederatedChannel = FederatedChannelGrpc.newBlockingStub(channel);
		asyncFederatedChannel = FederatedChannelGrpc.newStub(channel);

		if (logger.isDebugEnabled()) {
			logger.debug("init HubFigClient");
		}

		// Send a subscription request, get back a stream of messages from server
		asyncFederatedChannel.clientEventStream(Subscription.newBuilder().setFilter("")
				.setIdentity(Identity.newBuilder().setType(Identity.ConnectionType.FEDERATION_HUB_CLIENT).setServerId(fedHubConfig.getFullId()).setName(fedName).setUid(clientUid).build()).build(),
				new StreamObserver<FederatedEvent>() {

					@Override
					public void onNext(FederatedEvent fedEvent) {
						FederationHubBrokerService.getInstance().handleRead(fedEvent, fedName);
					}

					@Override
					public void onError(Throwable t) {
						logger.error("Event Stream Error: ", t);
						processDisconnect();
						if (shouldRetry.get()) {
							FederationHubBrokerService.getInstance().scheduleRetry(fedName);
						}
					}

					@Override
					public void onCompleted() {}
				});
		
		asyncFederatedChannel.serverFederateGroupsStream(
				Subscription.newBuilder().setFilter("")
						.setIdentity(Identity.newBuilder().setName(fedName).setUid(clientUid).build()).build(),
				new StreamObserver<FederateGroups>() {

					@Override
					public void onNext(FederateGroups value) {
						// once the group stream is established, we are ready to setup event streaming
						if (value.getStreamUpdate() != null && value.getStreamUpdate().getStatus() == ServingStatus.SERVING) {
							setupEventStreamSender();
							
							FederateGroups federateGroups = FederationHubBrokerService.getInstance().getFederationHubGroups(fedName).toBuilder()
									.setStreamUpdate(ServerHealth.newBuilder().setStatus(ServerHealth.ServingStatus.SERVING).build())
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
							logger.error("Server Group Stream Error: ", t);
							processDisconnect();
						}
					}

					@Override
					public void onCompleted() {}
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
						ClientHealth.ServingStatus servingStatus = ClientHealth.ServingStatus.valueOf(value.getStatus().toString());
						eventStreamHolder.updateClientHealth(ClientHealth.newBuilder().setStatus(servingStatus).build());
					} else {
						processDisconnect();
						throw new RuntimeException("Not Healthy");
					}
					
					if (initROLStream.compareAndSet(false, true)) {
						// open the client ROL stream only after getting a health check back. This will trigger transmission of federated mission changes.
						// Subscription / Stream to receive ROL messages from server
						asyncFederatedChannel.clientROLStream(
								Subscription.newBuilder().setFilter("")
										.setIdentity(Identity.newBuilder().setName(fedName).setUid(clientUid).build()).build(),
								new StreamObserver<ROL>() {

									@Override
									public void onNext(ROL value) {
										FederationHubBrokerService.getInstance().parseRol(value, fedName);
									}

									@Override
									public void onError(Throwable t) {
										logger.error("ROL Stream Error: ", t);
										processDisconnect();
									}

									@Override
									public void onCompleted() {}
								});
					}
				}

				@Override
				public void onError(Throwable t) {
					processDisconnect();
				}

				@Override
				public void onCompleted() {
				}
			});
		}, 3, 3, TimeUnit.SECONDS);
	}
	
	private ManagedChannel openFigConnection(final String host, final int port, SslContext sslContext)
			throws IOException {
		return NettyChannelBuilder.forAddress(host, port)
				.negotiationType(NegotiationType.TLS)
				.sslContext(sslContext)
				.maxInboundMessageSize(fedHubConfig.getMaxMessageSizeBytes())
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

							String fingerprint = FederationUtils.getBytesSHA256(clientCert.getEncoded());
							String issuerDN = clientCert.getIssuerX500Principal().getName();
							String issuerCN = Optional.ofNullable(FederationHubBrokerImpl.getCN(issuerDN)).map(cn -> cn.toLowerCase()).orElse("");							
							
							if (fedHubConfig.isUseCaGroups()) {
								try {
									hubClientFederate = new Federate(new FederateIdentity(fedName));
									hubClientFederate.addGroupIdentity(new FederateIdentity(issuerDN + "-" + FederationUtils.getBytesSHA256(caCert.getEncoded())));
									String group = issuerDN + "-" + FederationUtils.getBytesSHA256(caCert.getEncoded());
									hubClientGroups = new ArrayList<>();
									hubClientGroups.add(group);
									FederationHubDependencyInjectionProxy.getInstance().fedHubPolicyManager().addCaFederate(hubClientFederate, hubClientGroups);
								} catch (Exception e) {
									logger.error("error updating federate node", e);
									processDisconnect();
									return null;
								}
							}
							
							FederationPolicyGraph fpg = FederationHubBrokerService.getInstance().getFederationPolicyGraph();
							requireNonNull(fpg, "federation policy graph object");

							Federate clientNode = FederationHubBrokerService.getInstance()
									.getFederationPolicyGraph()
									.getFederate(new FederateIdentity(fedName));
							
							requireNonNull(clientNode, "federation policy node for newly connected client");
							
							setupGroupStreamSender();
							setupRolStreamSender();
						} catch (Exception e) {
							logger.error("Error parsing cert", e);
							processDisconnect();
							return null;
						}

						return certs;
					}
				}).figTlsProtocolNegotiator(sslContext, FederationUtils.authorityFromHostAndPort(host, port))).build();
	}

	private void setupGroupStreamSender() {
		MethodDescriptor<FederateGroups, Subscription> methodDescripton = MethodDescriptor.<FederateGroups, Subscription>newBuilder()
				.setType(MethodDescriptor.MethodType.CLIENT_STREAMING)
				.setFullMethodName(generateFullMethodName("com.atakmap.FederatedChannel", "ClientFederateGroupsStream"))
				.setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.FederateGroups.getDefaultInstance()))
				.setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.Subscription.getDefaultInstance()))
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
			public void onReady() {}

		}, new Metadata());

		// Notify gRPC to receive one response. Without this line, onMessage() would
		// never be called.
		groupsCall.request(1);
		
		groupStreamHolder = new GuardedStreamHolder<FederateGroups>(groupsCall,
                fedName, new Comparator<FederateGroups>() {
                    @Override
                    public int compare(FederateGroups a, FederateGroups b) {
                        return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                    }
                }, true
            );
		FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().addGroupStream(fedName, groupStreamHolder);
	}
	
	private void setServerSubscriptionForConnection() {
		if (eventStreamHolder != null && serverSubscription != null) {
			eventStreamHolder.setSubscription(serverSubscription);
			
			List<HubConnectionInfo> existingConnectionsFromRemoteServer = FederationHubDependencyInjectionProxy.getInstance()
				.hubConnectionStore()
				.getConnectionInfos()
				.stream()
				.filter(i -> i.getRemoteServerId().equals(serverSubscription.getIdentity().getServerId()))
				.collect(Collectors.toList());
			
			// if we already have a connection to/from this server, don't allow another. force close without reconnect.
			if (existingConnectionsFromRemoteServer.size() > 0) {
					logger.info("Error: Connection to/from " + fedName +  " already exists. Disallowing duplicate");
				processDisconnectWithoutRetry();
			} else {
				info.setConnectionId(fedName);
				info.setRemoteConnectionType(serverSubscription.getIdentity().getType().toString());
				info.setLocalConnectionType(Identity.ConnectionType.FEDERATION_HUB_CLIENT.toString());
				info.setRemoteServerId(serverSubscription.getIdentity().getServerId());
				info.setFederationProtocolVersion(2);
            	info.setGroupIdentities(FederationHubBrokerService.getInstance().getFederationPolicyGraph().getFederate(fedName).getGroupIdentities());
            	info.setRemoteAddress(host);

				FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().addConnectionInfo(fedName, info);
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
		
		eventStreamHolder = new GuardedStreamHolder<FederatedEvent>(clientCall,
                fedName, new Comparator<FederatedEvent>() {
                    @Override
                    public int compare(FederatedEvent a, FederatedEvent b) {
                        return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                    }
                }, true
            );
		
		setServerSubscriptionForConnection();
		
		eventStreamHolder.send(FederatedEvent.newBuilder().build());
		FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().addClientStreamHolder(fedName, eventStreamHolder);
		
		FederationPolicyGraph fpg = FederationHubBrokerService.getInstance().getFederationPolicyGraph();                        
        String fedId = eventStreamHolder.getFederateIdentity().getFedId();
        Federate clientNode = fpg.getFederate(fedId);
		
        // Send contact messages from other clients back to this new client.
        for (GuardedStreamHolder<FederatedEvent> otherClient : FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().getClientStreamMap().values()) {

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
				.setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.atakmap.Tak.Subscription.getDefaultInstance()))
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
		
		rolStreamHolder = new GuardedStreamHolder<ROL>(rolCall,
                fedName, new Comparator<ROL>() {
                    @Override
                    public int compare(ROL a, ROL b) {
                        return ComparisonChain.start().compare(a.hashCode(), b.hashCode()).result();
                    }
                }, true
            );
		
		// get the changes, but don't send till we add the rolStream because the stream will get used
        // down the line for getting the session id        
		FederationHubMissionDisruptionManager.OfflineMissionChanges changes = null;
        if (fedHubConfig.isMissionFederationDisruptionEnabled()) {
        	changes = federationHubMissionDisruptionManager.getMissionChangesAndTrackConnectEvent(
            		rolStreamHolder.getFederateIdentity().getFedId(), sessionCerts);
        }
		
		FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().addRolStream(fedName, rolStreamHolder);
		
		AtomicLong delayMs = new AtomicLong(5000l);
		
		if (changes != null) {
			for(final Entry<ObjectId, ROL.Builder> entry: changes.getResourceRols().entrySet()) {
				FederationHubResources.mfdtScheduler.schedule(() -> {
					ROL rol = federationHubMissionDisruptionManager.hydrateResourceROL(entry.getKey(), entry.getValue());
					rolStreamHolder.send(rol);
				}, delayMs.getAndAdd(500), TimeUnit.MILLISECONDS);
			}
			for(final ROL rol: changes.getRols()) {
				FederationHubResources.mfdtScheduler.schedule(() -> {
					rolStreamHolder.send(rol);
				}, delayMs.getAndAdd(100), TimeUnit.MILLISECONDS);
			}
		}
	}

	private final AtomicBoolean hasDisconnected = new AtomicBoolean(false);
	private final AtomicBoolean shouldRetry = new AtomicBoolean(true);
	
	// force close the connection, don't retry to connect since this was intentional.
	public void processDisconnectWithoutRetry() {
		shouldRetry.set(false);
		processDisconnect();
	}
	
	public void processDisconnect() {
		if (hasDisconnected.getAndSet(true)) return;
		
		logger.info("processDisconnect for " + fedName);

		if (healthScheduler != null && !healthScheduler.isCancelled()) {
			healthScheduler.cancel(true);
		}

		try {
			if (groupsCall != null)
				groupsCall.cancel("Close group stream", new Error("disconnect"));
		} catch (Exception e) {
			logger.error("error closing group call", e);
		}

		try {
			if (clientCall != null)
				clientCall.cancel("Close client stream", new Error("disconnect"));
		} catch (Exception e) {
			logger.error("error closing clientCall", e);
		}

		try {
			if (rolCall != null)
				rolCall.cancel("Close rol stream", new Error("disconnect"));
		} catch (Exception e) {
			logger.error("error closing rolCall", e);
		}

		try {
			channel.shutdownNow();
		} catch (Exception e) {
			logger.warn("error terminating federated channel", e);
		}
		
		FederationHubDependencyInjectionProxy.getSpringContext().publishEvent(new HubClientDisconnectEvent(this, fedName));
		FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().clearIdFromAllStores(fedName);
	}
}
