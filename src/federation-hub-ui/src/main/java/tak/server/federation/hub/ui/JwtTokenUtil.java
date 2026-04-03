package tak.server.federation.hub.ui;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
 
public class JwtTokenUtil {
	private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final int MAX_NAME_VALUE_SIZE = 4096;
    private Validator validator = new MartiValidator();
    
    private JwtDecoder jwtDecoder;
    
    private FederationHubUIConfig fedHubConfig;
    
    private String keycloakAuthEndpoint;
	private String keycloakTokenEndpoint;
	private String keycloakCertEndpoint;
    
    public JwtTokenUtil(FederationHubUIConfig fedHubConfig) throws Exception {
    	this.fedHubConfig = fedHubConfig;
    }
    
    @PostConstruct
    public void init() {
        if (fedHubConfig.isAllowOauth()) {
            startKeycloakReconnectLoop();
        } else {
            logger.info("OAuth disabled — skipping Keycloak initialization");
        }
    }
    
    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void startKeycloakReconnectLoop() {
        scheduler.execute(new Runnable() {
            private int attempt = 0;

            @Override
            public void run() {
                try {
                    connectToKeycloak();
                    logger.info("Connected to Keycloak successfully");
                } catch (Exception e) {
                    attempt++;
                    long delay = Math.min(60, (attempt * 10)); // cap at 5 min
                    logger.warn("Cannot connect to Keycloak. Retrying in {} seconds", delay, e);
                    scheduler.schedule(this, delay, TimeUnit.SECONDS);
                }
            }
        });
    }

    private void connectToKeycloak() throws Exception {
        RestTemplate restTemplate =
                getRestTemplateWithKeycloakCert(fedHubConfig.getKeycloakTlsCertFile());

        ResponseEntity<String> tokenResponse = restTemplate.exchange(
                fedHubConfig.getKeycloakConfigurationEndpoint(),
                HttpMethod.GET,
                null,
                String.class
        );

        if (tokenResponse.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException(
                    "token endpoint returned " + tokenResponse.getStatusCodeValue());
        }

        JSONObject response = (JSONObject) new JSONParser().parse(tokenResponse.getBody());

        if (!response.containsKey("authorization_endpoint") ||
            !response.containsKey("token_endpoint") ||
            !response.containsKey("jwks_uri")) {
            throw new IllegalStateException("Keycloak metadata missing required fields");
        }

        keycloakAuthEndpoint = (String) response.get("authorization_endpoint");
        keycloakTokenEndpoint = (String) response.get("token_endpoint");
        keycloakCertEndpoint = (String) response.get("jwks_uri");

        jwtDecoder = buildJwtDecoder(fedHubConfig.getKeycloakTlsCertFile());
    }
    
    // build the request with keycloak cert in the chain of trust
    public RestTemplate getRestTemplateWithKeycloakCert(String certFilePath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (FileInputStream fis = new FileInputStream(certFilePath)) {
            caCert = (X509Certificate) cf.generateCertificate(fis);
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("keycloak", caCert);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        
        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
                javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        sslContext.init(null, tmf.getTrustManagers(), null);

        // disable hostname verification
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (javax.net.ssl.X509TrustManager) tmf.getTrustManagers()[0])
                .hostnameVerifier((hostname, session) -> true)
                .build();

        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(client));
    }
    
    public JwtDecoder buildJwtDecoder(String certFilePath) throws Exception {
        RestTemplate restTemplate = getRestTemplateWithKeycloakCert(certFilePath);

        return NimbusJwtDecoder.withJwkSetUri(keycloakCertEndpoint)
                .restOperations(restTemplate)
                .build();
    }

    public Claims parseClaims(String token, SignatureAlgorithm signatureAlgorithm) throws ExpiredJwtException {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            Map<String, Object> springClaims = jwt.getClaims();
            Claims jjwtClaims = new DefaultClaims(springClaims);

            return jjwtClaims;

        } catch (JwtValidationException e) {
        	if (isExpiredJwt(e)) {
        		throw new io.jsonwebtoken.ExpiredJwtException(null, null, e.getLocalizedMessage());
        	} else {
        		 logger.info("exception checking expiration status", e);
        		 return null;
        	}
        } catch (Exception e) {
            logger.info("exception in parseClaims", e);
            return  null;
        }

    }
    
    public static boolean isExpiredJwt(JwtValidationException ex) {
        return ex.getErrors().stream()
            .anyMatch(err -> err.getDescription() != null 
                          && err.getDescription().contains("Jwt expired"));
    }
	
    public String handleKeycloakTokenRequest(
            MultiValueMap<String, String> requestBody,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        // build chain of trust for talking to keycloak server
        RestTemplate restTemplate = getRestTemplateWithKeycloakCert(fedHubConfig.getKeycloakTlsCertFile()); 
        		
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> tokenResponse = restTemplate.exchange(
        		keycloakTokenEndpoint, HttpMethod.POST,
                new HttpEntity<MultiValueMap<String, String>>(requestBody, headers),
                String.class);

        // validate the response
        if (tokenResponse.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("token endpoint returned " + tokenResponse.getStatusCodeValue());
        }

        // extract the token
        JSONObject tokenJson = (JSONObject) new JSONParser().parse(tokenResponse.getBody());
        if (!tokenJson.containsKey("access_token")) {
            throw new IllegalStateException("missing access_token in response");
        }

        // store the tokens in the cookie
		String access_token = (String) tokenJson.get("access_token");
		response.addHeader(HttpHeaders.SET_COOKIE, createCookie("access_token", access_token, -1, true).toString());

		String refresh_token = (String) tokenJson.get("refresh_token");
		response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refresh_token", refresh_token, -1, true).toString());

		// current does nothing since we are stateless
		if (!Strings.isNullOrEmpty(access_token)) {
			request.getSession().setAttribute("access_token", access_token);
		}

		if (!Strings.isNullOrEmpty(refresh_token)) {
			request.getSession().setAttribute("refresh_token", refresh_token);
		}

        response.setHeader("Cache-Control", "must-revalidate, max-age=0, no-cache, no-store");
        response.setDateHeader("Expires", 0);
        return access_token;
    }
    
    public ResponseCookie createCookie(final String name, final String value, int maxAge, boolean sameSiteStrict, String path) {
        String validatedName;
        String validatedValue;
        try {
            validatedName = validator.getValidInput(this.getClass().getName(), name,
            		MartiValidator.Regex.MartiSafeString.name(), MAX_NAME_VALUE_SIZE, false);
            validatedValue = validator.getValidInput(this.getClass().getName(), value,
            		MartiValidator.Regex.MartiSafeString.name(), MAX_NAME_VALUE_SIZE, true);
        } catch (ValidationException e) {
            logger.error("ValidationException in createCookie!", e);
            return null;
        }

        ResponseCookie.ResponseCookieBuilder responseCookieBuilder = ResponseCookie
                .from(validatedName, validatedValue)
                .secure(true)
                .httpOnly(true)
                .path(path)
                .maxAge(maxAge);

        if (sameSiteStrict) {
            responseCookieBuilder = responseCookieBuilder.sameSite("Strict");
        }

        return responseCookieBuilder.build();
    }

    public ResponseCookie createCookie(final String name, final String value, int maxAge, boolean sameSiteStrict) {
        return createCookie(name, value, maxAge, sameSiteStrict, "/");
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().compareToIgnoreCase(OAuth2TokenType.ACCESS_TOKEN.getValue()) == 0) {

                String token = cookie.getValue();

                response.setHeader("Location", "/");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Pragma", "no-cache");
                response.setHeader(HttpHeaders.SET_COOKIE, createCookie(
                		"access_token", token, 0, true).toString());

                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                return;
            }
        }
    }

	public String getKeycloakAuthEndpoint() {
		return keycloakAuthEndpoint;
	}

	public String getKeycloakTokenEndpoint() {
		return keycloakTokenEndpoint;
	}

	public String getKeycloakCertEndpoint() {
		return keycloakCertEndpoint;
	}
}