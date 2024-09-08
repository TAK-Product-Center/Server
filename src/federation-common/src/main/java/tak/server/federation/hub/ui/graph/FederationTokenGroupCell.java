package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonTypeName("FederationTokenGroupCell")
public class FederationTokenGroupCell extends PolicyObjectCell {
    private String type = "FederationTokenGroup";

    @JsonProperty("roger_federation")
    @JsonDeserialize(as=FederationTokenGroupProperties.class)
    private FederationTokenGroupProperties properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FederationTokenGroupProperties getProperties() {
        return properties;
    }

    public void setProperties(FederationTokenGroupProperties properties) {
        this.properties = properties;
    }

}
