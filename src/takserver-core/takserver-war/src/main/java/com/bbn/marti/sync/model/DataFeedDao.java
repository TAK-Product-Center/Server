package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ComparisonChain;

import tak.server.feeds.DataFeed.DataFeedType;

@JsonInclude(Include.NON_NULL) 
@Entity
@Table(name = "data_feed")
public class DataFeedDao implements Serializable, Comparable<DataFeedDao> {

	private static final long serialVersionUID = -5463730874196814957L;
	
	protected Long id = 0L;
	protected String uuid = "";
	protected String name = "";
	protected String auth = "";
	protected boolean authRequired;
	protected String protocol;
	protected Integer port;
	protected String feedGroup;
	protected String iface;
	protected boolean archive;
	protected boolean anongroup;
	protected boolean archiveOnly;
	protected boolean sync;
	protected Integer coreVersion;
	protected String coreVersion2TlsVersions;
	protected int type;
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, columnDefinition="long")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "uuid", unique = true, nullable = false, columnDefinition="string")
    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
    
    @Column(name = "name", unique = false, nullable = false, columnDefinition="string")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Column(name = "auth", unique = false, nullable = true, columnDefinition="string")
    public String getAuth() {
    	return auth;
    }
    
    public void setAuth(String auth) {
    	this.auth = auth;
    }

    @Column(name = "type", unique = false, nullable = false, columnDefinition="integer")
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
    @Column(name = "port", unique = false, nullable = true, columnDefinition="integer")
    public Integer getPort() {
    	return port;
    }
    
    public void setPort(Integer port) {
    	this.port = port;
    }
    
    @Column(name = "auth_required", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getAuthRequired() {
    	return authRequired;
    }
    
    public void setAuthRequired(boolean authRequired) {
    	this.authRequired = authRequired;
    }
    
    @Column(name = "protocol", unique = false, nullable = true, columnDefinition="string")
    public String getProtocol() {
    	return protocol;
    }
    
    public void setProtocol(String protocol) {
    	this.protocol = protocol;
    }
    
    @Column(name = "feed_group", unique = false, nullable = true, columnDefinition="string")
    public String getFeedGroup() {
    	return feedGroup;
    }
    
    public void setFeedGroup(String feedGroup) {
    	this.feedGroup = feedGroup;
    }
    
    @Column(name = "iface", unique = false, nullable = true, columnDefinition="string")
    public String getIface() {
    	return iface;
    }
    
    public void setIface(String iface) {
    	this.iface = iface;
    }
    
    @Column(name = "archive", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getArchive() {
    	return archive;
    }
    
    public void setArchive(boolean archive) {
    	this.archive = archive;
    }
    
    @Column(name = "anongroup", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getAnongroup() {
    	return anongroup;
    }
    
    public void setAnongroup(boolean anongroup) {
    	this.anongroup = anongroup;
    }
    
    @Column(name = "archive_only", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getArchiveOnly() {
    	return archiveOnly;
    }
    
    public void setArchiveOnly(boolean archiveOnly) {
    	this.archiveOnly = archiveOnly;
    }

    @Column(name = "sync", unique = false, nullable = true, columnDefinition="boolean")
    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    @Column(name = "core_version", unique = false, nullable = true, columnDefinition="integer")
    public Integer getCoreVersion() {
    	return coreVersion;
    }
    
    public void setCoreVersion(Integer coreVersion) {
    	this.coreVersion = coreVersion;
    }
    
    @Column(name = "core_version_tls_versions", unique = false, nullable = true, columnDefinition="string")
    public String getCoreVersion2TlsVersions() {
    	return coreVersion2TlsVersions;
    }
    
    public void setCoreVersion2TlsVersions(String coreVersion2TlsVersions) {
    	this.coreVersion2TlsVersions = coreVersion2TlsVersions;
    }


	@Override
	public int compareTo(DataFeedDao that) {
		return ComparisonChain.start().compare(this.getId(), that.getId()).result();
	}

}
