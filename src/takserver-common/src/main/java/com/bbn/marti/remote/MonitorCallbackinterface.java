

package com.bbn.marti.remote;

import java.rmi.Remote;

public interface MonitorCallbackinterface extends Remote {
	void monitorCallback(String attrName, Object newValue); 
}
