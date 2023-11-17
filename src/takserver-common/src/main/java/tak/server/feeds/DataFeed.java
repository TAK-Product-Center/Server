package tak.server.feeds;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;

import com.bbn.marti.config.AuthType;
import com.google.common.collect.ComparisonChain;

public class DataFeed implements Serializable, Comparable<DataFeed> {

	private static final long serialVersionUID = -7747557200324370313L;

	public enum DataFeedType {
		Streaming,
		API,
		Plugin,
		Federation
	}
	
	private String uuid;
	
	private String name;
	
	private DataFeedType type;
	
	private List<String> tags = new ArrayList<>();
	
	private AuthType auth;
	
	private Integer port;
	
	private boolean authRequired;
	
	private String protocol;
	
	private String group;
	
	private String iface;
	
	private boolean archive;
	
	private boolean anongroup;
	
	private boolean archiveOnly;

	private boolean sync;
	
	private Integer coreVersion;
	
	private String coreVersion2TlsVersions;
	
	private List<String> filterGroups;
		
	private int syncCacheRetentionSeconds = 3600;
	
	private boolean federated;
	
	private boolean binaryPayloadWebsocketOnly = false;
	
	private DataFeed() {}
	
	public DataFeed(String uuid, String name, DataFeedType type, List<String> tags) {
		this.uuid = uuid;
		this.name = name;
		this.type = type;
		this.tags.addAll(tags);
	}

	public DataFeed(com.bbn.marti.config.DataFeed datafeed) {
		this.uuid = datafeed.getUuid();
		this.name = datafeed.getName();
		this.tags = datafeed.getTag();
		this.auth = datafeed.getAuth();
		this.authRequired = datafeed.isAuthRequired();
		this.protocol = datafeed.getProtocol();
		this.group = datafeed.getGroup();
		this.iface = datafeed.getIface();
		this.archive = datafeed.isArchive();
		this.anongroup = datafeed.isAnongroup();
		this.archiveOnly = datafeed.isArchiveOnly();
		this.sync = datafeed.isSync();
		this.coreVersion = datafeed.getCoreVersion();
		this.coreVersion2TlsVersions = datafeed.getCoreVersion2TlsVersions();
		this.filterGroups = datafeed.getFiltergroup();
		this.syncCacheRetentionSeconds = datafeed.getSyncCacheRetentionSeconds();
		this.federated = datafeed.isFederated();
		this.binaryPayloadWebsocketOnly = datafeed.isBinaryPayloadWebsocketOnly();
		
		if (datafeed.getPort() == 0) {
			this.port = null;
		} else {
			this.port = datafeed.getPort();
		}

		this.type = EnumUtils.getEnumIgnoreCase(DataFeedType.class, datafeed.getType());
		
		if (type == null) this.type = DataFeedType.Streaming;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Collection<String> getTags() {
		return tags;
	}
	
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public DataFeedType getType() {
		return type;
	}

	public void setType(DataFeedType type) {
		this.type = type;
	}
	
	public AuthType getAuth() {
		return auth;
	}
	
	public void setAuth(AuthType auth) {
		this.auth = auth;
	}
	
	public boolean getAuthRequired() {
		return authRequired;
	}
	
	public void setAuthRequired(boolean authRequired) {
		this.authRequired = authRequired;
	}
	
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public Integer getPort() {
		return port;
	}
	
	public void setPort(Integer port) {
		this.port = port;
	}
	
	public String getGroup() {
		return group;
	}
	
	public void setGroup(String group) {
		this.group = group;
	}
	
	public String getIface() {
		return iface;
	}
	
	public void setIface(String iface) {
		this.iface = iface;
	}
	
	public boolean getArchive() {
		return archive;
	}
	
	public void setArchive(boolean archive) {
		this.archive = archive;
	}
	
	public boolean getAnongroup() {
		return anongroup;
	}
	
	public void setAnongroup(boolean anongroup) {
		this.anongroup = anongroup;
	}
	
	public boolean getArchiveOnly() {
		return archiveOnly;
	}
	
	public void setArchiveOnly(boolean archiveOnly) {
		this.archiveOnly = archiveOnly;
	}

	public boolean getSync() {
		return sync;
	}

	public void setSync(boolean sync) {
		this.sync = sync;
	}

	public Integer getCoreVersion() {
		return coreVersion;
	}
	
	public void setCoreVersion(Integer coreVersion) {
		this.coreVersion = coreVersion;
	}
	
	public String getCoreVersion2TlsVersions() {
		return coreVersion2TlsVersions;
	}
	
	public void setCoreVersion2TlsVersions(String coreVersion2TlsVersions) {
		this.coreVersion2TlsVersions = coreVersion2TlsVersions;
	}
	
	public List<String> getFilterGroups() {
		return filterGroups;
	}
	
	public void setFilterGroups(List<String> filterGroups) {
		this.filterGroups = filterGroups;
	}

	public int getsSyncCacheRetentionSeconds() {
		return syncCacheRetentionSeconds;
	}

	public void setSyncCacheRetentionSeconds(int syncCacheRetentionSeconds) {
		this.syncCacheRetentionSeconds = syncCacheRetentionSeconds;
	}

	public boolean isFederated() {
		return federated;
	}

	public void setFederated(boolean federated) {
		this.federated = federated;
	}
	
	public boolean isBinaryPayloadWebsocketOnly() {
		return binaryPayloadWebsocketOnly;
	}

	public void setBinaryPayloadWebsocketOnly(boolean binaryPayloadWebsocketOnly) {
		this.binaryPayloadWebsocketOnly = binaryPayloadWebsocketOnly;
	}

	@Override
	public String toString() {
		return "DataFeed [uuid=" + uuid + ", name=" + name + ", type=" + type + ", tags=" + tags + ", auth=" + auth
				+ ", port=" + port + ", authRequired=" + authRequired + ", protocol=" + protocol + ", group=" + group
				+ ", iface=" + iface + ", archive=" + archive + ", anongroup=" + anongroup + ", archiveOnly="
				+ archiveOnly + ", sync=" + sync + ", coreVersion=" + coreVersion + ", coreVersion2TlsVersions="
				+ coreVersion2TlsVersions + ", filterGroups=" + filterGroups + ", syncCacheRetentionSeconds="
				+ syncCacheRetentionSeconds + ", federated=" + federated + ", binaryPayloadWebsocketOnly="
				+ binaryPayloadWebsocketOnly + "]";
	}

	@Override
	public int compareTo(DataFeed that) {
		return ComparisonChain.start().compare(this.getUuid(), that.getUuid()).result();
	}

}
