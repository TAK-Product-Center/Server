package com.bbn.marti.video;


import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity
@Table(name = "video_connections_v2")
@Cacheable
@XmlRootElement(name = "videoCollection")
public class VideoConnection implements Serializable, Comparable<VideoConnection>  {

    public VideoConnection() {
        this.feeds = new ArrayList<FeedV2>();
    }

    private Long id;
    private String uid;
    private boolean active;
    private String alias;
    private String thumbnail;
    private String classification;
    private String xml;
    private String groupVector;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @XmlTransient
    @JsonIgnore
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }


    @Column(name = "uid", unique = false, nullable = false, columnDefinition="VARCHAR")
    @XmlAttribute(name = "uid")
    public String getUuid() {
        return uid;
    }
    public void setUuid(String uuid) {
        this.uid = uuid;
    }


    @Column(name = "active", unique = false, nullable = false, columnDefinition="boolean")
    @XmlAttribute(name = "active")
    public boolean getActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }


    @Column(name = "alias", unique = false, nullable = false, columnDefinition="VARCHAR")
    @XmlAttribute(name = "alias")
    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }


    @Column(name = "thumbnail", unique = false, nullable = false, columnDefinition="VARCHAR")
    @XmlAttribute(name = "thumbnail")
    public String getThumbnail() {
        return thumbnail;
    }
    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }


    @Column(name = "classification", unique = false, nullable = false, columnDefinition="VARCHAR")
    @XmlAttribute(name = "classification")
    public String getClassification() {
        return classification;
    }
    public void setClassification(String classification) {
        this.classification = classification;
    }

    @Column(name = "xml", columnDefinition = "VARCHAR")
    @XmlTransient
    @JsonIgnore
    public String getXml() {
        return xml;
    }
    public void setXml(String xml) {
        this.xml = xml;
    }

    @Column(name = "groups", columnDefinition = "bit varying")
    @XmlTransient
    @JsonIgnore
    public String getGroupVector() {
        return groupVector;
    }
    public void setGroupVector(String groupVector) {
        this.groupVector = groupVector;
    }


    private List<FeedV2> feeds = null;
    @Transient
    @XmlElement(name = "feed")
    public List<FeedV2> getFeeds() {
        return feeds;
    }


    @Override
    public int compareTo(VideoConnection videoConnection) {
        return 0;
    }
}