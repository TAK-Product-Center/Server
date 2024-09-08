package tak.db.profiler.table;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tak.db.profiler.TakDBProfilerConstants;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceTable {
	private static final Logger logger = LoggerFactory.getLogger(ResourceTable.class);
	
	private List<Resource> resources = new ArrayList<>();
	
	private long increment = 1000;
	
	public void initializeTable(DataSource dataSource, GroupsTable groupsTable) {
		try {
			doPagedQuery(dataSource, groupsTable, 0);
			
		} catch (SQLException e) {
			logger.error("Error populating resource table", e);
		}
	}

	private void doPagedQuery(DataSource dataSource, GroupsTable groupsTable, long currIdStart) throws SQLException {
		PreparedStatement ps = dataSource.getConnection().prepareStatement("select mimetype, id, groups, keywords, expiration, octet_length(data) as file_size from resource where id > ? limit ?;");
		ps.setLong(1, currIdStart);
		ps.setLong(2, increment);
		
		ResultSet rs = ps.executeQuery();
		int rowcount = 0;	
		
		while(rs.next()) {
			rowcount++;

			String id = rs.getString("id");
			
			Resource resource = new Resource();
			resource.setId(id);
			resource.setMimetype(rs.getString("mimetype"));
			resource.setExpiration(rs.getLong("expiration"));
			resource.setSize(rs.getLong("file_size"));
			
			// get the groups and reverse the chars so that we can iterate in ascending order
			String groups = rs.getString("groups");
			groups = new StringBuilder(groups).reverse().toString();
			
			List<String> foundGroups = new ArrayList<String>();
			for (int i = 0; i < groups.toCharArray().length; i++) {
				// skip null groups					
				if (groups.charAt(i) == '0')
					continue;
				
				String groupName = groupsTable.getGroupsMap().get(i);
				if (groupName != null)
					foundGroups.add(groupName);
			}
			resource.setGroups(foundGroups);
			
			Array keywordsObj = rs.getArray("keywords");

			// handle case for null keywords
			String[] keywords;
			if (keywordsObj == null) {
				keywords = new String[0];
			} else {
				keywords = (String[]) keywordsObj.getArray();
			}
			
			// only keyword we care about is mission package
			List<String> foundKeywords = new ArrayList<String>();
			for (int i = 0; i < keywords.length; i++) {
				String keyword = keywords[i];
				if ("missionpackage".equals(keyword)) 
					foundKeywords.add(keyword);
			}
			resource.setKeywords(foundKeywords);
			
			resources.add(resource);
		}
		rs.close();
		
		if (rowcount == increment) {
			doPagedQuery(dataSource, groupsTable, currIdStart + increment);
		}
	}
	
	public List<Resource> getResources() {
		return resources;
	}

	public void setResources(List<Resource> resources) {
		this.resources = resources;
	}

	@Override
	public String toString() {
		return "ResourceTable [resources=" + resources + "]";
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Resource {
		private String id;
		private String mimetype;
		private long expiration;
		private long size;
		private List<String> groups;
		private List<String> keywords;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getMimetype() {
			return mimetype;
		}
		public void setMimetype(String mimetype) {
			this.mimetype = mimetype;
		}
		public long getExpiration() {
			return expiration;
		}
		public void setExpiration(long expiration) {
			this.expiration = expiration;
		}
		public long getSize() {
			return size;
		}
		public void setSize(long size) {
			this.size = size;
		}
		public List<String> getGroups() {
			return groups;
		}
		public void setGroups(List<String> groups) {
			this.groups = groups;
		}
		public List<String> getKeywords() {
			return keywords;
		}
		public void setKeywords(List<String> keywords) {
			this.keywords = keywords;
		}
		@Override
		public String toString() {
			return "Resource [id=" + id + ", mimetype=" + mimetype + ", expiration=" + expiration + ", size=" + size
					+ ", groups=" + groups + ", keywords=" + keywords + "]";
		}
	}
}
