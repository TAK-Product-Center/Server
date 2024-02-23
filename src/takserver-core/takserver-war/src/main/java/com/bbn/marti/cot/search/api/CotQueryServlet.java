

package com.bbn.marti.cot.search.api;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.EsapiServlet;
import com.bbn.marti.HttpParameterConstraints;
import com.bbn.marti.cot.search.model.query.Column;
import com.bbn.marti.cot.search.model.query.DeliveryProtocol;
import com.bbn.marti.cot.search.model.query.GeospatialQuery;
import com.bbn.marti.cot.search.model.query.ImageOption;
import com.bbn.marti.cot.search.service.CotQueryParameter;
import com.bbn.marti.cot.search.service.CotSearchService;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;

import tak.server.Constants;

/**
 * Servlet for processing CoT queries via HTTP. At this point, the servlet
 * supports only one canned query.
 * 
 * 
 */
public class CotQueryServlet extends EsapiServlet {

    /**
     * Enum of the HTTP request parameters recognized by this servlet.
     * They are usually interpreted in a case-insensitive manner.
     * @see getParameterValue
     * 
     *
     */
    private Map<String, HttpParameterConstraints> requiredHttpParameters;
    private Map<String, HttpParameterConstraints> optionalHttpParameters;

    private static final DeliveryProtocol DEFAULT_DELIVERY_PROTOCOL = DeliveryProtocol.TCP;

    private static final long serialVersionUID = 4700270460940395186L;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CotQueryServlet.class);
    
    @Autowired
    private CotSearchService cotSearchService;

    /**
     * GET and POST are both supported and do the same thing. 
     * See Marti Interface Specification for required parameters.
     */
    @Override
    public void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        log.fine("processing GET request");
        try {
            processHttpQuery(request, response);
        } catch (Exception ex) {
            log.severe(ex.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /**
     * GET and POST are both supported and do the same thing.
     * See Marti Interface Specification for required parameters.
     */
    @Override
    public void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        log.fine("processing POST request");
        try {
            processHttpQuery(request, response);
        } catch (Exception ex) {
            log.severe(ex.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /**
     * Processes a database query that comes in the form of an HTTP request.
     * Query parameters are defined by HTTP request parameters.
     * 
     * @see doPost
     * @param request HTTP request; see doPost documentation for required parameters
     * @param response
     *            HTTP response whose body is safe to display to a user's
     *            browser (without information leakage)
     * @throws ServletException
     * @throws IOException
     *             if there is a failure sending an HTTP error response
     */
    private void processHttpQuery(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Entering processHttpQuery");

        initAuditLog(request);

        String esapiContext = "QueryServlet";
        SimpleDateFormat CotDateFormat = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
        CotDateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
        Map<String, String[]> httpParameters = validateParams(esapiContext, request, response, 
                requiredHttpParameters, optionalHttpParameters);

        if (httpParameters == null) {
            return;
        }
        String hostName = getParameterValue(httpParameters, CotQueryParameter.hostName.name());
        String protocolString = getParameterValue(httpParameters, CotQueryParameter.protocol.name());
        DeliveryProtocol protocol = DEFAULT_DELIVERY_PROTOCOL;
        if (protocolString != null && !protocolString.isEmpty()) {
            try {
                protocol = DeliveryProtocol.valueOf(protocolString.toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warning("Unrecognized value for " + CotQueryParameter.protocol.toString() + "; ignoring.");
            }
        }

        String portString = getParameterValue(httpParameters, CotQueryParameter.port.name());

        Integer portNumber = null;
        try {
            portNumber = Integer.parseInt(portString);
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid port number");
            return;
        }

        StringBuilder predicateBuilder = new StringBuilder();
        List<Object> queryParameters = new LinkedList<Object>();

        // Whether the user wants all results or only the latest for each UID
        String latestByUidString = getParameterValue(httpParameters, CotQueryParameter.latestByUid.name());
        boolean latestByUid = (latestByUidString == null) ? false : Boolean.parseBoolean(latestByUidString);
        // Whether user wants messages played at the relative times they were received
        String replayModeString = getParameterValue(httpParameters, CotQueryParameter.replayMode.name());
        boolean replayMode = (replayModeString == null) ? false : Boolean.parseBoolean(replayModeString);
        Double replaySpeed = null;
        try {
            replaySpeed = Double.parseDouble(getParameterValue(httpParameters, CotQueryParameter.replaySpeed.name()));
            log.fine("Replay speed set to " + replaySpeed);
        } catch (NullPointerException npe) {
            // Leave replay speed at null
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad numeric format for " 
                    + CotQueryParameter.replaySpeed.toString());
        }
        if (replayMode && replaySpeed < 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, CotQueryParameter.replaySpeed.toString() + 
                    " cannot be negative.");
        }

        // Constraint on maximum number of results.
        Integer maximumResults = null;
        String value = getParameterValue(httpParameters, CotQueryParameter.maximumResults.name());
        if (value != null) {
            try {
                maximumResults = Integer.parseInt(value);
                log.finer("Maximum results set to " + maximumResults);
            } catch (NumberFormatException ex) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Number format error for " 
                        + CotQueryParameter.maximumResults.toString());
                return;
            } 

            if (maximumResults != null && maximumResults < 1) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                        CotQueryParameter.maximumResults.toString() + " may not be less than 1. ");
                return;
            }
        }
        // Constraint on CoT type
        value = getParameterValue(httpParameters, CotQueryParameter.cotType.name());
        if (value != null) {
            predicateBuilder.append(Column.cot_type.toString());
            predicateBuilder.append(" ~ ?");
            queryParameters.add(value);
        }

        // Constraint on UID
        value = getParameterValue(httpParameters, CotQueryParameter.uid.name());
        if (value != null) {
            if (predicateBuilder.length() > 0) {
                predicateBuilder.append(" AND ");
            }
            predicateBuilder.append(Column.uid.toString());
            predicateBuilder.append(" ~ ?");
            queryParameters.add(value);

        }

        // Constraints on event start time
        value = getParameterValue(httpParameters, CotQueryParameter.minimumStartTime.name());
        if (value != null) {
            try {
                Date startDate = CotDateFormat.parse(value);
                queryParameters.add(new Timestamp(startDate.getTime()));
            } catch (ParseException ex) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date format. An example of the "
                        + " correct date format is: 2001-03-29T16:41:03.5Z");
                return;
            }

            if (predicateBuilder.length() > 0) {
                predicateBuilder.append(" AND ");
            }
            predicateBuilder.append("start >= ? ");
        }
        value = getParameterValue(httpParameters, CotQueryParameter.maximumStartTime.name());
        if (value != null) {
            try {
                Date stopDate = CotDateFormat.parse(value);
                queryParameters.add(new Timestamp(stopDate.getTime()));
            } catch (ParseException ex) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date format");
                return;
            }

            if (predicateBuilder.length() > 0) {
                predicateBuilder.append(" AND ");
            }
            predicateBuilder.append("start <= ? ");
        }

        // Geospatial constraint
        value = getParameterValue(httpParameters, CotQueryParameter.searchArea.name());
        if (value != null) {
            if (predicateBuilder.length() > 0) {
                predicateBuilder.append(" AND ");
            }
            GeospatialQuery definedQuery = GeospatialQuery.fromString(value);

            StringBuilder errorBuilder = new StringBuilder();
            if (definedQuery != null) {
                predicateBuilder.append(definedQuery.getGeometryExpression());
                for (CotQueryParameter required : definedQuery.getRequiredParameters()) {
                    value = getParameterValue(httpParameters, required.name());
                    if (value == null) {
                        errorBuilder.append("Required parameter \"" + required.toString() + "\" is missing.\n");
                    } else {
                        try {
                            Double numericValue = Double.parseDouble(value);
                            queryParameters.add(numericValue);
                        } catch (NumberFormatException ex) {
                            errorBuilder.append("Required parameter \"" + required.toString() + "\" is unparseable.\n");
                        }
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognized value for " 
                        + CotQueryParameter.searchArea.toString());
            }

            if (errorBuilder.length() > 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errorBuilder.toString());
                return;
            }
        }

        // Whether to include images -- full image by default but this may change in a future release
        ImageOption imageOption = ImageOption.FULL; // Include the full image by default
        value = getParameterValue(httpParameters, CotQueryParameter.image.name());
        if (value != null) {
            try {
                imageOption = ImageOption.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warning("Unrecognized value for HTTP parameter \"" + CotQueryParameter.image + "\"."
                        + " Using default (" + imageOption.toString() + "\"");
            }
        }
        log.finer("ImageOption is " + imageOption.toString());

        try {

            // spawn a thread, and asynchronously execute CoT sql query. Return the query id.
            String queryId = cotSearchService.executeCotQueryAsync(hostName, portNumber, protocol, predicateBuilder.toString(), queryParameters, maximumResults, latestByUid, imageOption, replayMode, replaySpeed);

            // Set response content type
            response.setContentType("text/html");
            
            String cotStatusUrl = "CotSearchStatus.jsp?queryId=" + queryId;
            
            // send a response that will redirect the browser to the query status page
            String html = "<!doctype html>\r\n" + 
                    "<html>\r\n" + 
                    "<head>\r\n" + 
                    "<meta charset=\"utf-8\">\r\n" + 
                    "\r\n" + 
                    "<title>Query ID Result</title>\r\n" + 
                    "</head>\r\n" + 
                    "\r\n" + 
                    "<script type=\"text/javascript\">\r\n" + 
                    "  window.location.href = \"" + cotStatusUrl + "\"" +
                    "</script>\r\n" + 
                    "\r\n" + 
                    "<body></body>\r\n" + 
                    "\r\n" + 
                    "</html>";
            
            ServletOutputStream os = response.getOutputStream();

            os.print(html);
            os.flush();
            os.close();

        } catch (Exception e) {
            logger.error("exception writing servlet response", e);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void initalizeEsapiServlet() {
        log = Logger.getLogger(CotQueryServlet.class.getCanonicalName());
        requiredHttpParameters = new HashMap<String, HttpParameterConstraints>();
        requiredHttpParameters.put(CotQueryParameter.hostName.name(), new HttpParameterConstraints("MartiSafeString", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        requiredHttpParameters.put(CotQueryParameter.port.name(), new HttpParameterConstraints("NonNegativeInteger", MartiValidatorConstants.SHORT_STRING_CHARS));

        optionalHttpParameters = new HashMap<String, HttpParameterConstraints>();

        optionalHttpParameters.put(CotQueryParameter.cotType.name(), new HttpParameterConstraints("RestrictedRegex", MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.centerLatitude.name(), new HttpParameterConstraints("Double", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.centerLongitude.name(), new HttpParameterConstraints("Double", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.image.name(), new HttpParameterConstraints("MartiSafeString", MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.latestByUid.name(), new HttpParameterConstraints("MartiSafeString", MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.maximumResults.name(), new HttpParameterConstraints("NonNegativeInteger", MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.maximumStartTime.name(), new HttpParameterConstraints("Timestamp", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.minimumStartTime.name(), new HttpParameterConstraints("Timestamp", MartiValidatorConstants.DEFAULT_STRING_CHARS));

        optionalHttpParameters.put(CotQueryParameter.protocol.name(), new HttpParameterConstraints("MartiSafeString", MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.searchArea.name(), new HttpParameterConstraints("MartiSafeString", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.radius.name(), new HttpParameterConstraints("Double", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.rectangleBottom.name(), new HttpParameterConstraints("Double", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.rectangleLeft.name(), new HttpParameterConstraints("Double", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.rectangleRight.name(), new HttpParameterConstraints("Double", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.rectangleTop.name(), new HttpParameterConstraints("Double", MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.replayMode.name(), new HttpParameterConstraints("MartiSafeString", MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.replaySpeed.name(), new HttpParameterConstraints("Double", MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(CotQueryParameter.uid.name(), new HttpParameterConstraints("RestrictedRegex", MartiValidatorConstants.SHORT_STRING_CHARS));
    }
}
