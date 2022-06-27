

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;

import com.google.common.base.Strings;

//@WebServlet("/sync/delete")
public class DeleteServlet extends EnterpriseSyncServlet {

	public static final String ID_KEY = "PrimaryKey";
	public static final String HASH_KEY = "Hash";
	
	public static final int MAX_INT_CHARACTERS = 12; // Length of MIN_INT including minus sign
	
	public static final int MAX_HASH_LEN = 1024;

	private static final long serialVersionUID = 8573446720325820749L;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		this.doDelete(request, response);
	}

	/**
	 * Processes a POST request to delete a file from the server.
	 * 
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		this.doDelete(request, response);
	}


	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException {
	    
	    log.fine("doDelete");
	    
	    // Get group vector for the user associated with this session
        String groupVector = null;
        
        try {
            groupVector = martiUtil.getGroupBitVector(request);
        } catch (RemoteException e) { }
        
        if (Strings.isNullOrEmpty(groupVector)) {
            throw new IllegalStateException("empty group vector");
        }
	    
		Map<String, String[]> httpParameters = request.getParameterMap();
		
		String hash = null;
		
		initAuditLog(request);
		
		List<Integer> toDelete = new LinkedList<Integer>();
		// Parse the HTTP parameters and slurp out all valid integers for parameters matching ID_KEY
		
		log.finest("http params: " + httpParameters.keySet());
		try {
			for (String key : httpParameters.keySet()) {
				if (key.compareToIgnoreCase(ID_KEY) == 0 ) {
				    
				    log.finest("delete by id");
				    
					for (String value : httpParameters.get(key)) {
						try {
							String numericString = value;
							if (validator != null) {
								numericString = validator.getValidInput("DeleteServlet", value, "NonNegativeInteger",
										MAX_INT_CHARACTERS, false);
							} 
							toDelete.add(Integer.parseInt(numericString));
						} catch (ValidationException | NumberFormatException e) {
							response.sendError(HttpServletResponse.SC_BAD_REQUEST, ID_KEY + " must be numeric.");
							return;
						} catch (IntrusionException e) {
							log.severe("Intrusion attempt from " + request.getServerName() + ": " + e.getMessage());
							response.sendError(HttpServletResponse.SC_BAD_REQUEST, ID_KEY + " must be numeric.");
							return;
						} 
					} 
				} else if (key.compareToIgnoreCase(HASH_KEY) == 0 ) {
				    
                    log.finest("delete by hash ");

				    if (!(httpParameters.get(key).length == 0)) {

				        String value = httpParameters.get(key)[0];
				        
	                    log.finest("delete by hash " + value);

				        try {
				            if (validator != null) {
				                hash = validator.getValidInput("DeleteServlet", value, "Hexidecimal", MAX_HASH_LEN, false);
				            } else {
				                hash = value;
				            }
				        } catch (ValidationException | NumberFormatException e) {
				            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ID_KEY + " must be numeric.");
				            return;
				        } catch (IntrusionException e) {
				            log.severe("Intrusion attempt from " + request.getServerName() + ": " + e.getMessage());
				            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ID_KEY + " must be numeric.");
				            return;
				        } 
				    }
				}
			}


			try {
				log.fine("Deleting " + toDelete.size() + " resource(s).");
				
				if (hash == null) {
				    // delete by primary key
				    enterpriseSyncService.delete(toDelete, groupVector);
				} else {
				    log.finest("delete by hash" + hash);
				    // delete by hash
				    enterpriseSyncService.delete(hash, groupVector);
				}
				PrintWriter out = response.getWriter();
				String title="Enterprise Sync Status";
				out.println("<html>\n");
				out.println("<head>\n");
				out.println("<title>" + title + "</title>\n");
				out.println("</head>\n");
				out.println("<h1>Success</h1>\n");
				out.println("<p>Deleted " + (hash == null ? toDelete.size() : "1") + " resource(s).</p>\n");
				out.println("</html>\n");
				
			} catch (SQLException | NamingException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
						"Failed to process delete request: " + e.getMessage());
				return;
			}

		} catch (IOException ioex) {
			log.warning("Failed to send HTTP response to " + request.getServerName());
			return;
		}	
	}
	
	@Override
	protected void initalizeEsapiServlet() {
		this.log = Logger.getLogger(DeleteServlet.class.getCanonicalName());

	}
	

}
