package com.bbn.marti.util.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bbn.marti.config.Network;
import com.bbn.marti.remote.config.CoreConfigFacade;

public class CustomHeaders {

    public static void checkAndApplyCustomHeadersForConnector(HttpServletRequest request, HttpServletResponse response) {
        for (Network.Connector connector : CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getConnector()) {
            if (request.getLocalPort() == connector.getPort()) {
                if (!connector.getHeader().isEmpty()) {
                    connector.getHeader().stream().forEach(header -> {
                        response.setHeader(header.getKey(), header.getValue());
                    });
                }
                return;
            }
        }
    }
}
