package tak.server.oidc;

import java.util.Collections;
import java.util.Set;

import com.bbn.marti.remote.groups.Group;

/**
 * Outcome of mapping an OIDC identity's claims to TAK groups.
 *
 * A DENY result carries no groups. The reason code is stable text intended for the audit log, never a
 * message built from token contents.
 */
public final class MappingResult {

    public enum Decision {
        ALLOW,
        /** The claims mapped to no permitted TAK group and no default group is configured. */
        DENY_NO_MAPPING
    }

    private final Decision decision;
    private final Set<Group> groups;
    private final boolean admin;
    private final String reason;

    private MappingResult(Decision decision, Set<Group> groups, boolean admin, String reason) {
        this.decision = decision;
        this.groups = Collections.unmodifiableSet(groups);
        this.admin = admin;
        this.reason = reason;
    }

    public static MappingResult allow(Set<Group> groups, boolean admin, String reason) {
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("an ALLOW result must carry at least one group");
        }
        return new MappingResult(Decision.ALLOW, groups, admin, reason);
    }

    public static MappingResult denyNoMapping() {
        return new MappingResult(Decision.DENY_NO_MAPPING, Collections.emptySet(), false, "no-mapping");
    }

    public Decision getDecision() {
        return decision;
    }

    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }

    /** Unhydrated groups (name + direction). The caller hydrates through GroupManager. Empty on DENY. */
    public Set<Group> getGroups() {
        return groups;
    }

    public boolean isAdmin() {
        return admin;
    }

    /** Stable reason code for the audit event: "mapping", "legacy", "default-group", "no-mapping". */
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "MappingResult[" + decision + " groups=" + groups.size() + " admin=" + admin + " reason=" + reason + "]";
    }
}
