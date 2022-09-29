package com.bbn.marti.citrap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.owasp.esapi.errors.ValidationException;
import org.postgresql.geometric.PGbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.JDBCQueryAuditLogHelper;
import com.bbn.marti.citrap.reports.ReportType;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;

public class PersistenceStore {

    private static final Logger logger = LoggerFactory.getLogger(com.bbn.marti.citrap.PersistenceStore.class);
    private static final String NEW_SHIFT_LONGITUDE_NAME = "ST_ShiftLongitude";
    private static final String OLD_SHIFT_LONGITUDE_NAME = "ST_Shift_Longitude";
    private static final String NEW_DISTANCE_SPHERE_NAME = "ST_DistanceSphere";
    private static final String OLD_DISTANCE_SPHERE_NAME = "ST_Distance_Sphere";

    private String SHIFT_LONGITUDE_NAME;
    private String DISTANCE_SPHERE_NAME;

    @Autowired
    private JDBCQueryAuditLogHelper wrapper;
    
    @Autowired
    private DataSource ds;
    
    @Autowired
    private MartiValidator validator;

	@EventListener({ContextRefreshedEvent.class})
    private void initDbFunctionNames() {
        SHIFT_LONGITUDE_NAME = initDbFunction(NEW_SHIFT_LONGITUDE_NAME, OLD_SHIFT_LONGITUDE_NAME);
        if (logger.isDebugEnabled()) {
            logger.debug("ci trap set shift longitude function name to " + SHIFT_LONGITUDE_NAME);
        }

        DISTANCE_SPHERE_NAME = initDbFunction(NEW_DISTANCE_SPHERE_NAME, OLD_DISTANCE_SPHERE_NAME);
    	if (logger.isDebugEnabled()) {
            logger.debug("ci trap set distance sphere function name to " + DISTANCE_SPHERE_NAME);
    	}
    }

    private String initDbFunction(String newName, String oldName) {

        // check to see if the old function exists
        String sql = "select * from pg_proc where proname = '" + oldName.toLowerCase() + "'";

        try (Connection connection = ds.getConnection(); PreparedStatement select = wrapper.prepareStatement(sql, connection)) {
            try (ResultSet results = wrapper.doQuery(select)) {
                if (results.next()) {
                    return oldName;
                }
            }
        } catch (Exception e) {
            logger.error("Exception trying to determine " + newName + " function name", e);
        }

        return newName;
    }

    private void validateReport(ReportType report) throws ValidationException {
        validator.getValidInput("citrap", report.getId(),
        		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);
        validator.getValidInput("citrap", report.getType(),
        		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);
        validator.getValidInput("citrap", report.getDateTime(),
        		MartiValidatorConstants.Regex.Timestamp.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);
// TODO add validation rule for WKT
//        validator.getValidInput("citrap", report.getLocation(),
//                MartiValidatorConstants.Regex.Coordinates.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);
        validator.getValidInput("citrap", report.getEventScale(),
        		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);
        validator.getValidInput("citrap", report.getImportance(),
        		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);
    }


    public int createReport(ReportType report, String reportXml, String groupVector) throws Exception {
        validateReport(report);

        try (Connection connection = ds.getConnection(); PreparedStatement insert = wrapper.prepareStatement(
        		"insert into ci_trap ( uid, type, user_callsign, user_description, " +
        				"date_time,  date_time_description, location, location_description, " +
        				"event_scale, importance, xml, groups, title  " +
        				") values ( ?, ?, ?, ?, ?, ?, ST_SetSRID(ST_GeomFromText(?), 4326), ?, ?, ?, ?, " +
        				"?" + RemoteUtil.getInstance().getGroupType() +
        		", ?)", connection)) {

        	insert.setString(1, report.getId());
        	insert.setString(2, report.getType());
        	insert.setString(3, report.getUserCallsign());
        	insert.setString(4, report.getUserDescription());
        	Date dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(report.getDateTime());
        	insert.setTimestamp(5, new Timestamp(dateTime.getTime()));
        	insert.setString(6, report.getDateTimeDescription());
        	insert.setString(7, report.getLocation());
        	insert.setString(8, report.getLocationDescription());
        	insert.setString(9, report.getEventScale());
        	insert.setString(10, report.getImportance());
        	insert.setString(11, reportXml);
        	insert.setString(12, groupVector);
        	insert.setString(13, report.getTitle());

        	int created = insert.executeUpdate();
        	insert.close();
        	return created;
        }
    }

    public int updateReport(ReportType report, String reportXml, String groupVector) throws Exception {
        validateReport(report);

        try (Connection connection = ds.getConnection(); PreparedStatement update = wrapper.prepareStatement(
        		"update ci_trap set " +
        				"type = ?, " +
        				"user_callsign = ?, " +
        				"user_description = ?, " +
        				"date_time = ?,  " +
        				"date_time_description = ?, " +
        				"location = ST_SetSRID(ST_GeomFromText(?), 4326), " +
        				"location_description = ?, " +
        				"event_scale = ?, " +
        				"importance = ?, " +
        				"xml = ?, " +
        				"groups = ?" + RemoteUtil.getInstance().getGroupType() + ", " +
        				"title = ? " +
        		" where uid = ?", connection)) {

        	update.setString(1, report.getType());
        	update.setString(2, report.getUserCallsign());
        	update.setString(3, report.getUserDescription());
        	Date dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(report.getDateTime());
        	update.setTimestamp(4, new Timestamp(dateTime.getTime()));
        	update.setString(5, report.getDateTimeDescription());
        	update.setString(6, report.getLocation());
        	update.setString(7, report.getLocationDescription());
        	update.setString(8, report.getEventScale());
        	update.setString(9, report.getImportance());
        	update.setString(10, reportXml);
        	update.setString(11, groupVector);
        	update.setString(12, report.getTitle());
        	update.setString(13, report.getId());

        	int updated = update.executeUpdate();
        	update.close();
        	return updated;
        }
    }

    public Double[] getCentroid(String id) throws Exception {
    	validator.getValidInput("citrap", id,
    			MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);

    	try (Connection connection = ds.getConnection(); PreparedStatement select = wrapper.prepareStatement("with centroid as ( " +
    			"select ST_Centroid(location) as center from ci_trap where uid = ? ) " +
    			"select ST_X(center) as longitude, ST_Y(center) as latitude from centroid", connection)) {
    		select.setString(1, id);

    		try (ResultSet results = wrapper.doQuery(select)) {
    			Double[] lonlat = new Double[2];
    			if (results.next()) {
    				lonlat[0] = results.getDouble("longitude");
    				lonlat[1] = results.getDouble("latitude");
    			}
    			return lonlat;
    		}
    	}
    }

    public ReportType errorReportFromResults(ResultSet results) throws Exception {
        ReportType report = new ReportType();
        report.setId(results.getString("uid"));
        report.setType(results.getString("type"));
        report.setTitle(results.getString("title"));

        report.setUserCallsign(results.getString("user_callsign"));
        report.setUserDescription(results.getString("user_description"));

        Date dateTime = new Date();
        dateTime.setTime(results.getTimestamp("date_time").getTime());
        report.setDateTime(new SimpleDateFormat(tak.server.Constants.COT_DATE_FORMAT).format(dateTime));

        report.setDateTimeDescription(results.getString("date_time_description"));
        report.setLocation(results.getString("location_text"));
        report.setLocationDescription(results.getString("location_description"));
        report.setEventScale(results.getString("event_scale"));
        report.setImportance(results.getString("importance"));

        try {
            String xml = results.getString("xml");
            ReportType temp = CITrapReportService.deserializeReport(xml);
            report.setStatus(temp.getStatus());
        } catch (Exception e) {
            logger.error("Unable to set report status!", e);
        }

        return report;
    }

    private StringBuilder addWhereOrAnd(StringBuilder sb) {
        if (sb.length() == 0) {
            sb.append(" where ");
        } else {
            sb.append(" and ");
        }
        return sb;
    }

    public List<ReportType> getReports(
            String groupVector, String keywords, PGbox bbox,
            Timestamp startTime, Timestamp endTime, String maxReportCount,
            String type, String callsign) throws Exception {

        List<ReportType> reports = new ArrayList<ReportType>();
        StringBuilder query = new StringBuilder();
        List<Object> queryParameters = new LinkedList<Object>();

        if (keywords != null) {
            addWhereOrAnd(query);

            query.append(" ( ");
            String[] keywordArray = keywords.split(",");
            for (int i=0; i < keywordArray.length; i++) {
                if (i != 0) {
                    query.append(" or ");
                }

                String keyword = keywordArray[i];
                query.append(" ( ");
                query.append("type ~* ? or ");
                query.append("title ~* ? or ");
                query.append("user_callsign ~* ? or ");
                query.append("user_description ~* ? or ");
                query.append("date_time_description ~* ? or ");
                query.append("location_description ~* ? or ");
                query.append("event_scale ~* ? or ");
                query.append("importance ~* ? or ");
                query.append("uid ~* ? or ");
                query.append("hash ~* ? ");
                query.append(" ) ");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
                queryParameters.add(".*" + keyword + ".*");
            }
            query.append(" ) ");
        }

        if (bbox != null) {
            boolean pt0EasternHemi = (0 <= bbox.point[0].x) && (bbox.point[0].x <= 180);
            boolean pt1EasternHemi = (0 <= bbox.point[1].x) && (bbox.point[1].x <= 180);

            // bbox spans the anti-meridian at 180
            if (pt0EasternHemi && !pt1EasternHemi) {
            	if (SHIFT_LONGITUDE_NAME == null) {
                    SHIFT_LONGITUDE_NAME = initDbFunction(NEW_SHIFT_LONGITUDE_NAME, OLD_SHIFT_LONGITUDE_NAME);
                }
                addWhereOrAnd(query).append(" ST_Intersects( " + SHIFT_LONGITUDE_NAME + "(ST_SetSRID(ST_MakeBox2D(ST_Point(?, ?), ST_Point(?, ?)), 4326)), " + SHIFT_LONGITUDE_NAME + "(location) ) ");
            } else {
                addWhereOrAnd(query).append(" ST_Intersects( ST_SetSRID(ST_MakeBox2D(ST_Point(?, ?), ST_Point(?, ?)), 4326), location ) ");
            }

            queryParameters.add(bbox.point[0].x);
            queryParameters.add(bbox.point[0].y);
            queryParameters.add(bbox.point[1].x);
            queryParameters.add(bbox.point[1].y);
        }

        if (startTime != null) {
            addWhereOrAnd(query).append(" date_time >= ? ");
            queryParameters.add(startTime);
        }

        if (endTime != null) {
            addWhereOrAnd(query).append(" date_time <= ? ");
            queryParameters.add(endTime);
        }

        if (type != null) {
            addWhereOrAnd(query);
            query.append(" ( ");
            String[] typeArray = type.split(",");
            for (int i=0; i < typeArray.length; i++) {
                if (i != 0) {
                    query.append(" or ");
                }

                query.append(" type = ? ");
                queryParameters.add(typeArray[i]);
            }
            query.append(" ) ");
        }

        if (callsign != null) {
            addWhereOrAnd(query);
            query.append(" ( ");
            String[] callsignArray = callsign.split(",");
            for (int i=0; i < callsignArray.length; i++) {
                if (i != 0) {
                    query.append(" or ");
                }

                query.append(" user_callsign = ? ");
                queryParameters.add(callsignArray[i]);
            }
            query.append(" ) ");
        }

        addWhereOrAnd(query).append(RemoteUtil.getInstance().getGroupClause("ci_trap"));

        query.append(" order by date_time desc ");
        if (maxReportCount != null) {
            query.append(" limit " + maxReportCount.toString());
        }

        try (Connection connection = ds.getConnection(); PreparedStatement select = wrapper.prepareStatement("select " +
                "id, type, user_callsign, user_description," +
                "date_time, date_time_description, " +
                "ST_AsText(location) as location_text, location_description, " +
                "event_scale, importance, uid, hash, xml, title " +
                "from ci_trap " + query.toString(), connection)) {

        int argCount = 1;
        for (Object parameter : queryParameters) {
            if (parameter instanceof Double) {
                select.setDouble(argCount, (Double) parameter);
            } else if (parameter instanceof Integer) {
                select.setInt(argCount, (Integer) parameter);
            } else if (parameter instanceof Timestamp){
                select.setTimestamp(argCount, (Timestamp)parameter);
            } else {
                select.setString(argCount, parameter.toString());
            }
            argCount++;
        }

        select.setString(argCount, groupVector);

        try (ResultSet results = wrapper.doQuery(select)) {
        	while (results.next()) {
        		reports.add(errorReportFromResults(results));
        	}
        }
        
        return reports;
        }
    }

    public int deleteReport(String uid, String groupVector) throws Exception {

        validator.getValidInput("citrap", uid,
        		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);

        try (Connection connection = ds.getConnection(); PreparedStatement del = wrapper.prepareStatement("delete from ci_trap where uid = ? " +
                " and " + RemoteUtil.getInstance().getGroupClause("ci_trap"), connection)) {
        del.setString(1, uid);
        del.setString(2, groupVector);
        int updated = del.executeUpdate();
        del.close();
        return updated;
    }
    }

    public String getReportAttrString(String uid, String attribute, String groupVector) throws Exception {

        String attr = null;

        validator.getValidInput("citrap", uid,
        		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.LONG_STRING_CHARS, true);

        try (Connection connection = ds.getConnection(); PreparedStatement select = wrapper.prepareStatement("select * from ci_trap where uid = ? " +
        		" and " + RemoteUtil.getInstance().getGroupClause("ci_trap"), connection)) {
        	select.setString(1, uid);
        	select.setString(2, groupVector);
        	try (ResultSet results = wrapper.doQuery(select)) {
        		if (results.next()) {
        			attr = results.getString(attribute);
        		}

        		return attr;
        	}
        }
    }

    public List<String> getUidsInRangeFromPoint(String cotType, String groupVector, int secAgo,
    		double lon, double lat, int radius) throws  Exception {

        if (DISTANCE_SPHERE_NAME == null) {
            DISTANCE_SPHERE_NAME = initDbFunction(NEW_DISTANCE_SPHERE_NAME, OLD_DISTANCE_SPHERE_NAME);
        }

    	String query = "select uid from cot_router where " +
    			" cot_type like ? and " +
                RemoteUtil.getInstance().getGroupClause() +
    			" and id in (select max(id) from cot_router where servertime > ? group by uid) and " +
                DISTANCE_SPHERE_NAME + "(event_pt, ST_GeomFromText(?,4326)) < ? ";

    	try (Connection connection = ds.getConnection(); PreparedStatement select = wrapper.prepareStatement(query, connection)) {

    		select.setString(1, cotType);
    		select.setString(2, groupVector);
    		select.setTimestamp(3, new Timestamp(System.currentTimeMillis() - (secAgo * 1000L)));
    		select.setString(4, "POINT(" + lon + " " + lat + ")");
    		select.setInt(5, radius);

    		try (ResultSet results = wrapper.doQuery(select)) {
    			List<String> uids = new LinkedList<String>();
    			while (results.next()) {
    				uids.add(results.getString(1));
    			}

    			return uids;
    		}
    	}
    }
}
