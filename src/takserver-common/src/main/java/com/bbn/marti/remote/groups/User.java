

package com.bbn.marti.remote.groups;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;

import tak.server.ignite.IgniteHolder;

/*
 * 
 * User abstract super class.
 * 
 * Model common properties of user subclasses
 * 
 */
public abstract class User implements Node, Comparable<User>, Serializable {

    private static final long serialVersionUID = 8002622862665912632L;

    protected final String id;
    protected String name;
    protected final String connectionId;
    protected final ConnectionType connectionType;
    protected final String address;
    protected final Date created;
    protected final X509Certificate cert;
    protected final Set<String> authorities;

    @JsonIgnore
    protected final UUID originNode;

    @JsonIgnore
    protected String token;

    public User(@NotNull String id, @NotNull String connectionId, @NotNull ConnectionType type, @NotNull String name, @NotNull String address, @Nullable X509Certificate cert) {
        
        // allowing empty string address
        if (id.isEmpty()) {
            throw new IllegalArgumentException("empty id in constructor");
        }
        
        created = new Date();
        this.id = id;
        this.connectionId = connectionId;
        this.connectionType = type;
        this.name = name;
        this.address = address;
        this.cert = cert;
        this.authorities = new ConcurrentSkipListSet<>();
        this.originNode = IgniteHolder.getInstance().getIgniteId();
    }

    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return name;
    }
    
    public Date getCreated() {
        return created;
    }

    @Override
    @JsonIgnore
    public Set<Node> getNeighbors() {
        return Sets.newHashSet();
    }
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setName(String name) { this.name = name; }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
    
    @JsonIgnore
    public X509Certificate getCert() {
        return cert;
    }
    
    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setToken(String token) { this.token = token; }

    public String getToken() { return token; }

    // User nodes are always leaves
    @Override
    @JsonIgnore
    public boolean isLeaf() {
        return true;
    }

    @JsonIgnore
    public UUID getOriginNode() {
		return originNode;
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connectionId == null) ? 0 : connectionId.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (connectionId == null) {
            if (other.connectionId != null)
                return false;
        } else if (!connectionId.equals(other.connectionId))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "User [displayName=" + getDisplayName() + " id=" + id + ", connectionId=" + connectionId + ", connectionType=" + connectionType + ", ip=" + address + ", created=" + created + "]";
    }

    @Override
    public int compareTo(User that) {
        // determine uniqueness by only this pair: {id, connectionId}
        return ComparisonChain.start().compare(getId(), that.getId()).compare(getConnectionId(), that.getConnectionId()).result();
    }
    
    
}
