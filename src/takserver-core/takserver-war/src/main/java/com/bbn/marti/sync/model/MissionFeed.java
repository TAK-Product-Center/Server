package com.bbn.marti.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

@Entity
@Table(name = "mission_feed")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionFeed implements Serializable, Comparable<MissionFeed> {

    protected static final Logger logger = LoggerFactory.getLogger(MissionFeed.class);

    protected String uid;
    protected String dataFeedUid;
    protected String filterBbox;
    protected String filterType;
    protected String filterCallsign;
    protected Mission mission;

    public MissionFeed() {

    }

    @Id
    @Column(name = "uid", unique = true, nullable = false)
    @JsonProperty("uid")
    @XmlElement(name = "uid")
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    @Column(name = "data_feed_uid", nullable = false, columnDefinition = "TEXT")
    public String getDataFeedUid() {
        return dataFeedUid;
    }
    public void setDataFeedUid(String dataFeedUid) {
        this.dataFeedUid = dataFeedUid;
    }

    @Column(name = "filter_bbox", nullable = false, columnDefinition = "TEXT")
    public String getFilterBbox() {
        return filterBbox;
    }
    public void setFilterBbox(String filterBbox) {
        this.filterBbox = filterBbox;
    }

    @Column(name = "filter_type", nullable = false, columnDefinition = "TEXT")
    public String getFilterType() {
        return filterType;
    }
    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    @Column(name = "filter_callsign", nullable = false, columnDefinition = "TEXT")
    public String getFilterCallsign() {
        return filterCallsign;
    }
    public void setFilterCallsign(String filterCallsign) {
        this.filterCallsign = filterCallsign;
    }

    @JsonIgnore
    @XmlTransient
    @ManyToOne
    @JoinColumn(name="mission_id")
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }


    @Override
    public int compareTo(MissionFeed that) {
        return ComparisonChain.start()
                .compare(this.uid, that.uid)
                .compare(this.dataFeedUid, that.dataFeedUid)
                .compare(this.filterBbox, that.filterBbox)
                .compare(this.filterType, that.filterType)
                .compare(this.filterCallsign, that.filterCallsign)
                .result();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        result = prime * result + ((dataFeedUid == null) ? 0 : dataFeedUid.hashCode());
        result = prime * result + ((filterBbox == null) ? 0 : filterBbox.hashCode());
        result = prime * result + ((filterType == null) ? 0 : filterType.hashCode());
        result = prime * result + ((filterCallsign == null) ? 0 : filterCallsign.hashCode());
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
        MissionFeed other = (MissionFeed) obj;

        if (uid == null) {
            if (other.uid != null)
                return false;
        } else if (!uid.equals(other.uid))
            return false;

        if (dataFeedUid == null) {
            if (other.dataFeedUid != null)
                return false;
        } else if (!dataFeedUid.equals(other.dataFeedUid))
            return false;

        if (filterBbox == null) {
            if (other.filterBbox != null)
                return false;
        } else if (!filterBbox.equals(other.filterBbox))
            return false;

        if (filterType == null) {
            if (other.filterType != null)
                return false;
        } else if (!filterType.equals(other.filterType))
            return false;

        if (filterCallsign == null) {
            if (other.filterCallsign != null)
                return false;
        } else if (!filterCallsign.equals(other.filterCallsign))
            return false;

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MissionFeed [uid=");
        builder.append(uid);

        builder.append(", dataFeedUid=");
        builder.append(dataFeedUid);

        builder.append(", filterBbox=");
        builder.append(filterBbox);

        builder.append(", filterType=");
        builder.append(filterType);

        builder.append(", filterCallsign=");
        builder.append(filterCallsign);

        builder.append("]");
        return builder.toString();
    }
}