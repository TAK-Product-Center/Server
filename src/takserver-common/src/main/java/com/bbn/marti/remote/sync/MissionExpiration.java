package com.bbn.marti.remote.sync;

public class MissionExpiration {

    private String missionName;
    private Long missionExpiration;
    
    public MissionExpiration() {
		super();
	}
    
	public MissionExpiration(String missionName, Long missionExpiration) {
		super();
		this.missionName = missionName;
		this.missionExpiration = missionExpiration;
	}
	public String getMissionName() {
		return missionName;
	}
	public void setMissionName(String missionName) {
		this.missionName = missionName;
	}
	public Long getMissionExpiration() {
		return missionExpiration;
	}
	public void setMissionExpiration(Long missionExpiration) {
		this.missionExpiration = missionExpiration;
	}
}