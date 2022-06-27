package tak.server.federation.hub.policy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;
import tak.server.federation.FederationFilter;
import tak.server.federation.FederationNode;
import tak.server.federation.FederationPolicyGraph;

/**
 * Created on 4/4/2017.
 */
public class FederationPolicyGraphImpl implements FederationPolicyGraph {
    private String name;
    private ConcurrentHashMap<FederateIdentity, FederationNode> nodeMap;
//    private ConcurrentHashMap<FederateIdentity, FederateGroup> groupMap;
    private Set<FederateEdge> edgeSet;
    private Set<String> filterObjectSet;
    private Map<String, Object> additionalData;

    private static final Logger logger = LoggerFactory.getLogger(FederationPolicyGraphImpl.class);

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isReachable(String s, String s1) throws FederationException {
        if (Strings.isNullOrEmpty(s) || Strings.isNullOrEmpty(s1)) {
            throw new FederationException("Given source or destination uid was null or empty");
        }
        if (this.getNode(s) == null || this.getNode(s1) == null) {
            throw new FederationException("One of the given nodes was not in the policy graph.");
        }
        return isReachable(this.getNode(s), this.getNode(s1));
    }

    @Override
    public Set<Federate> allReachableFederates(FederationNode source) throws FederationException {
        Set<FederationNode> unvisitedNodes = new HashSet<>();
        unvisitedNodes.addAll(nodeMap.values());
        return findReachableFederates(source, unvisitedNodes);
    }

    @Override
    public Set<Federate> allReachableFederates(String sourceUid) throws FederationException {
        FederationNode FederationNode = getNode(sourceUid);
        if (FederationNode == null) {
            throw new FederationException("The passed sourceID " + sourceUid + " was not found in the policy graph.");
        }
        return allReachableFederates(FederationNode);
    }

    @Override
    public List<FederationFilter> getFiltersAlongPath(String s, String s1) {
        return null;
    }

    public FederationPolicyGraphImpl(String name, Set<String> filterObjectSet) {

        logger.trace("Creating federation graph object: " + name + " " + filterObjectSet);

        this.name = name;
        this.filterObjectSet = filterObjectSet;

        nodeMap = new ConcurrentHashMap<>();
        edgeSet = ConcurrentHashMap.newKeySet();
        additionalData = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isEmpty() {
        return nodeMap.isEmpty();
    }

    @Override
    public long numberOfNodes() {
        return nodeMap.size();
    }

    @Override
    public long numberOfEdges() {
        return edgeSet.size();
    }

    @Override
    public boolean isReachable(FederationNode source, FederationNode destination) {
        return !getEdgePath(source, destination).isEmpty();
    }

    @Override
    public void addNode(FederationNode federationNode) {
        if (federationNode instanceof Federate) {
            addFederate((Federate) federationNode);
        } else {
            addGroup((FederateGroup) federationNode);
        }
    }

    @Override
    public void addFederate(Federate federate) {
        for (FederateIdentity identity : federate.getGroupIdentities()) {
            FederationNode node = nodeMap.get(identity);
            if (node instanceof FederateGroup) {
                ((FederateGroup) node).addFederateToGroup(federate);
            }
        }
        nodeMap.put(federate.getFederateIdentity(), federate);
    }

    @Override
    public Federate getFederate(String s) {
        FederationNode node = nodeMap.get(new FederateIdentity(s));
        if (node instanceof Federate) {
            return (Federate) node;
        }
        return null;
    }

    @Override
    public Federate getFederate(FederateIdentity federateIdentity) {
        FederationNode node = nodeMap.get(federateIdentity);
        if (node instanceof Federate) {
            return (Federate) node;
        }
        return null;
    }

    @Override
    public FederationNode getNode(String key) {
        return nodeMap.get(new FederateIdentity(key));
    }

    @Override
    public FederateGroup getGroup(String s) {
        FederationNode node = nodeMap.get(new FederateIdentity(s));
        if (node instanceof FederateGroup) {
            return (FederateGroup) node;
        }
        return null;
    }

    @Override
    public FederateGroup getGroup(FederateIdentity federateIdentity) {
        FederationNode node = nodeMap.get(federateIdentity);
        if (node instanceof FederateGroup) {
            return (FederateGroup) node;
        }
        return null;
    }

    @Override
    public FederateEdge getEdge(FederationNode source, FederationNode destination) {
        FederateEdge federateEdge =  source.getOutgoingEdges().stream().filter(edge ->
                edge.getDestinationIdentity().equals(destination.getFederateIdentity())).findFirst().orElse(null);


        // Check if there is an edge from a group the source belongs to
        if (federateEdge == null && source instanceof Federate) {
            for (FederateIdentity groupIdentity : ((Federate) source).getGroupIdentities()) {
                federateEdge = getEdge(this.getGroup(groupIdentity), destination);
                if (federateEdge != null) {
                    return federateEdge;
                }
            }
        }

        // Check if there is an edge to a group the destination belongs to
        if (federateEdge == null  && destination instanceof Federate) {
            for (FederateIdentity groupIdentity : ((Federate) destination).getGroupIdentities()) {
                federateEdge = getEdge(source, this.getGroup(groupIdentity));
                if (federateEdge != null) {
                    return federateEdge;
                }

                // Check if the source and destination are in an interconnected group
                if (source instanceof Federate) {
                    if (((Federate) source).getGroupIdentities().contains(groupIdentity)) {
                        FederateGroup group = this.getGroup(groupIdentity);
                        if (group.isInterconnected()) {
                            federateEdge = new FederateEdge(source.getFederateIdentity(), destination.getFederateIdentity(), group.getFilterExpression());
                            return federateEdge;
                        }
                    }
                }
            }
        }

        return federateEdge;
    }

    @Override
    public void addEdge(FederateEdge federateEdge) throws FederationException {
        if (!nodeMap.containsKey(federateEdge.getDestinationIdentity())) {
            throw new FederationException("The destination of this federate edge is not contained in the policy graph:"
                    + federateEdge);
        } else if (!nodeMap.containsKey(federateEdge.getSourceIdentity())) {
            throw new FederationException("The source of this federate edge is not contained in the policy graph:"
                    + federateEdge);
        } else if (isEdgeInGraph(federateEdge)) {
            throw new FederationException("A edge with this source and destination already exists in the graph.");
        }

        edgeSet.add(federateEdge);
        nodeMap.get(federateEdge.getSourceIdentity()).addOutgoingEdge(federateEdge);
        nodeMap.get(federateEdge.getDestinationIdentity()).addIncomingEdge(federateEdge);
    }

    @Override
    public List<FederationFilter> getFiltersAlongPath(FederationNode FederationNode, FederationNode FederationNode1) {
        return null;
    }

    @Override
    public long numberOfFederates() {
        return nodeMap.values().stream().filter(element -> element instanceof Federate).count();
    }

    @Override
    public void addGroup(FederateGroup federateGroup) {
        for (FederationNode node : nodeMap.values()) {
            if (node instanceof Federate &&
                    isFederateInGroup((Federate) node, federateGroup)) {
                federateGroup.addFederateToGroup((Federate) node);
            }
        }
        nodeMap.put(federateGroup.getFederateIdentity(), federateGroup);
    }

    private boolean isFederateInGroup(Federate federate, FederateGroup group) {
        return federate.getGroupIdentities().contains(group.getFederateIdentity());
    }

    private List<FederateEdge> getEdgePath(FederationNode source, FederationNode destination) {
        List<FederateEdge> edgePath = new LinkedList<>();
        Set<FederateEdge> edgesFromNode = source.getOutgoingEdges();
        Set<FederationNode> unvisitedNodes = new HashSet<>();
        unvisitedNodes.addAll(nodeMap.values());
        unvisitedNodes.remove(source);

        for (FederateEdge edge : edgesFromNode) {
            if (edge.getDestinationIdentity().equals(destination.getFederateIdentity())) {
                edgePath.add(edge);
                return edgePath;
            }

            edgePath = getEdgePath(nodeMap.get(edge.getDestinationIdentity()), destination,
                    unvisitedNodes);

            if (!edgePath.isEmpty()) {
                Collections.reverse(edgePath);
                return edgePath;
            }
        }

        return edgePath;
    }

    private List<FederateEdge> getEdgePath(FederationNode source, FederationNode destination,
                                           Set<FederationNode> unvisitedNodes) {
        List<FederateEdge> edgePath = new LinkedList<>();
        Set<FederateEdge> edgesFromNode = source.getOutgoingEdges();
        unvisitedNodes.remove(source);

        for (FederateEdge edge: edgesFromNode) {
            if (edge.getDestinationIdentity().equals(destination.getFederateIdentity())) {
                edgePath.add(edge);
                return edgePath;
            }

            if (unvisitedNodes.contains(nodeMap.get(edge.getDestinationIdentity()))) {
                edgePath = getEdgePath(nodeMap.get(edge.getDestinationIdentity()), destination, unvisitedNodes);
            }

            if (!edgePath.isEmpty()) {
                edgePath.add(edge);
                return edgePath;
            }
        }

        return edgePath;
    }

    private Set<Federate> findReachableFederates(FederationNode source, Set<FederationNode> unvisitedNodes) throws FederationException {
        Set<Federate> reachableFederates = new HashSet<>();
        Set<FederateEdge> edgesFromNode = source.getOutgoingEdges();
        unvisitedNodes.remove(source);

        for (FederateEdge edge : edgesFromNode) {
            FederationNode destinationNode = nodeMap.get(edge.getDestinationIdentity());
            if (destinationNode instanceof FederateGroup) {
                reachableFederates.addAll(((FederateGroup) destinationNode).getFederatesInGroup());
            } else {
                reachableFederates.add((Federate) destinationNode);
            }
        }

        // Get all connections from any groups the Federate belongs to
        if (source instanceof Federate) {
            Federate sourceFederate = (Federate) source;
            for (FederateIdentity groupIdentity : sourceFederate.getGroupIdentities()) {
                FederateGroup group = (FederateGroup) nodeMap.get(groupIdentity);
                if (group.isInterconnected()) {
                    reachableFederates.addAll(group.getFederatesInGroup());
                }
                reachableFederates.addAll(allReachableFederates(group));
            }
            // Remove source node in case it was added by a group add operation
            reachableFederates.remove(sourceFederate);
        }

        return reachableFederates;
    }

//    private boolean isNodeInGraph(FederationNode FederationNode) {
//        return (nodeMap.containsKey(FederationNode.getFederateIdentity()));
//    }

    private boolean isEdgeInGraph(FederateEdge federateEdge) {
        FederationNode source = nodeMap.get(federateEdge.getSourceIdentity());
        for (FederateEdge edge : source.getOutgoingEdges()) {
            if (edge.getDestinationIdentity().equals(federateEdge.getDestinationIdentity())) {
                return true;
            }
        }
        return false;
    }

    public Collection<FederationNode> getNodes() {
        return nodeMap.values();
    }

    public Set<FederateEdge> getEdgeSet() {
        return edgeSet;
    }

    @Override
    public Set<String> getFilterObjectSet() {
        return filterObjectSet;
    }

    @Override
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    @Override
    public void setAddtionalData(Map<String, Object> map) {
        this.additionalData = map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FederationPolicyGraphImpl that = (FederationPolicyGraphImpl) o;

        if (!nodeMap.equals(that.nodeMap)) return false;
        return edgeSet.equals(that.edgeSet);
    }

    @Override
    public int hashCode() {
        int result = 31 * nodeMap.hashCode();
        result = 31 * result + edgeSet.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FederationPolicyGraphImpl [name=");
        builder.append(name);
        builder.append(", nodeMap=");
        builder.append(nodeMap);
        builder.append(", edgeSet=");
        builder.append(edgeSet);
        builder.append("]");
        return builder.toString();
    }
}
