package tak.server.federation.hub.policy;

import tak.server.federation.FederationPolicyGraph;

public class FederationHubPolicyStore {
	
	private static FederationHubPolicyStore instance; 
    public static synchronized FederationHubPolicyStore getInstance() {
    	if (instance == null)
    		instance = new FederationHubPolicyStore();
    	
    	return instance;
	}
	
    private FederationPolicyGraph policyGraph;

    public FederationPolicyGraph getPolicyGraph() {
		return policyGraph;
	}
    
	public void setPolicyGraph(FederationPolicyGraph policyGraph) {
		this.policyGraph = policyGraph;
	}

}
