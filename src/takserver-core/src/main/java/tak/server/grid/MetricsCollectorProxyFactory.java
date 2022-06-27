package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.metrics.MetricsCollector;

import tak.server.Constants;

public class MetricsCollectorProxyFactory implements FactoryBean<MetricsCollector> {

	private static final Logger logger = LoggerFactory.getLogger(MetricsCollectorProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public MetricsCollector getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get " + getObjectType().getSimpleName() + " from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_METRICS_COLLECTOR, MetricsCollector.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return MetricsCollector.class;
	}
	
}
