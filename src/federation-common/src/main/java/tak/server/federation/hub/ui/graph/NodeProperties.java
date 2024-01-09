package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeProperties {
    /**
     * Catch all for Json properties that are not specifically defined above.
     * Adding this catch-all allows storage and recall of any Json fields from the client.
     */
    protected Map<String, Object> other = new HashMap<String, Object>();
    private String name;
    private String id;
    private String description;
    private List<Object> attributes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Object> attributes) {
        this.attributes = attributes;
    }

    @JsonIgnore
    public Map<String, Object> attributesToMap() {
        return attributesToMap(this.attributes);
    }

    @SuppressWarnings("unchecked")
    private Object nodeToAttributeValue(Map<String, Object> node) {
        if ((node.get("type")).equals("attribute")) {
            return node.get("value");
        } else if ((node.get("type")).equals("attributes")) {
            return node.get("values");
        } else if ((node.get("type")).equals("nodes")) {
            return attributesToMap((List<Object>) node.get("nodes"));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> attributesToMap(List<Object> nodes) {
        Map<String, Object> attributesMap = new HashMap<>();
        for (Object attribute : nodes) {
            Map<String, Object> attributeAsMap = (Map<String, Object>) attribute;
            attributesMap.put((String) attributeAsMap.get("key"), nodeToAttributeValue(attributeAsMap));
        }

        return attributesMap;
    }

    @JsonAnyGetter
    public Map<String,Object> getOther() {
        return other;
    }

    @JsonAnySetter
    public void addOther(String name, Object value) {
        other.put(name, value);
    }
}
