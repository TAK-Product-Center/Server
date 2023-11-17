package tak.server.retention.service;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LocalQueryService {
    private static final Logger logger = LoggerFactory.getLogger(LocalQueryService.class);

    private static final String DELETE_BY_EXPIRATION = " expiration is not null and expiration > -1 and expiration  <= :expiration";
    private static final String DELETE_BY_TTL = " < now() - (:ttl * INTERVAL '1 second')";
    private static final String WHERE_EXPIRATION_NOT_NULL = " where expiration is not null and expiration > -1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public void setDataSource(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public int deleteCotByTtl(@NotNull Integer ttl) {

        String deleteQuery = "delete from cot_router using cot_router cr left outer join mission_uid mu on mu.uid=cr.uid " +
                "where cot_router.id = cr.id and mu.uid is null and cot_router.cot_type != 'b-t-f' and cot_router.servertime" + DELETE_BY_TTL;

        int count = 0;
        if (ttl == null) {
            logger.info(" delete cot by time to live is null, nothing to do");
            return count;
        }
        MapSqlParameterSource namedParameters = new MapSqlParameterSource("ttl", ttl);
        count = namedParameterJdbcTemplate.update(deleteQuery, namedParameters);

        if (count > 0) {
            logger.info(" deleteCotByTtl, Number of rows deleted  " + count);
        }
        return count;
    }

    public int deleteFilesByExpiration(@NotNull Long expiration) {

        String deleteQuery = "delete from resource where" + DELETE_BY_EXPIRATION;

        if (expiration == null || (expiration.longValue() <= -1)) {
            throw new IllegalArgumentException("invalid expiration: " + expiration);
        }

        MapSqlParameterSource namedParameters = new MapSqlParameterSource("expiration", expiration);
        int count = namedParameterJdbcTemplate.update(deleteQuery, namedParameters);

        if (count > 0) {
            logger.info(" deleteFilesByExpiration,  Number of rows deleted  " + count);
        }
        return count;
    }

    public int deleteFilesByTtl(@NotNull Integer ttl) {

        String deleteQuery = "delete from resource using resource r left outer join mission_resource mr on mr.resource_id=r.id " +
                "where resource.id = r.id and mr.resource_id is null and resource.submissiontime" + DELETE_BY_TTL;
        int count = 0;

        if (ttl == null) {
            logger.info(" delete files by time to live is null, nothing to do");
            return count;
        }
        MapSqlParameterSource namedParameters = new MapSqlParameterSource("ttl", ttl);
        count = namedParameterJdbcTemplate.update(deleteQuery, namedParameters);

        if (count > 0) {
            logger.info("  delete files by time to live, Number of rows deleted  " + count);
        }
        return count;
    }

    public int deleteGeoChatByTtl(@NotNull Integer ttl) {

        String deleteQuery = "delete from cot_router_chat where servertime " + DELETE_BY_TTL;
        int count = 0;

        if (ttl == null) {
            logger.info(" delete geochat by time to live ttl is null, nothing to do");
            return count;
        }

        MapSqlParameterSource namedParameters = new MapSqlParameterSource("ttl", ttl);
        count = namedParameterJdbcTemplate.update(deleteQuery, namedParameters);

        if (count > 0) {
            logger.info(" deleteGeoChatByTtl, Number of rows deleted  " + count);
        }
        return count;
    }

    public int deleteLegacyGeoChatByTtl(@NotNull Integer ttl) {

        String deleteQuery = "delete from cot_router where cot_type = 'b-t-f' and servertime " + DELETE_BY_TTL;
        int count = 0;

        if (ttl == null) {
            logger.debug(" delete legacy geochat from cot_router table, ttl is null, nothing to do");
            return count;
        }

        MapSqlParameterSource namedParameters = new MapSqlParameterSource("ttl", ttl);
        count = namedParameterJdbcTemplate.update(deleteQuery, namedParameters);

        if (count > 0) {
            logger.info(" delete legacy geochat from cot_router table, Number of rows deleted  " + count);
        }
        return count;
    }

    public int deleteMissionPackageByExpiration(@NotNull Long expiration) {

        String deleteQuery = "delete from resource where keywords = '{\"missionpackage\"}' and" + DELETE_BY_EXPIRATION;
        if (expiration == null || (expiration.longValue() <= -1)) {
            throw new IllegalArgumentException("invalid expiration: " + expiration);
        }

        MapSqlParameterSource namedParameters = new MapSqlParameterSource("expiration", expiration);
        int count = namedParameterJdbcTemplate.update(deleteQuery, namedParameters);
        logger.info(" deleteMissionPackageByExpiration, Number of rows deleted  " + count);

        if (logger.isDebugEnabled()) {
            logger.debug(count + " deleteMissionPackageByExpiration, records deleted by expiration " + expiration);
        }
        return count;
    }

//    public int deleteMissionPackageByTtl(@NotNull Integer ttl) {
//
//        String deleteQuery = "delete from resource " +
//                "where keywords = '{\"missionpackage\"}'and submissiontime" + DELETE_BY_TTL;
//
//        int count = 0;
//
//        if (ttl == null) {
//            logger.info(" ttl is null, nothing to do");
//            return count;
//        }
//
//        MapSqlParameterSource namedParameters = new MapSqlParameterSource("ttl", ttl);
//        count = namedParameterJdbcTemplate.update(deleteQuery, namedParameters);
//        logger.info(" deleteMissionPackageByTtl, Number of rows deleted  " + count);
//
//        if (logger.isDebugEnabled()) {
//            logger.debug(count + " deleteMissionPackageByTtl, records deleted by time to live " + ttl);
//        }
//        return count;
//    }
    
    public Map<Map<String,Object>, Timestamp>  getMissionExpirationForArchivalByLatestMissionChanges() {
    	Map<Map<String,Object>, Timestamp> missionToTimestamp = new HashMap<>();

        getAllMissions().forEach(mission-> {        	
        	String selectExpiration = "SELECT * FROM mission_change WHERE mission_id = :mission_id ORDER BY ts DESC LIMIT 1";
            MapSqlParameterSource namedParameters = new MapSqlParameterSource("mission_id", mission.get("id"));
            List<Map<String, Object>> results = namedParameterJdbcTemplate.queryForList(selectExpiration, namedParameters);
            
            if (results.size() > 0) {
            	missionToTimestamp.put(mission, (Timestamp) results.get(0).get("ts"));
            } 
        });
        
        return missionToTimestamp;
    }
    
    public Map<Map<String,Object>, Timestamp> getMissionExpirationForArchivalByLatestSubscribe() {
    	Map<Map<String,Object>, Timestamp> missionToTimestamp = new HashMap<>();

        getAllMissions().forEach(mission-> {        	        	
        	String selectExpiration = "SELECT * FROM mission_subscription WHERE mission_id = :mission_id ORDER BY create_time DESC LIMIT 1";
            MapSqlParameterSource namedParameters = new MapSqlParameterSource("mission_id", mission.get("id"));
            List<Map<String, Object>> results = namedParameterJdbcTemplate.queryForList(selectExpiration, namedParameters);
            
            if (results.size() > 0) {
            	missionToTimestamp.put(mission, (Timestamp) results.get(0).get("create_time"));
            }        
        });
        
        return missionToTimestamp;
    }
    
    private List<Map<String, Object>> getAllMissions() {
        return jdbcTemplate
        	.queryForList("select name, id, groups, create_time, creatoruid from mission")
        	.stream()
        	.filter(mission->!mission.get("name").equals("exchecktemplates") && !mission.get("name").equals("citrap"))
        	.collect(Collectors.toList());
    }

    public List<Map<String, Object>> getResourceExpirations() {
        String selectExpiration = "select hash, expiration from resource" + WHERE_EXPIRATION_NOT_NULL;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(selectExpiration);
        logger.info(" loading resource expirations " + results);
        return results;
    }


    public List<Map<String, Object>> getMissionExpirations() {
        String selectExpiration = "select name, expiration from mission" + WHERE_EXPIRATION_NOT_NULL;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(selectExpiration);
        logger.info(" loading mission expirations " + results);
        return results;
    }

    public List<Map<String, Object>> getDebugResourceTtls() {
        String selectTtl = "select hash, expiration from copyresource" + WHERE_EXPIRATION_NOT_NULL;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(selectTtl);
        return results;
    }

}
