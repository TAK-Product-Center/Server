

package com.bbn.marti.dao.kml;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;
import org.ocpsoft.prettytime.PrettyTime;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.bbn.marti.AltitudeConverter;
import com.bbn.marti.JDBCQueryAuditLogHelper;
//import com.bbn.marti.cache.EhCacheBean;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.remote.UIDResult;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.kml.IconStrategy;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;
import com.google.common.base.Strings;

import tak.server.cot.CotElement;
import tak.server.util.Association;


/*
 * Data Access Object to obtain KML CoT data from database
 * 
 */
public class JDBCCachingKMLDao implements KMLDao {

    private static final Logger slfLogger = LoggerFactory.getLogger(JDBCCachingKMLDao.class);

    private static java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(JDBCQueryAuditLogHelper.class.getCanonicalName());
    
    // strategy pattern class to assign the icon appropriately for KML generation
    // This is autowired because at the moment there is only one implementation of IconStrategy
    @Autowired
    private IconStrategy<CotElement> iconStrategy;

    // the dataSource will be automatically obtained from the spring context
    @Autowired
    private DataSource dataSource;

    @Autowired
    protected AltitudeConverter converter;
    
    private final PrettyTime prettyTimeFormat = new PrettyTime();

    @Autowired
    protected Validator validator;

    protected String servletContextPath = null;
    
    private static final int COURSE_TOKEN_LENGTH = "Course='".length();
    private static final int SPEED_TOKEN_LENGTH = "Speed='".length();
    
    public List<CotElement> getCotElements(String cotType, String groupVector) {
        return getCotElements(cotType, 0, groupVector);
    }

    public IconStrategy<CotElement> getIconStrategy() {
        return iconStrategy;
    }

    public void setIconStrategy(IconStrategy<CotElement> iconStrategy) {
        this.iconStrategy = iconStrategy;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<CotElement> getCotElements(String cotType, int secAgo, String groupVector) {

        if (secAgo < 0) {
            throw new IllegalArgumentException("invalid negative value for secAgo: " + secAgo);
        }

        // ** LatestKML caching temporarily disabled **
        // use the secAgo value for the cache TTL
//        int ttl = secAgo;

        // for secAgo values > 10 minutes, limit the cache TTL to 10 minutes
//        if (secAgo > 600) {
//            slfLogger.debug("secAgo value greater than 10 minutes. Limiting cache TTL to 10 minutes");
//            ttl = 600;
//        } else if (secAgo == 0) {
//            ttl = 60;
//        }

        slfLogger.debug("getCotElements cotType: " + cotType + " secAgo: " + secAgo);

//        String cacheKey = "<List>CotElement_" + cotType + "_" + secAgo;

//        slfLogger.debug("cacheKey: " + cacheKey);

        List<CotElement> cotElements = null;

//        try {
//            cotElements = (List<CotElement>) cache.getCache().get(cacheKey);
//        } catch (Throwable t) {
//            slfLogger.error("error fetching element with key " + cacheKey + " from cache");
//        }

//        if (cotElements != null) {
//            slfLogger.debug("cache hit for key " + cacheKey + " returning cached object");
//
//            AuditLogUtil.auditLog("cache access [" + cacheKey + "]");
//
//            return cotElements;
//        } else {
//            slfLogger.debug("cache miss for key " + cacheKey + " performing database query");

            long start = System.currentTimeMillis();

            // append wildcard for LIKE query
            cotType = cotType + "%";

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            String query = "SELECT uid, ST_X(event_pt), ST_Y(event_pt), "
                    + "point_hae, cot_type, servertime, point_le, detail, id, how, stale "
                    + "FROM cot_router "
                    + "WHERE cot_type LIKE ? " 
                    + " and "
                    + RemoteUtil.getInstance().getGroupClause()
                    + " AND id IN (select MAX(id) FROM cot_router"
                    + (secAgo == 0 ? "" : " where servertime > ? ")
                    + " GROUP BY uid);"; 

            julLogger.fine("Executing " + query);
            slfLogger.debug("KML CoT query: " + query);

            // get all cot elements required for KML, including extracting additional fields from details object
            // the query will be performed using named parameters. All resources (connections, resultsets) will be automatically closed as needed by JdbcTemplate.
            Date timestamp = new Date(System.currentTimeMillis() - (secAgo * 1000L));

            AuditLogUtil.auditLog(query + " [cotType: " + cotType + "] timestamp: " + "[" + timestamp + "]");

            if (secAgo == 0) {
                cotElements = jdbcTemplate.query(query, new Object[] {cotType, groupVector}, new CotElementResultSetExtractor());
            } else {
                cotElements = jdbcTemplate.query(query, new Object[] {cotType, groupVector, timestamp}, new CotElementResultSetExtractor());
            }

            long duration = System.currentTimeMillis() - start;
            slfLogger.debug("latest kml query time " + duration + " ms");

            // cache the query result
//            try {
//                cache.getCache().put(cacheKey, cotElements, ttl);
//            } catch (Throwable t) {
//                slfLogger.error("exception putting element with key " + cacheKey + " ttl " + ttl + " into cache");
//            }

            return cotElements;
//        }
    }
    
    /*
     * Get a list of uids that correspond to images in the database
     * 
     */
    @Override
    public Set<String> getUidsHavingImages(Date start, Date end, String groupsBitVector) {

        if (start == null || end == null) {
            throw new IllegalArgumentException("null start or end");
        }

        // TODO: check whether end is before start
        
        // cache ttl
//        int ttl = 15;

        slfLogger.debug("getUidsHavingImages start: " + start + " end: " + end);
        
        // *Caching uid results temporarily disabled*

//        String cacheKey = "<Set>String_uidsHavingImages_" + start.getTime() + "_" + end.getTime();

//        slfLogger.debug("cacheKey: " + cacheKey);

        Set<String> uidSet = null;

//        try {
//            uidSet = (Set<String>) cache.getCache().get(cacheKey);
//        } catch (Throwable t) {
//            slfLogger.error("error fetching element with key " + cacheKey + " from cache");
//        }

//        if (uidSet != null) {
//            slfLogger.debug("cache hit for key " + cacheKey + " returning cached object");
//
//            AuditLogUtil.auditLog("cache access [" + cacheKey + "]");
//
//            return uidSet;
//        } else {
//            slfLogger.debug("cache miss for key " + cacheKey + " performing database query");

            long st = System.currentTimeMillis();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            String query = "SELECT uid " 
                    + "FROM cot_router "
                    + "r inner join cot_image i on r.id = i.cot_id "
                    + "WHERE servertime BETWEEN ? AND ? and cot_type != 'b-t-f' and cot_type != 'b-f-t-r' and cot_type != 'b-f-t-a'"
                    + RemoteUtil.getInstance().getGroupAndClause(); // include group membership
                       
            julLogger.fine("Executing " + query);

            AuditLogUtil.auditLog(query + " [start: " + start + "] end: " + "[" + end + "] group vector: " + "[" + groupsBitVector + "]");
            
            uidSet = jdbcTemplate.query(query, new Object[] {start, end, groupsBitVector}, new StringSetResultSetExtractor());
            
            long duration = System.currentTimeMillis() - st;
            slfLogger.debug("latest kml query time " + duration + " ms");

            // cache the query result
//            try {
//                cache.getCache().put(cacheKey, uidSet, ttl);
//            } catch (Throwable t) {
//                slfLogger.error("exception putting element with key " + cacheKey + " ttl " + ttl + " into cache");
//            }

            return uidSet;
//        }
    }

    /**
     * Maps the entire domain of database results to a collection of QueryResults, each of which contains an active cot object.
     * 
     * 
     */
    public class CotElementResultSetExtractor implements ResultSetExtractor<List<CotElement>> {

        // returns an empty List<CotElement> or throws an exception, will not return null.
        @Override
        public List<CotElement> extractData(ResultSet resultSet) throws SQLException {

            List<CotElement> cotList = new ArrayList<>();

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            } else if (!resultSet.isBeforeFirst()) {
                //there were no results, so just return an empty set
                return cotList;
            }

            while(resultSet.next()) {
                try {

                    CotElement cotElement = parseFromResultSet(resultSet);

                    if ((cotElement != null) && cotElement.ensure()) {
                        cotList.add(cotElement);
                    }

                } catch (Throwable t) {
                    slfLogger.debug("exception processing CoT row", t);
                }
            }

            return cotList;
        }
    }

    public class CotElementResultExtractor implements ResultSetExtractor<CotElement> {

        // returns an empty List<CotElement> or throws an exception, will not return null.
        @Override
        public CotElement extractData(ResultSet resultSet) throws SQLException {

            CotElement CotElement = null;

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            } else if (!resultSet.isBeforeFirst()) {
                //there were no results, so just return an empty set
                return CotElement;
            }

            if (resultSet.next()) {
                try {
                    CotElement = parseFromResultSet(resultSet);
                } catch (Throwable t) {
                    slfLogger.debug("exception processing CoT row", t);
                }
            }

            return CotElement;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Association<Date, Long>> getCotEventCountByHour(String groupVector, boolean useCache, boolean userGroupsOnly) {

        // cache result for 30 seconds
        int ttl = 30;
        String cacheKey = "cotEventCountByHour";

        slfLogger.debug("cacheKey: " + cacheKey);
        
        List<Association<Date, Long>> counts = null;
//        
//        if (useCache) {
//            try {
//                counts = (List<Association<Date, Long>>) cache.getCache().get(cacheKey);
//            } catch (Throwable t) {
//                slfLogger.error("exception fetching element with key " + cacheKey + " from cache", t);
//            }
//        }

        if (counts != null) {
            slfLogger.debug("cache hit for key " + cacheKey + " returning cached object");

            AuditLogUtil.auditLog("cache access [" + cacheKey + "]");

            return counts;
        } else {
            slfLogger.debug(" performing database query");

            long start = System.currentTimeMillis();
            
            slfLogger.debug("groupVector: " + groupVector);

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            String query;
            
            if (userGroupsOnly) { 
                query = "SELECT servertime_hour, COUNT(1) FROM cot_router where " +
                        RemoteUtil.getInstance().getGroupClause() +
                        " GROUP BY servertime_hour order by servertime_hour;";
            } else {
                query = "SELECT servertime_hour, COUNT(1) FROM cot_router GROUP BY servertime_hour order by servertime_hour;";
            }
            
            julLogger.fine("Executing " + query);
            slfLogger.debug("Hourly CoT event: " + query);

            AuditLogUtil.auditLog(query);
            
            if (userGroupsOnly) {
                counts = jdbcTemplate.query(query, new Object[] {groupVector},new ListAssociationDateLongResultSetExtractor());
            } else {
                counts = jdbcTemplate.query(query, new ListAssociationDateLongResultSetExtractor());
            }

            long duration = System.currentTimeMillis() - start;
            slfLogger.debug("hourly CoT event count query time " + duration + " ms");

//            if (useCache) {
//                // cache the query result
//                try {
//                    cache.getCache().put(cacheKey, counts, ttl);
//                } catch (Throwable t) {
//                    slfLogger.error("exception putting element " + cacheKey + " ttl " + ttl + " into cache", t);
//                }
//            }

            return counts;
        }
    }

    /**
     * Maps query results which are a simple list of integers
     * 
     */
    private class IntegerListResultSetExtractor implements ResultSetExtractor<List<Integer>> {

        // returns an empty List<CotElement> or throws an exception, will not return null.
        @Override
        public List<Integer> extractData(ResultSet resultSet) throws SQLException {

            List<Integer> intList = new ArrayList<>();

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            }

            while(resultSet.next()) {
                try {

                    Integer i = resultSet.getInt(1);

                    if (i != null) {
                        intList.add(i);
                    }

                } catch (Throwable t) {
                    slfLogger.debug("exception processing row", t);
                }
            }

            return intList;
        }
    }
    
    /**
     * Maps query results which are a simple list of strings
     * 
     */
    private class StringListResultSetExtractor implements ResultSetExtractor<List<String>> {

        // returns an empty List<CotElement> or throws an exception, will not return null.
        @Override
        public List<String> extractData(ResultSet resultSet) throws SQLException {

            List<String> list = new ArrayList<>();

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            }

            while(resultSet.next()) {
                try {

                    String s = resultSet.getString(1);

                    if (s != null) {
                        list.add(s);
                    }

                } catch (Throwable t) {
                    slfLogger.debug("exception processing row", t);
                }
            }

            return list;
        }
    }

    /**
     * Maps query results which are a simple list of UIDResult instances
     * 
     */
    private class UIDResultListResultSetExtractor implements ResultSetExtractor<List<UIDResult>> {

        @Override
        public List<UIDResult> extractData(ResultSet resultSet) throws SQLException {

            List<UIDResult> list = new ArrayList<>();

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            }

            while(resultSet.next()) {
                try {

                    String uid = resultSet.getString("uid");
                    String callSign = resultSet.getString("callsign");

                    list.add(new UIDResult(uid, callSign));
                } catch (Throwable t) {
                    slfLogger.debug("exception processing row", t);
                }
            }

            return list;
        }
    }

    /**
     * Maps query results which are a simple set of strings
     * 
     */
    private class StringSetResultSetExtractor implements ResultSetExtractor<Set<String>> {

        // returns an empty List<CotElement> or throws an exception, will not return null.
        @Override
        public Set<String> extractData(ResultSet resultSet) throws SQLException {

            Set<String> set = new HashSet<>();

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            }

            while(resultSet.next()) {
                try {

                    String s = resultSet.getString(1);

                    if (s != null) {
                        set.add(s);
                    }

                } catch (Throwable t) {
                    slfLogger.debug("exception processing row", t);
                }
            }

            return set;
        }
    }

    
    /**
     * Maps query results which are pairs of Date, Long
     * 
     */
    private class ListAssociationDateLongResultSetExtractor implements ResultSetExtractor<List<Association<Date, Long>>> {

        // returns an empty <List<Association<Date, Long>>> or throws an exception, will not return null.
        @Override
        public List<Association<Date, Long>> extractData(ResultSet resultSet) throws SQLException {

            List<Association<Date, Long>> pairList = new ArrayList<>();

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            }

            while(resultSet.next()) {
                try {

                    Association<Date, Long> pair = new Association<>(new Date(resultSet.getTimestamp(1).getTime()), resultSet.getLong(2));

                    pairList.add(pair);

                } catch (Throwable t) {
                    slfLogger.debug("exception processing row", t);
                }
            }

            return pairList;
        }
    }

    /**
     * Maps query results which are a list of Longs
     * 
     */
    private class LongListResultSetExtractor implements ResultSetExtractor<List<Long>> {

        // returns an empty <List<Association<Date, Long>>> or throws an exception, will not return null.
        @Override
        public List<Long> extractData(ResultSet resultSet) throws SQLException {

            List<Long> longList = new ArrayList<>();

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            }

            while(resultSet.next()) {
                try {

                    Long result = resultSet.getLong(1);

                    longList.add(result);
                } catch (Throwable t) {
                    slfLogger.debug("exception processing row", t);
                }
            }

            return longList;
        }
    }

    /**
     * Maps query results which are a list of Longs
     * 
     */
    private class ByteArrayResultSetExtractor implements ResultSetExtractor<byte[]> {

        // returns an empty <List<Association<Date, Long>>> or throws an exception, will not return null.
        @Override
        public byte[] extractData(ResultSet resultSet) throws SQLException {

            byte[] array = new byte[0];

            if (resultSet == null) {
                throw new IllegalArgumentException("null ResultSet");
            }

            while(resultSet.next()) {
                try {
                    array = resultSet.getBytes(1);
                } catch (Throwable t) {
                    slfLogger.debug("exception processing row", t);
                }
            }

            return array;
        }
    }

    /*
     * populate CotElement fields from the detailText field.  
     * 
     */
    public static void ParseDetailText(CotElement cotElement) {

        if (cotElement.detailtext != null) {
            cotElement.hasImage = cotElement.detailtext.contains("image");
            if (cotElement.detailtext.contains("contact") && cotElement.detailtext.contains(" callsign=\"")) {
                cotElement.callsign = cotElement.detailtext.substring(
                        cotElement.detailtext.indexOf(" callsign=\"") + " callsign=\"".length(),
                        cotElement.detailtext.indexOf("\"", cotElement.detailtext.indexOf(" callsign=\"") + " callsign=\"".length()));
            } else if (cotElement.detailtext.contains("contact") && cotElement.detailtext.contains(" callsign=\'")) {
                cotElement.callsign = cotElement.detailtext.substring(
                        cotElement.detailtext.indexOf(" callsign=\'") + " callsign=\'".length(),
                        cotElement.detailtext.indexOf("\'", cotElement.detailtext.indexOf(" callsign=\'") + " callsign=\'".length()));
            }
        }

        //I'd like to use a more structured parsing method below (e.g., DocumentBuilder),
        //but this method is invoked from several threads during processing and the DocumentBuilder class is not thread safe. 
        //Creating a new factory and builder with each invocation would not perform well. In short, we may want to rework the architecture here, 
        //but for now I'll keep it consistent with string based parsing.
        
        int track_start, track_end, course_start, course_end, speed_start, speed_end;
        String raw_course = null, raw_speed = null;
        
        if (cotElement.detailtext != null) {
        	track_start = cotElement.detailtext.indexOf("<track");
        	if (track_start > -1) {
        		track_end = cotElement.detailtext.indexOf("/>", track_start);
        		String track = cotElement.detailtext.substring(track_start, track_end);
        		
        		//Parse course
        		course_start = track.indexOf("course=\"");
        		if (course_start == -1) {
        			course_start = track.indexOf("course='");
        			course_end = track.indexOf("'", course_start + COURSE_TOKEN_LENGTH);
        		} else {
        			course_end = track.indexOf("\"", course_start + COURSE_TOKEN_LENGTH);
        		}
        		if (course_start > -1 && course_end > -1) {
        			raw_course = track.substring(course_start + COURSE_TOKEN_LENGTH, course_end);
        			if (raw_course != null & raw_course.trim().length() > 0) {
        				try {
        					cotElement.course = Double.valueOf(raw_course);
        				} catch (Exception e) {
        					cotElement.course = Double.NaN;
        				}
        			}
        		}

        		//Parse speed
        		speed_start = track.indexOf("speed=\"");
        		if (speed_start == -1) {
        			speed_start = track.indexOf("speed='");
        			speed_end = track.indexOf("'", speed_start + SPEED_TOKEN_LENGTH);
        		} else {
        			speed_end = track.indexOf("\"", speed_start + SPEED_TOKEN_LENGTH);
        		}
        		if (speed_start > -1 && speed_end > -1) {
        			raw_speed = track.substring(speed_start + SPEED_TOKEN_LENGTH, speed_end);
        			if (raw_speed != null & raw_speed.trim().length() > 0) {
        				try {
        					cotElement.speed = Double.valueOf(raw_speed);
        				} catch (Exception e) {
        					cotElement.speed = Double.NaN;
        				}
        			}
        		}
        	}
        }
        
        // parse the detailtext field, get the usericon tag and color tag, deserialize those into objects or fields
        // then assign the appropriate icons and styles
        //        iconStrategy.assignIcon(cotElement);
    }

    @Override
    public void parseDetailText(CotElement cotElement) {
        ParseDetailText(cotElement);
    }

    /* (non-Javadoc)
     * @see com.bbn.marti.service.kml.KMLService#parseFromResultSet(java.sql.ResultSet)
     */
    @Override
    public CotElement parseFromResultSet(ResultSet results) throws SQLException {

        // performance metrics
        long parseTiming = System.nanoTime();
        long hcTiming = 0;
        long dateTiming = 0;
        long validateTiming = 0;
        long parseDetailTiming = 0;
        long iconTiming = 0;

        CotElement cotElement = new CotElement();
        try {
            Double longitude = results.getDouble(2);
            Double latitude = results.getDouble(3);

            cotElement.lat = latitude;
            cotElement.lon = longitude;
            cotElement.hae = Double.toString(results.getDouble(4));  // don't ask why

            if (validator == null) {
                cotElement.uid = results.getString(1);
                cotElement.cottype = results.getString(5);
                cotElement.detailtext = results.getString(8);
                cotElement.how = results.getString(10);
            } else {
                validateTiming = System.nanoTime();
                cotElement.uid = validator.getValidInput(servletContextPath, results.getString(1), MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                cotElement.cottype = validator.getValidInput(servletContextPath, results.getString(5), "CotType", MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                cotElement.detailtext = validator.getValidInput(servletContextPath, results.getString(8), "XmlBlackListWordOnly", MartiValidatorConstants.LONG_STRING_CHARS, true);
                cotElement.how = validator.getValidInput(servletContextPath, results.getString(10), MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                validateTiming = System.nanoTime() - validateTiming;
            }

            try {
                hcTiming = System.nanoTime();
                cotElement.msl = converter.haeToMsl(Double.parseDouble(cotElement.hae), cotElement.lat, cotElement.lon);     
                hcTiming = System.nanoTime() - hcTiming;
            } catch (Throwable t) {
                slfLogger.debug("exception converting hae to msl", t);
            }

            try {
                cotElement.geom = cotElement.lon + "," + cotElement.lat + (!Double.isNaN(cotElement.msl) ? "," + cotElement.msl : "");
            } catch (Throwable t) {
                slfLogger.debug("exception setting geom", t);
            }

            try {
                cotElement.servertime = results.getTimestamp(6);
                cotElement.staletime = results.getTimestamp(11);

                dateTiming = System.nanoTime();
                cotElement.prettytime = prettyTimeFormat.format(new Date(cotElement.servertime.getTime()));
                dateTiming = System.nanoTime() - dateTiming;
            } catch (Throwable t) {
                slfLogger.debug("exception getting pretty formatted date", t);
            }

            try {
                iconTiming = System.nanoTime();
                iconStrategy.assignIcon(cotElement);
                iconTiming = System.nanoTime() - iconTiming;
            } catch (Throwable t) {
                slfLogger.debug("exception assigning icon", t);
            }

            try {
                parseDetailTiming = System.nanoTime();
                parseDetailText(cotElement);
                parseDetailTiming = System.nanoTime() - parseDetailTiming;
            } catch (Throwable t) {
                slfLogger.debug("exception parsing detail text", t);
            }

            if (validator != null) {
                validateTiming = System.nanoTime();
                cotElement.uid = validator.getValidInput(servletContextPath, cotElement.uid, MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                cotElement.cottype = validator.getValidInput(servletContextPath, cotElement.cottype, "CotType", MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                cotElement.detailtext = validator.getValidInput(servletContextPath, cotElement.detailtext, "XmlBlackListWordOnly", MartiValidatorConstants.LONG_STRING_CHARS, true);
                validateTiming = System.nanoTime() - validateTiming;
            }

            try {
                cotElement.cotId = results.getLong(9);
            } catch (Exception e) {
                slfLogger.debug("exception getting cotId", e);
            }

        } catch (ValidationException e) {
            slfLogger.warn("validation exception processing CoT query result", e);
            return null;
        } catch (Exception excpt) {
            slfLogger.warn("exception processing CoT query result", excpt);
            return null;
        }

        parseTiming = System.nanoTime() - parseTiming;
        slfLogger.trace("CoT parse time (ns): " + parseTiming + " " + " date parse timing: " + dateTiming + " hae conversion timing: " + hcTiming + " validation timing: " + validateTiming + " parse detail text timing: " +  parseDetailTiming + " icon timing: " + iconTiming);

        return cotElement;
    }

    @Override
    public CotElement deserialize(ResultSet results) throws SQLException {

        // performance metrics
        long parseTiming = System.nanoTime();

        // hae, geom, prettytime, parsedetailText
        CotElement cotElement = new CotElement();
        try {
            cotElement.lon = results.getDouble(2);
            cotElement.lat = results.getDouble(3);

            if (cotElement.lon == 0 && cotElement.lat == 0 && !(results.getString(5).equals("b-m-r"))) {
                // filter out non-routes at 0,0
                return null;
            }

            cotElement.hae = results.getString(4);
            slfLogger.trace("parsed hae: " + cotElement.hae);

            cotElement.le = results.getDouble(7);
            cotElement.servertime = results.getTimestamp(6);

            cotElement.uid = results.getString(1);
            cotElement.cottype = results.getString(5);
            cotElement.detailtext = results.getString(8);
            cotElement.ce = results.getDouble(9);

        } catch (Exception excpt) {
            slfLogger.warn("exception processing CoT query result", excpt);
            return null;
        }

        parseTiming = System.nanoTime() - parseTiming;
        slfLogger.trace("CoT deserialize parse time (ns): " + parseTiming);

        return cotElement;
    }

    @Override
    public CotElement parse(CotElement cotElement) throws SQLException {

        slfLogger.trace("parsing CotElement: " + cotElement);

        // performance metrics
        long parseTiming = System.nanoTime();
        long hcTiming = 0;
        long dateTiming = 0;
        long validateTiming = 0;
        long parseDetailTiming = 0;
        long iconTiming = 0;

        try {

            try {
                hcTiming = System.nanoTime();
                cotElement.msl = converter.haeToMsl(Double.parseDouble(cotElement.hae), cotElement.lat, cotElement.lon);     
                hcTiming = System.nanoTime() - hcTiming;
            } catch (Throwable t) {
                slfLogger.debug("exception converting hae to msl", t);
            }

            try {
                cotElement.geom = cotElement.lon + "," + cotElement.lat + (!Double.isNaN(cotElement.msl) ? "," + cotElement.msl : "");
            } catch (Throwable t) {
                slfLogger.debug("exception setting geom", t);
            }

            try {
                dateTiming = System.nanoTime();
                cotElement.prettytime = prettyTimeFormat.format(new Date(cotElement.servertime.getTime()));
                dateTiming = System.nanoTime() - dateTiming;
            } catch (Throwable t) {
                slfLogger.debug("exception getting pretty formatted date", t);
            }

            try {
                iconTiming = System.nanoTime();
                iconStrategy.assignIcon(cotElement);
                iconTiming = System.nanoTime() - iconTiming;
            } catch (Throwable t) {
                slfLogger.debug("exception assigning icon", t);
            }

            try {
                parseDetailTiming = System.nanoTime();
                parseDetailText(cotElement);
                parseDetailTiming = System.nanoTime() - parseDetailTiming;
            } catch (Throwable t) {
                slfLogger.debug("exception parsing detail text", t);
            }

            if (validator != null) {
                validateTiming = System.nanoTime();
                cotElement.uid = validator.getValidInput(servletContextPath, cotElement.uid, MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                cotElement.cottype = validator.getValidInput(servletContextPath, cotElement.cottype, "CotType", MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                cotElement.detailtext = validator.getValidInput(servletContextPath, cotElement.detailtext, "XmlBlackListWordOnly", MartiValidatorConstants.LONG_STRING_CHARS, true);
                validateTiming = System.nanoTime() - validateTiming;
            }

        } catch (Exception excpt) {
            slfLogger.warn("exception processing CoT query result", excpt);
            return null;
        }

        parseTiming = System.nanoTime() - parseTiming;

        slfLogger.trace("parsed CotElement: " + cotElement);

        slfLogger.debug("CoT parse time (ns): " + parseTiming + " " + " date parse timing: " + dateTiming + " hae conversion timing: " + hcTiming + " validation timing: " + validateTiming + " parse detail text timing: " +  parseDetailTiming + " icon timing: " + iconTiming);

        return cotElement;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> getImageCotIdsByCallsign(String callsign, int limit) {

        // 60 second ttl for this query
        int TTL = 60;

        if (Strings.isNullOrEmpty(callsign) || limit < 1) {
            throw new IllegalArgumentException("invalid callsign " + callsign + " or limit " + limit);
        }

        slfLogger.debug("getCotIdsByCallsign callsign: " + callsign + " limit: " + limit);

        String cacheKey = "<List>Integer_cotids_by_callsign_" + callsign + "_" + limit;

        slfLogger.debug("cacheKey: " + cacheKey);

        List<Integer> cotIds = new ArrayList<>();

//        try {
//            cotIds = (List<Integer>) cache.getCache().get(cacheKey);
//        } catch (Throwable t) {
//            slfLogger.error("error fetching element with key " + cacheKey + " from cache");
//        }

        if (cotIds != null) {
            slfLogger.debug("cache hit for key " + cacheKey + " returning cached object");

            AuditLogUtil.auditLog("cache access [" + cacheKey + "]");

            return cotIds;
        } else {
            slfLogger.debug("cache miss for key " + cacheKey + " performing database query");

            long start = System.currentTimeMillis();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // only select cot_router routers that correspond to images
            String query = "select cr.id as id from cot_router cr inner join cot_image ci on cr.id = ci.cot_id where ((xpath('/detail/contact/@callsign', detail::xml))[1])::text = ? order by cr.id desc limit ?";

            julLogger.fine("Executing " + query);
            slfLogger.debug("CoT ids query for callsign: " + query);

            AuditLogUtil.auditLog(query + " [callsign: " + callsign + "] limit: " + "[" + limit + "]");

            cotIds = jdbcTemplate.query(query, new Object[] {callsign, limit}, new IntegerListResultSetExtractor());

            long duration = System.currentTimeMillis() - start;
            slfLogger.debug("cotids by callsign query time " + duration + " ms");

            // cache the query result
//            try {
//                cache.getCache().put(cacheKey, cotIds, TTL);
//            } catch (Throwable t) {
//                slfLogger.error("exception putting element with key " + cacheKey + " ttl " + TTL + " into cache");
//            }

            return cotIds;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> getImageCotIdsByUid(String uid, int limit) {

        // 60 second ttl for this query
        int TTL = 60;

        if (Strings.isNullOrEmpty(uid) || limit < 1) {
            throw new IllegalArgumentException("invalid uid " + uid + " or limit " + limit);
        }

        slfLogger.debug("getImageCotIdsByUid uid: " + uid + " limit: " + limit);

        String cacheKey = "<List>Integer_cotids_by_uid_" + uid + "_" + limit;

        slfLogger.debug("cacheKey: " + cacheKey);

        List<Integer> cotIds = new ArrayList<>();

//        try {
//            cotIds = (List<Integer>) cache.getCache().get(cacheKey);
//        } catch (Throwable t) {
//            slfLogger.error("error fetching element with key " + cacheKey + " from cache");
//        }

        if (cotIds != null) {
            slfLogger.debug("cache hit for key " + cacheKey + " returning cached object");

            AuditLogUtil.auditLog("cache access [" + cacheKey + "]");

            return cotIds;
        } else {
            slfLogger.debug("cache miss for key " + cacheKey + " performing database query");

            long start = System.currentTimeMillis();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // only select cot_router routers that correspond to images
            String query = "select cr.id as id from cot_router cr inner join cot_image ci on cr.id = ci.cot_id where cr.uid = ? order by cr.id desc limit ?";

            julLogger.fine("Executing " + query);
            slfLogger.debug("CoT ids query for callsign: " + query);

            AuditLogUtil.auditLog(query + " [uid: " + uid + "] limit: " + "[" + limit + "]");

            cotIds = jdbcTemplate.query(query, new Object[] {uid, limit}, new IntegerListResultSetExtractor());

            long duration = System.currentTimeMillis() - start;
            slfLogger.debug("cotids by uid query time " + duration + " ms");

//            // cache the query result
//            try {
//                cache.getCache().put(cacheKey, cotIds, TTL);
//            } catch (Throwable t) {
//                slfLogger.error("exception putting element with key " + cacheKey + " ttl " + TTL + " into cache");
//            }

            return cotIds;
        }
    }

    @Override
    public byte[] getImageBytesByCotId(Integer cotId) {
        // ttl - last forever
        int TTL = 0; 

        if (cotId == null || cotId < 1) {
            throw new IllegalArgumentException("invalid cotId " + cotId);
        }

        slfLogger.debug("getImageBytesByCotId cotId: " + cotId);

        String cacheKey = "byte[]_image_cotId_" + cotId;

        slfLogger.debug("cacheKey: " + cacheKey);

        byte[] imageBytes = null;

//        try {
//            imageBytes = (byte[]) cache.getCache().get(cacheKey);
//        } catch (Throwable t) {
//            slfLogger.error("error fetching element with key " + cacheKey + " from cache");
//        }

        if (imageBytes != null) {
            slfLogger.debug("cache hit for key " + cacheKey + " returning cached object");

            AuditLogUtil.auditLog("cache access [" + cacheKey + "]");

            return imageBytes;
        } else {
            slfLogger.debug("cache miss for key " + cacheKey + " performing database query");

            long start = System.currentTimeMillis();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            String query = "select image.image from cot_image image, cot_router cot where image.cot_id = ? and cot.id = ?";

            slfLogger.debug("get image for cotId: " + cotId);

            AuditLogUtil.auditLog(query + " [cotId: " + cotId + "]");

            imageBytes = jdbcTemplate.query(query, new Object[] {cotId, cotId}, new ByteArrayResultSetExtractor());

            long duration = System.currentTimeMillis() - start;
            slfLogger.debug("cotids by callsign query time " + duration + " ms");

            // cache the query result
//            try {
//                cache.getCache().put(cacheKey, imageBytes, TTL);
//            } catch (Throwable t) {
//                slfLogger.error("exception putting element with key " + cacheKey + " ttl " + TTL + " into cache");
//            }

            return imageBytes;
        }
    }

    @Override
    public List<UIDResult> searchUIDs(Date startDate, Date endDate) {

        List<UIDResult> results = null;

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String query = "select distinct cr.uid, unnest(xpath('//detail/contact/@callsign', xmlparse(content cr.detail)))::TEXT as callsign " +
        		"from cot_router cr " +
        		"where cr.time >= ? and cr.time <= ? " +
        		"order by callsign asc";
        
        results = jdbcTemplate.query(query, new Object[] {startDate, endDate}, new UIDResultListResultSetExtractor());

        return results;
    }

    @Override
    public @NotNull String latestCallsign(String uid) {
        if (uid == null) { return ""; }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String query = "SELECT callsign FROM client_endpoint WHERE uid= ? ORDER BY id DESC";
        List<String> results = jdbcTemplate.query(query, new Object[] { uid }, new StringListResultSetExtractor());
        if (results != null && !results.isEmpty()) {
            return results.get(0);
        }
        return "";
    }
}
