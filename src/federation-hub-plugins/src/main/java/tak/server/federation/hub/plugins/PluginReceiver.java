package tak.server.federation.hub.plugins;

import java.util.UUID;

import org.apache.ignite.lang.IgniteBiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;

import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.Message;
import tak.server.federation.hub.plugin.FederationHubPluginType;
import tak.server.federation.hub.plugins.clients.PluginReceiverClient;

public class PluginReceiver extends PluginBase {
	private static final Logger logger = LoggerFactory.getLogger(PluginReceiver.class);

	private final PluginReceiverClient client;

	private IgniteBiPredicate<UUID, Object> ignitePredicate;
	
	public PluginReceiver(PluginReceiverClient client) {
		super(client, FederationHubPluginType.RECEIVER);
		this.client = client;
	}

	public PluginReceiver(PluginReceiverClient client, PluginIgniteConfig pluginIgniteConfig) {
		super(client, FederationHubPluginType.RECEIVER, pluginIgniteConfig);
		this.client = client;
	}
	
	@Override
	protected void onRegistered() {
		initListener();
	}

	@Override
	protected void onUnRegistered() {
		if (ignitePredicate != null)
			ignite.message().stopLocalListen(FederationHubConstants.FEDERATION_HUB_PLUGIN_SUBSCRIBE_TOPIC, ignitePredicate);
	}

	private void initListener() {
		try {
			ignitePredicate = (uuid, msg) -> {
				if (!isRegistered.get())
					return true;

				try {
					Message message = (Message) msg;
					if (message.getPayload() == null || message.getPayload().getContent() == null)
						return true;

					Object content = message.getPayload().getContent();

					if (content instanceof FederatedEvent) {
						client.receiveFederatedEvent((FederatedEvent) content);
					} else if (content instanceof FederateGroups) {
						client.receiveFederateGroups((FederateGroups) content);
					} else if (content instanceof BinaryBlob) {
						client.receiveBinaryBlob((BinaryBlob) content);
					} else if (content instanceof ROL) {
						client.receiveROL((ROL) content);
					}
				} catch (Exception e) {
					logger.error("Error handling received message", e);
				}

				return true;
			};

			ignite.message().localListen(FederationHubConstants.FEDERATION_HUB_PLUGIN_SUBSCRIBE_TOPIC, ignitePredicate);
		} catch (Exception e) {
			logger.error("Error connecting to Ignite listener", e);
		}
	}
}
