package tak.server.federation.hub.plugin;

public enum FederationHubPluginType {
    SENDER,
    RECEIVER,
    INTERCEPTOR;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static FederationHubPluginType fromString(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Plugin type cannot be null or blank.");
        }

        switch (type.toLowerCase()) {
            case "sender":
                return SENDER;
            case "receiver":
                return RECEIVER;
            case "interceptor":
                return INTERCEPTOR;
            default:
                throw new IllegalArgumentException("Invalid plugin type: " + type);
        }
    }
}
