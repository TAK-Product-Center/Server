package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;

import tak.server.Constants;
import tak.server.PluginManager;

public class PluginManagerProxyFactory implements FactoryBean<PluginManager> {

	private static final Logger logger = LoggerFactory.getLogger(PluginManagerProxyFactory.class);

	@Autowired
	Ignite ignite;

	@Override
	public PluginManager getObject() throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("get PluginManager from ignite");
		}

		return ignite.services(ClusterGroupDefinition.getPluginManagerClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_MANAGER, PluginManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return PluginManager.class;
	}
}
