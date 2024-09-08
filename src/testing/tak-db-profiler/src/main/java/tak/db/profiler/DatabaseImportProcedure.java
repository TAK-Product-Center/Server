package tak.db.profiler;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.joda.time.DateTime;
import javax.sql.DataSource;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.DatatypeConverter;
import tak.db.profiler.table.CotRouterTable;
import tak.db.profiler.table.CotRouterTable.Cot;
import tak.db.profiler.table.GroupsTable;
import tak.db.profiler.table.MissionResourceTable;
import tak.db.profiler.table.MissionTable;
import tak.db.profiler.table.MissionTable.Mission;
import tak.db.profiler.table.MissionUidTable;
import tak.db.profiler.table.ResourceTable;
import tak.db.profiler.table.ResourceTable.Resource;
import tak.db.profiler.table.DatabaseStatistics;

public class DatabaseImportProcedure {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseImportProcedure.class);
	public static final int GROUPS_BIT_VECTOR_LEN = 32768;
	private static final Calendar utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

	private static Random random = new Random();
	
	@Autowired
	DataSource dataSource;
	
	@Autowired
	@Qualifier("databaseStatisticsDB")
	DatabaseStatistics databaseStatisticsDB;
	
	@Autowired
	@Qualifier("databaseStatisticsFile")
	DatabaseStatistics databaseStatisticsFile;
	
	@Autowired
	@Qualifier("groupsTableFile")
	GroupsTable groupsTableFile;
	
	@Autowired
	@Qualifier("cotRouterTableFile")
	CotRouterTable cotRouterTableFile;
	
	@Autowired
	@Qualifier("resourceTableFile")
	ResourceTable resourceTableFile;
	
	@Autowired
	@Qualifier("missionTableFile")
	MissionTable missionTableFile;

	@Autowired
	@Qualifier("missionResourceTableFile")
	MissionResourceTable missionResourceTableFile;
	
	@Autowired
	@Qualifier("missionUidTableFile")
	MissionUidTable missionUidTableFile;
	
	@PostConstruct
	public void init() {
		checkIfDBIsEmpty();
		
		insertGroups();
		
		insertCot();
		insertResources();
		
		insertMissions();
		
		insertMissionUids();
		insertMissionResouces();
	}

	private void insertMissionResouces() {
		missionResourceTableFile.getMissionResources().forEach(res -> {
			try (Connection conn = dataSource.getConnection()) {
				PreparedStatement missionResourceInsert = conn.prepareStatement("INSERT INTO mission_resource"
						+ " (mission_id, resource_id, resource_hash) VALUES "
						+ "(?,(select id from resource where id = 1),(select hash from resource where id = 1) ) ");
				
				missionResourceInsert.setInt(1, res.getMissionId());
				
				missionResourceInsert.execute();
			} catch (SQLException e) {
				logger.info("exception executing mission resource insert ", e);
			}
		});
	}

	private void insertMissionUids() {
		missionUidTableFile.getMissionCots().forEach(cot -> {
			
			try (Connection conn = dataSource.getConnection()) {
				ResultSet rs = conn.prepareStatement("SELECT COUNT(*) FROM cot_router").executeQuery();
				long cotRouterCount = 0;
		   		while (rs.next()) {
		   			cotRouterCount = rs.getLong(1);
		   		}
						   		
		   		Random rand = new Random();
		   		Set<Long> cotIds = new HashSet<>();
		   		
		   		while(cotIds.size() != cot.getCount()) {
		   			long randomNum = rand.longs(1, 101l, 101l + cotRouterCount).findFirst().getAsLong();
		   			cotIds.add(randomNum);
		   		}
		   		
		   		for (long id: cotIds) {
		   			PreparedStatement missionUidInsert = conn.prepareStatement("INSERT INTO mission_uid"
							+ " (mission_id, uid) VALUES "
							+ "(?, (select uid from cot_router where id = ?) ) ");
					
					missionUidInsert.setInt(1, cot.getMissionId());
					missionUidInsert.setLong(2, id);
					
					missionUidInsert.execute();
		   		}
			} catch (SQLException e) {
				logger.info("exception executing mission uid insert ", e);
			}
		});
	}

	private void insertMissions() {
		missionTableFile.getMissions().forEach(mission -> {
			try (Connection conn = dataSource.getConnection()) {
				PreparedStatement missionInsert = conn.prepareStatement("INSERT INTO mission"
						+ " (id, name, create_time, tool, default_role_id, guid, groups) VALUES "
						+ "(?,?,?,?,?,?,(?)::bit(" + GROUPS_BIT_VECTOR_LEN + ") ) ");
				
				setMissionQueryParams(missionInsert, mission);
				
				missionInsert.execute();
				conn.prepareStatement("select nextval('mission_id_seq');").execute();
			} catch (SQLException e) {
				logger.info("exception executing mission insert ", e);
			}
		});
	}

	private void insertResources() {
		resourceTableFile.getResources().forEach(res -> {
			try (Connection conn = dataSource.getConnection()) {
				PreparedStatement resourceInsert = conn.prepareStatement("INSERT INTO resource"
						+ " (hash, filename, name, mimetype, keywords, expiration, groups, data) VALUES "
						+ "(?,?,?,?,?,?,(?)::bit(" + GROUPS_BIT_VECTOR_LEN + "),? ) ");
				
				setResourceQueryParams(conn, resourceInsert, res);
				
				resourceInsert.execute();
				
			} catch (SQLException e) {
				logger.info("exception executing resource insert ", e);
			}
		});
	}

	private void insertCot() {
		AtomicLong cotUidCounter = new AtomicLong(1);
		cotRouterTableFile.getCotMessages().forEach(cot -> {
			try (Connection conn = dataSource.getConnection()) {
				for (int i = 0; i < cot.getCount(); i++) {
					PreparedStatement cotRouterInsert = conn.prepareStatement("INSERT INTO cot_router"
							+ " (uid, event_pt, cot_type, "
							+ "start, time, stale, detail, "
							+ "access, qos, opex, "
							+ "how, point_hae, point_ce, point_le, groups, "
							+ "id, servertime, caveat, releaseableto) VALUES "
							+ "(?,ST_GeometryFromText(?, 4326),?,?,?,?,?,?,?,?,?,?,?,?,(?)::bit(" + GROUPS_BIT_VECTOR_LEN + "), nextval('cot_router_seq'),?,?,?) ");
					
					setCotQueryParams(cotRouterInsert, cot, cotUidCounter.getAndIncrement());
					
					cotRouterInsert.execute();
				}	
			} catch (SQLException e) {
				logger.info("exception executing CoT insert ", e);
			}
		});
	}

	private void insertGroups() {
		groupsTableFile.getGroups().forEach(group -> {
			// try to create group
			try {
				JdbcTemplate template = new JdbcTemplate(dataSource);
				Integer bitpos = null;

				// allocate a bit position
				bitpos = template.query("update group_bitpos_sequence set bitpos = bitpos + 1 returning bitpos + 1",
						new ResultSetExtractor<Integer>() {
							@Override
							public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
								// group exists, return it
								if (rs.next()) {
									return rs.getInt(1);
								} else {
									throw new IllegalStateException("unable to increment bit position");
								}
							}
						});

				template.update("insert into groups (name, bitpos, create_ts, type) values (?, ?, now(), ?)",
						new Object[] { group, bitpos, 1 }); 
			} catch (Exception e) {
				logger.info("exception saving group " + group + " " + e.getMessage(), e);
			}
		});
	}

	private void checkIfDBIsEmpty() {
		databaseStatisticsDB.getDatabaseStatistics().forEach(table -> {
			if (table.getTableName().equals("cot_router") && table.getRowCount() > 0) {
				logger.info("*** Database is not empty, aborting! ***");
				Runtime.getRuntime().halt(0);
			}
			if (table.getTableName().equals("groups") && table.getRowCount() > 0) {
				logger.info("*** Database is not empty, aborting! ***");
				Runtime.getRuntime().halt(0);
			}
		});
	}
	
	private void setMissionQueryParams(PreparedStatement ps, Mission mission) throws SQLException {
		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();
		String serverTime = formatter.print(new Date().getTime());
		
		ps.setInt(1, mission.getId());
		ps.setString(2, mission.getName());
		ps.setTimestamp(3, new Timestamp(DatatypeConverter.parseDateTime(serverTime).getTimeInMillis()), utcCalendar);
		ps.setString(4, "public");
		ps.setInt(5, 2);
		ps.setObject(6, UUID.randomUUID());
		
		char[] groupsBitVector = new char[32768];
		Arrays.fill(groupsBitVector, '0');
		mission.getGroups().forEach(g -> {
			int group = Integer.valueOf(g);
			groupsBitVector[group] = '1';
		});
		
		String groups = String.valueOf(groupsBitVector);
		ps.setString(7, new StringBuilder(groups).reverse().toString());
	}
	
	private void setCotQueryParams(PreparedStatement ps, Cot cot, long uid) throws SQLException {
		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();
		String serverTime = formatter.print(new Date().getTime());
		
		ps.setString(1, String.valueOf(uid));
		int lat = random.nextInt(90 + 1 - -90) + -90;
		int lon = random.nextInt(180 + 1 - -180) + -180;
		ps.setString(2, "POINT(" + lat + " " + lon + ")");
		ps.setString(3, cot.getCotType());

		//start
		ps.setTimestamp(4, new Timestamp(DatatypeConverter.parseDateTime(serverTime).getTimeInMillis()), utcCalendar);
		//time
		ps.setTimestamp(5, new Timestamp(DatatypeConverter.parseDateTime(serverTime).getTimeInMillis()), utcCalendar);
		//stale
		ps.setTimestamp(6, new Timestamp(DatatypeConverter.parseDateTime(serverTime).getTimeInMillis()), utcCalendar);

		ps.setString(7, "");
		ps.setString(8, "");
		ps.setString(9, "");
		ps.setString(10, "");
		ps.setString(11, "m-g");
		ps.setDouble(12, 9999999.0);
		ps.setDouble(13, 9999999.0);
		ps.setDouble(14, 9999999.0);

		char[] groupsBitVector = new char[32768];
		Arrays.fill(groupsBitVector, '0');
		cot.getGroups().forEach(g -> {
			int group = Integer.valueOf(g);
			groupsBitVector[group] = '1';
		});
		
		String groups = String.valueOf(groupsBitVector);
		ps.setString(15, new StringBuilder(groups).reverse().toString());

		ps.setTimestamp(16, new Timestamp(DatatypeConverter.parseDateTime(serverTime).getTimeInMillis()), utcCalendar);

		ps.setString(17, "");
		ps.setString(18, "");
	}
	
	private void setResourceQueryParams(Connection conn, PreparedStatement ps, Resource res) throws SQLException {
		// generate fake data matching close to the original size
		String content = generateRandomString(res.getSize());
		byte[] contentB = content.getBytes();
		
		MessageDigest msgDigest = null;
		StringBuffer hash = new StringBuffer();
		try {
			msgDigest = MessageDigest.getInstance("SHA-256");
			msgDigest.update(contentB);
			byte[] mdbytes = msgDigest.digest();
			for (int i = 0; i < mdbytes.length; i++) {
				hash.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
		} catch (NoSuchAlgorithmException e) {}
		
		String filename = generateRandomString(16);
		String mimetype;
		byte[] data = new byte[0];
		// handle resource vs mission package
		if (res.getKeywords().contains("missionpackage")) {
			mimetype = "application/zip";
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            BufferedOutputStream bos = new BufferedOutputStream(baos);
	            ZipOutputStream zos = new ZipOutputStream(bos);

	            zos.putNextEntry(new ZipEntry(filename + ".txt"));
	            zos.write(contentB);
	            zos.closeEntry();
	            
	            zos.close();
	            
	            data = baos.toByteArray();
			} catch (Exception e) {
				logger.error("exception creating mission package", e);
			}
			filename = filename + ".zip";
		} else {
			filename = filename + ".txt";
			mimetype = "text/plain";
			data = contentB;
		}
				
		ps.setString(1, hash.toString());
		ps.setString(2, filename);
		ps.setString(3, filename);
		ps.setString(4, mimetype);
		
		java.sql.Array keywords = conn.createArrayOf("VARCHAR", res.getKeywords().toArray());
		ps.setArray(5, keywords);
		ps.setLong(6, res.getExpiration());
		
		char[] groupsBitVector = new char[32768];
		Arrays.fill(groupsBitVector, '0');
		res.getGroups().forEach(g -> {
			int group = Integer.valueOf(g);
			groupsBitVector[group] = '1';
		});
		
		String groups = String.valueOf(groupsBitVector);
		ps.setString(7, new StringBuilder(groups).reverse().toString());
		ps.setBytes(8, data);
	}
	
	private String generateRandomString(long sizeB) {
		String candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
	    StringBuilder sb = new StringBuilder();
	    Random random = new Random();
	    for (int i = 0; i < sizeB; i++) {
	        sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
	    }

	    return sb.toString();
	}
}
