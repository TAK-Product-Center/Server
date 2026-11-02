package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import tak.server.oidc.AuthorizationRequestStore.PendingRequest;

/**
 * {@link Pkce} against the RFC 7636 test vector, and {@link AuthorizationRequestStore} for single use, TTL and
 * bounding. Threat model rows: T6 (PKCE binding), T7 (state consumed server-side), T19 (bounded memory).
 */
public class PkceAndStateTest {

    // ---- PKCE ---------------------------------------------------------------------------------------

    @Test
    public void s256MatchesRfc7636AppendixB() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challengeS256(verifier));
    }

    @Test
    public void verifiersAreWellFormedAndUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String v = Pkce.newVerifier();
            assertEquals(43, v.length());
            assertTrue(Pkce.isValidVerifier(v));
            assertTrue("verifier must be fresh", seen.add(v));
        }
    }

    @Test
    public void challengeRejectsMalformedVerifiers() {
        String[] bad = {null, "", "short", "has space in it and is otherwise long enough to pass the length",
                "x".repeat(129), "ok-length-but-has-a-plus+sign-which-is-not-unreserved-ok"};
        for (String v : bad) {
            assertFalse(String.valueOf(v), Pkce.isValidVerifier(v));
            try {
                Pkce.challengeS256(v);
                throw new AssertionError("accepted " + v);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }

    @Test
    public void challengeIsDeterministicAndVerifierSpecific() {
        String a = Pkce.newVerifier();
        String b = Pkce.newVerifier();
        assertEquals(Pkce.challengeS256(a), Pkce.challengeS256(a));
        assertNotEquals(Pkce.challengeS256(a), Pkce.challengeS256(b));
        assertNotEquals("challenge must not reveal the verifier", a, Pkce.challengeS256(a));
    }

    // ---- state store ---------------------------------------------------------------------------------

    /** A clock the test can move. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-09-18T22:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    public void stateIsConsumedExactlyOnce() {
        AuthorizationRequestStore store = new AuthorizationRequestStore(Duration.ofMinutes(10), 100, new TestClock());
        String state = AuthorizationRequestStore.newToken();
        store.issue(state, "verifier-1", "nonce-1", "idp");

        PendingRequest first = store.consume(state);
        assertNotNull(first);
        assertEquals("verifier-1", first.getCodeVerifier());
        assertEquals("nonce-1", first.getNonce());
        assertEquals("idp", first.getProviderName());

        assertNull("replay of the same state must fail (T7)", store.consume(state));
        assertEquals(0, store.size());
    }

    @Test
    public void unknownStateIsRejected() {
        AuthorizationRequestStore store = new AuthorizationRequestStore(Duration.ofMinutes(10), 100, new TestClock());
        store.issue("real", "v", "n", "idp");
        assertNull(store.consume("forged"));
        assertNull(store.consume(""));
        assertNull(store.consume(null));
        assertEquals("a rejected lookup must not consume the real one", 1, store.size());
    }

    @Test
    public void expiredStateIsRejected() {
        TestClock clock = new TestClock();
        AuthorizationRequestStore store = new AuthorizationRequestStore(Duration.ofMinutes(10), 100, clock);
        store.issue("s", "v", "n", "idp");
        clock.now = clock.now.plus(Duration.ofMinutes(11));
        assertNull(store.consume("s"));
        assertEquals(0, store.size());
    }

    @Test
    public void stateInsideTtlIsAccepted() {
        TestClock clock = new TestClock();
        AuthorizationRequestStore store = new AuthorizationRequestStore(Duration.ofMinutes(10), 100, clock);
        store.issue("s", "v", "n", "idp");
        clock.now = clock.now.plus(Duration.ofMinutes(9));
        assertNotNull(store.consume("s"));
    }

    @Test
    public void storeIsBoundedAndEvictsOldest() {
        TestClock clock = new TestClock();
        AuthorizationRequestStore store = new AuthorizationRequestStore(Duration.ofMinutes(10), 3, clock);
        for (int i = 0; i < 3; i++) {
            store.issue("s" + i, "v", "n", "idp");
            clock.now = clock.now.plusSeconds(1);
        }
        store.issue("s3", "v", "n", "idp");
        assertEquals(3, store.size());
        assertNull("oldest evicted (T19)", store.consume("s0"));
        assertNotNull(store.consume("s1"));
        assertNotNull(store.consume("s3"));
    }

    @Test
    public void tokensAreFreshAndUrlSafe() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String t = AuthorizationRequestStore.newToken();
            assertEquals(43, t.length());
            assertTrue(t.matches("^[A-Za-z0-9_-]+$"));
            assertTrue(seen.add(t));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroTtlIsRefused() {
        new AuthorizationRequestStore(Duration.ZERO, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyStateCannotBeIssued() {
        new AuthorizationRequestStore(Duration.ofMinutes(1), 10).issue("", "v", "n", "idp");
    }
}
