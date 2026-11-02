package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Stable external identity tests for requirement R-04. The key is {@code oidc-} plus 40 hex characters of
 * SHA-256 over the length-delimited exact issuer and exact subject (review finding 4: readable encodings
 * collide or leak unsafe characters).
 */
public class OidcIdentityTest {

    private static String key(String issuer) {
        return key(issuer, "sub");
    }

    private static String key(String issuer, String sub) {
        return new OidcIdentity("provider", issuer, sub, Collections.emptyMap()).getIdentityKey();
    }

    private static String expected(String issuer, String sub) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        byte[] i = issuer.getBytes(StandardCharsets.UTF_8);
        d.update((i.length + ":").getBytes(StandardCharsets.US_ASCII));
        d.update(i);
        d.update((byte) ':');
        d.update(sub.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder("oidc-");
        for (byte b : Arrays.copyOf(d.digest(), 20)) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    public void r04KeyIsThePinnedDigestOfIssuerAndSubject() throws Exception {
        assertEquals(expected("https://lastwall.idcommand.lastwall.com/idp", "sub"),
                key("https://lastwall.idcommand.lastwall.com/idp"));
        assertEquals(expected("https://kc.local:8443/realms/tak", "f47ac10b-58cc-4372-a567-0e02b2c3d479"),
                key("https://kc.local:8443/realms/tak", "f47ac10b-58cc-4372-a567-0e02b2c3d479"));
    }

    @Test
    public void r04KeyIsBoundedAndCertificateCommonNameSafe() {
        String k = key("https://" + "x".repeat(200) + ".example/realms/" + "y".repeat(100), "z".repeat(255));
        assertEquals(45, k.length());
        assertTrue(k, k.matches("^oidc-[0-9a-f]{40}$"));
    }

    @Test
    public void r04SameHostDifferentIssuerPathsProduceDifferentKeys() {
        assertNotEquals(key("https://idp.example/realms/blue"), key("https://idp.example/realms/red"));
        // the collision a separator-based encoding has: a/b versus a_b
        assertNotEquals(key("https://h.example/a/b"), key("https://h.example/a_b"));
        // a trailing slash is a different issuer string, so a different identity
        assertNotEquals(key("https://idp.example/idp"), key("https://idp.example/idp/"));
    }

    @Test
    public void r04SubjectsDifferingOnlyInCaseAreDifferentIdentitiesEvenWhenLowerCased() {
        String a = key("https://idp.example/idp", "Alice");
        String b = key("https://idp.example/idp", "alice");
        assertNotEquals(a, b);
        // TAK lower-cases usernames in the group cache; the keys stay distinct after that too
        assertNotEquals(a.toLowerCase(), b.toLowerCase());
    }

    @Test
    public void r04LengthDelimitingKeepsIssuerAndSubjectApart() {
        // "https://h/a" + "b:c" versus "https://h/a:b" + "c" would collide under naive concatenation
        assertNotEquals(key("https://h.example/a", "b:c"), key("https://h.example/a:b", "c"));
    }

    @Test
    public void r04EveryIdentityKeyMatchesTakMartiSafeString() throws Exception {
        Properties validation = new Properties();
        try (InputStream in = OidcIdentityTest.class.getResourceAsStream("/validation.properties")) {
            assertNotNull("validation.properties must be on the test runtime classpath", in);
            validation.load(in);
        }
        Pattern martiSafeString = Pattern.compile(validation.getProperty("Validator.MartiSafeString"));
        List<String> keys = Arrays.asList(
                key("https://lastwall.idcommand.lastwall.com/idp"),
                key("https://kc.local:8443/realms/tak"),
                key("https://idp.example/"),
                key("https://IDP.Example/realm/blue", "sub with spaces and /slashes/ and %20"));
        for (String identityKey : keys) {
            assertTrue(identityKey + " must match Validator.MartiSafeString",
                    martiSafeString.matcher(identityKey).matches());
        }
    }
}
