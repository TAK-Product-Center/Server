package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.config.OidcIdentityKey;
import com.bbn.marti.oauth.OAuthApi;
import com.bbn.marti.util.spring.TakAuthenticationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Review v02 packet 2a: D2 well-known contract, openid exact token, M2 identity mode, F-05 token gating, D3 log hygiene. */
public class OidcV02Packet2aTest {

    private static OidcProviderConfig.Builder validBuilder() {
        return new OidcProviderConfig.Builder()
                .name("idcommand")
                .issuer("https://idp.example/idp")
                .authorizationEndpoint("https://idp.example/authorize")
                .tokenEndpoint("https://idp.example/token")
                .jwksUri("https://idp.example/jwks")
                .webClientId("tak-web")
                .redirectUri("https://tak.example/login/redirect")
                .androidClientId("atak-civ");
    }

    // D2

    @Test
    public void d2WellKnownScopesIsAnArrayOfExactTokensAndAndroidRedirectIsPresent() throws Exception {
        OidcProviderConfig cfg = validBuilder().scopes("openid  profile groups").androidRedirect("loopback").build();
        String json = new ObjectMapper().writeValueAsString(OAuthApi.openIdConfiguration(cfg));
        assertTrue(json, json.contains("\"scopes\":[\"openid\",\"profile\",\"groups\"]"));
        assertTrue(json, json.contains("\"android_redirect\":\"loopback\""));
        assertTrue(json, json.contains("\"android_client_id\":\"atak-civ\""));
        assertTrue(json, json.contains("\"issuer\":\"https://idp.example/idp\""));
        assertTrue(json, json.contains("\"authorization_endpoint\":\"https://idp.example/authorize\""));
        assertTrue(json, json.contains("\"token_endpoint\":\"https://idp.example/token\""));
        assertTrue(json, json.contains("\"code_challenge_methods_supported\":[\"S256\"]"));
    }

    @Test
    public void d2AndroidRedirectDefaultsToScheme() throws Exception {
        OidcProviderConfig cfg = validBuilder().build();
        assertEquals("scheme", cfg.getAndroidRedirect());
        assertEquals(Arrays.asList("openid", "profile"), cfg.getScopeTokens());
        String json = new ObjectMapper().writeValueAsString(OAuthApi.openIdConfiguration(cfg));
        assertTrue(json, json.contains("\"android_redirect\":\"scheme\""));
        assertTrue(json, json.contains("\"scopes\":[\"openid\",\"profile\"]"));
    }

    @Test
    public void d2BlankAndroidRedirectKeepsTheDefaultAndUnknownValueIsRefusedNamingTheField() {
        assertEquals("scheme", validBuilder().androidRedirect(" ").build().getAndroidRedirect());
        assertEquals("loopback", validBuilder().androidRedirect(" loopback ").build().getAndroidRedirect());
        try {
            validBuilder().androidRedirect("custom-tab").build();
            fail("unknown androidRedirect must refuse registration");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("androidRedirect"));
            assertTrue(e.getMessage(), e.getMessage().contains("custom-tab"));
        }
    }

    // openid exact token

    @Test
    public void openidMustBeAnExactTokenNotASubstring() {
        for (String bad : new String[] { "not-openid profile", "openidx", "profile openid_connect", "profile" }) {
            try {
                validBuilder().scopes(bad).build();
                fail("scopes '" + bad + "' must be refused");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("openid"));
            }
        }
        assertEquals(Arrays.asList("groups", "openid"), validBuilder().scopes("groups openid").build().getScopeTokens());
        assertEquals(Arrays.asList("openid"), validBuilder().scopes("openid").build().getScopeTokens());
        assertEquals(Collections.emptyList(), OidcProviderConfig.scopeTokens(null));
    }

    // M2 / Astra rank 5

    @Test
    public void m2UsernameClaimOnAnEnabledProviderIsRefusedAtRegistrationAndNamesTheField() {
        Oauth oauth = OidcRegistryFailClosedTest.oauthWith(OidcRegistryFailClosedTest.enabled("idc", "https://idp.example/idp"));
        oauth.setIdentityKey(OidcIdentityKey.USERNAME_CLAIM);
        AtomicInteger discoveries = new AtomicInteger();
        ListAppender<ILoggingEvent> appender = capture(OidcProviderRegistry.class);
        try {
            OidcProviderRegistry.Generation gen = OidcProviderRegistry.build(oauth, null, cfg -> {
                discoveries.incrementAndGet();
                throw new IllegalStateException("must not be reached");
            });
            OidcProviderRegistry registry = new OidcProviderRegistry(gen);
            assertEquals(0, discoveries.get());
            assertTrue(registry.isConfigured());
            assertNull(registry.first());
            assertTrue(registry.unavailableProviders().toString(), registry.unavailableProviders().contains("idc"));
            String logged = appender.list.toString();
            assertTrue(logged, logged.contains("identityKey=\"usernameClaim\""));
            assertTrue(logged, logged.contains("R-04"));
        } finally {
            detach(OidcProviderRegistry.class, appender);
        }
    }

    @Test
    public void m2UsernameClaimStaysAvailableWhenNoProviderIsEnabled() {
        Oauth oauth = new Oauth();
        oauth.setIdentityKey(OidcIdentityKey.USERNAME_CLAIM);
        Oauth.OpenIdDiscoveryConfiguration legacy = new Oauth.OpenIdDiscoveryConfiguration();
        legacy.setName("legacy");
        oauth.getOpenIdDiscoveryConfiguration().add(legacy);
        OidcProviderRegistry registry = new OidcProviderRegistry(OidcProviderRegistry.build(oauth, null, cfg -> {
            throw new IllegalStateException("must not be reached");
        }));
        assertFalse(registry.isConfigured());
        assertEquals(OidcIdentityKey.USERNAME_CLAIM, registry.identityKeyMode());
        // the pure check itself never throws for issuerSubject or an absent oauth block
        OidcProviderRegistry.refuseUsernameClaimOnEnabled(null);
        Oauth ok = new Oauth();
        ok.setIdentityKey(OidcIdentityKey.ISSUER_SUBJECT);
        OidcProviderRegistry.refuseUsernameClaimOnEnabled(ok);
    }

    // F-05

    @Test
    public void f05OnlyAnOidcIssuedTokenIsStoredOnTheCertificateRecord() {
        assertEquals("oidc.jwt", OidcTokenGate.tokenToStore("oidc.jwt", t -> true));
        assertNull(OidcTokenGate.tokenToStore("legacy.jwt", t -> false));
        assertNull(OidcTokenGate.tokenToStore(null, t -> true));
        assertNull(OidcTokenGate.tokenToStore("", t -> true));
        assertTrue(OidcTokenGate.isOidcToken("x", t -> t.equals("x")));
        assertFalse(OidcTokenGate.isOidcToken("y", t -> t.equals("x")));
    }

    @Test
    public void f05UnreadableExpRefusesOnlyForOidcTokens() {
        Date dflt = new Date(1_000L);
        assertEquals(new Date(1_700_000_000_000L), OidcTokenGate.notAfter(1_700_000_000L, true, dflt));
        assertEquals(new Date(1_700_000_000_000L), OidcTokenGate.notAfter(1_700_000_000L, false, dflt));
        assertSame(dflt, OidcTokenGate.notAfter(null, false, dflt));
        try {
            OidcTokenGate.notAfter(null, true, dflt);
            fail("an OIDC assertion without a readable exp must refuse signing");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("exp"));
        }
    }

    // D3

    @Test
    public void d3AuthProviderLogLineForABearerPrincipalNeverContainsTheToken() {
        String token = "eyJhbGciOiJSUzI1NiJ9.SECRET-BODY-3f9a.SIGNATURE-7c1e";
        ListAppender<ILoggingEvent> appender = capture(TakAuthenticationProvider.class);
        try {
            TakAuthenticationProvider.logAuthenticationAttempt(new BearerTokenAuthenticationToken(token));
            assertEquals(1, appender.list.size());
            String line = appender.list.get(0).getFormattedMessage();
            assertFalse(line, line.contains(token));
            assertFalse(line, line.contains("SECRET-BODY"));
            assertTrue(line, line.contains("BearerTokenAuthenticationToken"));
            assertTrue(line, line.contains("len=" + token.length()));
            String described = TakAuthenticationProvider.describeAuthentication(new BearerTokenAuthenticationToken(token));
            assertFalse(described, described.contains(token));
            assertEquals("none", TakAuthenticationProvider.describeToken(null));
            assertNotNull(TakAuthenticationProvider.describeAuthentication(null));
        } finally {
            detach(TakAuthenticationProvider.class, appender);
        }
    }

    private static ListAppender<ILoggingEvent> capture(Class<?> cls) {
        Logger logger = (Logger) LoggerFactory.getLogger(cls);
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detach(Class<?> cls, ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(cls)).detachAppender(appender);
    }
}
