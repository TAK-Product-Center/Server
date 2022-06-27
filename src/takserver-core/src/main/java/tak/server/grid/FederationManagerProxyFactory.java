package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.FederationManager;

import tak.server.Constants;

public class FederationManagerProxyFactory implements FactoryBean<FederationManager> {

	private static final Logger logger = LoggerFactory.getLogger(FederationManagerProxyFactory.class);

	@Autowired
	Ignite ignite;
	
	@Override
	public FederationManager getObject() throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("get distributed FederationManagerInterface proxy");
		}

		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_FEDERATION_MANAGER, FederationManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return FederationManager.class;
	}

}
