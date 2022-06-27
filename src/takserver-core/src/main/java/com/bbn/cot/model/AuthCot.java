

package com.bbn.cot.model;

/*
 * 
 * Value class representing the CoT part of an auth message
 * 
 */
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class AuthCot {
    
    private String username;
    private String password;
    private String uid;
    private String callsign;
    
    @XmlAttribute
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    @XmlAttribute
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    @XmlAttribute
    public String getUid() {
        return uid;
    }
    
    public void setUid(String uid) {
        this.uid = uid;
    }
    
    @XmlAttribute
    public String getCallsign() {
        return callsign;
    }
    
    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    @Override
    public String toString() {
        return "AuthCot [username=" + username + ", uid=" + uid + ", callsign=" + callsign + "]";
    }

}
