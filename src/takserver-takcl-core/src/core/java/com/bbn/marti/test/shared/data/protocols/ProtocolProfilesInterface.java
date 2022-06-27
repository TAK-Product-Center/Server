package com.bbn.marti.test.shared.data.protocols;

import com.bbn.marti.test.shared.TestConnectivityState;
import org.jetbrains.annotations.Nullable;

/**
 * Created on 7/21/16.
 */
public interface ProtocolProfilesInterface {
	String getValue();

	ProtocolProfiles.ConnectionType getConnectionType();

	boolean canConnect();

	boolean canListen();

	boolean canSend();

	TestConnectivityState getInitialState();

	boolean isTLS();

	boolean clientConnectionSeveredWithInputRemoval();

	@Override
	String toString();

	String getConsistentUniqueReadableIdentifier();

	@Nullable
	Integer getCoreNetworkVersion();
}
