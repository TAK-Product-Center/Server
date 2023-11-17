package com.bbn.marti.service;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

import com.bbn.marti.nio.protocol.connections.StreamingProtoBufProtocol;
import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;

import gov.tak.cop.proto.v1.Binarypayload;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;
import tak.server.messaging.MessageConverter;

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
	
	public static void brokerWebSocketMessage(Set<String> websocketConnectionIds, CotEventContainer data, String serverId) {

		if (data.getBinaryPayloads() != null && !data.getBinaryPayloads().isEmpty()) {
			Message messageV3 = MessageConverter.cotToMessage(data, false, serverId);
			ByteBuffer message = StreamingProtoBufProtocol.convertGeneratedMessageV3ToProtoBufBytes(messageV3);

			IgniteHolder.getInstance()
					.getIgnite()
					.message(IgniteHolder.getInstance().getIgnite().cluster().forClients().forAttribute(Constants.TAK_PROFILE_KEY, Constants.API_PROFILE_NAME))
					.send("websocket-payload-write-listener", new WebsocketMessageTransporter(websocketConnectionIds, message.array(), data.getUid(), data.getType()));
		} else {
			ByteBuffer message = StreamingProtoBufProtocol.convertCotToProtoBufBytes(data);

			IgniteHolder.getInstance()
					.getIgnite()
					.message(IgniteHolder.getInstance().getIgnite().cluster().forClients().forAttribute(Constants.TAK_PROFILE_KEY, Constants.API_PROFILE_NAME))
					.send("websocket-write-listener", new WebsocketMessageTransporter(websocketConnectionIds, message.array(), data.getUid(), data.getType()));
		}
	}
	
	public static class WebsocketMessageTransporter {
		public Set<String> websocketConnectionIds;
		public byte[] message;
		public String publisherId;
		public String messageType;
		
		public WebsocketMessageTransporter(Set<String> websocketConnectionIds, byte[] message, String messageType, String publisherId) {
			this.websocketConnectionIds = websocketConnectionIds;
			this.message = message;
			this.messageType = messageType;
			this.publisherId = publisherId;
		}
	}

}
