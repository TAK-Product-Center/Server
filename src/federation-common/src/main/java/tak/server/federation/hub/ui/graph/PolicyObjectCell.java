package tak.server.federation.hub.ui.graph;

import tak.server.federation.hub.ui.JsonRawValueDeserializer;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 5/15/2017.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="graphType")
@JsonSubTypes({ @JsonSubTypes.Type(value = FederateCell.class, name = "FederateCell"),
                @JsonSubTypes.Type(value = GroupCell.class, name= "GroupCell"),
                @JsonSubTypes.Type(value = EdgeCell.class, name = "EdgeCell")})
public abstract class PolicyObjectCell {

    private String id;

    /**
     * Special field for storing UI attributes. These attributes sometimes
     * have Json fields that start with a '.', which is not allowed in Mongo,
     * so we just store the entire attrs as a raw value.
     */
    @JsonRawValue
    @JsonDeserialize(using = JsonRawValueDeserializer.class)
    private Object attrs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getAttrs() {
        return attrs;
    }

    public void setAttrs(Object attrs) {
        this.attrs = attrs;
    }

    /**
     * Catch all for Json properties that are not specifically defined above.
     * Adding this catch-all allows storage and recall of any Json fields from the client.
     */
    protected Map<String, Object> other = new HashMap<String, Object>();


    @JsonAnyGetter
    public Map<String,Object> getOther() {
        return other;
    }

    @JsonAnySetter
    public void addOther(String name, Object value) {
        other.put(name, value);
    }
}
