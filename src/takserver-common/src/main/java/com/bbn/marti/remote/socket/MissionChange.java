

package com.bbn.marti.remote.socket;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionChange extends TakMessage {
	
	private static final String className = MissionChange.class.getSimpleName();
	
	public static String getClassName() {
		return className;
	}
    
    private static final long serialVersionUID = 7861394571341L;
    
    public String getMessageType() {
        return className;
    }
    
    private String missionName;
    
    private String cotType;
    
    private Long time;

    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public String getCotType() {
        return cotType;
    }

    public void setCotType(String cotType) {
        this.cotType = cotType;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cotType == null) ? 0 : cotType.hashCode());
        result = prime * result
                + ((className == null) ? 0 : className.hashCode());
        result = prime * result
                + ((missionName == null) ? 0 : missionName.hashCode());
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MissionChange other = (MissionChange) obj;
        if (cotType == null) {
            if (other.cotType != null)
                return false;
        } else if (!cotType.equals(other.cotType))
            return false;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (missionName == null) {
            if (other.missionName != null)
                return false;
        } else if (!missionName.equals(other.missionName))
            return false;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MissionChange [messageType=");
        builder.append(className);
        builder.append(", missionName=");
        builder.append(missionName);
        builder.append(", cotType=");
        builder.append(cotType);
        builder.append(", time=");
        builder.append(time);
        builder.append("]");
        return builder.toString();
    }
}
