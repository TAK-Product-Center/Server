package tak.server.messaging;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

public abstract class IgniteTopicBasedMessageProcessor<T> implements MessageProcessor<T> {
	
	protected final String topic;
	
	protected final Ignite ignite;
	
	protected final Class<T> type;
	
	protected final Logger logger = LoggerFactory.getLogger(IgniteTopicBasedMessageProcessor.class);
	
	public IgniteTopicBasedMessageProcessor(Ignite ignite, final String topic, final Class<T> type) {
		this.ignite = ignite;
		this.topic = topic;
		this.type = type;
	}
	
	@SuppressWarnings("unchecked") // type is checked below by isInstance
	@EventListener({ContextRefreshedEvent.class})
	public void init() {
		
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " init");
		}
		
		// listen for messages
        ignite.message().localListen(topic, (nodeId, message) -> {
        	
        	if (logger.isTraceEnabled()) {
        		logger.trace(getClass().getSimpleName() + " received message " + message);
        	}
        	
        	if (!(type.isInstance(message))) {
        		
        		if (logger.isDebugEnabled()) {
        			logger.debug("ignoring unsupported message type " + message.getClass().getName());
        		}
        		
        		return true;
        	}
        	
        	process((T) message);
        	
        	// return true to continue listening
        	return true;
        });		
	}
}
