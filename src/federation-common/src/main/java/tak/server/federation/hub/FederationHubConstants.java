package tak.server.federation.hub;

public class FederationHubConstants {

    public static final String FEDERATION_HUB_IGNITE_PROFILE_KEY = "fedhub-profile";
    public static final String FEDERATION_HUB_BROKER_IGNITE_PROFILE = "fedhub-broker-profile";
    public static final String FEDERATION_HUB_POLICY_IGNITE_PROFILE = "fedhub-policy-profile";
    public static final String FEDERATION_HUB_UI_IGNITE_PROFILE = "fedhub-ui-profile";
    public static final String FED_HUB_POLICY_MANAGER_SERVICE = "fed-hub-policy-manager";
    public static final String FED_HUB_BROKER_SERVICE = "fed-hub-broker";

    /* Ignite configuration parameters. */
    public static final String FEDERATION_HUB_IGNITE_HOST = "127.0.0.1";
    public static final Integer NON_MULTICAST_DISCOVERY_PORT = 48500;
    public static final Integer NON_MULTICAST_DISCOVERY_PORT_COUNT = 100;
    public static final Integer COMMUNICATION_PORT = 48100;
    public static final Integer COMMUNICATION_PORT_COUNT = 100;

}
