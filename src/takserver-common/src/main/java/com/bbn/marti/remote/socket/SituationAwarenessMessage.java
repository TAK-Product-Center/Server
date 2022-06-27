

package com.bbn.marti.remote.socket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SituationAwarenessMessage extends TakMessage {
	
	private static final String className = SituationAwarenessMessage.class.getSimpleName();
	
	public static String getClassName() {
		return className;
	}

    private static final long serialVersionUID = -613511945635443840L;
    public static final String COT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.S'Z'";
    
    public String getMessageType() {
        return className;
    }
    
    private String uid;
    public SituationAwarenessMessage setUid(String uid) {
        this.uid = uid;
        return this;
    }
    public String getUid() { return uid; }

    private String type;
    public SituationAwarenessMessage setType(String type) {
        this.type = type;
        return this;
    }
    public String getType() { return type; }

    private Double lat;
    public SituationAwarenessMessage setLat(Double lat) {
        this.lat = lat;
        return this;
    }
    public Double getLat() { return lat; }

    private Double lon;
    public SituationAwarenessMessage setLon(Double lon) {
        this.lon = lon;
        return this;
    }
    public Double getLon() { return lon; }

    private String callsign;
    public SituationAwarenessMessage setCallsign(String callsign) {
        this.callsign = callsign;
        return this;
    }
    public String getCallsign() { return callsign; }

    private String how;
    public SituationAwarenessMessage setHow(String how) {
        this.how = how;
        return this;
    }
    public String getHow() { return how; }

    private Double hae;
    public SituationAwarenessMessage setHae(Double hae) {
        this.hae = hae;
        return this;
    }
    public Double getHae() { return hae; }

    private Double ce;
    public SituationAwarenessMessage setCe(Double ce) {
        this.ce = ce;
        return this;
    }
    public Double getCe() { return ce; }

    private Double le;
    public SituationAwarenessMessage setLe(Double le) {
        this.le = le;
        return this;
    }
    public Double getLe() { return le; }

    private Long start;
    public SituationAwarenessMessage setStart(Long start) {
        this.start = start;
        return this;
    }
    public Long getStart() { return start; }

    private Long time;
    public SituationAwarenessMessage setTime(Long time) {
        this.time = time;
        return this;
    }
    public Long getTime() { return time; }

    private Long stale;
    public SituationAwarenessMessage setStale(Long stale) {
        this.stale = stale;
        return this;
    }
    public Long getStale() { return stale; }

    private String group;
    public SituationAwarenessMessage setGroup(String group) {
        this.group = group;
        return this;
    }
    public String getGroup() { return group; }

    private String role;
    public SituationAwarenessMessage setRole(String role) {
        this.role = role;
        return this;
    }
    public String getRole() { return role; }

    private String iconsetPath;
    public SituationAwarenessMessage setIconsetPath(String iconsetPath) {
        this.iconsetPath = iconsetPath;
        return this;
    }
    public String getIconsetPath() { return iconsetPath; }

    private String color;
    public SituationAwarenessMessage setColor(String color) {
        this.color = color;
        return this;
    }

    private String phoneNumber;
    public SituationAwarenessMessage setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }
    public String getPhoneNumber() { return phoneNumber; }

    private String takv;
    public SituationAwarenessMessage setTakv(String takv) {
        this.takv = takv;
        return this;
    }
    public String getTakv() { return takv; }

    private Boolean persistent;
    public SituationAwarenessMessage setPersistent(Boolean persistent) {
        this.persistent = persistent;
        return this;
    }
    public Boolean getPersistent() { return persistent; }

    private String remarks;
    public SituationAwarenessMessage setRemarks(String remarks) {
        this.remarks = remarks;
        return this;
    }
    public String getRemarks() { return remarks; }
    
    public String getColor() { return color; }

    @JsonProperty("detail")
    @JsonRawValue
	private String detailJson;

	public String getDetailJson() {
		return detailJson;
	}
	public SituationAwarenessMessage setDetailJson(JsonNode detailJson) {
		this.detailJson = detailJson.toString();
		return this;
	}
	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SituationAwarenessMessage [messageType=");
        builder.append(className);
        builder.append(", uid=");
        builder.append(uid);
        builder.append(", type=");
        builder.append(type);
        builder.append(", lat=");
        builder.append(lat);
        builder.append(", lon=");
        builder.append(lon);
        builder.append(", callsign=");
        builder.append(callsign);
        builder.append(", how=");
        builder.append(how);
        builder.append(", hae=");
        builder.append(hae);
        builder.append(", ce=");
        builder.append(ce);
        builder.append(", le=");
        builder.append(le);
        builder.append(", start=");
        builder.append(start);
        builder.append(", time=");
        builder.append(time);
        builder.append(", stale=");
        builder.append(stale);
        builder.append(", group=");
        builder.append(group);
        builder.append(", role=");
        builder.append(role);
        builder.append(", iconsetPath=");
        builder.append(iconsetPath);
        builder.append(", color=");
        builder.append(color);
        builder.append(", phoneNumber=");
        builder.append(phoneNumber);
        builder.append(", takv=");
        builder.append(takv);
        builder.append(", persistent=");
        builder.append(persistent);
        builder.append(", remarks=");
        builder.append(remarks);
        builder.append(", getMissions()=");
        builder.append(getMissions());
        builder.append(", getUids()=");
        builder.append(getUids());
        builder.append(", getGroups()=");
        builder.append(getGroups());
        builder.append(", getAddresses()=");
        builder.append(getAddresses());
        builder.append("]");
        return builder.toString();
    }

}
