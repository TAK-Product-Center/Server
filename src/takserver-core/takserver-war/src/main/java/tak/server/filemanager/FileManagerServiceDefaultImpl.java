package tak.server.filemanager;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.bbn.marti.remote.util.ConcurrentMultiHashMap;
import com.bbn.marti.sync.model.Resource;
import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Multimap;

import tak.server.Constants;

public class FileManagerServiceDefaultImpl implements FileManagerService {

private static final Logger logger = LoggerFactory.getLogger(FileManagerServiceDefaultImpl.class);
	
	private final DataSource dataSource;
	
	@PersistenceContext
    private EntityManager entityManager;
	
	public FileManagerServiceDefaultImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public List<String> getKeywordsForResource(String hash) {
		String sql = "select keywords from resource where hash = ?;";

		return new JdbcTemplate(dataSource).query(sql,
				new Object[] { hash },
				new ResultSetExtractor<List<String>>() {
			@Override
			public List<String> extractData(ResultSet results) throws SQLException {
				if (results == null) {
					throw new IllegalStateException("null result set");
				}

				List<String> values = new ArrayList<String>();
				
				while (results.next()) {
					Array value = results.getArray(1);

					if (value != null) {
						values.addAll(Lists.newArrayList((String[]) value.getArray()));
					}
				}
				if(values.size() == 0) {
					values.add("none");
				}
				return values;
			}
		});
	}
	 
	public List<Resource> getMissionPackageResources(int limit, int offset, String sort, Boolean ascending){
		String sql = "select hash, name, octet_length(data), submitter, groups, submissiontime, mimetype, expiration, uid "
				   + "from resource where 'missionpackage' = ANY(keywords) ";
		List<Object> params = new ArrayList<>();
		// If Limit is 0, do not page
		if (limit == 0) {
			if(!sort.isBlank() && !getResourceColumnName(sort).isBlank()) {
				sql = sql + "order by " + getResourceColumnName(sort) + " ";
				if(ascending) {
					sql = sql + "asc ";
				} else {
					sql = sql + "desc ";
				}
			}
		} else {
			if(!sort.isBlank() && !getResourceColumnName(sort).isBlank()) {
				sql = sql + "order by " + getResourceColumnName(sort) + " ";
				if(ascending) {
					sql = sql + "asc ";
				} else {
					sql = sql + "desc ";
				}
			}
			// Add offset and limit at the end of statement
			sql = sql + "offset ? limit ?";
			params.add(offset);
			params.add(limit);
		}
		return getAndParseResourceQuery(sql, params.toArray());
	}
	

	@Override
	public List<Resource> getResourcesByMission(String mission, int limit, int offset, String sort, Boolean ascending) {
		String sql = "select r.hash, r.name, octet_length(data), r.submitter, r.groups, r.submissiontime, r.mimetype, r.expiration, r.uid "
				   + "from resource r inner join mission_resource mr on r.id = mr.resource_id "
				   + "inner join mission mi on mr.mission_id = mi.id where mi.name = ? ";
		List<Object> params = new ArrayList<>();
		params.add(mission);
		// If Limit is 0, do not page
		if (limit == 0) {
			if(!sort.isBlank() && !getResourceColumnName(sort).isBlank()) {
				sql = sql + "order by " + getResourceColumnName(sort) + " ";
				if(ascending) {
					sql = sql + "asc ";
				} else {
					sql = sql + "desc ";
				}
			}
		} else {
			if(!sort.isBlank() && !getResourceColumnName(sort).isBlank()) {
				sql = sql + "order by " + getResourceColumnName(sort) + " ";
				if(ascending) {
					sql = sql + "asc ";
				} else {
					sql = sql + "desc ";
				}
			}
			// Add offset and limit at the end of statement
			sql = sql + "offset ? limit ?";
			params.add(offset);
			params.add(limit);
		}
		return getAndParseResourceQuery(sql, params.toArray());
	}
	
	public int getPackageResourceCount(){
		String sql = "select count(*) from resource where 'missionpackage' = ANY(keywords) ";
		return new JdbcTemplate(dataSource).query(sql,
				new ResultSetExtractor<Integer>() {
			@Override
			public Integer extractData(ResultSet results) throws SQLException {
				if (results == null) {
					throw new IllegalStateException("null result set");
				}
				
				while (results.next()) {
					return results.getInt(1);
				}
				return 0;

			}
		});
	}
	
	@SuppressWarnings("deprecation")
	public int getResourceCountByMission(String mission) {
		String sql = "select count(*) "
				   + "from resource r inner join mission_resource mr on r.id = mr.resource_id "
				   + "inner join mission mi on mr.mission_id = mi.id where mi.name = ? ";
		return new JdbcTemplate(dataSource).query(sql, new Object[] { mission },
				new ResultSetExtractor<Integer>() {
			@Override
			public Integer extractData(ResultSet results) throws SQLException {
				if (results == null) {
					throw new IllegalStateException("null result set");
				}
				
				while (results.next()) {
					return results.getInt(1);
				}
				return 0;

			}
		});
	}
	
	@SuppressWarnings("deprecation")
	private List<Resource> getAndParseResourceQuery(String sql, Object[] params){
		return new JdbcTemplate(dataSource).query(sql, params,
				new ResultSetExtractor<List<Resource>>() {
			@Override
			public List<Resource> extractData(ResultSet results) throws SQLException {
				if (results == null) {
					throw new IllegalStateException("null result set");
				}

				List<Resource> values = new ArrayList<Resource>();
				
				while (results.next()) {
					Resource value = parseResult(results);
					values.add(value);
				}

				return values;
			}
		});
		
	}
	
	private String getResourceColumnName(String sort) {
		switch(sort) {
		  case "name":
		    return "name";
		  case "submissionTime":
		     return "submissiontime";
		  case "size":
			  return "octet_length(data)";
		  default:
		    return "";
		}
	}
	
	private Resource parseResult(ResultSet results) {
		Resource value = new Resource();
		try {
			value.setHash(results.getString(1));
			value.setName(results.getString(2));
			value.setSize(results.getLong(3));
			value.setSubmitter(results.getString(4));
			value.setGroupVector(results.getString(5));
			value.setSubmissionTime(results.getTimestamp(6));
			value.setMimeType(results.getString(7));
			value.setExpiration(results.getLong(8));
			value.setUid(results.getString(9));
		} catch (SQLException e) {
			logger.error("Failed parsing SQL: ",e);
		}
		return value;
	}

}
