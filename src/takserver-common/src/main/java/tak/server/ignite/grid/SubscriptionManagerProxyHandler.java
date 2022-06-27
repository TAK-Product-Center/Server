package tak.server.ignite.grid;

import java.util.UUID;

import org.apache.ignite.cluster.ClusterGroup;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.SubscriptionManagerLite;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;

public class SubscriptionManagerProxyHandler extends IgniteProxyHandler {
	
	public SubscriptionManagerProxyHandler(CoreConfig coreConfig) {
		super(coreConfig);
	}

	public SubscriptionManagerLite getSubscriptionManagerForClientUid(String uid) {
		if (coreConfig.getRemoteConfiguration().getCluster().isEnabled()) {
			return getSubscriptionManager(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite())
					.forNodeId(getIgniteSubscriptionClientUidTackerCache().get(uid).originNode));
		} else {
			return getSubscriptionManager(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite()));
		}
	}

	public SubscriptionManagerLite getSubscriptionManagerForSubscriptionUid(String uid) {
		if (coreConfig.getRemoteConfiguration().getCluster().isEnabled()) {
			return getSubscriptionManager(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite())
					.forNodeId(getIgniteSubscriptionUidTackerCache().get(uid).originNode));
		} else {
			return getSubscriptionManager(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite()));
		}
	}
	
	private SubscriptionManagerLite getSubscriptionManager(ClusterGroup group) {
		return IgniteHolder.getInstance()
				.getIgnite()
				.services(group)
				.serviceProxy(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, SubscriptionManagerLite.class, false);
	}
}
