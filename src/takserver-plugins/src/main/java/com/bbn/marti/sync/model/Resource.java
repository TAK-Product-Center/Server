package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.SimpleTimeZone;

import org.hibernate.annotations.Formula;
//import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Point;

import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.Metadata.Field;
import com.bbn.marti.util.xml.DateAdapter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import tak.server.Constants;

/*
 * 
 * Value class representing enterprise sync / mission resource record
 * 
 * The following database columns are not currently used in this class:
 * 
 * permissions
 * remarks
 * 
 */
//don't include null fields in JSON
@JsonInclude(Include.NON_NULL) 
@Entity
@Table(name = "resource")
@Cacheable
@XmlRootElement(name = "Resource")
public class Resource implements Serializable, Comparable<Resource> {
    
    private static final long serialVersionUID = 19798723425L;

    protected Integer id = 0;
    protected String filename = "";
    protected List<String> keywords = new ArrayList<>();
    protected String mimeType = "";
    protected String contentType;
    protected String name = "";
    protected Date submissionTime;
    protected String submitter = ""; // SubmissionUser
    protected String uid = "";
    protected String creatorUid = "";
    protected String hash = "";
    protected Long size = 0L;
    protected String tool;
    protected String groupVector;
    protected NavigableSet<String> groups;
    protected Long expiration;

    // location is populated via hibernate when Resource is returned via jpa
    protected Point location;

    // lat/long values are populated manually when Resource is instantiated from metadata
    protected Double latitude;
    protected Double longitude;

    protected Double altitude;

    public Resource() { }
    
    public Resource(String hash) {
        this.hash = hash;
    }
    
    public Resource(String hash, 
    				String name,
    				String user,
    				Long size,
    				String groupVector,
    				Date time,
    				String mimeType,
    				Long expiration) {
        this.hash = hash;
        this.name = name;
        this.submitter = user;
        this.size = size;
        this.groupVector = groupVector;
        this.submissionTime = time;
        this.mimeType = mimeType;
        this.expiration = expiration;
    }
    
    // Adapt Metadata object to Resource object
    public Resource(@NotNull Metadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("null metadata object");
        }
        
        setFilename(metadata.getFirst(Metadata.Field.DownloadPath) != null ? metadata.getFirst(Metadata.Field.DownloadPath) : metadata.getFirst(Metadata.Field.Name));
        
        if (metadata.getKeywords() != null) {
            getKeywords().addAll(Lists.newArrayList(metadata.getKeywords()));
        }

        setMimeType(metadata.getFirst(Metadata.Field.MIMEType));
        
        setName(metadata.getFirst(Metadata.Field.Name));
        
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
        
        sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
        
        try {
        	
            String submissionTime = metadata.getFirst(Metadata.Field.SubmissionDateTime);
            
            if (!Strings.isNullOrEmpty(submissionTime)) {
        	
            	setSubmissionTime(sdf.parse(metadata.getFirst(Metadata.Field.SubmissionDateTime)));
            } else {
            	setSubmissionTime(new Date());
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        
        setUid(metadata.getUid());
        
        setHash(metadata.getHash());
        
        if (metadata.getSize() != null) {
        	setSize(Long.valueOf(metadata.getSize()));
        }
        
        setSubmitter(metadata.getFirst(Field.SubmissionUser));
        
        setCreatorUid(metadata.getFirst(Field.CreatorUid));

        setTool(metadata.getFirst(Field.Tool));

        setLatitude(metadata.getLatitude());

        setLongitude(metadata.getLongitude());

        setAltitude(metadata.getAltitude());
    }

    @JsonIgnore
    @XmlTransient
    // database primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    // Note - this column will have to be handled manually, since it's a Postgres array
    @Transient
    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @Column(name = "mimetype")
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Transient
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Column
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlJavaTypeAdapter(DateAdapter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT)
    @Column(name = "submissiontime")
    public Date getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(Date submissionTime) {
        this.submissionTime = submissionTime;
    }

    @Column
    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    @Column
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Column
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
    
    @Formula("octet_length(data)")
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
    
    @Column
    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    @Column
    public String getTool() { return tool; }

    public void setTool(String tool) {
        this.tool = tool;
    }

    @Transient
    public Double getLatitude() {
        if (getLocation() == null) {
            return latitude;
        }

        return getLocation().getY();
    }

    public void setLatitude(Double latitude) {
        if (latitude != null && !Double.isNaN(latitude)) {
            this.latitude = latitude;
        }
    }

    @Transient
    public Double getLongitude() {
        if (getLocation() == null) {
            return longitude;
        }

        return getLocation().getX();
    }

    public void setLongitude(Double longitude) {
        if (longitude != null && !Double.isNaN(longitude)) {
            this.longitude = longitude;
        }
    }

    //@Type(type = "org.locationtech.jts.geom.Point")
    //@Convert(converter=Point.class)
    @Column(columnDefinition="Point", nullable = true)
    Point getLocation() { return location; }

    public void setLocation(Point location) { this.location = location; }

    @Column
    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        if (altitude != null && !Double.isNaN(altitude)) {
            this.altitude = altitude;
        }
    }
    
    @Column(name = "groups", columnDefinition = "bit varying")
    @JsonIgnore
    public String getGroupVector() {
        return groupVector;
    }

    public void setGroupVector(String groupVector) {
        this.groupVector = groupVector;
    }
    
    @Column(name = "expiration", unique = false, nullable = true)
    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }
    
    @Transient
    public NavigableSet<String> getGroups() { return groups; }

    public void setGroups(NavigableSet<String> groups) { this.groups = groups; }

    @Override
    public String toString() {
        return "Resource [id=" + id + ", filename=" + filename + ", keywords="
                + keywords + ", mimeType=" + mimeType + ", name=" + name
                + ", submissionTime=" + submissionTime + ", submitter="
                + submitter + ", uid=" + uid + ", hash=" + hash + ", creatorUid=" + creatorUid + ", size="
                + size + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hash == null) ? 0 : hash.hashCode());
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
        Resource other = (Resource) obj;
        if (hash == null) {
            if (other.hash != null)
                return false;
        } else if (!hash.equals(other.hash))
            return false;
        return true;
    }
    
    /* Return Metadata view of this Resource object
     * 
     */
    public Metadata toMetadata() {
    	
    		Metadata metadata = new Metadata();

    		metadata.set(Metadata.Field.UID, getUid());
    		metadata.set(Metadata.Field.DownloadPath, getFilename());
    		metadata.set(Metadata.Field.Name, getName());
    		metadata.set(Metadata.Field.Keywords, getKeywords().toArray(new String[0]));
    		metadata.set(Metadata.Field.MIMEType, this.getMimeType());

    		SimpleDateFormat sdf = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
            sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
            
            metadata.set(Metadata.Field.SubmissionDateTime, sdf.format(getSubmissionTime()));
            metadata.set(Metadata.Field.SubmissionUser, getSubmitter());
            metadata.set(Metadata.Field.CreatorUid, getCreatorUid());
            metadata.set(Metadata.Field.Hash, getHash());
            metadata.set(Metadata.Field.Size, getSize().toString());
            metadata.set(Metadata.Field.Tool, getTool());

            metadata.set(Field.Latitude, getLatitude());
            metadata.set(Field.Longitude, getLongitude());
            metadata.set(Field.Altitude, getAltitude());

            return metadata;
    }

    // compare based on hash
    @Override
    public int compareTo(Resource that) {
        return ComparisonChain.start().compare(this.getHash(), that.getHash()).result();
    }
    
}
