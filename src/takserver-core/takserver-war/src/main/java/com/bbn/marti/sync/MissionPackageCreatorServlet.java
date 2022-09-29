

package com.bbn.marti.sync;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.bbn.marti.util.CommonUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.util.DateUtil;
import com.google.common.base.Strings;

/**
 * Servlet that accepts POST requests (only) to upload mission packages to the
 * Enterprise Sync database. 
 *
 * Requires the parameters "senderuid", "hash", and "filename".
 */
public class MissionPackageCreatorServlet extends EnterpriseSyncServlet {
	public static final String SIZE_LIMIT_VARIABLE_NAME = "EnterpriseSyncSizeLimitMB";
	private static final long serialVersionUID = -1782550124681449153L;
	private static final String TAG = "Mission Package Creator Servlet: ";	
	private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MissionPackageCreatorServlet.class);

	@Autowired
	private CoreConfig coreConfig;

	@Autowired
	private SubmissionInterface submission;

	/**
	 * Required parameter enum for posting a mission package.
	 * the parameter string is the expected URL argument. The metadata field is where the parameter gets mapped to.
	 * 
	 * A request is queried by iterating over all of the parameter strings in the enum, and emplacing the param value into the metadata field.
	 * @note One of the parameters MUST map to a metdata uid field - getUid() is used to query the database for potential collisions.
	 */
	private static enum PostParameter {
		FILENAME    ("filename", Metadata.Field.Name, true, null);

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

		protected boolean isRequired() {
			return defaultValue == null;
		}

		protected boolean isOptional() {
			return !isRequired();
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
	}

	@Override
	protected void initalizeEsapiServlet() {
		this.log = Logger.getLogger(MissionPackageCreatorServlet.class.getCanonicalName()); // Tomcat logger

	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	private static String ensureEndsIn(final String str, final String end) {
		if(!str.endsWith(end))
			return str + end;
		return str;
	}

	public static String getFileNameFromPart(final Part part) {
		String filename = null;
		String cd = part.getHeader("content-disposition");
		logger.trace("content-disposition=" + cd);
		String filenameToken = "filename=\"";
		if (cd != null && cd.contains(filenameToken)) {
			try {
				int filenameIndex = cd.indexOf(filenameToken);
				String filenameFragment = cd.substring(filenameIndex + filenameToken.length()).trim();
				logger.trace("filenameFragment=" + filenameFragment);
				String[] pieces = filenameFragment.split("\"");
				for(String piece : pieces) {
					logger.trace("piece=" + piece);
				}
				// Strip off closing quote 
				//filename = pieces[0].substring(0, pieces[0].length() - 1);
				filename = pieces[0];
			} catch (Exception e) {
				/* any exception here can be safely ignored since this is a best-effort try to get the filename */
			}
		}
		return filename;
	}

	private static String[] getContacts(HttpServletRequest request) throws IOException, ServletException {
		// Get the list of people to send an advertisement to (OPTIONAL)
		String[] contacts = request.getParameterValues("contacts");
		if(contacts == null || contacts.length == 0) {
			List<String> contactList = new LinkedList<String>();
			for(Part part : request.getParts()) {
				if(part.getName().equalsIgnoreCase("contacts")) {
					try(InputStream in = part.getInputStream()) {
						StringWriter writer = new StringWriter();
						IOUtils.copy(in, writer);
						contactList.add(writer.toString());
					}
				}
			}
			contacts = contactList.toArray(new String[contactList.size()]);
		}
		return contacts;
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException 
	{

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


		Metadata uploadedMetadata = null;
		try {
			initAuditLog(request);

			ByteArrayOutputStream zippedByteStream = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(zippedByteStream);

			Collection<Part> parts = request.getParts();
			logger.debug("Uploading multi-part content with " + parts.size() + " parts.");
			String firstFilename = null;
			for(Part part : parts) {
				if("assetfile".equalsIgnoreCase(part.getName()) ||
						"resource".equalsIgnoreCase(part.getName()))
				{
					try (InputStream payloadIn = part.getInputStream()){
						String filename = getFileNameFromPart(part);
						if(firstFilename == null) firstFilename = filename;
						if(filename != null && filename.length() > 0 && payloadIn != null) {
							zip.putNextEntry(new ZipEntry(filename));
							IOUtils.copy(payloadIn, zip);
							payloadIn.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
						return;
					}
				}
			}

			zip.close();

			// map post parameters to metadata field entries 
			Map<String,String[]> paramsMap = request.getParameterMap();
			Metadata toStore = new Metadata();
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
				logger.debug("Request is: " + toStore.toJSONObject().toJSONString());
				//log.fine("Content length is " + payloadIn.available());
			}

			// Get the Zipped-up byte[]
			byte[] zippedBytes = zippedByteStream.toByteArray();

			// Compute the SHA-256 hash
			MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
			msgDigest.update(zippedBytes);
			byte[] mdbytes = msgDigest.digest();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			String shaHash = sb.toString();
			toStore.set(Metadata.Field.UID, shaHash); // make the hash of this missionpackage the UID
			toStore.set(Metadata.Field.Keywords, "missionpackage");

			// Make sure the ZIP file ends in .zip
			String zipFileName = toStore.getFirst(Metadata.Field.Name);
			if(zipFileName == null || zipFileName.isEmpty()) {
				zipFileName = firstFilename + ".zip";
			}
			zipFileName = ensureEndsIn(zipFileName, ".zip");
			toStore.set(Metadata.Field.Name, zipFileName);

			toStore.set(Metadata.Field.SubmissionUser, SecurityContextHolder.getContext().getAuthentication().getName());
			// Insert into the data store
			uploadedMetadata = enterpriseSyncService.insertResource(toStore, zippedBytes, groupVector);

			// Pull out the primary key
			String primaryKey = uploadedMetadata.getFirst(Metadata.Field.PrimaryKey);
			String requestUrl = request.getRequestURL().toString();
			String url = requestUrl.substring(0, requestUrl.indexOf(request.getServletPath()))
					+ "/Marti/sync/content?hash=" + shaHash; // yes: uid will be set to the shaHash in the CoT message (see below)

			// Get contacts to send to
			String[] contacts = getContacts(request);

			if(contacts != null && contacts.length > 0) {
				logger.debug("Sending to the following contacts: " );
				for(String contact : contacts) {
					logger.debug(" `- " + contact);
				}

				// Generate the CoT message
				String cotMessage = CommonUtil.getFileTransferCotMessage(
						/*String uid*/ shaHash, // yes: set the UID to be the hash, this makes it consistent with ATAK-generated mission packages
						/*String shaHash*/ shaHash, 
						/*String callsign*/  SecurityContextHolder.getContext().getAuthentication().getName(),
						/*String filename*/ uploadedMetadata.getFirst(Metadata.Field.Name), 
						/*String url*/ url,
						/*long sizeInBytes*/ zippedBytes.length,
						/*String[] contacts*/ contacts);

				try {
					logger.debug("submitting mission package announce CoT message: " + cotMessage);

					submission.submitCot(cotMessage, commonUtil.getGroupsFromRequest(request));

				} catch (Exception exi) {

					logger.error("error submitting mission package announce message " + exi.getMessage());

					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
							"Could not connect to Marti Core to send Mission Package to desired contacts.");
					exi.printStackTrace();
					return;
				}
			} else {
				logger.warn("No contacts to send to - not sending mission package announce");
			}

			if (response.containsHeader("Content-Type")) {
				response.setHeader("Content-Type", "text/plain");
			} else {
				response.addHeader("Content-Type", "text/plain");
			}

			response.setStatus(HttpServletResponse.SC_OK);
			PrintWriter writer = response.getWriter();
			writer.print(url);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
			return;
		}
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"HTTP Put is not supported. Use HTTP POST instead.");
	}
}
