package com.bbn.marti.remote;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Set;


public class LdapGroup implements Serializable {

	private static final long serialVersionUID = -6205682790971137582L;

	public LdapGroup() {}
	
	public LdapGroup(String cn, String dn) {
		this.cn = cn;
		this.dn = dn;
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

	@JsonIgnore
	public Set<String> getMembers() {
		return members;
	}

	public void setMembers(Set<String> members) {
		this.members = members;
	}

	private String cn;
	private String dn;
	private String description;
	private Set<String> members;
}
