package com.bbn.marti.device.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created on 5/9/2018.
 */
@Entity
@Table(name = "device_profile_directory")
@Cacheable
public class ProfileDirectory implements Serializable {

    private Long id;
    private Long profileId;
    private String path;

    public ProfileDirectory(){
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

    @JsonIgnore
    @Column(name = "device_profile_id", unique = true, nullable = false)
    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    @Column(name = "path", unique = false, nullable = false, columnDefinition="VARCHAR")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}


