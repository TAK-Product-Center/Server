package tak.server.federation;

import java.util.HashSet;
import java.util.Set;

/*
 * Object to represent Federate Edges from the federate policy language. This
 * should be kept up to date with changes in the language. This object should
 * be maintained such that it is naturally serializable.
 *
 * Currently, this object has:
 *     Source - federate identity of the source node. This does not point directly
 *       to the node to avoid a circular dependency.
 *     Destination - federate identity of the destination node. This does not
 *       point directly to the node to avoid a circular dependency.
 *     Filter Set - a set of filter methods that apply to this edge.
 */
public class FederateEdge {
	
	public enum GroupFilterType {
		ALL, ALLOWED, DISALLOWED, ALLOWED_AND_DISALLOWED
		
	}
	
	public static GroupFilterType getGroupFilterType(String name) {
		if ("allgroups".equals(name.toLowerCase())) {
			return GroupFilterType.ALL;
		}
		
		if ("allowed".equals(name.toLowerCase())) {
			return GroupFilterType.ALLOWED;
		}
		
		if ("disallowed".equals(name.toLowerCase())) {
			return GroupFilterType.DISALLOWED;
		}
		
		if ("allowedanddisallowed".equals(name.toLowerCase())) {
			return GroupFilterType.ALLOWED_AND_DISALLOWED;
		}
		
		return GroupFilterType.ALL;
	}
	
    private final FederateIdentity source;
    private final FederateIdentity destination;
    private final Set<String> allowedGroups;
    private final Set<String> disallowedGroups;
    private final GroupFilterType filterType;

    public FederateEdge(FederateIdentity source, FederateIdentity destination,
            Set<String> allowedGroups, Set<String> disallowedGroups, GroupFilterType filterType) {
        this.source = source;
        this.destination = destination;
        
        this.allowedGroups = allowedGroups;
        this.disallowedGroups = disallowedGroups;
        this.filterType = filterType;
    }
    
    

    public Set<String> getAllowedGroups() {
		return allowedGroups;
	}

	public Set<String> getDisallowedGroups() {
		return disallowedGroups;
	}

	public GroupFilterType getFilterType() {
		return filterType;
	}

	public FederateIdentity getSourceIdentity() {
        return source;
    }

    public FederateIdentity getDestinationIdentity() {
        return destination;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) { return true; }
        if (object == null || getClass() != object.getClass()) { return false; }

        FederateEdge that = (FederateEdge) object;

        if (!source.equals(that.source)) { return false; }
        if (!destination.equals(that.destination)) { return false; }
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + destination.hashCode();
        return result;
    }

    @Override
	public String toString() {
		return "FederateEdge [source=" + source + ", destination=" + destination + ", allowedGroups=" + allowedGroups
				+ ", disallowedGroups=" + disallowedGroups + ", filterType=" + filterType + "]";
	}
}
