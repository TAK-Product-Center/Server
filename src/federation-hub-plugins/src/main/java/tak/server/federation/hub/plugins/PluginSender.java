package tak.server.federation.hub.plugins;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.FederatedEvent;

import tak.server.federation.hub.*;
import tak.server.federation.hub.plugin.*;
import tak.server.federation.hub.plugins.clients.PluginInterceptorClient;
import tak.server.federation.hub.plugins.clients.PluginSenderClient;

public class PluginSender extends PluginBase {
	private static final Logger logger = LoggerFactory.getLogger(PluginSender.class);

	private final PluginSenderClient client;
	
	public PluginSender(PluginSenderClient client) {
		super(client, FederationHubPluginType.SENDER);
		this.client = client;
	}

	public PluginSender(PluginSenderClient client, PluginIgniteConfig pluginIgniteConfig) {
		super(client, FederationHubPluginType.SENDER, pluginIgniteConfig);
		this.client = client;
	}
	
	@Override
	protected void onRegistered() {}

	@Override
	protected void onUnRegistered() {}

	public void sendMessage(FederatedEvent event) {
		if (!isRegistered.get())
			return;

		try {
			FederatedEvent eventWithProv = event.toBuilder().addFederateProvenance(getProvenance()).build();

			Message message = new Message(new HashMap<>(), new FederatedEventPayload(eventWithProv));
			message.setMetadataValue("plugin-name", client.getMetadata().getName());

			ignite.message().send(FederationHubConstants.FEDERATION_HUB_PLUGIN_PUBLISH_TOPIC, message);
		} catch (Exception e) {
			logger.error("Error sending message", e);
		}
	}
}
