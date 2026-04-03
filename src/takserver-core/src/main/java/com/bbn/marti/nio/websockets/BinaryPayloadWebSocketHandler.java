package com.bbn.marti.nio.websockets;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.core.instrument.Metrics;

import org.apache.ignite.Ignite;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.tomcat.websocket.WsSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.WebsocketMessagingBroker.WebsocketMessageTransporter;
import com.bbn.marti.remote.util.SpringContextBeanForApi;

import tak.server.Constants;
import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.ignite.IgniteHolder;
import tak.server.ignite.IgniteReconnectEventHandler;
import tak.server.ignite.grid.SubscriptionManagerProxyHandler;
import tak.server.qos.MessageDeliveryStrategy;

public class BinaryPayloadWebSocketHandler extends BinaryWebSocketHandler {

	private static final Logger logger = LoggerFactory.getLogger(BinaryPayloadWebSocketHandler.class);

	private ConcurrentHashMap<String, WebSocketSession> websocketMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, String> websocketMessagingMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, String> websocketGroupVectorMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, String> websocketSessionIdMap = new ConcurrentHashMap<>();

	private MessageDeliveryStrategy mds = null;

	private MessageDeliveryStrategy mds() {
		if (mds == null) {
			mds = (MessageDeliveryStrategy) SpringContextBeanForApi.getSpringContext().getBean("mds");
		}
		return mds;
	}

	private SubscriptionManagerProxyHandler subscriptionManagerProxy = null;

	private SubscriptionManagerProxyHandler smp() {
		if (subscriptionManagerProxy == null) {
			subscriptionManagerProxy = (SubscriptionManagerProxyHandler) SpringContextBeanForApi.getSpringContext()
					.getBean(SubscriptionManagerProxyHandler.class);
		}
		return subscriptionManagerProxy;
	}

	public BinaryPayloadWebSocketHandler() {

		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			setupIgniteListeners();
		}

		IgniteReconnectEventHandler.registerListener(() -> {
			Resources.tcpProcessor.execute(() -> {
				ignite().message(
						ignite().cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME))
						.localListen("websocket-payload-write-listener", (nodeId, message) -> {
							Resources.messageSendExecutor.submit(() -> {
								try {
									if (message instanceof BinaryObject obj) {
										byte[] payload = obj.field("payload");
										String publisherId = obj.field("publisherId");
										String messageType = obj.field("messageType");
										String[] connectionIds = obj.field("connectionIds");

										for (String id : connectionIds) {
											WebSocketSession session = websocketMap.get(id);
											if (session == null) continue;
											
											if (!mds().isAllowed(messageType, publisherId, id)) continue;

											try {
												session.sendMessage(new BinaryMessage(payload));
												Metrics.counter(Constants.METRIC_MESSAGE_WRITE_COUNT, "takserver", "api").increment();
											} catch (Exception e) {
												if (logger.isDebugEnabled()) {
													logger.debug("Error submitting message to websocket via ignite write listener", e);
												}
											}
										}
									}
								} catch (Exception e) {
									logger.error("Error handling websocket message from messaging node", e);
								}
							});

							return true;
						});
			});
		});
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

		String clientUid = (String) session.getAttributes().get("clientUid");

		if (logger.isTraceEnabled()) {
			logger.trace("afterConnectionEstablished " + hashCode(session));
		}

		ConcurrentWebSocketSessionDecorator concurrentSession = new ConcurrentWebSocketSessionDecorator(session,
				CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketSendTimeoutMs(),
				CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketSendBufferSizeLimit(),
				OverflowStrategy.DROP);

		websocketMap.put(hashCode(session), concurrentSession);

		Resources.tcpProcessor.execute(() -> {
			try {

				String sessionId = ((WsSession) ((StandardWebSocketSession) session).getNativeSession()).getHttpSessionId();

				websocketSessionIdMap.put(hashCode(session), sessionId);

				// this is blocking but has a time limit and is only called once per connection
				SubscriptionManagerLite subMgr = smp().getSubscriptionManagerForClientUid(clientUid).get();

				if (subMgr == null) {
					session.close();
					return;
				}

				String handlingNode = subMgr.linkWebsocketToExistingSub(hashCode(session), clientUid,
						session.getPrincipal().getName());

				if (handlingNode == null) {
					session.close();
					return;
				}

				websocketMessagingMap.put(hashCode(session), handlingNode);

			} catch (Exception e) {
				if (logger.isTraceEnabled()) {
					logger.trace("afterConnectionEstablished ERROR " + e);
				}
				forceClose(session);
			}
		});
	}

	private void forceClose(WebSocketSession session) {
		try {
			session.close();
		} catch (IOException e) {
			try {
				removeWebSocketSubscription(session);
			} catch (Exception e1) {
				logger.error("Error force closing websocket", e);
			}
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

		if (logger.isTraceEnabled()) {
			logger.trace("handleTransportError" + hashCode(session));
		}
		removeWebSocketSubscription(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

		if (logger.isTraceEnabled()) {
			logger.trace("afterConnectionClosed " + hashCode(session));
		}

		removeWebSocketSubscription(session);
	}

	private void removeWebSocketSubscription(WebSocketSession session) throws Exception {
		removeWebSocketSubscription(hashCode(session));
	}

	private void removeWebSocketSubscription(String hashcode) {

		try {
			ClusterGroup clusterGroup = ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite())
					.forNodeId(UUID.fromString(websocketMessagingMap.get(hashcode)));

			ignite().services(clusterGroup)
					.serviceProxy(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, SubscriptionManager.class, false)
					.unlinkWebsocketExistingSub(hashcode,
							(String) websocketMap.get(hashcode).getAttributes().get("clientUid"));

		} catch (Exception e) {
			logger.debug("Could not find messaging node that created websocket subscription");
		}

		try {
			WebSocketSession session = websocketMap.get(hashcode);
			if (session != null) {
				session.close();
			}
		} catch (Exception e) {
			logger.debug("Could not find and close websocket session");
		}

		websocketMap.remove(hashcode);
		websocketMessagingMap.remove(hashcode);
		websocketGroupVectorMap.remove(hashcode);
		websocketSessionIdMap.remove(hashcode);
	}

	private Ignite ignite = null;

	private Ignite ignite() {
		if (ignite == null) {
			ignite = IgniteHolder.getInstance().getIgnite();
		}
		return ignite;
	}

	private String hashCode(WebSocketSession session) {
		return IgniteHolder.getInstance().getIgniteStringId() + String.valueOf(session.hashCode());
	}

	private void setupIgniteListeners() {

		// we need to recreate websocket subscriptions that were created on a messaging
		// node that went down
		IgnitePredicate<DiscoveryEvent> ignitePredicate = new IgnitePredicate<DiscoveryEvent>() {
			@Override
			public boolean apply(DiscoveryEvent event) {
				// only react to api nodes, this will only ever be called in the cluster
				if (Constants.MESSAGING_PROFILE_NAME.equals(event.eventNode().attribute(Constants.TAK_PROFILE_KEY))) {

					websocketMessagingMap.entrySet().parallelStream().forEach(e -> {
						if (UUID.fromString(e.getValue()).equals(event.eventNode().id())) {
							removeWebSocketSubscription(e.getKey());
						}
					});
				}
				return true;
			}
		};

		IgniteHolder.getInstance().getIgnite().events().localListen(ignitePredicate, EventType.EVT_NODE_LEFT,
				EventType.EVT_NODE_FAILED);
	}
}
