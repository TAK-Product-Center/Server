

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.security.web.SecurityUtils;
import com.google.common.base.Strings;

import io.micrometer.core.instrument.Metrics;

/**
 * Servlet for retrieving the content
 */
@WebServlet("/sync/content")
public class ContentServlet extends EnterpriseSyncServlet {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ContentServlet.class);

	private static final long serialVersionUID = -4951951000582076313L;
	public static final String DEFAULT_FILENAME="EnterpriseSync.dat";
	
	/**
	 * Enum of the HTTP request parameters supported by this servlet. 
	 *
	 */
	public enum RequestParameters {
		UID,
		Hash,
		offset
	}

	private static final int DEFAULT_PARAMETER_LENGTH = 1024;

	private void getResource(HttpServletRequest request, HttpServletResponse response, HttpMethod method)
			throws ServletException, IOException {

	  Metrics.counter("DownloadMissionContent", "missions", "content").increment();

	  initAuditLog(request);
	  // Get group vector for the user associated with this session
	  String groupVector = martiUtil.getGroupBitVector(request);

	  if (Strings.isNullOrEmpty(groupVector)) {
	  	throw new IllegalStateException("empty group vector");
	  }

	  log.finer("group vector: " + groupVector);

      String remoteHost = "unidentified host";
      String context = "GET request parameters";
      byte[] content = new byte[0];
      Metadata match = null;
      List<Metadata> matches = null;
      try {
        String requestHost = request.getRemoteHost();
        if (requestHost != null && validator != null) {
          remoteHost = validator.getValidInput(context, requestHost, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
        }
        // Parse & validate the request parameters
        String uid = getUid(request.getParameterMap());
		String hash = getHash(request.getParameterMap());
		Integer offset = getOffset(request.getParameterMap());

		String query = "uid: " + uid + "; hash: " + hash + "; offset " + offset;
		if (logger.isDebugEnabled()) {
			logger.debug(query);
		}
		
        if (uid != null){
          content = enterpriseSyncService.getContentByUid(uid, groupVector);
          matches = enterpriseSyncService.getMetadataByUid(uid, groupVector);
        } else if( hash != null) {
			content = enterpriseSyncService.getContentByHash(hash, groupVector);
			if (logger.isDebugEnabled()) {
				logger.debug("content by hash size: " + (content != null ? content.length : "null"));
			}
			try {
			    matches = enterpriseSyncService.getMetadataByHash(hash, groupVector);
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception getting metadata " + e.getMessage(), e);
				}
			}
		}

	  	if (matches == null || matches.isEmpty()) {
        	throw new NotFoundException("no metadata results for " + query);
	  	}

	  	if (content == null) {
        	logger.error("found null content for :" + query);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

	  	// Just take the first one on the list, which is the most recent
	  	match = matches.get(0);

	  	if (logger.isDebugEnabled()) {
	  		logger.debug("Metadata is: " + match.toJSONObject().toString());
	  	}

        String mimeType = match.getFirst(Metadata.Field.MIMEType);
        if(validator != null && validator.isValidInput("MIME Type", mimeType, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false)) {
			  // Set MIME type of HTTP response
			  if (response.containsHeader("Content-Type")) {
				  response.setHeader("Content-Type", mimeType);
			  } else {
				  response.addHeader("Content-Type", mimeType);
			  }
        }

        // Set file name of HTTP response
        String filename = match.getFirstSafely(Metadata.Field.DownloadPath);
        
        if (filename.isEmpty()) {
          filename = match.getFirstSafely(Metadata.Field.Name);
        }
     
        if (filename.isEmpty()) {
          filename = DEFAULT_FILENAME;
        }

        if (logger.isDebugEnabled()) {
        	logger.debug("enterprise sync filename: " + filename);
        }

        String contentDisposition = "inline; filename=" + filename;
        if (validator != null && validator.isValidInput("Content Disposition", contentDisposition, "Filename", DEFAULT_PARAMETER_LENGTH, false)) {
        	if (response.containsHeader("Content-Disposition")) {
        		response.setHeader("Content-Disposition", contentDisposition);
        	} else {
        		response.addHeader("Content-Disposition", contentDisposition);
        	}
        }

        response.setStatus(HttpServletResponse.SC_OK);

	    if (method == HttpMethod.GET) {


			// apply offset param
			if (offset != null && offset > 0) {

				// validate the offset
				if (content.length < 1 || offset > content.length - 1) {
					throw new IllegalArgumentException("invalid offset " + offset + " for file size " + content.length);
				}

				if (logger.isInfoEnabled()) {
					logger.info("applying offset " + offset);
				}
				content = Arrays.copyOfRange(content, offset, content.length);
			}

			try (OutputStream outStream = response.getOutputStream()) {

				if (outStream == null) {
					logger.error("response.getOutputStream() returned null!");
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}

				outStream.write(content);
			}
	    }		
		
      } catch (NamingException | SQLException ex) {
        logger.error("error fetching file " + ex.getMessage(), ex);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      } catch (IntrusionException e) {
        logger.error("Intrusion attempt from " + remoteHost + ": " + e.getMessage());
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Intrusion attempt detected! HTTP request denied.");
        return;
      } catch (ValidationException | IllegalArgumentException e) {
        logger.error("Invalid input from " + remoteHost + ": " + e.getMessage());
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        return;
      } catch (NotFoundException e) {
        logger.debug("Query didn't return anything " + e.getMessage(), e);
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
      } catch (Exception e) {
		logger.error("Exception in getResource! " + e.getMessage(), e);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	  }
	}

	/**
	 * Get the content for the resource identified by UID or primary key.
	 * Request parameters must include exactly one of: <code>Metadata.Field.UID</code>
	 * or <code>Metadata.Field.PrimaryKey</code>. If <code>Metadata.Field.UID</code>
	 * is present, then <code>Metadata.Field.Version</code> may optionally be
	 * specified to retrieve a specific version of the metadata. Otherwise, the
	 * latest version of the metadata will be returned.
	 *
	 * If the resource is found, this method returns HTTP OK and the resource content
	 * is in the response body.
	 *
	 * If the resource is not found, this method returns HTTP error 404.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		getResource(request, response, HttpMethod.GET);
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		getResource(request, response, HttpMethod.HEAD);
	}

	/**
	 * Always returns HTTP error <code>HttpServletResponse.SC_METHOD_NOT_ALLOWED</code>
	 * The POST method is not allowed for this servlet.
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
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

	/**
	 * Finds the value of RequestParameter.Hash using case-insensitve match and performs input validation
	 * @param parameters parameter map of the HTTP request
	 * @return the value of the parameter named, approximately, "Hash;" or null if no match found
	 * @throws IntrusionException
	 * @throws ValidationException
	 */
	private String getHash(Map<String, String[]> parameters) throws ValidationException, IntrusionException {
		String hash = null;
		String[] values = SecurityUtils.getCaseInsensitiveParameter(parameters, RequestParameters.Hash.toString());
		if (values != null && values.length > 0) {
			hash = values[0];
			if (validator != null) {
				hash = validator.getValidInput("Parsing Hash", hash, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
			}
		}
		return hash;
	}
	
	private Integer getOffset(Map<String, String[]> parameters) throws ValidationException, IntrusionException {
        String offset = null;
        String[] values = SecurityUtils.getCaseInsensitiveParameter(parameters, RequestParameters.offset.toString());
        if (values != null && values.length > 0) {
            offset = values[0];
            if (validator != null) {
                offset = validator.getValidInput("Parsing offset", offset, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
            }
            
            Integer result = Integer.parseInt(offset);
            
            if (result < 0) {
                throw new IllegalArgumentException("invalid offset: " + result);
            }
            
            return result;
        }
        
        return null;      
    }
	
	@Override
	protected void initalizeEsapiServlet() {
		 this.log = Logger.getLogger(ContentServlet.class.getCanonicalName());	
	}
	
	
}
