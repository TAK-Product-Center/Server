package com.bbn.marti.test.shared.data.servers;

import com.bbn.marti.config.Configuration;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created on 9/12/16.
 */
public class ImmutableServerProfiles extends AbstractServerProfile {

	public static final String DEFAULT_LOCAL_IP = "127.0.0.1";

	private final static HashMap<String, ImmutableServerProfiles> valueMap = new HashMap<>();

	public static final ImmutableServerProfiles UNDEFINED;
	public static final ImmutableServerProfiles SERVER_0;
	public static final ImmutableServerProfiles SERVER_1;
	public static final ImmutableServerProfiles SERVER_2;
	public static final ImmutableServerProfiles SERVER_CLI;
	public static final ImmutableServerProfiles DEFAULT_LOCAL_SERVER;

	static {
		int portCounter = 17651;
		UNDEFINED = new ImmutableServerProfiles("UNDEFINED", "", "127.0.0.1", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null); // Generic test source meant to be used for anything
//		SERVER_0 = new ImmutableServerProfiles("SERVER_0", "s0", "127.0.0.1", 17600, 9, 18600, 9, portCounter++, portCounter++, 8443, portCounter++, portCounter++, portCounter++, TAKCLCore.TakclOption.Server0DbHost.getStringOrNull()); // Used for generated tests
		SERVER_0 = new ImmutableServerProfiles("SERVER_0", "s0", "127.0.0.1", 47500, 9, 47100, 9, portCounter++, portCounter++, 8443, portCounter++, portCounter++, portCounter++, TAKCLCore.TakclOption.Server0DbHost.getStringOrNull()); // Used for generated tests
		SERVER_1 = new ImmutableServerProfiles("SERVER_1", "s1", "127.0.0.1", 17610, 9, 18610, 9, portCounter++, portCounter++, portCounter++, portCounter++, portCounter++, portCounter++, TAKCLCore.TakclOption.Server1DbHost.getStringOrNull()); // Used for generated tests
		SERVER_2 = new ImmutableServerProfiles("SERVER_2", "s2", "127.0.0.1", 17620, 9, 18620, 9, portCounter++, portCounter++, portCounter++, portCounter++, portCounter++, portCounter++, TAKCLCore.TakclOption.Server2DbHost.getStringOrNull()); // Used for generated tests
		SERVER_CLI = new ImmutableServerProfiles("SERVER_CLI", "sCLI", "127.0.0.1", 17640, 9, 17600, 9, portCounter++, portCounter++, portCounter++, portCounter++, portCounter++, portCounter++, null); // Used for CLI tools
		DEFAULT_LOCAL_SERVER = new ImmutableServerProfiles("DEFAULT_LOCAL_SERVER", "sD", "127.0.0.1", 47500, 100, 47100, 100, 9000, 9001, 8446, 8443, 8444, 8000, null) {
			public String getUserAuthFilePath() {
				Configuration config = Util.getCoreConfig();
				if (config == null) {
					return null;
				}

				String path = config.getAuth().getFile().getLocation();
				if (path.startsWith("/")) {
					return path;
				} else {
					return Util.getCoreConfigPath().getParent().resolve(path).toAbsolutePath().toString();
				}
			}

			public String getConfigFilePath() {
				return Util.getCoreConfigPath().toString();
			}
		};
	}

	private static CopyOnWriteArraySet<ImmutableServerProfiles> valueSet;
	private final String tag;

	public ImmutableServerProfiles(@NotNull String identifier, @NotNull String tag, @NotNull String url, @Nullable Integer igniteDiscoveryPort,
	                               @Nullable Integer igniteDiscoveryPortCount, @Nullable Integer igniteCommunicationPort,
	                               @Nullable Integer igniteCommunicationPortCount, int federationPort, int federationV2Port, int certHttpsPort, int fedHttpsPort, int httpPlaintextPort, int httpsPort, String dbHost) {
		super(identifier, url, igniteDiscoveryPort, igniteDiscoveryPortCount, igniteCommunicationPort, igniteCommunicationPortCount, federationPort, federationV2Port, certHttpsPort, fedHttpsPort, httpPlaintextPort, httpsPort, dbHost);
		this.tag = tag;
		valueMap.put(identifier, this);
	}

	@Override
	public List<AbstractConnection> getInputs() {
		List<AbstractConnection> connectionsList = new LinkedList<>();

		for (ImmutableConnections connection : ImmutableConnections.values()) {
			if (connection.getServer().getConsistentUniqueReadableIdentifier().equals(this.getConsistentUniqueReadableIdentifier()) && connection.getProtocol().getConnectionType() == ProtocolProfiles.ConnectionType.INPUT) {
				connectionsList.add(connection);
			}
		}
		return connectionsList;
	}

	@Override
	public List<AbstractConnection> getSubscriptions() {
		List<AbstractConnection> connectionsList = new LinkedList<>();

		for (ImmutableConnections connection : ImmutableConnections.values()) {
			if (connection.getServer().getConsistentUniqueReadableIdentifier().equals(this.getConsistentUniqueReadableIdentifier()) && connection.getProtocol().getConnectionType() == ProtocolProfiles.ConnectionType.SUBSCRIPTION) {
				connectionsList.add(connection);
			}
		}
		return connectionsList;
	}

	public MutableServerProfile getMutableInstance() {
		return new MutableServerProfile(
				this,
				getConsistentUniqueReadableIdentifier(),
				getUrl(),
				getIgniteDiscoveryPort(),
				getIgniteDiscoveryPortCount(),
				getIgniteCommunicationPort(),
				getIgniteCommunicationPortCount(),
				getFederationV1ServerPort(),
				getFederationV2ServerPort(),
				getCertHttpsPort(),
				getFedHttpsPort(),
				getHttpPlaintextPort(),
				getHttpsPort(),
				getDbHost());
	}

	public String getTag() {
		return tag;
	}

	private static synchronized void initStaticValuesIfNecessary() {
		if (!valueMap.isEmpty()) {
			if (valueSet == null) {
				valueSet = new CopyOnWriteArraySet<>(valueMap.values());
			}
		}
	}

	public static CopyOnWriteArraySet<ImmutableServerProfiles> valueSet() {
		initStaticValuesIfNecessary();
		return valueSet;
	}
}
