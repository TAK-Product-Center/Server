

package com.bbn.marti;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ocpsoft.prettytime.PrettyTime;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.service.kml.KMLService;
import com.bbn.marti.util.CommonUtil;
import com.bbn.security.web.MartiValidator;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import tak.server.Constants;

public class LatestKMLServlet extends EsapiServlet {
	private static final long serialVersionUID = 8019579139250658614L;
	public static final String CONTEXT = "LatestKML";
	public static final SimpleDateFormat sqlDateFormat = new SimpleDateFormat(Constants.SQL_DATE_FORMAT);
	public static final String DEFAULT_FILENAME_BASE = "TAK-Latest";
	PrettyTime prettyTimeFormat = new PrettyTime();

	// slf4j logger
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LatestKMLServlet.class);

	// The bean for these services are obtained from the spring context
	@Autowired
	private KMLService kmlService;
	
	@Autowired
    private CommonUtil martiUtil;

	public enum QueryParameter {
		cotType,
		refreshRate,
		secago,
		startTime,
		endTime,
		format,
		interval,
		multiTrackThreshold,
		extendedData,
		optimizeExport,
		groups
	}

	public enum AllowedFormat {
		kml,
		kmz
	}

	protected Map<String, HttpParameterConstraints> requiredHttpParameters;
	protected Map<String, HttpParameterConstraints> optionalHttpParameters;
	
	@Autowired
	protected AltitudeConverter converter;

	@Override
	protected void initalizeEsapiServlet()
	{
		logger.debug("LatestKMLServlet initializeEsapiServlet");
		log = Logger.getLogger(LatestKMLServlet.class.getCanonicalName());

		requiredHttpParameters = new HashMap<String, HttpParameterConstraints>();

		optionalHttpParameters = new HashMap<String, HttpParameterConstraints>();
		optionalHttpParameters.put(QueryParameter.cotType.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.interval.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.Double, MartiValidator.SHORT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.startTime.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.Timestamp, MartiValidator.DEFAULT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.endTime.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.Timestamp, MartiValidator.DEFAULT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.refreshRate.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.NonNegativeInteger, MartiValidator.SHORT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.secago.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.NonNegativeInteger, MartiValidator.SHORT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.format.name(), 
				new HttpParameterConstraints(MartiValidator.Regex.SafeString, MartiValidator.SHORT_STRING_CHARS));

	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse unsafeResponse) 
			throws ServletException, IOException {
		processHttpQuery(request, unsafeResponse);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse unsafeResponse) 
			throws ServletException, IOException {
		processHttpQuery(request, unsafeResponse);
	}

	private void processHttpQuery(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
	    
	    initAuditLog(request);

		long start = System.nanoTime();
		
		// process and validate request parameters

		sqlDateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
		Map<String, String[]> httpParameters = validateParams("LatestKMLServlet", request, response, 
				requiredHttpParameters, optionalHttpParameters);
		if(httpParameters == null) {
			// if that is null, then validateParams already did response.sendError
			return;
		}

         String cotType = getParameterValue(httpParameters, QueryParameter.cotType.name());
                if(cotType != null) {
                   if (validator != null) {
			try {
				cotType = validator.getValidInput(CONTEXT, cotType, MartiValidator.Regex.CotType.name(), 
						MartiValidator.DEFAULT_STRING_CHARS, true);
			} catch (ValidationException ex) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsafe parameter value detected.");
				return;
			} catch (IntrusionException ex) {
				log.severe(ex.getMessage());
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsafe parameter value detected.");
				return;
			}
                   }
		} else {
                   cotType = "";
                }


		int secAgoInt = 0;
		String secagoArg = request.getParameter("secago");

		if (secagoArg != null) {
			try {
				secAgoInt = Integer.parseInt(secagoArg);
			} catch (NumberFormatException ex) {
				log.severe(ex.getMessage());
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal value for parameter \"secago\"" );
				return;
			}
		}

		OutputStream servletReponseOutputStream = null;
		String contentDisposition = "filename=" + DEFAULT_FILENAME_BASE + "-" + cotType + ".kml";
		try {
			contentDisposition = validator.getValidInput("Content Disposition", contentDisposition, "Filename", MartiValidator.DEFAULT_STRING_CHARS, false);
		} catch (ValidationException ex) {
			log.severe(ex.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsafe filename value detected.");
			return;
		}

		response.setContentType("application/vnd.google-earth.kml+xml");
		response.setHeader("Content-Disposition", contentDisposition);

		try {

			if (kmlService == null || martiUtil == null) {
				throw new RuntimeException("latestKMLController bean not initialized");
			}
			
			String groupVector = null;

	        try {
	         
	            // Get group vector for the user associated with this session
	            groupVector = martiUtil.getGroupBitVector(request);
	            log.finer("groups bit vector: " + groupVector);
	        } catch (Exception e) {
	            log.fine("exception getting group membership for current web user " + e.getMessage());
	        }

			logger.debug("latestKMLService: " + kmlService + " calling process for cotType: " + cotType + " secAgoInt: " + secAgoInt + " groupVector: " + groupVector);

			// do the KML processing for the requested CoT parameters
			Kml kml = kmlService.process(cotType, secAgoInt, groupVector);

			servletReponseOutputStream = response.getOutputStream();
			
			// send the KML data as the response through the servlet OutputStream
			kml.marshal(servletReponseOutputStream);
			
		} catch (Exception e) {
			logger.error("Exception completing KML request", e);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Database error: " + e.getMessage());
			return;
		} finally {
			if (servletReponseOutputStream != null) {
				try {
					servletReponseOutputStream.flush();
					servletReponseOutputStream.close();
				} catch (IOException e) {
					log.warning("exception closing servlet output stream " + e.getClass().getName() + " " + e.getMessage());
				}
			}
		}

		long stop = System.nanoTime();
		long diff = stop - start;
		long diffSeconds = diff / 1000000000;
		double diffDecimal = ((double) (diff % 1000000000)) / 1000000000.0;
		double seconds = ((double) diffSeconds) + diffDecimal;

		logger.debug("Query took " + seconds);
	}
}
