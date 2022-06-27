package tak.server.federation.hub.ui.graph;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created on 5/16/2017.
 */
public class FederateProperties extends NodeProperties {
    @JsonProperty("groups")
    private List<String> groupIdentities;


    public List<String> getGroupIdentities() {
        return groupIdentities;
    }

    public void setGroupIdentities(List<String> groupIdentities) {
        this.groupIdentities = groupIdentities;
    }

}
