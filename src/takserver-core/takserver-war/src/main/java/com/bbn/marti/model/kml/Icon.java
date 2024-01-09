

package com.bbn.marti.model.kml;

import java.io.Serializable;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Entity
@Table(name = "icon")
@Cacheable
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Icon implements Serializable {

    private static final long serialVersionUID = -3245749504867358250L;

    private static final Logger logger = LoggerFactory.getLogger(Icon.class);

    protected Long id; // db primary key
    protected String name;
    protected String group;
    protected String type2525b;
    protected byte[] bytes;
    protected String iconsetUid;
    protected Iconset iconset;
    protected String mimeType;
    protected Date createdTimestamp;

    // no-arg constructor
    public Icon() {
        createdTimestamp = new Date();
    }
    
    public Icon(byte[] bytes, String type2525b, String name) {
        if (bytes == null || bytes.length == 0 || Strings.isNullOrEmpty(type2525b) || Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("invalid constructor args: data length: " + (bytes == null ? "null" : bytes.length) + " type2525b: " + type2525b + " name: " + name);
        }
        
        this.bytes = bytes;
        this.type2525b = type2525b;
        this.name = name;
        this.group = "";
        this.createdTimestamp = new Date();
        
        // determine content type from file extension
        String mimeType = URLConnection.guessContentTypeFromName(name);
        
        logger.trace("mimeType: " + mimeType);
        
        setMimeType(mimeType);
    }

    // not present in XML representation
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    @XmlTransient
    public Long getId() {
        return id;
    }

    @XmlAttribute
    @Column
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // not present in XML representation
    @Column(name = "group_name")
    @XmlTransient
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @XmlAttribute(name = "type2525b")
    public String getType2525b() {
        return type2525b;
    }

    public void setType2525b(String type2525b) {
        this.type2525b = type2525b;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // not present in XML representation
    @XmlTransient
    @Column
    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @XmlTransient
    public String getIconsetUid() {
        return iconsetUid;
    }

    public void setIconsetUid(String iconsetUid) {
        this.iconsetUid = iconsetUid;
    }

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public Iconset getIconset() {
        return iconset;
    }

    public void setIconset(Iconset iconset) {
        this.iconset = iconset;
    }
    
    // not present in XML representation
    @XmlTransient
    @Column
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Column(name = "created")
    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        return "Icon [id=" + id + ", name=" + name + " group=" + group + ", type2525b=" + type2525b + ", data " + (bytes == null ? 0 : bytes.length) + "bytes, iconset uid: " + iconsetUid + " mime type: " + mimeType + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result
                + ((iconsetUid == null) ? 0 : iconsetUid.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((type2525b == null) ? 0 : type2525b.hashCode());
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
        Icon other = (Icon) obj;
        if (group == null) {
            if (other.group != null)
                return false;
        } else if (!group.equals(other.group))
            return false;
        if (iconsetUid == null) {
            if (other.iconsetUid != null)
                return false;
        } else if (!iconsetUid.equals(other.iconsetUid))
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
        if (type2525b == null) {
            if (other.type2525b != null)
                return false;
        } else if (!type2525b.equals(other.type2525b))
            return false;
        return true;
    }   

    /*
     * parse the group and name from the name field
     * 
     */
    public void parseNameAndGroup() {
        // if the group field is already assigned, do nothing
        if (Strings.isNullOrEmpty(getGroup())) {
            if (!Strings.isNullOrEmpty(getName())) {
                List<String> nameParts = Lists.newArrayList(Splitter.on('/').split(getName()));

                if (nameParts.size() == 2) {
                    setGroup(nameParts.get(0));
                    setName(nameParts.get(1));

                    logger.trace("icon group: " + getGroup() + " name: " + getName());
                } else {
                    setGroup("");
                    logger.warn("unexpected format for icon name: " + this);
                }
            }
        }
    }
    
    public static class IconParts {
        public String iconsetUid = "";
        public String group = "";
        public String name = "";
    }

    public static IconParts parseIconPath(String iconsetPath) {
        logger.trace("iconsetpath found, parsing: " + iconsetPath);
        
        if (Strings.isNullOrEmpty(iconsetPath)) {
            throw new IllegalArgumentException("empty iconsetPath");
        }
    
        IconParts iconParts = new IconParts();
        
        List<String> iconSetPathParts = Lists.newArrayList(Splitter.on('/').split(iconsetPath));
    
        if (iconSetPathParts.size() > 3) {
            throw new IllegalArgumentException("invalid iconsetpath - too many parts: " + iconsetPath);
        }
    
        if (iconSetPathParts.size() > 1) {
            iconParts.iconsetUid = iconSetPathParts.get(0);
            
            if (Strings.isNullOrEmpty(iconParts.iconsetUid)) {
                throw new IllegalStateException("empty iconset uid");
            }
            
            // if there are only two parts, the first is the uid, the second is the name. Otherwise, the following format is expected: {iconsetUid}/{group}/{name}
            if (iconSetPathParts.size() == 2) {
                iconParts.name = iconSetPathParts.get(1);
                if (Strings.isNullOrEmpty(iconParts.name)) {
                    throw new IllegalStateException("empty icon name");
                }
            } else {
                iconParts.group = iconSetPathParts.get(1);
                iconParts.name = iconSetPathParts.get(2);
    
                if (Strings.isNullOrEmpty(iconParts.name)) {
                    throw new IllegalStateException("empty icon name");
                }
            }
    
        } else {
            throw new IllegalArgumentException("invalid iconsetpath: " + iconsetPath);
        }
    
        logger.debug("iconsetUid: " + iconParts.iconsetUid + " icon group: " + iconParts.group + " icon name: " + iconParts.name);
        
        return iconParts;
    }
}
