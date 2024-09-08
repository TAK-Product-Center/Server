package tak.server.plugins;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.sync.Metadata;

/**
 */
public interface PluginDataApi {
	
	static final Logger logger = LoggerFactory.getLogger(PluginDataApi.class);
	
	/*
	 * Submit data to a plugin for processing by the plugin. Implementation of this function is provided by the plugin.
	 * 
	 */
	default	void onSubmitData(Map<String, String> allRequestParams, String data, String contentType) {
		
		logger.info("submitDataToPlugin method not implemented in plugin class " + getClass().getName() + " must be implemented in order to submit data."); 
	}
	
	/*
	 * Submit data to a plugin for processing by the plugin. Returns string of processed data.
	 * Implementation of this function is provided by the plugin.
	 * 
	 */
	default PluginResponse onSubmitDataWithResult(Map<String, String> allRequestParams, String data, String contentType) {
		
		logger.info("submitDataToPluginResult method not implemented in plugin class " + getClass().getName() + " must be implemented in order to submit data.");
		return null;
	}

	/*
	 * Update data in a plugin. Implementation of this function is provided by the plugin.
	 *
	 */
	default void onUpdateData(Map<String, String> allRequestParams, String data, String contentType) {

		logger.info("updateDataInPlugin method not implemented in plugin class " + getClass().getName() + " must be implemented in order to submit data.");
	}

	/*
	 * Request data from a plugin. Implementation of this function is provided by the plugin.
	 *
	 */
	default PluginResponse onRequestData(Map<String, String> allRequestParams, String accept) {

		logger.info("requestDataFromPlugin method not implemented in plugin class " + getClass().getName() + " must be implemented in order to submit data.");
		return null;
	}

	/*
	 * Delete data in a plugin. Implementation of this function is provided by the plugin.
	 *
	 */
	default void onDeleteData(Map<String, String> allRequestParams, String contentType) {

		logger.info("deleteDataFromPlugin method not implemented in plugin class " + getClass().getName() + " must be implemented in order to submit data.");
	}
	
	/*
	 * Plugin received notification of a file upload event. Implementation of this function is provided by the plugin.
	 * 
	 */
	default	void onFileUploadEvent(Metadata metadata) {
		
		logger.info("onFileUploadEvent method not implemented in plugin class " + getClass().getName() + ". Must be implemented in order to process the notification."); 
	}
	
}
