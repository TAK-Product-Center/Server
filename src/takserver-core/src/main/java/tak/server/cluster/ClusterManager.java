package tak.server.cluster;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Strings;
import io.nats.client.Connection;
import io.nats.client.Nats;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import atakmap.commoncommo.protobuf.v1.Missionannouncement.MissionAnnouncement;

import com.atakmap.Tak.ROL;

import tak.server.util.ActiveProfiles;
import com.bbn.marti.config.Cluster;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.remote.util.SpringContextBeanForApi;


import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;

import tak.server.CommonConstants;
import tak.server.Constants;
import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.cot.CotEventContainer;
import tak.server.messaging.MessageConverter;
import tak.server.qos.MessageBaseStrategy;

public class ClusterManager implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

	private static final Logger logger = LoggerFactory.getLogger(ClusterManager.class);

	private Connection natsConnection;

	private static ApplicationContext applicationContext;

	private static ClusterManager instance;

	private final CoreConfigFacade coreConfig = CoreConfigFacade.getInstance();

	private final Cluster config = coreConfig.getRemoteConfiguration().getCluster();
	
	private static final String SUBSCRIPTION_COUNTER = "subscriptionCounter";

	private static final String MESSAGES_RECEIVED_COUNTER = "messagesReceivedCounter";
	private static final String MESSAGES_SENT_COUNTER = "messagesSentCounter";
	private static final String CLUSTER_MESSAGES_SENT_COUNTER = "clusterMessagesSentCounter";
	private static final String CLUSTER_MESSAGES_RECEIVED_COUNTER = "clusterMessagesReceivedCounter";

	private static final AtomicLong messagesSentTempCounter = new AtomicLong();
	private static final AtomicLong subscriptionsTempCounter = new AtomicLong();
	private static final AtomicLong messagesReceivedTempCounter = new AtomicLong();
	private static final AtomicLong clusterMessagesReceivedTempCounter = new AtomicLong();
	private static final AtomicLong clusterMessagesSentTempCounter = new AtomicLong();

	private MessageConverter clusterMessageConverter;

	@Autowired
	private Ignite ignite;

	@Autowired
	private ServerInfo serverInfo;

	// dependency on CacheManager is for initialization ordering only
	public ClusterManager(CacheManager cacheManager, MessageConverter clusterMessageConverter) {
		this.clusterMessageConverter = clusterMessageConverter;
	}

	public static ClusterManager getInstance() {
		if (instance == null) {
			synchronized (ClusterManager.class) {
				if (instance == null) {
					instance = applicationContext.getBean(ClusterManager.class);
				}
			}
		}

		return instance;
	}
	
	// After the spring context is initialized, set up the message clustering and
	// metrics
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			natsConnection = Nats.connect(config.getNatsURL());
		} catch (IOException | InterruptedException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("error establishing NATS connection", e);
			}
		}
		

		if (ActiveProfiles.getInstance().isMessagingProfileActive()) {
			Resources.clusterStateProcessor.execute(() -> {
				try {
					natsConnection.createDispatcher().subscribe(Constants.CLUSTER_DATA_MESSAGE, m -> {
						try {		
							
							countClusterMessageRecieved();
							
							CotEventContainer clusterCot = clusterMessageConverter.dataMessageToCot(m.getData());
						
							String sourceClusterNodeId = (String) clusterCot.getContext(Constants.CLUSTER_MESSAGE_KEY);

							if (sourceClusterNodeId == null || sourceClusterNodeId.toLowerCase(Locale.ENGLISH)
									.equals(serverInfo.getServerId().toLowerCase(Locale.ENGLISH))) {
								if (logger.isDebugEnabled()) {
									logger.debug("ignoring clustered message that originated in this node");
								}
								return;
							}
										
							MessagingDependencyInjectionProxy.getInstance().submissionService().addToInputQueue(clusterCot);
						} catch (Exception e) {
							logger.warn("exception processing clustered message", e);
						}

					});
				} catch (Exception e) {
					logger.error("exception connecting to NATS server to receive messages", e);
				}
			});

			// receive plugin messages over NATS in the cluster
			Resources.clusterStateProcessor.execute(() -> {
				if (coreConfig.getRemoteConfiguration().getPlugins().isUsePluginMessageQueue()) {
					try {
						natsConnection.createDispatcher().subscribe(CommonConstants.CLUSTER_PLUGIN_PUBLISH_TOPIC, m -> {
							try {
								CotEventContainer pluginCotEvent = clusterMessageConverter.dataMessageToCot((byte[]) m.getData(), false);
								pluginCotEvent.setContextValue(Constants.PLUGIN_MESSAGE_KEY, Boolean.TRUE);
								MessagingDependencyInjectionProxy.getInstance().cotMessenger().send(pluginCotEvent);
							} catch (Exception e) {
								logger.error("exception connecting to NATS server to receive messages", e);
							} 
						});
					} catch (Exception e2) {
						logger.error("exception subscribing to " + CommonConstants.CLUSTER_PLUGIN_PUBLISH_TOPIC, e2);
					}
				}
			});	
			
			// Receive cluster mission data messages
			Resources.clusterMissionStateProcessor.execute(() -> {
				try {
					natsConnection.createDispatcher().subscribe(Constants.CLUSTER_MISSION_DATA_MESSAGE, m -> {
						try {
							countClusterMessageRecieved();
							
							MissionAnnouncement missionannouncement = MissionAnnouncement.parseFrom(m.getData());	
							CotEventContainer missionCot = clusterMessageConverter.getCotFromMissionAnnouncement(missionannouncement);
							
							
							
							switch (ClusterMissionAnnouncementType.valueOf(missionannouncement.getMissionAnnouncementType())) {
							case AnnounceMissionChange:
								DistributedSubscriptionManager.getInstance()
									.submitAnnounceMissionChangeCot(missionannouncement.getMissionName(), UUID.fromString(missionannouncement.getMissionGuid()), missionCot);
								break;
							case BroadcastMissionAnnouncement:
								DistributedSubscriptionManager.getInstance()
									.submitBroadcastMissionAnnouncementCot(missionannouncement.getCreatorUid(), missionannouncement.getGroupVector(), missionCot);
								break;
							case SendMissionInvite:
								DistributedSubscriptionManager.getInstance()
									.submitSendMissionInviteCot(missionannouncement.getUidsList().stream().toArray(String[]::new), missionCot);
								break;
							case SendMissionRoleChange:
								DistributedSubscriptionManager.getInstance()
									.submitSendMissionRoleChangeCot(missionannouncement.getClientUid(), missionCot);
								break;
							default:
								break;
							}
						} catch (Exception e) {
							logger.warn("exception processing clustered mission message", e);
						}

					});
				} catch (Exception e) {
					logger.error("exception connecting to NATS server to receive messages", e);
				}
			});
		}

		// Receive cluster control messages
		Resources.clusterStateProcessor.execute(() -> {
			try {
				natsConnection.createDispatcher().subscribe(Constants.CLUSTER_CONTROL_MESSAGE, m -> {
					try {

						ROL controlMessage = clusterMessageConverter.controlMessageToRol(m.getData());

						if (logger.isDebugEnabled()) {
							logger.debug("received control message: " + controlMessage);
						}

						// process the control message
						onControlMessage(controlMessage);

					} catch (Exception e) {
						logger.warn("exception processing clustered add input message", e);
					}

				});
			} catch (Exception e) {
				logger.error("exception connecting to NATS server to receive messages", e);
			}
		});

		// Periodically updated clustered counter for messages sent
		Resources.scheduledClusterStateExecutor.scheduleWithFixedDelay(() -> {
			try {
				if (messagesSentTempCounter.get() > 0) {
					getMessagesSentCounter().addAndGet(messagesSentTempCounter.getAndSet(0));
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("error updating messages sent clustered counter", e);
				}
			}
		}, config.getMetricsIntervalSeconds(), config.getMetricsIntervalSeconds(), TimeUnit.SECONDS);

		// Periodically updated clustered counter for messages received
		Resources.scheduledClusterStateExecutor.scheduleWithFixedDelay(() -> {
			try {
				if (messagesReceivedTempCounter.get() > 0) {
					getMessagesReceivedCounter().addAndGet(messagesReceivedTempCounter.getAndSet(0));
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("error updating messages received clustered counter");
				}
			}
		}, config.getMetricsIntervalSeconds(), config.getMetricsIntervalSeconds(), TimeUnit.SECONDS);

		// Periodically updated clustered messages clustered counter
		Resources.scheduledClusterStateExecutor.scheduleWithFixedDelay(() -> {
			try {
				if (clusterMessagesReceivedTempCounter.get() > 0) {
					getClusterMessagesReceivedCounter().addAndGet(clusterMessagesReceivedTempCounter.getAndSet(0));
				}
				
				if (clusterMessagesSentTempCounter.get() > 0) {
					getClusterMessagesSentCounter().addAndGet(clusterMessagesSentTempCounter.getAndSet(0));
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("error updating messages clustered clustered counter");
				}
			}
		}, config.getMetricsIntervalSeconds(), config.getMetricsIntervalSeconds(), TimeUnit.SECONDS);
		
		// Periodically updated clustered subscription counter
		Resources.scheduledClusterStateExecutor.scheduleWithFixedDelay(() -> {
			try {
				if (subscriptionsTempCounter.get() != 0) {
					int total = (int) getSubscriptionCounter().addAndGet(subscriptionsTempCounter.getAndSet(0));
					// when in the cluster, make sure our message strategies for limiting traffic get updated with global subscription metrics					
					SpringContextBeanForApi.getSpringContext().getBeansOfType(MessageBaseStrategy.class).values().forEach(ms -> ms.changeRateLimitIfRequired(total));
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("error updating subscription clustered counter");
				}
			}
		}, config.getMetricsIntervalSeconds(), config.getMetricsIntervalSeconds(), TimeUnit.SECONDS);

	}

	// asynchronous
	public void onDataMessage(CotEventContainer message) {
		countClusterMessageSent();
		if (message.hasContextKey(Constants.CLUSTER_MESSAGE_KEY)) {
			if (logger.isDebugEnabled()) {
				logger.debug("not clustering already clustered message");
			}
			
			return;
		}

		try {	
			message.setContext(Constants.NATS_MESSAGE_KEY, true);
			natsConnection.publish(Constants.CLUSTER_DATA_MESSAGE, clusterMessageConverter.cotToDataMessage(message));
		} catch (NotFoundException nfe) {
			// will be thrown by
			// clusterMessageConverter.cotEventContainerToClusterMessageJson if message has
			// no groups
			// this can occur when clients are in the process of disconnecting
			if (logger.isDebugEnabled()) {
				logger.debug("not trying to cluster message that contains no groups");
			}
		} catch (Exception e) {
			logger.error("exception publishing clustered data message", e);
		}
	}
	
	public void onPluginMessage(byte[] rawMessage) {
		try {
			natsConnection.publish(CommonConstants.CLUSTER_PLUGIN_SUBSCRIBE_TOPIC, rawMessage);
		} catch (Exception e) {
			logger.error("exception publishing clustered data message", e);
		}
	}

	public void onControlMessage(ROL clusterControl) {

		if (clusterControl == null || Strings.isNullOrEmpty(clusterControl.getProgram())) {
			throw new IllegalArgumentException("Null or empty cluster control message or program");
		}

		// interpret and execute the ROL program
		RolLexer lexer = new RolLexer(new ANTLRInputStream(clusterControl.getProgram()));

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		RolParser parser = new RolParser(tokens);
		parser.setErrorHandler(new BailErrorStrategy());

		// parse the ROL program
		ParseTree rolControlMessageParseTree = parser.program();

		requireNonNull(rolControlMessageParseTree, "parsed ROL control message");

		if (logger.isDebugEnabled()) {
			logger.debug("about to process ROL control message: " + clusterControl.getProgram());
		}
	}
	
	public void onAnnounceMissionChangeMessage(CotEventContainer changeMessage, String missionName, UUID missionGuid) {
		ClusterMissionAnnouncementDetail missionDetail = new ClusterMissionAnnouncementDetail();
		missionDetail.cot = changeMessage;
		missionDetail.missionAnnouncementType = ClusterMissionAnnouncementType.AnnounceMissionChange.name();
		missionDetail.missionName = missionName;
		missionDetail.missionGuid = missionGuid;
		publishMissionMessage(missionDetail);
	}
	
	public void onBroadcastMissionAnnouncementMessage(CotEventContainer message, String creatorUid, String groupVector) {
		ClusterMissionAnnouncementDetail missionDetail = new ClusterMissionAnnouncementDetail();
		missionDetail.cot = message;
		missionDetail.missionAnnouncementType = ClusterMissionAnnouncementType.BroadcastMissionAnnouncement.name();
		missionDetail.creatorUid = creatorUid;
		missionDetail.groupVector = groupVector;
		publishMissionMessage(missionDetail);
	}
	
	public void onSendMissionInviteMessage(CotEventContainer inviteMessage, String[] uids) {
		ClusterMissionAnnouncementDetail missionDetail = new ClusterMissionAnnouncementDetail();
		missionDetail.cot = inviteMessage;
		missionDetail.missionAnnouncementType = ClusterMissionAnnouncementType.SendMissionInvite.name();
		missionDetail.uids = uids;
		publishMissionMessage(missionDetail);
	}
	
	public void onSendMissionRoleChangeMessage(CotEventContainer roleChangeMessage, String clientUid) {
		ClusterMissionAnnouncementDetail missionDetail = new ClusterMissionAnnouncementDetail();
		missionDetail.cot = roleChangeMessage;
		missionDetail.missionAnnouncementType = ClusterMissionAnnouncementType.SendMissionRoleChange.name();
		missionDetail.clientUid = clientUid;
		publishMissionMessage(missionDetail);
	} 
	
	private void publishMissionMessage(ClusterMissionAnnouncementDetail missionDetail) {
		// convert to clustered message format, and publish the message to cluster
		try {
			missionDetail.cot.setContext(Constants.NATS_MESSAGE_KEY, true);
			natsConnection.publish(Constants.CLUSTER_MISSION_DATA_MESSAGE, clusterMessageConverter.missionAnnouncementToDataMessage(missionDetail));
		} catch (Exception e) {
			logger.error("exception publishing clustered mission message", e);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ClusterManager.applicationContext = applicationContext;
	}

	public static void addSubscription(final Subscription subscription) {
		subscriptionsTempCounter.incrementAndGet();
	}

	public static void removeSubscription(final Subscription subscription) {
		subscriptionsTempCounter.decrementAndGet();
	}
	
	public long getSubscriptionCount() {
		return getSubscriptionCounter().get();
	}
	
	private IgniteAtomicLong getSubscriptionCounter() {
		return ignite.atomicLong(SUBSCRIPTION_COUNTER, 0, true);
	}
	
	public static void countMessageReceived() {
		messagesReceivedTempCounter.incrementAndGet();
	}

	public static void countClusterMessageRecieved() {
		clusterMessagesReceivedTempCounter.incrementAndGet();
	}

	public static void countClusterMessageSent() {
		clusterMessagesSentTempCounter.incrementAndGet();
	}

	public static void countMessageSent() {
		messagesSentTempCounter.incrementAndGet();
	}

	public long getMessagesReceivedCount() {
		return getMessagesReceivedCounter().get();
	}

	public long getClusterMessagesSentCount() {
		return getClusterMessagesSentCounter().get();
	}

	public long getClusterMessagesReceivedCount() {
		return getClusterMessagesReceivedCounter().get();
	}

	public long getMessagesSentCount() {
		return getMessagesSentCounter().get();
	}

	private IgniteAtomicLong getMessagesReceivedCounter() {
		return ignite.atomicLong(MESSAGES_RECEIVED_COUNTER, 0, true);
	}

	private IgniteAtomicLong getClusterMessagesSentCounter() {
		return ignite.atomicLong(CLUSTER_MESSAGES_SENT_COUNTER, 0, true);
	}

	private IgniteAtomicLong getClusterMessagesReceivedCounter() {
		return ignite.atomicLong(CLUSTER_MESSAGES_RECEIVED_COUNTER, 0, true);
	}

	private IgniteAtomicLong getMessagesSentCounter() {
		return ignite.atomicLong(MESSAGES_SENT_COUNTER, 0, true);
	}
	
	public static enum ClusterMissionAnnouncementType {
		AnnounceMissionChange, BroadcastMissionAnnouncement, SendMissionInvite, SendMissionRoleChange;
	}
	
	public static class ClusterMissionAnnouncementDetail {
		public CotEventContainer cot;
		public String missionAnnouncementType;
		public String missionName;
		public UUID missionGuid;
		public String groupVector;
		public String creatorUid;
		public String clientUid;
		public String[] uids;
	}
}
