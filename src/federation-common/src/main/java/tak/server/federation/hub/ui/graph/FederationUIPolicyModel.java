package tak.server.federation.hub.ui.graph;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationNode;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.policy.FederationPolicyGraphImpl;

/**
 * Object used to hold front end UI's representation of a federation policy
 * graph.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FederationUIPolicyModel {
	private static final Logger logger = LoggerFactory.getLogger(FederationUIPolicyModel.class);

	private String name;
	private String version;
	private String type;
	private String description;
	private String thumbnail;

	private FederationUISettingsPolicyModel settings;
	private FederationUIPluginsPolicyModel pluginsData;
	private FederationUIGraphPolicyModel graphData;
	
	// For backward compatibility: maps "additionalData.uiData.cells" to this setter
    @JsonProperty("additionalData")
    private void unpackAdditionalData(JsonNode additionalData) {
    	graphData = new  FederationUIGraphPolicyModel();
        if (additionalData != null && additionalData.has("uiData")) {
            JsonNode uiData = additionalData.get("uiData");
            if (uiData.has("cells")) {
                ObjectMapper mapper = new ObjectMapper();
				try {
					this.graphData.setNodes(mapper.readerForListOf(PolicyObjectCell.class).readValue(uiData.get("cells")));
				} catch (IOException e) {
					logger.warn("Failed to parse cells from old format", e);
				}
            }
            if (uiData.has("settings")) {
                ObjectMapper mapper = new ObjectMapper();
				try {
					Map<String, Object> settings = mapper.readerForMapOf(Object.class).readValue(uiData.get("settings"));
					this.graphData.setAdditionalProperties("settings", settings);
				} catch (IOException e) {
					logger.warn("Failed to parse cells from old format", e);
				}
            }
        }
    }

	public FederationUIGraphPolicyModel getGraphData() {
		return graphData;
	}

	public void setGraphData(FederationUIGraphPolicyModel graph) {
		this.graphData = graph;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public FederationUISettingsPolicyModel getSettings() {
		return settings;
	}

	public void setSettings(FederationUISettingsPolicyModel settings) {
		this.settings = settings;
	}

	public FederationUIPluginsPolicyModel getPluginsData() {
		return pluginsData;
	}

	public void setPluginsData(FederationUIPluginsPolicyModel pluginsData) {
		this.pluginsData = pluginsData;
	}

	@Override
	public String toString() {
		return "FederationUIPolicyModel [name=" + name + ", version=" + version + ", type=" + type + ", description="
				+ description + ", thumbnail=" + thumbnail + ", settings=" + settings + ", plugins=" + pluginsData + "]";
	}

	@JsonIgnore
	public FederationPolicyGraph getPolicyGraphFromModel() {
		FederationPolicyGraph policyGraph = new FederationPolicyGraphImpl(this);
		
		if (graphData != null && graphData.getNodes() != null) {
			graphData.getNodes().stream().filter(cell -> cell instanceof GroupCell).forEach(cell -> {
				GroupCell groupCell = (GroupCell) cell;
				FederateIdentity identity = new FederateIdentity(groupCell.getProperties().getName());
				FederateGroup group = new FederateGroup(identity);
				group.setInterconnected(groupCell.getProperties().isInterconnected());
				group.setAllowTokenAuth(groupCell.getProperties().isAllowTokenAuth());
				group.setTokenAuthDuration(groupCell.getProperties().getTokenAuthDuration());
				policyGraph.addGroup(group);
			});

			graphData.getNodes().stream().filter(cell -> cell instanceof FederationTokenGroupCell).forEach(cell -> {
				FederationTokenGroupCell tokenGroupCell = (FederationTokenGroupCell) cell;
				FederateIdentity identity = new FederateIdentity(tokenGroupCell.getProperties().getName());
				FederateGroup group = new FederateGroup(identity);
				group.setInterconnected(tokenGroupCell.getProperties().isInterconnected());
				policyGraph.addGroup(group);
			});

			graphData.getNodes().stream().filter(cell -> cell instanceof FederateCell).forEach(cell -> {
				FederateCell federateCell = (FederateCell) cell;
				FederateIdentity identity = new FederateIdentity(federateCell.getProperties().getName());
				Federate federateNode = new Federate(identity);
				federateNode.getGroupIdentities()
						.addAll(stringsToFederateIdentity(federateCell.getProperties().getGroupIdentities()));
				policyGraph.addNode(federateNode);
			});

			graphData.getNodes().stream().filter(cell -> cell instanceof FederationOutgoingCell).forEach(cell -> {
				FederationOutgoingCell federateCell = (FederationOutgoingCell) cell;
				FederateIdentity identity = new FederateIdentity(federateCell.getProperties().getName());
				Federate federateOutgoing = new Federate(identity);
				policyGraph.addNode(federateOutgoing);
			});

			graphData.getNodes().stream().filter(cell -> cell instanceof EdgeCell).forEach(cell -> {
				EdgeCell edgeCell = (EdgeCell) cell;
				FederationNode sourceNode = policyGraph.getNode(getCellNameFromId(edgeCell.getSourceId()));
				FederationNode destinationNode = policyGraph.getNode(getCellNameFromId(edgeCell.getDestinationId()));
				try {
					policyGraph.addEdge(
							new FederateEdge(sourceNode.getFederateIdentity(), destinationNode.getFederateIdentity(),
									new HashSet<String>(edgeCell.getProperties().getAllowedGroups()),
									new HashSet<String>(edgeCell.getProperties().getDisallowedGroups()),
									FederateEdge.getGroupFilterType(edgeCell.getProperties().getGroupsFilterType())));
				} catch (Exception e) {
					// UI should provide this check for us, so just log error
					logger.error("There was an error converting the graph model to a policy object");
					e.printStackTrace();
				}
			});	
		}

		return policyGraph;
	}

	private String getCellNameFromId(String id) {
		for (PolicyObjectCell cell : graphData.getNodes()) {
			if (cell.getId().equals(id)) {
				if (cell instanceof FederateCell) {
					return ((FederateCell) cell).getProperties().getName();
				} else if (cell instanceof GroupCell) {
					return ((GroupCell) cell).getProperties().getName();
				} else if (cell instanceof FederationOutgoingCell) {
					return ((FederationOutgoingCell) cell).getProperties().getName();
				} else if (cell instanceof FederationTokenGroupCell) {
					return ((FederationTokenGroupCell) cell).getProperties().getName();
				}
				return ((EdgeCell) cell).getProperties().getName();
			}
		}
		return null;
	}

	private Set<FederateIdentity> stringsToFederateIdentity(List<String> nameList) {
		Set<FederateIdentity> identities = new HashSet<>();
		for (String name : nameList) {
			identities.add(new FederateIdentity(name));
		}
		return identities;
	}

}
