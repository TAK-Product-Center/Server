package tak.server.federation.hub.ui;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import jakarta.servlet.http.HttpServletResponse;
import tak.server.federation.hub.ui.jwt.UserService;
import tak.server.federation.hub.ui.manage.AuthManager;
import tak.server.federation.hub.ui.manage.AuthorizationFileWatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

//	@Autowired
//	private UserService userService;

	@Autowired
	private JwtTokenFilter jwtTokenFilter;

	@Autowired
	private AuthorizationFileWatcher authFileWatcher;

//	@Override
//	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//		auth.userDetailsService(username -> {
//			return userService.loadUserByUsername(username);
//		});
//	}
	
	@Bean
	public AuthenticationManager authenticationManager(UserService userService) {
		
		 DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		 authenticationProvider.setUserDetailsService(userService);
		 return new ProviderManager(authenticationProvider);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		http.csrf().disable();
		
		http.x509().authenticationUserDetailsService(new X509AuthenticatedUserDetailsService());

		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);

//		http.authorizeRequests().antMatchers("/error","/oauth/**", "/login**", "/login/**", "/bowerDependencies/**", "/favicon.ico").permitAll()
//			.anyRequest().authenticated();
		http.authorizeHttpRequests().requestMatchers("/error","/oauth/**", "/login**", "/login/**", "/bowerDependencies/**", "/favicon.ico").permitAll()
			.anyRequest().authenticated();

		http.exceptionHandling().authenticationEntryPoint((request, response, ex) -> {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
		});

		http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
		
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