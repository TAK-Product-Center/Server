package tak.server.ignite.grid;

import java.util.UUID;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.apache.ignite.cluster.ClusterGroup;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.SubscriptionManagerLite;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;
import tak.server.ignite.cache.IgniteCacheHolder;

public class SubscriptionManagerProxyHandler {

	public SubscriptionManagerProxyHandler() { }

	public SubscriptionManagerLite getSubscriptionManagerForClientUid(String uid) {
		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			return getSubscriptionManager(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite())
					.forNodeId(IgniteCacheHolder.getIgniteSubscriptionClientUidTackerCache().get(uid).originNode));
		} else {
			return getSubscriptionManager(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite()));
		}
	}

	public SubscriptionManagerLite getSubscriptionManagerForSubscriptionUid(String uid) {
		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			return getSubscriptionManager(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite())
					.forNodeId(IgniteCacheHolder.getIgniteSubscriptionUidTackerCache().get(uid).originNode));
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
