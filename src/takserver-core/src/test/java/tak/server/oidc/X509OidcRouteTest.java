package tak.server.oidc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.Oauth;
import com.bbn.marti.groups.X509Authenticator;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.tak.tls.TakCert;
import com.bbn.tak.tls.repository.TakCertRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Review v02 H1: the certificate route with the provider configured but not live. The stored assertion names the
 * configured issuer, the registry has no provider for it, and the connection must be refused with an audit line,
 * never routed to the file, LDAP or anonymous branches.
 */
public class X509OidcRouteTest {

    /** Self-signed, CN=oidc-test, RSA 2048, valid 2026 to 2036. Only its bytes matter (fingerprint lookup). */
    private static final String CERT_PEM = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDCTCCAfGgAwIBAgIUObDo6A3tVfwI0t0lhVQyD8CxSL8wDQYJKoZIhvcNAQEL\n"
            + "BQAwFDESMBAGA1UEAwwJb2lkYy10ZXN0MB4XDTI2MDkxOTA1NDYzNloXDTM2MDkx\n"
            + "NjA1NDYzNlowFDESMBAGA1UEAwwJb2lkYy10ZXN0MIIBIjANBgkqhkiG9w0BAQEF\n"
            + "AAOCAQ8AMIIBCgKCAQEA1JHptiRbGkrY5BkDSEIBB+SnsGSLf0KZZnldVcoJjaIN\n"
            + "pPdHQQyIWZY2RFhtlmWvBhJIUZFIyHNOIMSFzDlmHukdwUiV5ozfAS4osPJyPhKS\n"
            + "NxNOpFQjk5KAJWAyEoLq8QQgH7fFf9yAqzlq6QxtKdJwtz1uWuf62TaEQ0IydSjm\n"
            + "I1B9MDTQEiiZycc7nEmwcfDB31OJyVbzhrORL8HyJf7ifHGcnM2PX8CkiGKVMqz9\n"
            + "qwgnfHGRQ4lOAwwij4eb+3Ry1e514f53stZIDYP7b84W01jz2mWos421Yrv+RucA\n"
            + "PzYjSehl3eCBvCV5f50Q1QJVj843yveafXRs1p5DwQIDAQABo1MwUTAdBgNVHQ4E\n"
            + "FgQUzbR2d9jYqp5ZkJMdPhcXtUOBPVowHwYDVR0jBBgwFoAUzbR2d9jYqp5ZkJMd\n"
            + "PhcXtUOBPVowDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAdv+J\n"
            + "683MTIyussZBPLOkxZT+4GGpvW562oYcZVERm0f6aCVua3+KFVuy3PqBdH8KrGV0\n"
            + "GWKX1i3WhkuWFy5J086TKVx693Io8pJLe23ITNWCci5olV7yX4G7WfbFIRCVAKuy\n"
            + "sWI+v9PLH/KBJXG4INvs7JGsoG6frE87rVtv6NEcSzU1rfkBVZwffYKYdcZHd6Bi\n"
            + "5JAtvWy/s4wKre+8K5D2vBxcF0a5ZAIxnO7RsFSIMoVn23RvlrVyS3m5jBxCAz+t\n"
            + "dSjdEx0rVrYk7UTVdYK4DKvNPCqdNGxM8xW314s/h6/7S8sJksgmjhxQZ3ODheNS\n"
            + "6UvmAuE/+rbbBVvxMQ==\n"
            + "-----END CERTIFICATE-----\n";

    static X509Certificate certificate() throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(CERT_PEM.getBytes(StandardCharsets.US_ASCII)));
    }

    private ListAppender<ILoggingEvent> audit;
    private Logger auditLogger;

    @Before
    public void setUp() throws Exception {
        OidcRegistryFailClosedTest.keys();
        auditLogger = (Logger) LoggerFactory.getLogger("tak.server.oidc.audit");
        audit = new ListAppender<>();
        audit.start();
        auditLogger.addAppender(audit);
    }

    @After
    public void tearDown() throws Exception {
        auditLogger.detachAppender(audit);
        OidcProviderRegistry.reset();
        facade(null);
    }

    /** X509Authenticator reads CoreConfig through the static facade; give it a plain in-memory configuration. */
    private static void facade(CoreConfigFacade instance) throws Exception {
        Field field = CoreConfigFacade.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, instance);
    }

    private static Configuration configurationWith(Oauth oauth) {
        Configuration configuration = new Configuration();
        Auth auth = new Auth();
        auth.setOauth(oauth);
        configuration.setAuth(auth);
        return configuration;
    }

    @Test
    public void h1CertificateEnrolledWithAConfiguredButUnavailableProviderIsRefusedAndAudited() throws Exception {
        Oauth oauth = OidcRegistryFailClosedTest.oauthWith(
                OidcRegistryFailClosedTest.enabled(OidcRegistryFailClosedTest.NAME, OidcRegistryFailClosedTest.ISSUER));
        CoreConfigFacade config = mock(CoreConfigFacade.class);
        when(config.getRemoteConfiguration()).thenReturn(configurationWith(oauth));
        facade(config);

        // configured (one enabled element) but no live provider: the cold-start outage state
        OidcProviderRegistry.install(OidcProviderRegistry.build(oauth, Collections.emptyList()));

        TakCert record = new TakCert();
        record.setSubjectDn("CN=oidc-test");
        record.setToken(OidcRegistryFailClosedTest.tokenWithIssuer(OidcRegistryFailClosedTest.ISSUER));
        TakCertRepository certificates = mock(TakCertRepository.class);
        when(certificates.findOneByHash(anyString())).thenReturn(record);
        GroupManager groupManager = mock(GroupManager.class);

        X509Authenticator x509 = new X509Authenticator(groupManager, null, certificates, null);
        // Mocked: the User constructor joins the Ignite cluster, which a unit test must not do. The route under
        // test reads only the certificate before it decides.
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getCert()).thenReturn(certificate());

        try {
            x509.authenticate(user);
            fail("an OIDC-enrolled certificate must be refused while its provider is unavailable");
        } catch (TakException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("not available"));
        }

        verify(certificates).findOneByHash(anyString());
        verify(groupManager, never()).authenticate(anyString(), any(User.class));
        verify(groupManager, never()).addUserToGroup(any(User.class), any());
        verify(groupManager, never()).updateGroups(any(User.class), any());

        boolean denied = false;
        for (ILoggingEvent event : audit.list) {
            String line = event.getFormattedMessage();
            if (line.contains("outcome=DENY surface=CERT") && line.contains("reason=provider-unavailable")
                    && line.contains("iss=" + OidcRegistryFailClosedTest.ISSUER)) {
                denied = true;
            }
        }
        assertTrue("one audit DENY line with surface=CERT reason=provider-unavailable: " + audit.list, denied);
    }
}
