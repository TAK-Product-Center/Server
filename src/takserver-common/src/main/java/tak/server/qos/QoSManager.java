package tak.server.qos;

import java.util.List;
import java.util.Map.Entry;

import com.bbn.marti.config.Qos;
import com.bbn.marti.config.RateLimitRule;


public interface QoSManager {
		
	void setReadLimiterEnabled(boolean enable);
	
	void setDeliveryLimiterEnabled(boolean enable);
	
	void setDOSLimiterEnabled(boolean enable);
	
	Entry<Integer, Integer> getActiveDeliveryRateThresholdAndLimit();
	
	Entry<Integer, Integer> getActiveReadRateThresholdAndLimit();
	
	Entry<Integer, Integer> getActiveDOSRateThresholdAndLimit();

	Qos qosConf();
}
