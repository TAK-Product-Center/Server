package tak.server.federation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Interface for a FederationPolicyGraph object to represent a federation
 * policy as defined by the federation policy schema.
 *
 * An implementing class should support all methods as described. An
 * implementing class should be naturally serializable, at least in terms
 * of the objects defined in the policy schema.
 *
 * Any method throwing a FederationException should have some validation
 * involved in the implementation. Any method that does not does not require
 * validation in the implementation, but it can be allowed based on your
 * use case. When to throw a FederationException is detailed in the comments.
 */
public interface FederationPolicyGraph {

    /*
     * The human readable name of the federation policy object.
     * This has no usage in policy decisions.
     */
    public String getName();

    /*
     * True if the graph object has no nodes, and false if it has any nodes.
     */
    public boolean isEmpty();

    /*
     * The number of nodes in the graph
     */
    public long numberOfNodes();

    /*
     * Number of specific, individual federates in the group
     */
    long numberOfFederates();

    /*
     * The number of edges in the graph
     */
    public long numberOfEdges();

    /* Operations on nodes. */

    /*
     * Add a node to the policy graph.
     */
    public void addNode(FederationNode node);

    /*
     * Add a federate to the policy graph.
     */
    void addFederate(Federate federate);

    /*
     * Get the federate corresponding to the passed string ID.
     */
    Federate getFederate(String federateId);

    /*
     * Get the federate corresponding to the passed FederateIdentity.
     */
    Federate getFederate(FederateIdentity federateId);

    /*
     * Add a group to the policy graph.
     */
    void addGroup(FederateGroup group);

    /*
     * Get a group from the policy graph from a given string id.
     */
    FederateGroup getGroup(String groupId);

    /*
     * Get a group from the policy graph from a federate identity.
     */
    FederateGroup getGroup (FederateIdentity groupIdentity);

    /*
     * Get he FederationNode corresponding to the nodeUid.
     */
    public FederationNode getNode(String nodeUid);

    /*
     * Checks if there is a valid path to the destination node from
     * the source node. Throws FederationException if either the
     * source or destination node is not contained in the graph.
     */
    public boolean isReachable(FederationNode source,
        FederationNode destination) throws FederationException;

    /*
     * Checks if there is a valid path to the destination node from
     * the source node. Throws FederationException if either the
     * source or destination node is not contained in the graph.
     */
    public boolean isReachable(String sourceUid,
        String destinationUid) throws FederationException;

    /*
     * Returns the set of all nodes that the given source node
     * has a valid path to. Throws FederationException if the
     * given source node does not exist.
     */
    public Set<Federate> allReachableFederates(FederationNode source)
        throws FederationException;

    /*
     * Returns the set of all nodes that the given source node
     * indicated by the sourceUid has a valid path to. Throws
     * FederationException if the given source node does not exist.
     */
    public Set<Federate> allReachableFederates(String sourceUid)
        throws FederationException;

    /* Operations on edges. */

    /*
     * Add the given FederateEdge to the graph. Throws FederationException
     * if either the source or destination node of the edge does not exist
     * in the graph.
     */
    public void addEdge(FederateEdge edge) throws FederationException;

    /*
     * Get the edge between the given source and destination nodes.
     */
    public FederateEdge getEdge(FederationNode source, FederationNode destination);

    /*
     * Returns a list of filters along the path between the
     * source and destination node.
     */
    public List<FederationFilter> getFiltersAlongPath(FederationNode source,
        FederationNode destination);

    /*
     * Returns a list of filters along the path between the
     * source and destination node.
     */
    public List<FederationFilter> getFiltersAlongPath(String sourceUid,
        String destinationUid);

    /*
     * Get the set of all edges in the graph. Required for serialization frameworks.
     */
    public Set<FederateEdge> getEdgeSet();

    public Collection<FederationNode> getNodes();

    /*
     * Get the configured set of fully-qualified names of
     * singleton objects containing lambda filter implementations.
     */
    public Set<String> getFilterObjectSet();

    public Map<String, Object> getAdditionalData();

    public void setAddtionalData(Map<String, Object> additionalData);
}
