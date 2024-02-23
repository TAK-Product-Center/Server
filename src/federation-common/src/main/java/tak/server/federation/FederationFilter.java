package tak.server.federation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* All of these warnings come from the equals method. */
@SuppressWarnings({"PMD.IfStmtsMustUseBraces", "PMD.ModifiedCyclomaticComplexity",
    "PMD.StdCyclomaticComplexity", "PMD.NPathComplexity"})
public class FederationFilter {
    private final String filterType;
    private final String methodName;
    private final Map<String, Object> messageAttributes;
    private final Map<String, Object> sourceAttributes;
    private final Map<String, Object> destinationAttributes;

    public FederationFilter(String filterType, String methodName) {
        this.filterType = filterType;
        this.methodName = methodName;
        messageAttributes = new ConcurrentHashMap<>();
        sourceAttributes = new ConcurrentHashMap<>();
        destinationAttributes = new ConcurrentHashMap<>();
    }

    public String getFilterType() {
        return filterType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void addMessageAttribute(String key, Object value) {
        if (isValueValidType(value)) {
            messageAttributes.put(key, value);
        }
    }

    public Map<String, Object> getMessageAttributes() {
        return messageAttributes;
    }

    public void addSourceAttribute(String key, Object value) {
        if (isValueValidType(value)) {
            sourceAttributes.put(key, value);
        }
    }

    public Map<String, Object> getSourceAttributes() {
        return sourceAttributes;
    }

    public void addDestinationAttribute(String key, Object value) {
        if (isValueValidType(value)) {
            destinationAttributes.put(key, value);

        }
    }

    public Map<String, Object> getDestinationAttributes() {
        return destinationAttributes;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;

        FederationFilter that = (FederationFilter) object;

        if (!filterType.equals(that.filterType))
            return false;

        if (!methodName.equals(that.methodName))return false;

        if (messageAttributes != null
                ? !messageAttributes.equals(that.messageAttributes)
                : that.messageAttributes != null)
            return false;

        if (sourceAttributes != null
                ? !sourceAttributes.equals(that.sourceAttributes)
                : that.sourceAttributes != null)
            return false;

        return destinationAttributes != null
            ? destinationAttributes.equals(that.destinationAttributes)
            : that.destinationAttributes == null;
    }

    @Override
    public int hashCode() {
        int result = filterType.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + (messageAttributes != null ? messageAttributes.hashCode() : 0);
        result = 31 * result + (sourceAttributes != null ? sourceAttributes.hashCode() : 0);
        result = 31 * result + (destinationAttributes != null ? destinationAttributes.hashCode() : 0);
        return result;
    }

    private boolean isValueValidType(Object value) {
        return (value instanceof String) ||
            (value instanceof Integer) ||
            (value instanceof Boolean) ||
            (value instanceof List);
    }

    @Override
    public String toString() {
        return "FederationFilter{" +
            "filterType='" + filterType + '\'' +
            ", methodName='" + methodName + '\'' +
            ", messageAttributes=" + messageAttributes +
            ", sourceAttributes=" + sourceAttributes +
            ", destinationAttributes=" + destinationAttributes +
            '}';
    }
}
