package tak.server.federation.hub.plugin.manager;

import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import tak.server.federation.hub.FederationHubConstants;

public class FederationHubPluginManagerProxyFactory implements FactoryBean<FederationHubPluginManager> {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubPluginManagerProxyFactory.class);

    @Autowired
    Ignite ignite;

    @Override
    public FederationHubPluginManager getObject() throws Exception {
        ClusterGroup cg = ignite.cluster().forAttribute(
            FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
            FederationHubConstants.FEDERATION_HUB_PLUGIN_MANAGER_IGNITE_PROFILE);
        return ignite.services(cg)
            .serviceProxy(FederationHubConstants.FED_HUB_PLUGIN_MANAGER_SERVICE,
            		FederationHubPluginManager.class, false);
    }

    @Override
    public Class<?> getObjectType() {
        return FederationHubPluginManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
