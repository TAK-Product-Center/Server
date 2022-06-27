package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.groups.GroupManager;

import tak.server.Constants;

public class GroupManagerProxyFactory implements FactoryBean<GroupManager> {

	private static final Logger logger = LoggerFactory.getLogger(GroupManagerProxyFactory.class);

	@Autowired
	Ignite ignite;

	@Override
	public GroupManager getObject() throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("get GroupManager from ignite");
		}

		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_GROUP_MANAGER, GroupManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return GroupManager.class;
	}
}
