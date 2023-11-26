package com.bbn.marti.sync.model;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Entity
@Table(name = "mission_feed")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionFeed implements Serializable, Comparable<MissionFeed> {

	private static final long serialVersionUID = 6360858783716327399L;

	protected static final Logger logger = LoggerFactory.getLogger(MissionFeed.class);

    protected String uid;
    protected String dataFeedUid;
    protected String filterPolygon;
    protected String filterCotTypesSerialized;
    protected String filterCallsign;
    protected Mission mission;
    @Transient protected String name;

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

    @Transient
    @JsonProperty("name")
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "filter_polygon", nullable = false, columnDefinition = "TEXT")
    public String getFilterPolygon() {
		return filterPolygon;
	}
	public void setFilterPolygon(String filterPolygon) {
		this.filterPolygon = filterPolygon;
	}
    
    @Column(name = "filter_cot_types", nullable = false, columnDefinition = "TEXT")
    public String getFilterCotTypesSerialized() {
        return filterCotTypesSerialized;
    }
	public void setFilterCotTypesSerialized(String filterCotTypesSerialized) {
        this.filterCotTypesSerialized = filterCotTypesSerialized;
    }
    
    @JsonIgnore
    @XmlTransient
    @Transient
    public List<String> getFilterCotTypes() {

        if (Strings.isNullOrEmpty(filterCotTypesSerialized)) {
            return new ArrayList<>();
        }

    	try {
			ObjectMapper mapper = new ObjectMapper();
			List<String> deserialized = Arrays.asList(mapper.readValue(this.filterCotTypesSerialized, String[].class));
			return deserialized;
		} catch (Exception e) {
			logger.error("Error parsing filterCotTypesSerialized for MissionFeed uid {}", uid ,e);
			return null; 
		}
    }
    public void setFilterCotTypes(List<String> filterCotTypes) {
        if (filterCotTypes == null) {
            return;
        }

    	Collections.sort(filterCotTypes);
    	String serialized = JSONArray.toJSONString(filterCotTypes);
    	this.setFilterCotTypesSerialized(serialized);
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
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="mission_id")
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }

    @Override
    public int compareTo(MissionFeed that) {
        return ComparisonChain.start()
                .compare(this.uid, that.uid)
                .compare(this.dataFeedUid, that.dataFeedUid)
                .compare(this.filterPolygon, that.filterPolygon)
                .compare(this.filterCotTypesSerialized, that.filterCotTypesSerialized)
                .compare(this.filterCallsign, that.filterCallsign)
                .result();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dataFeedUid == null) ? 0 : dataFeedUid.hashCode());
        result = prime * result + ((filterPolygon == null) ? 0 : filterPolygon.hashCode());
        result = prime * result + ((filterCotTypesSerialized == null) ? 0 : filterCotTypesSerialized.hashCode());
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

        if (dataFeedUid == null) {
            if (other.dataFeedUid != null)
                return false;
        } else if (!dataFeedUid.equals(other.dataFeedUid))
            return false;

        if (filterPolygon == null) {
            if (other.filterPolygon != null)
                return false;
        } else if (!filterPolygon.equals(other.filterPolygon))
            return false;
        
        if (filterCotTypesSerialized == null) {
            if (other.filterCotTypesSerialized != null)
                return false;
        } else if (!filterCotTypesSerialized.equals(other.filterCotTypesSerialized))
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

        builder.append(", filterPolygon=");
        builder.append(filterPolygon);
        
        builder.append(", filterCotTypesSerialized=");
        builder.append(filterCotTypesSerialized);

        builder.append(", filterCallsign=");
        builder.append(filterCallsign);

        builder.append("]");
        return builder.toString();
    }
}