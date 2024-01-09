

package com.bbn.marti.network;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.dao.kml.JDBCCachingKMLDao;
import com.bbn.marti.dao.kml.KMLDao;
import com.bbn.marti.remote.UIDResult;

import tak.server.Constants;

/**
 * 
 * REST endpoint for interfacing with uid searching
 *
 */
@RestController
public class UIDSearchApi extends BaseRestController {
    
    Logger logger = LoggerFactory.getLogger(UIDSearchApi.class);
   
    @Autowired
    private KMLDao dao;
    
	@RequestMapping(value = "/uidsearch", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<List<UIDResult>>> getUIDResults(HttpServletResponse response, 
    		@RequestParam(value="startDate") String startDate, @RequestParam(value="endDate") String endDate) {

		Date start = null;
		Date end = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
    	setCacheHeaders(response);
    	
    	List<String> errors = new ArrayList<String>();
    	
        ResponseEntity<ApiResponse<List<UIDResult>>> result = null;
        
        try {

        	if (startDate == null || endDate == null || startDate.trim().length() == 0 || endDate.trim().length() == 0) {
        		errors.add("You must provide a start and end date to query UID and Callsign mappings.");
        	} else {
        		if (startDate.replaceFirst("^\\d{4}-\\d{2}-\\d{2}$", "").length() > 0) {
        			errors.add("Start date contains invalid characters.");
        		}
        		if (endDate.replaceFirst("^\\d{4}-\\d{2}-\\d{2}$", "").length() > 0) {
        			errors.add("End date contains invalid characters.");
        		}
        	}
        	        	
        	if (errors.isEmpty()) {
            	start = sdf.parse(startDate);
            	end = sdf.parse(endDate);

        		List<UIDResult> data = dao.searchUIDs(start, end);
	            
	            result = new ResponseEntity<ApiResponse<List<UIDResult>>>(new ApiResponse<List<UIDResult>>(Constants.API_VERSION, UIDResult.class.getName(), data), HttpStatus.OK);
        	}
        } catch (Exception e) { 
        	errors.add("An unexpected error has occurred. Contact the system administrator.");
            logger.error("Exception getting UID search results.", e);  
        }

        if (result == null) {
        	//This would be an error condition (not an empty list)
            result = new ResponseEntity<ApiResponse<List<UIDResult>>>(new ApiResponse<List<UIDResult>>(Constants.API_VERSION, UIDResult.class.getName(), null, errors), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return result;
    }
}