package tak.server.federation.hub.ui;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UidHolder {

    private String uid;
    private List<String> groups;
    Map<String, Object> attributes;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> newAttributes) {
        attributes = newAttributes;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public UidHolder() {
        attributes = new HashMap<>();
    }

    public UidHolder(String uid) {
        super();
        this.uid = uid;
    }
}
