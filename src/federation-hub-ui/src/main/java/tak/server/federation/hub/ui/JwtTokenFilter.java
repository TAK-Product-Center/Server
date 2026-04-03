package tak.server.federation.hub.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.base.Strings;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtTokenFilter extends OncePerRequestFilter {

    private JwtTokenUtil jwtUtil;

    private FederationHubUIConfig fedHubConfig;
    
    public JwtTokenFilter(FederationHubUIConfig fedHubConfig, JwtTokenUtil jwtUtil) {
    	this.jwtUtil = jwtUtil;
    	this.fedHubConfig = fedHubConfig;
    }
    
    private String extractAccessToken(Cookie[] cookies) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
    
    private String extractRefreshToken(Cookie[] cookies) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
    	
    	// Skip token check entirely for the login initiation endpoints
        String uri = request.getRequestURI();
        if (uri.equals("/api/oauth/login/auth") || uri.equals("/api/oauth/login/redirect")) {
            chain.doFilter(request, response);
            return;
        }

        if (!uri.startsWith("/api")) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractAccessToken(request.getCookies());

        if (token != null) {
            Claims claims = null;

            try {
                claims = jwtUtil.parseClaims(token, SignatureAlgorithm.RS256);
            } catch (ExpiredJwtException e) {
                token = handleRefreshToken(request, response);
                if (token != null) {
                	// successful refresh
                    claims = jwtUtil.parseClaims(token, SignatureAlgorithm.RS256);
                } else {
                	// no refresh
                    clearAuth(response, request);
                    return;
                }
            } catch (Exception e) {
            	logger.error("Cannot parse claims", e);
                clearAuth(response, request);
                return;
            }

            if (claims != null && isAuthorized(claims)) {
                setAuthentication(token, request);
            } else if (claims != null) {   
            	if(logger.isTraceEnabled()) {
            		logger.trace("Token does not have the proper claims to be admin authorized.");
            	}
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    // Check if token has required claim and value
    private boolean isAuthorized(Claims claims) {
        Object authClaim = claims.get(fedHubConfig.getKeycloakClaimName());
        
        if(logger.isTraceEnabled()) {
        	logger.trace("Keycloak Claim Name For Admin Grant: " + fedHubConfig.getKeycloakClaimName());
        	logger.trace("Expected Keycloak Claim Value For Admin Grant: " + fedHubConfig.getKeycloakAdminClaimValue());
        }

        if (authClaim instanceof ArrayList<?> list) {
        	if(logger.isTraceEnabled()) {
        		logger.trace("Claim Name Found With Values:");
        		list.forEach(logger::trace);
        	}
        	
            return list.contains(fedHubConfig.getKeycloakAdminClaimValue());
        } else if (authClaim instanceof String s) {
        	if(logger.isTraceEnabled()) {
        		logger.trace("Claim Name Found With Value: " + s);
        	}
  
            return s.equals(fedHubConfig.getKeycloakAdminClaimValue());
        } else {
        	if(logger.isTraceEnabled()) {
        		logger.trace("Claim Name Not Found On Token");
        	}
        }
        return false;
    }

    private void setAuthentication(String token, HttpServletRequest request) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        KeycloakToken authentication = new KeycloakToken(token, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Refresh expired token if refresh token is available
    private String handleRefreshToken(HttpServletRequest request, HttpServletResponse response) {
    	String refreshToken = extractRefreshToken(request.getCookies());
                
        if (Strings.isNullOrEmpty(refreshToken)) return null;

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        body.add("client_id", fedHubConfig.getKeycloakClientId());
        body.add("client_secret", fedHubConfig.getKeycloakSecret());

        try {
            return jwtUtil.handleKeycloakTokenRequest(body, request, response);
        } catch (Exception e) {
            return null;
        }
    }

    // Clear authentication and remove cookies
    private void clearAuth(HttpServletResponse response, HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        jwtUtil.logout(request, response);
    }

    // Custom authentication token
    private static class KeycloakToken extends AbstractAuthenticationToken {

        private final Object principal;
        private Object credentials;

        public KeycloakToken(Object principal, Object credentials,
                             Collection<? extends GrantedAuthority> authorities) {
            super(authorities);
            this.principal = principal;
            this.credentials = credentials;
            super.setAuthenticated(true);
        }

        @Override
        public Object getCredentials() { return this.credentials; }

        @Override
        public Object getPrincipal() { return this.principal; }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            Assert.isTrue(!isAuthenticated,
                    "Cannot set this token to trusted - use constructor with authorities");
            super.setAuthenticated(false);
        }

        @Override
        public void eraseCredentials() {
            super.eraseCredentials();
            this.credentials = null;
        }
    }
}
