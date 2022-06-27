package com.bbn.marti.oauth;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;


public class TAKClientDetailsService implements ClientDetailsService {


    public final static String defaultClientId = "TAK";
    private final String defaultScope = "rw"; // not checking for scope yet but need value
    private final String defaultResourceIds = "";
    private final String defaultRedirectUri = "";
    private final String defaultSecret = "";
    private final String defaultAuthorities = "ROLE_ANONYMOUS";
    private final int defaultAccessTokenValiditySeconds = 86400; // 24 hrs
    private final int defaultRefreshTokenValiditySeconds = 86400; // 24 hrs
    private final String defaultGrantTypes = "password,client_credentials";
    private CoreConfig coreConfig;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {

        String scope = defaultScope;
        String resourceIds = defaultResourceIds;
        String redirectUri = defaultRedirectUri;
        String secret = defaultSecret;
        String authorities = defaultAuthorities;
        String grantTypes = defaultGrantTypes;
        int accessTokenValiditySeconds = defaultAccessTokenValiditySeconds;
		int refreshTokenValiditySeconds = accessTokenValiditySeconds;

        Oauth.Client clientConfig = loadClientFromConfig(clientId);
        if (clientConfig != null) {
            if (clientConfig.getScope() != null) { scope = clientConfig.getScope(); }
            if (clientConfig.getResourceIds() != null) { resourceIds = clientConfig.getResourceIds(); }
            if (clientConfig.getRedirectUri() != null) { redirectUri = clientConfig.getRedirectUri(); }
            if (clientConfig.getSecret() != null) { secret = clientConfig.getSecret(); }
            if (clientConfig.getAuthorities() != null) { authorities = clientConfig.getAuthorities(); }
            if (clientConfig.getRefreshTokenValidity() != null) { refreshTokenValiditySeconds = clientConfig.getRefreshTokenValidity(); }
            if (clientConfig.getAuthorizedGrantTypes() != null) { grantTypes = clientConfig.getAuthorizedGrantTypes(); }
        } else {
            clientId = defaultClientId;
        }
		
		if (coreConfig() != null && coreConfig().getRemoteConfiguration() != null) {
			accessTokenValiditySeconds = coreConfig().getRemoteConfiguration().getNetwork().getHttpSessionTimeoutMinutes() * 60;
			refreshTokenValiditySeconds = accessTokenValiditySeconds;
		}

        BaseClientDetails clientDetails = new BaseClientDetails(
                clientId, resourceIds, scope, grantTypes,authorities, redirectUri);
        clientDetails.setClientSecret(secret);
        clientDetails.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
        clientDetails.setRefreshTokenValiditySeconds(refreshTokenValiditySeconds);

        return clientDetails;
    }

    private Oauth.Client loadClientFromConfig(String clientId) {
        if (oauthConfig() == null || oauthConfig().getClient() == null) {
            return null;
        }

        for (Oauth.Client client : oauthConfig().getClient()) {
            if (clientId.compareTo(client.getClientId()) == 0) {
                return client;
            }
        }

        return null;
    }

    private Oauth oauthConfig() {
        if (coreConfig() == null ||
            coreConfig().getRemoteConfiguration() == null ||
            coreConfig().getRemoteConfiguration().getAuth()  == null ||
            coreConfig().getRemoteConfiguration().getAuth().getOauth() == null) {
            return null;
        }

        return coreConfig().getRemoteConfiguration().getAuth().getOauth();
    }

    private CoreConfig coreConfig() {
        if (coreConfig != null) {
            return coreConfig;
        }
        try {
            synchronized (this) {
                if (SpringContextBeanForApi.getSpringContext() != null) {
                    coreConfig = SpringContextBeanForApi.getSpringContext().getBean(CoreConfig.class);
                }
                return coreConfig;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
