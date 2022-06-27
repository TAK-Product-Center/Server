package tak.server.federation;

import java.util.HashSet;
import java.util.Set;

/*
 * Object to represent Federate Edges from the federate policy language. This
 * should be kept up to date with changes in the language. This object should
 * be maintained such that it is naturally serializable.
 *
 * Currently, this object has:
 *     Source - federate identity of the source node. This does not point directly
 *       to the node to avoid a circular dependency.
 *     Destination - federate identity of the destination node. This does not
 *       point directly to the node to avoid a circular dependency.
 *     Filter Set - a set of filter methods that apply to this edge.
 *     Filter Expression - the filter expression of this edge.
 */
public class FederateEdge {
    private final FederateIdentity source;
    private final FederateIdentity destination;
    private final Set<FederationFilter> filterSet;
    private final String filterExpression;

    public FederateEdge(FederateIdentity source, FederateIdentity destination,
            String filterExpression) {
        this.source = source;
        this.destination = destination;
        filterSet = new HashSet<>();
        this.filterExpression = filterExpression;
    }

    public FederateIdentity getSourceIdentity() {
        return source;
    }

    public FederateIdentity getDestinationIdentity() {
        return destination;
    }

    public void addFilter(FederationFilter newFilter) {
        filterSet.add(newFilter);
    }

    public Set<FederationFilter> getFilterSet() {
        return filterSet;
    }

    public String getFilterText() {
        return filterExpression;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) { return true; }
        if (object == null || getClass() != object.getClass()) { return false; }

        FederateEdge that = (FederateEdge) object;

        if (!source.equals(that.source)) { return false; }
        if (!destination.equals(that.destination)) { return false; }
        return filterSet != null ? filterSet.equals(that.filterSet) : that.filterSet == null;
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + filterSet.hashCode();
        return result;
    }

    public String getFilterExpression() {
        return filterExpression;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(67);
        builder.append("FederateEdge [source=")
            .append(source)
            .append(", destination=")
            .append(destination)
            .append(", filterSet=")
            .append(filterSet)
            .append(", filterExpression=")
            .append(filterExpression)
            .append(']');
        return builder.toString();
    }
}
