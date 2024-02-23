package com.bbn.marti.device.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on 5/8/2018.
 */
@Entity
@Table(name = "device_profile")
@Cacheable
public class Profile implements Serializable, Comparable<Profile>  {

    public static final String TYPE_CONNECTION = "Connection";
    public static final String TYPE_ENROLLMENT = "Enrollment";
    public static final String TYPE_TOOL = "Tool";

    private Long id;
    private String name;
    private boolean active;
    private boolean applyOnEnrollment;
    private boolean applyOnConnect;
    private String type;
    private Date updated;
    private String tool;

    protected String groupVector;
    protected List<String> groupNames = new ArrayList<>();

    public Profile(){
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "name", unique = false, nullable = false, columnDefinition="VARCHAR")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "active", unique = false, nullable = false, columnDefinition="boolean")
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Column(name = "apply_on_enrollment", unique = false, nullable = false, columnDefinition="boolean")
    public boolean getApplyOnEnrollment() {
        return applyOnEnrollment;
    }

    public void setApplyOnEnrollment(boolean applyOnEnrollment) {
        this.applyOnEnrollment = applyOnEnrollment;
    }

    @Column(name = "apply_on_connect", unique = false, nullable = false, columnDefinition="boolean")
    public boolean getApplyOnConnect() {
        return applyOnConnect;
    }

    public void setApplyOnConnect(boolean applyOnConnect) {
        this.applyOnConnect = applyOnConnect;
    }

    @Transient
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void updateType() {
        if (getApplyOnEnrollment()) {
            setType(Profile.TYPE_ENROLLMENT);
        } else if (getApplyOnConnect()) {
            setType(Profile.TYPE_CONNECTION);
        } else {
            setType(Profile.TYPE_TOOL);
        }
    }

    @Column(name = "groups", columnDefinition = "bit varying", updatable = false)
    @JsonIgnore
    public String getGroupVector() {
        return groupVector;
    }

    public void setGroupVector(String groupVector) {
        this.groupVector = groupVector;
    }

    @Column(name = "updated", nullable = false)
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Column(name = "tool", nullable = false)
    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    @Transient
    @JsonProperty("groups")
    public List<String> getGroupNames() { return groupNames; }

    @Override
    public int compareTo(Profile profile) {
        return 0;
    }
}

