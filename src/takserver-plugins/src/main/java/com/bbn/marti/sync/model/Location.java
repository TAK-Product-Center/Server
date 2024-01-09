package com.bbn.marti.sync.model;

import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "Location")
public class Location implements Serializable {

    private static final long serialVersionUID = -2340896168483402682L;

    public Location() { }

    public Location(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    @XmlAttribute(name = "lat")
    public Double lat;

    @XmlAttribute(name = "lon")
    public Double lon;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((lat == null) ? 0 : lat.hashCode());
        result = prime * result + ((lon == null) ? 0 : lon.hashCode());
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
        Location other = (Location) obj;
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

        return true;
    }

    @Override
    public String toString() {
        return "UidDetails [lat=" + lat + ", lon=" + lon + "]";
    }
}
