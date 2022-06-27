package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.service.InputManager;

import tak.server.Constants;

/**
 */
public class InputManagerProxyFactory  implements FactoryBean<InputManager> {

	private static final Logger logger = LoggerFactory.getLogger(InputManagerProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public InputManager getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get " + getObjectType().getSimpleName() + " from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return InputManager.class;
	}

}
