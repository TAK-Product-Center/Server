package com.bbn.marti;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import org.owasp.esapi.errors.ValidationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.kml.KmlParser;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.util.KmlUtils;
import com.bbn.marti.util.CommonUtil;
import com.bbn.security.web.MartiValidator;
import com.google.common.base.Strings;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import tak.server.cot.CotElement;

public class TracksKMLServlet extends EsapiServlet {
	
	@Autowired
	private JDBCQueryAuditLogHelper wrapper;
	
	@Autowired
	private DataSource ds;

	public enum QueryParameter {
		uid,
		callsign,
		cotType,
		groupName,
		groupRole
	}

	@Autowired
	private CommonUtil martiUtil;

	protected Map<String, HttpParameterConstraints> requiredHttpParameters;
	protected Map<String, HttpParameterConstraints> optionalHttpParameters;
	
	private static final long serialVersionUID = 3330866908781918226L;
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TracksKMLServlet.class);
	private static final long staleMinutes = 5;
		
	@Override
	protected void initalizeEsapiServlet()
	{
		logger.debug("initializeEsapiServlet");
		
		requiredHttpParameters = new HashMap<String, HttpParameterConstraints>();
		requiredHttpParameters.put(QueryParameter.uid.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
		requiredHttpParameters.put(QueryParameter.callsign.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
	
		optionalHttpParameters = new HashMap<String, HttpParameterConstraints>();		
		optionalHttpParameters.put(QueryParameter.cotType.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.groupName.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.groupRole.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
	}
	
	private List<Integer> getNextPrimaryKeyBatchFromSequence(String sequence, int batchSize) {
		
		List<Integer> idBatch = new LinkedList<Integer>();
		
		try (Connection connection = ds.getConnection(); PreparedStatement sql = wrapper.prepareStatement("select nextval('"+sequence+"') from generate_series(1,?)", connection)) {
			sql.setInt(1, batchSize);
			try (ResultSet results = sql.executeQuery()) {
				if(results != null) {
					while(results.next())
						idBatch.add(results.getInt(1));					
				}
			}
		} catch (PSQLException pe) { 
		    logger.warn("PSQLException getting batch of primary keys from DB", pe);
		    ServerErrorMessage sem = pe.getServerErrorMessage();
		    logger.warn("Postgres server error message: " + sem + " pausing repository service");
		} catch (SQLException e) {
			logger.warn("Generic SQLException getting batch of primary keys from DB", e);
		} catch (NamingException e) {
			logger.warn("NamingException getting primary key batch", e);
		}
		
		return idBatch;		
	}
	
	private String cotDetail(
			CotElement cotTrack, String remoteAddr, String groupName, String groupRole) {
		
		String detail = "<detail>"
		+	"<contact endpoint=\"" + remoteAddr + ":4242:tcp\" callsign=\"" + cotTrack.callsign + "\"/>"
		+	"<uid Droid=\"" + cotTrack.uid + "\"/>"
		+ 	"<track speed=\"" + cotTrack.speed + "\" course=\"" + cotTrack.course + "\"/>";
		
		if (groupName != null && groupRole != null) {
			detail += "<__group name=\"" + groupName + "\" role=\"" + groupRole + "\"/>";
		}
		
		if (cotTrack.detailtext.length() > 0) {
			detail += "<precisionlocation " + cotTrack.detailtext + "/>";
		}
		
		detail += "</detail>";
		return detail;
	}   

	private double round(double value, int digits) {
		double tmp = Math.pow(10,  digits);
		double rounded = Math.round(value*tmp)/tmp;		
		return rounded;
	}
	
	@SuppressWarnings("deprecation")
    private boolean insertCotTrack(CotElement cotTrack, Integer id, String groupVector) {
		
		try {

			// fixup bad elevations seen in MultiTrack data
			double hae = 0;
			try {
				hae = Double.parseDouble(cotTrack.hae);
				if (hae < 1e-6) {
					hae = 0;
				}
			} catch (NumberFormatException e) {
				hae = 0;
			}

			try {
				// 
				// validate the cot element before inserting into the database
				//
				validator.getValidInput("tracksKml", cotTrack.uid, 
						MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
				validator.getValidInput("tracksKml", cotTrack.cottype, 
						MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
				validator.getValidInput("tracksKml", Double.toString(hae), 
						MartiValidator.Regex.Double.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
				validator.getValidInput("tracksKml", Double.toString(cotTrack.ce), 
						MartiValidator.Regex.Double.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
				validator.getValidInput("tracksKml", Double.toString(cotTrack.le), 
						MartiValidator.Regex.Double.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
				validator.getValidInput("tracksKml", cotTrack.detailtext, 
						MartiValidator.Regex.XmlBlackList.name(), MartiValidator.LONG_STRING_CHARS, true);

			} catch (ValidationException e) {
				logger.error("EASPI ValiationException!", e);	    	
				return false;
			}			

			XPath geoPointSrcExpression = DocumentHelper.createXPath("/detail/precisionlocation/@geopointsrc");
			Document doc = DocumentHelper.parseText(cotTrack.detailtext);
			String geopointsrc = (String) geoPointSrcExpression.valueOf(doc);

			String how = null;
			if (geopointsrc == null || geopointsrc.length() == 0 || geopointsrc.equals("GPS")) {
				how = "m-g";
			} else if (geopointsrc.equals("User")) {
				how = "h-g-i-g-o";
			}

			//
			// check for a duplicate cot event. 
			// do not include servertime/servertime_hour, detail, or stale in the comparison
			//
			try (Connection connection = ds.getConnection(); PreparedStatement select = wrapper.prepareStatement("select * from cot_router where " +
					" uid = ? and cot_type = ? "
					+ "and start = ? and time = ? "
					+ "and how = ? and "
					+ "round(point_hae, 6) = ? and round(point_ce, 6) = ? and round(point_le, 6) = ? "
					+ "and round(ST_X(event_pt)::numeric, 6) = ? and round(ST_Y(event_pt)::numeric, 6) = ?", connection)) { 

				select.setString(1, cotTrack.uid);
				select.setString(2, cotTrack.cottype);
				select.setTimestamp(3,  cotTrack.servertime);
				select.setTimestamp(4,  cotTrack.servertime);
				select.setString(5, how);
				select.setDouble(6, round(hae, 6));
				select.setDouble(7, round(cotTrack.ce, 6));
				select.setDouble(8, round(cotTrack.le, 6));
				select.setDouble(9, round(cotTrack.lon, 6));
				select.setDouble(10, round(cotTrack.lat, 6));

				try (ResultSet results =  select.executeQuery()) {        	
					boolean found = results.next();
					if (found) {
						return false;
					}
				}
			} catch (SQLException | NamingException ex) {
				logger.warn("exception execution select from cot_router", ex);
			}


			//
			// insert the cot event
			//
			try (Connection connection = ds.getConnection(); PreparedStatement insert = wrapper.prepareStatement("insert into cot_router (" +
					"id, uid, cot_type, "
					+ "start, time, stale, "
					+ "how, "
					+ "point_hae, point_ce, point_le, detail, event_pt,"
					+ " servertime, servertime_hour, groups ) " +
					"values (?,?,?,?,?,?,?,?,?,?,?,ST_GeometryFromText(?, 4326),?,?, ?" + RemoteUtil.getInstance().getGroupType() +" ) ", connection)) {
				insert.setInt(1, id);
				insert.setString(2, cotTrack.uid);
				insert.setString(3, cotTrack.cottype);
				insert.setTimestamp(4,  cotTrack.servertime);
				insert.setTimestamp(5,  cotTrack.servertime);
				insert.setTimestamp(6,  new Timestamp(cotTrack.servertime.getTime() + staleMinutes * 60 * 1000));
				insert.setString(7, how);
				insert.setDouble(8, hae);
				insert.setDouble(9, cotTrack.ce);
				insert.setDouble(10, cotTrack.le);
				insert.setString(11, cotTrack.detailtext);
				insert.setString(12, "POINT(" + cotTrack.lon + " " + cotTrack.lat + ")");
				insert.setTimestamp(13,  cotTrack.servertime);
				Timestamp servertime_hour = (Timestamp)cotTrack.servertime.clone();
				servertime_hour.setMinutes(0);
				servertime_hour.setSeconds(0);
				insert.setTimestamp(14,  servertime_hour);
				insert.setString(15, groupVector);

				insert.executeUpdate();
				insert.close(); 

			} catch (Exception e) {
				logger.error("exception in insertCotTrack", e);
				return false;
			} 
		} catch (DocumentException ex) {
			logger.warn("document exception", ex);
		}
		
		return true;
	}
		
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
    	
    	try {
            Map<String, String[]> httpParameters = validateParams("tracksKml", request, response, 
            		requiredHttpParameters, optionalHttpParameters);

            if(httpParameters == null)
                return;

			String groupVector = martiUtil.getGroupBitVector(request);

			if (Strings.isNullOrEmpty(groupVector)) {
				throw new IllegalStateException("empty group vector");
			}

			logger.info("group vector: " + groupVector);

	      	String uid = getParameterValue(httpParameters, QueryParameter.uid.name());
	      	String callsign = getParameterValue(httpParameters, QueryParameter.callsign.name());
	      	String cotType = getParameterValue(httpParameters, QueryParameter.cotType.name());
	      	String groupName = getParameterValue(httpParameters, QueryParameter.groupName.name());
	      	String groupRole = getParameterValue(httpParameters, QueryParameter.groupRole.name());
	      	
	      	String input = IOUtils.toString(request.getInputStream());
	    	try {
				validator.getValidInput("tracksKml", input, 
						MartiValidator.Regex.XmlBlackListWordOnly.name(), input.length(), true);
			
		    } catch (ValidationException e) {
		        logger.error("EASPI ValiationException!", e);	    	
	        	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	      		return;
		    }	      	

	      	// TODO support kmz too
	      	Kml kmlTrack = KmlParser.unmarshal(input);
	    	
	      	// set default cot type if none supplied
	      	if (cotType == null) {
	      		cotType = "a-f-G-U-C-I";
	      	}
	      	
	      	// parse the kml
	      	List<CotElement> cotTracks = KmlUtils.trackKmlToCot(kmlTrack);
	      	if (cotTracks == null) {
	      		logger.error("error converting kml to cot");
	        	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	      		return;
	      	}

	      	//
	      	// insert each track into the database
	      	//
	      	
	      	
				List<Integer> dbIds = getNextPrimaryKeyBatchFromSequence("cot_router_seq", cotTracks.size());
				if (dbIds == null || dbIds.size() != cotTracks.size()) {
					logger.error("error retrieving db ids");
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}

				Iterator<Integer> itId = dbIds.iterator();
				Iterator<CotElement> itTrack = cotTracks.iterator();
				while (itId.hasNext() && itTrack.hasNext()) {

					Integer id = itId.next();
					CotElement cotTrack = itTrack.next();

					cotTrack.uid = uid;
					cotTrack.callsign = callsign;
					cotTrack.cottype = cotType;
					cotTrack.detailtext = cotDetail(
							cotTrack, request.getRemoteAddr(), groupName, groupRole);

					insertCotTrack(cotTrack, id, groupVector);
				}
    	} 
    	catch (Exception e) {
    		logger.error("exception in doPost", e);
        	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    	}
	    	
      	return;
    }
}
