package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.service.SubscriptionManager;

import tak.server.Constants;

public class SubscriptionManagerProxyFactory implements FactoryBean<SubscriptionManager> {

	private static final Logger logger = LoggerFactory.getLogger(SubscriptionManagerProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public SubscriptionManager getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get DistributedSubscriptionManager from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, SubscriptionManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return SubscriptionManagerLite.class;
	}	
}
