package tak.server.federation.hub.policy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import tak.server.federation.Federate;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederationException;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.ui.graph.FederationUIPolicyModel;
import tak.server.federation.hub.ui.graph.PolicyObjectCell;

public interface FederationHubPolicyManager {
    void addCaGroup(FederateGroup federateGroup);
    Collection<FederateGroup> getCaGroups();
    FederationPolicyGraph addCaFederate(Federate federate, List<String> federateCaNames);
    FederationPolicyGraph getActivePolicyGraph();
    FederationPolicyGraph getPolicyGraph(String policyId);
    void setPolicyGraph(FederationPolicyGraph newPolicyGraph) throws FederationException;
	Collection<PolicyObjectCell> getPolicyCells();
	void removeCaGroup(FederateGroup federateGroup);
	Map<String, FederationUIPolicyModel> getAllPolicies();
	
	FederationUIPolicyModel savePolicyFile(FederationUIPolicyModel policy);
	FederationUIPolicyModel saveGraphPolicyFile(FederationUIPolicyModel policy);
	FederationUIPolicyModel saveSettingsPolicyFile(FederationUIPolicyModel policy);
	FederationUIPolicyModel savePluginsPolicyFile(FederationUIPolicyModel policy);
}
