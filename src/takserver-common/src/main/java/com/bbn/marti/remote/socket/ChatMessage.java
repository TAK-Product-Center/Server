

package com.bbn.marti.remote.socket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage extends TakMessage {
	
	private static final String className = ChatMessage.class.getSimpleName();
	
	public static String getClassName() {
		return className;
	}
    
    private static final long serialVersionUID = 987123498721341L;
    
    public String getMessageType() {
        return className;
    }
    
    private String from;
    private Double lat;
    private Double lon;
    private Double hae;
    private String body;
    private Long timestamp;
    @JsonIgnore
    private String senderCallsign;
    @JsonIgnore
    private Set<String> conversationUids;
    
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getHae() {
        return hae;
    }

    public void setHae(Double hae) {
        this.hae = hae;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderCallsign() { return senderCallsign; }
    public void setSenderCallsign(String senderCallsign) { this.senderCallsign = senderCallsign; }

    public Set<String> getConversationUids() { return conversationUids; }
    public void setConversationUids(Set<String> conversationUids) { this.conversationUids = conversationUids; }

    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((addresses == null) ? 0 : addresses.hashCode());
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + ((hae == null) ? 0 : hae.hashCode());
        result = prime * result + ((lat == null) ? 0 : lat.hashCode());
        result = prime * result + ((lon == null) ? 0 : lon.hashCode());
        result = prime * result
                + ((className == null) ? 0 : className.hashCode());
        result = prime * result
                + ((timestamp == null) ? 0 : timestamp.hashCode());
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
        ChatMessage other = (ChatMessage) obj;
        if (addresses == null) {
            if (other.addresses != null)
                return false;
        } else if (!addresses.equals(other.addresses))
            return false;
        if (body == null) {
            if (other.body != null)
                return false;
        } else if (!body.equals(other.body))
            return false;
        if (from == null) {
            if (other.from != null)
                return false;
        } else if (!from.equals(other.from))
            return false;
        if (hae == null) {
            if (other.hae != null)
                return false;
        } else if (!hae.equals(other.hae))
            return false;
        if (lat == null) {
            if (other.lat != null)
                return false;
        } else if (!lat.equals(other.lat))
            return false;
        if (lon == null) {
            if (other.lon != null)
                return false;
        } else if (!lon.equals(other.lon))
            return false;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.getClassName()))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ChatMessage [messageType=");
        builder.append(className);
        builder.append(", from=");
        builder.append(from);
        builder.append(", addresses=");
        builder.append(addresses);
        builder.append(", lat=");
        builder.append(lat);
        builder.append(", lon=");
        builder.append(lon);
        builder.append(", hae=");
        builder.append(hae);
        builder.append(", body=");
        builder.append(body);
        builder.append(", timestamp=");
        builder.append(timestamp);
        builder.append(", missions=");
        builder.append(missions);
        builder.append(", groups=");
        builder.append(groups);
        builder.append("]");
        return builder.toString();
    }
 }
