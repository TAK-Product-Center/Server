package tak.server.federation.hub.broker;

import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import tak.server.federation.hub.FederationHubConstants;

public class FederationHubBrokerProxyFactory implements FactoryBean<FederationHubBroker> {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubBrokerProxyFactory.class);

    @Autowired
    Ignite ignite;

    @Override
    public FederationHubBroker getObject() throws Exception {
        ClusterGroup cg = ignite.cluster().forAttribute(
            FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
            FederationHubConstants.FEDERATION_HUB_BROKER_IGNITE_PROFILE);
        return ignite.services(cg)
            .serviceProxy(FederationHubConstants.FED_HUB_BROKER_SERVICE,
                FederationHubBroker.class, false);
    }

    @Override
    public Class<?> getObjectType() {
        return FederationHubBroker.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
