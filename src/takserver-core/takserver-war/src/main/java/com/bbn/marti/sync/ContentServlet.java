

package com.bbn.marti.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.naming.NamingException;

import org.apache.catalina.util.URLEncoder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;
import com.bbn.security.web.SecurityUtils;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import io.micrometer.core.instrument.Metrics;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tak.server.Constants;


/**
 * Servlet for retrieving the content
 */
@WebServlet("/sync/content")
public class ContentServlet extends EnterpriseSyncServlet implements AsyncListener {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ContentServlet.class);

	private static final long serialVersionUID = -4951951000582076313L;
	public static final String DEFAULT_FILENAME="EnterpriseSync.dat";

	@Autowired
	private MissionService missionService;

	@Autowired
	private CommonUtil martiUtil;

	/**
	 * Enum of the HTTP request parameters supported by this servlet. 
	 *
	 */
	public enum RequestParameters {
		UID,
		Hash,
		offset,
		length
	}

	private static final int DEFAULT_PARAMETER_LENGTH = 1024;

	private void getResource(AsyncContext async, HttpMethod method, boolean isAdmin, String groupVector) throws ServletException, IOException {
		
		logger.debug("is admin: {}", isAdmin);
		
		HttpServletResponse response = (HttpServletResponse) async.getResponse();
		
		Metrics.counter("DownloadMissionContent", "missions", "content").increment();

		initAuditLog((HttpServletRequest) async.getRequest());

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalStateException("empty group vector");
		}

		String remoteHost = "unidentified host";
		String context = "GET request parameters";
		
		InputStream contentStream = null;
		
		Metadata match = null;
		List<Metadata> matches = null;
		try {
			String requestHost = ((HttpServletRequest) async.getRequest()).getRemoteHost();
			if (requestHost != null && validator != null) {
				remoteHost = validator.getValidInput(context, requestHost, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
			}
			// Parse & validate the request parameters
			String uid = getParameter(((HttpServletRequest) async.getRequest()).getParameterMap(), RequestParameters.UID);
			String hash = getParameter(((HttpServletRequest) async.getRequest()).getParameterMap(), RequestParameters.Hash);
			Integer offset = getIntegerParameter(((HttpServletRequest) async.getRequest()).getParameterMap(), RequestParameters.offset);
			Integer length = getIntegerParameter(((HttpServletRequest) async.getRequest()).getParameterMap(), RequestParameters.length);

			String query = "uid: " + uid + "; hash: " + hash + "; offset " + offset + "; length " + length;
			if (logger.isDebugEnabled()) {
				logger.debug(query);
			}

			if (hash != null) {
				contentStream = enterpriseSyncService.getContentStreamByHash(hash, groupVector, isAdmin);
				
				try {
					matches = enterpriseSyncService.getMetadataByHash(hash, groupVector);
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("exception getting metadata " + e.getMessage(), e);
					}
				}
			} else if (uid != null){
				contentStream = enterpriseSyncService.getContentStreamByUid(uid, groupVector, isAdmin);
				matches = enterpriseSyncService.getMetadataByUid(uid, groupVector);
			}

			response.addHeader("api-version", Constants.API_VERSION);

			if (matches == null || matches.isEmpty()) {
				throw new NotFoundException("no metadata results for " + query);
			}

			if (contentStream == null) {
				if (logger.isErrorEnabled()) {
					logger.error("found null content stream for :" + StringUtils.normalizeSpace(query));
				}
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
			
			logger.debug("non-null content stream {} for hash {}", contentStream, hash);

			// Just take the first one on the list, which is the most recent
			match = matches.get(0);

			logger.debug("first found file metadata {}", match.toJSONObject());

			String mimeType = match.getFirst(Metadata.Field.MIMEType);
			if (validator != null && validator.isValidInput("MIME Type", mimeType, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false)) {
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

			String contentDisposition = "inline; filename=\"" + URLEncoder.DEFAULT.encode(filename, StandardCharsets.UTF_8) + "\"";
			if (validator != null && validator.isValidInput("Content Disposition", contentDisposition, "Filename", DEFAULT_PARAMETER_LENGTH, false)) {
				if (response.containsHeader("Content-Disposition")) {
					response.setHeader("Content-Disposition", contentDisposition);
				} else {
					response.addHeader("Content-Disposition", contentDisposition);
				}
			}

			String accept = ((HttpServletRequest) async.getRequest()).getHeader("Accept-Encoding");
			if (accept != null && accept.toLowerCase().contains("gzip")) {

				// gzip the content stream
				ByteArrayOutputStream gzipStream = new ByteArrayOutputStream();
				gzip(contentStream, gzipStream);
				contentStream = new ByteArrayInputStream(gzipStream.toByteArray());

				// Set encoding type of HTTP response
				if (response.containsHeader("Content-Encoding")) {
					response.setHeader("Content-Encoding", "gzip");
				} else {
					response.addHeader("Content-Encoding", "gzip");
				}
			}

			int totalSize = contentStream.available();

			response.setStatus(HttpServletResponse.SC_OK);

			if (method == HttpMethod.GET) {

				// apply offset param
				if (offset != null && offset > 0) {

					try {
						contentStream.skip(offset);
					} catch (Exception e) {
						throw new TakException("error applying offset parameter in request " + offset, e);
					}

					if (logger.isDebugEnabled()) {
						logger.debug("applying offset " + offset);
					}
				} else {
					offset = 0;
				}

				if (length != null && length > 0
						&& offset + length < totalSize) {
					response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				}

				try (OutputStream outStream = response.getOutputStream()) {

					if (outStream == null) {
						logger.error("response.getOutputStream() returned null!");
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						return;
					}

					if (length != null && length > 0) {
						IOUtils.copyLarge(contentStream, outStream, 0, length);
					} else {
						// use guava buffered stream conversion to copy data stream from database to servlet request OutputStream
						ByteStreams.copy(contentStream, outStream);
					}
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
			logger.debug("Query didn't return anything ", e);
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
		} catch (Exception e) {
			logger.error("Exception in getResource ", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			if (contentStream != null) {
				try {
					contentStream.close();
				} catch (Exception e) {
					logger.error("Exception closing content stream", e);
				}
			}
			async.complete();
		}
	}

	public static void gzip(InputStream is, OutputStream os) throws IOException {
		GZIPOutputStream gzipOs = new GZIPOutputStream(os);
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		while ((bytesRead = is.read(buffer)) > -1) {
			gzipOs.write(buffer, 0, bytesRead);
		}
		is.close();
		os.close();
		gzipOs.close();
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
		
		final boolean isAdmin = commonUtil.isAdmin((HttpServletRequest) request);
		final String groupVector = commonUtil.getGroupBitVector((HttpServletRequest) request);
		
		logger.debug("is admin: {}", isAdmin);
		
		Metrics.counter("DownloadMissionContent", "missions", "content").increment();

		AsyncContext async = request.startAsync();

		async.addListener(this);

		if (logger.isDebugEnabled()) {
			logger.debug("GET resource");
		}
		
		// Set the timeout for async context for file download (ms)
		async.setTimeout(CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeDownloadTimeoutMillis());

		async.start(() -> {
			try {
				getResource(async, HttpMethod.GET, isAdmin, groupVector);
			} catch (Exception e) {
				logger.error("error processing getResource", e);
			} 
		});		
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		final boolean isAdmin = commonUtil.isAdmin((HttpServletRequest) request);
		final String groupVector = commonUtil.getGroupBitVector((HttpServletRequest) request);
		
		logger.debug("is admin: {}", isAdmin);
		
		AsyncContext async = request.startAsync();

		if (logger.isDebugEnabled()) {
			logger.debug("HEAD resource");
		}

		// Set the timeout for async context for file download (ms)
		async.setTimeout(CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeDownloadTimeoutMillis());
		
		async.start(() -> {
			try {
				getResource(async, HttpMethod.HEAD, isAdmin, groupVector);
			} catch (Exception e) {
				logger.error("error processing HEAD getResource", e);
			} 
		});		
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

	private String getParameter(Map<String, String[]> parameters, RequestParameters parameter) throws ValidationException, IntrusionException {
		String result = null;
		String[] values = SecurityUtils.getCaseInsensitiveParameter(parameters, parameter.toString());
		if (values != null && values.length > 0) {
			result = values[0];
			if (validator != null) {
				result = validator.getValidInput("Parsing " + parameter.name(), result, "MartiSafeString", DEFAULT_PARAMETER_LENGTH, false);
			}
		}
		return result;
	}

	private Integer getIntegerParameter(Map<String, String[]> parameters, RequestParameters parameter) throws ValidationException, IntrusionException {
		try {
			String result = getParameter(parameters, parameter);
			if (result != null) {
				return Integer.parseInt(result);
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.error("exception in getIntegerParameter", e);
			return null;
		}
	}

	@Override
	protected void initalizeEsapiServlet() {
		 this.log = Logger.getLogger(ContentServlet.class.getCanonicalName());	
	}

	@Override
	public void onComplete(AsyncEvent event) {
		try {
			if (CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue()
					.getMissionConcurrentDownloadLimit() == null) {
				return;
			}

			int status = ((HttpServletResponse)event.getSuppliedResponse()).getStatus();
			String feedName = event.getSuppliedRequest().getParameter("feedName");

			if (status == HttpServletResponse.SC_OK && !Strings.isNullOrEmpty(feedName)) {
				String hash = event.getSuppliedRequest().getParameter("hash");
				missionService.checkAndSendPendingNotifications(feedName, hash, 1,
						martiUtil.getGroupVectorBitString(((HttpServletRequest)event.getSuppliedRequest())));
			}
		} catch (Exception e) {
			logger.error("exception in AsyncListener.onComplete", e);
		}
	}

	@Override
	public void onTimeout(AsyncEvent event) {
		logger.error("async timeout occurred");
	}

	@Override
	public void onError(AsyncEvent event) {
		logger.error("async error occurred");
	}

	@Override
	public void onStartAsync(AsyncEvent event) {
		logger.error("onStartAsync should not be called");
	}
}
