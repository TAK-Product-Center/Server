package tak.server.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;

public abstract class MessageReceiverBase extends PluginBase implements MessageReceiver {
	
	private static final Logger logger = LoggerFactory.getLogger(MessageReceiverBase.class);
	
    public MessageReceiverBase() throws ReservedConfigurationException {
        super();
    }

}
