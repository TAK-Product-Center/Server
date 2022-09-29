package com.bbn.marti.maplayer.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;

import com.bbn.marti.sync.model.Mission;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

//import io.swagger.annotations.ApiModel;
import tak.server.Constants;

@Entity(name = "maplayer")
@Table(name = "maplayer")
@Cacheable
//@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MapLayer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	private Integer minZoom;
	private Integer maxZoom;
    private Double north;
    private Double south;
    private Double east;
    private Double west;
    private String uid;
    private String creatorUid;
    private String name;
    private String description;
    private String type = "MapTile"; // MapTile or WMS
    private String url;
    private String tileType;
    private String serverParts;
    private String backgroundColor;
    private String tileUpdate;
    private String additionalParameters;
    private String coordinateSystem;
    private String version;
    private String layers;
    private Integer opacity;
    private Date createTime;
    private Date modifiedTime;
    private boolean defaultLayer;
    private boolean enabled;
    private boolean ignoreErrors;
    private boolean invertYCoordinate;
    protected Mission mission;


    public MapLayer() {
    }

    public MapLayer(MapLayer other) {
        this.minZoom = other.minZoom;
        this.maxZoom = other.maxZoom;
        this.north = other.north;
        this.south = other.south;
        this.east = other.east;
        this.west = other.west;
        //this.uid = other.uid;
        this.creatorUid = other.creatorUid;
        this.name = other.name;
        this.description = other.description;
        this.type = other.type;
        this.url = other.url;
        this.tileType = other.tileType;
        this.serverParts = other.serverParts;
        this.backgroundColor = other.backgroundColor;
        this.tileUpdate = other.tileUpdate;
        this.additionalParameters = other.additionalParameters;
        this.coordinateSystem = other.coordinateSystem;
        this.layers = other.layers;
        this.createTime = other.createTime;
        this.modifiedTime = other.modifiedTime;
        this.defaultLayer = other.defaultLayer;
        this.enabled = other.enabled;
        this.ignoreErrors = other.ignoreErrors;
        this.invertYCoordinate = other.invertYCoordinate;
        this.opacity = other.opacity;
        this.version = other.version;
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

    @Column(name = "min_zoom")
    public Integer getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(Integer minZoom) {
        this.minZoom = minZoom;
    }

    @Column(name = "max_zoom")
    public Integer getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(Integer maxZoom) {
        this.maxZoom = maxZoom;
    }

    @Column(name = "tile_type")
    public String getTileType() {
        return tileType;
    }

    public void setTileType(String tileType) {
        this.tileType = tileType;
    }

    @Column(name = "server_parts")
    public String getServerParts() {
        return serverParts;
    }

    public void setServerParts(String serverParts) {
        this.serverParts = serverParts;
    }

    @Column(name = "background_color")
    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Column(name = "tile_update")
    public String getTileUpdate() {
        return tileUpdate;
    }

    public void setTileUpdate(String tileUpdate) {
        this.tileUpdate = tileUpdate;
    }

    @Column(name = "ignore_errors")
    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    @Column(name = "invert_y_coordinate")
    public boolean isInvertYCoordinate() {
        return invertYCoordinate;
    }

    public void setInvertYCoordinate(boolean invertYCoordinate) {
        this.invertYCoordinate = invertYCoordinate;
    }

    @Column(name = "north")
    public Double getNorth() {
        return north;
    }

    public void setNorth(Double north) {
        this.north = north;
    }

    @Column(name = "south")
    public Double getSouth() {
        return south;
    }

    public void setSouth(Double south) {
        this.south = south;
    }

    @Column(name = "east")
    public Double getEast() {
        return east;
    }

    public void setEast(Double east) {
        this.east = east;
    }

    @Column(name = "west")
    public Double getWest() {
        return west;
    }

    public void setWest(Double west) {
        this.west = west;
    }

    @Column(name = "additional_parameters")
    public String getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(String additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    @Column(name = "coordinate_system")
    public String getCoordinateSystem() {
        return coordinateSystem;
    }

    public void setCoordinateSystem(String coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
    }

    @Column(name = "layers")
    public String getLayers() {
        return layers;
    }

    public void setLayers(String layers) {
        this.layers = layers;
    }

    @Column(name = "version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Column(name = "opacity")
    public Integer getOpacity() {
        return opacity;
    }

    public void setOpacity(Integer opacity) {
        this.opacity = opacity;
    }


    @JsonIgnore
    @XmlTransient
    @ManyToOne
    @JoinColumn(name="mission_id")
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapLayer mapLayer = (MapLayer) o;
        return
                Objects.equals(minZoom, mapLayer.minZoom) &&
                Objects.equals(maxZoom, mapLayer.maxZoom) &&
                Objects.equals(north, mapLayer.north) &&
                Objects.equals(south, mapLayer.south) &&
                Objects.equals(east, mapLayer.east) &&
                Objects.equals(west, mapLayer.west) &&
                Objects.equals(name, mapLayer.name) &&
                Objects.equals(description, mapLayer.description) &&
                Objects.equals(type, mapLayer.type) &&
                Objects.equals(url, mapLayer.url) &&
                Objects.equals(tileType, mapLayer.tileType) &&
                Objects.equals(serverParts, mapLayer.serverParts) &&
                Objects.equals(backgroundColor, mapLayer.backgroundColor) &&
                Objects.equals(tileUpdate, mapLayer.tileUpdate) &&
                Objects.equals(additionalParameters, mapLayer.additionalParameters) &&
                Objects.equals(coordinateSystem, mapLayer.coordinateSystem) &&
                Objects.equals(layers, mapLayer.layers) &&
                // Dont use these attributes in equality check since they are not always sent up from clients
                //Objects.equals(creatorUid, mapLayer.creatorUid) &&
                //Objects.equals(createTime, mapLayer.createTime) &&
                //Objects.equals(modifiedTime, mapLayer.modifiedTime) &&
                Objects.equals(defaultLayer, mapLayer.defaultLayer) &&
                Objects.equals(enabled, mapLayer.enabled) &&
                Objects.equals(ignoreErrors, mapLayer.ignoreErrors) &&
                Objects.equals(invertYCoordinate, mapLayer.invertYCoordinate) &&
                Objects.equals(opacity, mapLayer.opacity) &&
                Objects.equals(version, mapLayer.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minZoom, maxZoom, north, south, east, west, /*uid, creatorUid, */
                name, description, type, url, tileType, serverParts, backgroundColor, tileUpdate,
                additionalParameters, coordinateSystem, version, layers, opacity, /*createTime, modifiedTime, */
                defaultLayer, enabled, ignoreErrors, invertYCoordinate);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MapLayer{");
        sb.append(", name='").append(name).append("'");
        sb.append(", description='").append(description).append("'");
        sb.append(", type='").append(type).append("'");
        sb.append(", url='").append(url).append("'");
        sb.append(", uid='").append(uid).append("'");
        sb.append(", creatorUid='").append(creatorUid).append("'");
        sb.append(", createTime=").append(createTime);
        sb.append(", modifiedTime=").append(modifiedTime);
        sb.append(", defaultLayer=").append(defaultLayer);
        sb.append(", enabled=").append(enabled);
        sb.append('}');
        return sb.toString();
    }
}
