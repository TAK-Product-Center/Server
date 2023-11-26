package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ComparisonChain;

import tak.server.Constants;

@Entity
@Table(name = "mission_invitation")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionInvitation implements Serializable, Comparable<MissionInvitation> {

    private static final long serialVersionUID = 68934983535979L;

    public enum Type {
        clientUid, callsign, userName, group, team
    }

    protected static final Logger logger = LoggerFactory.getLogger(MissionInvitation.class);

    protected Long id;
    protected String missionName;
    protected String invitee;
    protected String type;
    protected String creatorUid;
    protected Date createTime;
    protected String token;
    protected MissionRole role;
    protected Long missionId;

    // no-arg constructor
    public MissionInvitation(String missionName, String invitee, String type, String creatorUid, Date createTime, String token, MissionRole role, Long missionId) {
    	
    	if (missionId == null) {
    		throw new IllegalArgumentException("null missionId in MissionInvitation constructor");
    	}
    	
        this.missionName = missionName;
        this.invitee = invitee;
        this.type = type;
        this.creatorUid = creatorUid;
        this.createTime = createTime;
        this.token = token;
        this.role = role;
        this.missionId = missionId;
    }

    public MissionInvitation() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @JsonIgnore
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "mission_name", unique = true, nullable = false)
    public String getMissionName() {
        return missionName;
    }
    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    @Column(name = "invitee", nullable = false, columnDefinition = "TEXT")
    public String getInvitee() {
        return invitee;
    }
    public void setInvitee(String invitee) {
        this.invitee = invitee;
    }

    @Column(name = "type", nullable = false, columnDefinition = "TEXT")
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "creator_uid", nullable = false, columnDefinition = "TEXT")
    public String getCreatorUid() {
        return creatorUid;
    }
    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT)
    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="role_id")
    public MissionRole getRole() { return role; }
    public void setRole(MissionRole role) { this.role = role; }

    @Override
    public int compareTo(MissionInvitation that) {
        return ComparisonChain.start()
                .compare(this.missionName, that.missionName)
                .compare(this.invitee, that.invitee)
                .compare(this.type, that.type)
                .compare(this.creatorUid, that.creatorUid)
                .compare(this.createTime, that.createTime)
                .result();
    }
    
    @JsonIgnore
    @Column(name = "mission_id")
    public Long getMissionId() {
		return missionId;
	}

	public void setMissionId(Long missionId) {
		this.missionId = missionId;
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((missionName == null) ? 0 : missionName.hashCode());
        result = prime * result + ((invitee == null) ? 0 : invitee.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((creatorUid == null) ? 0 : creatorUid.hashCode());
        result = prime * result + ((createTime == null) ? 0 : createTime.hashCode());
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
        MissionInvitation other = (MissionInvitation) obj;

        if (missionName == null) {
            if (other.missionName != null)
                return false;
        } else if (!missionName.equals(other.missionName))
            return false;

        if (invitee == null) {
            if (other.invitee != null)
                return false;
        } else if (!invitee.equals(other.invitee))
            return false;

        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;

        if (creatorUid == null) {
            if (other.creatorUid != null)
                return false;
        } else if (!creatorUid.equals(other.creatorUid))
            return false;

        if (createTime == null) {
            if (other.createTime  != null)
                return false;
        } else if (!createTime .equals(other.createTime ))
            return false;

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MissionInvitation[missionName=");
        builder.append(missionName);

        builder.append(", invitee=");
        builder.append(invitee);

        builder.append(", type=");
        builder.append(type);

        builder.append(", creatorUid=");
        builder.append(creatorUid);

        builder.append(", createTime=");
        builder.append(createTime);

        builder.append("]");
        return builder.toString();
    }
}