

package com.bbn.marti.network;

import java.io.Serializable;

import com.bbn.marti.network.FederationApi.DirectionValue;

public class FederateGroupAssociation implements Serializable {

	private static final long serialVersionUID = -6280258951747614201L;

	public FederateGroupAssociation() {};
	
	public FederateGroupAssociation(String federateId, String group, DirectionValue direction) {
		this.federateId = federateId;
		this.group = group;
		this.direction = direction;
	}
	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public DirectionValue getDirection() {
		return direction;
	}

	public void setDirection(DirectionValue direction) {
		this.direction = direction;
	}

	public String getFederateId() {
		return federateId;
	}

	public void setFederateId(String federateId) {
		this.federateId = federateId;
	}

	private String group;
	private DirectionValue direction;
	private String federateId;
}
