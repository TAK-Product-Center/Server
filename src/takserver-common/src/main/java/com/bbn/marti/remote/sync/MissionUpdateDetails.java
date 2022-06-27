package com.bbn.marti.remote.sync;

/*
 * Value class to represent federated mission content
 * 
 */

/*
 *     public Mission createMission(String name, String creatorUid, String groupVector, String description, String chatRoom, String tool) {

 * 
 */
public class MissionUpdateDetails {
	
	private MissionContent content;
	private String creatorUid;
	private MissionChangeType changeType;
	private String missionName;
	private String missionCreatorUid;
	private String missionChatRoom;
	private String missionTool;
	private String missionDescription;
   
	public MissionContent getContent() {
		return content;
	}
	public void setContent(MissionContent content) {
		this.content = content;
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
	public MissionChangeType getChangeType() {
		return changeType;
	}
	public void setChangeType(MissionChangeType changeType) {
		this.changeType = changeType;
	}
	public String getMissionCreatorUid() {
		return missionCreatorUid;
	}
	public void setMissionCreatorUid(String missionCreatorUid) {
		this.missionCreatorUid = missionCreatorUid;
	}
	public String getMissionChatRoom() {
		return missionChatRoom;
	}
	public void setMissionChatRoom(String missionChatRoom) {
		this.missionChatRoom = missionChatRoom;
	}
	public String getMissionTool() {
		return missionTool;
	}
	public void setMissionTool(String missionTool) {
		this.missionTool = missionTool;
	}
	public String getMissionDescription() {
		return missionDescription;
	}
	public void setMissionDescription(String missionDescription) {
		this.missionDescription = missionDescription;
	}
	@Override
	public String toString() {
		return "MissionUpdateDetails [content=" + content + ", creatorUid=" + creatorUid + ", changeType=" + changeType
				+ ", missionName=" + missionName + ", missionCreatorUid=" + missionCreatorUid + ", missionChatRoom="
				+ missionChatRoom + ", missionTool=" + missionTool + ", missionDescription=" + missionDescription + "]";
	}
}
