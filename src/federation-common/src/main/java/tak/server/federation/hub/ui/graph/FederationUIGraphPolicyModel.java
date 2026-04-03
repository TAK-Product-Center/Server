package tak.server.federation.hub.ui.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class FederationUIGraphPolicyModel {
	private Collection<PolicyObjectCell> nodes;
	
	private Map<String, Object> additionalProperties = new HashMap<>();

    public Collection<PolicyObjectCell> getNodes() {
		return nodes;
	}

	public void setNodes(Collection<PolicyObjectCell> nodes) {
		this.nodes = nodes;
	}

	@JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String key, Object value) {
        this.additionalProperties.put(key, value);
    }
	
}
