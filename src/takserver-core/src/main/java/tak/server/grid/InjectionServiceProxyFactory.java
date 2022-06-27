package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.service.InjectionService;

import tak.server.Constants;

/**
 */
public class InjectionServiceProxyFactory  implements FactoryBean<InjectionService> {

	private static final Logger logger = LoggerFactory.getLogger(InjectionServiceProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public InjectionService getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get " + getObjectType().getSimpleName() + " from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_INJECTION_SERVICE, InjectionService.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return InjectionService.class;
	}

}
