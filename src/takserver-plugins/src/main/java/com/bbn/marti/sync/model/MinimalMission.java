package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.collect.ComparisonChain;

/*
 * Minimal value class to contain on the mission elements that are necessary for mission data feed filtering.
 */
public class MinimalMission implements Serializable, Comparable<MinimalMission> {
	private static final long serialVersionUID = 3173373473652168141L;
	private String name;
	private String tool;
	private String boundingPolygon;
	private String bbox;
	private Long id;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTool() {
		return tool;
	}
	public void setTool(String tool) {
		this.tool = tool;
	}
	public String getBoundingPolygon() {
		return boundingPolygon;
	}
	public void setBoundingPolygon(String boundingPolygon) {
		this.boundingPolygon = boundingPolygon;
	}
	public String getBbox() {
		return bbox;
	}
	public void setBbox(String bbox) {
		this.bbox = bbox;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	public MinimalMission() { }
	
	public MinimalMission(String name, String tool, String boundingPolygon, String bbox, Long id) {
		super();
		this.name = name;
		this.tool = tool;
		this.boundingPolygon = boundingPolygon;
		this.bbox = bbox;
		this.id = id;
	}
	
	
	public MinimalMission(Mission mission) {
		super();
		setName(mission.getName());
		setTool(mission.getTool());
		setBoundingPolygon(mission.getBoundingPolygon());
		setBbox(mission.getBbox());
		setId(mission.getId());
	}
	@Override
	public int hashCode() {
		return Objects.hash(bbox, boundingPolygon, id, name, tool);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MinimalMission other = (MinimalMission) obj;
		return Objects.equals(bbox, other.bbox) && Objects.equals(boundingPolygon, other.boundingPolygon)
				&& Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(tool, other.tool);
	}
	@Override
	public String toString() {
		return "MinimalMission [name=" + name + ", tool=" + tool + ", boundingPolygon=" + boundingPolygon + ", bbox="
				+ bbox + ", id=" + id + "]";
	}
	
	@Override
    public int compareTo(MinimalMission that) {
        return ComparisonChain.start().compare(this.getName().toLowerCase(), that.getName().toLowerCase()).result();
    }	

}
