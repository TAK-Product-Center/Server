package com.bbn.marti.oauth;

import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.bbn.marti.config.Tls;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.google.common.base.Strings;
import okhttp3.OkHttpClient;

import org.jetbrains.annotations.NotNull;
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
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
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

import com.bbn.marti.jwt.JwtUtils;
import com.bbn.marti.config.Oauth;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.util.spring.MartiSocketUserDetailsImpl;
import com.bbn.security.web.MartiValidatorConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tak.server.Constants;
import tak.server.oidc.AuthorizationRequestStore;
import tak.server.oidc.OidcAuditEvent;
import tak.server.oidc.OidcHttp;
import tak.server.oidc.OidcIdentity;
import tak.server.oidc.OidcProvider;
import tak.server.oidc.OidcProviderConfig;
import tak.server.oidc.OidcProviderRegistry;
import tak.server.oidc.OidcValidationException;
import tak.server.oidc.Pkce;


@RestController
public class OAuthApi {

    protected static final Logger logger = LoggerFactory.getLogger(OAuthApi.class);

    @Autowired
    private Validator validator;

    private volatile Cache<String, Oauth.AuthServer> oauthConfigurationCache;

    @PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
    @RequestMapping(value = "/login/auth", method = RequestMethod.GET)
    public void handleAuthRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            // OIDC (Phase 1): a registered discovery provider takes the hardened flow. Without one, the
            // pre-existing flow below runs unchanged.
            OidcProviderRegistry registry = OidcProviderRegistry.getInstance();
            if (!registry.isEmpty()) {
                handleOidcAuthRequest(registry, request, response);
                return;
            }

            // get the auth server config.
            Map<String, Oauth.AuthServer> knownConfigs = getAuthServerConfig().asMap();
            if (knownConfigs == null) {
                throw  new IllegalStateException("missing auth server config");
            }

            // create a random state value to track the auth request
            SecureRandom secureRandom = new SecureRandom();
            byte[] code = new byte[32];
            secureRandom.nextBytes(code);
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(code);

            // attach the state to a cookie that we will validate in the redirect
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    "state", state, -1, false, request.isSecure()).toString());

            // build the auth url
            Iterator<Map.Entry<String, Oauth.AuthServer>> entrySet = knownConfigs.entrySet().iterator();
            Oauth.AuthServer authServer  =  entrySet.next().getValue();
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

    private String processAuthServerRequest(
            MultiValueMap<String, String> requestBody,
            HttpServletRequest request, HttpServletResponse response)
            throws NoSuchAlgorithmException, KeyManagementException, ParseException {

        // get the auth server config
        Map<String, Oauth.AuthServer> knownConfigs = getAuthServerConfig().asMap();
        if (knownConfigs.isEmpty()) {
            throw new IllegalStateException("missing auth server config");
        }

        // call the token endpoint
        RestTemplate restTemplate;
        Iterator<Map.Entry<String, Oauth.AuthServer>> entrySet = knownConfigs.entrySet().iterator();
        Oauth.AuthServer authServer  =  entrySet.next().getValue();
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
            throw new IllegalStateException(String.format("JWT Response did not include AccessToken field %s", authServer.getAccessTokenName()));
        }

        // store the access token in a cookie
        String access_token = (String)tokenJson.get(authServer.getAccessTokenName());

        for (ResponseCookie cookie : AuthCookieUtils.createCookiesWithMaxSize(
                OAuth2TokenType.ACCESS_TOKEN.getValue(), access_token, -1, true, request.isSecure())) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        // store the refresh token in the session, if we have one
        if (tokenJson.containsKey(authServer.getRefreshTokenName())) {
            String refreshToken = (String)tokenJson.get(authServer.getRefreshTokenName());
            request.getSession().setAttribute(authServer.getRefreshTokenName(), refreshToken);
        }

        response.setHeader("Cache-Control", "must-revalidate, max-age=0, no-cache, no-store");
        response.setDateHeader("Expires", 0);

        return access_token;
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
                if (!OidcProviderRegistry.getInstance().isEmpty()) {
                    OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, "-", "-", null, request.getLocalPort(),
                            OidcValidationException.Reason.STATE);
                }
                throw new IllegalStateException("state did not match request!");
            }

            // clean up the state cookie
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    "state", stateCookie, 0, false, request.isSecure()).toString());

            // OIDC (Phase 1)
            OidcProviderRegistry registry = OidcProviderRegistry.getInstance();
            if (!registry.isEmpty()) {
                return handleOidcRedirect(registry, code, state, request, response);
            }

            // get the auth server config
            Map<String, Oauth.AuthServer> knownConfigs = getAuthServerConfig().asMap();
            if (knownConfigs.isEmpty()) {
                throw new IllegalStateException("missing auth server config");
            }
            Iterator<Map.Entry<String, Oauth.AuthServer>> entrySet = knownConfigs.entrySet().iterator();
            Oauth.AuthServer authServer  =  entrySet.next().getValue();
            // build up the parameters for the token request
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("grant_type", "authorization_code");
            requestBody.add("code", code);
            requestBody.add("client_id", authServer.getClientId());
            requestBody.add("client_secret", authServer.getSecret());
            requestBody.add("redirect_uri", authServer.getRedirectUri());

            String access_token = processAuthServerRequest(requestBody, request, response);

            String redirect = "/Marti/login/redirect.html";

            Oauth oauthConf = getOuathConfig();
            if (oauthConf != null && oauthConf.getWebtakScope() != null) {

                Claims claims = JwtUtils.getInstance().parseClaims(
                        access_token, SignatureAlgorithm.RS256, false);
                if (claims == null) {
                    throw new InvalidBearerTokenException("Unable to parse claims from token"); // token not logged (T8)
                }

                if (!AuthCookieUtils.userHasWebtakAccess(oauthConf, claims)) {
                    redirect = "/Marti/login/webtak-role-error.html";
                }
            }

            return new ModelAndView(new InternalResourceView(redirect));

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
            Map<String, Oauth.AuthServer> knownConfigs = getAuthServerConfig().asMap();
            if (!knownConfigs.isEmpty()) {
                Iterator<Map.Entry<String, Oauth.AuthServer>> entrySet = knownConfigs.entrySet().iterator();
                Oauth.AuthServer authServer  =  entrySet.next().getValue();
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

        OidcProviderRegistry registry = OidcProviderRegistry.getInstance();
        if (!registry.isEmpty()) {
            name = registry.first().getName();
            status = HttpStatus.OK;
        } else {
            Map<String, Oauth.AuthServer> knownConfigs = getAuthServerConfig().asMap();
            if (!knownConfigs.isEmpty()) {
                Iterator<Map.Entry<String, Oauth.AuthServer>> entrySet = knownConfigs.entrySet().iterator();
                Oauth.AuthServer authServer  =  entrySet.next().getValue();
                name = authServer.getName();
                status = HttpStatus.OK;
            }
        }

        return new ResponseEntity<ApiResponse<String>>(
                new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), name), status);
    }

    /**
     * What a TAK client needs to start the SSO flow against the configured IdP without typing an issuer URL
     * (v0.2 AN-10). Additive: the two pre-existing fields keep their meaning; the rest are null when no
     * discovery provider is configured.
     */
    public static class OpenIdConfiguration {
        public String authorization_endpoint;
        public String token_endpoint;
        /** The IdP's issuer; the client fetches {@code <issuer>/.well-known/openid-configuration} to confirm. */
        public String issuer;
        /** The public client id ATAK uses (Authorization Code with PKCE, no secret). */
        public String android_client_id;
        /** Scopes to request; includes the scope that carries the groups claim when the IdP needs one. */
        public String scopes;
        /** Redirect shape ATAK should use: "scheme" (private-use URI scheme) or "loopback" (RFC 8252 7.3). */
        public String android_redirect;
        /** TAK always sends S256; advisory for the client. */
        public String[] code_challenge_methods_supported;
    }

    @PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
    @RequestMapping(value = "/login/.well-known/openid-configuration", method = RequestMethod.GET)
    public OpenIdConfiguration getOpenIdConfiguration() {
        OpenIdConfiguration openIdConfiguration = new OpenIdConfiguration();
        OidcProviderRegistry registry = OidcProviderRegistry.getInstance();
        if (!registry.isEmpty()) {
            OidcProviderConfig cfg = registry.first().getConfig();
            openIdConfiguration.authorization_endpoint = cfg.getAuthorizationEndpoint();
            openIdConfiguration.token_endpoint = cfg.getTokenEndpoint();
            openIdConfiguration.issuer = cfg.getIssuer();
            openIdConfiguration.android_client_id = cfg.getAndroidClientId();
            openIdConfiguration.scopes = cfg.getScopes();
            openIdConfiguration.android_redirect = "scheme";
            openIdConfiguration.code_challenge_methods_supported = new String[] { "S256" };
            return openIdConfiguration;
        }
        Map<String, Oauth.AuthServer> knownConfigs = getAuthServerConfig().asMap();
        if (!knownConfigs.isEmpty()) {
            Iterator<Map.Entry<String, Oauth.AuthServer>> entrySet = knownConfigs.entrySet().iterator();
            Oauth.AuthServer authServer  =  entrySet.next().getValue();
            openIdConfiguration.authorization_endpoint = authServer.getAuthEndpoint();
            openIdConfiguration.token_endpoint = authServer.getTokenEndpoint();
        }
        return openIdConfiguration;
    }

    @RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.POST })
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        AuthCookieUtils.logout(request, response);
    }

    @RequestMapping(value = "/token/access", method = RequestMethod.GET)
    public ApiResponse<String> getAccessToken(HttpServletRequest request, HttpServletResponse response) {
        if (logger.isDebugEnabled()) {
            logger.debug("in getAccessToken");
        }

        Oauth oauthConf = getOuathConfig();
        if (oauthConf == null || !oauthConf.isAllowAccessTokenRetrieval()) {
            logger.error("illegal attempt to retrieve access token");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        String token = ((MartiSocketUserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getToken();
        return new ApiResponse<String>(Constants.API_VERSION, String.class.getSimpleName(), token);
    }

    // ---- OIDC (Phase 1) -----------------------------------------------------------------------------

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Authorization request for a registered provider. Adds to the pre-existing flow: a nonce (bound to this
     * request, checked on the ID token), PKCE S256 (verifier kept server-side, never sent to the browser), and
     * server-side registration of the state so the callback can consume it exactly once (T6, T7). The cookie
     * binding upstream already had is kept: the browser presents the raw value, the IdP echoes its SHA-256.
     */
    private void handleOidcAuthRequest(OidcProviderRegistry registry, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        OidcProvider provider = registry.first();
        OidcProviderConfig cfg = provider.getConfig();

        String cookieState = AuthorizationRequestStore.newToken();
        String state = sha256(cookieState);
        String nonce = AuthorizationRequestStore.newToken();
        String verifier = cfg.isUsePkce() ? Pkce.newVerifier() : null;
        registry.requestStore().issue(state, verifier, nonce, cfg.getName());

        response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                "state", cookieState, -1, false, request.isSecure()).toString());

        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(cfg.getAuthorizationEndpoint())
                .queryParam("response_type", "code")
                .queryParam("client_id", cfg.getWebClientId())
                .queryParam("redirect_uri", cfg.getRedirectUri())
                .queryParam("scope", cfg.getScopes())
                .queryParam("state", state)
                .queryParam("nonce", nonce);
        if (verifier != null) {
            uri.queryParam("code_challenge", Pkce.challengeS256(verifier))
               .queryParam("code_challenge_method", "S256");
        }

        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(uri.build().encode().toUriString());
    }

    /**
     * The callback. Order matters: the state is consumed before any network call, so a replayed callback costs
     * nothing (T7); the ID token is validated (signature, issuer, audience, expiry, nonce) as the authentication
     * event; only then is a session credential set. The credential TAK keeps in the cookie is the access token
     * when it is a JWT from this provider (upstream behaviour, so groups and scopes read the same way), otherwise
     * the ID token. The authenticator re-validates whichever it is on every request.
     */
    private ModelAndView handleOidcRedirect(OidcProviderRegistry registry, String code, String state,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        int port = request.getLocalPort();

        AuthorizationRequestStore.PendingRequest pending = registry.requestStore().consume(state);
        if (pending == null) {
            OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, "-", "-", null, port,
                    OidcValidationException.Reason.STATE);
            throw new IllegalStateException("authorization request state is unknown, expired or already used");
        }
        OidcProvider provider = registry.byName(pending.getProviderName());
        if (provider == null) {
            throw new IllegalStateException("provider for this authorization request is no longer configured");
        }
        OidcProviderConfig cfg = provider.getConfig();

        // The code exchange goes through OkHttp directly, not RestTemplate: RestTemplate logs request bodies at
        // DEBUG, and this body carries the code, the client secret and the PKCE verifier (T8).
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("client_id", cfg.getWebClientId());
        form.put("client_secret", cfg.getWebClientSecret());
        form.put("redirect_uri", cfg.getRedirectUri());
        form.put("code_verifier", pending.getCodeVerifier());
        OidcHttp.FormResponse tokenResponse;
        try {
            tokenResponse = OidcHttp.postForm(provider.getHttp(), cfg.getTokenEndpoint(), form);
        } catch (Exception e) {
            OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, cfg.getName(), cfg.getIssuer(), null, port,
                    OidcValidationException.Reason.EXCHANGE);
            // The exception text from the HTTP client can include the response body; never let it near a log.
            throw new IllegalStateException("token endpoint call failed: " + e.getClass().getSimpleName());
        }
        if (tokenResponse.status != 200 || tokenResponse.body.isEmpty()) {
            OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, cfg.getName(), cfg.getIssuer(), null, port,
                    OidcValidationException.Reason.EXCHANGE);
            throw new IllegalStateException("token endpoint returned " + tokenResponse.status);
        }
        JsonNode json;
        try {
            json = JSON.readTree(tokenResponse.body);
        } catch (Exception e) {
            json = null;
        }
        String idToken = json == null ? null : text(json, "id_token");
        if (idToken == null) {
            OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, cfg.getName(), cfg.getIssuer(), null, port,
                    OidcValidationException.Reason.EXCHANGE);
            throw new IllegalStateException("token response carries no id_token");
        }

        OidcIdentity identity;
        try {
            identity = provider.validate(idToken, false);
        } catch (OidcValidationException e) {
            OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, cfg.getName(), cfg.getIssuer(), null, port, e.getReason());
            throw new IllegalStateException("id_token rejected: " + e.getReason());
        }
        // The ID token must be for the web client specifically, not merely for some configured audience.
        Object aud = identity.getClaim("aud");
        boolean forWebClient = aud instanceof Collection<?> ? ((Collection<?>) aud).contains(cfg.getWebClientId())
                : cfg.getWebClientId().equals(aud);
        if (!forWebClient) {
            OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, cfg.getName(), cfg.getIssuer(), identity.getSubject(),
                    port, OidcValidationException.Reason.AUD);
            throw new IllegalStateException("id_token audience is not the web client");
        }
        if (!pending.getNonce().equals(identity.getStringClaim("nonce", null))) {
            OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, cfg.getName(), cfg.getIssuer(), identity.getSubject(),
                    port, OidcValidationException.Reason.NONCE);
            throw new IllegalStateException("id_token nonce does not match the authorization request");
        }

        // The session credential: the access token when it is a JWT from this provider (so groups and scopes
        // read the same way as on the bearer port), else the ID token. Whichever it is, the identity TAK acts
        // on from here is the credential's, and it must be the same subject the nonce-checked ID token proved.
        String accessToken = text(json, cfg.getAccessTokenName());
        String credential = idToken;
        OidcIdentity credentialIdentity = identity;
        if (accessToken != null && registry.providerFor(accessToken) == provider) {
            try {
                credentialIdentity = provider.validate(accessToken, false);
                credential = accessToken;
            } catch (OidcValidationException e) {
                OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, cfg.getName(), cfg.getIssuer(), identity.getSubject(),
                        port, e.getReason());
                throw new IllegalStateException("access_token rejected: " + e.getReason());
            }
            if (!identity.getSubject().equals(credentialIdentity.getSubject())) {
                OidcAuditEvent.deny(OidcAuditEvent.Surface.BROWSER, cfg.getName(), cfg.getIssuer(), identity.getSubject(),
                        port, OidcValidationException.Reason.CLAIMS);
                throw new IllegalStateException("access_token subject differs from the id_token subject");
            }
        }

        for (ResponseCookie cookie : AuthCookieUtils.createCookiesWithMaxSize(
                OAuth2TokenType.ACCESS_TOKEN.getValue(), credential, -1, true, request.isSecure())) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        String refreshToken = text(json, cfg.getRefreshTokenName());
        if (refreshToken != null) {
            request.getSession().setAttribute(cfg.getRefreshTokenName(), refreshToken);
        }
        response.setHeader("Cache-Control", "must-revalidate, max-age=0, no-cache, no-store");
        response.setDateHeader("Expires", 0);

        // No ALLOW line here: authorization (group mapping) has not happened yet. The first request the browser
        // makes with the cookie runs the authenticator, which writes the ALLOW or DENY line with the groups.

        String redirect = "/Marti/login/redirect.html";
        Oauth oauthConf = getOuathConfig();
        if (oauthConf != null && oauthConf.getWebtakScope() != null
                && !AuthCookieUtils.userHasWebtakAccess(oauthConf, credentialIdentity.getClaims())) {
            redirect = "/Marti/login/webtak-role-error.html";
        }
        return new ModelAndView(new InternalResourceView(redirect));
    }

    private static String text(JsonNode node, String field) {
        JsonNode n = node == null || field == null ? null : node.get(field);
        return (n == null || !n.isTextual() || n.asText().isEmpty()) ? null : n.asText();
    }

    private Oauth getOuathConfig() {
        if (CoreConfigFacade.getInstance() != null &&
                CoreConfigFacade.getInstance().getRemoteConfiguration() != null &&
                CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth() != null &&
                CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth() != null) {
            return CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth();
        }
        return null;
    }

    private Cache<String, Oauth.AuthServer> getAuthServerConfig() {
        Cache<String, Oauth.AuthServer> oAuthCache = getOrCreateOAuthCache();
        if (oAuthCache .asMap().isEmpty()) {
            ArrayList<Oauth.AuthServer> knownConfigs = new ArrayList<Oauth.AuthServer>();
            knownConfigs.addAll(getManualConfigurations());
            knownConfigs.addAll(getAutomatedConfigurations());
            for (Oauth.AuthServer config : knownConfigs ){
                oAuthCache.asMap().put(config.getName(), config);
            }
        }

        return oAuthCache;
    }

    private @NotNull List<Oauth.AuthServer> getManualConfigurations() {
        List<Oauth.AuthServer> knownConfigs = new ArrayList<Oauth.AuthServer>();
        if (getOuathConfig() != null &&
                getOuathConfig().getAuthServer() != null &&
                !getOuathConfig().getAuthServer().isEmpty()) {
            knownConfigs =  CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth().getAuthServer();
        }
        return knownConfigs;
    }

    private @NotNull List<Oauth.AuthServer> getAutomatedConfigurations() {
        List<Oauth.AuthServer> knownConfigs = new ArrayList<Oauth.AuthServer>();
        if (getOuathConfig() != null &&
                getOuathConfig().getOpenIdDiscoveryConfiguration() != null &&
                !getOuathConfig().getOpenIdDiscoveryConfiguration().isEmpty()) {
            // OIDC (Phase 1): an <openIdDiscoveryConfiguration enabled="true"> is resolved by OidcProviderRegistry
            // on Nimbus and never by the pre-existing conversion below; one the registry refused stays refused. An
            // element without enabled="true" keeps the pre-existing conversion exactly (R-01).
            List<Oauth.OpenIdDiscoveryConfiguration> trustedConfigs = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth().getOpenIdDiscoveryConfiguration();
            int enabled = 0;
            for (Oauth.OpenIdDiscoveryConfiguration config : trustedConfigs) {
                if (config.isEnabled()) {
                    enabled++;
                    continue;
                }
                try {
                    Oauth.AuthServer manualConfig = OAuthUtils.processTrustedAuthServerConfig(config);
                    knownConfigs.add(manualConfig);
                } catch (Exception e) {
                    logger.error("exception calling processTrustedAuthServerConfig", e);
                }
            }
            if (enabled > 0 && OidcProviderRegistry.getInstance().isEmpty()) {
                logger.error("{} openIdDiscoveryConfiguration element(s) enabled for Phase 1 and none registered; "
                        + "external login is disabled until discovery succeeds (see earlier 'NOT registered' lines)", enabled);
            }
        }
        return knownConfigs;
    }

    private Cache<String, Oauth.AuthServer> getOrCreateOAuthCache() {
        if (oauthConfigurationCache == null) {
            synchronized (this) {
                if (oauthConfigurationCache == null) {
                    oauthConfigurationCache = Caffeine.newBuilder().expireAfterWrite(
                            CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue()
                                    .getOAuthConfigurationsCacheSeconds(), TimeUnit.SECONDS).build();
                }
            }
        }

        return oauthConfigurationCache;
    }

    private String sha256(String input) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = input.getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(bytes));
    }
}
