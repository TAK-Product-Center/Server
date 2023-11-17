package com.bbn.marti.oauth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletResponse;

import com.bbn.marti.config.Tls;
import okhttp3.OkHttpClient;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.util.UriComponentsBuilder;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.security.web.MartiValidatorConstants;
import tak.server.Constants;


@RestController
public class OAuthApi {

    protected static final Logger logger = LoggerFactory.getLogger(OAuthApi.class);

    @Autowired
    CoreConfig coreConfig;

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

    @PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
    @RequestMapping(value = "/login/redirect", method = RequestMethod.GET)
    public ModelAndView handleRedirect(
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "state", required = true) String state,
            @CookieValue(value = "state", required = true) String stateCookie,
            HttpServletResponse response) {

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

            // get the auth server config
            Oauth.AuthServer authServer = getAuthServerConfig();
            if (authServer == null) {
                throw new IllegalStateException("missing auth server config");
            }

            // validate the request state
            if (!sha256(stateCookie).equals(state)) {
                throw new IllegalStateException("state did not match request!");
            }

            // clean up the state cookie
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    "state", stateCookie, 0, false).toString());

            // build up the parameters for the token request
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("grant_type", "authorization_code");
            requestBody.add("code", code);
            requestBody.add("client_id", authServer.getClientId());
            requestBody.add("client_secret", authServer.getSecret());
            requestBody.add("redirect_uri", authServer.getRedirectUri());

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

                Tls tlsConfig = coreConfig.getRemoteConfiguration().getSecurity().getTls();
                SSLContext sslContext = SSLContext.getInstance(tlsConfig.getContext());
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                OkHttpClient.Builder builder = new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier((hostname, session) -> true);
                restTemplate =  new RestTemplate(new OkHttp3ClientHttpRequestFactory(builder.build()));

            } else {
                restTemplate =  new RestTemplate(new OkHttp3ClientHttpRequestFactory());
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
            if (!tokenJson.containsKey(authServer.getTokenName())) {
                throw new IllegalStateException("missing access_token in response");
            }
            String access_token = (String) tokenJson.get(authServer.getTokenName());

            // store the access token in a cookie
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    OAuth2AccessToken.ACCESS_TOKEN, access_token, -1, true).toString());
            response.setHeader("Cache-Control", "must-revalidate, max-age=0, no-cache, no-store");
            response.setDateHeader("Expires", 0);

            // TODO save token in database, test revocation with ui manager

            return new ModelAndView(new InternalResourceView("/Marti/login/redirect.html"));

        } catch (Exception e) {
            logger.error("exception in handleRedirect", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

    private Oauth.AuthServer getAuthServerConfig() {
        if (coreConfig != null &&
                coreConfig.getRemoteConfiguration() != null &&
                coreConfig.getRemoteConfiguration().getAuth() != null &&
                coreConfig.getRemoteConfiguration().getAuth().getOauth() != null) {
            return coreConfig.getRemoteConfiguration().getAuth().getOauth().getAuthServer();
        }
        return null;
    }

    private String sha256(String input) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = input.getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(bytes));
    }
}