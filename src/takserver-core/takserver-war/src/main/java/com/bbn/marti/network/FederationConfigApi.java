package com.bbn.marti.network;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.remote.FederationConfigInfo;
import com.bbn.marti.remote.FederationConfigInterface;
import com.bbn.marti.remote.FederationManager;

import tak.server.Constants;
import tak.server.ignite.MessagingIgniteBroker;

@RestController
public class FederationConfigApi extends BaseRestController{

	@Autowired
	private FederationConfigInterface federationInterface;

	@Autowired
	private FederationManager fedManagerInterface;

	private final Logger logger = LoggerFactory.getLogger(FederationConfigInterface.class.getName());

	@RequestMapping(value = "/federationconfig", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<FederationConfigInfo>> getFederationConfig(){
        try {
            FederationConfigInfo info = federationInterface.getFederationConfig();
            return new ResponseEntity<ApiResponse<FederationConfigInfo>>(new ApiResponse<FederationConfigInfo>(Constants.API_VERSION, FederationConfigInfo.class.getName(), info), HttpStatus.OK);
        }
        catch(Exception e){
        	logger.error("Error getting federation config: " + e.toString());
        	return new ResponseEntity<ApiResponse<FederationConfigInfo>>(new ApiResponse<FederationConfigInfo>(Constants.API_VERSION, FederationConfigInfo.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	@RequestMapping(value = "/federationconfig", method = RequestMethod.PUT)
    public ResponseEntity<ApiResponse<String>> modifyFederationConfig(@RequestBody FederationConfigInfo info){
        try {
        	File truststoreFileObj = new File(info.getTruststorePath());
        	if (!truststoreFileObj.exists()) {
        		return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, FederationConfigInfo.class.getName(), "Failed to modify Federation config - Truststore File specified does not exist"),
        				HttpStatus.BAD_REQUEST);
        	}
        	if (!FilenameUtils.getExtension(info.getTruststorePath()).equals("jks")) {
        		return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, FederationConfigInfo.class.getName(), "Failed to modify Federation config - Truststore File specified is not a .jks file"),
        				HttpStatus.BAD_REQUEST);
        	}
            federationInterface.modifyFederationConfig(info);
            MessagingIgniteBroker.brokerVoidServiceCalls((s) -> ((FederationManager) s).reconfigureFederation(), Constants.DISTRIBUTED_FEDERATION_MANAGER, FederationManager.class);
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, FederationConfigInfo.class.getName(), "Successfully modified Federation config"), HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, FederationConfigInfo.class.getName(), "Failed to modify Federation config"), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/federationconfig/verify", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<Boolean>> verifyFederationTruststore() {
        try {
            boolean isValidFedTruststore = federationInterface.verifyFederationTruststore();
            if (isValidFedTruststore) {
                return new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, FederationConfigInfo.class.getName(), Boolean.TRUE), HttpStatus.OK);
            } else {
                return new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, FederationConfigInfo.class.getName(), Boolean.FALSE), HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e) {
            return new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, FederationConfigInfo.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
