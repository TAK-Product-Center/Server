package com.bbn.marti.test.shared.data.servers;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract representation of a server to be used in instances where it is a local server that could be
 * Created on 4/6/16.
 */
public abstract class AbstractServerProfile implements Comparable<Object>, Comparator<Object> {

	private static final HashMap<AbstractServerProfile,String> kubernetesIpMap = new HashMap<>();

	public static final String MISSION_PATH_FORMAT_STRING = "https://%s:%d/";
	private static AtomicInteger altPortRangeStart = new AtomicInteger(22000);

	private final String consistentUniqueReadableIdentifier;
	private String url;
	private final int federationPort;
	private final int federationV2Port;

	private Integer igniteDiscoveryPort;
	private Integer igniteDiscoveryPortCount;

	private Integer igniteCommunicationPort;
	private Integer igniteCommunicationPortCount;

	private final int certHttpsPort;
	private final int fedHttpsPort;
	private final int httpPlaintextPort;
	private final int httpsPort;
	private final String dbHost;

	public AbstractServerProfile(@NotNull String identifier, @NotNull String url, @Nullable Integer igniteDiscoveryPort, @Nullable Integer igniteDiscoveryPortCount,
								 Integer igniteCommunicationPort, Integer igniteCommunicationPortCount,
								 int federationPort, int federationV2Port, int certHttpsPort, int fedHttpsPort, int httpPlaintextPort, int httpsPort, @Nullable String dbHost) {
		this.consistentUniqueReadableIdentifier = identifier;
		this.url = url;
		this.igniteDiscoveryPort = igniteDiscoveryPort;
		this.igniteDiscoveryPortCount = igniteDiscoveryPortCount;
		this.igniteCommunicationPort = igniteCommunicationPort;
		this.igniteCommunicationPortCount = igniteCommunicationPortCount;
		this.federationPort = federationPort;
		this.federationV2Port = federationV2Port;
		this.certHttpsPort = certHttpsPort;
		this.fedHttpsPort = fedHttpsPort;
		this.httpPlaintextPort = httpPlaintextPort;
		this.httpsPort = httpsPort;
		this.dbHost = dbHost;
	}

	public void rerollIgnitePorts() {
		int startNum = altPortRangeStart.addAndGet(20);
		igniteCommunicationPort = startNum;
		igniteDiscoveryPort = startNum + 10;
	}

	public String getMissionBaseUrl() {
		return String.format(MISSION_PATH_FORMAT_STRING, getUrl(), httpsPort);
	}

	public abstract List<AbstractConnection> getInputs();

	public abstract List<AbstractConnection> getSubscriptions();

	public final String getServerPath() {
		return TAKCLConfigModule.getInstance().getTakServerPath(this.consistentUniqueReadableIdentifier);
	}

	public String getConfigFilePath() {
		return TAKCLConfigModule.getInstance().getServerConfigFilepath(this.consistentUniqueReadableIdentifier);
	}

	public String getTAKIgniteConfigFilePath() {
		return TAKCLConfigModule.getInstance().getServerTAKIgniteConfigFilepath(this.consistentUniqueReadableIdentifier);
	}

	public String getUserAuthFilePath() {
		return TAKCLConfigModule.getInstance().getServerUserFile(this.consistentUniqueReadableIdentifier);
	}

	public final String getJarFileName() {
		return TAKCLConfigModule.getInstance().getTakJarFilename();
	}

	/**
	 * This is meant to be a unique identifier to be used for tracking purposes.
	 * It has a few properties:
	 * <p>
	 * - Mutable instances of users shall inherit it
	 * - It shall be consistent through the life of the item, unless intentionally overridden to test edge cases
	 *
	 * @return the server identifier
	 */
	public final String getConsistentUniqueReadableIdentifier() {
		return consistentUniqueReadableIdentifier;
	}

	public final int getFederationV1ServerPort() {
		return federationPort;
	}

	public final int getFederationV2ServerPort() {
		return federationV2Port;

	}

	@Nullable
	public final Integer getIgniteDiscoveryPort() {
		return igniteDiscoveryPort;
	}

	@Nullable
	public final Integer getIgniteDiscoveryPortCount() {
		return igniteDiscoveryPortCount;
	}

	@Nullable
	public final Integer getIgniteCommunicationPort() {
		return igniteCommunicationPort;
	}

	@Nullable
	public final Integer getIgniteCommunicationPortCount() {
		return igniteCommunicationPortCount;
	}

	public final int getCertHttpsPort() {
		return certHttpsPort;
	}

	public final int getFedHttpsPort() {
		return fedHttpsPort;
	}

	public final int getHttpPlaintextPort() {
		return httpPlaintextPort;
	}

	public final int getHttpsPort() {
		return httpsPort;
	}

	public final void setUrl(String url) {
		this.url = url;
	}

	public final String getUrl() {
		return url;
	}

	@Nullable
	public final String getDbHost() {
		return dbHost;
	}

	@NotNull
	public final String getDbPassword() {
		String password = TAKCLCore.postgresPassword;
		if (password == null) {
			throw new RuntimeException("The postgres password must be set with an environment variable!");
		}
		return password;
	}
	@Override
	public final int compareTo(Object o) {

		if (o == null) {
			return 1;
		} else {
			if (o instanceof AbstractServerProfile) {
				return (this.getConsistentUniqueReadableIdentifier().compareTo(((AbstractServerProfile) o).getConsistentUniqueReadableIdentifier()));
			} else if (o instanceof String) {
				return (this.getConsistentUniqueReadableIdentifier().compareTo((String) o));
			} else {
				return -1;
			}
		}
	}

	@Override
	public final int compare(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return 0;
		} else if (o1 == null) {
			return -1;
		} else if (o2 == null) {
			return 1;
		} else if (o1 instanceof AbstractServerProfile) {
			return ((AbstractServerProfile) o1).compareTo(o2);
		} else if (o2 instanceof AbstractServerProfile) {
			return ((AbstractServerProfile) o2).compareTo(o1);
		} else {
			return -1;
		}
	}

	@Override
	public final boolean equals(Object obj) {
		return (compareTo(obj) == 0);
	}

	@Override
	public final String toString() {
		return getConsistentUniqueReadableIdentifier();
	}
}
