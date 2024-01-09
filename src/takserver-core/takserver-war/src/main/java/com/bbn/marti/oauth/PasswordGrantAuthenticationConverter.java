package com.bbn.marti.oauth;

import com.bbn.marti.remote.config.CoreConfigFacade;
import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;


public class PasswordGrantAuthenticationConverter implements AuthenticationConverter {
    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        // grant_type (REQUIRED)
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!OAuth2ParameterNames.PASSWORD.equals(grantType)) {
            return null;
        }

        MultiValueMap<String, String> parameters = getParameters(request);

        // username (REQUIRED)
        String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
        if (Strings.isNullOrEmpty(username)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // password (REQUIRED)
        String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
        if (Strings.isNullOrEmpty(password)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(Duration.ofMinutes(
                        CoreConfigFacade.getInstance().getRemoteConfiguration()
                                .getNetwork().getHttpSessionTimeoutMinutes()))
                .build();

        RegisteredClient loginClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(username)
                .clientSecret(password)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .tokenSettings(tokenSettings)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        return new OAuth2ClientAuthenticationToken(
                loginClient, ClientAuthenticationMethod.NONE, password);
    }

    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            if (values.length > 0) {
                for (String value : values) {
                    parameters.add(key, value);
                }
            }
        });
        return parameters;
    }

}
