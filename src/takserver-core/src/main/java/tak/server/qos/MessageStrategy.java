package tak.server.qos;

import java.util.Map;

public interface MessageStrategy<T> {
	
	boolean isAllowed(T message, String clientId);
	
	boolean isAllowed(String messageType, String messageId, String clientId);
		
	void enable();
	
	void disable();
	
	Map.Entry<Integer, Integer> getActiveRateThresholdAndLimit();

}
