package com.bbn.marti.groups;

import java.util.List;

public class FileUserAccount {
	
    protected String identifier;
    protected String password;
    protected Boolean passwordHashed;
    protected List<String> groupList;
    protected List<String> groupListIN;
    protected List<String> groupListOUT;
    
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Boolean getPasswordHashed() {
		return passwordHashed;
	}
	public void setPasswordHashed(Boolean passwordHashed) {
		this.passwordHashed = passwordHashed;
	}
	public List<String> getGroupList() {
		return groupList;
	}
	public void setGroupList(List<String> groupList) {
		this.groupList = groupList;
	}
	public List<String> getGroupListIN() {
		return groupListIN;
	}
	public void setGroupListIN(List<String> groupListIN) {
		this.groupListIN = groupListIN;
	}
	public List<String> getGroupListOUT() {
		return groupListOUT;
	}
	public void setGroupListOUT(List<String> groupListOUT) {
		this.groupListOUT = groupListOUT;
	}

    
}
