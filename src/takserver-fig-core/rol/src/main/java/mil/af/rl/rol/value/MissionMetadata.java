package mil.af.rl.rol.value;

public class MissionMetadata extends Parameters {
    
    private static final long serialVersionUID = 9065876638429942749L;
    
    private String type = "MissionMetadata";
    private String name = "";
    private String creatorUid = "";
    private String description = "";
    private String chatRoom = "";
    private String tool = "public";
    private String boundingPolygon = "";
    private String bbox = "";
    private String passwordHash = "";
    private String path = "";
    private String classification = "";
    private String baseLayer = "";
    private long parentMissionId;
    private long defaultRoleId;
    private long expiration;
    private boolean inviteOnly;
    private String guid;
     
    public MissionMetadata() {
		super();
	}
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
    public String getBoundingPolygon() {
		return boundingPolygon;
	}
	public void setBoundingPolygon(String boundingPolygon) {
		this.boundingPolygon = boundingPolygon;
	}
	public String getBbox() {
		return bbox;
	}
	public void setBbox(String bbox) {
		this.bbox = bbox;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getClassification() {
		return classification;
	}
	public void setClassification(String classification) {
		this.classification = classification;
	}
	public long getParentMissionId() {
		return parentMissionId;
	}
	public void setParentMissionId(long parenMissionId) {
		this.parentMissionId = parenMissionId;
	}
	public long getDefaultRoleId() {
		return defaultRoleId;
	}
	public void setDefaultRoleId(long defaultRoleId) {
		this.defaultRoleId = defaultRoleId;
	}
	public long getExpiration() {
		return expiration;
	}
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}
	public String getPasswordHash() {
		return passwordHash;
	}
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
	public String getBaseLayer() {
		return baseLayer;
	}
	public void setBaseLayer(String baseLayer) {
		this.baseLayer = baseLayer;
	}
	public boolean getInviteOnly() {
		return inviteOnly;
	}
	public void setInviteOnly(boolean inviteOnly) {
		this.inviteOnly = inviteOnly;
	}
	public String getGuid() {
		return guid;
	}
	public void setGuid(String guid) {
		this.guid = guid;
	}
	@Override
	public String toString() {
		return "MissionMetadata [type=" + type + ", name=" + name + ", creatorUid=" + creatorUid + ", description="
				+ description + ", chatRoom=" + chatRoom + ", tool=" + tool + ", boundingPolygon=" + boundingPolygon
				+ ", bbox=" + bbox + ", passwordHash=" + passwordHash + ", path=" + path + ", classification="
				+ classification + ", baseLayer=" + baseLayer + ", parentMissionId=" + parentMissionId
				+ ", defaultRoleId=" + defaultRoleId + ", expiration=" + expiration + ", inviteOnly=" + inviteOnly
				+ ", guid=" + guid + "]";
	}

}
