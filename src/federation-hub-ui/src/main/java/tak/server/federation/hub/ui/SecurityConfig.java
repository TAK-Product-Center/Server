package tak.server.federation.hub.ui;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tak.server.federation.hub.ui.jwt.UserService;
import tak.server.federation.hub.ui.manage.AuthManager;
import tak.server.federation.hub.ui.manage.AuthorizationFileWatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
	
	@Autowired
	private JwtTokenUtil jwtUtil;

	@Autowired
	private AuthorizationFileWatcher authFileWatcher;
	
	@Autowired
	private FederationHubUIConfig fedHubConfig;

	@Bean
	public AuthenticationManager authenticationManager(UserService userService) {
		 DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		 authenticationProvider.setUserDetailsService(userService);
		 return new ProviderManager(authenticationProvider);
	}
	
	private RequestMatcher oauthPortMatch = new RequestMatcher() {
		@Override
		public boolean matches(HttpServletRequest request) {
			return fedHubConfig.isAllowOauth() && fedHubConfig.getOauthPort() != null 
					&& request.getLocalPort() == fedHubConfig.getOauthPort();
		}
	};
	
	private RequestMatcher mtlsPortMatch = new RequestMatcher() {
		@Override
		public boolean matches(HttpServletRequest request) {
			return request.getLocalPort() == fedHubConfig.getPort();
		}
	};

	@Bean
	@Order(1)
	public SecurityFilterChain mtlsChain(HttpSecurity http) throws Exception {
	    http.securityMatcher(mtlsPortMatch);

	    http.csrf().disable();
	    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);

	    http.x509().authenticationUserDetailsService(new X509AuthenticatedUserDetailsService());

	    http.authorizeHttpRequests(auth -> auth
	    		.requestMatchers(HttpMethod.GET, "/api/oauth/login/auth").denyAll()
	    	    .requestMatchers(HttpMethod.GET, "/api/oauth/login/redirect").denyAll()
	    	    .requestMatchers(HttpMethod.GET, "/login").denyAll()
	        .anyRequest().authenticated()
	    );

	    http.exceptionHandling().authenticationEntryPoint((req, res, ex) ->
	        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex == null ? "Unauthorized" : ex.getMessage()));

	    return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain oauthChain(HttpSecurity http) throws Exception {

		http.securityMatchers(sm -> sm
			    .requestMatchers(oauthPortMatch)    
			    .requestMatchers("/api/**")
			);
		
	    http.csrf().disable();
	    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

	    http.authorizeHttpRequests(auth -> auth
	    	    .requestMatchers(HttpMethod.GET, "/api/oauth/login/auth").permitAll()
	    	    .requestMatchers(HttpMethod.GET, "/api/oauth/login/redirect").permitAll()
	    	    .requestMatchers("/api/**").authenticated()
	    	    .anyRequest().permitAll()
	    	);

	    http.addFilterBefore(new JwtTokenFilter(fedHubConfig, jwtUtil), UsernamePasswordAuthenticationFilter.class);

	    
	    http.exceptionHandling(eh -> eh
	    	    .authenticationEntryPoint((req, res, ex) -> {
	    	    	// due to a bug with jetty, aviod using res.sendError and manually write the error instead
	    	        if (req.getRequestURI().startsWith("/api") || req.getServletPath().startsWith("/api")) {
	    	            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	    	            res.setContentType("text/plain;charset=UTF-8");
	    	            res.setCharacterEncoding("UTF-8");
	    	            try (var writer = res.getWriter()) {
	    	                writer.write("Unauthorized");
	    	                writer.flush();
	    	            } catch (IOException e) {
	    	                logger.error("Failed to write 401 response", e);
	    	            }
	    	        } else {
	    	            try {
	    	                res.sendRedirect("/login");
	    	            } catch (IOException e) {
	    	                logger.error("Redirect failed", e);
	    	            }
	    	        }
	    	    }));
	    	

	    return http.build();
	}
	
	private class X509AuthenticatedUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

		@Override
		public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
			X509Certificate certificate = (X509Certificate) token.getCredentials();
			String dn = certificate.getSubjectX500Principal().getName();
			
			try {
				String username = AuthManager.getCertificateUserName(certificate);
				String fingerprint = AuthManager.getCertificateFingerprint(certificate);
				if (authFileWatcher.userAuthorized(username, fingerprint))
					return new User(dn, "", AuthorityUtils.NO_AUTHORITIES);
			} catch (CertificateException | InvalidNameException | NoSuchAlgorithmException e) {
				logger.error("Unable to get username or fingerprint from certificate: " + e);
			}

			throw new UsernameNotFoundException("Authorized user for certificate " + dn + " not found");
		}
	}
}
