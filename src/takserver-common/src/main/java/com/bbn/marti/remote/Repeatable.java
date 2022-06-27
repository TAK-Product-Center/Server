

package com.bbn.marti.remote;

import java.io.Serializable;
import java.util.Date;

/**
 * This class represents the core of a repeatable message, and is what is shared with the MartiWebApps
 * for use and display in the admin interface.
 */
public class Repeatable implements Serializable {
	private static final long serialVersionUID = -8976670978893546521L;
	
	private String uid;
	private String repeatType;
	private String cotType;
	private Date dateTimeActivated;
	private String xml;
	private String callsign;
	
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getXml() {
		return xml;
	}
	public void setXml(String xml) {
		this.xml = xml;
	}
	public String getRepeatType() {
		return repeatType;
	}
	public void setRepeatType(String repeatType) {
		this.repeatType = repeatType;
	}
	public Date getDateTimeActivated() {
		return dateTimeActivated;
	}
	public void setDateTimeActivated(Date dateTimeActivated) {
		this.dateTimeActivated = dateTimeActivated;
	}
	public String getCallsign() {
		return callsign;
	}
	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}
	public String getCotType() {
		return cotType;
	}
	public void setCotType(String cotType) {
		this.cotType = cotType;
	}
	
}
