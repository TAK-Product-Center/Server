package com.bbn.marti.takcl.connectivity.interfaces;

import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.jetbrains.annotations.Nullable;

/**
 * Created on 11/13/15.
 */
public interface ConnectingInterface extends ReceivingInterface, SendingInterface {

	TestConnectivityState connect(boolean authenticateIfNecessary, @Nullable String xmlData);

	TestConnectivityState authenticate();

	@Deprecated
	TestConnectivityState getConnectivityState();
	TestConnectivityState getActualConnectivityState();

	void disconnect();

	boolean isConnected();

	void cleanup();

	AbstractUser getProfile();
}
