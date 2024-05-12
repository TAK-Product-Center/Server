package com.bbn.marti.util.spring;

import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bbn.marti.config.Network;
import com.bbn.marti.remote.config.CoreConfigFacade;

public class CorsHeaders {

    public static boolean checkAndApplyCorsForConnector(HttpServletRequest request, HttpServletResponse response) {
        for (Network.Connector connector : CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getConnector()) {
            if (request.getLocalPort() == connector.getPort() && !Strings.isNullOrEmpty(connector.getAllowOrigins())) {
                response.setHeader("Access-Control-Allow-Origin", connector.getAllowOrigins());

                if (!Strings.isNullOrEmpty(connector.getAllowHeaders())) {
                    response.setHeader("Access-Control-Allow-Headers", connector.getAllowHeaders());
                }

                if (!Strings.isNullOrEmpty(connector.getAllowMethods())) {
                    response.setHeader("Access-Control-Allow-Methods", connector.getAllowMethods());
                }

                if (connector.isAllowCredentials()) {
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                }

                return true;
            }
        }
        return false;
    }

    public static boolean checkAllowCredentials(HttpServletResponse response) {
        if (!Strings.isNullOrEmpty(response.getHeader("Access-Control-Allow-Credentials")))
            return true;
        return false;
    }

}
