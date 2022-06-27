package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.ServerInfo;

import tak.server.Constants;

public class ServerInfoProxyFactory implements FactoryBean<ServerInfo> {

	private static final Logger logger = LoggerFactory.getLogger(ServerInfoProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public ServerInfo getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get " + getObjectType().getSimpleName() + " from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_SERVER_INFO, ServerInfo.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return ServerInfo.class;
	}

}
