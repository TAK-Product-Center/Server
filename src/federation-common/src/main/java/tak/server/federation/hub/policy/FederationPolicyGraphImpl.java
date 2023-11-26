package tak.server.federation.hub.policy;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederateOutgoing;
import tak.server.federation.FederationException;
import tak.server.federation.FederationFilter;
import tak.server.federation.FederationNode;
import tak.server.federation.FederationPolicyGraph;

/**
 * Created on 4/4/2017.
 */
public class FederationPolicyGraphImpl implements FederationPolicyGraph, Serializable {
	private static final long serialVersionUID = -7327365173619071592L;
	private String name;
    private ConcurrentHashMap<FederateIdentity, FederationNode> nodeMap;
//    private ConcurrentHashMap<FederateIdentity, FederateGroup> groupMap;
    private Set<FederateEdge> edgeSet;
    private Set<String> filterObjectSet;
    private Map<String, Object> additionalData;

    private static final Logger logger = LoggerFactory.getLogger(FederationPolicyGraphImpl.class);
    
    public final class FederationPolicyReachabilityHolder {
    	public Set<Federate> federates = new HashSet<>();
    	
    	public Set<FederateEdge> edges = new HashSet<>();

    	private Map<FederationNode, FederateEdge> destinationToFederateEdge = new HashMap<>();
    	public Map<FederationNode, FederateEdge> getDestinationToFederateEdgeMappings() {
    		return destinationToFederateEdge;
    	}
    
    	private Map<FederationNode, Set<FederateEdge>> destinationToFederateGroupEdges = new HashMap<>();
    	public Map<FederationNode, Set<FederateEdge>> getDestinationToFederateGroupEdgeMappings() {
    		return destinationToFederateGroupEdges;
    	}
    	
    	public void addDestinationEdgeMapping(FederationNode dest, FederateEdge edge) {
    		FederationNode edgeSource = getNode(edge.getSourceIdentity().getFedId());
    		if (edgeSource instanceof FederateOutgoing || edgeSource instanceof Federate) {
    			destinationToFederateEdge.put(dest, edge);
    		}
    		
    		if (edgeSource instanceof FederateGroup) {
    			if (!destinationToFederateGroupEdges.containsKey(dest)) {
        			destinationToFederateGroupEdges.put(dest, new HashSet<>());
        		}
        		destinationToFederateGroupEdges.get(dest).add(edge);
    		}
    	}
    	
    	public void addDestinationEdgeMapping(FederationNode dest, Set<FederateEdge> edges) {
    		edges.forEach(edge -> addDestinationEdgeMapping(dest, edge));
    	}
    }

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
    public FederationPolicyReachabilityHolder allReachableFederates(FederationNode source) throws FederationException {
        Set<FederationNode> unvisitedNodes = new HashSet<>();
        unvisitedNodes.addAll(nodeMap.values());
        return findReachableFederates(source, unvisitedNodes);
    }

    @Override
    public FederationPolicyReachabilityHolder allReachableFederates(String sourceUid) throws FederationException {
        FederationNode federationNode = getNode(sourceUid);
        if (federationNode == null) {
            throw new FederationException("The passed sourceID " + sourceUid + " was not found in the policy graph.");
        }
        return allReachableFederates(federationNode);
    }
    
    @Override
    public Set<Federate> allReceivableFederates(FederationNode source) throws FederationException {
        Set<FederationNode> unvisitedNodes = new HashSet<>();
        unvisitedNodes.addAll(nodeMap.values());
        return findReceivableFederates(source, unvisitedNodes);
    }

	@Override
    public Set<Federate> allReceivableFederates(String sourceUid) throws FederationException {
        FederationNode federationNode = getNode(sourceUid);
        if (federationNode == null) {
            throw new FederationException("The passed sourceID " + sourceUid + " was not found in the policy graph.");
        }
        return allReceivableFederates(federationNode);
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
        } else if (federationNode instanceof FederateOutgoing) {
        	nodeMap.put(federationNode.getFederateIdentity(), federationNode);
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
                        	// interconnected connections will be forced to share all groups
                            federateEdge = new FederateEdge(source.getFederateIdentity(), destination.getFederateIdentity(), group.getFilterExpression(), 
                            		new HashSet<String>(), new HashSet<String>(), FederateEdge.GroupFilterType.ALL);
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
            logger.warn("An edge with this source and destination already exists in the graph: " + federateEdge);
            return;
        }

        edgeSet.add(federateEdge);
        nodeMap.get(federateEdge.getSourceIdentity()).addOutgoingEdge(federateEdge);
        nodeMap.get(federateEdge.getDestinationIdentity()).addIncomingEdge(federateEdge);
    }

    @Override
    public List<FederationFilter> getFiltersAlongPath(FederationNode federationNode, FederationNode federationNode1) {
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
   
    private FederationPolicyReachabilityHolder findReachableFederates(FederationNode source, Set<FederationNode> unvisitedNodes) throws FederationException {
    	FederationPolicyReachabilityHolder reachability = new FederationPolicyReachabilityHolder();
        unvisitedNodes.remove(source);
                
        for (FederateEdge edge : source.getOutgoingEdges()) {
            FederationNode destinationNode = nodeMap.get(edge.getDestinationIdentity());
            if (destinationNode instanceof FederateGroup) {
            	Set<Federate> groupDestinations = ((FederateGroup) destinationNode).getFederatesInGroup();
            	reachability.federates.addAll(groupDestinations);
            	groupDestinations.forEach(dest -> reachability.addDestinationEdgeMapping(dest, edge));
            } else {
            	reachability.federates.add((Federate) destinationNode);
            	reachability.addDestinationEdgeMapping(destinationNode, edge);
            }
        }

        // Get all connections from any groups the Federate belongs to
        if (source instanceof Federate) {
            Federate sourceFederate = (Federate) source;
            for (FederateIdentity groupIdentity : sourceFederate.getGroupIdentities()) {
                FederateGroup group = (FederateGroup) nodeMap.get(groupIdentity);
                if (group.isInterconnected()) {
                	reachability.federates.addAll(group.getFederatesInGroup());
                }
                FederationPolicyReachabilityHolder groupsReachability = allReachableFederates(group);
                reachability.federates.addAll(groupsReachability.federates);
                groupsReachability.getDestinationToFederateGroupEdgeMappings().entrySet().forEach(entry -> {
                	reachability.addDestinationEdgeMapping(entry.getKey(), entry.getValue());
                });
            }
            // Remove source node in case it was added by a group add operation
            reachability.federates.remove(sourceFederate);
        }
        return reachability;
    }
    
    private Set<Federate> findReceivableFederates(FederationNode source, Set<FederationNode> unvisitedNodes) throws FederationException {
        Set<Federate> receivableFederates = new HashSet<>();
        unvisitedNodes.remove(source);
                
        for (FederateEdge edge : source.getIncomingEdges()) {
            FederationNode receivingNode = nodeMap.get(edge.getSourceIdentity());
            if (receivingNode instanceof FederateGroup) {
                receivableFederates.addAll(((FederateGroup) receivingNode).getFederatesInGroup());
            } else {
                receivableFederates.add((Federate) receivingNode);
            }
        }

        // Get all connections from any groups the Federate belongs to
        if (source instanceof Federate) {
            Federate sourceFederate = (Federate) source;
            for (FederateIdentity groupIdentity : sourceFederate.getGroupIdentities()) {
                FederateGroup group = (FederateGroup) nodeMap.get(groupIdentity);
                if (group.isInterconnected()) {
                    receivableFederates.addAll(group.getFederatesInGroup());
                }
                receivableFederates.addAll(allReceivableFederates(group));
            }
            // Remove source node in case it was added by a group add operation
            receivableFederates.remove(sourceFederate);
        }
        return receivableFederates;
    }
    

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
