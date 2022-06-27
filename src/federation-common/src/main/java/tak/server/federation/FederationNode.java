package tak.server.federation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Abstract class to represent federate nodes from the federate policy language.
 * Individual Federates and FederateGroups extend this class. This should be kept
 * up to date with changes in the policy schema. This object should be maintained
 * such that it is naturally serializable.
 *
 * This object has:
 *     Name - human readable designation for the node. This is NOT required, nor
 *       used in policy decisions
 *     Federate identity - identity of the node within the federation. Required.
 *     Attributes - a map of attribute objects that can be assigned to the node.
 *       These may be used in making policy decisions. Not required.
 *     Incoming edges - a set of directed edges that point to this node.
 *     Outgoing edges - a set of directed edges that point from this node.
 */

/* Suppress IfStmtsMustUseBraces warning to get auto-generated equals hashcode methods to pass PMD. */
@SuppressWarnings({"PMD.IfStmtsMustUseBraces", "PMD.AbstractNaming", "PMD.AbstractClassWithoutAbstractMethod"})
public abstract class FederationNode {
    private String name;
    private final FederateIdentity federateIdentity;
    private final ConcurrentHashMap<String, Object> attributes;
    private final Set<FederateEdge> incomingEdges;
    private final Set<FederateEdge> outgoingEdges;

    public FederationNode(FederateIdentity federateIdentity) {
        this.federateIdentity = federateIdentity;
        attributes = new ConcurrentHashMap<>();
        incomingEdges = new HashSet<>();
        outgoingEdges = new HashSet<>();
    }

    public FederationNode(String nodeName, FederateIdentity federateIdentity) {
        this.name = nodeName;
        this.federateIdentity = federateIdentity;
        attributes = new ConcurrentHashMap<>();
        incomingEdges = new HashSet<>();
        outgoingEdges = new HashSet<>();
    }

    public FederateIdentity getFederateIdentity() {
        return this.federateIdentity;
    }

    public String getName() {
        return this.name;
    }

    public void addAttribute(String key, Object value) {
        if (isValueValidType(value)) {
            attributes.put(key, value);
        } else {
            throw new IllegalArgumentException("The passed value was not a valid type.");
        }
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void addIncomingEdge(FederateEdge edge) {
        incomingEdges.add(edge);
    }

    public void addOutgoingEdge(FederateEdge edge) {
        outgoingEdges.add(edge);
    }

    public Set<FederateEdge> getIncomingEdges() {
        return incomingEdges;
    }

    public Set<FederateEdge> getOutgoingEdges() {
        return outgoingEdges;
    }

    public void setName(String name) {
        this.name = name;
    }

    private boolean isValueValidType(Object value) {
        return (value instanceof String) ||
            (value instanceof Integer) ||
            (value instanceof Boolean) ||
            (value instanceof List);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;

        FederationNode that = (FederationNode) other;

        if (federateIdentity != null
                ? !federateIdentity.equals(that.federateIdentity)
                : that.federateIdentity != null)
            return false;

        return attributes != null
            ? attributes.equals(that.attributes)
            : that.attributes == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + federateIdentity.hashCode();
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FederationNode{" +
            "name='" + name + '\'' +
            ", uid='" + federateIdentity.toString() + '\'' +
            ", attributes=" + attributes +
            '}';
    }
}
