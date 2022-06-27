package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Object to represent a Federate node as managed by the front end
 */
@JsonTypeName("FederateCell")
public class FederateCell extends PolicyObjectCell {
    private String type = "Federate";

    @JsonProperty("roger_federation")
    @JsonDeserialize(as=FederateProperties.class)
    private FederateProperties properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FederateProperties getProperties() {
        return properties;
    }

    public void setProperties(FederateProperties properties) {
        this.properties = properties;
    }
}
