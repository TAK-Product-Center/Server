package com.bbn.marti.oauth;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.bbn.marti.remote.config.CoreConfigFacade;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tak.server.Constants;


@RestController
public class TokenApi extends BaseRestController {

    public static final Logger logger = LoggerFactory.getLogger(TokenApi.class);

    @Autowired
    private JdbcOAuth2AuthorizationService jdbcOAuth2AuthorizationService;

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
        if (CoreConfigFacade.getInstance() == null ||
                CoreConfigFacade.getInstance().getRemoteConfiguration() == null ||
                CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth()  == null ||
                CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth() == null) {
            return null;
        }

        return CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth();
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

    @RequestMapping(value = "/token", method = RequestMethod.GET)
    public ApiResponse<List<TokenResult>> getAll(
            @RequestParam(value = "expired", defaultValue = "false") boolean expired)  {

        List<TokenResult> results = new ArrayList<>();

        List<OAuth2Authorization> authorizations = jdbcOAuth2AuthorizationService.findAll();

        for (OAuth2Authorization authorization : authorizations) {

            if (authorization.getAccessToken().isExpired()  && !expired) {
                continue;
            }

            TokenResult result = new TokenResult();
            result.setClientId(authorization.getRegisteredClientId());
            result.setUsername((String) authorization.getAccessToken().getClaims().get("sub"));
            result.setToken(authorization.getAccessToken().getToken().getTokenValue());
            result.setExpires(Date.from(authorization.getAccessToken().getToken().getExpiresAt()));

            results.add(result);
        }

        return new ApiResponse<List<TokenResult>>(Constants.API_VERSION, TokenResult.class.getSimpleName(), results);
    }

    @RequestMapping(value = "/token/{token}", method = RequestMethod.DELETE)
    public void revokeToken(@PathVariable("token") String token) {
        try {
            OAuth2Authorization authorization = jdbcOAuth2AuthorizationService.findByToken(
                    token, OAuth2TokenType.ACCESS_TOKEN);
            if (authorization != null) {
                jdbcOAuth2AuthorizationService.remove(authorization);
            }
        } catch (Exception e) {
            logger.error("exception in revokeToken!", e);
        }
    }

    @RequestMapping(value = "/token/revoke/{tokens}", method = RequestMethod.DELETE)
    public void revokeTokens(
            @PathVariable("tokens") String tokens) throws IOException {
        try {
            for (String token : Arrays.asList(tokens.split(","))) {
                revokeToken(token);
            }
        } catch (Exception e) {
            logger.error("exception in revokeTokens!", e);
        }
    }
}