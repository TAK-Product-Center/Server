package com.bbn.marti.device.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Created on 5/9/2018.
 */
@Entity
@Table(name = "device_profile_file")
@Cacheable
public class ProfileFile implements Serializable, Comparable<ProfileFile>  {

    private Long id;
    private Long profileId;
    private String name;
    private byte[] data;

    public ProfileFile(){
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

    @Column(name = "name", unique = false, nullable = false, columnDefinition="VARCHAR")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    @Column(name = "data", unique = false, nullable = false)
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public int compareTo(ProfileFile profile) {
        return 0;
    }
}

