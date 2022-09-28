package tak.server.messaging;

import org.apache.ignite.Ignite;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;

import tak.server.PluginManager;
import tak.server.plugins.PluginSelfStopApi;
import tak.server.Constants;

public class DistributedPluginSelfStopApi implements PluginSelfStopApi, org.apache.ignite.services.Service {

	private static final long serialVersionUID = 2276211741137405196L;
	
	private static final Logger logger = LoggerFactory.getLogger(DistributedPluginSelfStopApi.class);

	@Autowired
	Ignite ignite;

    private PluginManager pluginManager() throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("get PluginManager from ignite");
        }

        return ignite.services(ClusterGroupDefinition.getPluginManagerClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_PLUGIN_MANAGER, PluginManager.class, false);
    }
	
	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
    	
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	@Override
	public void pluginSelfStop(String pluginName) {
		
		PluginManager pluginManager;
		try {
			pluginManager = pluginManager();
			if (pluginManager == null) {
				logger.error("Plugin Manager is NULL");
			}else {
				pluginManager.stopPluginByName(pluginName);
			}		
		} catch (Exception e) {
			logger.error("Error in pluginSelfStop in DistributedSelfStopApi", e);
		}	
				
	}

}
