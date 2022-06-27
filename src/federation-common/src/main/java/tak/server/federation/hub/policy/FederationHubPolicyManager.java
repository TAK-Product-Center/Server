package tak.server.federation.hub.policy;

import tak.server.federation.Federate;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederationException;
import tak.server.federation.FederationPolicyGraph;

import tak.server.federation.hub.ui.graph.FederationPolicyModel;

import java.util.Collection;
import java.util.List;

public interface FederationHubPolicyManager {
    void addCaGroup(FederateGroup federateGroup);
    Collection<FederateGroup> getCaGroups();
    void addCaFederate(Federate federate, List<String> federateCaNames);
    FederationPolicyGraph getPolicyGraph();
    void updatePolicyGraph(FederationPolicyModel federationPolicyModel, Object updateFile) throws FederationException;
    void setPolicyGraph(FederationPolicyModel newPolicyModel, Object updateFile) throws FederationException;
}
