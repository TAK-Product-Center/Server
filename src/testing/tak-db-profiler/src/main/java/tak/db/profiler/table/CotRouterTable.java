package tak.db.profiler.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CotRouterTable {
	private static final Logger logger = LoggerFactory.getLogger(CotRouterTable.class);

	private Collection<Cot> cotMessages = new ArrayList<>();

	@JsonIgnore
	private final Map<Cot, Cot> cotMap = new HashMap<>();
	
	private long increment = 1000;
	
	public void initializeTable(DataSource dataSource, GroupsTable groupsTable) {
		try {
			doPagedQuery(dataSource, groupsTable, 100);
		} catch (SQLException e) {
			logger.error("Error populating cot router table", e);
		}
		cotMessages = cotMap.values();
	}

	private void doPagedQuery(DataSource dataSource, GroupsTable groupsTable, long currIdStart) throws SQLException {
		PreparedStatement ps = dataSource.getConnection().prepareStatement("select cot_type, groups from cot_router where id > ? limit ?;");
		ps.setLong(1, currIdStart);
		ps.setLong(2, increment);
		
		ResultSet rs = ps.executeQuery();
		int rowcount = 0;		
		while(rs.next()) {
			rowcount++;
			
			String cotType = rs.getString("cot_type");
			String groups = rs.getString("groups");
			groups = new StringBuilder(groups).reverse().toString();
			
			Set<String> foundGroups = new HashSet<String>();
			for (int i = 0; i < groups.toCharArray().length; i++) {
				// skip null groups					
				if (groups.charAt(i) == '0')
					continue;
				
				String groupName = groupsTable.getGroupsMap().get(i);
				if (groupName != null)
					foundGroups.add(groupName);
			}
			
			Cot cot = new Cot();
			cot.setCotType(cotType);
			cot.setGroups(foundGroups);
			
			Cot foundCot = cotMap.get(cot);
			
			if (foundCot == null) {
				cot.incrementCount();
				cotMap.put(cot, cot);
			} else {
				foundCot.incrementCount();
			}
		}
		rs.close();
		
		if (rowcount == increment) {
			doPagedQuery(dataSource, groupsTable, currIdStart + increment);
		}
	}

	public Collection<Cot> getCotMessages() {
		return cotMessages;
	}
	
	@Override
	public String toString() {
		return "CotRouterTable [cotMessages=" + cotMessages + "]";
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Cot {
		private long count = 0;
		private String cotType = "";
		private Set<String> groups = new HashSet<>();
		public String getCotType() {
			return cotType;
		}
		public void setCotType(String cotType) {
			this.cotType = cotType;
		}
		public Set<String> getGroups() {
			return groups;
		}
		public void setGroups(Set<String> groups) {
			this.groups = groups;
		}
		public void incrementCount() {
			count++;
		}
		public long getCount() {
			return count;
		}
		public void setCount(long count) {
			this.count = count;
		}
		@Override
		public int hashCode() {
			return Objects.hash(cotType, groups);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Cot other = (Cot) obj;
			return Objects.equals(cotType, other.cotType) && Objects.equals(groups, other.groups);
		}
	}
}
