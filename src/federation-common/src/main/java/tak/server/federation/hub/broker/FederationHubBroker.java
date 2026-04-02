package tak.server.federation.hub.broker;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import tak.server.federation.hub.ui.graph.FederationUIPluginsPolicyModel;
import tak.server.federation.hub.ui.graph.FederationUIPolicyModel;

public interface FederationHubBroker {
    void addGroupCa(X509Certificate ca);
    void updatePolicy(FederationUIPolicyModel federationPolicyModel);
    List<HubConnectionInfo> getActiveConnections();
    FederationHubBrokerMetrics getFederationHubBrokerMetrics();
	FederationHubBrokerGlobalMetrics getFederationHubBrokerGlobalMetrics();
    List<String> getGroupsForNode(String federateId);
	void deleteGroupCa(String groupId);
	void disconnectFederate(String connectionId);
	Map<String, X509Certificate> getCAsFromFile();
	byte[] getSelfCaFile();
	FederationHubServerConfig getFederationHubBrokerConfig();
	FederationHubServerConfig saveFederationHubServerConfig(FederationHubServerConfig brokerConfig);
}