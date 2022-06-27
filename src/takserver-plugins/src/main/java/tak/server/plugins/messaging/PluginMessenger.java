package tak.server.plugins.messaging;

import org.apache.ignite.Ignite;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import atakmap.commoncommo.protobuf.v1.DetailOuterClass.Detail;
import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.CommonConstants;
import tak.server.messaging.Messenger;

public class PluginMessenger implements Messenger<Message> {

	private static final String ANON_GROUP = "__ANON__";

	private static final Logger logger = LoggerFactory.getLogger(PluginMessenger.class);

	public PluginMessenger() {
		logger.info("create PluginMessenger");
	}

	@Autowired
	private Ignite ignite;

	@Autowired
	private MessageConverter converter;

	@EventListener(ContextRefreshedEvent.class)
	private void init() {
		logger.info("starting PluginMessenger");
	}

	@Override
	public void send(Message message) {

		// if no groups specified, use anon group
		if (message.getGroupsCount() == 0) {
			Message.Builder mb = message.toBuilder();

			mb.addGroups(ANON_GROUP);
			message = mb.build();
		}

		// if callsigns or uids are specified, set them in the message 
		if (message.getDestCallsignsCount() != 0 || message.getDestClientUidsCount() != 0) {
			Message.Builder mb = message.toBuilder(); 

			Detail.Builder db = mb.getPayloadBuilder().getCotEventBuilder().getDetailBuilder();
			
			try {
				
				Document detailDoc = converter.replaceXmlDetailDestCallsignsAndUids(db.getXmlDetail(), message.getDestCallsignsList(), message.getDestClientUidsList());
				
				if (logger.isDebugEnabled()) {
					logger.debug("updated detail: " + detailDoc.asXML());
				}
				
				if (logger.isDebugEnabled()) {
					logger.debug("updated detail text: " + detailDoc.getText());
				}
				
				StringBuilder detailContents = new StringBuilder();
				
				for (Element el : detailDoc.getRootElement().elements()) {
					detailContents.append(el.asXML());
				}
				
				if (logger.isDebugEnabled()) {
					logger.debug("updated detail contents: " + detailContents.toString());
				}
				
				String updatedContents = detailContents.toString();
				
				db.setXmlDetail(updatedContents);
				
				// replace detail contents with updates
				message = mb.build();
					
				} catch (DocumentException e) {
					logger.warn("exception parsing detail tag in message - skipping", e);
					return;
				}
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("sending message: " + message);
		}

		networkSend(message);

	}
	
	public void networkSend(Message message) {
		
		if (logger.isTraceEnabled()) {
			logger.trace("send plugin message to ignite " + CommonConstants.PLUGIN_PUBLISH_TOPIC + " " + message);
		}
			
		ignite.message().send(CommonConstants.PLUGIN_PUBLISH_TOPIC, message.toByteArray());
	}
}