package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

import tak.server.oidc.OidcValidationException.Reason;

/**
 * Exercises {@link OidcTokenValidator} against real RS256 tokens and a JWKS served by a stubbed HTTP client.
 * No network. Threat model rows: T1 (aud), T2 (iss), T3 (exp), T4 (alg), T5 (kid), T8 (no token in messages).
 */
public class OidcTokenValidatorTest {

    private static final String ISSUER = "https://idp.test/realms/tak";
    private static final String JWKS_URI = "https://idp.test/realms/tak/protocol/openid-connect/certs";
    private static final List<String> AUDIENCES = Arrays.asList("tak-web", "atak-civ");

    private static RSAKey providerKey;   // the provider's real signing key, published in the JWKS
    private static RSAKey rogueKey;      // an attacker's key, never published
    private static String jwksJson;

    @BeforeClass
    public static void keys() throws Exception {
        providerKey = new RSAKeyGenerator(2048).keyID("k1").keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256).generate();
        rogueKey = new RSAKeyGenerator(2048).keyID("k1").keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256).generate();
        jwksJson = new JWKSet(providerKey.toPublicJWK()).toString();
    }

    /** Serves the canned JWKS for the configured URI; counts fetches so T5/T19 can assert on them. */
    static final class JwksStub extends RestTemplate {
        final AtomicInteger fetches = new AtomicInteger();
        final String body;
        final boolean fail;

        JwksStub(String body, boolean fail) {
            this.body = body;
            this.fail = fail;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> exchange(RequestEntity<?> request, Class<T> responseType) {
            fetches.incrementAndGet();
            if (fail) {
                throw new ResourceAccessException("connection refused (stub)");
            }
            assertEquals(JWKS_URI, request.getUrl().toString());
            return (ResponseEntity<T>) ResponseEntity.ok(body);
        }
    }

    private static OidcTokenValidator.Settings settings() {
        return new OidcTokenValidator.Settings("test-idp", ISSUER, JWKS_URI, AUDIENCES, Duration.ofSeconds(60), null);
    }

    private static OidcTokenValidator validator(RestOperations rest) {
        return new OidcTokenValidator(settings(), rest);
    }

    private static JWTClaimsSet.Builder goodClaims() {
        long now = System.currentTimeMillis();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject("alice")
                .audience("tak-web")
                .issueTime(new Date(now))
                .expirationTime(new Date(now + 5 * 60 * 1000))
                .claim("preferred_username", "alice")
                .claim("groups", Arrays.asList("tak_ops"));
    }

    private static String sign(RSAKey key, JWSAlgorithm alg, String kid, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(alg).keyID(kid).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static String signGood(JWTClaimsSet claims) throws Exception {
        return sign(providerKey, JWSAlgorithm.RS256, "k1", claims);
    }

    private static OidcValidationException expectReject(OidcTokenValidator v, String token, Reason... acceptable) {
        try {
            v.validate(token);
        } catch (OidcValidationException e) {
            assertTrue("reason " + e.getReason() + " not in " + Arrays.toString(acceptable) + ": " + e.getMessage(),
                    Arrays.asList(acceptable).contains(e.getReason()));
            // T8: the token never appears in anything that could reach a log.
            if (token != null && token.length() > 8) {
                assertFalse("token leaked into exception message", e.getMessage().contains(token));
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    assertFalse("token leaked into cause message", e.getCause().getMessage().contains(token));
                }
            }
            return e;
        }
        fail("expected rejection with " + Arrays.toString(acceptable));
        return null;
    }

    // ---- happy path ------------------------------------------------------------------------------

    @Test
    public void validTokenYieldsIssuerQualifiedIdentity() throws Exception {
        JwksStub stub = new JwksStub(jwksJson, false);
        OidcIdentity id = validator(stub).validate(signGood(goodClaims().build()));
        assertEquals(ISSUER, id.getIssuer());
        assertEquals("alice", id.getSubject());
        assertEquals("alice@idp.test_realms_tak", id.getIdentityKey());
        assertEquals("test-idp", id.getProviderName());
        assertEquals("alice", id.getStringClaim("preferred_username", null));
        assertEquals(Arrays.asList("tak_ops"), id.getClaim("groups"));
        assertEquals(1, stub.fetches.get());
    }

    @Test
    public void azpAloneSatisfiesAudience() throws Exception {
        JWTClaimsSet claims = goodClaims().audience(Collections.emptyList()).claim("azp", "atak-civ").build();
        OidcIdentity id = validator(new JwksStub(jwksJson, false)).validate(signGood(claims));
        assertEquals("alice", id.getSubject());
    }

    @Test
    public void jwksIsCachedAcrossTokens() throws Exception {
        JwksStub stub = new JwksStub(jwksJson, false);
        OidcTokenValidator v = validator(stub);
        for (int i = 0; i < 5; i++) {
            v.validate(signGood(goodClaims().subject("user" + i).build()));
        }
        assertEquals("one JWKS fetch for five tokens (T19)", 1, stub.fetches.get());
    }

    // ---- T1 audience ------------------------------------------------------------------------------

    @Test
    public void wrongAudienceIsRejected() throws Exception {
        expectReject(validator(new JwksStub(jwksJson, false)),
                signGood(goodClaims().audience("other-client").build()), Reason.AUD);
    }

    @Test
    public void missingAudienceAndAzpIsRejected() throws Exception {
        expectReject(validator(new JwksStub(jwksJson, false)),
                signGood(goodClaims().audience(Collections.emptyList()).build()), Reason.AUD);
    }

    // ---- T2 issuer --------------------------------------------------------------------------------

    @Test
    public void wrongIssuerIsRejectedEvenWithTheRightKey() throws Exception {
        expectReject(validator(new JwksStub(jwksJson, false)),
                signGood(goodClaims().issuer("https://other-idp.test/realms/tak").build()), Reason.ISS);
    }

    @Test
    public void missingIssuerIsRejected() throws Exception {
        expectReject(validator(new JwksStub(jwksJson, false)),
                signGood(goodClaims().issuer(null).build()), Reason.ISS);
    }

    // ---- T3 time ----------------------------------------------------------------------------------

    @Test
    public void expiredTokenIsRejected() throws Exception {
        long past = System.currentTimeMillis() - 5 * 60 * 1000;
        expectReject(validator(new JwksStub(jwksJson, false)),
                signGood(goodClaims().issueTime(new Date(past - 600_000)).expirationTime(new Date(past)).build()), Reason.EXP);
    }

    @Test
    public void expiryInsideClockSkewIsAccepted() throws Exception {
        long justPast = System.currentTimeMillis() - 30 * 1000;
        validator(new JwksStub(jwksJson, false)).validate(
                signGood(goodClaims().issueTime(new Date(justPast - 600_000)).expirationTime(new Date(justPast)).build()));
    }

    @Test
    public void notYetValidTokenIsRejected() throws Exception {
        long future = System.currentTimeMillis() + 10 * 60 * 1000;
        expectReject(validator(new JwksStub(jwksJson, false)),
                signGood(goodClaims().notBeforeTime(new Date(future)).build()), Reason.NBF);
    }

    // ---- T4 algorithm -----------------------------------------------------------------------------

    @Test
    public void unsignedTokenIsRejected() throws Exception {
        String token = new PlainJWT(goodClaims().build()).serialize();
        expectReject(validator(new JwksStub(jwksJson, false)), token, Reason.ALG, Reason.MALFORMED);
    }

    @Test
    public void hmacWithThePublicKeyAsSecretIsRejected() throws Exception {
        byte[] secret = providerKey.toRSAPublicKey().getEncoded();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("k1").build(), goodClaims().build());
        jwt.sign(new MACSigner(secret));
        expectReject(validator(new JwksStub(jwksJson, false)), jwt.serialize(), Reason.ALG);
    }

    @Test
    public void algorithmNotMatchingThePublishedKeyIsRejected() throws Exception {
        // The JWKS says k1 is an RS256 key. A valid RS512 signature with the same key must not verify.
        String token = sign(providerKey, JWSAlgorithm.RS512, "k1", goodClaims().build());
        expectReject(validator(new JwksStub(jwksJson, false)), token, Reason.ALG, Reason.KID);
    }

    @Test
    public void settingsPinAlgorithmsAndCanNeverNameHmac() {
        // SignatureAlgorithm is Spring's asymmetric-only enum; HMAC lives in MacAlgorithm and cannot be passed
        // here at all. The type is the guard; this test pins the default and the explicit form.
        OidcTokenValidator.Settings pinned = new OidcTokenValidator.Settings("x", ISSUER, JWKS_URI, AUDIENCES, null,
                Arrays.asList(SignatureAlgorithm.RS256));
        assertEquals(Collections.singleton(SignatureAlgorithm.RS256), pinned.getAlgorithms());
        OidcTokenValidator.Settings all = new OidcTokenValidator.Settings("x", ISSUER, JWKS_URI, AUDIENCES, null, null);
        assertEquals(OidcTokenValidator.ASYMMETRIC, all.getAlgorithms());
        for (SignatureAlgorithm a : SignatureAlgorithm.values()) {
            assertFalse(a.getName().startsWith("HS"));
        }
    }

    // ---- T5 keys ----------------------------------------------------------------------------------

    @Test
    public void tokenSignedByAnUnpublishedKeyIsRejected() throws Exception {
        String token = sign(rogueKey, JWSAlgorithm.RS256, "k1", goodClaims().build());
        expectReject(validator(new JwksStub(jwksJson, false)), token, Reason.SIG);
    }

    @Test
    public void unknownKeyIdIsRejectedAfterABoundedRefetch() throws Exception {
        JwksStub stub = new JwksStub(jwksJson, false);
        OidcTokenValidator v = validator(stub);
        v.validate(signGood(goodClaims().build()));
        int before = stub.fetches.get();
        String token = sign(providerKey, JWSAlgorithm.RS256, "k9", goodClaims().build());
        expectReject(v, token, Reason.KID);
        assertTrue("at most one refetch on unknown kid, saw " + (stub.fetches.get() - before),
                stub.fetches.get() - before <= 1);
    }

    @Test
    public void unreachableJwksFailsClosed() throws Exception {
        expectReject(validator(new JwksStub(jwksJson, true)), signGood(goodClaims().build()), Reason.JWKS);
    }

    // ---- claims and inputs ------------------------------------------------------------------------

    @Test
    public void missingSubjectIsRejected() throws Exception {
        expectReject(validator(new JwksStub(jwksJson, false)),
                signGood(goodClaims().subject(null).build()), Reason.CLAIMS);
    }

    @Test
    public void garbageIsRejectedAsMalformed() {
        expectReject(validator(new JwksStub(jwksJson, false)), "not.a.jwt", Reason.MALFORMED, Reason.SIG, Reason.ALG);
        expectReject(validator(new JwksStub(jwksJson, false)), "", Reason.MALFORMED);
        expectReject(validator(new JwksStub(jwksJson, false)), null, Reason.MALFORMED);
    }

    // ---- T18 / T24 configuration -----------------------------------------------------------------

    @Test
    public void plaintextEndpointsAreRefusedAtConstruction() {
        try {
            new OidcTokenValidator.Settings("x", "http://idp.test", JWKS_URI, AUDIENCES, null, null);
            fail("http issuer must be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("issuer"));
        }
        try {
            new OidcTokenValidator.Settings("x", ISSUER, "http://idp.test/certs", AUDIENCES, null, null);
            fail("http jwks_uri must be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("jwksUri"));
        }
    }

    @Test
    public void emptyAudienceListIsRefusedAtConstruction() {
        try {
            new OidcTokenValidator.Settings("x", ISSUER, JWKS_URI, Collections.emptyList(), null, null);
            fail("empty audiences must be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("audience"));
        }
        try {
            new OidcTokenValidator.Settings("x", ISSUER, JWKS_URI, Arrays.asList(" ", ""), null, null);
            fail("blank audiences must be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("audience"));
        }
    }

    /** R-04: the key is iss+sub, whole issuer. Two realms on one host are two identities (IDCOMMAND-FACTS). */
    @Test
    public void identityKeyKeepsHostPortAndPath() {
        OidcIdentity id = new OidcIdentity("p", "https://idp.tactical.local:8443/realms/ops", "u-42", Collections.emptyMap());
        assertEquals("u-42@idp.tactical.local:8443_realms_ops", id.getIdentityKey());
        OidcIdentity other = new OidcIdentity("p", "https://idp.tactical.local:8443/realms/dev", "u-42", Collections.emptyMap());
        assertFalse(id.getIdentityKey().equals(other.getIdentityKey()));
        OidcIdentity idc = new OidcIdentity("p", "https://lastwall.idcommand.lastwall.com/idp", "u-42", Collections.emptyMap());
        assertEquals("u-42@lastwall.idcommand.lastwall.com_idp", idc.getIdentityKey());
        assertTrue(id.toString().contains(id.getIdentityKey()));
    }
}
