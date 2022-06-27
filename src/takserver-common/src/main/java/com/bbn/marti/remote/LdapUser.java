package com.bbn.marti.remote;

import com.bbn.marti.config.Auth;

public class LdapUser {

    public LdapUser() {}

    public LdapUser(String cn, String dn, String description, String callsign, String color, String role) {
        this.cn = cn;
        this.dn = dn;
        this.description = description;
        this.callsign = callsign;
        this.color = color;
        this.role = role;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    private String cn;
    private String dn;
    private String description;
    private String callsign;
    private String color;
    private String role;
}
