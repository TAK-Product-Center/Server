package com.bbn.marti.remote;

import java.io.Serializable;
import java.util.List;

public class RemoteSubscriptionLite implements Serializable {
	private List<String> filterGroups;
    private String notes;
    private String callsign;
    private String team;
    private String role;
    private String takv;
    private String uid;
    
    public RemoteSubscriptionLite(RemoteSubscription remoteSubscription) {
    	this.uid = remoteSubscription.clientUid;
    	this.takv = remoteSubscription.takv;
    	this.role = remoteSubscription.role;
    	this.team = remoteSubscription.team;
    	this.callsign = remoteSubscription.callsign;
    	this.notes = remoteSubscription.notes;
    	this.filterGroups = remoteSubscription.filterGroups;
    }

	public List<String> getFilterGroups() {
		return filterGroups;
	}

	public void setFilterGroups(List<String> filterGroups) {
		this.filterGroups = filterGroups;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getCallsign() {
		return callsign;
	}

	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getTakv() {
		return takv;
	}

	public void setTakv(String takv) {
		this.takv = takv;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
}
