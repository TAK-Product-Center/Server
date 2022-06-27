package tak.server.federation;

import java.util.HashSet;
import java.util.Set;

/*
 * POJO representing a federate group in the federate policy. If groups
 * are supported by a policy implementation, an incoming or outgoing edge
 * involving this group indicates all nodes in the group are managed by
 * that edge. If interconnectivity is enabled, then all nodes within a
 * group are all also connected to each other by a bi-directional edge,
 * and the optional filter applies to this connection.
 */
public class FederateGroup extends FederationNode {
    private boolean interconnected;
    private String filterExpression;
    private final Set<Federate> federatesInGroup;

    public FederateGroup(FederateIdentity federateIdentity) {
        super(federateIdentity);
        this.interconnected = true;
        this.federatesInGroup = new HashSet<>();
        this.filterExpression = "";
    }

    public FederateGroup(FederateIdentity federateIdentity, boolean interconnected) {
        super(federateIdentity);
        this.interconnected = interconnected;
        this.federatesInGroup = new HashSet<>();
        this.filterExpression = "";
    }

    /* If there is a group filter expression, the group is interconnected. */
    public FederateGroup(FederateIdentity federateIdentity, String filterExpression) {
        super(federateIdentity);
        this.interconnected = true;
        this.filterExpression = filterExpression;
        this.federatesInGroup = new HashSet<>();
    }

    public FederateGroup(String name, FederateIdentity federateIdentity) {
        super(name, federateIdentity);
        this.interconnected = true;
        this.federatesInGroup = new HashSet<>();
        this.filterExpression = "";
    }

    public FederateGroup(String name, FederateIdentity federateIdentity, boolean interconnected) {
        super(name, federateIdentity);
        this.interconnected = interconnected;
        this.federatesInGroup = new HashSet<>();
        this.filterExpression = "";
    }

    /* If there is a group filter expression, the group is interconnected. */
    public FederateGroup(String name, FederateIdentity federateIdentity, String filterExpression) {
        super(name, federateIdentity);
        this.interconnected = true;
        this.filterExpression = filterExpression;
        this.federatesInGroup = new HashSet<>();
    }

    public boolean isInterconnected() {
        return interconnected;
    }

    public void setInterconnected(boolean interconnected) {
        this.interconnected = interconnected;
    }

    public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    public Set<Federate> getFederatesInGroup() {
        return federatesInGroup;
    }

    public void addFederateToGroup(Federate federate) {
        federatesInGroup.add(federate);
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

        FederateGroup that = (FederateGroup) other;

        if (interconnected != that.interconnected) {
            return false;
        }
        if (filterExpression != null
                ? !filterExpression.equals(that.filterExpression)
                : that.filterExpression != null) {
            return false;
        }
        return federatesInGroup.equals(that.federatesInGroup);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (interconnected ? 1 : 0);
        result = 31 * result + (filterExpression != null ? filterExpression.hashCode() : 0);
        result = 31 * result + federatesInGroup.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
