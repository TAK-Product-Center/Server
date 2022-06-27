package com.bbn.marti.oauth;

import com.bbn.marti.util.spring.SpringContextBeanForApi;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;


public class TAKTokenStore implements TokenStore {

    private static TokenStore tokenStore;

    private static TokenStore tokenStore() {
        if (tokenStore != null) {
            return tokenStore;
        }

        boolean connected = true;

        DataSource dataSource = SpringContextBeanForApi.getSpringContext().getBean(DataSource.class);
        try {
            dataSource.getConnection();
        } catch (SQLException e) {
            connected = false;
        }

        if (connected) {
            tokenStore = new JdbcTokenStore(dataSource);
            ((JdbcTokenStore)tokenStore).setAuthenticationKeyGenerator(new TAKAuthenticationKeyGenerator());
        } else {
            tokenStore = new InMemoryTokenStore();
            ((InMemoryTokenStore)tokenStore).setAuthenticationKeyGenerator(new TAKAuthenticationKeyGenerator());
        }

        return tokenStore;
    }

    @Override
    public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
        return tokenStore().readAuthentication(token);
    }

    @Override
    public OAuth2Authentication readAuthentication(String token) {
        return tokenStore().readAuthentication(token);
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        tokenStore().storeAccessToken(token, authentication);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String tokenValue) {
        return tokenStore().readAccessToken(tokenValue);
    }

    @Override
    public void removeAccessToken(OAuth2AccessToken token) {
        tokenStore().removeAccessToken(token);
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        tokenStore().storeRefreshToken(refreshToken, authentication);
    }

    @Override
    public OAuth2RefreshToken readRefreshToken(String tokenValue) {
        return tokenStore().readRefreshToken(tokenValue);
    }

    @Override
    public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
        return tokenStore().readAuthenticationForRefreshToken(token);
    }

    @Override
    public void removeRefreshToken(OAuth2RefreshToken token) {
        tokenStore().removeRefreshToken(token);
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        tokenStore().removeAccessTokenUsingRefreshToken(refreshToken);
    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        return tokenStore().getAccessToken(authentication);
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName) {
        return tokenStore().findTokensByClientIdAndUserName(clientId, userName);
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
        return tokenStore().findTokensByClientId(clientId);
    }
}
