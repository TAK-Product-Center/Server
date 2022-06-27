

package com.bbn.marti.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.CotImageBean;
import com.bbn.marti.config.AuthType;
import com.bbn.marti.config.Network.Input;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.MessagingConfigInfo;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;
import com.bbn.marti.remote.service.InputManager;
import com.bbn.security.web.MartiValidator;
import com.google.common.collect.ComparisonChain;
import tak.server.Constants;
import tak.server.ignite.MessagingIgniteBroker;

/**
 *
 * REST endpoint for interfacing with  submission service
 *
 */
@RestController
public class SubmissionApi extends BaseRestController {
    private static final String CONTEXT = "SubmissionApi";
    Logger logger = LoggerFactory.getLogger(SubmissionApi.class);

    @Autowired
    private MartiValidator validator;

    @Autowired
    ApplicationContext context;

    @Autowired
    private InputManager inputManager;

    @Autowired
	private CoreConfig coreConfig;

    @RequestMapping(value = "/inputs", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<SortedSet<InputMetric>>> getInputMetrics(HttpServletResponse response) {

    	setCacheHeaders(response);

        SortedSet<InputMetric> metrics = null;

        // sort metrics by input port number, then name
        try {
            metrics = new ConcurrentSkipListSet<InputMetric>(new Comparator<InputMetric>() {
                @Override
                public int compare(InputMetric thiz, InputMetric that) {
                    return ComparisonChain.start()
                            .compare(thiz.getInput().getPort(), that.getInput().getPort())
                            .compare(thiz.getInput().getName(), that.getInput().getName())
                            .result();
                }
            });

            Collection<InputMetric> inputs = inputManager.getInputMetrics();

            if (logger.isDebugEnabled()) {
            	logger.debug("inputs: " + inputs);
            }

            metrics.addAll(inputs);
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("Exception getting input metrics", e);
        	}
        }

        if (metrics != null) {
            return new ResponseEntity<ApiResponse<SortedSet<InputMetric>>>(new ApiResponse<SortedSet<InputMetric>>(Constants.API_VERSION, InputMetric.class.getName(), metrics), HttpStatus.OK);
        } else {
        	//This would be an error condition (not an empty input list)
            return new ResponseEntity<ApiResponse<SortedSet<InputMetric>>>(new ApiResponse<SortedSet<InputMetric>>(Constants.API_VERSION, InputMetric.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/inputs/{name}", method = RequestMethod.DELETE)
    public ResponseEntity<ApiResponse<Input>> deleteInput(@PathVariable("name") String name) {

        //Not clear what the payload for the ApiResponse should be here (Integer number of inputs deleted, the input itself). We don't have either of these returned by service
    	//layer for now, so just set payload to Input and set the value to null.
    	ResponseEntity<ApiResponse<Input>> result = null;


        try {
            if (!getInputNameValidationErrors(name).isEmpty()) {
                result = new ResponseEntity<ApiResponse<Input>>(new ApiResponse<Input>(Constants.API_VERSION,
                		Input.class.getName(), null), HttpStatus.BAD_REQUEST);
            } else {
	        	if (inputManager != null) {
	    			MessagingIgniteBroker.brokerVoidServiceCalls(service -> ((InputManager) service)
	    					.deleteInput(name), Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

		            //Again, we're returning ok even if nothing was actually deleted, which is not ideal, but we need to change the
		            //interface to return something besides void here
		            result = new ResponseEntity<ApiResponse<Input>>(new ApiResponse<Input>(Constants.API_VERSION,
		            		Input.class.getName(), null), HttpStatus.OK);
	            }
            }
        } catch (Exception e) {
            logger.error("Exception deleting input.", e);
        }

        if (result == null) {
        	//This would be an error condition (not an empty input list or a bad request)
        	result = new ResponseEntity<ApiResponse<Input>>(new ApiResponse<Input>(Constants.API_VERSION, Input.class.getName(), null),
        			HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    @RequestMapping(value = "/inputs/{id}", method = RequestMethod.PUT)
    public ResponseEntity<ApiResponse<ConnectionModifyResult>> modifyInput(@PathVariable("id") String id, @RequestBody Input input) {

    	ResponseEntity<ApiResponse<ConnectionModifyResult>> result = null;

    	List<String> errors = new ArrayList<>();

    	try {
    		ConnectionModifyResult updateResult = MessagingIgniteBroker.brokerServiceCalls(service -> ((InputManager) service)
    				.modifyInput(id, input), Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

    		result = new ResponseEntity<ApiResponse<ConnectionModifyResult>>(new ApiResponse<ConnectionModifyResult>(Constants.API_VERSION,
    				ConnectionModifyResult.class.getName(), updateResult), HttpStatus.valueOf(updateResult.getHttpStatusCode()));

    	} catch (Exception e) {
    		logger.error("Exception updating input.", e);
    		//In general, revealing raw exceptions to the user is not a good idea, but for now, as discussed,
    		//we'll leave this in until more granular exceptions are thrown by the service layer.
    		//The types of errors we'll see here concern missing cert files or lack of root access
    		//for lower port numbers the user has provided.
    		errors.add(e.getMessage());
    	}

    	if (result == null) {
    		//This would be an error condition (not an empty input list or bad request)
    		result = new ResponseEntity<ApiResponse<ConnectionModifyResult>>(new ApiResponse<ConnectionModifyResult>(Constants.API_VERSION,
    				ConnectionModifyResult.class.getName(), new ConnectionModifyResult("Error: No response received from server.", HttpStatus.INTERNAL_SERVER_ERROR.value())),
    				HttpStatus.INTERNAL_SERVER_ERROR);
    	}

    	return result;
    }

    @RequestMapping(value = "/inputs", method = RequestMethod.POST)
    public ResponseEntity<ApiResponse<Input>> createInput(@RequestBody Input input) {

        ResponseEntity<ApiResponse<Input>> result = null;

        List<String> errors = null;

        try {
        	errors = getValidationErrors(input);
            if (errors.isEmpty()) {

	            NetworkInputAddResult addResult = MessagingIgniteBroker.brokerServiceCalls(service -> ((InputManager) service)
	            		.createInput(input), Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

	            if (addResult == NetworkInputAddResult.SUCCESS) {
		            result = new ResponseEntity<ApiResponse<Input>>(new ApiResponse<Input>(Constants.API_VERSION,
		            		Input.class.getName(), input), HttpStatus.OK);
	            } else {
	            	logger.error("NetworkInputAddResult was " + addResult.getDisplayMessage());
	            	errors.add(addResult.getDisplayMessage());
	            }
            } else {
	            result = new ResponseEntity<ApiResponse<Input>>(new ApiResponse<Input>(Constants.API_VERSION,
	            		Input.class.getName(), input, errors), HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Exception adding input.", e);
            //In general, revealing raw exceptions to the user is not a good idea, but for now, as discussed,
            //we'll leave this in until more granular exceptions are thrown by the service layer.
            //The types of errors we'll see here concern missing cert files or lack of root access
            //for lower port numbers the user has provided.
            errors.add(e.getMessage());
        }

        if (result == null) {
        	//This would be an error condition (not an empty input list or bad request)
        	result = new ResponseEntity<ApiResponse<Input>>(new ApiResponse<Input>(Constants.API_VERSION, Input.class.getName(), null, errors),
        			HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    @RequestMapping(value = "/inputs/{name}", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<InputMetric>> getInputMetric(@PathVariable("name") String name, HttpServletResponse response) {

    	setCacheHeaders(response);

        //Really the way this function is used is to get an Input, not an InputMetric, but leave it as generic for CRUD purposes
        InputMetric metric = null;
        ResponseEntity<ApiResponse<InputMetric>> result = null;

        try {
            if (!getInputNameValidationErrors(name).isEmpty()) {
                result = new ResponseEntity<ApiResponse<InputMetric>>(new ApiResponse<InputMetric>(Constants.API_VERSION,
                		InputMetric.class.getName(), null), HttpStatus.BAD_REQUEST);
            } else {
            	Collection<InputMetric> metrics = inputManager.getInputMetrics();
            	if (metrics != null) {
            		for (InputMetric m : metrics) {
            			if (m.getInput().getName().equalsIgnoreCase(name)) {
            				metric = m;
            				break;
            			}
            		}
            	}
            	result = new ResponseEntity<ApiResponse<InputMetric>>(new ApiResponse<InputMetric>(Constants.API_VERSION,
	            		InputMetric.class.getName(), metric), HttpStatus.OK);
            }
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("Exception getting input metric", e);
        	}
        }

        if (result == null) {
        	//This would be an error condition (not an empty input list or bad request)
        	result = new ResponseEntity<ApiResponse<InputMetric>>(new ApiResponse<InputMetric>(Constants.API_VERSION,
            		InputMetric.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    @RequestMapping(value = "/inputs/config", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<MessagingConfigInfo>> getConfigInfo(){
    	try{
    		MessagingConfigInfo messagingConfigInfo = inputManager.getConfigInfo();
    		return new ResponseEntity<ApiResponse<MessagingConfigInfo>>(new ApiResponse<MessagingConfigInfo>(Constants.API_VERSION, MessagingConfigInfo.class.getName(),
					messagingConfigInfo), HttpStatus.OK);
		}
		catch(Exception e){
    		logger.info("MessagingConfigInfo: " + e.getMessage());
    		return new ResponseEntity<ApiResponse<MessagingConfigInfo>>(new ApiResponse<MessagingConfigInfo>(Constants.API_VERSION, MessagingConfigInfo.class.getName(),
			null), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/inputs/config", method = RequestMethod.PUT)
	public ResponseEntity<ApiResponse<String>> modifyConfigInfo(@RequestBody MessagingConfigInfo msgInfo){
    	try{
    	    //Validate username thats displayed to prevent XSS exploit
    		validator.getValidInput(CONTEXT, msgInfo.getDbUsername(), "MartiSafeString", MartiValidator.DEFAULT_STRING_CHARS, false);

    		MessagingIgniteBroker.brokerVoidServiceCalls(service -> ((InputManager) service)
					.modifyConfigInfo(msgInfo), Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

    		return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, MessagingConfigInfo.class.getName(),
					"Successfully modified messaging config"), HttpStatus.OK);
		}
		catch (Exception e){
    		return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, null, null), HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/database/cotCount", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<Map<String, Integer>>> getDatabaseCotCounts() {
	    Map<String, Integer> cotCounts = new HashMap<String, Integer>();
        try {
            CotImageBean cotBean = context.getBean(CotImageBean.class);
            cotCounts.put("cotEvents", cotBean.getCountOfCoT());
            cotCounts.put("cotImages", cotBean.getCountOfImages());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new ResponseEntity<ApiResponse<Map<String, Integer>>>(new ApiResponse<Map<String, Integer>>(Constants.API_VERSION, Map.class.getName(), cotCounts), HttpStatus.OK);
	}

	@RequestMapping(value = "/inputs/storeForwardChat/enabled", method = RequestMethod.GET)
	public boolean isStoreForwardChatEnabled() {
		return coreConfig.getRemoteConfiguration().getBuffer().getQueue().isEnableStoreForwardChat();
	}

	@RequestMapping(value = "/inputs/storeForwardChat/enable", method = RequestMethod.PUT)
	public void enableStoreForwardChat() {
		coreConfig.setAndSaveStoreForwardChatEnabled(true);
	}

	@RequestMapping(value = "/inputs/storeForwardChat/disable", method = RequestMethod.PUT)
	public void disableStoreForwardChat() {
		coreConfig.setAndSaveStoreForwardChatEnabled(false);
	}

    /**
     * Validates input name for both save and query REST endpoints.
     *
     * @param name
     * @return
     */
    private List<String> getInputNameValidationErrors(String name) {

    	List<String> errors = new ArrayList<String>();

    	//Validate input name
    	if (name == null || name.trim().length() == 0) {
    		errors.add("Missing input name.");
    	} else {
    		name = name.trim();

    		if (name.length() > INPUT_NAME_MAX_LENGTH) {
    			errors.add("Invalid input name length.");
    		} else {
    			if (name.replaceFirst("^[A-Za-z0-9_\\s]+$", "").length() > 0) {
    				errors.add("Invalid input name.");
    			}
    		}
    	}

    	return errors;
    }


    private List<String> getValidationErrors(Input input) {

    	List<String> errors = new ArrayList<String>();

    	if (input == null) {
    		//Should not occur
    		errors.add("Missing input definition.");
    	} else {
	    	//Validate auth type (required)
	    	if (input.getAuth() == null) {
	    		errors.add("Missing authentication type.");
	    	}

	    	//Validate input name
	    	errors.addAll(getInputNameValidationErrors(input.getName()));
	    	if (input.getName() != null) {
	    		input.setName(input.getName().trim());
	    	}

	    	//Validate protocol
	    	if (!PROTOCOLS.contains(input.getProtocol().toLowerCase())) {
	    		errors.add("Invalid protocol selection.");
	    	} else {
	    		input.setProtocol(input.getProtocol().toLowerCase());
	    	}

	    	//Validate port
	    	if (input.getPort() < PORT_RANGE_LOW || input.getPort() > PORT_RANGE_HIGH) {
	    		errors.add("Invalid port value.");
	    	}

	    	//Validate multicast group
	    	if (input.getGroup() != null && input.getGroup().trim().length() > 0) {
	    		input.setGroup(input.getGroup().trim());
	    		if (input.getGroup().replaceFirst("^([0-9]{1,3}\\.){3}[0-9]{1,3}$", "").length() > 0) {
	    			errors.add("Invalid multicast group value.");
	    		}
	    	}

	    	//Validate interface
	    	if (input.getIface() != null && input.getIface().trim().length() > 0) {
	    		input.setIface(input.getIface().trim());
	    		if (input.getIface().replaceFirst("^[A-Za-z0-9]+$", "").length() > 0) {
	    			errors.add("Invalid interface value.");
	    		}
	    	}

	    	//Enforce (Anonymous <=> (tcp v udp))

	    	//If protocol is tcp or udp, then auth type must be anonymous
	    	if ((input.getProtocol().equals("tcp") || input.getProtocol().equals("udp") || input.getProtocol().equals("mcast") || input.getProtocol().equals("cotmcast") ) && input.getAuth() != AuthType.ANONYMOUS) {
	    		errors.add("If Protocol is set to TCP, UDP or Multicast, then Authentication Type should be Anonymous.");
	    	}

	    	//Enforce (File v LDAP) <=> (stcp v tls v prototls v cottls))
	    	//If auth type is file or ldap, then protocol must be stcp or tls
	    	if ((input.getAuth() == AuthType.FILE || input.getAuth() == AuthType.LDAP) && !(input.getProtocol().equals("stcp") || input.getProtocol().equals("tls") || input.getProtocol().equals("prototls") || input.getProtocol().equals("cottls"))) {
	    		errors.add("If Authentication Type is set to File or LDAP, then Protocol should be be Streaming TCP or Secure Streaming TCP.");
	    	}

	    	//If protocol is mcast, then multicast group must not be empty
	    	if ((input.getProtocol().equals("mcast") || input.getProtocol().equals("cotmcast")) && (input.getGroup() == null || input.getGroup().trim().isEmpty())) {
	    		errors.add("If Protocol is set to Multicast, then the Multicast Group must be provided.");
	    	}

	    	//If multicast group must is not empty, then protocol must be mcast
	    	if (input.getGroup() != null && !input.getGroup().trim().isEmpty() && !input.getProtocol().equals("mcast") && !input.getProtocol().equals("cotmcast")) {
	    		errors.add("If the Multicast Group is provided, then Protocol should be set to Multicast.");
	    	}
    	}

    	return errors;
    }

    private static final int INPUT_NAME_MAX_LENGTH = 30;
    private static final int PORT_RANGE_LOW = 1;
    private static final int PORT_RANGE_HIGH = 65535;

    //As discussed with team, this will go into an enum in a future release; ok for now
    public static final List<String> PROTOCOLS = Collections.unmodifiableList(Arrays.asList("tcp", "udp", "stcp", "tls", "mcast", "cotmcast", "prototls", "cottls"));
}