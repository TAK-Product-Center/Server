package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonTypeName("GroupCell")
public class GroupCell extends PolicyObjectCell {
    private String type = "Group";

    @JsonProperty("roger_federation")
    @JsonDeserialize(as=GroupProperties.class)
    private GroupProperties properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public GroupProperties getProperties() {
        return properties;
    }

    public void setProperties(GroupProperties properties) {
        this.properties = properties;
    }

}
