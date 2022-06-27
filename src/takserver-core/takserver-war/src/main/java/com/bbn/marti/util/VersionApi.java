package com.bbn.marti.util;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;

import tak.server.Constants;
import tak.server.system.ApiDependencyProxy;
import tak.server.util.VersionInfo;

@RestController
public class VersionApi extends BaseRestController {
	
	private static final Logger logger = LoggerFactory.getLogger(VersionApi.class);
	
	@Autowired
	private VersionBean versionBean;

	@Autowired
	private HttpServletRequest request;

	@RequestMapping(value = "/version", method = RequestMethod.GET)
	public String getVersion() throws IOException {
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("version: " + versionBean.getVer());
    	}
    	
		return versionBean.getVer();
	}
	
	@RequestMapping(value = "/version/info", method = RequestMethod.GET)
	public VersionInfo getVersionInfo() throws IOException {
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("versionInfo: " + versionBean.getVersionInfo());
    	}
    	
		return versionBean.getVersionInfo();
	}
    
    @RequestMapping(value = "/node/id", method = RequestMethod.GET)
	public String getNodeId() {
    	
    	return ApiDependencyProxy.getInstance().serverInfo().getServerId();
	}

	private class ServerConfig {
    	@SuppressWarnings("unused")
		public String version;
    	@SuppressWarnings("unused")
		public String api;
    	@SuppressWarnings("unused")
		public String hostname;
	}

	@RequestMapping(value = "/version/config", method = RequestMethod.GET)
	public ApiResponse<ServerConfig> getVersionConfig() throws IOException, URISyntaxException {

		ServerConfig serverConfig = new ServerConfig();
		serverConfig.api = Constants.API_VERSION;
		serverConfig.hostname = new URI(request.getRequestURL().toString()).getHost();

    	String version = versionBean.getVer().
				replace("\n", "").replace("TAK Server", "").trim();

    	String[] tokens = version.split("-");
		if (tokens != null && tokens.length >= 3) {
			serverConfig.version = tokens[0] + "." + tokens[2] + "-" + tokens[1];
		}

		return new ApiResponse<ServerConfig>(Constants.API_VERSION, ServerConfig.class.getSimpleName(), serverConfig);
	}
}
