package com.bbn.marti.network;

import java.util.List;

import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.Federate.Mission;

public class FederateMissionPerConnectionSettings {
	
	private boolean missionFederateDefault;
	private List<Federation.Federate.Mission> missions;
	
	public FederateMissionPerConnectionSettings() {
		super();
	}
	
	public FederateMissionPerConnectionSettings(boolean missionFederateDefault, List<Mission> missions) {
		super();
		this.missionFederateDefault = missionFederateDefault;
		this.missions = missions;
	}
	public List<Federation.Federate.Mission> getMissions() {
		return missions;
	}
	public void setMissions(List<Federation.Federate.Mission> missions) {
		this.missions = missions;
	}
	public boolean isMissionFederateDefault() {
		return missionFederateDefault;
	}
	public void setMissionFederateDefault(boolean missionFederateDefault) {
		this.missionFederateDefault = missionFederateDefault;
	}
	
}
