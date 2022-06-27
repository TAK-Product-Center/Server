package com.bbn.useraccountmanagement.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimpleUserGroupModel {

	private String username;
	private String[] groupList;
	private String[] groupListIN;
	private String[] groupListOUT;
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String[] getGroupList() {
		return groupList;
	}

	public void setGroupList(String[] groupList) {
		this.groupList = groupList;
	}

	public String[] getGroupListIN() {
		return groupListIN;
	}

	public void setGroupListIN(String[] groupListIN) {
		this.groupListIN = groupListIN;
	}

	public String[] getGroupListOUT() {
		return groupListOUT;
	}

	public void setGroupListOUT(String[] groupListOUT) {
		this.groupListOUT = groupListOUT;
	}

	

}
