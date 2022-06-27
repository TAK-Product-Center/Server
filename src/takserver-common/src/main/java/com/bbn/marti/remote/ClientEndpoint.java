package com.bbn.marti.remote;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;

public class ClientEndpoint implements Serializable {
	
	private static final long serialVersionUID = 8608993795587801515L;

	public ClientEndpoint() {}
	
	public ClientEndpoint(String callsign, String uid, Date lastEventTime, String lastEventName, String groups) {
		this.callsign = callsign;
		this.uid = uid;
		this.lastEventTime = lastEventTime;
		this.lastStatus = lastEventName;
		this.groups = groups;
	}
	
	public String getCallsign() {
		return callsign;
	}
	
	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}
	
	public String getUid() {
		return uid;
	}
	
	public void setUid(String uid) {
		this.uid = uid;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.S'Z'")
	public Date getLastEventTime() {
		return lastEventTime;
	}

	public void setLastEventTime(Date lastEventTime) {
		this.lastEventTime = lastEventTime;
	}

	public String getLastStatus() {
		return lastStatus;
	}

	public void setLastStatus(String lastStatus) {
		this.lastStatus = lastStatus;
	}

	@JsonIgnore
	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}


	private String callsign;
	private String uid;
	private Date lastEventTime;
	private String lastStatus;
	private String groups;
}
