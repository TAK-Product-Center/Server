package tak.server.federation.hub.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import tak.server.federation.hub.ui.GroupHolder;
import tak.server.federation.hub.ui.StringEdge;
import tak.server.federation.hub.ui.UidHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FederationPolicy {

    private String name;
    private String version;
    private String type;

    private Set<UidHolder> federate_nodes;
    private Set<StringEdge> federate_edges;
    private Set<GroupHolder> groups;
    private Set<String> filter_objects;
    private Map<String, Object> additionalData;


    public FederationPolicy() {
        federate_nodes = new HashSet<>();
        federate_edges = new HashSet<>();
        groups = new HashSet<>();
        filter_objects = new HashSet<>();
        additionalData = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<UidHolder> getFederate_nodes() {
        return federate_nodes;
    }

    public void setFederate_nodes(Set<UidHolder> federate_nodes) {
        this.federate_nodes = federate_nodes;
    }

    public Set<StringEdge> getFederate_edges() {
        return federate_edges;
    }

    public void setFederate_edges(Set<StringEdge> federate_edges) {
        this.federate_edges = federate_edges;
    }

    public Set<GroupHolder> getGroups() {
        return groups;
    }

    public void setGroups(Set<GroupHolder> federation_groups) {
        this.groups = federation_groups;
    }

    public Set<String> getFilter_objects() {
        return filter_objects;
    }

    public void setFilter_objects(Set<String> filter_objects) {
        this.filter_objects = filter_objects;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public void addAdditionalData(String key, Object value) {
        additionalData.put(key, value);
    }
}
