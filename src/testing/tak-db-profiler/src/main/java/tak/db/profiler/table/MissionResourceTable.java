package tak.db.profiler.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MissionResourceTable {
	private static final Logger logger = LoggerFactory.getLogger(MissionResourceTable.class);
	
	List<MissionResource> missionResources = new ArrayList<>();

	private long increment = 1000;

	public void initializeTable(DataSource dataSource) {
		try {
			doPagedQuery(dataSource, 0);
		} catch (SQLException e) {
			logger.error("Error populating mission resources table", e);
		}
	}

	private void doPagedQuery(DataSource dataSource, long currIdStart) throws SQLException {
		PreparedStatement ps = dataSource.getConnection().prepareStatement("select mission_id,resource_id from mission_resource order by mission_id offset ? limit ?;");
		ps.setLong(1, currIdStart);
		ps.setLong(2, increment);
		
		ResultSet rs = ps.executeQuery();
		int rowcount = 0;		
		while(rs.next()) {
			rowcount++;
			
			int missionId = rs.getInt("mission_id");
			String resource_id = rs.getString("resource_id");
			
			MissionResource missionResource = new MissionResource();
			missionResource.setMissionId(missionId);
			missionResource.setResourceId(resource_id);
			
			missionResources.add(missionResource);
		}
		
		rs.close();
		
		if (rowcount == increment) {
			doPagedQuery(dataSource, currIdStart + increment);
		}
	}

	public List<MissionResource> getMissionResources() {
		return missionResources;
	}

	public void setMissionResources(List<MissionResource> missionResources) {
		this.missionResources = missionResources;
	}

	@Override
	public String toString() {
		return "MissionResourceTable [missionResources=" + missionResources + "]";
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MissionResource {
		private int missionId;
		private String resourceId;
		public int getMissionId() {
			return missionId;
		}
		public void setMissionId(int missionId) {
			this.missionId = missionId;
		}
		public String getResourceId() {
			return resourceId;
		}
		public void setResourceId(String resourceId) {
			this.resourceId = resourceId;
		}
		@Override
		public String toString() {
			return "MissionResource [missionId=" + missionId + ", resourceId=" + resourceId + "]";
		}
	}
}
