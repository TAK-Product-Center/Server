package com.bbn.marti.remote.groups;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimpleGroupWithUsersModel {

	private String groupname;
	private String[] usersInGroupList;
	private String[] usersInGroupListIN;
	private String[] usersInGroupListOUT;
	
	public String getGroupname() {
		return groupname;
	}
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}
	public String[] getUsersInGroupList() {
		return usersInGroupList;
	}
	public void setUsersInGroupList(String[] usersInGroupList) {
		this.usersInGroupList = usersInGroupList;
	}
	public String[] getUsersInGroupListIN() {
		return usersInGroupListIN;
	}
	public void setUsersInGroupListIN(String[] usersInGroupListIN) {
		this.usersInGroupListIN = usersInGroupListIN;
	}
	public String[] getUsersInGroupListOUT() {
		return usersInGroupListOUT;
	}
	public void setUsersInGroupListOUT(String[] usersInGroupListOUT) {
		this.usersInGroupListOUT = usersInGroupListOUT;
	}
	


}
