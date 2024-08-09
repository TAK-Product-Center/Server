package tak.db.profiler.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupsTable {
	private static final Logger logger = LoggerFactory.getLogger(GroupsTable.class);
		
	@JsonIgnore
	private final Map<Integer, String> groupsMap = new HashMap<>();
	
	@JsonIgnore
	private final Set<String> groupSet = new HashSet<>();
	
	private Collection<String> groups = new ArrayList<>();
	
	public void initializeTable(DataSource dataSource) {
		try {
			ResultSet rs = dataSource.getConnection().prepareStatement("select bitpos from groups;").executeQuery();
			
			while(rs.next()) {
				int bitpos = rs.getInt("bitpos");
				String group = String.valueOf(bitpos);
				groupsMap.put(bitpos, group);
				groupSet.add(group);
			}
			
			groups = groupsMap.values();
		} catch (SQLException e) {
			logger.error("Error populating groups table", e);
		}
	}

	public Collection<String> getGroups() {
		return groups;
	}

	public void setGroups(Collection<String> groups) {
		this.groups = groups;
	}

	public Map<Integer, String> getGroupsMap() {
		return groupsMap;
	}

	public Set<String> getGroupSet() {
		return groupSet;
	}

	@Override
	public String toString() {
		return "GroupsTable [groupsMap=" + groupsMap + ", groups=" + groups + "]";
	}
}
