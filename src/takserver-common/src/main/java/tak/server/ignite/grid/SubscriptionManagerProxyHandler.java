package tak.server.ignite.grid;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ignite.cluster.ClusterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.TakException;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;
import tak.server.ignite.cache.IgniteCacheHolder;

public class SubscriptionManagerProxyHandler {
	private static final Logger logger = LoggerFactory.getLogger(SubscriptionManagerProxyHandler.class);
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public SubscriptionManagerProxyHandler() {
	}

	public CompletableFuture<SubscriptionManagerLite> getSubscriptionManagerForClientUid(String uid) {
		CompletableFuture<SubscriptionManagerLite> future = new CompletableFuture<>();

		final int maxRetries = 10;
		final long delayMs = 1000;

		// if we're in the cluster, we need to grab the SubscriptionManager the user is
		// connected to. in standalone there is only one SubscriptionManager
		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			
			RemoteSubscription sub = IgniteCacheHolder.getSubscriptionFromIgniteSubscriptionClientUidTrackerCache(uid);
			// sometimes if api requests are made before the messaging is finished setting things up,
			// the sub will not be in the cache yet.
			if (sub == null) {
				AtomicInteger attempt = new AtomicInteger(0);
				AtomicReference<ScheduledFuture<?>> schedulerRef = new AtomicReference<>();
				// we will return a future here and attempt retries to ensure the request
				// can succeed in most cases
				ScheduledFuture<?> scheduler = executor.scheduleAtFixedRate(() -> {
					try {
						RemoteSubscription subRetry = IgniteCacheHolder
							.getSubscriptionFromIgniteSubscriptionClientUidTrackerCache(uid);

						// sub was found on a retry - complete the future
						if (subRetry != null) {
							future.complete(getSubscriptionManager(ClusterGroupDefinition
									.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite())
									.forNodeId(subRetry.originNode)));
							
							if (schedulerRef.get() != null) {
								schedulerRef.get().cancel(true);
							}
							return;
						}
					} catch (Exception e) {}

					
					if (attempt.incrementAndGet() >= maxRetries) {
						future.completeExceptionally(new TakException("Client Messaging Node Not Reachable"));
						if (schedulerRef.get() != null) {
							schedulerRef.get().cancel(true);
						}
					}
				}, 100, delayMs, TimeUnit.MILLISECONDS);
				schedulerRef.set(scheduler);
			} else {
				future.complete(getSubscriptionManager(ClusterGroupDefinition
						.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite())
						.forNodeId(sub.originNode)));
			}
		} else {
			future.complete(getSubscriptionManager(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite())));
		}
		return future;
	}

	private SubscriptionManagerLite getSubscriptionManager(ClusterGroup group) {
		return IgniteHolder.getInstance().getIgnite().services(group)
				.serviceProxy(Constants.DISTRIBUTED_SUBSCRIPTION_MANAGER, SubscriptionManagerLite.class, false);
	}
}
