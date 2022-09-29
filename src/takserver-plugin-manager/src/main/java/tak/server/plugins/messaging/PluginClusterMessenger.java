package tak.server.plugins.messaging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import io.nats.client.Connection;
import io.nats.client.Nats;
import tak.server.CommonConstants;

public class PluginClusterMessenger extends PluginMessenger {
	private static final Logger logger = LoggerFactory.getLogger(PluginClusterMessenger.class);
	
	private Connection natsConnection;
	
	public PluginClusterMessenger(String natsURL) {
		try {
			natsConnection = Nats.connect(natsURL);
		} catch (IOException | InterruptedException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("error establishing NATS connection", e);
			}
		}
	}
	
	@Override
	public void networkSend(Message message) {
		try {
			natsConnection.publish(CommonConstants.CLUSTER_PLUGIN_PUBLISH_TOPIC, message.toByteArray());
		} catch (Exception e) {
			logger.error("exception publishing clustered data message", e);
		} 
	}

}
