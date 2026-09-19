package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.config.OidcGroupAccess;
import com.bbn.marti.groups.OAuthAuthenticator;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import tak.server.oidc.MappingResult.Decision;
import tak.server.oidc.OidcProviderRegistry.Generation;

/**
 * Review v02 H1/F-01, F-03, F-07 and M1: the registry loader's availability semantics, the deny-all mapper for a
 * malformed mapping, the explicit stored-assertion flag, and the trustAllCerts refusal.
 */
public class OidcRegistryFailClosedTest {

    static final String NAME = "test-idp";
    static final String ISSUER = "https://idp.example/realms/tak";
    static final String JWKS_URI = "https://idp.example/realms/tak/certs";
    private static RSAKey providerKey;
    private static String jwksJson;

    @BeforeClass
    public static void keys() throws Exception {
        providerKey = new RSAKeyGenerator(2048).keyID("fc-k1").keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256).generate();
        jwksJson = new JWKSet(providerKey.toPublicJWK()).toString();
    }

    static final class JwksStub extends RestTemplate {
        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> exchange(RequestEntity<?> request, Class<T> responseType) {
            assertEquals(JWKS_URI, request.getUrl().toString());
            return (ResponseEntity<T>) ResponseEntity.ok(jwksJson);
        }
    }

    static OidcProvider provider() {
        OidcProviderConfig config = new OidcProviderConfig.Builder()
                .name(NAME)
                .issuer(ISSUER)
                .authorizationEndpoint("https://idp.example/authorize")
                .tokenEndpoint("https://idp.example/token")
                .jwksUri(JWKS_URI)
                .webClientId("tak-web")
                .redirectUri("https://tak.example/login/redirect")
                .build();
        return new OidcProvider(config, new JwksStub());
    }

    static Oauth.OpenIdDiscoveryConfiguration enabled(String name, String issuer) {
        Oauth.OpenIdDiscoveryConfiguration cfg = new Oauth.OpenIdDiscoveryConfiguration();
        cfg.setName(name);
        cfg.setEnabled(true);
        cfg.setIssuer(issuer);
        cfg.setConfigurationUri("https://127.0.0.1:1/.well-known/openid-configuration");
        cfg.setClientId("tak-web");
        cfg.setSecret("secret");
        cfg.setRedirectUri("https://tak.example/login/redirect");
        return cfg;
    }

    static Oauth oauthWith(Oauth.OpenIdDiscoveryConfiguration... cfgs) {
        Oauth oauth = new Oauth();
        for (Oauth.OpenIdDiscoveryConfiguration cfg : cfgs) {
            oauth.getOpenIdDiscoveryConfiguration().add(cfg);
        }
        return oauth;
    }

    static String tokenWithIssuer(String issuer) throws Exception {
        return sign(new JWTClaimsSet.Builder().issuer(issuer).subject("alice").build());
    }

    static String sign(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("fc-k1").build(), claims);
        jwt.sign(new RSASSASigner(providerKey));
        return jwt.serialize();
    }

    private static final OidcProviderRegistry.Discovery UNREACHABLE = cfg -> {
        throw new IOException("connect refused: " + cfg.getConfigurationUri());
    };

    // ---- H1 / F-01: loader semantics -------------------------------------------------------------

    @Test
    public void h1RefreshFailureRetainsTheLastGoodProvider() throws Exception {
        Oauth oauth = oauthWith(enabled(NAME, ISSUER));
        OidcProvider lastGood = provider();
        Generation previous = OidcProviderRegistry.build(oauth, Collections.singletonList(lastGood));

        Generation refreshed = OidcProviderRegistry.build(oauth, previous, UNREACHABLE);
        OidcProviderRegistry registry = new OidcProviderRegistry(refreshed);

        assertTrue(registry.isConfigured());
        assertFalse("the last good provider is retained across a failed refresh", registry.isEmpty());
        assertSame(lastGood, registry.byIssuer(ISSUER));
        assertSame(lastGood, registry.providerFor(tokenWithIssuer(ISSUER)));
        assertTrue(registry.unavailableProviders().isEmpty());
    }

    @Test
    public void h1ColdStartWithEveryProviderFailingIsConfiguredButUnavailableNotLegacy() throws Exception {
        Oauth oauth = oauthWith(enabled(NAME, ISSUER));

        OidcProviderRegistry cold = new OidcProviderRegistry(OidcProviderRegistry.build(oauth, null, UNREACHABLE));

        assertTrue("configured comes from CoreConfig, not from discovery", cold.isConfigured());
        assertTrue(cold.isUnavailable());
        assertTrue(cold.isEmpty());
        assertEquals(Collections.singletonList(NAME), cold.unavailableProviders());
        assertTrue(cold.isConfiguredIssuer(ISSUER));
        assertFalse(cold.isConfiguredIssuer(ISSUER + "/"));
        assertNull("no live provider can verify anything", cold.providerFor(tokenWithIssuer(ISSUER)));

        OidcProviderRegistry legacy = new OidcProviderRegistry(OidcProviderRegistry.build(new Oauth(), null, UNREACHABLE));
        assertFalse("no enabled element: the pre-Phase-1 path", legacy.isConfigured());
        assertFalse(legacy.isUnavailable());
        assertFalse(legacy.isConfiguredIssuer(ISSUER));
    }

    @Test
    public void h1RefreshThatCanBuildNothingThrowsSoTheLoaderKeepsThePreviousGeneration() throws Exception {
        Oauth oauth = oauthWith(enabled(NAME, ISSUER));
        Generation unavailable = OidcProviderRegistry.build(oauth, null, UNREACHABLE);
        try {
            OidcProviderRegistry.build(oauth, unavailable, UNREACHABLE);
            fail("a refresh with nothing to keep must throw, not publish an empty generation");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("previous generation"));
        }
    }

    @Test
    public void h1SuccessfulRefreshReplacesTheRetainedProvider() throws Exception {
        Oauth oauth = oauthWith(enabled(NAME, ISSUER));
        Generation previous = OidcProviderRegistry.build(oauth, Collections.singletonList(provider()));
        OidcProvider fresh = provider();
        Generation refreshed = OidcProviderRegistry.build(oauth, previous, cfg -> fresh);
        assertSame(fresh, new OidcProviderRegistry(refreshed).byIssuer(ISSUER));
    }

    @Test
    public void h1EnabledProviderWithoutPinnedIssuerIsRefusedBeforeDiscovery() throws Exception {
        AtomicInteger discoveries = new AtomicInteger();
        Oauth oauth = oauthWith(enabled(NAME, null));
        Generation gen = OidcProviderRegistry.build(oauth, null, cfg -> {
            discoveries.incrementAndGet();
            return provider();
        });
        assertEquals("no network call without a pinned issuer", 0, discoveries.get());
        OidcProviderRegistry registry = new OidcProviderRegistry(gen);
        assertTrue(registry.isConfigured());
        assertTrue(registry.isUnavailable());
    }

    // ---- F-03: a malformed groupMapping denies OIDC identities and nothing else ------------------

    @Test
    public void f03EmptyGroupInMappingDoesNotThrowAndDeniesEveryIdentity() throws Exception {
        Oauth oauth = oauthWith(enabled(NAME, ISSUER));
        Oauth.GroupMapping bad = new Oauth.GroupMapping();
        bad.setClaimValue("ops");
        bad.setGroup("");
        bad.setAccess(OidcGroupAccess.BOTH);
        oauth.getGroupMapping().add(bad);
        oauth.setDefaultGroup("Guests");
        oauth.setLegacyGroupPassthrough(true);

        OidcProvider live = provider();
        OidcProviderRegistry registry = new OidcProviderRegistry(OidcProviderRegistry.build(oauth, null, cfg -> live));

        assertSame("the provider is still registered; only the mapping is disabled", live, registry.byIssuer(ISSUER));
        assertEquals(Decision.DENY_NO_MAPPING,
                registry.groupMapper().map(Collections.singletonList("ops"), false).getDecision());
        assertEquals("neither defaultGroup nor legacy pass-through survives a malformed mapping",
                Decision.DENY_NO_MAPPING,
                registry.groupMapper().map(Collections.singletonList("Operations"), true).getDecision());

        OidcProviderRegistry viaPackageBuild = new OidcProviderRegistry(
                OidcProviderRegistry.build(oauth, Collections.singletonList(live)));
        assertEquals(Decision.DENY_NO_MAPPING,
                viaPackageBuild.groupMapper().map(Collections.singletonList("ops"), false).getDecision());
    }

    // ---- M1: trustAllCerts on an enabled provider is refused, naming the field --------------------

    @Test
    public void m1TrustAllCertsOnAnEnabledProviderIsRefusedByTheRegistrationPath() throws Exception {
        Oauth.OpenIdDiscoveryConfiguration cfg = enabled(NAME, ISSUER);
        cfg.setTrustAllCerts(true);
        try {
            OidcProviderRegistry.productionDiscovery(60, null).discover(cfg);
            fail("trustAllCerts must be refused before any network call");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("trustAllCerts"));
            assertTrue(e.getMessage(), e.getMessage().contains("truststoreFile"));
        }
        OidcProviderRegistry registry = new OidcProviderRegistry(
                OidcProviderRegistry.build(oauthWith(cfg), null, OidcProviderRegistry.productionDiscovery(60, null)));
        assertTrue(registry.isConfigured());
        assertTrue(registry.isUnavailable());
    }

    // ---- F-07: expiry is waived by the explicit flag, never by the certificate alone -------------

    @Test
    public void f07ExpiredTokenWithCertificateButWithoutStoredAssertionFlagIsRejected() throws Exception {
        // Mocked: the User constructor joins the Ignite cluster, which a unit test must not do.
        AuthenticatedUser withCert = mock(AuthenticatedUser.class);
        when(withCert.getCert()).thenReturn(X509OidcRouteTest.certificate());
        AuthenticatedUser noCert = mock(AuthenticatedUser.class);
        when(noCert.getCert()).thenReturn(null);

        assertFalse("a certificate alone does not waive expiry", OAuthAuthenticator.expiryWaived(withCert, false));
        assertTrue(OAuthAuthenticator.expiryWaived(withCert, true));
        assertFalse("the flag without the certificate does not waive expiry", OAuthAuthenticator.expiryWaived(noCert, true));

        Date past = new Date(System.currentTimeMillis() - 3_600_000L);
        String expired = sign(new JWTClaimsSet.Builder().issuer(ISSUER).subject("alice").audience("tak-web")
                .issueTime(new Date(past.getTime() - 60_000L)).expirationTime(past).build());
        OidcProvider provider = provider();
        try {
            provider.validate(expired, OAuthAuthenticator.expiryWaived(withCert, false));
            fail("expired token on a non-stored-assertion path must be rejected");
        } catch (OidcValidationException e) {
            assertEquals(OidcValidationException.Reason.EXP, e.getReason());
        }
        assertEquals("alice", provider.validate(expired, OAuthAuthenticator.expiryWaived(withCert, true)).getSubject());
    }
}
