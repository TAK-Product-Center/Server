package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonTypeName("FederationOutgoingCell")
public class FederationOutgoingCell extends PolicyObjectCell {
    private String type = "FederationOutgoing";

    @JsonProperty("federation")
    @JsonAlias("roger_federation")
    @JsonDeserialize(as=FederateOutgoingProperties.class)
    private FederateOutgoingProperties properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FederateOutgoingProperties getProperties() {
        return properties;
    }

    public void setProperties(FederateOutgoingProperties properties) {
        this.properties = properties;
    }

	@Override
	public String toString() {
		return "FederationOutgoingCell [type=" + type + ", properties=" + properties + "]";
	}
}
