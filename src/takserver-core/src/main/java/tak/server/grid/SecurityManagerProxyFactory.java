package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.service.SecurityManager;

import tak.server.Constants;

/**
 */
public class SecurityManagerProxyFactory  implements FactoryBean<SecurityManager> {

	private static final Logger logger = LoggerFactory.getLogger(SecurityManagerProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public SecurityManager getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get " + getObjectType().getSimpleName() + " from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_SECURITY_MANAGER, SecurityManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return SecurityManager.class;
	}

}
