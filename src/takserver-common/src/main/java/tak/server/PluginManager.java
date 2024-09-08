package tak.server;

import java.util.Collection;
import java.util.Map;

import com.bbn.marti.sync.Metadata;

import tak.server.plugins.PluginInfo;
import tak.server.plugins.PluginResponse;

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
	 * Submit data to a plugin for processing. Implementation of this function is entirely delegated to the plugin.
	 * 
	 */
	void submitDataToPlugin(String pluginClassName, Map<String, String> allRequestParams, String data, String contentType);
	
	/*
	 * Submit data to a plugin for processing. Returns submitted data. Implementation of this function is entirely delegated to the plugin.
	 * 
	 */
	PluginResponse submitDataToPluginWithResult(String pluginClassName, Map<String, String> allRequestParams, String data, String contentType);

	/*
	 * Update data in a plugin. Implementation of this function is entirely delegated to the plugin.
	 * 
	 */
	void updateDataInPlugin(String pluginClassName, Map<String, String> allRequestParams, String data, String contentType);
	
	/*
	 * Request data from a plugin. Implementation of this function is entirely delegated to the plugin.
	 * 
	 */
	PluginResponse requestDataFromPlugin(String pluginClassName, Map<String, String> allRequestParams, String accept);
	
	/*
	 * Delete data from a plugin. Implementation of this function is entirely delegated to the plugin.
	 * 
	 */
	void deleteDataFromPlugin(String pluginClassName, Map<String, String> allRequestParams, String contentType);
	
	/*
	 * Notify Plugin of a File Upload Event. Implementation of this function is entirely delegated to the plugin.
	 */
	void onFileUpload(String pluginClassName, Metadata metadata);
}
