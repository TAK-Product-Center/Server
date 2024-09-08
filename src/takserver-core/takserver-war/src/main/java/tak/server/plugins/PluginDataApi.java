/*
 * Copyright (c) 2013-2015 Raytheon BBN Technologies. Licensed to US Government with unlimited rights.
 */

package tak.server.plugins;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.sync.api.MissionApi.ValidatedBy;
import com.bbn.marti.util.spring.RequestHolderBean;
import com.google.common.base.Charsets;

import tak.server.Constants;
import tak.server.PluginManager;

/*
 * 
 * REST API for plugin data
 * 
 * base path is /Marti/api
 * 
 */
@RestController
public class PluginDataApi extends BaseRestController {

	private static final Logger logger = LoggerFactory.getLogger(PluginDataApi.class);

	@Autowired(required = false)	
	private PluginManager pluginManager;
	
	@Autowired
	private RequestHolderBean requestHolderBean;

	/*
	 * Submit generic to a plugin for processing. Data must be UTF-8 text. 
	 */
	@RequestMapping(value = "/plugins/{name:.+}/submit", method = RequestMethod.PUT)
	public ResponseEntity<ApiResponse<String>> submitToPluginUTF8(
			@PathVariable("name") @ValidatedBy("MartiSafeString") @NotNull String pluginClassName,
			@RequestParam Map<String,String> allRequestParams,
			@RequestBody(required = true) byte[] requestBodyBytes) {

		// TODO: handle IllegalArgumentException better
		try {
			if (requestBodyBytes == null) {
				throw new IllegalArgumentException("null data submitted to plugin - ignoring.");
			}

			String contentType =  requestHolderBean.getRequest().getHeader("content-type");

			String requestBodyString = new String(requestBodyBytes, Charsets.UTF_8);

			logger.info("submit " + pluginClassName);
			logger.info("body string: " + requestBodyString);
			logger.info("content type: " + contentType);

			try {
				pluginManager.submitDataToPlugin(pluginClassName, allRequestParams, requestBodyString, contentType);
			} catch (Exception e) {
				throw new TakException("error accesing PluginManager process - is it running?", e);
			}

		} catch (Exception e) {
			logger.error("exception submitting plugin data", e);
		}

		return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, PluginInfo.class.getName(), "data submitted to " + pluginClassName), HttpStatus.OK);
	}

	/*
	 * Submit generic to a plugin for processing. Data must be UTF-8 text. 
	 */
	@RequestMapping(value = "/plugins/{name:.+}/submit/result", method = RequestMethod.PUT)
	public ResponseEntity<String> submitToPluginUTF8WithResult(
			@PathVariable("name") @ValidatedBy("MartiSafeString") @NotNull String pluginClassName,
			@RequestParam Map<String,String> allRequestParams,
			@RequestBody(required = true) byte[] requestBodyBytes) {

		PluginResponse result = null;
		// TODO: handle IllegalArgumentException better
		try {
			if (requestBodyBytes == null) {
				throw new IllegalArgumentException("null data submitted to plugin - ignoring.");
			}

			String contentType =  requestHolderBean.getRequest().getHeader("content-type");

			String requestBodyString = new String(requestBodyBytes, Charsets.UTF_8);

			logger.info("submit " + pluginClassName);
			logger.info("body string: " + requestBodyString);
			logger.info("content type: " + contentType);

			try {
				result = pluginManager.submitDataToPluginWithResult(pluginClassName, allRequestParams, requestBodyString, contentType);
			} catch (Exception e) {
				throw new TakException("error accesing PluginManager process - is it running?", e);
			}

		} catch (Exception e) {
			logger.error("exception submitting plugin data", e);
		}
		
		if (result == null) {
			return 	new ResponseEntity<String>("result from plugin was null", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if (result.getData() == null) {
			return 	new ResponseEntity<String>("result data from plugin was null",  HttpStatus.BAD_REQUEST);
		}

		HttpHeaders headers = new HttpHeaders();
		if (result.getContentType() != null) {
			headers.setContentType(org.springframework.http.MediaType.parseMediaType(result.getContentType()));
		}
		return 	new ResponseEntity<String>(result.getData(), headers, HttpStatus.OK);
	}

	
	/*
	 * Update generic to a plugin for processing. Data must be UTF-8 text.
	 */
	@RequestMapping(value = "/plugins/{name:.+}/submit", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<String>> updateInPlugin(
			@PathVariable("name") @ValidatedBy("MartiSafeString") @NotNull String pluginClassName,
			@RequestParam Map<String,String> allRequestParams,
			@RequestBody(required = true) byte[] requestBodyBytes) {

		try {
			if (requestBodyBytes == null) {
				throw new IllegalArgumentException("null data updated in plugin - ignoring.");
			}

			String contentType =  requestHolderBean.getRequest().getHeader("content-type");

			String requestBodyString = new String(requestBodyBytes, Charsets.UTF_8);

			logger.info("submit " + pluginClassName);
			logger.info("body string: " + requestBodyString);
			logger.info("content type: " + contentType);

			try {
				pluginManager.updateDataInPlugin(pluginClassName, allRequestParams, requestBodyString, contentType);
			} catch (Exception e) {
				return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, PluginInfo.class.getName(), "error accesing PluginManager process - is it running?"), HttpStatus.BAD_REQUEST);
			}

		} catch (Exception e) {
			logger.error("exception updating plugin data", e);
			return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, PluginInfo.class.getName(), e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, PluginInfo.class.getName(), "data updated in " + pluginClassName), HttpStatus.OK);
	}
	
	/*
	 * Request generic data from a plugin for processing.
	 */
	@RequestMapping(value = "/plugins/{name:.+}/submit", method = RequestMethod.GET)
	public ResponseEntity<String> requestFromPlugin(
			@PathVariable("name") @ValidatedBy("MartiSafeString") @NotNull String pluginClassName,
			@RequestParam Map<String,String> allRequestParams) {

		PluginResponse result = null;
		// TODO: handle IllegalArgumentException better
		try {

			String accept =  requestHolderBean.getRequest().getHeader("accept");

			logger.info("submit " + pluginClassName);
			logger.info("accept " + accept);

			try {
				result = pluginManager.requestDataFromPlugin(pluginClassName, allRequestParams, accept);
			} catch (Exception e) {
				throw new TakException("error accesing PluginManager process - is it running?", e);
			}

		} catch (Exception e) {
			logger.error("exception requesting plugin data", e);
			return 	new ResponseEntity<String>("error retrieving data from plugin", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if (result == null || result.getData() == null) {
			return 	new ResponseEntity<String>("result from plugin was null", HttpStatus.BAD_REQUEST);
		}
		
		HttpHeaders headers = new HttpHeaders();
		if (result.getContentType() != null) {
			headers.setContentType(org.springframework.http.MediaType.parseMediaType(result.getContentType()));
		}

		return 	new ResponseEntity<String>(result.getData(), headers, HttpStatus.OK);
	}
	
	/*
	 * Delete generic data from a plugin.
	 */
	@RequestMapping(value = "/plugins/{name:.+}/submit", method = RequestMethod.DELETE)
	public ResponseEntity<ApiResponse<String>> deleteFromPlugin(
			@PathVariable("name") @ValidatedBy("MartiSafeString") @NotNull String pluginClassName,
			@RequestParam Map<String,String> allRequestParams) {

		try {

			String contentType =  requestHolderBean.getRequest().getHeader("content-type");

			logger.info("submit " + pluginClassName);
			logger.info("content type: " + contentType);

			try {
				pluginManager.deleteDataFromPlugin(pluginClassName, allRequestParams, contentType);
			} catch (Exception e) {
				return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, PluginInfo.class.getName(), "error accesing PluginManager process - is it running?"), HttpStatus.BAD_REQUEST);
			}

		} catch (Exception e) {

			logger.error("exception requesting plugin data", e);
			return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, PluginInfo.class.getName(), e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, PluginInfo.class.getName(), "deleted data from  " + pluginClassName), HttpStatus.OK);
	}
	
}