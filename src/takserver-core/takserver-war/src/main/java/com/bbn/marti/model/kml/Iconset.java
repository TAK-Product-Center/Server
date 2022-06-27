

package com.bbn.marti.model.kml;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "iconset")
@Cacheable
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "iconset")
public class Iconset implements Serializable {

    private static final long serialVersionUID = -2811748768123764250L;
    
    protected static final Logger logger = LoggerFactory.getLogger(Iconset.class);
    
    protected Long id;
    protected String name;
    protected String uid;
    protected String defaultFriendly;
    protected String defaultHostile;
    protected String defaultUnknown;
    protected Boolean skipResize;
    protected Integer version;
    protected List<Icon> icons;
    protected Date createTimestamp;
    
    // no-arg constructor
    public Iconset() {
        createTimestamp = new Date();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "name", unique = false, nullable = true)
    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column (unique = true, nullable = false)
    @XmlAttribute
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Column
    @XmlAttribute
    public String getDefaultFriendly() {
        return defaultFriendly;
    }

    public void setDefaultFriendly(String defaultFriendly) {
        this.defaultFriendly = defaultFriendly;
    }

    @Column
    @XmlAttribute
    public String getDefaultHostile() {
        return defaultHostile;
    }

    public void setDefaultHostile(String defaultHostile) {
        this.defaultHostile = defaultHostile;
    }

    @Column
    @XmlAttribute
    public String getDefaultUnknown() {
        return defaultUnknown;
    }

    public void setDefaultUnknown(String defaultUnknown) {
        this.defaultUnknown = defaultUnknown;
    }

    @Column
    @XmlAttribute
    public Boolean getSkipResize() {
        return skipResize;
    }

    public void setSkipResize(Boolean skipResize) {
        this.skipResize = skipResize;
    }

    @Column
    @XmlAttribute
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "iconset")
    @XmlElement(name = "icon")
    public List<Icon> getIcons() {
        return icons;
    }

    public void setIcons(List<Icon> icons) {
        this.icons = icons;
    }
    
    @Column(name = "created")
    @XmlElement
    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    @Override
    public String toString() {
        return "Iconset [id=" + id + ", name=" + name + ", uid=" + uid
                + ", defaultFriendly=" + defaultFriendly + ", defaultHostile="
                + defaultHostile + ", defaultUnknown=" + defaultUnknown
                + ", skipResize=" + skipResize + ", version=" + version
                + ", icons=" + icons + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((defaultFriendly == null) ? 0 : defaultFriendly.hashCode());
        result = prime * result
                + ((defaultHostile == null) ? 0 : defaultHostile.hashCode());
        result = prime * result
                + ((defaultUnknown == null) ? 0 : defaultUnknown.hashCode());
        result = prime * result + ((icons == null) ? 0 : icons.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((skipResize == null) ? 0 : skipResize.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        Iconset other = (Iconset) obj;
        if (defaultFriendly == null) {
            if (other.defaultFriendly != null)
                return false;
        } else if (!defaultFriendly.equals(other.defaultFriendly))
            return false;
        if (defaultHostile == null) {
            if (other.defaultHostile != null)
                return false;
        } else if (!defaultHostile.equals(other.defaultHostile))
            return false;
        if (defaultUnknown == null) {
            if (other.defaultUnknown != null)
                return false;
        } else if (!defaultUnknown.equals(other.defaultUnknown))
            return false;
        if (icons == null) {
            if (other.icons != null)
                return false;
        } else if (!icons.equals(other.icons))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (skipResize == null) {
            if (other.skipResize != null)
                return false;
        } else if (!skipResize.equals(other.skipResize))
            return false;
        if (uid == null) {
            if (other.uid != null)
                return false;
        } else if (!uid.equals(other.uid))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }
}



