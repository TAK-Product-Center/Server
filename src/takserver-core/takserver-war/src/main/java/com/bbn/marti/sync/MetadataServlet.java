

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.ValidatorUtils;
import com.bbn.marti.util.CommonUtil;
import com.bbn.security.web.SecurityUtils;

/**
 * Servlet for retrieving metadata about Enterprise Sync resources.
 * 
 * This servlet supports HTTP GET, only.
 */
public class MetadataServlet extends EnterpriseSyncServlet {
	/**
	 * Enum of the HTTP request parameters supported by this servlet. 
	 *
	 */
	public enum RequestParameters {
		UID,
		PrimaryKey
	}
	
	private static final int DEFAULT_PARAMETER_LENGTH = 1024;

	private static final long serialVersionUID = -7939609448873825097L;
	
	@Autowired
	private CommonUtil commonUtil;
	
	/**
     * @see HttpServlet#HttpServlet()
     */
    public MetadataServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
    	
        initAuditLog(request);
        
    	String remoteHost = "unidentified host";
		
    	String queryResult = null;
    	try {
    		if (validator != null) {
    			HashSet<String> optionalParameters = new HashSet<String>();
    			for (RequestParameters parameter : RequestParameters.values()) {
    				optionalParameters.add(parameter.toString());
    			}
//    			validator.assertValidHTTPRequestParameterSet("MetadataServlet parameters", request, 
//    					new HashSet<String>(), // No required parameters
//    					optionalParameters); // optional parameters // FIXME
    			
    			ValidatorUtils.assertValidHTTPRequestParameterSet("MetadataServlet parameters", request.getParameterMap().keySet(), 
    					new HashSet<String>(), // No required parameters
    					optionalParameters, validator); // optional parameters
    		}
    		
    		String requestHost = request.getRemoteHost();
    		if (requestHost != null && validator != null) {
    			remoteHost = validator.getValidInput("HttpServletRequest.getRemoteHost()", requestHost, 
    					"MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
    		}
    		
    		// Actually process the request
    		queryResult = this.getMetadataAsJSON(request);
    		
    		// Write the response
    		if (response.containsHeader("Content-Type")) {
    			response.setHeader("Content-Type", "text/json");
    		} else {
    			response.addHeader("Content-Type", "text/json");
    		}
    		if (response.containsHeader("Content-Type")) {
    			response.setHeader("Content-Type", "text/json");
    		} else {
    			response.addHeader("Content-Type", "text/json");
    		}
    		PrintWriter writer = response.getWriter();
    		writer.append(queryResult);
    		writer.close();
    		
    		response.setStatus(HttpServletResponse.SC_OK);
    		
    	} catch (IntrusionException ex) {
    		log.severe("Intrusion attempt from " + remoteHost + ": " + ex.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Intrusion attempt detected! HTTP request denied.");
			return;
    	} catch (NamingException e) {
    		e.printStackTrace();
    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    		return;
    	} catch (SQLException e) {
    		e.printStackTrace();
    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    		return;
    	} catch (ValidationException e) {
			log.warning("Invalid input from " + remoteHost + ": " + e.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			e.printStackTrace();
		}
    }

	/**
	 * Always returns HttpServletResponse.SC_METHOD_NOT_ALLOWED. This servlet implements GET, only.
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	/**
	 * Retrieves the metadata for a resource from the persistence layer.
	 *
	 * @param request must contain exactly one of the fields <code>Metadata.Field.PrimaryKey</code> or  
	 * <code>Metadata.Field.UID</code>
	 * @return JSON document containing zero or more metadata objects matching the request
	 * @throws NamingException if there is a JNDI lookup error connecting to the persistence layer
	 * @throws SQLException if there is a read error from the persistence layer
	 * @throws EnterpriseSyncException if the request contains parameters that fail white-list validation
	 * @throws IntrusionException 
	 * @throws ValidationException 
	 */
	private String getMetadataAsJSON(HttpServletRequest request) 
			throws  NamingException, SQLException, IntrusionException, ValidationException {
	    
	    String groupVector = null;

	    try {
	        // Get group vector for the user associated with this session
	        groupVector = commonUtil.getGroupBitVector(request);
	        log.finer("groups bit vector: " + groupVector);
	    } catch (Exception e) {
	        log.fine("exception getting group membership for current web user " + e.getMessage());
	    }
	    
	    

		List<Metadata> allResults = null;
		Integer primaryKey = getPrimaryKey(request.getParameterMap());
		String uid = getUid(request.getParameterMap());
		
		if (primaryKey == null && (uid == null || uid.isEmpty())) {
			throw new IllegalArgumentException("resource request requires either a UID or primary key!");
		} else if (primaryKey != null && uid != null && !uid.isEmpty()) {
			throw new IllegalArgumentException("resource request requires a UID or a primary key, not both!");
		} else if (primaryKey != null) {
			log.fine("Getting metdata for primary key " + primaryKey);
			Metadata result = enterpriseSyncService.getMetadata(primaryKey, groupVector);
			allResults = new LinkedList<Metadata>();
			try {
				commonUtil.validateMetadata(result);
			} catch (ValidationException ex) {
				log.warning("Database result failed input validation, id=" + result.getPrimaryKey() 
						+ ": " + ex.getMessage());
			} catch (IntrusionException ex) {
				log.severe("Intrusion detected in resource database table, id=" + result.getPrimaryKey());
			}
			allResults.add(result);
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Getting metadata for UID " + StringUtils.normalizeSpace(uid));
			}
			allResults = enterpriseSyncService.getMetadataByUid(uid, groupVector);
			Set<Metadata> badResults = new HashSet<Metadata>();
			for (Metadata result : allResults) {
				try {
					commonUtil.validateMetadata(result);
				} catch (ValidationException ex) {
					badResults.add(result);
					log.warning("Database result failed input validation: " + ex.getMessage());
				} catch (IntrusionException ex) {
					badResults.add(result);
					log.severe("Intrusion detected in resource database table, id=" + result.getPrimaryKey());
				}
			}
			for (Metadata badResult : badResults) {
				allResults.remove(badResult);
			}
			
		}
		
		log.fine("Query returned " + allResults.size() + " results");
		
		StringBuilder builder = new StringBuilder();
		for (Metadata result : allResults) {
			builder.append(result.toJSONObject().toJSONString());
			builder.append('\n');
		}
		
		log.finer("Results are:\n" + builder.toString().trim());
		
		return builder.toString().trim();
	}

	/**
	 * Finds the value of RequestParameter.PRIMARYKEY using case-insensitve match and converts it to an Integer
	 * @param parameters parameter map of the HTTP request
	 * @return the value of the parameter named, approximately, "PrimaryKey;" or null if no match found
	 */
	private Integer getPrimaryKey(Map<String, String[]> parameters) {
		Integer primaryKey = null;
		String[] values = SecurityUtils.getCaseInsensitiveParameter(parameters, RequestParameters.PrimaryKey.toString());
		if (values != null && values.length > 0) {
			primaryKey = Integer.parseInt(values[0]);
		}
		return primaryKey;
	}
	
	/**
	 * Finds the value of RequestParameter.UID using case-insensitve match and performs input validation
	 * @param parameters parameter map of the HTTP request
	 * @return the value of the parameter named, approximately, "UID;" or null if no match found
	 * @throws IntrusionException 
	 * @throws ValidationException 
	 */
	private String getUid(Map<String, String[]> parameters) throws ValidationException, IntrusionException {
		String uid = null;
		String[] values = SecurityUtils.getCaseInsensitiveParameter(parameters, RequestParameters.UID.toString());
		if (values != null && values.length > 0) {
			uid = values[0];
			if (validator != null) {
				uid = validator.getValidInput("Parsing UID", uid, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
			}
		}
		return uid;
	}

	@Override
	protected void initalizeEsapiServlet() {
		this.log = Logger.getLogger(MetadataServlet.class.getCanonicalName());
	}
	
	
}
