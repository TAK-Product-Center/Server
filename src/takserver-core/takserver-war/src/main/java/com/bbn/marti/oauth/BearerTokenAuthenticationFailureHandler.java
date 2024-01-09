package com.bbn.marti.oauth;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import java.io.IOException;


public class BearerTokenAuthenticationFailureHandler
        implements org.springframework.security.web.authentication.AuthenticationFailureHandler {

    protected static final Logger logger = LoggerFactory.getLogger(BearerTokenAuthenticationFailureHandler.class);

    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                 AuthenticationException exception) throws IOException, ServletException {

        if (logger.isDebugEnabled()) {
            logger.debug("onAuthenticationFailure", exception.getCause());
        }

        if (exception.getCause() instanceof ExpiredJwtException) {
            response.setHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    OAuth2TokenType.ACCESS_TOKEN.getValue(), null, 0, true).toString());
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "must-revalidate, max-age=0, no-cache, no-store");
            response.setDateHeader("Expires", 0);
            response.setHeader("Location", "/login/refresh");
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        } else if (exception.getCause() instanceof AuthenticationException) {
            SecurityContextHolder.clearContext();
            AuthCookieUtils.logout(request, response);
            throw exception;
        }
    }

}
