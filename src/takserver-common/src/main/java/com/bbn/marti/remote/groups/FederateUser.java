

package com.bbn.marti.remote.groups;

import java.security.cert.X509Certificate;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Federation.Federate;
import com.fasterxml.jackson.annotation.JsonIgnore;

/*
 * 
 * FederateUser value class
 * 
 * Model properties of actively connected federate users
 * 
 */
public class FederateUser extends User {
	
	private static final Logger logger = LoggerFactory.getLogger(FederateUser.class);
    
    private static final long serialVersionUID = 1202174530216911860L;
    
    // chain of trust for this federates X509 cert
    protected final X509Certificate[] trustChain;
    
    // link back to the federate configuration object from CoreConfig
    protected Federate federateConfig;

    public FederateUser(
            @NotNull String id,
            @NotNull String connectionId,
            @NotNull String name,
            @NotNull String address,
            @Nullable X509Certificate cert,
            @Nullable X509Certificate[] trustChain,
            @Nullable Federate federateConfig) {
   
        super(id, connectionId, ConnectionType.FEDERATE, name, address, cert);
        
        this.trustChain = trustChain;
        this.federateConfig = federateConfig;
    }

    public FederateUser(FederateUser src) {
        super(src.id, UUID.randomUUID().toString().replace("-", ""), src.connectionType, src.name, src.address, src.cert);
        this.trustChain = src.trustChain;
        this.federateConfig = src.federateConfig;
    }

    @JsonIgnore
    public Federate getFederateConfig() {
        return federateConfig;
    }

    public void setFederateConfig(Federate federateConfig) {
        this.federateConfig = federateConfig;
    }
    
    @JsonIgnore
    public X509Certificate[] getTrustChain() {
        return trustChain;
    }
 
	@Override
    public String toString() {
        return "FederateUser [federateConfig=" + federateConfig + ", id=" + id
                + ", connectionId=" + connectionId + ", connectionType="
                + connectionType + ", address=" + address
                + ", getDisplayName()=" + getDisplayName() + "]";
    }    
}
