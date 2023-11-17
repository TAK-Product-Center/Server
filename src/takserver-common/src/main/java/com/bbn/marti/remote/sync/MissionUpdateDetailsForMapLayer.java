package com.bbn.marti.remote.sync;

import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.sync.model.Mission;

public class MissionUpdateDetailsForMapLayer {

	private String missionName;
	private String creatorUid;
	private Mission mission;
	private MapLayer mapLayer;
	private MissionUpdateDetailsForMapLayerType type;
    
    public MissionUpdateDetailsForMapLayer() {
		super();
	}

	public String getMissionName() {
		return missionName;
	}

	public void setMissionName(String missionName) {
		this.missionName = missionName;
	}

	public String getCreatorUid() {
		return creatorUid;
	}

	public void setCreatorUid(String creatorUid) {
		this.creatorUid = creatorUid;
	}

	public Mission getMission() {
		return mission;
	}

	public void setMission(Mission mission) {
		this.mission = mission;
	}

	public MapLayer getMapLayer() {
		return mapLayer;
	}

	public void setMapLayer(MapLayer mapLayer) {
		this.mapLayer = mapLayer;
	}

	public MissionUpdateDetailsForMapLayerType getType() {
		return type;
	}

	public void setType(MissionUpdateDetailsForMapLayerType type) {
		this.type = type;
	}

}