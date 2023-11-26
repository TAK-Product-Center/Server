package tak.server.federation.hub.ui;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import tak.server.federation.FederateEdge.GroupFilterType;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StringEdge {

    private String source;
    private String destination;
    private String filterExpression;
    private Set<String> allowedGroups;
    private Set<String> disallowedGroups;
    private GroupFilterType groupsFilterType;

    public StringEdge(String source, String destination) {
        super();
        this.source = source;
        this.destination = destination;
    }

    public Set<String> getAllowedGroups() {
		return allowedGroups;
	}

	public void setAllowedGroups(Set<String> allowedGroups) {
		this.allowedGroups = allowedGroups;
	}

	public Set<String> getDisallowedGroups() {
		return disallowedGroups;
	}

	public void setDisallowedGroups(Set<String> disallowedGroups) {
		this.disallowedGroups = disallowedGroups;
	}

	public GroupFilterType getGroupsFilterType() {
		return groupsFilterType;
	}

	public void setGroupsFilterType(GroupFilterType groupFilterType) {
		this.groupsFilterType = groupFilterType;
	}

	public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public StringEdge() { }
}
