package com.bbn.marti.remote.sync;

import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionLayer;

public class MissionUpdateDetailsForMissionLayer {
	
	private String missionName;
	private Mission mission;
	private String uid;
	private String name;
	private MissionLayer.Type missionLayerType;
	private String parentUid;
	private String after;
	private String creatorUid;
	private String layerUid;
	private MissionUpdateDetailsForMissionLayerType type;
	
	public MissionUpdateDetailsForMissionLayer() {
		super();
	}
	public String getMissionName() {
		return missionName;
	}
	public void setMissionName(String missionName) {
		this.missionName = missionName;
	}
	public Mission getMission() {
		return mission;
	}
	public void setMission(Mission mission) {
		this.mission = mission;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public MissionLayer.Type getMissionLayerType() {
		return missionLayerType;
	}
	public void setMissionLayerType(MissionLayer.Type missionLayerType) {
		this.missionLayerType = missionLayerType;
	}
	public String getParentUid() {
		return parentUid;
	}
	public void setParentUid(String parentUid) {
		this.parentUid = parentUid;
	}
	public String getAfter() {
		return after;
	}
	public void setAfter(String after) {
		this.after = after;
	}
	public String getCreatorUid() {
		return creatorUid;
	}
	public void setCreatorUid(String creatorUid) {
		this.creatorUid = creatorUid;
	}
	public String getLayerUid() {
		return layerUid;
	}
	public void setLayerUid(String layerUid) {
		this.layerUid = layerUid;
	}
	public MissionUpdateDetailsForMissionLayerType getType() {
		return type;
	}
	public void setType(MissionUpdateDetailsForMissionLayerType type) {
		this.type = type;
	}	

}