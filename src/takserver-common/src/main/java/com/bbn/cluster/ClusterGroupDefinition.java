package com.bbn.cluster;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterGroup;

import tak.server.Constants;
import tak.server.plugins.PluginManagerConstants;


/**
 */
public class ClusterGroupDefinition {
	private static boolean isCluster = false;
	private static String profile;
	
	public static void setProfile(String profile) {
		ClusterGroupDefinition.profile = profile;
	}
	
	public static void setCluster(boolean isCluster) {
		ClusterGroupDefinition.isCluster = isCluster;
	}
	
	public static ClusterGroup getMessagingClusterDeploymentGroup(Ignite ignite) {
		return getClusterGroup(ignite, ignite.cluster(), Constants.MESSAGING_PROFILE_NAME);
	}
	
	public static ClusterGroup getMessagingLocalClusterDeploymentGroup(Ignite ignite) {		
		return getClusterGroup(ignite, ignite.cluster().forLocal(), Constants.MESSAGING_PROFILE_NAME);
	}
	
	public static ClusterGroup getMessagingNonLocalClusterDeploymentGroup(Ignite ignite) {
		return getClusterGroup(ignite, ignite.cluster().forPredicate(node -> node != ignite.cluster().localNode()), Constants.MESSAGING_PROFILE_NAME);
	}
	
	public static ClusterGroup getApiClusterDeploymentGroup(Ignite ignite) {
		return getClusterGroup(ignite, ignite.cluster(), Constants.API_PROFILE_NAME);
	}
	
	public static ClusterGroup getApiLocalClusterDeploymentGroup(Ignite ignite) {		
		return getClusterGroup(ignite, ignite.cluster().forLocal(), Constants.API_PROFILE_NAME);
	}
	
	public static ClusterGroup getApiNonLocalClusterDeploymentGroup(Ignite ignite) {
		return getClusterGroup(ignite, ignite.cluster().forPredicate(node -> node != ignite.cluster().localNode()), Constants.API_PROFILE_NAME);
	}

	public static ClusterGroup getPluginManagerClusterDeploymentGroup(Ignite ignite) {
		return ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, PluginManagerConstants.PLUGIN_MANAGER_IGNITE_PROFILE);
	}

	public static ClusterGroup getRetentionClusterDeploymentGroup(Ignite ignite) {
		return ignite.cluster().forAttribute(Constants.TAK_PROFILE_KEY, Constants.RETENTION_PROFILE_NAME);
	}

	public static ClusterGroup getConfigClusterDeploymentGroup(Ignite ignite) {
		return getClusterGroup(ignite, ignite.cluster(), Constants.CONFIG_PROFILE_NAME);
	}


	private static ClusterGroup getClusterGroup(Ignite ignite, ClusterGroup cluster, String requestedProfile) {
		// all takserver ignite nodes will be clients in the cluster
		if (isCluster) {
			// just incase we ever need to run monolith in the cluster..
			if (profile == Constants.MONOLITH_PROFILE_NAME) {
				return cluster.forClients().forAttribute(Constants.TAK_PROFILE_KEY, Constants.MONOLITH_PROFILE_NAME);
			} 
			else {
				return cluster.forClients().forAttribute(Constants.TAK_PROFILE_KEY, requestedProfile);
			}
		} 
		// pull services from monolith node
		else if (profile == Constants.MONOLITH_PROFILE_NAME) {
			return cluster.forAttribute(Constants.TAK_PROFILE_KEY, Constants.MONOLITH_PROFILE_NAME);
		} 
		// pull services from requested profile node(s)
		else {
			return cluster.forAttribute(Constants.TAK_PROFILE_KEY, requestedProfile);
		}
	}
}
