package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

/**
 * Object to represent a Federate edge as managed by the front end
 */
@JsonTypeName("EdgeCell")
public class EdgeCell extends PolicyObjectCell {
    private String type = "Policy";

    @JsonProperty("roger_federation")
    @JsonDeserialize(as=EdgeProperties.class)
    private EdgeProperties properties;


    @JsonIgnore
    @SuppressWarnings("unchecked")
    public String getSourceId() {
        return (String) ((Map<String, Object>)this.other.get("source")).get("id");
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public String getDestinationId() {
        return (String) ((Map<String, Object>) this.other.get("target")).get("id");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EdgeProperties getProperties() {
        return properties;
    }

    public void setProperties(EdgeProperties properties) {
        this.properties = properties;
    }
}
