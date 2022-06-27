package tak.server.federation;

import java.util.HashSet;
import java.util.Set;

public class Federate extends FederationNode {
    private final Set<FederateIdentity> groupIdentities;

    public Federate(FederateIdentity federateIdentity) {
        super(federateIdentity);
        groupIdentities = new HashSet<>();
    }

    public Federate(String nodeName, FederateIdentity federateIdentity) {
        super(nodeName, federateIdentity);
        groupIdentities = new HashSet<>();
    }

    public Set<FederateIdentity> getGroupIdentities() {
        return groupIdentities;
    }

    public void addGroupIdentity(FederateIdentity groupIdentity) {
        groupIdentities.add(groupIdentity);
    }

    @Override
    public int hashCode() {
        int result =  super.hashCode();
        result = 31 * result + groupIdentities.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        Federate federate = (Federate) other;

        return groupIdentities.equals(federate.groupIdentities);
    }
}
