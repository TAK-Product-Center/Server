package com.bbn.marti.service;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;

import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufProtocol;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;
import tak.server.messaging.MessageConverter;

/**
 */
public class WebsocketMessagingBroker {
	
	public static void brokerTargetedWebSocketMessage(Set<String> websocketConnectionIds, CotEventContainer data, UUID websocketApiId) {
		Resources.messageSendExecutor.submit(() -> {
			ByteBuffer message = StreamingProtoBufProtocol.convertCotToProtoBufBytes(data, true);

			BinaryObject binaryMsg = buildBinaryObject(websocketConnectionIds, message.array(), data.getUid(),
					data.getType());

			IgniteHolder.getInstance()
				.getIgnite()
				.message(IgniteHolder.getInstance().getIgnite().cluster().forNodeId(websocketApiId))
				.send("websocket-write-listener", binaryMsg);
		});
	}
	
	public static void brokerWebSocketMessage(Set<String> websocketConnectionIds, CotEventContainer data, String serverId) {
		Resources.messageSendExecutor.submit(() -> {
			if (data.getProtoBufBytes() != null) {
				AbstractBroadcastingChannelHandler.totalBytesWritten.getAndAdd(data.getProtoBufBytes().remaining() * websocketConnectionIds.size());
			}
			
			AbstractBroadcastingChannelHandler.totalNumberOfWrites.getAndAdd(websocketConnectionIds.size());
			
			if (data.getBinaryPayloads() != null && !data.getBinaryPayloads().isEmpty()) {
				Message messageV3 = MessageConverter.cotToMessage(data, false, serverId);
				ByteBuffer message = StreamingProtoBufProtocol.convertGeneratedMessageV3ToProtoBufBytes(messageV3);

				BinaryObject binaryMsg = buildBinaryObject(websocketConnectionIds, message.array(), data.getUid(),
						data.getType());

				IgniteHolder.getInstance()
						.getIgnite()
						.message(IgniteHolder.getInstance().getIgnite().cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.API_PROFILE_NAME))
						.send("websocket-payload-write-listener", binaryMsg);
			} else {
				ByteBuffer message = StreamingProtoBufProtocol.convertCotToProtoBufBytes(data, true);

				BinaryObject binaryMsg = buildBinaryObject(websocketConnectionIds, message.array(), data.getUid(),
						data.getType());

				IgniteHolder.getInstance()
						.getIgnite()
						.message(IgniteHolder.getInstance().getIgnite().cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.API_PROFILE_NAME))
						.send("websocket-write-listener", binaryMsg);
			}
		});
	}
	
	private static BinaryObject buildBinaryObject(Set<String> websocketConnectionIds, byte[] payload, String publisherId, String messageType) {
		BinaryObjectBuilder builder = IgniteHolder.getInstance().getIgnite().binary().builder("WebsocketMessage");
		builder.setField("connectionIds", websocketConnectionIds.toArray(new String[0]));
		builder.setField("payload", payload);
		builder.setField("publisherId", publisherId);
		builder.setField("messageType", messageType);

		BinaryObject binaryMsg = builder.build();
		
		return binaryMsg;
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
