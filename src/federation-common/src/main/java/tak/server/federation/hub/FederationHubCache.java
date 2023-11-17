package tak.server.federation.hub;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;

import tak.server.federation.FederationPolicyGraph;

public class FederationHubCache {
	
    public static final String POLICY_GRAPH_CACHE_KEY = "policyGraph";
	private static IgniteCache<String, FederationPolicyGraph> configurationCache;
	public static IgniteCache<String, FederationPolicyGraph> getFederationHubPolicyStoreCache(Ignite ignite) {
		if (configurationCache == null) {
			CacheConfiguration<String, FederationPolicyGraph> cfg = new CacheConfiguration<String, FederationPolicyGraph>();
		
			cfg.setName("FederationHubPolicyStore");
			cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
			configurationCache = ignite.getOrCreateCache(cfg);
		}
		
		return configurationCache;
	}

}
