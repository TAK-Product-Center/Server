package tak.server.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.antlr.v4.parse.GrammarTreeVisitor.locals_return;
import org.springframework.beans.factory.annotation.Autowired;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.Constants;
import tak.server.messaging.Messenger;
import tak.server.plugins.messaging.MessageConverter;

public abstract class MessageInterceptorBase extends PluginBase implements MessageInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiverBase.class);

    @Autowired
    private Messenger<Message> messenger;

    @Autowired
    private MessageConverter converter;
    
    @Autowired
    private PluginDataFeedApi pluginDataFeedApi;

    MessageInterceptorBase() throws ReservedConfigurationException {
        super();
    }

    protected MessageConverter getConverter() {
        return converter;
    }

    protected PluginDataFeedApi getPluginDataFeedApi() {
		return pluginDataFeedApi;
	}
    
    @Override
    public Message intercept(Message message) {
    	return message;
    }
    
    @Override
	public void send(Message message) {
    	logger.info("send is NO-OP for interceptors. use the intercept method.");
    }
    
    @Override
	public void send(Message message, String feedUuid) {
    	logger.info("send is NO-OP for interceptors. use the intercept method.");
    }

}