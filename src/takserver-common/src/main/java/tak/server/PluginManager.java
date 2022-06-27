package tak.server;

import java.util.Collection;

import tak.server.plugins.PluginInfo;

public interface PluginManager {
	
	/*
	 * @Returns a Collection of PluginInfo objects, describing all plugins
	 */
	Collection<PluginInfo> getAllPluginInfo();
	
	/*
	 * start all registered plugins.
	 */
	void startAllPlugins();
	
	/*
	 * stop all registered plguins.
	 */
	void stopAllPlugins();
	
	/*
	 * Start one plugin by id.
	 */
	void startPluginByName(String name);
	
	/*
	 * Stop one plugin by id.
	 */
	void stopPluginByName(String name);
}
