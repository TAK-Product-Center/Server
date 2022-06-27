package com.bbn.marti.test.shared.data.connections;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfilesInterface;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created on 3/15/16.
 */
public class ConnectionFilter {
	private final Set<ImmutableServerProfiles> serverProfiles = new HashSet<>();
	private final Set<ProtocolProfilesInterface> protocolProfiles = new HashSet<>();
	private final Set<AuthType> authTypes = new HashSet<>();
	private final Set<ProtocolProfiles.ConnectionType> connectionTypes = new HashSet<>();
	private Boolean hasAnonAccess = null;

	public final ConnectionFilter addServerProfile(@NotNull ImmutableServerProfiles server) {
		serverProfiles.add(server);
		return this;
	}

	public final ConnectionFilter addProtocolProfiles(@NotNull ProtocolProfilesInterface... protocols) {
		for (ProtocolProfilesInterface protocolProfile : protocols) {
			this.protocolProfiles.add(protocolProfile);
		}
		return this;
	}

	public final ConnectionFilter addAuthType(@NotNull AuthType authType) {
		authTypes.add(authType);
		return this;
	}

	public final ConnectionFilter addConnectionTypes(@NotNull ProtocolProfiles.ConnectionType... connectionTypes) {
		for (ProtocolProfiles.ConnectionType connectionType : connectionTypes) {
			this.connectionTypes.add(connectionType);
		}
		return this;
	}

	public final ConnectionFilter addAnonAccess(@Nullable Boolean hasAnonAccess) {
		this.hasAnonAccess = hasAnonAccess;
		return this;
	}


	public final CopyOnWriteArraySet<AbstractConnection> filterConnections(CopyOnWriteArraySet<AbstractConnection> currentSet) {
		Set<AbstractConnection> returnSet = new HashSet<>();

		for (AbstractConnection connection : currentSet) {
			if (doesConnectionMatch(connection)) {
				returnSet.add(connection);
			}
		}
		return new CopyOnWriteArraySet<>(returnSet);
	}

	public final AbstractConnection filterOneConnection(CopyOnWriteArraySet<AbstractConnection> currentSet) {
		for (AbstractConnection connection : currentSet) {
			if (doesConnectionMatch(connection)) {
				return connection;
			}
		}
		return null;
	}

	public final boolean doesConnectionMatch(AbstractConnection connection) {
		if (!serverProfiles.isEmpty() && !serverProfiles.contains(connection.getServer())) {
			return false;
		} else if (!protocolProfiles.isEmpty() && !protocolProfiles.contains(connection.getProtocol())) {
			return false;
		} else if (!authTypes.isEmpty() && !authTypes.contains(connection.getAuthType())) {
			return false;
		} else if (!connectionTypes.isEmpty() && !connectionTypes.contains(connection.getConnectionType())) {
			return false;
		} else if (hasAnonAccess != null && (hasAnonAccess != connection.getAnonAccess())) {
			return false;
		} else {
			return true;
		}
	}
}
