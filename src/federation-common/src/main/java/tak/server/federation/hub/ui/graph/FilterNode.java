package tak.server.federation.hub.ui.graph;

import java.util.List;

/**
 * Created on 7/18/2017.
 */
public class FilterNode {
    String type;
    EdgeFilter filter;
    List<FilterNode> nodes;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EdgeFilter getFilter() {
        return filter;
    }

    public void setFilter(EdgeFilter filter) {
        this.filter = filter;
    }

    public List<FilterNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<FilterNode> nodes) {
        this.nodes = nodes;
    }
}
