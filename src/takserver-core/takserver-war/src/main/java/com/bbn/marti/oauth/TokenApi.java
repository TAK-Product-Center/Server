package com.bbn.marti.oauth;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.web.bind.annotation.*;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.jwt.JwtUtils;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import tak.server.Constants;


@RestController
public class TokenApi extends BaseRestController {

    public static final Logger logger = LoggerFactory.getLogger(TokenApi.class);

    @Autowired
    private TAKTokenStore takTokenStore;

    @Autowired
    DefaultTokenServices defaultTokenServices;

    private CoreConfig coreConfig;

    public class TokenResult implements Serializable {
        private String clientId;
        private String token;
        private String username;
        private Date expires;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT)
        public Date getExpires() { return expires; }
        public void setExpires(Date expires) { this.expires = expires; }
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

    private List<String> loadClientIdsFromConfig() {
        List<String> results = new ArrayList<>();
        if (oauthConfig() == null || oauthConfig().getClient() == null) {
            return null;
        }

        for (Oauth.Client client : oauthConfig().getClient()) {
            results.add(client.getClientId());
        }

        return results;
    }

    @RequestMapping(value = "/token/access", method = RequestMethod.GET)
    public ApiResponse<String> getAccessToken(HttpServletRequest request) {
        if (logger.isDebugEnabled()) {
            logger.debug("in getAccessToken");
        }

        String token = null;
        if (SecurityContextHolder.getContext().getAuthentication().getDetails()
                instanceof OAuth2AuthenticationDetails) {
            token = ((OAuth2AuthenticationDetails)SecurityContextHolder.getContext().
                    getAuthentication().getDetails()).getTokenValue();
        }

        return new ApiResponse<String>(Constants.API_VERSION, String.class.getSimpleName(), token);
    }

    @RequestMapping(value = "/token", method = RequestMethod.GET)
    public ApiResponse<List<TokenResult>> getAll(
            @RequestParam(value = "expired", defaultValue = "false") boolean expired)  {

        List<TokenResult> results = new ArrayList<>();
        List<String> clientIds = new ArrayList<>(Arrays.asList(TAKClientDetailsService.defaultClientId));
        List<String> clientIdsFromConfig = loadClientIdsFromConfig();
        if (clientIdsFromConfig != null) {
            clientIds.addAll(clientIdsFromConfig);
        }

        for (String clientId : clientIds) {

            List<OAuth2AccessToken> tokens = new ArrayList<>(takTokenStore.findTokensByClientId(clientId));
            for (OAuth2AccessToken token : tokens) {

                if (token.isExpired() && !expired) {
                    continue;
                }

                Claims claims = JwtUtils.getInstance().parseClaims(token.getValue(), SignatureAlgorithm.RS256);

                TokenResult result = new TokenResult();
                result.setClientId(clientId);
                result.setUsername((String) claims.get("user_name"));
                result.setToken(token.getValue());
                result.setExpires(token.getExpiration());

                results.add(result);
            }
        }

        return new ApiResponse<List<TokenResult>>(Constants.API_VERSION, TokenResult.class.getSimpleName(), results);
    }

    @RequestMapping(value = "/token/{token}", method = RequestMethod.DELETE)
    public void revokeToken(@PathVariable("token") String token) {
        try {
            defaultTokenServices.revokeToken(token);
        } catch (Exception e) {
            logger.error("exception in revokeToken!", e);
        }
    }

    @RequestMapping(value = "/token/revoke/{tokens}", method = RequestMethod.DELETE)
    public void revokeTokens(
            @PathVariable("tokens") String tokens) throws IOException {
        try {
            for (String token : Arrays.asList(tokens.split(","))) {
                defaultTokenServices.revokeToken(token);
            }
        } catch (Exception e) {
            logger.error("exception in revokeTokens!", e);
        }
    }
}