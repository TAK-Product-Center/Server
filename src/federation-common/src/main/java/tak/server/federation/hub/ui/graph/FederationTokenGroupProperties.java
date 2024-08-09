package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FederationTokenGroupProperties extends NodeProperties {
    private boolean interconnected;
    @JsonProperty("groupFilters")
    private List<FilterNode> filters;
    private List<TokenNode> tokens;

    public boolean isInterconnected() {
        return interconnected;
    }

    public void setInterconnected(boolean interconnected) {
        this.interconnected = interconnected;
    }

    public List<TokenNode> getTokens() {
		return tokens;
	}

	public void setTokens(List<TokenNode> tokens) {
		this.tokens = tokens;
	}

	public List<FilterNode> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterNode> filters) {
        this.filters = filters;
    }

    /**
     * Returns the filter expression generated from the filter node tree of this edge.  If there are multiple top level
     * nodes, they will be OR'ed.
     */
    @JsonIgnore
    public String getFilterExpression() {
        if (filters.size() < 1) {
            return "";
        } else if (filters.size() > 1) {
            return FilterUtils.filterNodeToString(filters.get(0));
        }

        return FilterUtils.oredFiltersToString(filters);
    }

}
