

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.errors.ValidationException;

import com.google.common.base.Strings;

/**
 * Servlet that accepts POST requests (only) to upload mission packages to the
 * Enterprise Sync database. 
 *
 * Requires the parameters "senderuid", "hash", and "filename".
 */
@WebServlet("/sync/missionquery")
@MultipartConfig
public class MissionPackageQueryServlet extends EnterpriseSyncServlet {
	private static final long serialVersionUID = -255123540681449153L;
	private static Set<String> optionalParameters = new HashSet<String>();
	private static Set<String> requiredParameters;
	static {
		// static initializer for all required parameters
		requiredParameters = new HashSet<String>(PostParameter.values().length);
		for (PostParameter param : PostParameter.values()) {
			requiredParameters.add(param.getParameterString());
		}
	}

	/**
	* Required parameter enum for posting a mission package.
	* the parameter string is the expected URL argument. The metadata field is where the parameter gets mapped to.
	* 
	* A request is queried by iterating over all of the parameter strings in the enum, and emplacing the param value into the metadata field.
	* @note One of the parameters MUST map to a metdata uid field - getUid() is used to query the database for potential collisions.
	*/
	private enum PostParameter {
		HASH        ("hash", 	 Metadata.Field.UID);
		
		private final String param;
		private final Metadata.Field field;

		// protected constructor
		PostParameter(String paramName, Metadata.Field metaField) {
			this.param = paramName;
			this.field = metaField;
		}
		
		protected String getParameterString() {
			return param;
		}
		
		protected Metadata.Field getMetadataField() {
			return field;
		}
	}
	
	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
	}

	@Override
	protected void initalizeEsapiServlet() {
		this.log = Logger.getLogger(MissionPackageQueryServlet.class.getCanonicalName()); // Tomcat logger
		
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	private void processRequest(HttpServletRequest request, HttpServletResponse response) 
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
	    
		try {
		    
		    initAuditLog(request);
		    
			// validate all parameters -- enforces required/optional parameter presence, throws an exception if invalid
    		if (validator != null) {
    			validator.assertValidHTTPRequestParameterSet("Mission package query", request,
    					requiredParameters,
    					optionalParameters);
    		}

    		// map post parameters to metadata field entries
    		Map<String,String[]> paramsMap = request.getParameterMap();
    		Map<Metadata.Field,String[]> metaParams = new HashMap<Metadata.Field,String[]>();
    		
    		for (PostParameter param : PostParameter.values()) {
    			String[] paramArgs;
    			if ((paramArgs = paramsMap.get(param.getParameterString())) != null) {	
    				Metadata.Field field = param.getMetadataField();
    				metaParams.put(field, paramArgs);
    			} else {
    				String message = String.format("HTTP post missing required param %s.", param.getParameterString());
					log.warning(message);
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
    				return;
    			}
    		}
    		// initialize metadata from field->string[] map
    		Metadata toStore = Metadata.fromMap(metaParams);
    		
			if (response.containsHeader("Content-Type")) {
				response.setHeader("Content-Type", "text/plain");
			} else {
				response.addHeader("Content-Type", "text/plain");
			}

			String uid = getUid(toStore, groupVector);
			if (uid == null) {
				// don't have in database - return 404 file not found -- not an error, so we don't log anything
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
				return;
			} else {
				
				// retrieve host name from request
				String responseStr = String.format("%s/Marti/sync/content?hash=%s", getBaseUrl(request), uid);

				response.setStatus(HttpServletResponse.SC_OK);
				PrintWriter writer = response.getWriter();
				writer.print(responseStr);
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.warning(e.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
	}
	
	/**
	* Executes a database query to see if the proposed metadata entry is a unique filename and hash double.
	* -- we can, in theory reverse the list to find the most recent entry, but that seems unnecessary -- there should be a double in there
	*/
	private String getUid(Metadata proposed, String groupVector) throws SQLException, NamingException, ValidationException {
		String databaseUid = proposed.getUid();
		List<Metadata> matchingUids = enterpriseSyncService.getMetadataByUid(databaseUid, groupVector);
				
		for (Metadata entry : matchingUids) {
			// search for a parameter -> metadata mapping that different arguments
			boolean same = true;
			for (PostParameter param : PostParameter.values()) {
				Metadata.Field testAttr = param.getMetadataField();
				String mine = proposed.getFirstSafely(testAttr);
				String theirs = entry.getFirstSafely(testAttr);
				
				if (!mine.equals(theirs)) {
					// found attribute with different value -- skip
					same = false;
					break;
				}
			}
			if (same) {
				// never found a differing argument -- are the same
				return databaseUid;
			} 
		}

		// never found a matching db entry
		return null;
	}

	public static String getBaseUrl(@NotNull HttpServletRequest request) throws URISyntaxException {
	    
	    String baseUrl = "";
	    
        String url = request.getRequestURL().toString();
        URI uri = new URI(url);
        baseUrl = uri.getScheme() + "://" + uri.getHost() + ":";
        if(uri.getPort() == 8444) {
            baseUrl += 8443;
        } else {
            baseUrl += uri.getPort();
        }
        
        return baseUrl;
	}
}
