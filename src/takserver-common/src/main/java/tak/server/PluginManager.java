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
	
	/*
	 * set plugin archive for one plugin
	 */
	void setPluginArchive(String name, boolean isArchiveEnabled);
	
	/*
	 * set plugin enabled atribute for one plugin
	 */
	void setPluginEnabled(String name, boolean isPluginEnabled);
	
	/*
	 * Submit data a plugin for processing. Implementation of this function is entirely delegated to the plugin.
	 * 
	 */
	void submitDataToPlugin(String pluginClassName, String scope, String data, String contentType);
}
