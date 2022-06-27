

package com.bbn.marti.remote.groups;

import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.util.RemoteUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;

/*
 * 
 * AuthenticatedUser value class
 * 
 * Model properties of authenticated users
 * 
 */
public class AuthenticatedUser extends User {
    
    private static final long serialVersionUID = 211193260538717664L;
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedUser.class);

    @JsonIgnore
    protected String password;

    protected String cotSaUid;
    protected String callsign;
   
    // username / login id
    protected final String login;
    
    public AuthenticatedUser(
                @NotNull String id,
                @NotNull String connectionId,
                @NotNull String address,
                @Nullable X509Certificate cert,
                @NotNull String login,
                @NotNull String password,
                @Nullable String cotSaUid) {
       
        super(id, connectionId, ConnectionType.CORE, login, address, cert);
        
        if (login.isEmpty()) {
            throw new IllegalArgumentException("empty login");
        }
        
        this.login = login;
        this.password = password;
        this.cotSaUid = (cotSaUid == null ? "" : cotSaUid);
        this.callsign = "";
    }
    
    public AuthenticatedUser(
            @NotNull String id,
            @NotNull String connectionId,
            @NotNull String address,
            @Nullable X509Certificate cert,
            @NotNull String login,
            @NotNull String password,
            @Nullable String cotSaUid,
            @NotNull ConnectionType connectionType) {
   
    super(id, connectionId, connectionType, login, address, cert);
    
    if (login.isEmpty()) {
        throw new IllegalArgumentException("empty login");
    }
    
    this.login = login;
    this.password = password;
    this.cotSaUid = (cotSaUid == null ? "" : cotSaUid);
    this.callsign = "";
}
    
    // Copy the user, but generate a new random connection id
    public AuthenticatedUser(AuthenticatedUser that) {
        
        super(that.getId(), UUID.randomUUID().toString().replace("-", ""), that.getConnectionType(), that.getLogin(), that.getAddress(), that.getCert());
        
        this.login = that.getLogin();
        
        this.cotSaUid = that.getCotSaUid();
        this.callsign = that.getCallsign();
        
        logger.trace("copied user " + that + " to " + this);
    }
    
    public String getLogin() {
        return login;
    }
    
    @Override
    public String getDisplayName() {
        return getLogin();
    }

    public String getPassword() {
      return password;
    }
    
    @Override
    @JsonIgnore
    public Set<Node> getNeighbors() {
        return Sets.newHashSet();
    }
    
    public String getCotSaUid() {
        return cotSaUid;
    }

    public void setCotSaUid(String cotSaUid) {
        this.cotSaUid = cotSaUid;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }
    
    public String getAddress() {
        return address;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }
    
    @Override
    public String toString() {
        return "AuthenticatedUser [cotSaUid=" + cotSaUid + ", callsign="
                + callsign + ", login=" + login + ", id=" + id
                + ", connectionId=" + connectionId + ", connectionType="
                + connectionType + ", address=" + address + " cert: " + (cert == null ? "null" : RemoteUtil.getInstance().getCertSHA256Fingerprint(getCert())) +
                " authorities: " + getAuthorities() + ", created="
                + created + "]";
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((cotSaUid == null) ? 0 : cotSaUid.hashCode());
		result = prime * result + ((login == null) ? 0 : login.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthenticatedUser other = (AuthenticatedUser) obj;
		if (cotSaUid == null) {
			if (other.cotSaUid != null)
				return false;
		} else if (!cotSaUid.equals(other.cotSaUid))
			return false;
		if (login == null) {
			if (other.login != null)
				return false;
		} else if (!login.equals(other.login))
			return false;
		return true;
	}
}
