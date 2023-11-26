

package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.sync.MissionChangeType;
import com.bbn.marti.util.xml.DateAdapter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

import tak.server.Constants;

@Entity
@Table(name = "mission_change")
@Cacheable
@JsonInclude(Include.NON_NULL)
@XmlRootElement(name = "MissionChange")
public class MissionChange implements Serializable, Comparable<MissionChange> {

    private static final long serialVersionUID = 1987123948726L;
    
    /**
     * 
     * 
     * Model class representing changes to the contents of a mission.
     * 
     */

    protected static final Logger logger = LoggerFactory.getLogger(MissionChange.class);
    
    protected Long id;
    protected boolean isFederatedChange;
    protected MissionChangeType type;
    protected String contentHash;
    protected String contentUid;

    protected String externalDataUid;
    protected String externalDataName;
    protected String externalDataTool;
    protected String externalDataToken;
    protected String externalDataNotes;
    protected String missionFeedUid;
    protected String mapLayerUid;

    protected Mission mission;
    protected String missionName; // mission name is denormalized here to track all event by mission name, even deletions (because if the mission id foreign key was joined on, the mission name would be lost when the mission record is deleted.
                                  // Mission change records will not be deleted at the application layer

    protected Date timestamp;
    protected Date servertime;
    protected String creatorUid;

    @Transient private UidDetails uidDetails = null;
    private ExternalMissionData externalMissionData = null;

    private Resource tempResource = null;
    private LogEntry tempLogEntry = null;
    private ExternalMissionData tempExternalMissionData = null;

    @Transient protected MapLayer mapLayer = null;
    @Transient protected MissionFeed missionFeed = null;
    @Transient protected Resource contentResource = null;
    
    // no-arg constructor
    public MissionChange() {
    	this.timestamp = new Date();
        this.servertime = new Date();
    }
    
    public MissionChange(@NotNull MissionChangeType type, @NotNull Mission mission, String hash, String uid) {
        this(type, mission);
        
        if (Strings.isNullOrEmpty(hash) && Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException("either hash or uid must be provided");
        }
        
        if (!(Strings.isNullOrEmpty(hash) || Strings.isNullOrEmpty(uid))) {
            throw new IllegalArgumentException("A MissionChange which specifies both uid and hash is invalid");
        }
        
        setContentHash(hash);
        setContentUid(uid);
    }
    
    public MissionChange(@NotNull MissionChangeType type, Mission mission) {
        this();
        
        this.type = type;
        
        this.mission = mission;
        
        if (mission != null) {
            this.missionName = mission.getName();
        }
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @JsonIgnore
    @XmlTransient
    public Long getId() {
        return id;
    }   

    public void setId(Long id) {
        this.id = id;
    }
    
    @Column(name = "change_type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    public MissionChangeType getType() {
        return type;
    }
    
    public void setType(MissionChangeType type) {
        this.type = type;
    }

    @JsonIgnore
    @XmlTransient
    @Column(name = "hash")
    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    @XmlJavaTypeAdapter(DateAdapter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT_PAD_MILLIS)
    @Column(name = "ts", nullable = false)
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @XmlTransient
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT_PAD_MILLIS)
    @Column(name = "servertime", nullable = false)
    public Date getServerTime() {
        return servertime;
    }

    public void setServerTime(Date servertime) {
        this.servertime = servertime;
    }

    @JsonIgnore
    @XmlTransient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    public Mission getMission() {
        return mission;
    }

    public void setMission(Mission mission) {
        this.mission = mission;
    }
    
    @Column(name = "mission_name")
    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }
    
    @Column(name = "remote_federated_change")
    public boolean getIsFederatedChange() {
        return isFederatedChange;
    }

    public void setIsFederatedChange(boolean isFederatedChange) {
        this.isFederatedChange = isFederatedChange;
    }

    @Column(name = "uid")
    public String getContentUid() {
        return contentUid;
    }

    public void setContentUid(String contentUid) {
        this.contentUid = contentUid;
    }

//    @Transient
//    @JsonProperty("details")
//    @XmlElement(name = "details")
//    public UidDetails getUidDetails() {
//        if (contentUid != null && timestamp != null)   {
//            try {
//                if (uidDetails == null) {
//                    uidDetails = new UidDetails();
//                    missionService.hydrate(uidDetails, contentUid, servertime);
//                }
//                return uidDetails;
//            } catch (Exception e) {
//                logger.debug("Exception in getUidDetails!", e);
//            }
//        }
//        return null;
//    }
    
    @Transient
    @JsonProperty("details")
    @XmlElement(name = "details")
	public UidDetails getUidDetails() {
		return this.uidDetails;
	}

	public void setUidDetails(UidDetails uidDetails) {
		this.uidDetails = uidDetails;
	}

//    @Transient // ignore for JPA
//    @JsonProperty("contentResource")
//    public Resource getContentResource() {
//
//        Resource resource = null;
//
//        if (contentHash != null) {
//
//            try {
//                resource = ResourceUtils.fetchResourceByHash(contentHash);
//               
//            } catch (Exception e) {
//                logger.debug("exception fetching resource by hash " + e.getMessage(), e);
//            }
//            
//            // in case the resource has been deleted, just capture the hash
//            if (resource == null) {
//                resource = new Resource();
//                resource.setHash(contentHash);
//                resource.setCreatorUid(null);
//                resource.setFilename(null);
//                resource.setId(null);
//                resource.setKeywords(null);
//                resource.setMimeType(null);
//                resource.setName(null);
//                resource.setSubmissionTime(null);
//                resource.setSize(null);
//                resource.setSubmitter(null);
//                resource.setUid(null);
//            }
//        }
//
//        return resource;
//
//    }
    
    @Transient // ignore for JPA
    @JsonProperty("contentResource")
    public Resource getContentResource() {
    	return this.contentResource;
    }

    public void setContentResource(Resource contentResource) {
    	this.contentResource = contentResource;
        setTempResource(contentResource);
    }

    public void setTempResource(Resource contentResource) {
        tempResource = contentResource;
    }

    @Transient
    @JsonIgnore
    @XmlTransient
    public Resource getTempResource() {
        return tempResource;
    }

    @Transient
    @JsonProperty("logEntry")
    public LogEntry getTempLogEntry() {
        return tempLogEntry;
    }

    public void setTempLogEntry(LogEntry tempLogEntry) {
        this.tempLogEntry = tempLogEntry;
    }

    @Column
    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    @Override
	public String toString() {
		return "MissionChange [id=" + id + ", type=" + type + ", contentHash=" + contentHash + ", contentUid="
				+ contentUid + ", externalDataUid=" + externalDataUid + ", externalDataName=" + externalDataName
				+ ", externalDataTool=" + externalDataTool + ", externalDataToken=" + externalDataToken
				+ ", externalDataNotes=" + externalDataNotes + ", mission=" + mission + ", missionName=" + missionName
				+ ", timestamp=" + timestamp + ", servertime=" + servertime + ", creatorUid=" + creatorUid
				+ ", uidDetails=" + uidDetails + ", externalMissionData=" + externalMissionData + ", tempResource="
				+ tempResource + ", tempLogEntry=" + tempLogEntry + "]";
	}

    @Override
    public int compareTo(MissionChange that) {
        return ComparisonChain
                .start()
                .compare(that.getId(), this.getId())
                .result();
    }

    @JsonIgnore
    @XmlTransient
    @Column(name = "external_data_uid")
    public String getExternalDataUid() {
        return externalDataUid;
    }

    public void setExternalDataUid(String externalDataUid) {
        this.externalDataUid = externalDataUid;
    }

    @JsonIgnore
    @XmlTransient
    @Column(name = "external_data_name")
    public String getExternalDataName() {
        return externalDataName;
    }

    public void setExternalDataName(String externalDataName) {
        this.externalDataName = externalDataName;
    }

    @JsonIgnore
    @XmlTransient
    @Column(name = "external_data_tool")
    public String getExternalDataTool() {
        return externalDataTool;
    }

    public void setExternalDataTool(String externalDataTool) {
        this.externalDataTool = externalDataTool;
    }

    @JsonIgnore
    @XmlTransient
    @Column(name = "external_data_token")
    public String getExternalDataToken() {
        return externalDataToken;
    }

    public void setExternalDataToken(String externalDataToken) {
        this.externalDataToken = externalDataToken;
    }

    @JsonIgnore
    @XmlTransient
    @Column(name = "external_data_notes")
    public String getExternalDataNotes() {
        return externalDataNotes;
    }

    public void setExternalDataNotes(String externalDataNotes) {
        this.externalDataNotes = externalDataNotes;
    }

    @JsonIgnore
    @XmlTransient
    @Column(name = "mission_feed_uid")
    public String getMissionFeedUid() {
        return missionFeedUid;
    }

    public void setMissionFeedUid(String missionFeedUid) {
        this.missionFeedUid = missionFeedUid;
    }

//    @Transient
//    @JsonProperty("missionFeed")
//    @XmlElement(name = "missionFeed")
//    public MissionFeed getMissionFeed() {
//        if (missionFeedUid != null) {
//            MissionFeed missionFeed =  missionService.getMissionFeed(missionFeedUid);
//            if (missionFeed == null) {
//                missionFeed = new MissionFeed();
//                missionFeed.setUid(missionFeedUid);
//            }
//
//            return missionFeed;
//        }
//
//        return null;
//    }

	@Transient
	@JsonProperty("missionFeed")
	@XmlElement(name = "missionFeed")
	public MissionFeed getMissionFeed() {
		return missionFeed;
	}

	public void setMissionFeed(MissionFeed missionFeed) {
		this.missionFeed = missionFeed;
	}
    

    @JsonIgnore
    @XmlTransient
    @Column(name = "map_layer_uid")
    public String getMapLayerUid() {
        return mapLayerUid;
    }

	public void setMapLayerUid(String mapLayerUid) {
        this.mapLayerUid = mapLayerUid;
    }


//    @Transient
//    @JsonProperty("mapLayer")
//    @XmlElement(name = "mapLayer")
//    public MapLayer getMapLayer() {
//        try {
//            if (mapLayerUid != null) {
//                MapLayer mapLayer = missionService.getMapLayer(mapLayerUid);
//                if (mapLayer == null) {
//                    mapLayer = new MapLayer();
//                    mapLayer.setUid(mapLayerUid);
//                }
//
//                return mapLayer;
//            }
//        } catch (Exception e) {
//            logger.error("exception in getMapLayer", e);
//        }
//
//        return null;
//    }

	@Transient
	@JsonProperty("mapLayer")
	@XmlElement(name = "mapLayer")
	public MapLayer getMapLayer() {
		return mapLayer;
	}

	public void setMapLayer(MapLayer mapLayer) {
		this.mapLayer = mapLayer;
	}	
	
    public void setTempExternalData(ExternalMissionData externalTempMissionData) {
        this.tempExternalMissionData = externalTempMissionData;
    }

	@Transient
    @JsonIgnore
    @XmlTransient
    public ExternalMissionData getTempExternalData() {
        return tempExternalMissionData;
    }

//    @Transient
//    @JsonProperty("externalData")
//    @XmlElement(name = "externalData")
//    public ExternalMissionData getExternalData() {
//
//        if (externalDataUid != null && externalDataName != null
//                && externalDataTool != null && externalDataNotes != null)   {
//            if (externalMissionData == null) {
//                externalMissionData = missionService.hydrate(
//                        externalDataUid, externalDataName, externalDataTool, externalDataToken, externalDataNotes);
//            }
//            return  externalMissionData;
//        }
//        return null;
//    }
  
	@Transient
	@JsonProperty("externalData")
	@XmlElement(name = "externalData")
	public ExternalMissionData getExternalMissionData() {
		return this.externalMissionData;
	}

	public void setExternalMissionData(ExternalMissionData externalMissionData) {
		this.externalMissionData = externalMissionData;
	}
	
}