package com.bbn.marti.util.spring;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import tak.server.Constants;

import javax.servlet.http.HttpServletRequest;

public class TakPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter  {

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        return Constants.ANONYMOUS_ROLE;
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "N/A";
    }
}