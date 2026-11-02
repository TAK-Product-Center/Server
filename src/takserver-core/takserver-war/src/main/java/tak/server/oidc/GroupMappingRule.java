package tak.server.oidc;

import java.util.Objects;

import com.bbn.marti.remote.groups.Direction;

/**
 * One configured mapping from a value found in the OIDC groups claim to a TAK group.
 *
 * Immutable. Built from the {@code <groupMapping>} elements under {@code <oauth>} in CoreConfig; kept as a
 * plain value type here so {@link GroupMapper} has no dependency on the JAXB-generated configuration classes
 * and can be tested without a CoreConfig.
 */
public final class GroupMappingRule {

    /** Which TAK directions the mapping grants. IN is write (client to server), OUT is read. */
    public enum Access {
        IN, OUT, BOTH;

        public boolean grantsIn() {
            return this == IN || this == BOTH;
        }

        public boolean grantsOut() {
            return this == OUT || this == BOTH;
        }
    }

    private final String claimValue;
    private final String group;
    private final Access access;

    public GroupMappingRule(String claimValue, String group, Access access) {
        if (claimValue == null || claimValue.isEmpty()) {
            throw new IllegalArgumentException("groupMapping claimValue must not be empty");
        }
        if (group == null || group.isEmpty()) {
            throw new IllegalArgumentException("groupMapping group must not be empty");
        }
        this.claimValue = claimValue;
        this.group = group;
        this.access = access == null ? Access.BOTH : access;
    }

    public String getClaimValue() {
        return claimValue;
    }

    public String getGroup() {
        return group;
    }

    public Access getAccess() {
        return access;
    }

    /** True when this rule grants the given direction. */
    public boolean grants(Direction direction) {
        return direction == Direction.IN ? access.grantsIn() : access.grantsOut();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupMappingRule)) return false;
        GroupMappingRule that = (GroupMappingRule) o;
        return claimValue.equals(that.claimValue) && group.equals(that.group) && access == that.access;
    }

    @Override
    public int hashCode() {
        return Objects.hash(claimValue, group, access);
    }

    @Override
    public String toString() {
        return "GroupMappingRule[" + claimValue + " -> " + group + " (" + access + ")]";
    }
}
