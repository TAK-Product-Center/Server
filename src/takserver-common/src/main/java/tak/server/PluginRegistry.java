package tak.server;

import java.util.Collection;

import tak.server.plugins.PluginInfo;

public interface PluginRegistry {
	
	/*
	 * Register a plugin
	 */
	void register(PluginInfo plugin);
	
	/*
	 * Get info about all registered plugins
	 */
	Collection<PluginInfo> getAllPluginInfo();
}
