package com.bbn.useraccountmanagement.model;

public class GroupNameModel {

	private String groupname;

	public GroupNameModel(String groupname) {
		this.groupname = groupname;
	}

	public String getGroupname() {
		return groupname;
	}

	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupname == null) ? 0 : groupname.hashCode());
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
		GroupNameModel other = (GroupNameModel) obj;
		if (groupname == null) {
			if (other.groupname != null)
				return false;
		} else if (!groupname.equals(other.groupname))
			return false;
		return true;
	}
	
	
	
}
