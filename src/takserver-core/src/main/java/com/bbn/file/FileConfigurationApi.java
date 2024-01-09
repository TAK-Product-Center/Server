package com.bbn.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.remote.config.CoreConfigFacade;


@Validated
@RestController
@RequestMapping(value = "/files/api")
public class FileConfigurationApi {
	
    Logger logger = LoggerFactory.getLogger(FileConfigurationApi.class);

    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public FileConfigurationModel getFileConfiguration() {
    	
    	try {
    		int uploadSizeLimit = CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB();
    		FileConfigurationModel config = new FileConfigurationModel();
    		config.setUploadSizeLimit(uploadSizeLimit);			
			return config;
		} catch (Exception e) {
			logger.error("Error in getFileConfiguration: ", e);
			throw new RuntimeException(e);
		}
    }
    
    @RequestMapping(value = "/config", method = RequestMethod.POST)
    public void setFileConfiguration(@RequestBody FileConfigurationModel fileConfigurationModel) {
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("setFileConfiguration to: {}", fileConfigurationModel.getUploadSizeLimit());
    	}
    	
    	try {
			CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().setEnterpriseSyncSizeLimitMB(fileConfigurationModel.getUploadSizeLimit());
			CoreConfigFacade.getInstance().saveChangesAndUpdateCache();
		} catch (Exception e) {
			logger.error("Error in setFileConfiguration: ", e);
			throw new RuntimeException(e);
		}
    }
}
