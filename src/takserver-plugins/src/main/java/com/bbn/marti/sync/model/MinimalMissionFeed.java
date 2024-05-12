package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Strings;

/*
 * Minimal value class to contain on the mission feed elements that are necessary for mission data feed filtering.
 */
public class MinimalMissionFeed implements Serializable {
	
	private static final long serialVersionUID = -4976888577605858909L;
	
	private String missionName;
	private String filterPolygon;
	private UUID missionGuid;
	private List<String> filterCotTypes;
	
	public String getMissionName() {
		return missionName;
	}
	public void setMissionName(String missionName) {
		this.missionName = missionName;
	}
	public String getFilterPolygon() {
		return filterPolygon;
	}
	public void setFilterPolygon(String filterPolygon) {
		this.filterPolygon = filterPolygon;
	}
	public List<String> getFilterCotTypes() {
		return filterCotTypes;
	}
	public void setFilterCotTypes(List<String> filterCotTypes) {
		this.filterCotTypes = filterCotTypes;
	}
	public UUID getMissionGuid() {
		return missionGuid;
	}
	public void setMissionGuid(UUID missionGuid) {
		this.missionGuid = missionGuid;
	}
	public MinimalMissionFeed() { }

	public MinimalMissionFeed(String missionName, String filterPolygon, List<String> filterCotTypes, UUID missionGuid) {
		super();
		this.missionName = missionName;
		this.filterPolygon = filterPolygon;
		this.filterCotTypes = filterCotTypes;
		this.missionGuid = missionGuid;
	}

	public MinimalMissionFeed(MissionFeed missionFeed) {
		
		if (missionFeed == null) {
			throw new IllegalArgumentException("null missionFeed");
		}
		
		if (missionFeed.getMission() == null) {
			throw new IllegalArgumentException("null missionFeed");
		}
		
		if (Strings.isNullOrEmpty(missionFeed.getMission().getName())) {
			throw new IllegalArgumentException("null mission name in mission feed");
		}
		
		if (Strings.isNullOrEmpty(missionFeed.getMission().getGuid())) {
			throw new IllegalArgumentException("null mission guid in mission feed");
		}
		
		this.missionName = missionFeed.getMission().getName();
		this.missionGuid = UUID.fromString(missionFeed.getMission().getGuid());
		
		this.filterPolygon = missionFeed.getFilterPolygon();
		this.filterCotTypes = missionFeed.getFilterCotTypes();
	}
	
	@Override
	public String toString() {
		return "MinimalMissionFeed [missionName=" + missionName + ", filterPolygon=" + filterPolygon + ", missionGuid="
				+ missionGuid + ", filterCotTypes=" + filterCotTypes + "]";
	}

}
