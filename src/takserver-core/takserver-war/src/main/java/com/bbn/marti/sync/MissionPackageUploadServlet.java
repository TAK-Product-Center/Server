

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.owasp.esapi.errors.ValidationException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.sync.Metadata.Field;
import com.google.common.base.Strings;

/**
 * Servlet that accepts POST requests (only) to upload mission packages to the
 * Enterprise Sync database. 
 * 
 * path: /Marti/sync/missionupload
 *
 */
public class MissionPackageUploadServlet extends UploadServlet {
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
		requiredParameters.add("filename");

		optionalParameters.add(Metadata.Field.CreatorUid.toString());
		optionalParameters.add(Metadata.Field.Tool.toString());
		optionalParameters.add(Metadata.Field.Hash.toString());
	}
	/**
	 * Required parameter enum for posting a mission package.
	 * the parameter string is the expected URL argument. The metadata field is where the parameter gets mapped to.
	 * 
	 * A request is queried by iterating over all of the parameter strings in the enum, and emplacing the param value into the metadata field.
	 * @note One of the parameters MUST map to a metdata uid field - getUid() is used to query the database for potential collisions.
	 */
	private static enum PostParameter {
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

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);

		uploadSizeLimitMB = coreConfig.getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB();

		logger.info("Enterprise Sync upload limit is " + uploadSizeLimitMB + " MB");

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
			// Get group vector for the user associated with this session
			groupVector = commonUtil.getGroupBitVector(request);
		} catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("exception getting group membership for current web user ", e);
			}
		}

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalStateException("empty group vector");
		}

		Metadata metadataResult = null;
		try (ServletInputStream requestInputStream = request.getInputStream()) {
			initAuditLog(request);

			// validate all parameters -- enforces required/optional parameter presence, throws an exception if invalid
			if (validator != null) {
				validator.assertValidHTTPRequestParameterSet("Mission package upload", request,
						requiredParameters,
						optionalParameters);
			}

			InputStream payloadInputStream = null ;
			String mimeType = request.getHeader("Content-Type");
			if (mimeType == null || !mimeType.contains("multipart/form-data")) {
				String msg = "Data package upload must use multipart/form-data POST. See https://everything.curl.dev/http/multipart for more info about multipart POST. Part name should be named 'assetfile'";
				logger.error(msg);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
				return;
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
					payloadInputStream = part.getInputStream();
				} else {
					if (logger.isTraceEnabled()) {
						for (Part myPart : parts) {
							logger.trace(myPart.getName() + ": " + myPart.getContentType() );
						}
					}
					logger.error("Unable to find content in multi-part submission");
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Upload request was not formatted in a way Marti can understand.\n"
									+ "Please try a different browser.");
					return;
				}
			}

			// map post parameters to metadata field entries 
			Map<String,String[]> paramsMap = request.getParameterMap();
			Metadata requestMetadata = new Metadata();
			for (PostParameter param : PostParameter.values()) {
				String[] args = paramsMap.get(param.getParameterString());
				if (param.isRequired()
						&& args == null) {
					// required param not present
					String message = String.format("Required parameter %s is not present", param.getParameterString());
					if (logger.isWarnEnabled()) {
						logger.warn(TAG + message);
					}
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
					return;
				} else if (args == null) {
					// optional param not present
					requestMetadata.set(param.getMetadataField(), param.getDefault());
				} else {
					// param present (required present, optional present)
					requestMetadata.set(param.getMetadataField(), args);
				}
				if (!isNewDatabaseEntry(requestMetadata, groupVector)) {
					// send back response message with same uri as was given to the existing message
					String message = String.format("HTTP post attempting to overwrite existing database entry with same values. Request: " + requestMetadata.toJSONObject().toJSONString());
					if (logger.isWarnEnabled()) {
						logger.warn(message);
					}
					// special response for attempting to overwrite
					response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
					return;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Request is: " + requestMetadata.toJSONObject().toJSONString());
				logger.debug("Content length is " + payloadInputStream.available());
			}
			requestMetadata.set(Metadata.Field.SubmissionUser, SecurityContextHolder.getContext().getAuthentication().getName());

			// insert row in resource as stream, and use hash as uid (calculated by database)
			metadataResult = enterpriseSyncService.insertResourceStreamUID(requestMetadata, payloadInputStream, groupVector, false);
			
			if (logger.isDebugEnabled()) {
				logger.debug("result hash: " + metadataResult.getHash() + " uid: " + metadataResult.getUid());
			}
			
			// if plugin classname is set, notify the plugin with the file upload event. The notification event will include the Metadata object and will not include the full byte[] payload.
			String pluginClassnames[] = paramsMap.get(Metadata.Field.PluginClassName.name());
			if (pluginClassnames != null) {
				String pluginClassname = pluginClassnames[0]; // To be consistent with UploadServlet where we allow only 1 pluginClassname
				logger.info("~~~ Notifying the plugin {} with file upload event", pluginClassname);
				pluginManager.onFileUpload(pluginClassname, requestMetadata);
			}

			// retrieve host name from request
			String responseStr = String.format("%s/Marti/sync/content?hash=%s", MissionPackageQueryServlet.getBaseUrl(request), metadataResult.getUid());

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
			if (logger.isErrorEnabled()) {
				logger.error("error processing and storing data package", e);
			}
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
	 * Executes a database query to check if the metadata entry is a unique filename and hash pair.
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
