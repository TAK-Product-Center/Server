package tak.server.federation.hub.policy;

import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import tak.server.federation.hub.FederationHubConstants;

public class FederationHubPolicyManagerProxyFactory implements FactoryBean<FederationHubPolicyManager> {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubPolicyManagerProxyFactory.class);

    @Autowired
    Ignite ignite;

    @Override
    public FederationHubPolicyManager getObject() throws Exception {
        ClusterGroup cg = ignite.cluster().forAttribute(
            FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
            FederationHubConstants.FEDERATION_HUB_POLICY_IGNITE_PROFILE);
        return ignite.services(cg)
            .serviceProxy(FederationHubConstants.FED_HUB_POLICY_MANAGER_SERVICE,
                FederationHubPolicyManager.class, false);
    }

    @Override
    public Class<?> getObjectType() {
        return FederationHubPolicyManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
