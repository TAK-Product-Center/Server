package com.bbn.marti.remote.sync;

import com.bbn.marti.sync.model.LogEntry;
import java.util.Date;


public class MissionUpdateDetailsForLogEntry {

    private String id;
    private LogEntry logEntry;
    private Date created;
    private MissionUpdateDetailsForLogEntryType missionUpdateDetailsForLogEntryType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public MissionUpdateDetailsForLogEntryType getMissionUpdateDetailsForLogEntryType() {
        return missionUpdateDetailsForLogEntryType;
    }

    public void setMissionUpdateDetailsForLogEntryType(MissionUpdateDetailsForLogEntryType missionUpdateDetailsForLogEntryType) {
        this.missionUpdateDetailsForLogEntryType = missionUpdateDetailsForLogEntryType;
    }
}
