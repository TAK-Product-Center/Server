package com.bbn.marti.nio.websockets;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


import com.bbn.marti.remote.groups.Direction;
import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.tomcat.websocket.WsSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.WebsocketMessagingBroker.WebsocketMessageTransporter;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

import io.micrometer.core.instrument.Metrics;
import tak.server.Constants;
import tak.server.ignite.IgniteHolder;
import tak.server.qos.MessageDeliveryStrategy;

public class TakProtoWebSocketHandler extends BinaryWebSocketHandler {

	private static final Logger logger = LoggerFactory.getLogger(TakProtoWebSocketHandler.class);
	private ConcurrentHashMap<String, WebSocketSession> websocketMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, String> websocketMessagingMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, String> websocketGroupVectorMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, String> websocketSessionIdMap = new ConcurrentHashMap<>();

	private GroupManager groupManager = null;
	
	private GroupManager groupManager() {
		if (groupManager == null ) {
			groupManager = (GroupManager) SpringContextBeanForApi.getSpringContext().getBean("groupManager");
		}
		return groupManager;
	}
	
	private MessageDeliveryStrategy mds = null;
	
	private MessageDeliveryStrategy mds() {
		if (mds == null ) {
			mds = (MessageDeliveryStrategy) SpringContextBeanForApi.getSpringContext().getBean("mds");
		}
		return mds;
	}
		
	public TakProtoWebSocketHandler() {

		if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			setupIgniteListeners();
		}
		
		Resources.tcpProcessor.execute(() -> {
			ignite().message(
				ignite().cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME))	
					.localListen("websocket-write-listener", (nodeId, message) -> {
						if (message instanceof WebsocketMessageTransporter) {
							final WebsocketMessageTransporter wmt = (WebsocketMessageTransporter) message;
							final BinaryMessage binaryMessage = new BinaryMessage(wmt.message);
							
							Resources.messageSendExecutor.submit(
									() -> {
										((WebsocketMessageTransporter) message).websocketConnectionIds.parallelStream().forEach(id -> {
											try {
												WebSocketSession session = websocketMap.get(id);
												
												if (session != null && binaryMessage != null) {
													
													if (mds().isAllowed(wmt.messageType, wmt.publisherId, id)) {
														session.sendMessage(binaryMessage);
														Metrics.counter(Constants.METRIC_MESSAGE_WRITE_COUNT, "takserver", "messaging").increment();
													}
												}
											} catch (Exception e) {
												if (logger.isDebugEnabled()) {
													logger.debug("Error submitting message to websocket via ignite write listener", e);
												}
											}
										});
									});
						}

						return true;
					});
		});
	}

	@Override
	public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {

		try {		
			Metrics.counter(Constants.METRIC_MESSAGE_READ_COUNT, "takserver", "messaging").increment();
			
			String inVector = RemoteUtil.getInstance()
					.bitVectorToString(RemoteUtil.getInstance()
							.getBitVectorForGroups(groupManager().getGroupsByConnectionId(websocketSessionIdMap.get(hashCode(session))), Direction.IN));

			String outVector = RemoteUtil.getInstance()
					.bitVectorToString(RemoteUtil.getInstance()
							.getBitVectorForGroups(groupManager().getGroupsByConnectionId(websocketSessionIdMap.get(hashCode(session))), Direction.OUT));

			if (!websocketGroupVectorMap.get(hashCode(session)).equals(inVector + outVector)) {
				ClusterGroup clusterGroup = ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite());
				clusterGroup = clusterGroup.forNodeId(UUID.fromString(websocketMessagingMap.get(hashCode(session))));

				ignite().services(clusterGroup)
						.serviceProxy(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, SubscriptionManager.class, false)
						.updateWebsocketSubscription(inVector, outVector, hashCode(session));
				
				websocketGroupVectorMap.put(hashCode(session), inVector + outVector);
			}
			
			ignite().message(
					ignite().cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MESSAGING_PROFILE_NAME))
					.send("websocket-read-listener-" + hashCode(session), message.getPayload().array());

		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("handleBinaryMessage ERROR " + e);
			}
			throw e;
		}
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {

		try {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("Error closing connection", e);
			}
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

		if (logger.isTraceEnabled()) {
			logger.trace("afterConnectionEstablished " + hashCode(session));
		}
		ConcurrentWebSocketSessionDecorator concurrentSession = new ConcurrentWebSocketSessionDecorator(session, 
				DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketSendTimeoutMs(), 
				DistributedConfiguration.getInstance().getRemoteConfiguration().getBuffer().getQueue().getWebsocketSendBufferSizeLimit(),
				OverflowStrategy.DROP);
		
		websocketMap.put(hashCode(session), concurrentSession);

		try {
			InetSocketAddress local = session.getLocalAddress();
			InetSocketAddress remote = session.getRemoteAddress();
			String sessionId = ((WsSession) ((StandardWebSocketSession) session).getNativeSession()).getHttpSessionId();
			websocketSessionIdMap.put(hashCode(session), sessionId);

			String inVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groupManager().getGroupsByConnectionId(sessionId), Direction.IN));
			String outVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groupManager().getGroupsByConnectionId(sessionId), Direction.OUT));

			websocketGroupVectorMap.put(hashCode(session), inVector + outVector);

			String handlingNode = SpringContextBeanForApi.getSpringContext()
					.getBean(SubscriptionManager.class)
					.createWebsocketSubscription(ignite().cluster().localNode().id(), 
							groupManager().getUserByConnectionId(sessionId),
							inVector, outVector, sessionId, hashCode(session), local, remote);
			
			if (handlingNode == null) {
				session.close();
				return;
			}
			
			websocketMessagingMap.put(hashCode(session), handlingNode);

		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("afterConnectionEstablished ERROR " + e);
			}
			throw e;
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
			ClusterGroup clusterGroup = ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite());
			clusterGroup = clusterGroup.forNodeId(UUID.fromString(websocketMessagingMap.get(hashcode)));

			ignite().services(clusterGroup)
					.serviceProxy(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, SubscriptionManager.class, false)
					.removeWebsocketSubscription(hashcode);
		} catch (Exception e) {
			logger.debug("Could not find messaging node that created websocket subscription");
		}

		try {
			websocketMap.get(hashcode).close();
		} catch (Exception e) {
			logger.debug("Could not find and close websocket session");
		}

		websocketMap.remove(hashcode);
		websocketMessagingMap.remove(hashcode);
	}

	Ignite ignite = null;

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

		IgniteHolder.getInstance()
				.getIgnite()
				.events()
				.localListen(ignitePredicate, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED);
	}
}
