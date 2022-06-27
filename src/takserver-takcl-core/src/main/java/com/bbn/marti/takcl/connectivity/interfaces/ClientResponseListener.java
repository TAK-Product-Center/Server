package com.bbn.marti.takcl.connectivity.interfaces;

import com.bbn.marti.test.shared.TestConnectivityState;

/**
 * Created on 11/13/15.
 */
public interface ClientResponseListener {
	void onMessageReceived(String response);

	void onMessageSent(String message);

	void onConnectivityStateChange(TestConnectivityState state);
}
