package tak.server.messaging;


import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.socket.TakMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tak.server.Constants;

public final class DistributedTakMessenger implements Messenger<TakMessage> {
	
	public DistributedTakMessenger(Ignite ignite, ServerInfo serverInfo) {
		this.ignite = ignite;
		this.serverInfo = serverInfo;
		
		serializer = new ObjectMapper();
	}
	
	private static final Logger logger = LoggerFactory.getLogger(DistributedTakMessenger.class);
	
	private final Ignite ignite;
	
	private final ServerInfo serverInfo;
	
	@Autowired
	@Qualifier(Constants.TAKMESSAGE_MAPPER)
	private ObjectMapper serializer;
	
	@Override
	public void send(TakMessage message) {
		
		if (logger.isTraceEnabled()) {
			logger.trace("sending TakMessage " + message);
		}

		try {
			String jsonMessage = serializer.writeValueAsString(message);

			ignite.message().send(serverInfo.getTakMessageTopic(), jsonMessage);

			if (logger.isTraceEnabled()) {
				logger.trace("sent serialized TakMessage " + jsonMessage);
			}
		} catch (JsonProcessingException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception serializing TakMessage ", e);
			}
		}
	}
}
