
package tak.server.qos;

import java.util.Collection;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.Qos;
import com.bbn.marti.config.RateLimitRule;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;

import tak.server.Constants;
import tak.server.ignite.MessagingIgniteBroker;

@RestController
public class QoSApi extends BaseRestController {

	    @Autowired
	    private QoSManager qosManager; 
	    
	    @RequestMapping(value = "/qos/delivery/enable", method = RequestMethod.PUT)
	    public void enableDelivery(@RequestBody boolean enable) {
	    	 MessagingIgniteBroker.brokerVoidServiceCalls(service -> ((QoSManager) service).setDeliveryLimiterEnabled(enable),
	    			 Constants.DISTRIBUTED_QOS_MANAGER, QoSManager.class);
	    }
	    
	    @RequestMapping(value = "/qos/read/enable", method = RequestMethod.PUT)
	    public void enableRead(@RequestBody boolean enable) {
	    	 MessagingIgniteBroker.brokerVoidServiceCalls(service -> ((QoSManager) service).setReadLimiterEnabled(enable),
	    			 Constants.DISTRIBUTED_QOS_MANAGER, QoSManager.class);
	    }
	    
	    @RequestMapping(value = "/qos/dos/enable", method = RequestMethod.PUT)
	    public void enableDOS(@RequestBody boolean enable) {
	    	 MessagingIgniteBroker.brokerVoidServiceCalls(service -> ((QoSManager) service).setDOSLimiterEnabled(enable),
	    			 Constants.DISTRIBUTED_QOS_MANAGER, QoSManager.class);
	    }
	    
	    @RequestMapping(value = "/qos/conf", method = RequestMethod.GET)
	    public ApiResponse<Qos> getQosConf() {
	    	return new ApiResponse<Qos>(Constants.API_VERSION, RateLimitRule.class.getSimpleName(), qosManager.qosConf());
	    }	
	    
	    @RequestMapping(value = "/qos/ratelimit/delivery/active", method = RequestMethod.GET)
	    public ApiResponse<Entry<Integer, Integer>> getActiveDeliveryRateLimit() {
	    	return new ApiResponse<Entry<Integer, Integer>>(Constants.API_VERSION, "RateLimit", qosManager.getActiveDeliveryRateThresholdAndLimit());
	    }	
	    
	    @RequestMapping(value = "/qos/ratelimit/read/active", method = RequestMethod.GET)
	    public ApiResponse<Entry<Integer, Integer>> getActiveReadRateLimit() {
	    	return new ApiResponse<Entry<Integer, Integer>>(Constants.API_VERSION, "RateLimit", qosManager.getActiveReadRateThresholdAndLimit());
	    }
	    
	    @RequestMapping(value = "/qos/ratelimit/dos/active", method = RequestMethod.GET)
	    public ApiResponse<Entry<Integer, Integer>> getActiveDOSRateLimit() {
	    	return new ApiResponse<Entry<Integer, Integer>>(Constants.API_VERSION, "RateLimit", qosManager.getActiveDOSRateThresholdAndLimit());
	    }
}
