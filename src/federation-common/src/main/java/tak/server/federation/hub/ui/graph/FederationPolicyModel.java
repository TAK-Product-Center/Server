package tak.server.federation.hub.ui.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;
import tak.server.federation.FederationNode;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.policy.FederationPolicy;
import tak.server.federation.hub.policy.FederationPolicyGraphImpl;
import tak.server.federation.hub.ui.GroupHolder;
import tak.server.federation.hub.ui.StringEdge;
import tak.server.federation.hub.ui.UidHolder;

/**
 * Object used to hold front end UI's representation of a federation policy graph.
 */
public class FederationPolicyModel {
    private String name;
    private String version;
    private String type;
    private String description;
    private String thumbnail;
    Collection<PolicyObjectCell> cells;

    // TODO holdover AMT field until we fully convert frontend
    private String diagramType;

    static String UI_DATA = "uiData";

    private static final Logger LOGGER = LoggerFactory.getLogger(FederationPolicyModel.class);

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<PolicyObjectCell> getCells() {
        return cells;
    }

    public void setCells(Collection<PolicyObjectCell> cells) {
        this.cells = cells;
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

    @JsonIgnore
    public FederationPolicyGraph getPolicyGraphFromModel() {
        FederationPolicyGraph policyGraph = new FederationPolicyGraphImpl(this.name, new HashSet<>());

        cells.stream().filter(cell -> cell instanceof GroupCell).forEach(cell -> {
            GroupCell groupCell = (GroupCell) cell;
            FederateIdentity identity = new FederateIdentity(groupCell.getProperties().getName());
            FederateGroup group = new FederateGroup(identity);
            group.getAttributes().putAll(groupCell.getProperties().attributesToMap());
            group.setInterconnected(groupCell.getProperties().isInterconnected());
            group.setFilterExpression(groupCell.getProperties().getFilterExpression());
            policyGraph.addGroup(group);
        });

        cells.stream().filter(cell -> cell instanceof FederateCell).forEach( cell -> {
            FederateCell federateCell = (FederateCell) cell;
            FederateIdentity identity = new FederateIdentity(federateCell.getProperties().getName());
            Federate federateNode = new Federate(identity);
            federateNode.getAttributes().putAll(federateCell.getProperties().attributesToMap());
            federateNode.getGroupIdentities().addAll(stringsToFederateIdentity(federateCell
                                                    .getProperties().getGroupIdentities()));
            policyGraph.addNode(federateNode);
        });
        
        cells.stream().filter(cell -> cell instanceof FederationOutgoingCell).forEach( cell -> {
        	FederationOutgoingCell federateCell = (FederationOutgoingCell) cell;
            FederateIdentity identity = new FederateIdentity(federateCell.getProperties().getName());
            Federate federateOutgoing = new Federate(identity);
            federateOutgoing.getAttributes().putAll(federateCell.getProperties().attributesToMap());
            policyGraph.addNode(federateOutgoing);
        });

        cells.stream().filter(cell -> cell instanceof EdgeCell).forEach(cell -> {
            EdgeCell edgeCell = (EdgeCell) cell;
            FederationNode sourceNode = policyGraph.getNode(getCellNameFromId(edgeCell.getSourceId()));
            FederationNode destinationNode = policyGraph.getNode(getCellNameFromId(edgeCell.getDestinationId()));
            try {
                policyGraph.addEdge(new FederateEdge(sourceNode.getFederateIdentity(), destinationNode.getFederateIdentity(), edgeCell.getProperties().getFilterExpression(),
                		new HashSet<String>(edgeCell.getProperties().getAllowedGroups()), new HashSet<String>(edgeCell.getProperties().getDisallowedGroups()), 
                		FederateEdge.getGroupFilterType(edgeCell.getProperties().getGroupsFilterType())));
               
                for (FilterNode node : edgeCell.getProperties().getFilters()) {
                    policyGraph.getFilterObjectSet().add(node.getFilter().getFilterObject());
                }
            } catch (FederationException e) {
                // UI should provide this check for us, so just log error
                LOGGER.error("There was an error converting the graph model to a policy object");
            }
        });

        policyGraph.getAdditionalData().put(UI_DATA, this);

        return policyGraph;
    }

    @JsonIgnore
    public FederationPolicy getFederationPolicyObjectFromModel() {
        FederationPolicy policy = getFederationPolicyObjectFromModelWithoutGraphData();
        policy.addAdditionalData(UI_DATA, this);
        return policy;
    }

    @JsonIgnore
    public FederationPolicy getFederationPolicyObjectFromModelWithoutGraphData() {
        FederationPolicy policy = new FederationPolicy();
        policy.setName(this.name);
        policy.setType(this.type);
        policy.setVersion(this.version);

        if (cells != null && !cells.isEmpty()) {

            cells.stream().filter(cell -> cell instanceof FederateCell).forEach(cell -> {
                FederateCell federateCell = (FederateCell) cell;
                UidHolder federate =  new UidHolder(federateCell.getProperties().getName());
                federate.setAttributes(federateCell.getProperties().attributesToMap());
                federate.setGroups(federateCell.getProperties().getGroupIdentities());
                policy.getFederate_nodes().add(federate);
            });
            
            cells.stream().filter(cell -> cell instanceof FederationOutgoingCell).forEach(cell -> {
            	FederationOutgoingCell outgoing = (FederationOutgoingCell) cell;
                UidHolder federate =  new UidHolder(outgoing.getProperties().getName());
                policy.getFederate_nodes().add(federate);
            });

            cells.stream().filter(cell -> cell instanceof EdgeCell).forEach(cell -> {
                EdgeCell edgeCell = (EdgeCell) cell;
                StringEdge stringEdge = new StringEdge(getCellNameFromId(edgeCell.getSourceId()), getCellNameFromId(edgeCell.getDestinationId()));
                stringEdge.setFilterExpression(edgeCell.getProperties().getFilterExpression());
                stringEdge.setAllowedGroups(new HashSet<>(edgeCell.getProperties().getAllowedGroups()));
                stringEdge.setDisallowedGroups(new HashSet<>(edgeCell.getProperties().getDisallowedGroups()));
                stringEdge.setGroupsFilterType(FederateEdge.getGroupFilterType(edgeCell.getProperties().getGroupsFilterType()));
                policy.getFederate_edges().add(stringEdge);
                for (FilterNode node : edgeCell.getProperties().getFilters()) {
                    policy.getFilter_objects().add(node.getFilter().getFilterObject());
                }

            });

            cells.stream().filter(cell -> cell instanceof GroupCell).forEach(cell -> {
                GroupCell groupCell = (GroupCell) cell;
                GroupHolder group = new GroupHolder(groupCell.getProperties().getName());
                group.setAttributes(groupCell.getProperties().attributesToMap());
                group.setInterconnected(groupCell.getProperties().isInterconnected());
                group.setFilterExpression(groupCell.getProperties().getFilterExpression());
                policy.getGroups().add(group);
            });
        }

        return policy;
    }

    private String getCellNameFromId(String id) {
        for (PolicyObjectCell cell : cells) {
            if (cell.getId().equals(id)) {
                if (cell instanceof FederateCell) {
                    return ((FederateCell) cell).getProperties().getName();
                } else if (cell instanceof GroupCell) {
                    return ((GroupCell) cell).getProperties().getName();
                }
                else if (cell instanceof FederationOutgoingCell) {
                    return ((FederationOutgoingCell) cell).getProperties().getName();
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

    @JsonIgnore
    public static FederationPolicyModel policyGraphToModel(FederationPolicyGraph policyGraph) {
        FederationPolicyModel policyModel = new FederationPolicyModel();
        policyModel.setName(policyGraph.getName());
        policyModel.setType("Federation");
        policyModel.setVersion("1.0.0"); // TODO: Maintain version in graph object
        policyModel.setDescription("Actively used federation by ROGER");
        Set<PolicyObjectCell> cellSet = new HashSet<>();
        for (FederationNode node : policyGraph.getNodes()) {
            cellSet.add(policyNodeToCell(node));
        }
        for (FederateEdge edge : policyGraph.getEdgeSet()) {
            cellSet.add(policyEdgeToCell(edge));
        }
        policyModel.setCells(cellSet);
        return policyModel;
    }

    private static PolicyObjectCell policyNodeToCell(FederationNode node) {
        if (node instanceof Federate) {
            return policyFederateToCell((Federate) node);
        } else if (node instanceof FederateGroup) {
            return policyGroupToCell((FederateGroup) node);
        }
        return null;
    }

    private static FederateCell policyFederateToCell(Federate federate) {
        FederateCell federateCell = new FederateCell();
        FederateProperties properties = new FederateProperties();
        properties.setName(federate.getFederateIdentity().getFedId());
        properties.setId(federate.getName());
        for (FederateIdentity identity : federate.getGroupIdentities()) {
            properties.getGroupIdentities().add(identity.getFedId());
        }
        federateCell.setProperties(properties);
        return federateCell;
    }

    private static GroupCell policyGroupToCell(FederateGroup group) {
        GroupCell groupCell = new GroupCell();
        GroupProperties properties = new GroupProperties();
        properties.setInterconnected(group.isInterconnected());
        properties.setName(group.getFederateIdentity().getFedId());
        properties.setId(group.getName());
        properties.setFilters(filterExpressionToFilterNodes(group.getFilterExpression()));

        return groupCell; 
    }

    private static EdgeCell policyEdgeToCell(FederateEdge edge) {
        EdgeCell edgeCell = new EdgeCell();
        return edgeCell;
    }

    private static List<Object> mapToAttributes(Map<String, Object> attributeMap) {
        List<Object> attributeList = new LinkedList<>();
        return attributeList;
    }

    private static List<FilterNode> filterExpressionToFilterNodes(String filterExpression) {
        return FilterUtils.filterExpressionsToFilterNodes(filterExpression);
    }


    // TODO Holdover getters and setters

    public String getDiagramType() {
        return diagramType;
    }

    public void setDiagramType(String diagramType) {
        this.diagramType = diagramType;
    }

}
