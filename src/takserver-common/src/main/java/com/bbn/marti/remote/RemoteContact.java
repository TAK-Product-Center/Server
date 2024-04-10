

package com.bbn.marti.remote;

import com.bbn.marti.remote.groups.GroupMapping;

import java.io.Serializable;
import java.util.List;

public class RemoteContact implements Serializable {

    private static final long serialVersionUID = -6291987036230718253L;

    private String contactName = null;
    private long lastHeardFromMillis = 0;
    private String endpoint = null;
    private String uid = null;
    private List<GroupMapping> groupMappings;
    private List<String> outGroups;
    private List<String> inGroups;

    public RemoteContact() {}

    public RemoteContact(RemoteContact r) {
        contactName = r.contactName;
        lastHeardFromMillis = r.lastHeardFromMillis;
        endpoint = r.endpoint;
        uid = r.uid;
        groupMappings = r.groupMappings;
        outGroups = r.outGroups;
        inGroups = r.inGroups;
    }

    public RemoteContact(RemoteContact r,
                         List<GroupMapping> groupMappings,
                         List<String> outGroups, List<String> inGroups) {
        contactName = r.contactName;
        lastHeardFromMillis = r.lastHeardFromMillis;
        endpoint = r.endpoint;
        uid = r.uid;
        this.groupMappings = groupMappings;
        this.outGroups = outGroups;
        this.inGroups = inGroups;
    }

    public List<GroupMapping> getGroupMappings() {
        return groupMappings;
    }

    public void setGroupMappings(List<GroupMapping> groupMappings) {
        this.groupMappings = groupMappings;
    }

    public List<String> getOutGroups() {
        return outGroups;
    }

    public void setOutGroups(List<String> outGroups) {
        this.outGroups = outGroups;
    }

    public List<String> getInGroups() {
        return inGroups;
    }

    public void setInGroups(List<String> inGroups) {
        this.inGroups = inGroups;
    }

    public String getContactName() {
        return contactName;
    }

    public RemoteContact setContactName(String name) {
        this.contactName = name;
        return this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public RemoteContact setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public RemoteContact setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public long getLastHeardFromMillis() {
        return lastHeardFromMillis;
    }

    public RemoteContact setLastHeardFromMillis(long millis) {
        this.lastHeardFromMillis = millis;
        return this;
    }

    @Override
	public boolean equals(Object otherContact) {
		if (otherContact != null &&
                      otherContact instanceof RemoteContact &&
                      this.uid.equals( ((RemoteContact)otherContact).uid))
                {
                   return true;
                }
		return false;
	}

	@Override
	public int hashCode() {
		return 7612 + uid.hashCode();
	}

	@Override
	public String toString() {
		return "<Contact " + contactName + " @ " + endpoint + ">";
	}
}