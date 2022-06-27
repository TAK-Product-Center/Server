

package com.bbn.marti.repeater;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.Repeatable;
import com.bbn.marti.remote.RepeaterManager;

@RestController
@RequestMapping("/Marti/api/repeater")
/**
 * API for accessing information about the repeating service.
 * 
 * Can access via: https://marti:8443/Marti/api/repeater/<OP>, e.g., https://marti:8443/Marti/api/repeater/list
 */
public class RepeaterApi extends BaseRestController {
	private static final String REPEATER_API_VERSION = "1.0.0";

	private static final Logger logger = LoggerFactory.getLogger(RepeaterApi.class);
	
	@Autowired
	private RepeaterManager repeaterManager;
	
	@RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<List<Repeatable>>> getList() {
		List<Repeatable> repeatables = new ArrayList<Repeatable>();
		
		HttpStatus responseCode = HttpStatus.OK;
		try {			
			repeatables = new ArrayList<Repeatable>(repeaterManager.getRepeatableMessages());
		} catch (Exception e) {
			logger.error("Exception occurred trying to retrieve list of active repeating elements", e);
			responseCode = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		
		ApiResponse<List<Repeatable>> response = new ApiResponse<List<Repeatable>>(REPEATER_API_VERSION, String.class.getName(), repeatables);
		return new ResponseEntity<ApiResponse<List<Repeatable>>>(response, responseCode);
		
	}
	
	@RequestMapping(value = "/period", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<Integer>> getPeriod() {
		HttpStatus responseCode = HttpStatus.OK;
		Integer periodMillis;
		try {
			periodMillis = repeaterManager.getPeriodMillis();
		} catch (Exception e) {
			logger.error("Exception occurred while trying to access periodic timing information", e);
			periodMillis = -1;
			responseCode = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		ApiResponse<Integer> response = new ApiResponse<Integer>(REPEATER_API_VERSION, Integer.class.getName(), periodMillis);
		return new ResponseEntity<ApiResponse<Integer>>(response, responseCode);
	}
	
	@RequestMapping(value = "/period", method = RequestMethod.POST)
	public ResponseEntity<Void> setPeriod(@RequestBody Integer period) {
		try {
			repeaterManager.setPeriodMillis(period);
			return new ResponseEntity<Void>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Exception occurred while trying to set periodic timing information", e);
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/remove/{uid:.+}", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<Boolean>> remove(@PathVariable("uid") String uid) {
		HttpStatus responseCode = HttpStatus.OK;
		boolean messageRemoved = false;
		try {
			messageRemoved = repeaterManager.removeMessage(uid, true);
		} catch (Exception e) {
			messageRemoved = false;
			logger.error("Exception occurred trying to remove a repeating message", e);
			responseCode = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		
		ApiResponse<Boolean> response = new ApiResponse<Boolean>(REPEATER_API_VERSION, String.class.getName(), messageRemoved);
		return new ResponseEntity<ApiResponse<Boolean>>(response, responseCode);
		
	}
}
