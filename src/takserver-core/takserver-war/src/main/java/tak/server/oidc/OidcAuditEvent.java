package tak.server.oidc;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.groups.Group;

/**
 * One line per OIDC authentication decision on the logger {@code tak.server.oidc.audit} (R-17, threat model T20).
 *
 * The formatter takes typed fields. There is no parameter of any kind that could carry a token, an authorization
 * code or a header, so a leak is a compile error rather than a code review finding. Free-text fields (provider,
 * issuer, subject, user, reason) are sanitised to one line with no spaces, so a line always parses as
 * {@code key=value} pairs.
 *
 * Schema, fixed:
 * <pre>
 * outcome=ALLOW|DENY surface=BEARER|BROWSER|CERT provider=&lt;name&gt; iss=&lt;issuer&gt; sub=&lt;sub&gt; user=&lt;takKey&gt;
 * port=&lt;localPort&gt; groups=[name:IN,name:OUT,...] admin=true|false reason=&lt;code&gt;
 * </pre>
 * {@code reason} on ALLOW is the mapper's reason ({@code mapping}, {@code legacy}, {@code default-group}); on DENY
 * it is the validator's {@link OidcValidationException.Reason} in lower case or {@code no-mapping}.
 */
public final class OidcAuditEvent {

    private static final Logger AUDIT = LoggerFactory.getLogger("tak.server.oidc.audit");

    /** Where the assertion arrived. */
    public enum Surface {
        /** Authorization: Bearer or access_token cookie on an API port (ATAK enrollment, WebTAK calls). */
        BEARER,
        /** The browser callback on /login/redirect (the ID token). */
        BROWSER,
        /** A certificate connection re-reading the token stored at enrollment. */
        CERT
    }

    private OidcAuditEvent() {
    }

    public static void allow(Surface surface, OidcIdentity identity, String takUser, int port,
            Collection<Group> groups, boolean admin, String reason) {
        AUDIT.info(format("ALLOW", surface, identity.getProviderName(), identity.getIssuer(), identity.getSubject(),
                takUser, port, groups, admin, reason));
    }

    public static void deny(Surface surface, String provider, String issuer, String subject, int port,
            OidcValidationException.Reason reason) {
        AUDIT.warn(format("DENY", surface, provider, issuer, subject, null, port, Collections.emptySet(), false,
                reason == null ? "unknown" : reason.name().toLowerCase(java.util.Locale.ENGLISH)));
    }

    public static void denyNoMapping(Surface surface, OidcIdentity identity, String takUser, int port) {
        AUDIT.warn(format("DENY", surface, identity.getProviderName(), identity.getIssuer(), identity.getSubject(),
                takUser, port, Collections.emptySet(), false, "no-mapping"));
    }

    /** Package-private so the test can pin the schema without a logger. */
    static String format(String outcome, Surface surface, String provider, String issuer, String subject,
            String takUser, int port, Collection<Group> groups, boolean admin, String reason) {
        StringBuilder sb = new StringBuilder(160);
        sb.append("outcome=").append(outcome);
        sb.append(" surface=").append(surface == null ? "UNKNOWN" : surface.name());
        sb.append(" provider=").append(clean(provider));
        sb.append(" iss=").append(clean(issuer));
        sb.append(" sub=").append(clean(subject));
        sb.append(" user=").append(clean(takUser));
        sb.append(" port=").append(port);
        sb.append(" groups=[");
        boolean first = true;
        for (String g : new TreeSet<>(groupStrings(groups))) {
            if (!first) {
                sb.append(',');
            }
            sb.append(g);
            first = false;
        }
        sb.append(']');
        sb.append(" admin=").append(admin);
        sb.append(" reason=").append(clean(reason));
        return sb.toString();
    }

    private static Collection<String> groupStrings(Collection<Group> groups) {
        Collection<String> out = new TreeSet<>();
        if (groups == null) {
            return out;
        }
        for (Group g : groups) {
            if (g != null && g.getName() != null) {
                out.add(clean(g.getName()) + ":" + (g.getDirection() == null ? "?" : g.getDirection().name()));
            }
        }
        return out;
    }

    /** One line, no whitespace, bounded length, never null. */
    static String clean(String value) {
        if (value == null || value.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder(Math.min(value.length(), 200));
        for (int i = 0; i < value.length() && sb.length() < 200; i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c) || c == '=' || c == '[' || c == ']'
                    || c == ',') {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
