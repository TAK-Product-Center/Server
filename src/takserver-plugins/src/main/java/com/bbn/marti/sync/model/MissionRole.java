package com.bbn.marti.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@XmlRootElement(name = "role")
@Entity
@Table(name = "role")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionRole implements Serializable, Comparable<MissionRole> {

	public enum Role {
		MISSION_OWNER,
		MISSION_SUBSCRIBER,
		MISSION_READONLY_SUBSCRIBER
	}

	public static final Role defaultRole = Role.MISSION_SUBSCRIBER;

	protected static final Logger logger = LoggerFactory.getLogger(MissionPermission.class);

	protected Long id;
	protected Role role;
	protected Set<MissionPermission> permissions;
	private boolean usingMissionDefault;

	public MissionRole() {
		permissions = new ConcurrentSkipListSet<>();
	}

	public MissionRole(Role role) {
		this();
		this.role = role;
	}

	public MissionRole(String role) {
		this();
		this.role = Role.valueOf(role);
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

	@JsonProperty("type")
	@XmlAttribute(name = "type")
	@Column(name = "role", unique = true, nullable = false, columnDefinition = "LONG")
	public Role getRole() {
		return role;
	}
	public void setRole(Role role) {
		this.role = role;
	}

	@JsonIgnore
	@ManyToMany(fetch=FetchType.EAGER)
	@XmlTransient
	@JoinTable(name="role_permission",
	joinColumns = {@JoinColumn(name="role_id", referencedColumnName="id")},
	inverseJoinColumns = {@JoinColumn(name="permission_id", referencedColumnName="id")}
			)
	public Set<MissionPermission> getMissionPermissions() { return permissions; }
	public void setMissionPermissions(Set<MissionPermission> permissions) { this.permissions = permissions; }

	@Transient
	@JsonIgnore
	@XmlElement(name = "permissions")
	public MissionPermissions getMissionPermissionsXml() { return new MissionPermissions(permissions); }

	@Transient
	@JsonIgnore
	@XmlTransient
	public boolean isUsingMissionDefault() { return  usingMissionDefault; }
	public void setUsingMissionDefault(boolean usingMissionDefault) { this.usingMissionDefault = usingMissionDefault; }

	@Transient
	public Set<String> getPermissions() {
		Set<String> results = new HashSet<>();
		for (MissionPermission permission : getMissionPermissions()) {
			results.add(permission.permission.name());
		}
		return results;
	}

	@Override
	public int compareTo(MissionRole that) {
		return ComparisonChain.start()
				.compare(this.role, that.role)
				.result();
	}

	public boolean hasPermission(MissionPermission.Permission permission) {
		for (MissionPermission nextPermission : permissions) {
			if (nextPermission.getPermission() == permission) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAllPermissions(MissionRole role) {
		for (MissionPermission permission : role.getMissionPermissions()) {
			if (!hasPermission(permission.permission)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "MissionRole [id=" + id + ", role=" + role + ", permissions=" + permissions + "]";
	}
	
	
}