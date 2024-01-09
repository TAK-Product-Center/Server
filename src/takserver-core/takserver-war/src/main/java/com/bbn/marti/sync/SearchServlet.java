

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGcircle;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.ValidatorUtils;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.KmlUtils;
import com.bbn.security.web.MartiValidatorConstants;
import com.google.common.base.Strings;

/**
 * Servlet for searching for Enterprise Sync resources.
 * The user submits a GET request containing search parameters.
 * Servlet's response contains JSON-formatted metadata of the matching items. This list may be empty.
 * The client can then parse the list and fetch the content for the desired resources, using the
 * metadata to construct the content requests.
 */
//@WebServlet("/sync/search")
public class SearchServlet extends EnterpriseSyncServlet {
	
	/**
	 * Enum of the HTTP request parameters supported by this servlet.
	 * The application layer (this servlet class) will translate these to the column
	 * names in the underlying persistence layer as needed. Some of the fields
	 * of this enum happen to coincide with members of <code>Metadata.Field</code>,
	 * but that's just to make the API more understandable to the user. The
	 * servlet logic still does a translation (sometimes a trivial translation)
	 * from the these REST API parameters to the persistence layer. So although
	 * it looks like the database columns are exposed to the user, they're really not.
	 * 
	 * @see Metadata.Field
	 *
	 */
	public enum RequestParameters {
		BBox("Coordinates"),
		Circle("Coordinates"),
		StartTime("Timestamp"),
		StopTime("Timestamp"),
		SubmissionDateTime("Timestamp"), // alias for StartTime
		MinAltitude("Double"),
		MaxAltitude("Double"),
		PrimaryKey("NonNegativeInteger"),
		Filename("RestrictedRegex"),
		Keywords("RestrictedRegex"),
		MIMEType("RestrictedRegex"),
		Name("RestrictedRegex"),
		Permissions("RestrictedRegex"),
		Remarks("RestrictedRegex"),
		UID("RestrictedRegex"),
		Tool("RestrictedRegex");
		
		public final String validationPattern;
		private RequestParameters(String pattern) {
			this.validationPattern = pattern;
		}
		
		public static RequestParameters fromString(String given) {
			for (RequestParameters value : RequestParameters.values()) {
				if (given.compareToIgnoreCase(value.toString().trim()) == 0) {
					return value;
				}
			}
			return null;
		}
	}

	private static final int DEFAULT_PARAMETER_LENGTH = 1024;
	
	private static final long serialVersionUID = 8326235618371629486L;
	
    private static HashSet<String> optionalParameters;
    
    @Autowired
    private CommonUtil commonUtil;
	
    static {
    	optionalParameters = new HashSet<String>();
    	for (RequestParameters parameter : RequestParameters.values()) {
			optionalParameters.add(parameter.toString());
		}
    }
	
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
        
        String groupVector = null;

        try {
            // Get group vector for the user associated with this session
            groupVector = commonUtil.getGroupBitVector(request);
            log.finer("groups bit vector: " + groupVector);
        } catch (Exception e) {
            log.fine("exception getting group membership for current web user " + e.getMessage());
        }
        
        if (Strings.isNullOrEmpty(groupVector)) {
            throw new IllegalStateException("empty group vector");
        }
    	
        initAuditLog(request);
        
    	String context = "GET request parameters";
    	String remoteHost = "unidentfied host";
		try {
			String requestHost = request.getRemoteHost();
			if (requestHost != null) {
				remoteHost = validator.getValidInput("HttpServletRequest.getRemoteHost()", requestHost, 
						"MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
			}
 
			ValidatorUtils.assertValidHTTPRequestParameterSet(context, request.getParameterMap().keySet(),
					new HashSet<String>(), // no required parameters
					optionalParameters, validator);
			
			for (String parameterName : request.getParameterMap().keySet()) {
				for (String parameterValue : request.getParameterValues(parameterName)) {
					RequestParameters recognizedParameter = RequestParameters.fromString(parameterName);
					validator.getValidInput(context, parameterValue, recognizedParameter.validationPattern,
							DEFAULT_PARAMETER_LENGTH, false);
				}
			} 
			
    		String boxString = null;
    		String circleString = null;
    		Double minimumAltitude = null;
    		Double maximumAltitude = null;
    		Timestamp startTime = null;
    		Timestamp stopTime = null;
    		String tool = null;

    		SimpleDateFormat dateFormat = new SimpleDateFormat(tak.server.Constants.COT_DATE_FORMAT);
    		dateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
			
			// Do case-insensitive matching of HTTP request parameters to metadata fields
			// Search parameters that directly correspond to metadata fields have the same name in both enums.
			Map<String, String[]> httpParameters = request.getParameterMap(); 
			Map<Metadata.Field, String> metadataConstraints = new HashMap<Metadata.Field, String>();
			for (String key : httpParameters.keySet()) {
				String[] values = httpParameters.get(key);
				if (values != null && values.length > 1) {
					log.warning("Ignoring duplicate value(s) for HTTP request parameter " + StringUtils.normalizeSpace(key));
				}

				RequestParameters searchParameter = RequestParameters.fromString(key);
				if (log.isLoggable(Level.FINE)) {
					log.fine("Setting search parameter " + StringUtils.normalizeSpace(searchParameter.toString()) + "="
							+ StringUtils.normalizeSpace(values[0]));
				}
				
				if (searchParameter != null) {	
					if (values.length > 1) {
						log.warning("Received " + values.length + " values for " + StringUtils.normalizeSpace(key) + ". Ignoring all but first.");
					}
					switch (searchParameter) {
					case BBox:
						boxString = values[0];
						break;
					case Circle:
						circleString = values[0];
						break;
					case Tool:
						tool = values[0];
						break;
					case MinAltitude:
						minimumAltitude = Double.parseDouble(values[0]);
		    			log.fine("Lower altitude limit is " + minimumAltitude);
		    			break;
					case MaxAltitude:
						maximumAltitude = Double.parseDouble(values[0]);
		    			log.fine("Upper altitude limit is " + maximumAltitude);
		    			break;
					case SubmissionDateTime:
						// fall through intentionally
					case StartTime:
						Date startDate = dateFormat.parse(values[0]);
		    			startTime = new Timestamp(startDate.getTime());
		    			log.fine("Lower time limit is " + startTime);
		    			break;
					case StopTime:
						Date stopDate = dateFormat.parse(values[0]);
		    			stopTime = new Timestamp(stopDate.getTime());
		    			log.fine("Upper time limit is " + stopTime);
		    			break;
		    		default:
		    			Metadata.Field metadataField = Metadata.Field.fromString(key);
		    			if (metadataField != null) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("Setting metadata constraint " + metadataField.toString() + "=" + StringUtils.normalizeSpace(values[0]));
							}
							metadataConstraints.put(metadataField, values[0]);
		    			} else {
		    				log.warning("Unrecognized request parameter: " + StringUtils.normalizeSpace(key));
		    			}
					} 
				}	
				} 
			log.fine("Finished processing HTTP parameters.");
			
    		// Parse PGobject from HTTP request parameters
    		PGobject spatialConstraint = null;
    		if (boxString != null && circleString != null) {
    			response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
    					"Cannot specify both \"" + RequestParameters.BBox.toString() 
    					+ " and \"" + RequestParameters.Circle.toString() + "\"" );
    			return;
    		} else if (boxString != null) {
    			Double[] coordinates = KmlUtils.parseSpatialCoordinates(boxString);
    			spatialConstraint = new PGbox(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
    			log.fine("Spatial constraint is " + spatialConstraint.toString());
    		} else if (circleString != null) {
    			Double[] coordinates = KmlUtils.parseSpatialCoordinates(circleString);
    			spatialConstraint = new PGcircle(coordinates[0], coordinates[1], coordinates[2]);
    			log.fine("Spatial constraint is " + spatialConstraint.toString());
    		}
	
    		SortedMap<String, List<Metadata>> searchResults = 
    		    enterpriseSyncService.search(
    		    		minimumAltitude,
    		    		maximumAltitude,
    		    		metadataConstraints, 
    		    		spatialConstraint, 
    		    		startTime, 
    		    		stopTime,
    		    		Boolean.FALSE,
    		    		null,
    		    		tool,
    		    		groupVector); // not allowing mission name search through this path
    		JSONArray array = new JSONArray();
    		for (String uid : searchResults.keySet()) {
    			if ( validator != null) {
    				if (validator.isValidInput("doGet processing database results", uid, "MartiSafeString", 
    						MartiValidatorConstants.SHORT_STRING_CHARS, false)) {
						if (log.isLoggable(Level.FINE)) {
							log.fine("Processing search results for uid " + StringUtils.normalizeSpace(uid));
						}
					} else {
    					log.warning("Invalid UID found in database!");
    				}
    			}
    			List<Metadata> resultsForUid = searchResults.get(uid);
    			for (Metadata item : resultsForUid) {
    			if ( validator != null ) {
    				try {
    					commonUtil.validateMetadata(item);
    				} catch (ValidationException ex) {
    					log.warning("Database result failed input validation, id=" + item.getPrimaryKey() 
    							+ ": " + ex.getMessage());
    					continue;
    				} catch (IntrusionException ex) {
    					log.severe("Intrusion detected in resource database table, id=" + item.getPrimaryKey());
    					continue;
    				}
        		}
    			array.add(item.toJSONObject());	
    			}
    		}
    		JSONObject results = new JSONObject();
    		results.put(SearchServletConstant.RESULT_COUNT_KEY, array.size());
    		results.put(SearchServletConstant.RESULT_KEY, array);

    		response.setContentType("text/json");
    		PrintWriter writer = response.getWriter();
    		writer.write(results.toJSONString());
    		writer.close();

    		response.setStatus(HttpServletResponse.SC_OK);
    	} catch (ValidationException ex) {
    		log.warning("Invalid request from " + remoteHost + ": " + ex.getMessage()); 
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			return;
		} catch (IntrusionException e) {
			log.severe("Intrusion attempt from " + remoteHost + ": " + e.getMessage()); 
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Intrusion attempt detected! HTTP request denied.");
			return;
		} catch (SQLException|NamingException e) {
			log.severe("Error processing SQL query: " + e.getMessage());
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
					"Error processing database query. See server logs for details.");
			return;
		} catch (ParseException e) {
			log.warning("Error parsing user input: " + e.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error parsing user input");
			return;
		} catch (NumberFormatException ex) {
			log.warning("Error parsing user input: " + ex.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad numeric format in request parameters.");
			return;
		} catch (Exception ex) {
			log.severe("Unhandled " + ex.getClass().getName() + ": " + ex.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
					"Server runtime exception: see server log for details.");
			return;
		}
    }

	/**
	 * Always returns HttpServletResponse.SC_METHOD_NOT_ALLOWED.
	 * The search servlet supports GET requests only.
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	
	@Override
	protected void initalizeEsapiServlet() {
		 this.log = Logger.getLogger(SearchServlet.class.getCanonicalName());
	}
}
