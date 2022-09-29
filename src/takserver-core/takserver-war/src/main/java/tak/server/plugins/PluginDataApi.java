/*
 * Copyright (c) 2013-2015 Raytheon BBN Technologies. Licensed to US Government with unlimited rights.
 */

package tak.server.plugins;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
			@RequestParam(value = "scope", required = false) String scope,
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
				pluginManager.submitDataToPlugin(pluginClassName, scope, requestBodyString, contentType);
			} catch (Exception e) {
				throw new TakException("error accesing PluginManager process - is it running?", e);
			}

		} catch (Exception e) {


			logger.error("exception submitting plugin data", e);
		}

		return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, PluginInfo.class.getName(), "data submitted to " + pluginClassName), HttpStatus.OK);
	}

}