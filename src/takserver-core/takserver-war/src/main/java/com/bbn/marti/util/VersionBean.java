

package com.bbn.marti.util;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.remote.exception.TakException;
import com.google.common.base.Charsets;
import com.google.gson.Gson;

import tak.server.Constants;
import tak.server.system.ApiDependencyProxy;
import tak.server.util.VersionInfo;

public class VersionBean {
    
    private String ver = null;
    
    private VersionInfo versionInfo = null;
    
    Logger logger = LoggerFactory.getLogger(VersionBean.class);
    
    @EventListener({ContextRefreshedEvent.class})
    private void init() {
    	try {
			logger.info("TAK Server version " + getVer());
		} catch (IOException e) {
			logger.error("error printing TAK Server version", e);
		}
    }
    
    public String getVer() throws IOException {
    	
    	if (ver == null) {
    		synchronized (this) {
    			if (ver == null) {
					ver = IOUtils.toString(
							VersionBean.class.getResourceAsStream(Constants.SHORT_VER_RESOURCE_PATH), Charsets.UTF_8);
    			}
    		}
    	}
    	
    	return ver;
    }
    
	public String getNodeId() {
    	
    	return ApiDependencyProxy.getInstance().serverInfo().getServerId();
	}
	
	public VersionInfo getVersionInfo() {

		if (versionInfo == null) {
			synchronized (this) {
				if (versionInfo == null) {

					try {
						String versionInfoJson = IOUtils.toString(
								VersionBean.class.getResourceAsStream(Constants.VERSION_INFO_JSON_PATH), Charsets.UTF_8);

						versionInfo = new Gson().fromJson(versionInfoJson, VersionInfo.class);

						if (versionInfo == null) {
							throw new IllegalArgumentException("unable to parse version JSON");
						}
						
						versionInfo.setVariant(Constants.FEDERATION_VARIANT);

					} catch (Exception e) {
						
						logger.error("exception parsing version info JSON file", e);
						
						throw new TakException(e);
					}
				}
			}
		}

		return versionInfo;

	}
}
