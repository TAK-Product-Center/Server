package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import tak.server.Constants;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.service.RetentionPolicyConfig;

public class RetentionPolicyConfigProxyFactory implements FactoryBean<RetentionPolicyConfig> {

	private static final Logger logger = LoggerFactory.getLogger(RetentionPolicyConfigProxyFactory.class);

	@Autowired
	Ignite ignite;

	@Override
	public RetentionPolicyConfig getObject() throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("get RetentionPolicyService from ignite");
		}

		return ignite.services(ClusterGroupDefinition.getRetentionClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_RETENTION_POLICY_CONFIGURATION,
				RetentionPolicyConfig.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return RetentionPolicyConfig.class;
	}
}
