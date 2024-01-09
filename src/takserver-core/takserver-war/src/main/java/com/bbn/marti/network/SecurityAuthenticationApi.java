package com.bbn.marti.network;

import com.bbn.marti.config.CAType;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.remote.AuthenticationConfigInfo;
import com.bbn.marti.remote.SecurityConfigInfo;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.service.SecurityManager;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
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
import tak.server.Constants;
import tak.server.ignite.MessagingIgniteBroker;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;


@RestController
public class SecurityAuthenticationApi extends BaseRestController {
    Logger logger = LoggerFactory.getLogger(SecurityAuthenticationApi.class.getName());

    @Autowired
    private SecurityManager securityManager;

    private static GroupManager groupManager;

    private static GroupManager getGroupManager() {
        if (groupManager != null) {
            return groupManager;
        }

        groupManager = SpringContextBeanForApi.getSpringContext().getBean(
            com.bbn.marti.remote.groups.GroupManager.class);
        return groupManager;
    }

    @RequestMapping(value = "/authentication/config", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<AuthenticationConfigInfo>> getAuthConfig() {
        try {
            AuthenticationConfigInfo info = MessagingIgniteBroker.brokerServiceCalls(service -> ((SecurityManager) service)
                .getAuthenticationConfig(), Constants.DISTRIBUTED_SECURITY_MANAGER, SecurityManager.class);

            if (info != null) {
                return new ResponseEntity<ApiResponse<AuthenticationConfigInfo>>(new ApiResponse<AuthenticationConfigInfo>(Constants.API_VERSION, AuthenticationConfigInfo.class.getName(), info), HttpStatus.OK);
            } else {
                return new ResponseEntity<ApiResponse<AuthenticationConfigInfo>>(new ApiResponse<AuthenticationConfigInfo>(Constants.API_VERSION, AuthenticationConfigInfo.class.getName(), null), HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.error("Error getting authentication config: " + e.toString());
            return new ResponseEntity<ApiResponse<AuthenticationConfigInfo>>(new ApiResponse<AuthenticationConfigInfo>(Constants.API_VERSION, AuthenticationConfigInfo.class.getName(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/authentication/config", method = RequestMethod.PUT)
    public ResponseEntity<ApiResponse<String>> modifyAuthConfig(@RequestBody AuthenticationConfigInfo info) {
        try {
            if (CoreConfigFacade.getInstance().isCluster()) {
                CoreConfigFacade.getInstance().modifyAuthenticationConfig(info);
            } else {
                MessagingIgniteBroker.brokerVoidServiceCalls(service -> ((SecurityManager) service)
                    .modifyAuthenticationConfig(info), Constants.DISTRIBUTED_SECURITY_MANAGER, SecurityManager.class);
            }
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, AuthenticationConfigInfo.class.getName(), "Successfully modified authentication config"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, AuthenticationConfigInfo.class.getName(), "Failed to modify authentication config"), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/authentication/config", method = RequestMethod.POST)
    public ResponseEntity<ApiResponse<String>> testAuthConfig() {

        HttpStatus result = HttpStatus.INTERNAL_SERVER_ERROR;

        try {
            if (getGroupManager().testLdap()) {
                result = HttpStatus.OK;
            }
        } catch (Exception e) {
            logger.error("exception in testAuthConfig!", e);
        }

        return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(
            Constants.API_VERSION, AuthenticationConfigInfo.class.getName(),
            "Auth config testing " + (result == HttpStatus.OK ? "passed" : "failed")), result);
    }

    @RequestMapping(value = "/security/config", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<SecurityConfigInfo>> getSecConfig() {
        try {
            SecurityConfigInfo info = securityManager.getSecurityConfig();
            return new ResponseEntity<ApiResponse<SecurityConfigInfo>>(new ApiResponse<SecurityConfigInfo>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), info), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error getting security config: " + e.toString());
            return new ResponseEntity<ApiResponse<SecurityConfigInfo>>(new ApiResponse<SecurityConfigInfo>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), null), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/security/config", method = RequestMethod.PUT)
    public ResponseEntity<ApiResponse<String>> modifySecConfig(@RequestBody SecurityConfigInfo info) {
        try {
            File keystoreFileObj = new File(info.getKeystoreFile());
            File truststoreFileObj = new File(info.getTruststoreFile());
            if (!keystoreFileObj.exists()) {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify config - Keystore File specified does not exist"), HttpStatus.BAD_REQUEST);
            }
            if (!truststoreFileObj.exists()) {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify config - Truststore File specified does not exist"), HttpStatus.BAD_REQUEST);
            }

            if (!FilenameUtils.getExtension(info.getKeystoreFile()).equals("jks")) {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify config - Keystore File specified is not a .jks file"), HttpStatus.BAD_REQUEST);
            }

            if (!FilenameUtils.getExtension(info.getTruststoreFile()).equals("jks")) {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify config - Truststore File specified is not a .jks file"), HttpStatus.BAD_REQUEST);
            }

            if (info.isEnableEnrollment()) {
                if (info.getCaType().equals(CAType.TAK_SERVER.value())) {
                    File signingKeystoreFileObj = new File(info.getSigningKeystoreFile());
                    if (!signingKeystoreFileObj.exists()) {
                        return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify config - Signing Keystore File specified does not exist"), HttpStatus.BAD_REQUEST);
                    }

                    if (!FilenameUtils.getExtension(info.getSigningKeystoreFile()).equals("jks")) {
                        return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify config - Signing Keystore File specified is not a .jks file"), HttpStatus.BAD_REQUEST);
                    }
                } else if (info.getCaType().equals(CAType.MICROSOFT_CA.value())) {
                    File mscaTruststoreFileObj = new File(info.getMscaTruststore());
                    if (!mscaTruststoreFileObj.exists()) {
                        return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify config - MSCA Truststore File specified does not exist"), HttpStatus.BAD_REQUEST);
                    }

                    if (!FilenameUtils.getExtension(info.getMscaTruststore()).equals("jks")) {
                        return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify config - MSCA Truststore File specified is not a .jks file"), HttpStatus.BAD_REQUEST);
                    }
                }
            }

            // If changes are saved during activation of the change the nodes can hit a situation where
            // the first node to complete triggers the CoreConfig update on other nodes before they apply 
            // the update which can lead to state consistency issues
            if (CoreConfigFacade.getInstance().isCluster()) {
                CoreConfigFacade.getInstance().modifySecurityConfig(info);
            } else {
                MessagingIgniteBroker.brokerNonLocalVoidServiceCalls(service -> ((SecurityManager) service)
                    .modifySecurityConfig(info), Constants.DISTRIBUTED_SECURITY_MANAGER, SecurityManager.class);
            }

            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Successfully modified security config"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, SecurityConfigInfo.class.getName(), "Failed to modify security config"), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/security/isSecure", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<String>> isSecure() {
        try {
            Collection<Integer> unsecurePorts = securityManager.getNonSecurePorts();
            if (unsecurePorts.isEmpty()) {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, "isSecure?", "true"), HttpStatus.OK);
            }
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, "isSecure?", unsecurePorts.toString()), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, "isSecure?", null), HttpStatus.BAD_REQUEST);
        }

    }

    @RequestMapping(value = "/security/verifyConfig", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<String>> verifyConfig() {
        try {
            //TODO: make this actually do something
            HashMap<String, Boolean> configVerification;
            configVerification = securityManager.verifyConfiguration();
            if (configVerification.get("keystoreFile") == false) {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, "verifyConfig?", "Failed to verify config - Keystore File specified does not exist"), HttpStatus.BAD_REQUEST);
            } else if (configVerification.get("truststoreFile") == false) {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, "verifyConfig?", "Failed to verify config - Truststore File specified does not exist"), HttpStatus.BAD_REQUEST);
            } else {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, "verifyConfig?", Boolean.TRUE.toString()), HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, "verifyConfig?", null), HttpStatus.BAD_REQUEST);
        }
    }

}
