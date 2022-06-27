

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.owasp.esapi.errors.ValidationException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bbn.marti.remote.CoreConfig;
import com.google.common.base.Strings;

/**
 * Servlet that accepts POST requests (only) to upload mission packages to the
 * Enterprise Sync database. 
 *
 * Requires the parameters "senderuid", "hash", and "filename".
 */
//@WebServlet("/sync/missionupload")
//@MultipartConfig
public class MissionPackageUploadServlet extends EnterpriseSyncServlet {
	public static final String SIZE_LIMIT_VARIABLE_NAME = "EnterpriseSyncSizeLimitMB";
	private static final long serialVersionUID = -1782550124681449153L;
	private static final String MISSION_PACKAGE_KEYWORD = "missionpackage";
	private static final String TAG = "Mission Package Upload Servlet: ";	
	private static Set<String> optionalParameters = new HashSet<String>();
	private static Set<String> requiredParameters = new HashSet<String>();

	@Autowired
	private CoreConfig coreConfig;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MissionPackageUploadServlet.class);

	static {
		// static initializer for all required parameters
		requiredParameters = new HashSet<String>(PostParameter.values().length);
		requiredParameters.add("hash");
		requiredParameters.add("filename");

		optionalParameters.add(Metadata.Field.CreatorUid.toString());
		optionalParameters.add(Metadata.Field.Tool.toString());
	}
	/**
	 * Required parameter enum for posting a mission package.
	 * the parameter string is the expected URL argument. The metadata field is where the parameter gets mapped to.
	 * 
	 * A request is queried by iterating over all of the parameter strings in the enum, and emplacing the param value into the metadata field.
	 * @note One of the parameters MUST map to a metdata uid field - getUid() is used to query the database for potential collisions.
	 */
	private static enum PostParameter {
		HASH        ("hash", 	 Metadata.Field.UID, true, null),
		FILENAME    ("filename", Metadata.Field.Name, true, null),
		MIMETYPE 	("mimetype", Metadata.Field.MIMEType, false, "application/x-zip-compressed"),
		KEYWORD    	("keyword",  Metadata.Field.Keywords, false, MISSION_PACKAGE_KEYWORD),
		TOOL        ("tool",  Metadata.Field.Tool, false, "public"),
		CREATORUID  ("creatorUid", Metadata.Field.CreatorUid, true, "");

		private final String param; // string value of the param
		private final Metadata.Field field; // metadata field that this param is entered into
		private final boolean comp; // flag for indicating whether the param is to be used for distinguishing db-entries
		private final String defaultValue; // value to be used for the db entry if a parameter is not given 

		// protected constructor
		PostParameter(String paramName, Metadata.Field metaField, boolean dbComparisonKey, String defaultValue) {
			this.param = paramName;
			this.field = metaField;
			this.comp = dbComparisonKey;
			this.defaultValue = defaultValue;
		}

		protected String getParameterString() {
			return param;
		}

		protected Metadata.Field getMetadataField() {
			return field;
		}

		protected boolean isComparableKey() {
			return comp;
		}

		protected boolean isRequired() {
			return defaultValue == null;
		}

		protected String getDefault() {
			if (isRequired()) throw new IllegalStateException("called getDefault() on a required param");
			return defaultValue;
		}
	}

	private int uploadSizeLimitMB;
	private long uploadSizeLimitBytes;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);

		uploadSizeLimitMB = coreConfig.getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB();

		logger.info("Enterprise Sync upload limit is " + uploadSizeLimitMB + " MB");

		uploadSizeLimitBytes = ((long) uploadSizeLimitMB) * 1000000L;
	}

	@Override
	protected void initalizeEsapiServlet() {
		this.log = Logger.getLogger(MissionPackageUploadServlet.class.getCanonicalName()); // Tomcat logger

	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {

		String groupVector = null;

		try {
			logger.debug("martiUtil: " + martiUtil);

			// Get group vector for the user associated with this session
			groupVector = martiUtil.getGroupBitVector(request);
			logger.trace("groups bit vector: " + groupVector);
		} catch (Exception e) {
			logger.debug("exception getting group membership for current web user " + e.getMessage());
		}

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalStateException("empty group vector");
		}

		Metadata uploadedMetadata = null;
		try {
			initAuditLog(request);

			// validate all parameters -- enforces required/optional parameter presence, throws an exception if invalid
			if (validator != null) {
				validator.assertValidHTTPRequestParameterSet("Mission package upload", request,
						requiredParameters,
						optionalParameters);
			}

			InputStream payloadIn = null ;
			String mimeType = request.getHeader("Content-Type");
			if (mimeType == null || !mimeType.contains("multipart/form-data")) {
				logger.debug("Uploading content.");
				request.getInputStream(); // we already did this above
			} else {
				Collection<Part> parts = request.getParts();
				logger.debug("Uploading multi-part content with " + parts.size() + " parts.");
				// ATAK sends a part called "assetfile"
				Part part = request.getPart("assetfile");
				if (part == null) {
					// Firefox and Chrome browsers send a part called "resource"
					part = request.getPart("resource");
				}

				if (part != null) {
					payloadIn = part.getInputStream();
				} else {
					for (Part myPart : parts) {
						logger.trace(myPart.getName() + ": " + myPart.getContentType() );
					}
					log.severe("Unable to find content in multi-part submission");
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Upload request was not formatted in a way Marti can understand.\n"
									+ "Please try a different browser.");
					return;
				}
			}

			// map post parameters to metadata field entries 
			Map<String,String[]> paramsMap = request.getParameterMap();
			Metadata toStore = new Metadata();
			byte [] data = null;
			for (PostParameter param : PostParameter.values()) {
				String[] args = paramsMap.get(param.getParameterString());
				if (param.isRequired()
						&& args == null) {
					// required param not present
					String message = String.format("Required parameter %s is not present", param.getParameterString());
					logger.warn(TAG + message);
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
					return;
				} else if (args == null) {
					// optional param not present
					toStore.set(param.getMetadataField(), param.getDefault());
				} else {
					// param present (required present, optional present)
					toStore.set(param.getMetadataField(), args);
				}
				if (!isNewDatabaseEntry(toStore, groupVector)) {
					// send back response message with same uri as was given to the existing message
					String message = String.format("HTTP post attempting to overwrite existing database entry with same values. Request: " + toStore.toJSONObject().toJSONString());
					logger.warn(message);
					// special response for attempting to overwrite
					response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
					return;
				}
			}
			logger.debug("Request is: " + toStore.toJSONObject().toJSONString());
			logger.debug("Content length is " + payloadIn.available());
			data = new byte[payloadIn.available()];
			payloadIn.read(data);
			toStore.set(Metadata.Field.SubmissionUser, SecurityContextHolder.getContext().getAuthentication().getName());
			long payloadByteCount = (long) data.length;
			if (payloadByteCount > uploadSizeLimitBytes) {
				String message = "Uploaded file exceeds server's size limit of " + uploadSizeLimitBytes
						+ " MB! (limit is set in server's conf/context.xml)";
				logger.warn(message);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				return;
			} else if (payloadByteCount == 0) {
				String message = "HTTP post body has no content.";
				logger.warn(message);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				return;
			}

			uploadedMetadata = enterpriseSyncService.insertResource(toStore, data, groupVector);

			// retrieve host name from request
			String responseStr = String.format("%s/Marti/sync/content?hash=%s", MissionPackageQueryServlet.getBaseUrl(request), uploadedMetadata.getUid());

			if (response.containsHeader("Content-Type")) {
				response.setHeader("Content-Type", "text/plain");
			} else {
				response.addHeader("Content-Type", "text/plain");
			}
			response.setStatus(HttpServletResponse.SC_OK);
			PrintWriter writer = response.getWriter();
			writer.print(responseStr);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
			logger.warn(e.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
			return;
		}
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"HTTP Put is not supported. Use HTTP POST instead.");
	}

	/**
	 * Executes a database query to see if the proposed metadata entry is a unique filename and hash double.
	 */
	private boolean isNewDatabaseEntry(Metadata proposed, String groupVector) throws SQLException, NamingException, ValidationException {
		String databaseUid = proposed.getUid();
		List<Metadata> matchingUids = enterpriseSyncService.getMetadataByUid(databaseUid, groupVector);

		// search for a parameter -> metadata mapping that different arguments
		for (Metadata entry : matchingUids) {
			boolean same = true;
			for (PostParameter param : PostParameter.values()) {
				if (param.isComparableKey()) {
					Metadata.Field testAttr = param.getMetadataField();
					String mine = proposed.getFirstSafely(testAttr);
					String theirs = entry.getFirstSafely(testAttr);

					if (!mine.equals(theirs)) {
						// found attribute with different value -- skip
						same = false;
						break;
					}
				}
			}
			if (same) {
				// never found a differing argument -- identical entry
				return false;
			}
		}
		// found a differing argument in all entries returned (if any were)
		return true;
	}
}
