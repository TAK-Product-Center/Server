

package com.bbn.marti.remote;

import java.io.Serializable;

public class RemoteContact implements Serializable {

    private static final long serialVersionUID = -6291987036230718253L;
    
    private String contactName = null;
    private long lastHeardFromMillis = 0;
    private String endpoint = null;
    private String uid = null;

    public RemoteContact() {}

    public RemoteContact(RemoteContact r) {
        contactName = r.contactName;
        lastHeardFromMillis = r.lastHeardFromMillis;
        endpoint = r.endpoint;
        uid = r.uid;
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
