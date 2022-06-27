

package com.bbn.marti.remote;

public enum ConnectionStatusValue {

    DISABLED("Disabled"),
    CONNECTED("Connected"),
    CONNECTING("Connecting"),
    WAITING_TO_RETRY("Waiting To Retry"),
    RETRY_SCHEDULED("Retry Scheduled");

	private final String value;

	ConnectionStatusValue(String value) {
		this.value = value;
    }

    public String value() {
        return value;
    }

	public static ConnectionStatusValue fromValue(String v) {
    	
    	ConnectionStatusValue result = null;
    	
        for (ConnectionStatusValue c: ConnectionStatusValue.values()) {
            if (c.value.equals(v)) {
                result = c;
            }
        }
        
        return result;
    }
}
