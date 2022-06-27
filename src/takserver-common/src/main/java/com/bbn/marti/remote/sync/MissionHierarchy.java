package com.bbn.marti.remote.sync;

public class MissionHierarchy {

    private String missionName;
    private String parentMissionName;

    public void setMissionName(String name) { missionName = name; }
    public String getMissionName() { return missionName; }
    public void setParentMissionName(String name) { parentMissionName = name; }
    public String getParentMissionName() { return parentMissionName; }
}