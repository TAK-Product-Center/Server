

package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ComparisonChain;

import tak.server.Constants;

@Entity
@Table(name = "mission_log")
@Cacheable
@JsonInclude(Include.NON_NULL)
public class LogEntry implements Serializable, Comparable<LogEntry> {

    private static final long serialVersionUID = 68934987235978L;

    /**
     * 
     * 
     * Model class representing a textual mission log entry, that can be associated with one or more missions.
     * 
     */

    protected static final Logger logger = LoggerFactory.getLogger(LogEntry.class);
    
    protected String id; // machine-generated string UUID
    protected String content;
    protected String creatorUid;
    protected String entryUid;
    protected Set<String> missionNames;
    protected Date servertime; // time when LogEntry is saved to the database
    protected Date dtg; // date time group - client provided timestamp
    protected Date created; // time when LogEntry gets created. will differ from servertime for offline changes
    protected Set<String> contentHashes;
    protected Set<String> keywords;
    
    // no-arg constructor
    public LogEntry() {
        content = "";
        creatorUid = "";
        missionNames = new ConcurrentSkipListSet<>();
        contentHashes = new ConcurrentSkipListSet<>();
        keywords = new ConcurrentSkipListSet<>();
    }
    
    public LogEntry(@NotNull String contents, @NotNull String sourceUid, @NotNull Date ts) {
        this.content = contents;
        this.creatorUid = sourceUid;
        this.servertime = ts;
    }
     
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2") // tell hibernate to use java.util.UUID to generate 
    @Column(name = "id", unique = true, nullable = false)
    public String getId() {
        return id;
    }   

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

    @Column(name = "creator_uid")
    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }
   
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "mission_log_mission_name", joinColumns = {@JoinColumn(name = "mission_log_id", referencedColumnName = "id")})
    public Set<String> getMissionNames() {
        return missionNames;
    }

    public void setMissionNames(Set<String> missionNames) {
        this.missionNames = missionNames;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT_PAD_MILLIS)
    public Date getServertime() {
        return servertime;
    }

    public void setServertime(Date servertime) {
        this.servertime = servertime;
    }
    
    @Column(name = "entry_uid")
    public String getEntryUid() {
        return entryUid;
    }

    public void setEntryUid(String entryUid) {
        this.entryUid = entryUid;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT_PAD_MILLIS)
    public Date getDtg() {
        return dtg;
    }

    public void setDtg(Date dtg) {
        this.dtg = dtg;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT_PAD_MILLIS)
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
    
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "mission_log_hash", joinColumns = {@JoinColumn(name = "mission_log_id", referencedColumnName = "id")})
    public Set<String> getContentHashes() {
        return contentHashes;
    }

    public void setContentHashes(Set<String> contentHashes) {
        this.contentHashes = contentHashes;
    }
   
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "mission_log_keyword", joinColumns = {@JoinColumn(name = "mission_log_id", referencedColumnName = "id")})
    @Column(name = "keyword")
    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public int compareTo(LogEntry that) {
        return ComparisonChain.start() // timestamps will almost certainly be different, but also use the id when comparing, just in case
                              .compare(this.servertime, that.servertime)
                              .compare(this.id, that.id)
                              .result();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result
                + ((creatorUid == null) ? 0 : creatorUid.hashCode());
        result = prime * result + ((content == null) ? 0 : content.hashCode());
        result = prime * result + ((servertime == null) ? 0 : servertime.hashCode());
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
        LogEntry other = (LogEntry) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (creatorUid == null) {
            if (other.creatorUid != null)
                return false;
        } else if (!creatorUid.equals(other.creatorUid))
            return false;
        if (content == null) {
            if (other.content != null)
                return false;
        } else if (!content.equals(other.content))
            return false;
        if (servertime == null) {
            if (other.servertime != null)
                return false;
        } else if (!servertime.equals(other.servertime))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LogEntry [id=");
        builder.append(id);
        builder.append(", content=");
        builder.append(content);
        builder.append(", sourceUid=");
        builder.append(creatorUid);
        builder.append(", ts=");
        builder.append(servertime);
        builder.append("]");
        return builder.toString();
    } 
}



