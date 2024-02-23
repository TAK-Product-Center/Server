package tak.server.messaging;

import com.bbn.marti.config.Configuration;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.SubscriptionStore;
import com.google.common.collect.ImmutableSet;

import tak.server.CommonConstants;
import tak.server.Constants;
import tak.server.cluster.ClusterManager;
import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.cot.CotEventContainer;

public class DistributedCotMessengerForApi implements Messenger<CotEventContainer> {

	public DistributedCotMessengerForApi(Ignite ignite, SubscriptionStore subscriptionStore, ServerInfo serverInfo, MessageConverter messageConverter) {
		this.ignite = ignite;
		this.subscriptionStore = subscriptionStore;
		this.serverInfo = serverInfo;
		this.messageConverter = messageConverter;

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

		this.isCluster = config.getCluster() != null && config.getCluster().isEnabled();
		this.isPlugins = config.getPlugins().isUsePluginMessageQueue();
	}

	private final Ignite ignite;
	
	private static final Logger logger = LoggerFactory.getLogger(DistributedCotMessengerForApi.class);

	private final SubscriptionStore subscriptionStore;

	private final ServerInfo serverInfo;

	private final MessageConverter messageConverter;

	
	private boolean isPlugins = false;
	private boolean isCluster = false;

	@Override
	public void send(CotEventContainer message) {
		CotEventContainer messageCopy = new CotEventContainer(message, true, ImmutableSet.of(Constants.SOURCE_TRANSPORT_KEY, Constants.SOURCE_PROTOCOL_KEY, Constants.USER_KEY));

		// make sure the source transport is tracked so that it can be reconstitued later after serialization and deserializtion
		if (message.getContextValue(Constants.CONNECTION_ID_KEY) != null && message.getContextValue(Constants.CONNECTION_ID_KEY) instanceof String) {
			try {
				String connectionId = (String) message.getContextValue(Constants.SOURCE_HASH_KEY);

				ConnectionInfo ci = new ConnectionInfo();

				ci.setConnectionId(connectionId);

				messageCopy.setContext(Constants.CONNECTION_ID_KEY, connectionId);

				if (subscriptionStore.getSubscriptionByConnectionInfo(ci) == null) {
					ChannelHandler handler = (ChannelHandler) message.getContext(Constants.SOURCE_TRANSPORT_KEY);
					if (handler != null) {
						Subscription sub = subscriptionStore.getByHandler((ChannelHandler) message.getContext(Constants.SOURCE_TRANSPORT_KEY));
						if (sub != null) {
							subscriptionStore.putSubscriptionToConnectionInfo(ci, sub);
						}
					}
				}

			} catch (ClassCastException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Not trying to get group info for message with invalid type of groups object: " + message);
				}
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("connection id context key not set for message: " + message);
			}
		}

		if (logger.isTraceEnabled()) {

			StringBuilder sb = new StringBuilder();

			sb.append(getClass().getSimpleName() + " sending message to ignite - context contents ");

			messageCopy.getContext().forEach((key, value) -> {
				sb.append("key: " + key);
				sb.append("val type: " + value.getClass().getSimpleName());
				sb.append("\n");
			});
		}

		if (logger.isDebugEnabled()) {
			logger.debug("sending message to submission topic " + serverInfo.getSubmissionTopic());
		}

		messageCopy.setContext(Constants.COT_MESSENGER_TOPIC_KEY, serverInfo.getSubmissionTopic());

		byte[] protoMessage = messageConverter.cotToDataMessage(messageCopy);
		ignite.message().send(serverInfo.getSubmissionTopic(), protoMessage);

		// push the message to the plugin queue, if plugins and plugin message queue are enabled
		if (isPlugins) {
			byte[] rawMsg = messageConverter.cotToDataMessage(new CotEventContainer(message, true, ImmutableSet.of(Constants.SOURCE_TRANSPORT_KEY, Constants.SOURCE_PROTOCOL_KEY, Constants.USER_KEY)));
			if (isCluster) {
				ClusterManager.getInstance().onPluginMessage(rawMsg);
			} else {
				// does not include the handler as this will be consumed by plugins in another process
				ignite.message().send(CommonConstants.PLUGIN_SUBSCRIBE_TOPIC, rawMsg);
			}
		}
	}
}
