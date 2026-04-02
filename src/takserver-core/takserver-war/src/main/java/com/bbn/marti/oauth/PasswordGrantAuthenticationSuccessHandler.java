package com.bbn.marti.oauth;

import com.bbn.marti.util.spring.CorsHeaders;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.bbn.marti.remote.config.CoreConfigFacade;


public class PasswordGrantAuthenticationSuccessHandler
        implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenHttpResponseConverter =
            new OAuth2AccessTokenResponseHttpMessageConverter();

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                 Authentication authentication) throws IOException, ServletException {

        OAuth2AccessTokenAuthenticationToken accessTokenAuthentication =
                (OAuth2AccessTokenAuthenticationToken) authentication;

        OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
        OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();
        Map<String, Object> additionalParameters = accessTokenAuthentication.getAdditionalParameters();

        OAuth2AccessTokenResponse.Builder builder =
                OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
                        .tokenType(accessToken.getTokenType())
                        .scopes(accessToken.getScopes());
        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            builder.expiresIn(ChronoUnit.SECONDS.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
        }
        if (refreshToken != null) {
            builder.refreshToken(refreshToken.getTokenValue());
        }
        if (!CollectionUtils.isEmpty(additionalParameters)) {
            builder.additionalParameters(additionalParameters);
        }
        OAuth2AccessTokenResponse accessTokenResponse = builder.build();
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);

        AuthCookieUtils.SameSite sameSite = AuthCookieUtils.SameSite.Strict;
        boolean partitioned = false;

        // Set SameSite=None and enable the Partitioned attribute when responding to cross-origin requests allowing credentials
        if (CorsHeaders.checkAndApplyCorsForConnector(request, response) && CorsHeaders.checkAllowCredentials(response)) {
            sameSite = AuthCookieUtils.SameSite.None;
            partitioned = true;
        }


        for (ResponseCookie cookie : AuthCookieUtils.createCookiesWithMaxSize(
                OAuth2TokenType.ACCESS_TOKEN.getValue(), accessTokenResponse.getAccessToken().getTokenValue(), -1, sameSite == AuthCookieUtils.SameSite.Strict, request.isSecure())) {
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookiePartitioned(cookie, partitioned));
        }

        this.accessTokenHttpResponseConverter.write(accessTokenResponse, null, httpResponse);
    }
}
