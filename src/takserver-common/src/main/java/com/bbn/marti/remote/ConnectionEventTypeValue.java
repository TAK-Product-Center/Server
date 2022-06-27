package com.bbn.marti.remote;

public enum ConnectionEventTypeValue {

    CONNECTED("Connected"),
    DISCONNECTED("Disconnected");

	private final String value;

	ConnectionEventTypeValue(String value) {
		this.value = value;
    }

    public String value() {
        return value;
    }

	public static ConnectionEventTypeValue fromValue(String v) {
    	
		ConnectionEventTypeValue result = null;
    	
        for (ConnectionEventTypeValue c: ConnectionEventTypeValue.values()) {
            if (c.value.equals(v)) {
                result = c;
            }
        }
        
        return result;
    }

}
