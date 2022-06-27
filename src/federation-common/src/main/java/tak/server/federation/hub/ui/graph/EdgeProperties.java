package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 5/16/2017.
 */

public class EdgeProperties {
    private String name;
    @JsonProperty("edgeFilters")
    private List<FilterNode> filters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
            return null;
        } else if (filters.size() > 1) {
            return FilterUtils.filterNodeToString(filters.get(0));
        }

        return FilterUtils.oredFiltersToString(filters);
    }


    /**
     * Catch all for Json properties that are not specifically defined above.
     * Adding this catch-all allows storage and recall of any Json fields from the client.
     */
    protected Map<String, Object> other = new HashMap<String, Object>();

    @JsonAnyGetter
    public Map<String,Object> getOther() {
        return other;
    }

    @JsonAnySetter
    public void addOther(String name, Object value) {
        other.put(name, value);
    }
}
