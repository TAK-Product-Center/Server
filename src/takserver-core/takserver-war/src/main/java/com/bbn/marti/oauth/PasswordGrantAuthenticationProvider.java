package com.bbn.marti.oauth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

public class PasswordGrantAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    private AuthenticationProvider authenticationProvider;

    public PasswordGrantAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                                 OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2ClientAuthenticationToken  oAuth2ClientAuthenticationToken =
                (OAuth2ClientAuthenticationToken) authentication;

        // Ensure the client is authenticated
        authenticationProvider.authenticate(authentication);

        OAuth2ClientAuthenticationToken clientPrincipal = (OAuth2ClientAuthenticationToken)authentication;
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        // Generate the access token
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(clientPrincipal)
                //.authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizationGrant(authentication)
                .build();

        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "The token generator failed to generate the access token.", null);
            throw new OAuth2AuthenticationException(error);
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(), null);

        // Initialize the OAuth2Authorization
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(clientPrincipal.getName())
                .authorizationGrantType(AuthorizationGrantType.PASSWORD);
        if (generatedAccessToken instanceof ClaimAccessor) {
            authorizationBuilder.token(accessToken, (metadata) ->
                    metadata.put(
                            OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                            ((ClaimAccessor) generatedAccessToken).getClaims())
            );
        } else {
            authorizationBuilder.accessToken(accessToken);
        }
        OAuth2Authorization authorization = authorizationBuilder.build();

        // Save the OAuth2Authorization
        this.authorizationService.save(authorization);

        return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public void setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }
}


