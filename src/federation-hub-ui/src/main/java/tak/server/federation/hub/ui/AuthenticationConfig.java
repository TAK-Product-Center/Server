package tak.server.federation.hub.ui;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import javax.naming.InvalidNameException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.federation.hub.ui.manage.AuthManager;
import tak.server.federation.hub.ui.manage.AuthorizationFileWatcher;

/* Enables X.509 client authentication. */

@Configuration
@EnableWebSecurity
public class AuthenticationConfig extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationConfig.class);

    @Autowired
    private AuthorizationFileWatcher authFileWatcher;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        super.configure(auth);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Enable X.509 client authentication.
        http.x509().authenticationUserDetailsService(new X509AuthenticatedUserDetailsService())
            .and().authorizeRequests().anyRequest().authenticated()
            .and().csrf().disable();
    }

    private class X509AuthenticatedUserDetailsService
            implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

        @Override
        public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token)
                throws UsernameNotFoundException {
            X509Certificate certificate = (X509Certificate)token.getCredentials();

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
