
package com.bbn.marti.network;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.remote.ClientEndpoint;
import com.bbn.marti.remote.exception.ForbiddenException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.util.CommonUtil;

import tak.server.Constants;

/**
 * 
 * REST endpoint for interfacing with contact information
 *
 */
@RestController
public class ContactManagerApi extends BaseRestController {
    
    Logger logger = LoggerFactory.getLogger(ContactManagerApi.class);
    
    @Autowired
    private ContactManagerService contactManagerService;

    @Autowired
	private CommonUtil martiUtil;

	@Autowired
	private RemoteUtil remoteUtil;

	@Autowired
	private GroupManager groupManager;


    @RequestMapping(value = "/clientEndPoints", method = RequestMethod.GET)
    public Callable<ResponseEntity<ApiResponse<List<ClientEndpoint>>>> getClientEndpoints(HttpServletRequest request, HttpServletResponse response,
    		@RequestParam(value="secAgo", required=false, defaultValue="0") long secAgo,
    		@RequestParam(value="showCurrentlyConnectedClients", required=false, defaultValue="false") String showCurrentlyConnectedClients,
    		@RequestParam(value="showMostRecentOnly", required=false, defaultValue="false") String showMostRecentOnly,
		    @RequestParam(value = "group", required = false) String[] queryGroupNames) {

    	if (logger.isDebugEnabled()) {
    		logger.debug("Received REST call for clientEndPoints with params: secAgo = " + secAgo + ", showCurrentlyConnectedClients = " + showCurrentlyConnectedClients);
    	}

    	final String groupVector = martiUtil.getGroupVectorBitString(request, Direction.OUT);

    	return () -> {

    		String useGroupVector = groupVector;

    		if (queryGroupNames != null) {
				BigInteger bitVectorUser = remoteUtil.bitVectorStringToInt(groupVector);

				Set<Group> queryGroups = groupManager.findGroups(Arrays.asList(queryGroupNames));
				String queryGroupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(queryGroups));
				BigInteger bitVectorQuery = remoteUtil.bitVectorStringToInt(queryGroupVector);

				if (bitVectorUser.compareTo(BigInteger.ZERO) == 0) {
					throw new ForbiddenException("Missing groups for user!");
				}

				if (bitVectorQuery.compareTo(BigInteger.ZERO) == 0) {
					throw new ForbiddenException("Missing groups for query!");
				}

				// ensure that the user has access to all groups contained in the query
				if (bitVectorUser.and(bitVectorQuery).compareTo(bitVectorQuery) != 0) {
					throw new ForbiddenException("Illegal attempt to set query groups!");
				}

				useGroupVector = queryGroupVector;
			}

    		setCacheHeaders(response);

    		List<String> errors = new ArrayList<String>();

    		ResponseEntity<ApiResponse<List<ClientEndpoint>>> result = null;
    		boolean connected = Boolean.valueOf(showCurrentlyConnectedClients);
    		boolean recent = Boolean.valueOf(showMostRecentOnly);

    		if (secAgo < 0) {
    			throw new IllegalArgumentException("invalid secAgo parameter " + secAgo);
    		}

    		try {

    			return new ResponseEntity<ApiResponse<List<ClientEndpoint>>>(new ApiResponse<List<ClientEndpoint>>(Constants.API_VERSION, ClientEndpoint.class.getName(), contactManagerService.getCachedClientEndpointData(connected, recent, useGroupVector, secAgo)), HttpStatus.OK);

    		} catch (Exception e) { 
    			errors.add("Exception getting client endpoint search results.");
    			logger.error("Exception getting client endpoint search results.", e);  
    		}


    		

    		return result;
    	};
    }
}
