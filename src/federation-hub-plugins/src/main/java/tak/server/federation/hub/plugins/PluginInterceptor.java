package tak.server.federation.hub.plugins;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;

import tak.server.federation.hub.*;
import tak.server.federation.hub.plugin.*;
import tak.server.federation.hub.plugins.clients.PluginInterceptorClient;

public class PluginInterceptor extends PluginBase {
	private static final Logger logger = LoggerFactory.getLogger(PluginInterceptor.class);

	private final PluginInterceptorClient client;

	private IgniteBiPredicate<UUID, Object> ignitePredicate;

	public PluginInterceptor(PluginInterceptorClient client) {
		super(client, FederationHubPluginType.INTERCEPTOR);
		this.client = client;
	}

	public PluginInterceptor(PluginInterceptorClient client, PluginIgniteConfig pluginIgniteConfig) {
		super(client, FederationHubPluginType.INTERCEPTOR, pluginIgniteConfig);
		this.client = client;
	}

	@Override
	protected void onRegistered() {
		initListener();
	}

	@Override
	protected void onUnRegistered() {
		if (ignitePredicate != null)
			ignite.message().stopLocalListen(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_SUBSCRIBE_TOPIC, ignitePredicate);
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
						CompletableFuture<FederatedEvent> future = client
								.interceptFederatedEvent((FederatedEvent) content);
						if (future != null) {
							future.thenAccept(modified -> handleEvent(message, modified));
						} else {
							logger.warn("FederatedEvent future was null, Dropping message!");
						}
					} else if (content instanceof FederateGroups) {
						CompletableFuture<FederateGroups> future = client
								.interceptFederateGroups((FederateGroups) content);
						if (future != null) {
							future.thenAccept(modified -> handleGroups(message, modified));
						} else {
							logger.warn("FederateGroups future was null, Dropping message!");
						}
					} else if (content instanceof BinaryBlob) {
						CompletableFuture<BinaryBlob> future = client.interceptBinaryBlob((BinaryBlob) content);
						if (future != null) {
							future.thenAccept(modified -> handleBinaryBlob(message, modified));
						} else {
							logger.warn("BinaryBlob future was null, Dropping message!");
						}
					} else if (content instanceof ROL) {
						CompletableFuture<ROL> future = client.interceptROL((ROL) content);
						if (future != null) {
							future.thenAccept(modified -> handleROL(message, modified));
						} else {
							logger.warn("ROL future was null, Dropping message!");
						}
					}
				} catch (Exception e) {
					logger.error("Error handling intercepted message", e);
				}

				return true;
			};

			ignite.message().localListen(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_SUBSCRIBE_TOPIC,
					ignitePredicate);
		} catch (Exception e) {
			logger.error("Error connecting to Ignite listener", e);
		}
	}

	private void handleEvent(Message message, FederatedEvent event) {
		try {
			FederatedEvent eventWithProv = event.toBuilder().addFederateProvenance(getProvenance()).build();
			message.setPayload(new FederatedEventPayload(eventWithProv));
			messageForwarder.accept(message);
		} catch (Exception e) {
			logger.error("Error forwarding FederatedEvent", e);
		}
	}

	private void handleGroups(Message message, FederateGroups groups) {
		try {
			FederateGroups withProv = groups.toBuilder().addFederateProvenance(getProvenance()).build();
			message.setPayload(new FederatedGroupPayload(withProv));
			messageForwarder.accept(message);
		} catch (Exception e) {
			logger.error("Error forwarding FederateGroups", e);
		}
	}

	private void handleBinaryBlob(Message message, BinaryBlob blob) {
		try {
			BinaryBlob withProv = blob.toBuilder().addFederateProvenance(getProvenance()).build();
			message.setPayload(new BinaryBlobPayload(withProv));
			messageForwarder.accept(message);
		} catch (Exception e) {
			logger.error("Error forwarding BinaryBlob", e);
		}
	}

	private void handleROL(Message message, ROL rol) {
		try {
			ROL withProv = rol.toBuilder().addFederateProvenance(getProvenance()).build();
			message.setPayload(new ROLPayload(withProv));
			messageForwarder.accept(message);
		} catch (Exception e) {
			logger.error("Error forwarding ROL", e);
		}
	}

	// if there is another interceptor in the chain, this message forwarder will
	// send the message to
	// the next interceptor plugin. otherwise it will be sent back to fedhub
	private final Consumer<Message> messageForwarder = (message) -> {
		@SuppressWarnings("unchecked")
		Set<String> destinations = (Set<String>) message
				.getMetadataValue(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_DESTINATIONS);

		// no more inerceptors, back to fedhub
		if (destinations == null || destinations.isEmpty()) {
			message.setMetadataValue(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_DESTINATIONS, null);
			ignite.message().send(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_PUBLISH_TOPIC, message);
			return;
		}

		// get the next interceptor id and remove it from the list
		Iterator<String> it = destinations.iterator();
		if (!it.hasNext()) {
			message.setMetadataValue(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_DESTINATIONS, null);
			ignite.message().send(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_PUBLISH_TOPIC, message);
			return;
		}

		String nextName = it.next();
		it.remove();

		ClusterGroup nextInterceptor = ignite.cluster()
				.forPredicate(node -> Objects.equals(node.attribute("plugin-name"), nextName));

		// if for some reason we can't find the interceptor plugin node in the cluster,
		// send back to fedhub
		if (nextInterceptor.nodes().isEmpty()) {
			message.setMetadataValue(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_DESTINATIONS, null);
			ignite.message().send(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_PUBLISH_TOPIC, message);
		} else {
			message.setMetadataValue(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_DESTINATIONS,
					destinations);
			ignite.message(nextInterceptor)
					.send(FederationHubConstants.FEDERATION_HUB_PLUGIN_INTERCEPTOR_SUBSCRIBE_TOPIC, message);
		}
	};
}
