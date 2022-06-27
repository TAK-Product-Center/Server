package com.bbn.marti.service;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

import com.bbn.marti.nio.protocol.connections.StreamingProtoBufProtocol;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;

/**
 */
public class WebsocketMessagingBroker {
	
	public static void brokerTargetedWebSocketMessage(Set<String> websocketConnectionIds, CotEventContainer data, UUID websocketApiId) {
		ByteBuffer message = StreamingProtoBufProtocol.convertCotToProtoBufBytes(data);
		
		IgniteHolder.getInstance()
			.getIgnite()
			.message(IgniteHolder.getInstance().getIgnite().cluster().forNodeId(websocketApiId))
			.send("websocket-write-listener", new WebsocketMessageTransporter(websocketConnectionIds, message.array(), data.getUid(), data.getType()));
	}
	
	public static void brokerWebSocketMessage(Set<String> websocketConnectionIds, CotEventContainer data) {
		ByteBuffer message = StreamingProtoBufProtocol.convertCotToProtoBufBytes(data);
		
		IgniteHolder.getInstance()
			.getIgnite()
			.message(IgniteHolder.getInstance().getIgnite().cluster().forClients().forAttribute(Constants.TAK_PROFILE_KEY, Constants.API_PROFILE_NAME))
			.send("websocket-write-listener", new WebsocketMessageTransporter(websocketConnectionIds, message.array(), data.getUid(), data.getType()));
	}
	
	public static class WebsocketMessageTransporter {
		public Set<String> websocketConnectionIds;
		public byte[] cot;
		public String publisherId;
		public String messageType;
		
		public WebsocketMessageTransporter(Set<String> websocketConnectionIds, byte[] cot, String messageType, String publisherId) {
			this.websocketConnectionIds = websocketConnectionIds;
			this.cot = cot;
			this.messageType = messageType;
			this.publisherId = publisherId;
		}
	}

}
