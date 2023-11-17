package tak.server.messaging;


import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.service.PluginStore;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import org.apache.ignite.Ignite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.CommonConstants;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;

public class DistributedCotMessenger implements Messenger<CotEventContainer> {

	private boolean isPlugins = false;
	private boolean isCluster = false;

	public DistributedCotMessenger(Ignite ignite, SubscriptionStore subscriptionStore, ServerInfo serverInfo, MessageConverter messageConverter, SubmissionService submissionService, CoreConfig config) {
		this.ignite = ignite;
		this.subscriptionStore = subscriptionStore;
		this.serverInfo = serverInfo;
		this.messageConverter = messageConverter;
		this.submissionService = submissionService;
		this.config = config;
		
		this.isCluster = config.getRemoteConfiguration().getCluster() != null && config.getRemoteConfiguration().getCluster().isEnabled();
		this.isPlugins = config.getRemoteConfiguration().getPlugins().isUsePluginMessageQueue();
	}
	
	private final Ignite ignite;
	
	@SuppressWarnings("unused")
	private final SubscriptionStore subscriptionStore;
	
	@SuppressWarnings("unused")
	private final ServerInfo serverInfo;
	
	private final MessageConverter messageConverter;
	
	private final SubmissionService submissionService;
	
	@SuppressWarnings("unused")
	private final CoreConfig config;
	
	private static final Logger logger = LoggerFactory.getLogger(DistributedCotMessenger.class);

	@Override
	public void send(CotEventContainer message) {
		if (logger.isTraceEnabled()) {
			logger.trace("sending message " + message);
		}

		boolean isControlMessage =
				submissionService.isControlMessage(message.getType());
		if (isControlMessage) {
			if (logger.isDebugEnabled()) {
				logger.debug("Adding control message to SubmissionService InputQueue.");
			}
			submissionService.addToInputQueue(message);
			return;
		}

		if (PluginStore.getInstance().getInterceptorPluginsActive() == 0 || !isPlugins) {
			if (logger.isDebugEnabled()) {
				logger.debug("Adding non-plugin message to SubmissionService InputQueue.");
			}
			submissionService.addToInputQueue(message);
		}

		// push the message to the plugin queue, if plugins and plugin message queue are enabled
		if (isPlugins) {
			try {
				byte[] rawMessage = messageConverter.cotToDataMessage(new CotEventContainer(message, true, ImmutableSet.of(Constants.SOURCE_TRANSPORT_KEY, Constants.SOURCE_PROTOCOL_KEY, Constants.USER_KEY)), true);

				if (isCluster) {
					MessagingDependencyInjectionProxy.getInstance().clusterManager().onPluginMessage(rawMessage);
				} else {
					// does not include the handler as this will be consumed by plugins in another process
					if (logger.isDebugEnabled()) {
						logger.debug("Sending the message over Ignite to the Plugin Subscribe Topic");
					}
					ignite.message().send(CommonConstants.PLUGIN_SUBSCRIBE_TOPIC, rawMessage);
				}
				
				if (logger.isTraceEnabled()) {
					logger.trace("sent binary message of size " + (rawMessage == null ? "null" : rawMessage.length) + " bytes to plugin subscribe topic " + CommonConstants.PLUGIN_SUBSCRIBE_TOPIC);
				}
			} catch (Exception e) {
				if (logger.isTraceEnabled()) {
					logger.trace("exception sending message to plugin subscribe topic " + CommonConstants.PLUGIN_SUBSCRIBE_TOPIC, e);
				}
			}
		}
	}
}
