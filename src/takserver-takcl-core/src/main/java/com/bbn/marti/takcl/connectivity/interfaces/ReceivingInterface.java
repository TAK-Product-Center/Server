package com.bbn.marti.takcl.connectivity.interfaces;

import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.jetbrains.annotations.NotNull;

/**
 * Created on 11/13/15.
 */
public interface ReceivingInterface {

	static String getGeneralPropertiesDisplayString(@NotNull AbstractConnection connection) {
		return (connection.requiresAuthentication() ? "auth-" : "") +
				connection.getProtocol().getValue() +
				(connection.getProtocol().getConnectionType() == ProtocolProfiles.ConnectionType.SUBSCRIPTION ? "-sub" : "");
	}

	enum OutputTarget {
		INHERIT_IO,
		DEV_NULL,
		FILE
	}

	void cleanup();

	void setAdditionalOutputTarget(OutputTarget target);

	AbstractUser getProfile();
}
