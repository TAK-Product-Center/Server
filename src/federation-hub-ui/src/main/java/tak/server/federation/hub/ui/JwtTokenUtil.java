package tak.server.federation.hub.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;

import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import tak.server.federation.hub.ui.FederationHubUIConfig;
import tak.server.federation.hub.ui.keycloak.AuthCookieUtils;
 
@Component
public class JwtTokenUtil {
	private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    private static final long EXPIRE_DURATION = 7 * 24 * 60 * 60 * 1000; // 7 days

    private PrivateKey privateKey = null;
    private SecretKeySpec secretkey; 
    
    @Autowired
    private FederationHubUIConfig fedHubConfig;
    
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser().setSigningKey(getSecret()).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
        	logger.error("Error validating token", e);
        } 
         
        return false;
    }
     
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }
     
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSecret())
                .parseClaimsJws(token)
                .getBody();
    }
     
    public String generateAccessToken(String username) {
        return Jwts.builder()
                .setSubject(String.format("%s", username))
                .setIssuer("Fedhub")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_DURATION))
                .signWith(SignatureAlgorithm.HS256, getSecret())
                .compact();
    }

    private SecretKeySpec getSecret() {
    	if (secretkey == null) {
    		try {
    			loadKeyStore();
    			secretkey = new SecretKeySpec(privateKey.getEncoded(), privateKey.getAlgorithm());
    		} catch (Exception e) {
    			logger.error("Error getting private key", e);
    		}
    	}
    	
    	return secretkey;
    }
    
	private void loadKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
			UnrecoverableKeyException {
		
		if (privateKey == null) {
			KeyStore keyStore = KeyStore.getInstance(fedHubConfig.getKeystoreType());
			keyStore.load(new FileInputStream(fedHubConfig.getKeystoreFile()), fedHubConfig.getKeystorePassword().toCharArray());

			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements() && privateKey == null) {
				String alias = aliases.nextElement();
				privateKey = (PrivateKey) keyStore.getKey(alias, fedHubConfig.getKeystorePassword().toCharArray());
			}
		}
	}
	
    public String processAuthServerRequest(
            MultiValueMap<String, String> requestBody,
            HttpServletRequest request, HttpServletResponse response) throws ParseException {
        // call the token endpoint
        RestTemplate restTemplate;

        restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> tokenResponse = restTemplate.exchange(
        		fedHubConfig.getKeycloakTokenEndpoint(), HttpMethod.POST,
                new HttpEntity<MultiValueMap<String, String>>(requestBody, headers),
                String.class);

        // validate the response
        if (tokenResponse.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("token endpoint returned " + tokenResponse.getStatusCodeValue());
        }

        // extract the token
        JSONObject tokenJson = (JSONObject) new JSONParser().parse(tokenResponse.getBody());

        if (!tokenJson.containsKey(fedHubConfig.getKeycloakAccessTokenName())) {
            throw new IllegalStateException("missing access_token in response");
        }

        // store the access token in a cookie
        String access_token = (String)tokenJson.get(fedHubConfig.getKeycloakAccessTokenName());
        response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                OAuth2TokenType.ACCESS_TOKEN.getValue(), access_token, -1, true).toString());

        // store the refresh token in the session, if we have one
        if (tokenJson.containsKey(fedHubConfig.getKeycloakRefreshTokenName())) {
            String refreshToken = (String)tokenJson.get(fedHubConfig.getKeycloakRefreshTokenName());
            request.getSession().setAttribute(fedHubConfig.getKeycloakRefreshTokenName(), refreshToken);
        }

        response.setHeader("Cache-Control", "must-revalidate, max-age=0, no-cache, no-store");
        response.setDateHeader("Expires", 0);
        return access_token;
    }
}