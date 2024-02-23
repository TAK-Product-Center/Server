package com.bbn.marti.groups;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.bbn.marti.config.Network.Connector;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.util.spring.ThreadLocalRequestHolder;

import com.bbn.marti.remote.config.CoreConfigFacade;

public class AuthenticatorUtil {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_WEBTAK = "ROLE_WEBTAK";
    public static final String ROLE_NON_ADMIN_UI = "ROLE_NON_ADMIN_UI";
        
    public static void setUserRolesBasedOnRequestPort(User user, Logger logger) {
    	
    	boolean portHasAdminEnabled = false;
    	boolean portHasWebtakEnabled = false;
    	boolean portHasNonAdminUIEnabled = false;
    	
        int requestPort = -1;

        HttpServletRequest request = ThreadLocalRequestHolder.getRequest();
        if (request != null) {
            requestPort = request.getLocalPort();
            if (logger.isDebugEnabled()) {
                logger.debug("RequestURI: {}", request.getRequestURI());
            }

            if (logger.isDebugEnabled()) {
                logger.debug("requestPort: {}", requestPort);
            }

            List<Connector> connectors = CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getConnector();

            Connector configConnectorOnTheRequestedPort = null;
            for (Connector connector : connectors) {
                if (connector.getPort() == requestPort) {
                    configConnectorOnTheRequestedPort = connector;
                    break;
                }
            }

            if (configConnectorOnTheRequestedPort == null) {
                throw new SecurityException("Connection is not allowed");
            }

            if (configConnectorOnTheRequestedPort.isEnableAdminUI()) {
                portHasAdminEnabled = true;
            }
            if (configConnectorOnTheRequestedPort.isEnableWebtak()) {
                portHasWebtakEnabled = true;
            }
            if (configConnectorOnTheRequestedPort.isEnableNonAdminUI()) {
                portHasNonAdminUIEnabled = true;
            }
        }
        
        // remove role ROLE_ADMIN if the port does not allow admin access
        if (user.getAuthorities().contains(ROLE_ADMIN) && portHasAdminEnabled == false) {
            user.getAuthorities().remove(ROLE_ADMIN);
            if (logger.isDebugEnabled()) {
                logger.debug("Removed role {}", ROLE_ADMIN);
            }
        }
        
        if (user.getAuthorities().isEmpty()) {
            user.getAuthorities().add("ROLE_ANONYMOUS");
        }

        // add role ROLE_WEBTAK if the port has webtak enabled
        if (portHasWebtakEnabled) {
            user.getAuthorities().add(ROLE_WEBTAK);
        }
        
        // add role ROLE_NON_ADMIN_UI if the port has non-admin-UI enabled
        if (portHasNonAdminUIEnabled) {
            user.getAuthorities().add(ROLE_NON_ADMIN_UI);
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("user.getAuthorities(): {}",user.getAuthorities());
        }
    }
}
