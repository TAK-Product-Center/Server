package com.bbn.marti.sync.api;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.GeospatialFilter;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.ValidationException;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.TimeUtils;
import com.bbn.marti.util.spring.RequestHolderBean;
import com.bbn.security.web.MartiValidator;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.cot.CotElement;


/*
 * 
 * REST API for querying CoT events
 * 
 */
@RestController
public class CotApi extends BaseRestController {
    
    private static final Logger logger = LoggerFactory.getLogger(CotApi.class);
	
    @Autowired
    MissionService missionService;
    
    @Autowired
    CommonUtil commonUtil;
    
    @Autowired
    RequestHolderBean requestHolder;

    @RequestMapping(value = "/cot/xml/{uid:.+}", method = RequestMethod.GET)
    Callable<ResponseEntity<String>> getCotEvent(@PathVariable("uid") @NotNull String uid) throws IntrusionException, org.owasp.esapi.errors.ValidationException {
    	
    	final String sessionId = requestHolder.sessionId();
		final String groupVector = commonUtil.getGroupVectorBitString(sessionId);
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("session id: " + requestHolder.sessionId());
    	}

    	return () -> {
    		//
    		// validate the uid parameter from URL
    		//
    		Validator validator = new MartiValidator();
    		try {
    			validator.getValidInput("cotquery", uid, 
    					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
    		} catch (ValidationException e) {
    			logger.error("ValidationException in getCotElement!", e);
    			return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
    		}

    		if (logger.isDebugEnabled()) {
    			logger.debug("get latest CoT for uid " + uid);
    		}

    		//
    		// query the database for the cot element
    		//
    		CotElement cot = missionService.getLatestCotForUid(uid, groupVector);
    		if (cot == null) {
    			return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
    		}

    		//
    		// ensure that the xml returned here matches what is used by StreamingProtobufProtocol.createFileTransferRequest
    		// fix hae precision to prevent double/string conversion issues, remove <marti> detail that gets stripped
    		// out during message processing prior to createFileTransferRequest being called.
    		//
    		cot.setHae(Double.parseDouble(cot.hae));
    		cot.detailtext = cot.detailtext.replaceAll("<marti>.+<\\/marti>", "");

    		HttpHeaders headers = new HttpHeaders();

    		headers.setContentType(org.springframework.http.MediaType.APPLICATION_XML);

    		//
    		// convert to xml and return
    		//
    		return new ResponseEntity<String>(Constants.XML_HEADER + cot.toCotXml(), headers, HttpStatus.OK);
    	};
    }

    @RequestMapping(value = "/cot/xml/{uid:.+}/all", method = RequestMethod.GET)
    Callable<ResponseEntity<String>> getAllCotEvents(
    		@PathVariable("uid") @NotNull String uid,
    		@RequestParam(value = "secago", required = false) Long secago,
    		@RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date rstart,
    		@RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date rend)
    				throws IntrusionException, org.owasp.esapi.errors.ValidationException {
    	
    	final String sessionId = requestHolder.sessionId();
		final String groupVector = commonUtil.getGroupVectorBitString(sessionId);
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("session id: " + requestHolder.sessionId());
    	}

    	return () -> {

    		//
    		// validate the uid parameter from URL
    		//
    		Validator validator = new MartiValidator();
    		try {
    			validator.getValidInput("cotquery", uid,
    					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
    		} catch (ValidationException e) {
    			logger.error("ValidationException in getAllCotEvents!", e);
    			return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
    		}

    		Map.Entry<Date, Date> timeInterval = TimeUtils.validateTimeInterval(secago, rstart, rend);

    		Date start = timeInterval.getKey();
    		Date end = timeInterval.getValue();

    		logger.debug("get all CoT for uid " + uid);

    		List<CotElement> cotElements;
    		try {
    			cotElements = missionService.getAllCotForUid(uid, start, end, groupVector);
    		} catch (NotFoundException nfe) {
    			cotElements = null;
    		}

    		if (cotElements == null || cotElements.size() == 0) {
    			return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
    		}

    		StringBuilder result = new StringBuilder();
    		result.append(Constants.XML_HEADER);
    		result.append("<events>");

    		// Get mission uids, then the latest CoT for each
    		for (CotElement cotElement : cotElements) {
    			result.append(cotElement.toCotXml());
    			result.append('\n');
    		}
    		result.append("</events>");

    		HttpHeaders headers = new HttpHeaders();
    		headers.setContentType(org.springframework.http.MediaType.APPLICATION_XML);
    		return new ResponseEntity<String>(result.toString(), headers, HttpStatus.OK);
    	};
    }

    // Get multiple CoT events given a list of UIDs
    @RequestMapping(value = "/cot", method = RequestMethod.GET)
    Callable<ResponseEntity<String>> getCotEvents(@RequestBody @NotNull Set<String> uids) {
    	
    	final String sessionId = requestHolder.sessionId();
		final String groupVector = commonUtil.getGroupVectorBitString(sessionId);
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("session id: " + requestHolder.sessionId());
    	}

    	return () -> {

    		if (uids == null || uids.isEmpty()) {
    			throw new IllegalArgumentException("one or more UIDs must be specified");
    		}

    		try {

    			Validator validator = new MartiValidator();

    			// Validate uid
    			for (String uid : uids) {

    				if (Strings.isNullOrEmpty(uid)) {
    					throw new IllegalArgumentException("one or more empty uids provided");
    				}

    				// let validation exceptions be handled by CustomExceptionHandler.java
    				validator.getValidInput("cotquery", uid, MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
    			}

    			StringBuilder result = new StringBuilder();
    			result.append(Constants.XML_HEADER);
    			result.append("<events>");

    			for (CotElement cotElement : missionService.getLatestCotForUids(uids, groupVector)) {
    				result.append(cotElement.toCotXml());
    				result.append('\n');
    			}
    			result.append("</events>");

    			HttpHeaders headers = new HttpHeaders();
    			headers.setContentType(org.springframework.http.MediaType.APPLICATION_XML);

    			return new ResponseEntity<String>(result.toString(), headers, HttpStatus.OK);

    		} catch (Exception e) {
    			logger.error("Exception in getCotElement!", e);
    			return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
    		}
    	};
    }

    @RequestMapping(value = "/cot/sa", method = RequestMethod.GET)
    Callable<ResponseEntity<String>> getCotEventsByTimeAndBbox(
    		@RequestParam(value = "start", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
    		@RequestParam(value = "end", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end,
    		@RequestParam(value = "left", required = false) Double left,
    		@RequestParam(value = "bottom", required = false) Double bottom,
    		@RequestParam(value = "right", required = false) Double right,
    		@RequestParam(value = "top", required = false) Double top) {
    	
    	final String sessionId = requestHolder.sessionId();
		final String groupVector = commonUtil.getGroupVectorBitString(sessionId);
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("session id: " + requestHolder.sessionId());
    	}

    	return () -> {

    		if (start.after(end) ||
    				TimeUnit.HOURS.convert(end.getTime() - start.getTime(), TimeUnit.MILLISECONDS) > 24) {
    			logger.error("attempt to query for CoT data outside of 24 hour window!");
    			throw new IllegalArgumentException();
    		}

    		GeospatialFilter.BoundingBox boundingBox = null;
    		if (left != null && bottom != null && right != null && top != null) {
    			boundingBox = new GeospatialFilter.BoundingBox();
    			boundingBox.setMinLongitude(left);
    			boundingBox.setMinLatitude(bottom);
    			boundingBox.setMaxLongitude(right);
    			boundingBox.setMaxLatitude(top);
    		}

    		List<CotElement> cotElements = missionService.getCotElementsByTimeAndBbox(
    				start, end, boundingBox, groupVector);

    		if (cotElements == null || cotElements.size() == 0) {
    			return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
    		}

    		StringBuilder result = new StringBuilder();
    		result.append(Constants.XML_HEADER);
    		result.append("<events>");

    		for (CotElement cotElement : cotElements) {
    			result.append(cotElement.toCotXml());
    			result.append('\n');
    		}
    		result.append("</events>");

    		HttpHeaders headers = new HttpHeaders();
    		headers.setContentType(org.springframework.http.MediaType.APPLICATION_XML);
    		return new ResponseEntity<String>(result.toString(), headers, HttpStatus.OK);
    	};
    }
}
