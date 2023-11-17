package mil.af.rl.rol.value;

import java.util.ArrayList;
import java.util.List;

public class DataFeedMetadata extends Parameters {

	private static final long serialVersionUID = 4175811435951426706L;
	
	private String type = "DataFeedMetadata";
	private String missionName = "";
    private String missionFeedUid = "";
    private String dataFeedUid = "";
    private String filterBbox = "";
    private String filterType = "";
    private String filterCallsign = "";
   
    private boolean archive = false;
    private boolean sync = false;
    private boolean enableLatestSA = true;
    private boolean archiveOnly = false;
    private String feedName = "";
    private String authType;
    private int syncCacheRetentionSeconds = 3600;
    private List<String> tags = new ArrayList<>();
    
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getMissionName() {
		return missionName;
	}
	public void setMissionName(String missionName) {
		this.missionName = missionName;
	}
	public String getMissionFeedUid() {
		return missionFeedUid;
	}
	public void setMissionFeedUid(String missionFeedUid) {
		this.missionFeedUid = missionFeedUid;
	}
	public String getDataFeedUid() {
		return dataFeedUid;
	}
	public void setDataFeedUid(String dataFeedUid) {
		this.dataFeedUid = dataFeedUid;
	}
	public String getFilterBbox() {
		return filterBbox;
	}
	public void setFilterBbox(String filterBbox) {
		this.filterBbox = filterBbox;
	}
	public String getFilterType() {
		return filterType;
	}
	public void setFilterType(String filterType) {
		this.filterType = filterType;
	}
	public String getFilterCallsign() {
		return filterCallsign;
	}
	public void setFilterCallsign(String filterCallsign) {
		this.filterCallsign = filterCallsign;
	}
	public boolean isArchive() {
		return archive;
	}
	public void setArchive(boolean archive) {
		this.archive = archive;
	}
	public boolean isSync() {
		return sync;
	}
	public void setSync(boolean sync) {
		this.sync = sync;
	}
	public boolean isArchiveOnly() {
		return archiveOnly;
	}
	public void setArchiveOnly(boolean archiveOnly) {
		this.archiveOnly = archiveOnly;
	}
	public String getFeedName() {
		return feedName;
	}
	public void setFeedName(String feedName) {
		this.feedName = feedName;
	}
	public String getAuthType() {
		return authType;
	}
	public void setAuthType(String authType) {
		this.authType = authType;
	}
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	public int getSyncCacheRetentionSeconds() {
		return syncCacheRetentionSeconds;
	}
	public void setSyncCacheRetentionSeconds(int syncCacheRetentionSeconds) {
		this.syncCacheRetentionSeconds = syncCacheRetentionSeconds;
	}
	
	@Override
	public String toString() {
		return "DataFeedMetadata [type=" + type + ", missionName=" + missionName + ", missionFeedUid=" + missionFeedUid
				+ ", dataFeedUid=" + dataFeedUid + ", filterBbox=" + filterBbox + ", filterType=" + filterType
				+ ", filterCallsign=" + filterCallsign + ", archive=" + archive + ", sync=" + sync + ", enableLatestSA="
				+ enableLatestSA + ", archiveOnly=" + archiveOnly + ", feedName=" + feedName + ", authType=" + authType
				+ ", syncCacheRetentionSeconds=" + syncCacheRetentionSeconds + ", tags=" + tags + "]";
	}

}
