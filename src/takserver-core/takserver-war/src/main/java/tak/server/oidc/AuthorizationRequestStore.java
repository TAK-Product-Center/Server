package tak.server.oidc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outstanding browser authorization requests, keyed by the {@code state} value, each consumed exactly once.
 *
 * Upstream compares the callback's {@code state} with a cookie and expires the cookie; nothing server-side
 * changes, so the same code, state and cookie replay until the IdP refuses the code (threat model T7). This
 * store makes the state single-use and time-bounded on the server, and holds the PKCE verifier and the OIDC
 * nonce for the request so they never travel to the browser.
 *
 * Keys are stored hashed; a memory dump does not yield usable states. The store is bounded: past
 * {@code maxEntries} the oldest requests are evicted, which only ever costs an attacker-triggered login
 * attempt a retry.
 *
 * In-memory and per node. In a TAK Server cluster the callback must land on the node that issued the
 * request, which the existing cookie-based flow already requires (the state cookie is per node too).
 */
public final class AuthorizationRequestStore {

    /** What the callback needs to complete one request. Immutable. */
    public static final class PendingRequest {
        private final String codeVerifier;
        private final String nonce;
        private final String providerName;
        private final Instant issuedAt;

        PendingRequest(String codeVerifier, String nonce, String providerName, Instant issuedAt) {
            this.codeVerifier = codeVerifier;
            this.nonce = nonce;
            this.providerName = providerName;
            this.issuedAt = issuedAt;
        }

        public String getCodeVerifier() {
            return codeVerifier;
        }

        public String getNonce() {
            return nonce;
        }

        public String getProviderName() {
            return providerName;
        }

        public Instant getIssuedAt() {
            return issuedAt;
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, PendingRequest> pending = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final int maxEntries;
    private final Clock clock;

    public AuthorizationRequestStore(Duration ttl, int maxEntries) {
        this(ttl, maxEntries, Clock.systemUTC());
    }

    AuthorizationRequestStore(Duration ttl, int maxEntries, Clock clock) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be at least 1");
        }
        this.ttl = ttl;
        this.maxEntries = maxEntries;
        this.clock = clock;
    }

    /** A fresh 256-bit value, base64url without padding. Used for both state and nonce. */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Records a new request under {@code state}. Returns the same state for convenience.
     */
    public String issue(String state, String codeVerifier, String nonce, String providerName) {
        if (state == null || state.isEmpty()) {
            throw new IllegalArgumentException("state required");
        }
        evictExpired();
        while (pending.size() >= maxEntries) {
            evictOldest();
        }
        pending.put(hash(state), new PendingRequest(codeVerifier, nonce, providerName, clock.instant()));
        return state;
    }

    /**
     * Removes and returns the request for {@code state}, or null when it is unknown, already consumed, or
     * expired. A second call with the same state always returns null: that is the replay guard.
     */
    public PendingRequest consume(String state) {
        if (state == null || state.isEmpty()) {
            return null;
        }
        PendingRequest request = pending.remove(hash(state));
        if (request == null) {
            return null;
        }
        if (request.issuedAt.plus(ttl).isBefore(clock.instant())) {
            return null;
        }
        return request;
    }

    public int size() {
        evictExpired();
        return pending.size();
    }

    private void evictExpired() {
        Instant cutoff = clock.instant().minus(ttl);
        Iterator<Map.Entry<String, PendingRequest>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().issuedAt.isBefore(cutoff)) {
                it.remove();
            }
        }
    }

    private void evictOldest() {
        String oldestKey = null;
        Instant oldest = null;
        for (Map.Entry<String, PendingRequest> e : pending.entrySet()) {
            if (oldest == null || e.getValue().issuedAt.isBefore(oldest)) {
                oldest = e.getValue().issuedAt;
                oldestKey = e.getKey();
            }
        }
        if (oldestKey != null) {
            pending.remove(oldestKey);
        }
    }

    private static String hash(String state) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(state.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
