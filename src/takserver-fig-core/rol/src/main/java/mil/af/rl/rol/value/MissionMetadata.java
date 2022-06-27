package mil.af.rl.rol.value;

public class MissionMetadata extends Parameters {
    
    private static final long serialVersionUID = 9065876638429942749L;
    
    private String name;
    private String creatorUid;
    private String description;
    private String chatRoom;
    private String tool;
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
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
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
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MissionMetadata [name=");
        builder.append(name);
        builder.append(", creatorUid=");
        builder.append(creatorUid);
        builder.append(", description=");
        builder.append(description);
        builder.append(", chatRoom=");
        builder.append(chatRoom);
        builder.append(", tool=");
        builder.append(tool);
        builder.append("]");
        return builder.toString();
    }
}
