package tak.server.federation.hub;

public class FederationHubConstants {
	// ignite node profiles	
    public static final String FEDERATION_HUB_IGNITE_PROFILE_KEY = "fedhub-profile";
    public static final String FEDERATION_HUB_BROKER_IGNITE_PROFILE = "fedhub-broker-profile";
    public static final String FEDERATION_HUB_POLICY_IGNITE_PROFILE = "fedhub-policy-profile";
    public static final String FEDERATION_HUB_UI_IGNITE_PROFILE = "fedhub-ui-profile";
    public static final String FEDERATION_HUB_PLUGIN_MANAGER_IGNITE_PROFILE = "fedhub-plugin-manager-profile";
    
    // ignite distributed services
    public static final String FED_HUB_POLICY_MANAGER_SERVICE = "fed-hub-policy-manager-service";
    public static final String FED_HUB_PLUGIN_MANAGER_SERVICE = "fed-hub-plugin-manager-service";
    public static final String FED_HUB_PLUGIN_SERVICE = "fed-hub-plugin-service";
    public static final String FED_HUB_BROKER_SERVICE = "fed-hub-broker";

    /* Ignite configuration parameters. */
    public static final String FEDERATION_HUB_IGNITE_HOST = "127.0.0.1";
    public static final Integer NON_MULTICAST_DISCOVERY_PORT = 48500;
    public static final Integer NON_MULTICAST_DISCOVERY_PORT_COUNT = 100;
    public static final Integer COMMUNICATION_PORT = 48100;
    public static final Integer COMMUNICATION_PORT_COUNT = 100;

    
    public static final String FEDERATION_HUB_PLUGIN_REGISTRATION_CACHE = "fedhub-plugin-registration-cache";
    public static final String FEDERATION_HUB_PLUGIN_CONFIG_CACHE = "fedhub-plugin-config-cache";
    
    public static final String FEDERATION_HUB_PLUGIN_SUBSCRIBE_TOPIC = "federation-hub-plugin-subscribe-topic";
    public static final String FEDERATION_HUB_PLUGIN_PUBLISH_TOPIC = "federation-hub-plugin-publish-topic";
    
    public static final String FEDERATION_HUB_PLUGIN_PROVENANCE = "federation-hub-plugin-provenance";
  
    // key for storing interceptor message destinations 
    public static final String FEDERATION_HUB_PLUGIN_INTERCEPTOR_DESTINATIONS = "federation-hub-plugin-interceptor-destinations";
    // plugins will listen on this topic for intercepted messages
    public static final String FEDERATION_HUB_PLUGIN_INTERCEPTOR_SUBSCRIBE_TOPIC = "federation-hub-plugin-interceptor-subscribe-topic";
    // TAK Server will listen on this topic for re-injected messages from interceptor
    public static final String FEDERATION_HUB_PLUGIN_INTERCEPTOR_PUBLISH_TOPIC = "federation-hub-plugin-interceptor-publish-topic";

}
