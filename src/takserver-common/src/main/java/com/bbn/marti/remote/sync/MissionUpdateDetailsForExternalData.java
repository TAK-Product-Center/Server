package com.bbn.marti.remote.sync;

import com.bbn.marti.sync.model.ExternalMissionData;
import java.util.UUID;

public class MissionUpdateDetailsForExternalData {

    private UUID missionGuid;
    private String missionName;
    private String creatorUid;
    private String externalMissionDataId;
    private String notes;
    private String token;
    private ExternalMissionData externalMissionData;
    private MissionUpdateDetailsForExternalDataType missionUpdateDetailsForExternalDataType;

    public UUID getMissionGuid() {
        return missionGuid;
    }

    public void setMissionGuid(UUID missionGuid) {
        this.missionGuid = missionGuid;
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

    public String getExternalMissionDataId() {
        return externalMissionDataId;
    }

    public void setExternalMissionDataId(String externalMissionDataId) {
        this.externalMissionDataId = externalMissionDataId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ExternalMissionData getExternalMissionData() {
        return externalMissionData;
    }

    public void setExternalMissionData(ExternalMissionData externalMissionData) {
        this.externalMissionData = externalMissionData;
    }

    public MissionUpdateDetailsForExternalDataType getMissionUpdateDetailsForExternalDataType() {
        return missionUpdateDetailsForExternalDataType;
    }

    public void setMissionUpdateDetailsForExternalDataType(MissionUpdateDetailsForExternalDataType missionUpdateDetailsForExternalDataType) {
        this.missionUpdateDetailsForExternalDataType = missionUpdateDetailsForExternalDataType;
    }
}
