package com.bbn.marti.maplayer.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import tak.server.Constants;

@Entity(name = "maplayer")
@Table(name = "maplayer")
@Cacheable
@ApiModel
public class MapLayer implements Serializable, Comparable<MapLayer> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
    private String uid;
    private String creatorUid;
    private String name;
    private String description;
    private String type; // MapTile or WMS
    private String url;
    private Date createTime;
    private Date modifiedTime;
    private boolean defaultLayer;
    private boolean enabled;


    public MapLayer() {
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

    @Column(name = "name", unique = false, nullable = false)
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

    @Column(name = "type", unique = false, nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "url", unique = false, nullable = false)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Column(name = "uid", unique = true, nullable = false)
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Column(name = "creator_uid")
    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT)
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT)
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified_time")
    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    @Column(name = "enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Column(name = "default_layer")
    public boolean isDefaultLayer() {
        return defaultLayer;
    }

    public void setDefaultLayer(boolean defaultLayer) {
        this.defaultLayer = defaultLayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapLayer mapLayer = (MapLayer) o;
        return Objects.equals(uid, mapLayer.uid) &&
                Objects.equals(url, mapLayer.url) &&
                Objects.equals(name, mapLayer.name) &&
                Objects.equals(type, mapLayer.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, url, name, type);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MapLayer{");
        sb.append(", name='").append(name);
        sb.append(", description='").append(description);
        sb.append(", type='").append(type);
        sb.append(", url='").append(url);
        sb.append(", uid='").append(uid);
        sb.append(", creatorUid='").append(creatorUid);
        sb.append(", createTime=").append(createTime);
        sb.append(", modifiedTime=").append(modifiedTime);
        sb.append(", defaultLayer=").append(defaultLayer);
        sb.append(", enabled=").append(enabled);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(@NotNull MapLayer mapLayer) {
        return 0;
    }

}
