package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

import com.bbn.marti.maplayer.model.MapLayer;

import tak.server.Constants;

/*
 * This class and the corresponding table don't have a lot of fields. They exist mainly for the purpose of providing a primary key,
 * and a single record for joining on the resources, uids and keywords.
 * 
 */
@Entity
@Table(name = "mission")
@Cacheable
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mission implements Serializable, Comparable<Mission> {

    /**
     * 
     * 
     * Model class representing a data sync mission.
     * 
     */
    private static final long serialVersionUID = 187567567687L;

    protected static final Logger logger = LoggerFactory.getLogger(Mission.class);
    
    // set of metadata objects for content items in this mission 
    protected Long id;
    protected String name;
    protected String description;
    protected String chatRoom;
    protected String baseLayer;
    protected String bbox;
    protected String boundingPolygon;
    protected String path;
    protected String classification;

    protected String tool;

    protected Set<Resource> contents;
    
    protected Set<String> uids;

    protected Set<String> keywords;

    protected String creatorUid;
    
    protected Date createTime;

    protected Date lastEdited;

    protected List<MissionAdd<String>> uidAdds;
    
    protected List<MissionAdd<Resource>> resourceAdds;
    
    protected String groupVector;

    protected NavigableSet<String> groups;

    protected Mission parent;

    protected Set<Mission> children = new ConcurrentSkipListSet<Mission>();

    protected Set<ExternalMissionData> externalData = new ConcurrentSkipListSet<ExternalMissionData>();

    protected Set<MissionFeed> feeds = new ConcurrentSkipListSet<>();

    protected Set<MapLayer> mapLayers = new ConcurrentSkipListSet<MapLayer>();

    protected Long pageCount;
    protected Long missionCount;
    protected Long pageSize;

    protected String passwordHash;
    protected MissionRole defaultRole;
    protected MissionRole ownerRole;
    protected String token;

    protected Set<MissionChange> missionChanges;
    protected List<LogEntry> logs;

    protected Long expiration;

    // no-arg constructor
    public Mission() {
        contents = new ConcurrentSkipListSet<>();
        keywords = new ConcurrentSkipListSet<>();
        uids = new ConcurrentSkipListSet<>();
        uidAdds = new ArrayList<>();
        resourceAdds = new ArrayList<>();
    }
    
    public Mission(@NotNull String name) {
        this();
        
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("empty name");
        }
        
        setName(name);
    }

    public Mission(Mission other, Date lastEdited) {
        id = other.id;
        name = other.name;
        description = other.description;
        chatRoom = other.chatRoom;
        baseLayer = other.baseLayer;
        bbox = other.bbox;
        boundingPolygon = other.boundingPolygon;
        path = other.path;
        classification = other.classification;
        tool = other.tool;
        contents = other.contents;
        uids = other.uids;
        keywords = other.keywords;
        creatorUid = other.creatorUid;
        createTime = other.createTime;
        uidAdds = other.uidAdds;
        resourceAdds = other.resourceAdds;
        groupVector = other.groupVector;
        groups = other.groups;
        parent = other.parent;
        children = other.children;
        externalData = other.externalData;
        feeds = other.feeds;
        mapLayers = other.mapLayers;
        pageCount = other.pageCount;
        missionCount = other.missionCount;
        pageSize = other.pageSize;
        passwordHash = other.passwordHash;
        defaultRole = other.defaultRole;
        ownerRole = other.ownerRole;
        token = other.token;
        missionChanges = other.missionChanges;
        logs = other.logs;
        expiration = other.expiration;

        this.lastEdited = lastEdited;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @JsonIgnore
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "name", unique = true, nullable = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "description", unique = false, nullable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "chatroom", unique = false, nullable = true)
    public String getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(String chatRoom) {
        this.chatRoom = chatRoom;
    }

    @Column(name = "base_layer", unique = false, nullable = true)
    public String getBaseLayer() {
        return baseLayer;
    }

    public void setBaseLayer(String baseLayer) {
        this.baseLayer = baseLayer;
    }

    @Column(name = "bbox", unique = false, nullable = true)
    public String getBbox() {
        return bbox;
    }

    public void setBbox(String bbox) {
        this.bbox = bbox;
    }
    
    @Column(name = "bounding_polygon", unique = false, nullable = true)
    public String getBoundingPolygon() {
        return boundingPolygon;
    }

    public void setBoundingPolygon(String boundingPolygon) {
        this.boundingPolygon = boundingPolygon;
    }

    @Column(name = "path", unique = false, nullable = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Column(name = "classification", unique = false, nullable = true)
    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    @Column(name = "tool", unique = false, nullable = true)
    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    @Column(name = "expiration", unique = false, nullable = true)
    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    @JsonIgnore
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "mission_resource", joinColumns = {@JoinColumn(name = "mission_id", referencedColumnName = "id")},
                                   inverseJoinColumns = {@JoinColumn(name = "resource_id", referencedColumnName = "id"),
                                                         @JoinColumn(name = "resource_hash", referencedColumnName = "hash")})
    public Set<Resource> getContents() {
        return contents;
    }

    public void setContents(Set<Resource> contents) {
        this.contents = contents;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mission_uid")
    @Column(name = "uid")
    @JsonIgnore
    public Set<String> getUids() {
        return uids;
    }

    public void setUids(Set<String> uids) {
        this.uids = uids;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mission_keyword")
    @Column(name = "keyword")
    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    @Column
    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT_PAD_MILLIS)
    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT_PAD_MILLIS)
    @Column(name = "last_edited", insertable = false, updatable = false)
    public Date getLastEdited() {
        return lastEdited;
    }

    public void setLastEdited(Date lastEdited) {
        this.lastEdited = lastEdited;
    }

    @Transient
    @JsonProperty("uids")
    public List<MissionAdd<String>> getUidAdds() {
        return uidAdds;
    }

    public void setUidAdds(List<MissionAdd<String>> uidAdds) {
        this.uidAdds = uidAdds;
    }

    @Transient
    @JsonProperty("contents")
    public List<MissionAdd<Resource>> getResourceAdds() {
        return resourceAdds;
    }

    public void setResourceAdds(List<MissionAdd<Resource>> resourceAdds) {
        this.resourceAdds = resourceAdds;
    }

    @Column(name = "groups", columnDefinition = "bit varying")
    @JsonIgnore
    public String getGroupVector() {
        return groupVector;
    }

    public void setGroupVector(String groupVector) {
        this.groupVector = groupVector;
    }

    @Transient
    public NavigableSet<String> getGroups() { return groups; }

    public void setGroups(NavigableSet<String> groups) { this.groups = groups; }

    @Override
    public int compareTo(Mission that) {
        return ComparisonChain.start().compare(this.getName().toLowerCase(), that.getName().toLowerCase()).result();
    }

    @JsonIgnore
    @ManyToOne(cascade={CascadeType.ALL}, fetch = FetchType.LAZY)
    @JoinColumn(name="parent_mission_id")
    public Mission getParent() { return parent; }

    public void setParent(Mission parent) { this.parent = parent; }

    @JsonIgnore
    @OneToMany(mappedBy="parent", fetch = FetchType.LAZY)
    public Set<Mission> getChildren() { return children; }

    public void setChildren(Set<Mission> children) { this.children = children; }

    @OneToMany(mappedBy="mission", fetch = FetchType.EAGER)
    public Set<ExternalMissionData> getExternalData() { return externalData; }

    public void setExternalData(Set<ExternalMissionData> externalData) { this.externalData = externalData; }

    @OneToMany(mappedBy="mission", fetch = FetchType.EAGER)
    public Set<MapLayer> getMapLayers() { return mapLayers; }

    public void setMapLayers(Set<MapLayer> mapLayers) { this.mapLayers = mapLayers; }

    @OneToMany(mappedBy="mission", fetch = FetchType.EAGER)
    public Set<MissionFeed> getFeeds() { return feeds; }

    public void setFeeds(Set<MissionFeed> feeds) { this.feeds = feeds; }

    @JsonIgnore
    @Column(name = "password_hash", unique = false, nullable = true)
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Transient
    @JsonProperty("passwordProtected")
    public boolean isPasswordProtected() {
        return !Strings.isNullOrEmpty(passwordHash);
    }
    @Transient
    @JsonProperty("token")
    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }
    @Transient
    public MissionRole getOwnerRole() { return ownerRole; }

    public void setOwnerRole(MissionRole ownerRole) { this.ownerRole = ownerRole; }
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="default_role_id")
    public MissionRole getDefaultRole() { return defaultRole; }

    public void setDefaultRole(MissionRole role) { this.defaultRole = role; }
    @Transient
    public Set<MissionChange> getMissionChanges() { return missionChanges; }

    public void setMissionChanges(Set<MissionChange> missionChanges) { this.missionChanges = missionChanges; }
    @Transient
    public List<LogEntry> getLogs() { return logs; }

    public void setLogs(List<LogEntry> logs) { this.logs = logs; }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Mission [name=");
        builder.append(name);
        builder.append(", contents=");
        builder.append(contents);
        builder.append(", uids=");
        builder.append(uids);
        builder.append(", keywords=");
        builder.append(keywords);
        builder.append(", creatorUid=");
        builder.append(creatorUid);
        builder.append(", createTime=");
        builder.append(createTime);
        builder.append(", description=");
        builder.append(description);
        builder.append(", chatRoom=");
        builder.append(chatRoom);
        builder.append(", tool=");
        builder.append(tool);
        builder.append(", expiration=");
        builder.append(expiration);
        builder.append("]");
        builder.append("uidAdds: " + getUidAdds() + " resourceAdds: " + getResourceAdds());
        
        return builder.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MissionAdd<T> {
        
        @Override
		public String toString() {
			return "MissionAdd [data=" + data + ", timestamp=" + timestamp + ", creatorUid=" + creatorUid
					+ ", keywords=" + keywords + "]";
		}

		private T data;
        
        private Date timestamp;
        
        private String creatorUid;

        private List<String> keywords;

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT_PAD_MILLIS)
        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public String getCreatorUid() {
            return creatorUid;
        }

        public void setCreatorUid(String creatorUid) {
            this.creatorUid = creatorUid;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MissionAddDetails<T> extends MissionAdd<T> {

		@Override
		public String toString() {
			return getData().toString();
		}

		private UidDetails uidDetails = null;

        @JsonProperty("details")
        public UidDetails getUidDetails() { return uidDetails; }

        public void setUidDetails(UidDetails uidDetails) { this.uidDetails = uidDetails; }
    }
    
    

    public void clear() {
        getContents().clear();
        getResourceAdds().clear();
        getUids().clear();
        getUidAdds().clear();
        getExternalData().clear();
        getMapLayers().clear();
        getFeeds().clear();
    }
}