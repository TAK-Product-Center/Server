package com.bbn.marti.citrap;

import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.postgresql.geometric.PGbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.citrap.reports.ReportType;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.exception.MissionDeletedException;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.ValidationException;
import com.bbn.marti.sync.MissionPackageQueryServlet;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.KmlUtils;
import com.bbn.security.web.MartiValidator;

@RestController
public class CITrapReportAPI extends BaseRestController {

	@Autowired
    private Validator validator = new MartiValidator();
    private static final Logger logger = LoggerFactory.getLogger(CITrapReportAPI.class);

    @Autowired
    private MissionService missionService;

    @Autowired
    private SubscriptionManagerLite subscriptionManager;

    @Autowired
    private CommonUtil martiUtil;

    @Autowired
    private com.bbn.marti.sync.EnterpriseSyncService syncStore;
    
    @Autowired
    private CITrapReportService ciTrapReportService;
    
    @Autowired
    com.bbn.marti.citrap.PersistenceStore persistenceStore;
    
    @Autowired
    CoreConfig coreConfig;
    
    @Autowired
    ApplicationContext context;

    @RequestMapping(value = "/citrap", method = RequestMethod.POST)
    ResponseEntity<String> postReport(
            @RequestBody byte[] reportMP,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // validate inputs
            validator.getValidInput("citrap", clientUid,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);

            // Get group vector for the user associated with this session
            String groupVector = martiUtil.getGroupBitVector(request);
        
            if (reportMP.length > coreConfig.getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB() * 1000000) {
                String message = "Uploaded file exceeds server's size limit of "
                        + coreConfig.getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB()
                        + " MB! (limit is set in CoreConfig)";
                logger.error(message);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            ReportType report = ciTrapReportService.addReport(
                    reportMP, clientUid, groupVector, missionService, subscriptionManager,
                    martiUtil.getGroupsFromRequest(request),
                    coreConfig.getRemoteConfiguration().getCitrap());
            if (report == null) {
                logger.error("addReport failed");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String url = MissionPackageQueryServlet.getBaseUrl(request) +
                    "/Marti/api/citrap/" + report.getId();

            // return the report uid as json
            response.addHeader("Content-Type", "application/json");
            String json = "{\"id\":\"" + report.getId() + "\"}";

            return ResponseEntity.created(new URI(url)).body(json);

        } catch (Exception e) {
            logger.error("Exception in postReport!", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/citrap", method = RequestMethod.GET)
    ResponseEntity searchReports(
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "bbox", required = false) String bbox,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "maxReportCount", required = false) String maxReportCount,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "callsign", required = false) String callsign,
            @RequestParam(value = "subscribe", required = false) String subscribe,
            @RequestParam(value = "clientUid", required = false) String clientUid,
            HttpServletRequest request)
    {
        try {
            // validate inputs
            validator.getValidInput("citrap", keywords,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("citrap", bbox,
                    MartiValidator.Regex.Coordinates.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("citrap", startTime,
                    MartiValidator.Regex.Timestamp.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("citrap", endTime,
                    MartiValidator.Regex.Timestamp.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("citrap", maxReportCount,
                    MartiValidator.Regex.NonNegativeInteger.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("citrap", type,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
            validator.getValidInput("citrap", callsign,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("citrap", subscribe,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("citrap", clientUid,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);

            // Get group vector for the user associated with this session
            String groupVector = martiUtil.getGroupBitVector(request);

            PGbox spatialConstraint = null;
            Timestamp startTimestamp = null;
            Timestamp endTimestamp = null;

            if (bbox != null) {
                Double[] coordinates = KmlUtils.parseSpatialCoordinates(bbox);
                spatialConstraint = new PGbox(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat(tak.server.Constants.COT_DATE_FORMAT);

            if (startTime != null) {
                Date dateTime = dateFormat.parse(startTime);
                startTimestamp = new Timestamp(dateTime.getTime());
            }

            if (endTime != null) {
                Date dateTime = dateFormat.parse(endTime);
                endTimestamp = new Timestamp(dateTime.getTime());
            }

            List<ReportType> reports = persistenceStore.getReports(
                    groupVector, keywords, spatialConstraint, startTimestamp, endTimestamp, maxReportCount, type, callsign);

            if (subscribe != null && subscribe.equalsIgnoreCase("true") && clientUid != null) {
                for (ReportType reportType : reports) {
                    try {
                        missionService.missionSubscribe(reportType.getId(), clientUid, groupVector);
                    } catch (JpaSystemException e) { } // DuplicateKeyException comes through as JpaSystemException due to transaction
                    catch (NotFoundException e) {
                        logger.error("missionSubscribe couldn't find mission for report id : " + reportType.getId());
                    }
                }
            }

            String accept = request.getHeader("Accept");
            if (accept != null) {
                accept = accept.toLowerCase();
            }

            String response;
            if (accept == null ||
                    (accept.contains("*/*") || accept.contains("application/json") || accept.length() == 0)) {
                response = ciTrapReportService.serializeReportsAsJson(reports);
            } else if (accept.contains("application/xml")) {
                response = ciTrapReportService.serializeReportsAsXml(reports);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            logger.error("exception in searchReports!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/citrap/{id}", method = RequestMethod.GET)
    ResponseEntity getReport(
            @PathVariable("id") String id ,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // validate inputs
            validator.getValidInput("citrap", id,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
            validator.getValidInput("citrap", clientUid,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);

            // Get group vector for the user associated with this session
            String groupVector = martiUtil.getGroupBitVector(request);

            String accept = request.getHeader("Accept");
            if (accept != null) {
                accept = accept.toLowerCase();
            }

            byte[] results = null;
            String filename = id;

            if (accept == null ||
                    (accept.contains("*/*") || accept.contains("application/zip") || accept.length() == 0)) {
                
                byte[] mp = syncStore.getContentByUid(id, groupVector);
                if (mp == null) {
                    logger.error("Unable to find ci-trap report uid in enterprise sync!: " + id);
                    return new ResponseEntity(HttpStatus.NOT_FOUND);
                }

                filename += ".zip";
                results = mp;

            } else if (accept.contains("application/xml")) {
                String xml = persistenceStore.getReportAttrString(id, "xml", groupVector);
                if (xml == null) {
                    return new ResponseEntity(HttpStatus.NOT_FOUND);
                }

                filename += ".xml";
                results = xml.getBytes();
            }

            if (results == null) {
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }
            //Validating Filename
            validator.getValidInput("CITrap report filename", filename, "FileName", 255, false);

            try {
                missionService.missionSubscribe(id, clientUid, groupVector);
            } catch (JpaSystemException e) { } // DuplicateKeyException comes through as JpaSystemException due to transaction

            response.setContentLength(results.length);

            response.addHeader(
                    "Content-Disposition",
                    "attachment; filename=" + filename);

            response.getOutputStream().write(results);
            return new ResponseEntity(HttpStatus.OK);

        }
        catch (ValidationException | IntrusionException e){
            logger.error("Validation failed!", e);
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        catch (Exception e) {
            logger.error("exception in getReport!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/citrap/{id}", method = RequestMethod.DELETE)
    ResponseEntity deleteReport(
            @PathVariable("id") String id,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request) {

        try {
            // validate inputs
            validator.getValidInput("citrap", id,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
            validator.getValidInput("citrap", clientUid,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);

            // Get group vector for the user associated with this session
            String groupVector = martiUtil.getGroupBitVector(request);

            if (!ciTrapReportService.deleteReport(id, clientUid, groupVector, missionService)) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity(HttpStatus.OK);
        } catch (MissionDeletedException mde) {
            return new ResponseEntity(HttpStatus.GONE);
        } catch (NotFoundException nfe) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("exception in deleteReport!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/citrap/{id}", method = RequestMethod.PUT)
    ResponseEntity putReport(
            @PathVariable("id") String id,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            @RequestBody byte[] contents,
            HttpServletRequest request) {

        try {
            // validate inputs
            validator.getValidInput("citrap", id,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
            validator.getValidInput("citrap", clientUid,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);

            // Get group vector for the user associated with this session
            String groupVector = martiUtil.getGroupBitVector(request);

            String contentType = request.getHeader("content-type");
            if (contentType != null) {
                contentType = contentType.toLowerCase();
            }

            if (!ciTrapReportService.reportExists(id, groupVector)) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }

            if (contentType != null && contentType.equalsIgnoreCase("application/xml")) {

                // pull the mission package from enterprise sync so we can update it
                byte[] mp = syncStore.getContentByUid(id, groupVector);
                if (mp == null) {
                    logger.error("Unable to find ci-trap report uid in enterprise sync!: " + id);
                    return new ResponseEntity(HttpStatus.NOT_FOUND);
                }

                // swap out the xml file in the mission package
                contents = CITrapReportService.replaceReportInMp(contents, mp);
                if (contents == null) {
                    logger.error("Unable to replace ci-trap report in mission package");
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            // update the report
            if (ciTrapReportService.updateReport(contents, clientUid, groupVector, id, missionService) == null) {
                logger.error("updateReport failed in putReport!");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            logger.error("exception in putReport!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/citrap/{id}/attachment", method = RequestMethod.POST)
    ResponseEntity addAttachment(
            @PathVariable("id") String id,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            @RequestBody byte[] contents,
            HttpServletRequest request) {

        try {
            // validate inputs
            validator.getValidInput("citrap", id,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
            validator.getValidInput("citrap", clientUid,
                    MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);

            // Get group vector for the user associated with this session
            String groupVector = martiUtil.getGroupBitVector(request);

            String contentDisposition = request.getHeader("content-disposition");
            if (contentDisposition == null) {
                logger.error("missing content-disposition header!");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            String filename = CITrapReportService.getFilename(contentDisposition);
            if (filename == null) {
                logger.error("filename missing in content-disposition header!");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // pull the mission package from enterprise sync so we can update it
            byte[] mp = syncStore.getContentByUid(id, groupVector);
            if (mp == null) {
                logger.error("Unable to find ci-trap report uid in enterprise sync!: " + id);
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }

            // add the attachment to the mp
            byte[] updatedMp = CITrapReportService.addAttachmentToMp(filename, contents, mp);
            if (updatedMp == null) {
                logger.error("Unable to replace ci-trap report in mission package");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // update the report
            if (ciTrapReportService.updateReport(updatedMp, clientUid, groupVector, id, missionService) == null) {
                logger.error("updateReport failed in addAttachment!");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            logger.error("exception in addAttachment!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
