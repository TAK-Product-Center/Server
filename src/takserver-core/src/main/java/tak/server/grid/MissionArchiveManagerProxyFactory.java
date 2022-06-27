package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import tak.server.Constants;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.service.MissionArchiveManager;
import com.bbn.marti.remote.service.RetentionPolicyConfig;

public class MissionArchiveManagerProxyFactory implements FactoryBean<MissionArchiveManager> {

	private static final Logger logger = LoggerFactory.getLogger(MissionArchiveManagerProxyFactory.class);

	@Autowired
	Ignite ignite;

	@Override
	public MissionArchiveManager getObject() throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("get MissionArchiveManager from ignite");
		}

		return ignite.services(ClusterGroupDefinition.getRetentionClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_MISSION_ARCHIVE_MANAGER,
				MissionArchiveManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return MissionArchiveManager.class;
	}
}
