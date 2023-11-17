package com.bbn.marti.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ComparisonChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

@Entity
@Table(name = "permission")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionPermission implements Serializable, Comparable<MissionPermission> {

  	private static final long serialVersionUID = 9879879879871L;

	public enum Permission {
        MISSION_READ,
        MISSION_WRITE,
        MISSION_DELETE,
        MISSION_SET_ROLE,
        MISSION_SET_PASSWORD,
        MISSION_UPDATE_GROUPS,
        MISSION_MANAGE_FEEDS,
        MISSION_MANAGE_LAYERS
    }

    protected static final Logger logger = LoggerFactory.getLogger(MissionPermission.class);

    protected Long id;
    protected Permission permission;

    public MissionPermission(Permission permission) {
        this.permission = permission;
    }

    public MissionPermission() {
    }

    public MissionPermission(String permission) {
        this();
        this.permission = MissionPermission.Permission.valueOf(permission);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @JsonIgnore
    @XmlTransient
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "permission", unique = true, nullable = false, columnDefinition = "LONG")
    @XmlAttribute(name = "type")
    public Permission getPermission() {
        return permission;
    }
    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    @Override
    public int compareTo(MissionPermission that) {
        return ComparisonChain.start()
                .compare(this.permission, that.permission)
                .result();
    }

	@Override
	public String toString() {
		return "MissionPermission [id=" + id + ", permission=" + permission + "]";
	}
    
    
}