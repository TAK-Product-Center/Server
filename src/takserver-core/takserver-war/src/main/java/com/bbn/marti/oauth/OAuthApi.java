package com.bbn.marti.oauth;

import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bbn.marti.config.Tls;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.google.common.base.Strings;
import okhttp3.OkHttpClient;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.util.UriComponentsBuilder;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.util.spring.MartiSocketUserDetailsImpl;
import com.bbn.security.web.MartiValidatorConstants;
import tak.server.Constants;


@RestController
public class OAuthApi {

    protected static final Logger logger = LoggerFactory.getLogger(OAuthApi.class);

    @Autowired
    private Validator validator;

    @PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
    @RequestMapping(value = "/login/auth", method = RequestMethod.GET)
    public void handleAuthRequest(HttpServletResponse response) {
        try {
            // get the auth server config
            Oauth.AuthServer authServer = getAuthServerConfig();
            if (authServer == null) {
                throw  new IllegalStateException("missing auth server config");
            }

            // create a random state value to track the auth request
            SecureRandom secureRandom = new SecureRandom();
            byte[] code = new byte[32];
            secureRandom.nextBytes(code);
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(code);

            // attach the state to a cookie that we will validate in the redirect
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    "state", state, -1, false).toString());

            // build the auth url
            UriComponentsBuilder uriComponentBuilder =
                    UriComponentsBuilder.fromHttpUrl(authServer.getAuthEndpoint())
                            .queryParam("response_type", "code")
                            .queryParam("client_id", authServer.getClientId())
                            .queryParam("redirect_uri", authServer.getRedirectUri())
                            .queryParam("state", sha256(state));

            // add the scope if provided
            if (authServer.getScope() != null) {
                uriComponentBuilder = uriComponentBuilder
                        .queryParam("scope", authServer.getScope());
            }

            // send the redirect
            response.sendRedirect(uriComponentBuilder.toUriString());

        } catch (Exception e) {
            logger.error("exception in handleAuth", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void processAuthServerRequest(
            MultiValueMap<String, String> requestBody,
            HttpServletRequest request, HttpServletResponse response)
            throws NoSuchAlgorithmException, KeyManagementException, ParseException {

        // get the auth server config
        Oauth.AuthServer authServer = getAuthServerConfig();
        if (authServer == null) {
            throw new IllegalStateException("missing auth server config");
        }

        // call the token endpoint
        RestTemplate restTemplate;

        if (authServer.isTrustAllCerts()) {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            Tls tlsConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getSecurity().getTls();
            SSLContext sslContext = SSLContext.getInstance(tlsConfig.getContext());
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true);
            restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory(builder.build()));

        } else {
            restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> tokenResponse = restTemplate.exchange(
                authServer.getTokenEndpoint(), HttpMethod.POST,
                new HttpEntity<MultiValueMap<String, String>>(requestBody, headers),
                String.class);

        // validate the response
        if (tokenResponse.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("token endpoint returned " + tokenResponse.getStatusCodeValue());
        }

        // extract the token
        JSONObject tokenJson = (JSONObject) new JSONParser().parse(tokenResponse.getBody());

        if (!tokenJson.containsKey(authServer.getAccessTokenName())) {
            throw new IllegalStateException("missing access_token in response");
        }

        // store the access token in a cookie
        String access_token = (String)tokenJson.get(authServer.getAccessTokenName());
        response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                OAuth2TokenType.ACCESS_TOKEN.getValue(), access_token, -1, true).toString());

        // store the refresh token in the session, if we have one
        if (tokenJson.containsKey(authServer.getRefreshTokenName())) {
            String refreshToken = (String)tokenJson.get(authServer.getRefreshTokenName());
            request.getSession().setAttribute(authServer.getRefreshTokenName(), refreshToken);
        }

        response.setHeader("Cache-Control", "must-revalidate, max-age=0, no-cache, no-store");
        response.setDateHeader("Expires", 0);
    }

    @PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
    @RequestMapping(value = "/login/redirect", method = RequestMethod.GET)
    public ModelAndView handleRedirect(
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "state", required = true) String state,
            @CookieValue(value = "state", required = true) String stateCookie,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            // validate the inputs
            validator.getValidInput(
                    OAuthApi.class.getName(), code,
                    MartiValidatorConstants.Regex.MartiSafeString.name(),
                    MartiValidatorConstants.LONG_STRING_CHARS, false);
            validator.getValidInput(
                    OAuthApi.class.getName(), state,
                    MartiValidatorConstants.Regex.MartiSafeString.name(),
                    MartiValidatorConstants.LONG_STRING_CHARS, false);
            validator.getValidInput(
                    OAuthApi.class.getName(), stateCookie,
                    MartiValidatorConstants.Regex.MartiSafeString.name(),
                    MartiValidatorConstants.LONG_STRING_CHARS, false);

            // validate the request state
            if (!sha256(stateCookie).equals(state)) {
                throw new IllegalStateException("state did not match request!");
            }

            // clean up the state cookie
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    "state", stateCookie, 0, false).toString());

            // get the auth server config
            Oauth.AuthServer authServer = getAuthServerConfig();
            if (authServer == null) {
                throw new IllegalStateException("missing auth server config");
            }

            // build up the parameters for the token request
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("grant_type", "authorization_code");
            requestBody.add("code", code);
            requestBody.add("client_id", authServer.getClientId());
            requestBody.add("client_secret", authServer.getSecret());
            requestBody.add("redirect_uri", authServer.getRedirectUri());

            processAuthServerRequest(requestBody, request, response);

            return new ModelAndView(new InternalResourceView("/Marti/login/redirect.html"));

        } catch (Exception e) {
            logger.error("exception in handleRedirect", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
    @RequestMapping(value = "/login/refresh", method = RequestMethod.GET)
    public ModelAndView handleRefresh(
            HttpServletRequest request, HttpServletResponse response) {
        try {
            Oauth.AuthServer authServer = getAuthServerConfig();
            if (authServer != null) {
                String refreshToken = (String) request.getSession().getAttribute(authServer.getRefreshTokenName());
                if (Strings.isNullOrEmpty(refreshToken)) {
                    SecurityContextHolder.clearContext();
                    AuthCookieUtils.logout(request, response);
                    return new ModelAndView(new InternalResourceView("/Marti/login/redirect.html"));
                }

                // build up the parameters for the token request
                MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();
                requestBody.add("grant_type", "refresh_token");
                requestBody.add("refresh_token", refreshToken);
                requestBody.add("client_id", authServer.getClientId());
                requestBody.add("client_secret", authServer.getSecret());

                processAuthServerRequest(requestBody, request, response);
            }

            return new ModelAndView(new InternalResourceView("/Marti/login/redirect.html"));

        } catch (Exception e) {
            logger.error("exception in handleRefresh", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
    }

    @PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
    @RequestMapping(value = "/login/authserver", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<String>> getAuthServerName() {

        String name = null;
        HttpStatus status = HttpStatus.NOT_FOUND;

        if (getAuthServerConfig() != null) {
            name = getAuthServerConfig().getName();
            status = HttpStatus.OK;
        }

        return new ResponseEntity<ApiResponse<String>>(
                new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), name), status);
    }

    @RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.POST })
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        AuthCookieUtils.logout(request, response);
    }

    @RequestMapping(value = "/token/access", method = RequestMethod.GET)
    public ApiResponse<String> getAccessToken(HttpServletRequest request) {
        if (logger.isDebugEnabled()) {
            logger.debug("in getAccessToken");
        }

        String token = ((MartiSocketUserDetailsImpl)SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getToken();

        return new ApiResponse<String>(Constants.API_VERSION, String.class.getSimpleName(), token);
    }

    private Oauth.AuthServer getAuthServerConfig() {
        if (CoreConfigFacade.getInstance() != null &&
                CoreConfigFacade.getInstance().getRemoteConfiguration() != null &&
                CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth() != null &&
                CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth() != null &&
                CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth().getAuthServer() != null &&
                !CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth().getAuthServer().isEmpty()) {
            return CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth().getAuthServer().get(0);
        }
        return null;
    }

    private String sha256(String input) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = input.getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(bytes));
    }
}