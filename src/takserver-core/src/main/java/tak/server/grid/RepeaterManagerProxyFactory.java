package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.RepeaterManager;

import tak.server.Constants;

public class RepeaterManagerProxyFactory implements FactoryBean<RepeaterManager> {

	private static final Logger logger = LoggerFactory.getLogger(RepeaterManagerProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public RepeaterManager getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get " + getObjectType().getSimpleName() + " from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_REPEATER_MANAGER, RepeaterManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return RepeaterManager.class;
	}

}
