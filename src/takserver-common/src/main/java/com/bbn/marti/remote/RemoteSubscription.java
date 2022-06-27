

package com.bbn.marti.remote;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.bbn.marti.config.Subscription;
import com.bbn.marti.remote.groups.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteSubscription implements Serializable {
	
	private static final String className = RemoteSubscription.class.getSimpleName();
	
	public static String getClassName() {
		return className;
	}
	
    private static final long serialVersionUID = 8134731508521872646L;
    
    @JsonIgnore @QuerySqlField public String uid = "";
    
    @JsonIgnore @QuerySqlField public String to = "";
    
    @JsonIgnore @QuerySqlField public String xpath = "";
    
    @JsonIgnore @QuerySqlField public String handlerType = "";
   
    @JsonIgnore public AtomicBoolean isWebsocket =  new AtomicBoolean(false);
   
    @JsonIgnore @QuerySqlField public UUID originNode;
    
    @JsonIgnore public UUID websocketApiNode;
    
    @JsonIgnore public AtomicBoolean isFederated = new AtomicBoolean(false);
    
    @JsonIgnore public AtomicBoolean suspended = new AtomicBoolean(false);
    
    @JsonIgnore @QuerySqlField public AtomicInteger numHits = new AtomicInteger(0);
    
    @JsonIgnore @QuerySqlField public AtomicLong writeQueueDepth = new AtomicLong(0L);
    
    @JsonIgnore @QuerySqlField public AtomicLong lastProcTime = new AtomicLong(0L);
    
    @JsonIgnore public AtomicBoolean hasUpdate = new AtomicBoolean(false);
    
    @JsonIgnore protected static final AtomicLong totalSubmitted = new AtomicLong(0L); 
    
    // the User object associated with this subscription, if there is one
    @JsonIgnore protected User user;

    @QuerySqlField @JsonIgnore protected String connectionId;
    @QuerySqlField @JsonIgnore protected String username;
    @JsonIgnore protected String dn;

    @JsonIgnore @QuerySqlField public boolean incognito = false;
    @JsonIgnore @QuerySqlField public String iface;

    public List<String> filterGroups;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public void prepareForSerialization() {
        if (getUser() != null) {
            setUsername(getUser().getId());
            setConnectionId(getUser().getConnectionId());

            if (getUser().getCert() != null) {
                setDn(getUser().getCert().getSubjectDN().toString());
            }
            setUser(null);
        }
    }

    @QuerySqlField public String notes = "";
    @QuerySqlField(index = true) public String callsign = "";
    @QuerySqlField public String team = "";
    @QuerySqlField public String role = "";
    @QuerySqlField public String takv = "";

    @JsonProperty("uid")
    @QuerySqlField(index = true)
    public String clientUid = "";

    // QoS attributes; need to be collected on-demand from external components
    @JsonIgnore @QuerySqlField public String mode;
    @JsonIgnore @QuerySqlField public int currentBandwidth;
    @JsonIgnore public int currentQueueDepth;
    @JsonIgnore public int queueCapacity;
    // End QoS attributes

    public RemoteSubscription(RemoteSubscription toCopy) {
        this.uid = toCopy.uid;
        this.to = toCopy.to;
        this.originNode = toCopy.originNode;
        this.websocketApiNode = toCopy.websocketApiNode;
        this.xpath = toCopy.xpath;
        this.isFederated.set(toCopy.isFederated.get());
        this.suspended = toCopy.suspended;
        this.numHits = toCopy.numHits;
        this.writeQueueDepth = toCopy.writeQueueDepth;
        this.lastProcTime = toCopy.lastProcTime;
        this.notes = toCopy.notes;
        this.callsign = toCopy.callsign;
        this.clientUid = toCopy.clientUid;
        this.team = toCopy.team;
        this.role = toCopy.role;
        this.takv = toCopy.takv;
        this.incognito = toCopy.incognito;
        this.filterGroups = toCopy.filterGroups;
        this.handlerType = toCopy.handlerType;
        this.isWebsocket = toCopy.isWebsocket;
        setUser(toCopy.getUser());
        this.connectionId = toCopy.connectionId;
        this.username = toCopy.username;
        this.dn = toCopy.dn;
        this.iface = toCopy.iface;

        this.mode = toCopy.mode;
        this.currentBandwidth = toCopy.currentBandwidth;
        this.currentQueueDepth = toCopy.currentQueueDepth;
    }

    public RemoteSubscription() {}

    public void incHit(long curTime) {
    	hasUpdate.set(true);
        lastProcTime.set(curTime);
        numHits.incrementAndGet();
    }

    public String toString() {
        return "uid: " + uid + "; to: " + to + "; xpath: " + xpath;
    }

    public Subscription.Static toStatic() throws Exception {
        String[] tokens = to.split(":");
        if (tokens.length < 3) {
            throw new Exception("Malformed endpoint!");
        }

        Subscription.Static s = new Subscription.Static();
        s.setName(uid);
        s.setProtocol(tokens[0]);
        s.setAddress(tokens[1]);
        s.setPort(Integer.parseInt(tokens[2]));
        s.setXpath(xpath);
        s.setIface(iface);
        for(String groupName : this.filterGroups){
            s.getFiltergroup().add(groupName);
        }
        return s;
    }

    @JsonIgnore
    public boolean isFederated() {
        return isFederated.get();
    }

    public static Comparator<RemoteSubscription> sortByCallsign(final boolean reversed) {
        return new Comparator<RemoteSubscription>() {
            public int compare(RemoteSubscription left, RemoteSubscription right) {
                if (left == null || right == null || left.callsign == null || right.callsign == null) {
                    throw new IllegalArgumentException("Subscription or callsign is null");
                }

                if (reversed) {
                    return right.callsign.compareToIgnoreCase(left.callsign);
                }

                return left.callsign.compareToIgnoreCase(right.callsign);
            }
        };
    }


    public static Comparator<RemoteSubscription> sortByClientUid(final boolean reversed) {
        return new Comparator<RemoteSubscription>() {
            public int compare(RemoteSubscription left, RemoteSubscription right) {
                if (left == null || right == null || left.clientUid == null || right.clientUid == null) {
                    throw new IllegalArgumentException("Subscription or clientUid is null");
                }

                if (reversed) {
                    return right.clientUid.compareToIgnoreCase(left.clientUid);
                }

                return left.clientUid.compareToIgnoreCase(right.clientUid);

            }
        };
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((clientUid == null) ? 0 : clientUid.hashCode());
        return result;
    }

    // determine equality by client uid
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RemoteSubscription other = (RemoteSubscription) obj;
        if (clientUid == null) {
            if (other.clientUid != null)
                return false;
        } else if (!clientUid.equals(other.clientUid))
            return false;
        return true;
    }
}


