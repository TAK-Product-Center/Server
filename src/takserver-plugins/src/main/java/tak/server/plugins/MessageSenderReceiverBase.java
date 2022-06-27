package tak.server.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.messaging.Messenger;
import tak.server.plugins.messaging.MessageConverter;

public abstract class MessageSenderReceiverBase extends PluginBase implements MessageSenderReceiver {
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiverBase.class);

    @Autowired
    private Messenger<Message> messenger;

    @Autowired
    private MessageConverter converter;

    private PluginInfo pluginInfo;

    MessageSenderReceiverBase() throws ReservedConfigurationException {
        super();
    }

    protected MessageConverter getConverter() {
        return converter;
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
        try {
            stop();
            pluginInfo.setEnabled(false);
        } catch (AbstractMethodError e) {
            logger.info("Recompile plugin " + getPluginInfo().getName() + " with TAK Server Plugin SDK version 4.2 or higher to support the stop method.");
        }
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