package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.DataFeedCotService;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.service.SubscriptionManager;

import tak.server.Constants;

public class DistributedDatafeedCotServiceProxyFactory implements FactoryBean<DataFeedCotService> {

	private static final Logger logger = LoggerFactory.getLogger(DistributedDatafeedCotServiceProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public DataFeedCotService getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get DistributedDataFeedCotService from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_DATAFEED_COT_SERVICE, DataFeedCotService.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return DataFeedCotService.class;
	}	
}
