

package tak.server.plugins;

import java.util.Collection;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;

import tak.server.Constants;
import tak.server.PluginManager;


/*
 * 
 * API providing access to plugin manager for the plugin management UI
 * 
 * base path is /Marti/api
 * 
 */
@RestController
public class PluginManagerApi extends BaseRestController {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PluginManagerApi.class);

	// don't let absence of plugin manager block startup 
	@Autowired(required = false)
	private PluginManager pluginManager;

	/*
	 * get info about all registered plugins
	 */
	@RequestMapping(value = "/plugins/info/all", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<Collection<PluginInfo>>> getAllPluginInfo() {
		ResponseEntity<ApiResponse<Collection<PluginInfo>>> result = null;

		if (pluginManager == null) {
			result = new ResponseEntity<ApiResponse<Collection<PluginInfo>>>(new ApiResponse<Collection<PluginInfo>>(Constants.API_VERSION, PluginInfo.class.getName(), new LinkedList<PluginInfo>()), HttpStatus.OK);
		}
		try {
			result = new ResponseEntity<ApiResponse<Collection<PluginInfo>>>(new ApiResponse<Collection<PluginInfo>>(Constants.API_VERSION, PluginInfo.class.getName(), pluginManager.getAllPluginInfo()), HttpStatus.OK);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Exception getting registered plugins. Plugin manager is not accessible ");
			}
		}

		if (result == null) {
			result = new ResponseEntity<ApiResponse<Collection<PluginInfo>>>(new ApiResponse<Collection<PluginInfo>>(Constants.API_VERSION, PluginInfo.class.getName(), new LinkedList<PluginInfo>()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}
	
	@RequestMapping(value = "/plugins/info/all/started", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<Boolean>> changeAllPluginStartedStatus(@RequestParam("status") boolean status) {
		ResponseEntity<ApiResponse<Boolean>> result = null;
		try {
			if (status) {
				pluginManager.startAllPlugins();
			} else {
				pluginManager.stopAllPlugins();
			}
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), status), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception changing plugin status.", e);
		}

		if (result == null) {
			//This would be an error condition
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), status), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}
	 
	@RequestMapping(value = "/plugins/info/started", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<Boolean>> changePluginStartedStatus(@RequestParam("name") String name, @RequestParam("status") boolean status) {
		ResponseEntity<ApiResponse<Boolean>> result = null;
		try {
			if (status) {
				pluginManager.startPluginByName(name);
			} else {
				pluginManager.stopPluginByName(name);
			}
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), status), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception changing plugin status.", e);
		}

		if (result == null) {
			//This would be an error condition
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), status), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}
	
	@RequestMapping(value = "/plugins/info/enabled", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<Boolean>> changePluginEnabledSetting(@RequestParam("name") String name, @RequestParam("status") boolean isEnabled) {
		ResponseEntity<ApiResponse<Boolean>> result = null;
		try {
			pluginManager.setPluginEnabled(name, isEnabled);

			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), isEnabled), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception changing plugin status.", e);
		}

		if (result == null) {
			//This would be an error condition
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), isEnabled), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}
	
	@RequestMapping(value = "/plugins/info/archive", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<Boolean>> changePluginArchiveSetting(@RequestParam("name") String name, @RequestParam("archiveEnabled") boolean archiveEnabled) {
		ResponseEntity<ApiResponse<Boolean>> result = null;
		try {
			pluginManager.setPluginArchive(name, archiveEnabled);

			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), archiveEnabled), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception changing plugin status.", e);
		}

		if (result == null) {
			//This would be an error condition
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), archiveEnabled), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

}