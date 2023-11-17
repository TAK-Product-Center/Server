package tak.server.federation.hub.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.base.Strings;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import tak.server.federation.hub.ui.jwt.HubAuthUser;
import tak.server.federation.hub.ui.keycloak.AuthCookieUtils;
import tak.server.federation.hub.ui.keycloak.KeycloakTokenParser;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

	@Autowired
	private JwtTokenUtil jwtUtil;

	@Autowired
	private KeycloakTokenParser keycloakTokenParser;

	@Autowired
	FederationHubUIConfig fedHubConfig;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// try cookie first
		String cookieToken = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				// for normal username + password auth from a file / DB
				if ("hubState".equals(cookie.getName())) {
					cookieToken = cookie.getValue();
					if (!jwtUtil.validateAccessToken(cookieToken)) {
						filterChain.doFilter(request, response);
						return;
					} else {
						setAuthenticationContext(cookieToken, request);
						filterChain.doFilter(request, response);
						return;
					}
				}

				// for keycloak
				if ("access_token".equals(cookie.getName())) {
					cookieToken = cookie.getValue();
					Claims claims = null;
					try {
						claims = keycloakTokenParser.parseClaims(cookieToken, SignatureAlgorithm.RS256);
					} catch (io.jsonwebtoken.ExpiredJwtException e) {
						// refresh the token if it expired
						String refreshToken = (String) request.getSession().getAttribute(fedHubConfig.getKeycloakRefreshTokenName());
						if (Strings.isNullOrEmpty(refreshToken)) {
							SecurityContextHolder.clearContext();
							AuthCookieUtils.logout(request, response, null);
							return;
						}

						// build up the parameters for the token request
						MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();
						requestBody.add("grant_type", "refresh_token");
						requestBody.add("refresh_token", refreshToken);
						requestBody.add("client_id", fedHubConfig.getKeycloakClientId());
						requestBody.add("client_secret", fedHubConfig.getKeycloakSecret());

						try {
							cookieToken = jwtUtil.processAuthServerRequest(requestBody, request, response);
							claims = keycloakTokenParser.parseClaims(cookieToken, SignatureAlgorithm.RS256);
						} catch (ParseException ee) {
							filterChain.doFilter(request, response);
							return;
						}
					}

					if (claims == null) {
						throw new InvalidTokenException("Unable to parse claims from token : " + cookieToken);
					}
										
					// make sure the keycloak token has an email and contains the admin claim
					if (claims.get("email") != null) {
						boolean hasAuthorization = false;
						
						Object authClaim = claims.get(fedHubConfig.getKeycloakClaimName());
						
						if (authClaim instanceof ArrayList<?>) {
							hasAuthorization = ((ArrayList<String>) authClaim).contains(fedHubConfig.getKeycloakAdminClaimValue());
						}
						
						if (authClaim instanceof String) {
							fedHubConfig.getKeycloakAdminClaimValue().equals((String) authClaim);
						}
												
						if (hasAuthorization) {
							setKeycloakAuth(cookieToken, request);
							filterChain.doFilter(request, response);
							return;
						} else {
							filterChain.doFilter(request, response);
							return;
						}
					} else {
						filterChain.doFilter(request, response);
						return;
					}
				}
			}
		}

		// no cookies found, last resort is auth bearer
		if (!hasAuthorizationBearer(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = getAccessToken(request);
		if (!jwtUtil.validateAccessToken(token)) {
			filterChain.doFilter(request, response);
			return;
		}

		setAuthenticationContext(token, request);
		filterChain.doFilter(request, response);
	}

	private boolean hasAuthorizationBearer(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (ObjectUtils.isEmpty(header) || !header.startsWith("Bearer")) {
			return false;
		}

		return true;
	}

	private String getAccessToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		String token = header.split(" ")[1].trim();
		return token;
	}
	
	private void setKeycloakAuth(String token, HttpServletRequest request) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		Claims claims = keycloakTokenParser.parseClaims(token, SignatureAlgorithm.RS256);

		KeycloakToken authentication = new KeycloakToken(token, null, authorities);

		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void setAuthenticationContext(String token, HttpServletRequest request) {
		UserDetails userDetails = getUserDetails(token);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				null);

		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private UserDetails getUserDetails(String token) {
		HubAuthUser userDetails = new HubAuthUser();
        String[] jwtSubject = jwtUtil.getSubject(token).split(",");

        userDetails.setUsername(jwtSubject[0]);

		return userDetails;
	}
	
	private static class KeycloakToken extends AbstractAuthenticationToken {

		private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

		private final Object principal;

		private Object credentials;

		public KeycloakToken(Object principal, Object credentials,
				Collection<? extends GrantedAuthority> authorities) {
			super(authorities);
			this.principal = principal;
			this.credentials = credentials;
			super.setAuthenticated(true); // must use super, as we override
		}

		@Override
		public Object getCredentials() {
			return this.credentials;
		}

		@Override
		public Object getPrincipal() {
			return this.principal;
		}

		@Override
		public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
			Assert.isTrue(!isAuthenticated,
					"Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
			super.setAuthenticated(false);
		}

		@Override
		public void eraseCredentials() {
			super.eraseCredentials();
			this.credentials = null;
		}
	}
}