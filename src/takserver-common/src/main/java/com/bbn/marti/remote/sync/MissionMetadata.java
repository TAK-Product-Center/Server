package com.bbn.marti.remote.sync;

/*
 * 
 * value class holding succinct metadata about a mission
 * 
 */
public class MissionMetadata {
	
	private String name;
	private String creatorUid;
	private String chatRoom;
	private String tool;
	private String description;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCreatorUid() {
		return creatorUid;
	}
	public void setCreatorUid(String creatorUid) {
		this.creatorUid = creatorUid;
	}
	public String getChatRoom() {
		return chatRoom;
	}
	public void setChatRoom(String chatRoom) {
		this.chatRoom = chatRoom;
	}
	public String getTool() {
		return tool;
	}
	public void setTool(String tool) {
		this.tool = tool;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@Override
	public String toString() {
		return "MissionMetadata [name=" + name + ", creatorUid=" + creatorUid + ", chatRoom=" + chatRoom + ", tool="
				+ tool + ", description=" + description + "]";
	}
}
