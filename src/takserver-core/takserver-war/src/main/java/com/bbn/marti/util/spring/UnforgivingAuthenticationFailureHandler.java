package com.bbn.marti.util.spring;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;

/*
 * 
 * Authentication failure handler that lets the exception propagate, instead of trying other filters in the chain. 
 * 
 */
public class UnforgivingAuthenticationFailureHandler implements org.springframework.security.web.authentication.AuthenticationFailureHandler {
    
    public UnforgivingAuthenticationFailureHandler() { }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        throw authException;
    }
}
