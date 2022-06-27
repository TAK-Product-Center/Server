package com.bbn.marti.test.shared.data.servers;

import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.BaseConnections;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 4/7/16.
 */
public class MutableServerProfile extends AbstractServerProfile {

	public static class Builder {

		private static AtomicInteger customInstanceCounter = new AtomicInteger(0);

		private final AbstractServerProfile baseServer;
		private String identifier;
		private String url = null;

		private Builder(AbstractServerProfile baseServer) {
			this.baseServer = baseServer;
			this.identifier = baseServer.getConsistentUniqueReadableIdentifier() + "x" + customInstanceCounter.getAndIncrement();
		}

		public static Builder build(AbstractServerProfile base) {
			return new Builder(base);
		}

		public Builder setUrl(@NotNull String url) {
			this.url = url;
			return this;
		}

		public Builder setIdentifier(@NotNull String identifier) {
			this.identifier = identifier;
			return this;
		}

		public MutableServerProfile create() {
			return new MutableServerProfile(
					this.baseServer,
					identifier,
					url == null ? baseServer.getUrl() : url,
					baseServer.getIgniteDiscoveryPort(),
					baseServer.getIgniteDiscoveryPortCount(),
					baseServer.getIgniteCommunicationPort(),
					baseServer.getIgniteCommunicationPortCount(),
					baseServer.getFederationV1ServerPort(),
					baseServer.getFederationV2ServerPort(),
					baseServer.getCertHttpsPort(),
					baseServer.getFedHttpsPort(),
					baseServer.getHttpPlaintextPort(),
					baseServer.getHttpsPort(),
					baseServer.getDbHost()
			);
		}
	}

	private Map<String, MutableConnection> mutableConnectionMap = new HashMap<>();
	public final  AbstractServerProfile baseProfile;

	public MutableServerProfile(@NotNull AbstractServerProfile baseProfile, @NotNull String identifier, @NotNull String url, @Nullable Integer igniteDiscoveryPort, @Nullable Integer igniteDiscoveryPortCount, @Nullable Integer igniteCommunicationPort,
            @Nullable Integer igniteCommunicationPortCount,int federationPort, int federationV2Port, int certHttpsPort, int fedHttpsPort, int httpPlaintextPort, int httpsPort, String dbHost) {
		super(identifier, url, igniteDiscoveryPort, igniteDiscoveryPortCount, igniteCommunicationPort, igniteCommunicationPortCount, federationPort, federationV2Port, certHttpsPort, fedHttpsPort, httpPlaintextPort, httpsPort, dbHost);
		this.baseProfile = baseProfile;
	}

	@Override
	public synchronized List<AbstractConnection> getInputs() {
		List<AbstractConnection> connectionsList = new LinkedList<>();

		for (MutableConnection connection : mutableConnectionMap.values()) {
			if (connection.getProtocol().getConnectionType() == ProtocolProfiles.ConnectionType.INPUT) {
				connectionsList.add(connection);
			}
		}
		return connectionsList;
	}


	@Override
	public synchronized List<AbstractConnection> getSubscriptions() {
		List<AbstractConnection> connectionsList = new LinkedList<>();

		for (MutableConnection connection : mutableConnectionMap.values()) {
			if (connection.getProtocol().getConnectionType() == ProtocolProfiles.ConnectionType.SUBSCRIPTION) {
				connectionsList.add(connection);
			}
		}
		return connectionsList;
	}

	public synchronized MutableConnection.Builder createConnectionBuilder(@NotNull BaseConnections connectionModel) {
		return MutableConnection.Builder.build(this, connectionModel);
	}

	public synchronized MutableConnection generateConnection(@NotNull String identifier, @NotNull BaseConnections connectionModel, int port) {
		if (mutableConnectionMap.containsKey(identifier)) {
			throw new RuntimeException("The mutableConnectionMap for MutableServerProfile '" + getConsistentUniqueReadableIdentifier() + "' already contains a connection with the identifier '" + identifier + "'!");
		}

		MutableConnection connection = new MutableConnection(identifier, connectionModel, port, this);
		mutableConnectionMap.put(identifier, connection);
		return connection;
	}

	public synchronized MutableConnection generateConnection(@NotNull ImmutableConnections connectionModel) {
		if (mutableConnectionMap.containsKey(connectionModel.getConsistentUniqueReadableIdentifier())) {
			throw new RuntimeException("The mutableConnectionMap for MutableServerProfile '" + getConsistentUniqueReadableIdentifier() + "' already contains a connection with the identifier '" + connectionModel + "'!");
		}

		MutableConnection connection = new MutableConnection(connectionModel, this);
		mutableConnectionMap.put(connectionModel.getConsistentUniqueReadableIdentifier(), connection);
		return connection;
	}

	public synchronized MutableConnection getGeneratedConnection(@NotNull String identifier) {
		return mutableConnectionMap.get(identifier);
	}
}
