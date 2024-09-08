package tak.db.profiler.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tak.db.profiler.TakDBProfilerConstants;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MissionTable {
	private static final Logger logger = LoggerFactory.getLogger(MissionTable.class);
	
	private static final Set<String> MISSION_SKIP_LIST = new HashSet<>();
		
	private List<Mission> missions = new ArrayList<>();
	private final Map<String, String> redactedMissionNames = new HashMap<>();
	private int missionCount = 0;
	
	private long increment = 1000;

	public void initializeTable(DataSource dataSource, GroupsTable groupsTable) {
		initMissionSkipList();
		
		try {
			doPagedQuery(dataSource, groupsTable, 2);
		} catch (SQLException e) {
			logger.error("Error populating groups table", e);
		}
	}

	private void doPagedQuery(DataSource dataSource, GroupsTable groupsTable, long currIdStart) throws SQLException {
		PreparedStatement ps = dataSource.getConnection().prepareStatement("select id, name, groups from mission where id > ? limit ?;");
		ps.setLong(1, currIdStart);
		ps.setLong(2, increment);
		
		ResultSet rs = ps.executeQuery();
		int rowcount = 0;	
		while(rs.next()) {
			rowcount++;
			
			int id = rs.getInt("id");
			String name = rs.getString("name");
			
			// get the groups and reverse the chars so that we can iterate in ascending order
			String groups = rs.getString("groups");
			groups = new StringBuilder(groups).reverse().toString();
					
			// skip missions we explicitly don't want to profile
			if (MISSION_SKIP_LIST.contains(name)) 
				continue;

			// create redacted mission names. use a hashmap so that mission's with the same name
			// receive the same redacted name
			if (!redactedMissionNames.containsKey(name)) {
				redactedMissionNames.put(name, "mission_" + missionCount++);
			}
			
			List<String> foundGroups = new ArrayList<String>();
			for (int i = 0; i < groups.toCharArray().length; i++) {
				// skip null groups					
				if (groups.charAt(i) == '0')
					continue;
				
				String groupName = groupsTable.getGroupsMap().get(i);
				if (groupName != null)
					foundGroups.add(groupName);
			}
			
			Mission mission = new Mission();
			mission.setId(id);
			mission.setName(redactedMissionNames.get(name));
			mission.setGroups(foundGroups);
			
			missions.add(mission);
		}
		
		rs.close();
		
		if (rowcount == increment) {
			doPagedQuery(dataSource, groupsTable, currIdStart + increment);
		}
	}
	
	public List<Mission> getMissions() {
		return missions;
	}

	public void setMissions(List<Mission> missions) {
		this.missions = missions;
	}

	private void initMissionSkipList() {
		MISSION_SKIP_LIST.add("exchecktemplates");
		MISSION_SKIP_LIST.add("citrap");
	}
	
	@Override
	public String toString() {
		return "MissionTable [missions=" + missions + ", redactedMissionNames=" + redactedMissionNames
				+ ", missionCount=" + missionCount + "]";
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Mission {
		private String name;
		private List<String> groups;
		private int id;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public List<String> getGroups() {
			return groups;
		}
		public void setGroups(List<String> groups) {
			this.groups = groups;
		}
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "Mission [name=" + name + ", groups=" + groups + ", id=" + id + "]";
		}
	}
}
