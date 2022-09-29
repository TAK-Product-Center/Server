package tak.server.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public interface PluginDataApi {
	
	static final Logger logger = LoggerFactory.getLogger(PluginDataApi.class);
	
	/*
	 * Submit data to a plugin for processing by the plugin. Implementation of this function is provided by the plugin.
	 * 
	 */
	default	void onSubmitData(String scope, String data, String contentType) {
		
		logger.info("submitDataToPlugin method not implemented in plugin class " + getClass().getName() + " must be implemented in order to submit data."); 
	}
	
}
