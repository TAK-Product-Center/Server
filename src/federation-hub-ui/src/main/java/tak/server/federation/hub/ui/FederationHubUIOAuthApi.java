package tak.server.federation.hub.ui;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RequestMapping("/api/oauth")
@RestController
public class FederationHubUIOAuthApi {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubUIOAuthApi.class);

    private Validator validator = new MartiValidator();

    @Autowired AuthenticationManager authManager;

    @Autowired JwtTokenUtil jwtUtil;

    @Autowired
    FederationHubUIConfig fedHubConfig;

    @RequestMapping(value = "/login/authserver", method = RequestMethod.GET)
    public ResponseEntity<String>  getAuthServerName() {

        if (fedHubConfig.isAllowOauth()) {
        	return new ResponseEntity<>("{\"data\":\"" + fedHubConfig.getKeycloakServerName() + "\"}", new HttpHeaders(), HttpStatus.OK);
        } else {
        	return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
    }
    
    @RequestMapping(value = "/login/auth", method = RequestMethod.GET)
    public void handleAuthRequest(@RequestParam(name = "force", required = false) String force, HttpServletResponse response) {
        try {
            // get the auth server config
            if (!fedHubConfig.isAllowOauth() ||
            		Strings.isNullOrEmpty(fedHubConfig.getKeycloakConfigurationEndpoint()) ||
            		 Strings.isNullOrEmpty(fedHubConfig.getKeycloakrRedirectUri())) {
            	throw  new IllegalStateException("missing auth server config");
            }

            // create a random state value to track the auth request
            SecureRandom secureRandom = new SecureRandom();
            byte[] code = new byte[32];
            secureRandom.nextBytes(code);
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(code);

            // attach the state to a cookie that we will validate in the redirect
            response.addHeader(HttpHeaders.SET_COOKIE, jwtUtil.createCookie(
                    "state", state, -1, false).toString());

            // build the auth url
            UriComponentsBuilder uriComponentBuilder =
            	    UriComponentsBuilder.fromHttpUrl(jwtUtil.getKeycloakAuthEndpoint())
            	        .queryParam("response_type", "code")
            	        .queryParam("client_id", fedHubConfig.getKeycloakClientId())
            	        .queryParam("redirect_uri", fedHubConfig.getKeycloakrRedirectUri())
            	        .queryParam("state", state);

                if ("true".equals(force)) {
                	uriComponentBuilder.queryParam("prompt", "login")
                           .queryParam("max_age", "0");
                }
                
            if (logger.isTraceEnabled()) {
            	logger.trace("request for /login/auth - redirecting to " + uriComponentBuilder.toUriString());
            }

            // send the redirect
            response.sendRedirect(uriComponentBuilder.toUriString());
        } catch (Exception e) {
            logger.error("exception in handleAuth", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/login/redirect", method = RequestMethod.GET)
    public void handleRedirect(
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "state", required = true) String state,
            @CookieValue(value = "state", required = true) String stateCookie,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            // validate the inputs
            validator.getValidInput(
                    FederationHubUIService.class.getName(), code,
                    MartiValidator.Regex.MartiSafeString.name(),
                    2047, false);
            validator.getValidInput(
            		FederationHubUIService.class.getName(), state,
                    MartiValidator.Regex.MartiSafeString.name(),
                    2047, false);
            validator.getValidInput(
            		FederationHubUIService.class.getName(), stateCookie,
                    MartiValidator.Regex.MartiSafeString.name(),
                    2047, false);

            // validate the request state
            if (!stateCookie.equals(state)) {
                throw new IllegalStateException("state did not match request!");
            }

            // clean up the state cookie
            response.addHeader(HttpHeaders.SET_COOKIE, jwtUtil.createCookie(
                    "state", stateCookie, 0, false).toString());
            
            // build up the parameters for the token request
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("grant_type", "authorization_code");
            requestBody.add("code", code);
            requestBody.add("client_id", fedHubConfig.getKeycloakClientId());
            requestBody.add("client_secret", fedHubConfig.getKeycloakSecret());
            requestBody.add("redirect_uri", fedHubConfig.getKeycloakrRedirectUri());

            jwtUtil.handleKeycloakTokenRequest(requestBody, request, response);

            response.sendRedirect("/");

        } catch (Exception e) {
            logger.error("exception in handleRedirect", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}