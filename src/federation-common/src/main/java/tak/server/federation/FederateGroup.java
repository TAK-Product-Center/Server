package tak.server.federation;

import java.util.HashSet;
import java.util.Objects;
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
    private boolean allowTokenAuth;
    private long tokenAuthDuration;
    private final Set<Federate> federatesInGroup;

    public FederateGroup(FederateIdentity federateIdentity) {
        super(federateIdentity);
        this.interconnected = true;
        this.federatesInGroup = new HashSet<>();
    }


    public boolean isInterconnected() {
        return interconnected;
    }

    public void setInterconnected(boolean interconnected) {
        this.interconnected = interconnected;
    }

    public Set<Federate> getFederatesInGroup() {
        return federatesInGroup;
    }

    public void addFederateToGroup(Federate federate) {
        federatesInGroup.add(federate);
    }

    public boolean isAllowTokenAuth() {
		return allowTokenAuth;
	}


	public void setAllowTokenAuth(boolean allowTokenAuth) {
		this.allowTokenAuth = allowTokenAuth;
	}


	public long getTokenAuthDuration() {
		return tokenAuthDuration;
	}


	public void setTokenAuthDuration(long tokenAuthDuration) {
		this.tokenAuthDuration = tokenAuthDuration;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FederateGroup other = (FederateGroup) obj;
		return allowTokenAuth == other.allowTokenAuth && Objects.equals(federatesInGroup, other.federatesInGroup)
				&& interconnected == other.interconnected && tokenAuthDuration == other.tokenAuthDuration;
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(allowTokenAuth, federatesInGroup, interconnected, tokenAuthDuration);
		return result;
	}

    @Override
    public String toString() {
        return super.toString();
    }
}
