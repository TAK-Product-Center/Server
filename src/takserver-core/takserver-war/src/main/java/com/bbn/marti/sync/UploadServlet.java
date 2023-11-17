

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.sync.Metadata.Field;
import com.google.common.base.Strings;

import io.micrometer.core.instrument.Metrics;
import tak.server.PluginManager;

/**
 * Servlet that accepts POST requests (only) to upload resources to the
 * Enterprise Sync database. Use POST to add a new resource and PUT to either
 * add a new resource or update an existing one.
 */
public class UploadServlet extends EnterpriseSyncServlet {

	private static final long serialVersionUID = -8151782550681449153L;
	private static final int DEFAULT_PARAMETER_LENGTH = 1024;
	private static Set<String> optionalParameters;
	
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UploadServlet.class);

	@Autowired
	private CoreConfig coreConfig;
	
	@Autowired(required = false)	
	protected PluginManager pluginManager;

	private int uploadSizeLimitMB;

	static {
		optionalParameters = new HashSet<String>();
		for (Metadata.Field field : Metadata.Field.values()) {
			if (!field.isMachineGenerated) {
				optionalParameters.add(field.toString());
			}
		}
		// Add the alias MIME => MIMEType to support the name ATAK uses
		optionalParameters.add("MIME");
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);


		uploadSizeLimitMB = coreConfig.getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB();
		
		if (uploadSizeLimitMB > 550) {
			throw new IllegalArgumentException("Invalid configuration in CoreConfig for EnterpriseSyncSizeLimitMB. Must be 550MB or less");
		}
		
		logger.info("Enterprise Sync upload limit is " + uploadSizeLimitMB + " MB");


	}

	/**
	 * Always returns HttpServletResponse.SC_METHOD_NOT_ALLOWED. GET is not
	 * supported. This servet is for uploading resources to the Enterprise Sync
	 * database.
	 * 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 * @see MetadataServlet
	 */
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * Uploads a file to the Enterprise Sync database. Request must specify, at
	 * minimum, the file name and MIME type. Providing a UID is optional but if
	 * one is provided, it must not already be present in the database. Use PUT
	 * request to update a resource that already exists. HTTP parameter names
	 * mtch the values in the eum <code>Metadata.Field</code>.
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 * @see <code>Metadata.Field</code>
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {

		String groupVector = null;

		try {
			// Get group vector for the user associated with this session
			groupVector = commonUtil.getGroupBitVector(request);
			if (logger.isDebugEnabled()) {
				logger.debug("groups bit vector: " + groupVector);
			}
		} catch (Exception e) {
			logger.error("exception getting group membership for current user ", e);
		}

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalStateException("empty group vector");
		}  

		initAuditLog(request);

		String remoteHost = "unknown host";
		String context = "Upload request parameters";
		Metadata uploadedMetadata = null;
		String badRequestPrefix = "";

		// Process the HTTP request
		try {
			String requestHost = request.getRemoteHost();
			if (requestHost != null && validator != null) {
				remoteHost = validator.getValidInput("HttpServletRequest.getRemoteHost()", requestHost, 
						"MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
			}
			badRequestPrefix = "Bad upload request from " + remoteHost + ": ";

			if (validator != null) {
				validator.assertValidHTTPRequestParameterSet(context, request, 
						new HashSet<String>(),
						optionalParameters);
			}

			Map<String, String[]> httpParameters = request.getParameterMap();
			Map<Metadata.Field, String[]> metadataParameters = new HashMap<Metadata.Field, String[]>();
			for (String key : httpParameters.keySet()) {
				Metadata.Field field = Metadata.Field.fromString(key);
				if (field == null ) {
					if (validator != null) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognized parameter " + 
								validator.getValidInput(context, key, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, 
										false));
						return;
					} else {
						// ignore it
						continue;
					}

				} else {
					String[] values = httpParameters.get(key);
					// Not all HTTP clients support multi-valued parameters.
					// If HTTP parameter corresponds to an array-valued metadata field, 
					// parse it as a comma-delimited list
					if (values.length == 1 && field.isArray) {
						String[] tokens = values[0].split("\\s*,\\s*");
						values = tokens;
					}
					if (validator != null) {
						for (String value : values) {
							validator.getValidInput(context, value, field.validationType.name(), 
									field.maximumLength, true);
						}
					}
					
					if (logger.isDebugEnabled()) {
						logger.debug("Added " + field.toString() + " (" + values.length + ") values");
					}
					metadataParameters.put(field, values);
				}
			}
			Metadata toStore = Metadata.fromMap(metadataParameters);
			commonUtil.validateMetadata(toStore);
			
			if (logger.isDebugEnabled()) {
				logger.debug("Request is: " + toStore.toJSONObject().toJSONString());
				logger.debug("Content length is " + request.getContentLength());
			}

			if (request.getContentLength() > uploadSizeLimitMB * 1000000) {
				String message = "Uploaded file exceeds server's size limit of " + uploadSizeLimitMB + " MB! (limit is set in CoreConfig.xml network.enterpriseSyncSizeLimitMB";
				
				if (logger.isWarnEnabled()) {
					logger.warn(badRequestPrefix + message);
				}
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				return;
			} else if (request.getContentLength() < 1) {
				String message = "HTTP request body has no content.";
				
				if (logger.isWarnEnabled()) {
					logger.warn(badRequestPrefix + message);
				}
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				return;
			}

			// assign random to resource if not specified in request
			if (toStore.getUid() == null || toStore.getUid().isEmpty()) {
				toStore.set(Metadata.Field.UID, new String[] {UUID.randomUUID().toString()});
			} 

			String mimeType = request.getHeader("Content-Type");

			InputStream inputStream = request.getInputStream();
			
			if (mimeType == null || !mimeType.contains("multipart/form-data")) {
				
				if (logger.isDebugEnabled()) {
					logger.debug("Uploading content.");
				}
				
				if (logger.isDebugEnabled()) {
					logger.debug("reading payload from request");
				}
				
				inputStream = request.getInputStream();
				
			} else {
				Collection<Part> parts = request.getParts();
				
				if (logger.isDebugEnabled()) {
					logger.debug("Uploading multi-part content with " + parts.size() + " parts.");
				}
				// ATAK sends a part called "assetfile"
				Part part = request.getPart("assetfile");
				if (part == null) {
					// Firefox and Chrome browsers send a part called "resource"
					part = request.getPart("resource");
				}

				if (part != null) {
					
					inputStream = part.getInputStream();
						
						if (logger.isDebugEnabled()) {
							logger.debug("reading payload from request");
						}
				} else {
					for (Part myPart : parts) {
						if (logger.isDebugEnabled()) {
							logger.debug(myPart.getName() + ": " + myPart.getContentType() );
						}
					}
					
					if (logger.isErrorEnabled()) {
						logger.error("Unable to find content in multi-part submission");
					}
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Upload request was not formatted in a way Marti can understand.\n"
									+ "Please try a different browser.");
					return;
				}

				// Get the file name from the part's content-disposition header if possible
				String cd = part.getHeader("content-disposition");
				
				if (logger.isDebugEnabled()) {
					logger.debug("content-disposition is: " + cd);
				}
				String filenameToken = "filename=\"";
				if (cd != null && cd.contains(filenameToken)) {
					int filenameIndex = cd.indexOf(filenameToken);
					String filenameFragment = cd.substring(filenameIndex + filenameToken.length()).trim();
					String[] pieces = filenameFragment.split("[\\s\\\\/]");
					// Strip off closing quote 
					String filename = pieces[0].substring(0, pieces[0].length() - 1);
					if (validator != null) {
						filename = validator.getValidInput(context, filename,
								Metadata.Field.DownloadPath.validationType.name(), 
								Metadata.Field.DownloadPath.maximumLength, true);
					}

					toStore.set(Metadata.Field.DownloadPath, new String[] {filename});
					if (toStore.getFirstSafely(Field.Name).isEmpty()) {
						toStore.set(Metadata.Field.Name, new String[] {filename});
					}
				} 
				mimeType = part.getHeader("content-type");
			} 

			// TODO: Set the name

			if (mimeType != null) {
				toStore.set(Metadata.Field.MIMEType, mimeType);
			}
			// Get the user ID from the request
			String userName = SecurityContextHolder.getContext().getAuthentication().getName();
			if (validator != null) {
				userName = validator.getValidInput(context, userName, 
						Metadata.Field.SubmissionUser.validationType.name(),
						Metadata.Field.SubmissionUser.maximumLength, true);
			}
			if (userName != null) {
				toStore.set(Metadata.Field.SubmissionUser, userName);
			}

			uploadedMetadata = enterpriseSyncService.insertResourceStream(toStore, inputStream, groupVector); // do not validate file length, as that's already checked above 

			Metrics.counter("UploadMissionContent", "missions", "content").increment();
			
			inputStream.close();
			
			// if plugin classname is set, notify the plugin with the file upload event. The notification event will include the Metadata object and will not include the full byte[] payload.
			if (toStore.getPluginClassName() != null) {
				
				logger.info("Notifying the plugin {} with file upload event", toStore.getPluginClassName());
				pluginManager.onFileUpload(toStore.getPluginClassName(), toStore);

			} 
			
		} catch (NamingException|SQLException ex) {
			String msg = "Enterprise Sync database failed to process write operation.";
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
			
			if (logger.isErrorEnabled()) {
				logger.error(msg, ex);
			}
			return;
		} catch (ServletException srvex) {
			// Thrown by request.getPart if request is not a well-formed 
			
			String msg = "Unparseable multi-part content in request.";
			
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
			if (logger.isErrorEnabled()) {
				logger.error(msg, srvex);
			}
			return;
		} catch (IOException ioex) {
			String errorMessage;
			int responseCode;
			if (request.getContentLength() == 0) {
				responseCode = HttpServletResponse.SC_BAD_REQUEST;
				errorMessage = "POST request contained no data!";
				if (logger.isWarnEnabled()) {
					logger.warn(badRequestPrefix + errorMessage, ioex);
				}
			} else {
				responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
				errorMessage = "Failed to read data from HTTP POST body";
				
				if (logger.isErrorEnabled()) {
					logger.error(errorMessage, ioex);
				}
			}
			response.sendError(responseCode, errorMessage);
			return;

		} catch (ValidationException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn(badRequestPrefix + ex.getLogMessage());
			}
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
					"Illegal characters detected. Accents and most punctuation characters are not allowed for security.");
			return;
		} catch (IntrusionException evilException) {
			if (logger.isErrorEnabled()) {
				logger.error("Intrusion attempt from " + remoteHost + ": ", evilException);
			}
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Intrusion attempt detected! HTTP request denied.");
			return;
		}

		PrintWriter writer = response.getWriter();
		
		if (logger.isDebugEnabled()) {
			logger.debug("response: " + response + " uploadedMetadata: " + uploadedMetadata);
		}
		
		writer.print(uploadedMetadata.toJSONObject());
		writer.close();
		if (response.containsHeader("Content-Type")) {
			response.setHeader("Content-Type", "text/json");
		} else {
			response.addHeader("Content-Type", "text/json");
		}

		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"HTTP Put is not supported. Use HTTP POST instead.");
	}

//	/**
//	 * Utility method that reads a byte array from an input stream using Guava.
//	 * 
//	 * @param in
//	 *            any InputStream containing some data
//	 * @return the InputStream's contents as a byte array; may be size 0 but
//	 *         will not be null.
//	 * @throws IOException
//	 *             if a read error occurs
//	 */
//	private byte[] readByteArray(InputStream in) throws IOException {
//	    
//	    return ByteStreams.toByteArray(in);
//	}

	@Override
	protected void initalizeEsapiServlet() {
		this.log = Logger.getLogger(UploadServlet.class.getCanonicalName()); // Tomcat logger

	}

}
