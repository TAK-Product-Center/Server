package com.bbn.marti.test.shared;

/**
 * Created on 10/14/15.
 */
public enum TestConnectivityState {
	Disconnected,
	ConnectedAuthenticatedIfNecessary,
	ConnectedUnauthenticated,
	ConnectedCannotAuthenticate,
	SendOnly,
	ReceiveOnly
}
