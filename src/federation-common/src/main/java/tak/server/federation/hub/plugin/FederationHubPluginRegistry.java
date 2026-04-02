package tak.server.federation.hub.plugin;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import tak.server.federation.hub.FederationHubConstants;

public class FederationHubPluginRegistry {
	private static IgniteCache<String, FederationHubPluginMetadata> registrationCache;
	
	public static IgniteCache<String, FederationHubPluginMetadata> registrationCache(Ignite ignite) {
		if (registrationCache == null) {
			CacheConfiguration<String, FederationHubPluginMetadata> cacheCfg = new CacheConfiguration<>(
					FederationHubConstants.FEDERATION_HUB_PLUGIN_REGISTRATION_CACHE);
			cacheCfg.setIndexedTypes(String.class, FederationHubPluginMetadata.class);
			registrationCache = ignite.getOrCreateCache(cacheCfg);
		}

		return registrationCache;
	}
	
	public static Map<String, FederationHubPluginMetadata> registrationCacheAsMap(Ignite ignite) {
		return registrationCache(ignite).query(new ScanQuery<String, FederationHubPluginMetadata>()).getAll().stream()
				.collect(Collectors.toMap(IgniteCache.Entry::getKey, IgniteCache.Entry::getValue));
	}

	public static void clearCache(Ignite ignite) {
		registrationCache(ignite).removeAll();
	}
}
