package tak.db.profiler.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tak.db.profiler.TakDBProfilerConstants;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MissionUidTable {
	private static final Logger logger = LoggerFactory.getLogger(MissionUidTable.class);
	
	@JsonIgnore
	private Map<MissionCot, MissionCot> missionCotMap = new HashMap<>();
	
	private Collection<MissionCot> missionCots = new ArrayList<>();
	
	private long increment = 10000;
	
	public void initializeTable(DataSource dataSource) {
		try {
			doPagedQuery(dataSource, 0);
			missionCots = missionCotMap.values();
			
		} catch (SQLException e) {
			logger.error("Error populating mission uid table", e);
		}
	}

	private void doPagedQuery(DataSource dataSource, long currIdStart) throws SQLException {
		PreparedStatement ps = dataSource.getConnection().prepareStatement("select mission_id from mission_uid order by mission_id offset ? limit ?;");
		ps.setLong(1, currIdStart);
		ps.setLong(2, increment);
		
		ResultSet rs = ps.executeQuery();
		int rowcount = 0;	
		while(rs.next()) {
			rowcount++;
			
			int missionId = rs.getInt("mission_id");
			
			MissionCot missionCot = new MissionCot();
			missionCot.setMissionId(missionId);
			
			MissionCot foundMissionCot = missionCotMap.get(missionCot);
			
			if (foundMissionCot == null) {
				missionCotMap.put(missionCot, missionCot);
				missionCot.incrementCount();
			} else {
				foundMissionCot.incrementCount();
			}
		}
		
		rs.close();
		
		if (rowcount == increment) {
			doPagedQuery(dataSource, currIdStart + increment);
		}
	}
	
	public Collection<MissionCot> getMissionCots() {
		return missionCots;
	}

	public void setMissionCots(Collection<MissionCot> missionCots) {
		this.missionCots = missionCots;
	}

	@Override
	public String toString() {
		return "MissionUidTable [missionCots=" + missionCots + "]";
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MissionCot {
		int missionId;
		int count = 0;
		public int getMissionId() {
			return missionId;
		}
		public void setMissionId(int missionId) {
			this.missionId = missionId;
		}
		public void incrementCount() {
			this.count++;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		@Override
		public String toString() {
			return "MissionCot [missionId=" + missionId + ", count=" + count + "]";
		}
	}
}
