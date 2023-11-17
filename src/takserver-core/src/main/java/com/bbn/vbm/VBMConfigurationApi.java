package com.bbn.vbm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.Vbm;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.service.DistributedConfiguration;

@Validated
@RestController
@RequestMapping(value = "/vbm/api")
public class VBMConfigurationApi {
	
    Logger logger = LoggerFactory.getLogger(VBMConfigurationApi.class);
    
    @Autowired
    CoreConfig config;

    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public VBMConfigurationModel getVBMConfiguration() {
    	
    	try {
    		Vbm vbm = DistributedConfiguration.getInstance().getVbm();
    		VBMConfigurationModel config = new VBMConfigurationModel();
    		config.setVbmEnabled(vbm.isEnabled());
    		config.setSADisabled(vbm.isDisableSASharing());
    		config.setChatDisabled(vbm.isDisableChatSharing());
			
			return config;
			
		} catch (Exception e) {
			logger.error("Error in getVBMConfiguration: ", e);
			throw new RuntimeException(e);
		}
    }
    
    @RequestMapping(value = "/config", method = RequestMethod.POST)
    public void setVBMConfiguration(@RequestBody VBMConfigurationModel vbmConfigurationModel) {
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("setVBMConfiguration to: {}", vbmConfigurationModel.isVbmEnabled());
    	}
    	
    	try {
    		Vbm vbm = new Vbm();
    		vbm.setReturnCopsWithPublicMissions(config.getRemoteConfiguration().getVbm().isReturnCopsWithPublicMissions());
    		vbm.setDisableChatSharing(vbmConfigurationModel.isChatDisabled());
    		vbm.setDisableSASharing(vbmConfigurationModel.isSADisabled());
    		vbm.setEnabled(vbmConfigurationModel.isVbmEnabled());
    		
    		config.setAndSaveVbmConfiguration(vbm);
		} catch (Exception e) {
			logger.error("Error in setVBMConfiguration: ", e);
			throw new RuntimeException(e);
		}
    }
}
