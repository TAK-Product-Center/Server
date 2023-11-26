package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.List;

/*
 * Minimal value class to contain on the mission feed elements that are necessary for mission data feed filtering.
 */
public class MinimalMissionFeed implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String missionName;
	private String filterPolygon;
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
	
	public MinimalMissionFeed() { }

	public MinimalMissionFeed(String missionName, String filterPolygon, List<String> filterCotTypes) {
		super();
		this.missionName = missionName;
		this.filterPolygon = filterPolygon;
		this.filterCotTypes = filterCotTypes;
	}

	public MinimalMissionFeed(String missionName, MissionFeed missionFeed) {
		this.missionName = missionName;
		this.filterPolygon = missionFeed.getFilterPolygon();
		this.filterCotTypes = missionFeed.getFilterCotTypes();
	}

	public MinimalMissionFeed(MissionFeed missionFeed) {
		this.missionName = missionFeed.getMission().getName();
		this.filterPolygon = missionFeed.getFilterPolygon();
		this.filterCotTypes = missionFeed.getFilterCotTypes();
	}
	
	@Override
	public String toString() {
		return "MinimalMissionFeed [missionName=" + missionName + ", filterPolygon=" + filterPolygon
				+ ", filterCotTypes=" + filterCotTypes + "]";
	}

}
