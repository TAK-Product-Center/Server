package tak.server.plugins;

import org.springframework.beans.factory.annotation.Autowired;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.messaging.Messenger;
import tak.server.plugins.messaging.MessageConverter;

public abstract class MessageInterceptorBase extends PluginBase implements MessageInterceptor {
//    private static final Logger logger = LoggerFactory.getLogger(MessageReceiverBase.class);

    @Autowired
    private Messenger<Message> messenger;

    @Autowired
    private MessageConverter converter;

    private PluginInfo pluginInfo;

    MessageInterceptorBase() throws ReservedConfigurationException {
        super();
    }

    protected MessageConverter getConverter() {
        return converter;
    }

    @Override
    public Message intercept(Message message) {
    	return message;
    }
    
    @Override
	public void send(Message message) {
		if (pluginInfo.isEnabled()) {
			// send the message to TAK Server
			messenger.send(message);
		}
	}

    @Override
    public final void internalStart() {
        start();
        pluginInfo.setEnabled(true);
    }

    @Override
    public final void internalStop() {
    	stop();
    	pluginInfo.setEnabled(false);
    }

    @Override
    public final PluginInfo getPluginInfo() {
        return pluginInfo;
    }

    @Override
    public final void setPluginInfo(PluginInfo pluginInfo) {
        this.pluginInfo = pluginInfo;
    }
}