package com.bbn.marti.sync.model;

import javax.persistence.*;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import tak.server.Constants;


@Entity
@Table(name = "mission_subscription")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionSubscription {

    protected String uid;
    protected String token;
    protected Mission mission;
    protected String clientUid;
    protected String username;
    protected Date createTime;
    protected MissionRole role;

    public MissionSubscription(String uid, String token, Mission mission, String clientUid, String username, Date createTime, MissionRole role) {
        this.uid = uid;
        this.token = token;
        this.mission = mission;
        this.clientUid = clientUid;
        this.username = username;
        this.createTime = createTime;
        this.role = role;
    }

    public MissionSubscription() {
    }

    @JsonIgnore
    @Id
    @Column(name = "uid", unique = true, nullable = true)
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    @ManyToOne(cascade={CascadeType.REFRESH}, fetch = FetchType.EAGER)
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }

    @Column(name = "client_uid", nullable = false, columnDefinition = "TEXT")
    public String getClientUid() {
        return clientUid;
    }
    public void setClientUid(String clientUid) {
        this.clientUid = clientUid;
    }

    @Column(name = "username", nullable = false, columnDefinition = "TEXT")
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.COT_DATE_FORMAT)
    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="role_id")
    public MissionRole getRole() { return role; }
    public void setRole(MissionRole role) { this.role = role; }
}
