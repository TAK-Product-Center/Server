package tak.server.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.Constants;
import tak.server.messaging.Messenger;
import tak.server.plugins.messaging.MessageConverter;

public abstract class MessageSenderReceiverBase extends PluginBase implements MessageSenderReceiver {
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiverBase.class);

    @Autowired
    private Messenger<Message> messenger;

    @Autowired
    private MessageConverter converter;
    
    @Autowired
    private PluginDataFeedApi pluginDataFeedApi;

    public MessageSenderReceiverBase() throws ReservedConfigurationException {
        super();
    }

    protected MessageConverter getConverter() {
        return converter;
    }
    
    protected PluginDataFeedApi getPluginDataFeedApi() {
		return pluginDataFeedApi;
	}

    @Override
    public void send(Message message) {
    	
        if (pluginInfo.isEnabled()) {
        	
			Message.Builder mb = message.toBuilder();
			mb.setArchive(pluginInfo.isArchiveEnabled());
			mb.addProvenance(Constants.PLUGIN_MANAGER_PROVENANCE);
			message = mb.build();
			
            // send the message to TAK Server
            messenger.send(message);
        }
    }
    
    @Override
    public void send(Message message, String feedUuid) {
    	
        if (pluginInfo.isEnabled()) {
        	
			Message.Builder mb = message.toBuilder();
			mb.setArchive(pluginInfo.isArchiveEnabled());
			mb.setFeedUuid(feedUuid);
			mb.addProvenance(Constants.PLUGIN_MANAGER_PROVENANCE);
			message = mb.build();
			
            // send the message to TAK Server
            messenger.send(message);
        }
    }

}