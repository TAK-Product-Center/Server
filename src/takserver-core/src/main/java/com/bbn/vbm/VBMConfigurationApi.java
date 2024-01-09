package com.bbn.vbm;

import com.bbn.marti.remote.CoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.Vbm;

import com.bbn.marti.remote.config.CoreConfigFacade;

@Validated
@RestController
@RequestMapping(value = "/vbm/api")
public class VBMConfigurationApi {
	
    Logger logger = LoggerFactory.getLogger(VBMConfigurationApi.class);
    
    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public VBMConfigurationModel getVBMConfiguration() {
    	
    	try {
    		Vbm vbm = CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm();
    		VBMConfigurationModel config = new VBMConfigurationModel();
    		config.setVbmEnabled(vbm.isEnabled());
    		config.setSADisabled(vbm.isDisableSASharing());
    		config.setChatDisabled(vbm.isDisableChatSharing());
			config.setIsmStrictEnforcing(vbm.isIsmStrictEnforcing());
			config.setIsmUrl(vbm.getIsmUrl());
    		config.setNetworkClassification(vbm.getNetworkClassification());
			
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
    		vbm.setReturnCopsWithPublicMissions(CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().isReturnCopsWithPublicMissions());
            vbm.setIsmConnectTimeoutSeconds(CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().getIsmConnectTimeoutSeconds());
            vbm.setIsmReadTimeoutSeconds(CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().getIsmReadTimeoutSeconds());
    		vbm.setDisableChatSharing(vbmConfigurationModel.isChatDisabled());
    		vbm.setDisableSASharing(vbmConfigurationModel.isSADisabled());
    		vbm.setEnabled(vbmConfigurationModel.isVbmEnabled());
    		vbm.setIsmStrictEnforcing(vbmConfigurationModel.isIsmStrictEnforcing());
			vbm.setIsmUrl(vbmConfigurationModel.getIsmUrl());
    		vbm.setNetworkClassification(vbmConfigurationModel.getNetworkClassification());
    		
    		CoreConfigFacade.getInstance().setAndSaveVbmConfiguration(vbm);
		} catch (Exception e) {
			logger.error("Error in setVBMConfiguration: ", e);
			throw new RuntimeException(e);
		}
    }

	@RequestMapping(value = "/classification", method = RequestMethod.GET)
	public String getVBMNetworkClassification() {

		try {
			String classification = null;
			Vbm vbm = CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm();
			if (vbm != null) {
				classification = vbm.getNetworkClassification();
			}
			return classification;
		} catch (Exception e) {
			logger.error("Error in getVBMNetworkClassification: ", e);
			throw new RuntimeException(e);
		}
	}
}
