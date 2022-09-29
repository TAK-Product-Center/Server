/*
 *
 * The Feed class stores metadata about video feeds
 *
 */

package com.bbn.marti.video;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.owasp.esapi.*;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;

@XmlRootElement(name = "feed")
public class FeedV2 {

    protected static final Logger logger = LoggerFactory.getLogger(Feed.class);

    protected int id;
    protected String uuid;
    protected boolean active;
    protected String alias;
    protected String url;
    protected Integer order;

    protected String macAddress;
    protected String roverPort;
    protected String ignoreEmbeddedKLV;
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

    protected Integer width;
    protected Integer height;
    protected Integer bitrate;

    public FeedV2() { }

    public FeedV2(Feed feedV1) {
        this.uuid = feedV1.uuid;
        this.active = feedV1.active;
        this.alias = feedV1.alias;

        this.url = "";

        if (!feedV1.protocol.equals("raw")) {
            this.url += feedV1.protocol + "://";
        }

        this.url += feedV1.address;

        if (feedV1.port != null && !feedV1.port.equals("-1")) {
            this.url += ":" + feedV1.port;
        }

        if (feedV1.path != null && feedV1.path.length() > 0) {
            if (!feedV1.path.startsWith("/")) {
                this.url += "/";
            }

            this.url += feedV1.path;
        }

        this.macAddress = feedV1.macAddress;
        this.roverPort = feedV1.roverPort;
        this.ignoreEmbeddedKLV = feedV1.ignoreEmbeddedKLV;
        this.source = feedV1.source;
        this.networkTimeout = feedV1.networkTimeout;
        this.bufferTime = feedV1.bufferTime;
        this.rtspReliable = feedV1.rtspReliable;
        this.thumbnail = feedV1.thumbnail;
        this.classification = feedV1.classification;
        this.latitude = feedV1.latitude;
        this.longitude = feedV1.longitude;
        this.fov = feedV1.fov;
        this.heading = feedV1.heading;
        this.range = feedV1.range;
    }

    public boolean validate(Validator validator) {

        try {
            if (getAlias() == null || getUrl() == null) {
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

    @XmlTransient
    @JsonIgnore
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    @XmlAttribute(name = "active")
    public boolean getActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    @XmlAttribute(name = "uid")
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @XmlAttribute(name = "alias")
    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }

    @XmlAttribute(name = "url")
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    @XmlAttribute(name = "order")
    public Integer getOrder() {
        return order;
    }
    public void setOrder(Integer order) {
        this.order = order;
    }

    @XmlAttribute(name = "preferredMacAddress")
    public String getMacAddress() {
        return macAddress;
    }
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    @XmlAttribute(name = "roverPort")
    public String getRoverPort() {
        return roverPort;
    }
    public void setRoverPort(String roverPort) {
        this.roverPort = roverPort;
    }

    @XmlAttribute(name = "ignoreEmbeddedKLV")
    public String getIgnoreEmbeddedKLV() {
        return ignoreEmbeddedKLV;
    }
    public void setIgnoreEmbeddedKLV(String ignoreEmbeddedKLV) {
        this.ignoreEmbeddedKLV = ignoreEmbeddedKLV;
    }

    @XmlAttribute(name = "source")
    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
    }

    @XmlAttribute(name = "timeout")
    public String getNetworkTimeout() {
        return networkTimeout;
    }
    public void setNetworkTimeout(String networkTimeout) {
        this.networkTimeout = networkTimeout;
    }

    @XmlAttribute(name = "buffer")
    public String getBufferTime() {
        return bufferTime;
    }
    public void setBufferTime(String bufferTime) {
        this.bufferTime= bufferTime;
    }

    @XmlAttribute(name = "rtspReliable")
    public String getRtspReliable() {
        return rtspReliable;
    }
    public void setRtspReliable(String rtspReliable) {
        this.rtspReliable = rtspReliable;
    }

    @XmlAttribute(name = "thumbnail")
    public String getThumbnail() {
        return thumbnail;
    }
    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    @XmlAttribute(name = "classification")
    public String getClassification() {
        return classification;
    }
    public void setClassification(String classification) {
        this.classification = classification;
    }

    @XmlAttribute(name = "latitude")
    public String getLatitude() {
        return latitude;
    }
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    @XmlAttribute(name = "longitude")
    public String getLongitude() {
        return longitude;
    }
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    @XmlAttribute(name = "fov")
    public String getFov() {
        return fov;
    }
    public void setFov(String fov) {
        this.fov = fov;
    }

    @XmlAttribute(name = "heading")
    public String getHeading() {
        return heading;
    }
    public void setHeading(String heading) {
        this.heading = heading;
    }

    @XmlAttribute(name = "range")
    public String getRange() {
        return range;
    }
    public void setRange(String range) {
        this.range = range;
    }

    @XmlAttribute(name = "width")
    public Integer getWidth() {
        return width;
    }
    public void setWidth(Integer width) {
        this.width = width;
    }

    @XmlAttribute(name = "height")
    public Integer getHeight() {
        return height;
    }
    public void setHeight(Integer height) {
        this.height = height;
    }

    @XmlAttribute(name = "bitrate")
    public Integer getBitrate() {
        return bitrate;
    }
    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }
}