package tak.server.retention.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import tak.server.retention.service.MissionArchiveHelper;

@Configuration
@ConfigurationProperties
@PropertySource(name="mission-store", factory=YamlPropertySourceFactory.class, value="file:mission-archive/mission-store.yml")
public class MissionArchiveStoreConfig implements Serializable {
	
	private List<MissonArchiveStoreEntry> missonArchiveStoreEntries = new ArrayList<>();

	public synchronized List<MissonArchiveStoreEntry> getMissonArchiveStoreEntries() {
		return missonArchiveStoreEntries;
	}

	public synchronized void setMissonArchiveStoreEntries(List<MissonArchiveStoreEntry> missonArchiveStoreEntries) {
		this.missonArchiveStoreEntries = missonArchiveStoreEntries;
	}
	
	public synchronized void addMissionEntry(MissonArchiveStoreEntry missonArchiveStoreEntry) {
		missonArchiveStoreEntries.add(missonArchiveStoreEntry);
	}

	public static class MissonArchiveStoreEntry {
		private String missionName;
		private String createTimeMs;
		private String archiveTimeMs;
		private String createTime;
		private String archiveTime;
		private int id;
		
		public String getMissionName() {
			return missionName;
		}
		public void setMissionName(String missionName) {
			this.missionName = missionName;
		}
		public String getCreateTimeMs() {
			return createTimeMs;
		}
		public void setCreateTimeMs(String createTimeMs) {
			this.createTimeMs = createTimeMs;
		}
		public String getArchiveTimeMs() {
			return archiveTimeMs;
		}
		public void setArchiveTimeMs(String archiveTimeMs) {
			this.archiveTimeMs = archiveTimeMs;
		}
		public String getCreateTime() {
			return createTime;
		}
		public void setCreateTime(String createTime) {
			this.createTime = createTime;
		}
		public String getArchiveTime() {
			return archiveTime;
		}
		public void setArchiveTime(String archiveTime) {
			this.archiveTime = archiveTime;
		}
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		@Override
		public String toString() {
			return "MissonArchiveStoreEntry [missionName=" + missionName + ", createTimeMs=" + createTimeMs
					+ ", archiveTimeMs=" + archiveTimeMs + ", createTime=" + createTime + ", archiveTime=" + archiveTime
					+ ", id=" + id + "]";
		}
	}

	@Override
	public String toString() {
		return "MissionArchiveStoreConfig [missonArchiveStoreEntries=" + missonArchiveStoreEntries + "]";
	}
}
