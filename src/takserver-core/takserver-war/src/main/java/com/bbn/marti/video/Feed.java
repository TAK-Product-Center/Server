/*
 * 
 * The Feed class stores metadata about video feeds
 * 
 */

package com.bbn.marti.video;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.owasp.esapi.*;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bbn.security.web.MartiValidator;


@XmlRootElement(name = "feed")
public class Feed {
	
	protected static final Logger logger = LoggerFactory.getLogger(Feed.class);
	
    public static enum Type {
        VIDEO,
        SENSOR_POINT
    }
	
    private int id;
    private String uuid;
    private Type type;
    
    private boolean active;
    private String alias;
    
    private String address;
    private String macAddress;
    private String port;
    private String roverPort;
    private String ignoreEmbeddedKLV;
    private String path;
    private String protocol;
    private String source;
    private String networkTimeout;
    private String bufferTime;
    private String rtspReliable;
    private String thumbnail;
    private String classification;

    private String latitude;
    private String longitude;
    private String fov;
    private String heading;
    private String range;
    

    public Feed() { }
    
    public boolean validate(Validator validator) {
    	
    	try {
    		if (getAlias() == null || getProtocol() == null || getAddress() == null) {
    			return false;
    		}
    		
			validator.getValidInput("feed", getUuid(), 
					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
			validator.getValidInput("feed", getAlias(), 
					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getLatitude(), 
					MartiValidator.Regex.Coordinates.name(), MartiValidator.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getLongitude(), 
					MartiValidator.Regex.Coordinates.name(), MartiValidator.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getFov(), 
					MartiValidator.Regex.Double.name(), MartiValidator.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getHeading(), 
					MartiValidator.Regex.Double.name(), MartiValidator.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getRange(), 
					MartiValidator.Regex.Double.name(), MartiValidator.SHORT_STRING_CHARS, true);
		
	    } catch (ValidationException e) {
	        e.printStackTrace();
	        logger.error("Exception!", e);	    	
	        return false;
	    }
		
    	return true;
    }  
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }   
    
    @XmlElement(name = "active")
    public boolean getActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }    
    
    @XmlElement(name = "uid")
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }    
    
    @XmlElement(name = "type")
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }        
    
    @XmlElement(name = "alias")
    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }        
    
    @XmlElement(name = "address")
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }  

    @XmlElement(name = "preferredMacAddress")
    public String getMacAddress() {
        return macAddress;
    }
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }  

    @XmlElement(name = "port")
    public String getPort() {
        return port;
    }
    public void setPort(String port) {
        this.port = port;
    }  

    @XmlElement(name = "roverPort")
    public String getRoverPort() {
        return roverPort;
    }
    public void setRoverPort(String roverPort) {
        this.roverPort = roverPort;
    }  

    @XmlElement(name = "ignoreEmbeddedKLV")
    public String getIgnoreEmbeddedKLV() {
        return ignoreEmbeddedKLV;
    }
    public void setIgnoreEmbeddedKLV(String ignoreEmbeddedKLV) {
        this.ignoreEmbeddedKLV = ignoreEmbeddedKLV;
    }  

    @XmlElement(name = "path")
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }  

    @XmlElement(name = "protocol")
    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }  

    @XmlElement(name = "source")
    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
    }  

    @XmlElement(name = "timeout")
    public String getNetworkTimeout() {
        return networkTimeout;
    }
    public void setNetworkTimeout(String networkTimeout) {
        this.networkTimeout = networkTimeout;
    }  
    
    @XmlElement(name = "buffer")
    public String getBufferTime() {
        return bufferTime;
    }
    public void setBufferTime(String bufferTime) {
        this.bufferTime= bufferTime;
    }  
    
    @XmlElement(name = "rtspReliable")
    public String getRtspReliable() {
        return rtspReliable;
    }
    public void setRtspReliable(String rtspReliable) {
        this.rtspReliable = rtspReliable;
    }

    @XmlElement(name = "thumbnail")
    public String getThumbnail() {
        return thumbnail;
    }
    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    @XmlElement(name = "classification")
    public String getClassification() {
        return classification;
    }
    public void setClassification(String classification) {
        this.classification = classification;
    }

    @XmlElement(name = "latitude")
    public String getLatitude() {
        return latitude;
    }
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }         
    
    @XmlElement(name = "longitude")
    public String getLongitude() {
        return longitude;
    }
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }         
    
    @XmlElement(name = "fov")
    public String getFov() {
        return fov;
    }
    public void setFov(String fov) {
        this.fov = fov;
    }         
    
    @XmlElement(name = "heading")
    public String getHeading() {
        return heading;
    }
    public void setHeading(String heading) {
        this.heading = heading;
    }         
    
    @XmlElement(name = "range")
    public String getRange() {
        return range;
    }
    public void setRange(String range) {
        this.range = range;
    }         
}
