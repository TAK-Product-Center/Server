package tak.server.federation.hub.broker;

import java.security.cert.X509Certificate;
import java.util.List;

import tak.server.federation.hub.ui.graph.FederationPolicyModel;

public interface FederationHubBroker {
    void addGroupCa(X509Certificate ca);
    void updatePolicy(FederationPolicyModel federationPolicyModel);
    List<HubConnectionInfo> getActiveConnections();
    List<String> getGroupsForNode(String federateId);
	void deleteGroupCa(String groupId);
}
