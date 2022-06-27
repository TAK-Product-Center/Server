package com.bbn.useraccountmanagement.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGenerationInBulkModel {

	private String usernameExpression; //e.g.“bbn-tak-[N]”
	private int startN;
	private int endN;
	private String[] groupList;
	private String[] groupListIN;
	private String[] groupListOUT;
	
	public String getUsernameExpression() {
		return usernameExpression;
	}

	public void setUsernameExpression(String usernameExpression) {
		this.usernameExpression = usernameExpression;
	}

	public int getStartN() {
		return startN;
	}

	public void setStartN(int startN) {
		this.startN = startN;
	}

	public int getEndN() {
		return endN;
	}

	public void setEndN(int endN) {
		this.endN = endN;
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
