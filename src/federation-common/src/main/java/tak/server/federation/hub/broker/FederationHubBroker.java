package tak.server.federation.hub.broker;

import java.security.cert.X509Certificate;

public interface FederationHubBroker {
    void addGroupCa(X509Certificate ca);
}
