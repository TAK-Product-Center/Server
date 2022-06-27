package tak.server.ignite;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterGroup;

import com.bbn.cluster.ClusterGroupDefinition;

public class IgniteBroker {
	
	protected static <S, R> R brokerMessagingNonLocalServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return serviceCalls(function, proxyName, clazz, getMessagingClusterGroup(false));
	}
	
	protected static <S> void brokerMessagingNonLocalVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		voidServiceCalls(function, proxyName, clazz, getMessagingClusterGroup(false));
	}
	
	protected static <S, R> R brokerMessagingServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return serviceCalls(function, proxyName, clazz, getMessagingClusterGroup(true));
	}
	
	protected static <S> void brokerMessagingVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		voidServiceCalls(function, proxyName, clazz, getMessagingClusterGroup(true));
	}
	
	protected static <S, R> R brokerApiNonLocalServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return serviceCalls(function, proxyName, clazz, getApiClusterGroup(false));
	}
	
	protected static <S> void brokerApiNonLocalVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		voidServiceCalls(function, proxyName, clazz, getApiClusterGroup(false));
	}
	
	protected static <S, R> R brokerApiServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return serviceCalls(function, proxyName, clazz, getApiClusterGroup(true));
	}
	
	protected static <S> void brokerApiVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		voidServiceCalls(function, proxyName, clazz, getApiClusterGroup(true));
	}
	
	protected static <S, R> R brokerPluginServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return serviceCalls(function, proxyName, clazz, getPluginClusterGroup(true));
	}
	
	protected static <S> void brokerPluginVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		voidServiceCalls(function, proxyName, clazz, getPluginClusterGroup(true));
	}
	
	@SuppressWarnings("unchecked")
	private static <S> void voidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz, ClusterGroup clusterGroup) {
		Ignite ignite = IgniteHolder.getInstance().getIgnite();
		clusterGroup
			.nodes()
			.stream()
			.map(node -> (S) ignite.services(ignite.cluster().forNode(node))
					.serviceProxy(proxyName, clazz, false))
			.forEach( service -> {
				function.accept(service);
			});
	}
	
	@SuppressWarnings("unchecked")
	private static <S, R> R serviceCalls(Function<S, R> function, String proxyName, Class<?> clazz, ClusterGroup clusterGroup) {
		Ignite ignite = IgniteHolder.getInstance().getIgnite();
		final Return returnHolder = new Return();
		
		clusterGroup
			.nodes()
			.parallelStream()
			.map(node -> (S) ignite.services(ignite.cluster().forNode(node))
					.serviceProxy(proxyName, clazz, false))
			.forEach( service -> {
				returnHolder.o = function.apply(service);
			});
		
		return (R) returnHolder.o;
	}
	
	private static class Return {
		public Object o;
	}
	
	private static ClusterGroup getMessagingClusterGroup(boolean includeLocal) {
		return includeLocal ? ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite()) : 
			ClusterGroupDefinition.getMessagingNonLocalClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite());
	}
	
	private static ClusterGroup getApiClusterGroup(boolean includeLocal) {
		return includeLocal ? ClusterGroupDefinition.getApiClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite()) : 
			ClusterGroupDefinition.getApiNonLocalClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite());
	}
	
	private static ClusterGroup getPluginClusterGroup(boolean includeLocal) {
		return ClusterGroupDefinition.getPluginManagerClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite());
	}

}
