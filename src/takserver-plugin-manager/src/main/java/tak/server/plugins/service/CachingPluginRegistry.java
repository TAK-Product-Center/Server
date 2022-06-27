package tak.server.plugins.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import tak.server.PluginRegistry;
import tak.server.plugins.PluginInfo;

public class CachingPluginRegistry implements PluginRegistry {
	
	private static final Logger logger = LoggerFactory.getLogger(CachingPluginRegistry.class);
	
	private final Map<UUID, PluginInfo> registryStore = new ConcurrentHashMap<>();

	@Override
	public void register(PluginInfo plugin) {
		
		if (plugin == null || plugin.getId() == null || Strings.isNullOrEmpty(plugin.getName())) {
			throw new IllegalArgumentException("invalid plugin info " + plugin);
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("register plugin " + plugin);
		}

		registryStore.put(plugin.getId(), plugin);
	}

	@Override
	public Collection<PluginInfo> getAllPluginInfo() {
		
		return registryStore.values();

	}
}
