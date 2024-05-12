package com.bbn.marti.takcl.connectivity.server;

import java.util.Set;

/**
 * Valid server process configurations
 * <p>
 * Each configuration indicates which processes run as part of the specified configuration
 */
public enum ServerProcessConfiguration {
    DefaultConfigMessagingApi(ServerProcessDefinition.ConfigService, ServerProcessDefinition.MessagingService,
        ServerProcessDefinition.ApiService),
    ConfigMessagingApiPlugins(ServerProcessDefinition.ConfigService, ServerProcessDefinition.MessagingService,
        ServerProcessDefinition.ApiService, ServerProcessDefinition.PluginManager),
    ConfigMessagingApiRetention(ServerProcessDefinition.ConfigService, ServerProcessDefinition.MessagingService,
        ServerProcessDefinition.ApiService, ServerProcessDefinition.RetentionService),
    ConfigMessagingApiPluginsRetention(ServerProcessDefinition.ConfigService, ServerProcessDefinition.MessagingService,
        ServerProcessDefinition.ApiService, ServerProcessDefinition.PluginManager, ServerProcessDefinition.RetentionService),
    FedhubBrokerFedhubPolicy(ServerProcessDefinition.FederationHubBroker, ServerProcessDefinition.FederationHubPolicy);

    private final Set<ServerProcessDefinition> enabledServerProcessDefinitions;

    ServerProcessConfiguration(ServerProcessDefinition... enabledServerProcessDefinitions) {
        this.enabledServerProcessDefinitions = Set.of(enabledServerProcessDefinitions);
    }

    public boolean isProcessEnabled(ServerProcessDefinition serverProcessDefinition) {
        return enabledServerProcessDefinitions.contains(serverProcessDefinition);
    }
}
