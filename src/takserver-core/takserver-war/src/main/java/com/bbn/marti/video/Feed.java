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
import com.bbn.security.web.MartiValidatorConstants;

import java.net.MalformedURLException;
import java.net.URL;


@XmlRootElement(name = "feed")
public class Feed {
	
	protected static final Logger logger = LoggerFactory.getLogger(Feed.class);
	
    public static enum Type {
        VIDEO,
        SENSOR_POINT
    }
	
    protected int id;
    protected String uuid;
    protected Type type;

    protected boolean active;
    protected String alias;

    protected String address;
    protected String macAddress;
    protected String port;
    protected String roverPort;
    protected String ignoreEmbeddedKLV;
    protected String path;
    protected String protocol;
    protected String source;
    protected String networkTimeout;
    protected String bufferTime;
    protected String rtspReliable;
    protected String thumbnail;
    protected String classification;

    protected String latitude;
    protected String longitude;
    protected String fov;
    protected String heading;
    protected String range;
    

    public Feed() { }

    public Feed(FeedV2 feedV2) {
        this.uuid = feedV2.uuid;
        this.active = feedV2.active;
        this.alias = feedV2.alias;

        try {
            URL url = new URL(feedV2.getUrl());
            this.protocol = url.getProtocol();
            this.address = url.getHost();
            this.port = String.valueOf(url.getPort());
            this.path = url.getPath();
        } catch (MalformedURLException e) {
            logger.error("exception parsing feedv2 url!!", e);
        }

        this.macAddress = feedV2.macAddress;
        this.roverPort = feedV2.roverPort;
        this.ignoreEmbeddedKLV = feedV2.ignoreEmbeddedKLV;
        this.source = feedV2.source;
        this.networkTimeout = feedV2.networkTimeout;
        this.bufferTime = feedV2.bufferTime;
        this.rtspReliable = feedV2.rtspReliable;
        this.thumbnail = feedV2.thumbnail;
        this.classification = feedV2.classification;
        this.latitude = feedV2.latitude;
        this.longitude = feedV2.longitude;
        this.fov = feedV2.fov;
        this.heading = feedV2.heading;
        this.range = feedV2.range;
    }


    public boolean validate(Validator validator) {
    	
    	try {
    		if (getAlias() == null || getProtocol() == null || getAddress() == null) {
    			return false;
    		}
    		
			validator.getValidInput("feed", getUuid(), 
					MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
			validator.getValidInput("feed", getAlias(), 
					MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getLatitude(), 
					MartiValidatorConstants.Regex.Coordinates.name(), MartiValidatorConstants.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getLongitude(), 
					MartiValidatorConstants.Regex.Coordinates.name(), MartiValidatorConstants.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getFov(), 
					MartiValidatorConstants.Regex.Double.name(), MartiValidatorConstants.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getHeading(), 
					MartiValidatorConstants.Regex.Double.name(), MartiValidatorConstants.SHORT_STRING_CHARS, true);
			validator.getValidInput("feed", getRange(), 
					MartiValidatorConstants.Regex.Double.name(), MartiValidatorConstants.SHORT_STRING_CHARS, true);
		
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
