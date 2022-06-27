package com.bbn.marti.test.shared.data.protocols;

import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.test.shared.TestConnectivityState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Consumer;

/**
 * Created on 10/23/15.
 */
public enum ProtocolProfiles implements ProtocolProfilesInterface, Comparator<ProtocolProfilesInterface> {
	INPUT_TCP("tcp", 2),
	INPUT_UDP("udp", 1),
	INPUT_MCAST("mcast", 1),
	INPUT_STCP("stcp", 2),
	INPUT_TLS("tls", 2),
	INPUT_SSL("ssl", 2),
	SUBSCRIPTION_TCP("tcp", 2),
	SUBSCRIPTION_UDP("udp", 1),
	SUBSCRIPTION_MCAST("mcast", 1),
	SUBSCRIPTION_STCP("stcp", 2),
	SUBSCRIPTION_TLS("tls", 2),
	SUBSCRIPTION_SSL("ssl", 2);


	private final String value;
	private Integer coreNetworkVersion;

	@Override
	public String getValue() {
		return value;
	}

	@Nullable
	@Override
	public Integer getCoreNetworkVersion() {
		return coreNetworkVersion;
	}

	public static ProtocolProfiles getInputByValue(String value) {
		for (ProtocolProfiles protocol : ProtocolProfiles.values()) {
			if (protocol.getValue().equals(value) && protocol.getConnectionType() == ConnectionType.INPUT) {
				return protocol;
			}
		}
		throw new RuntimeException("Unexpected Protocol '" + value + "'!");
	}

	public static ProtocolProfiles getSubscriptionByValue(String value) {
		for (ProtocolProfiles protocol : ProtocolProfiles.values()) {
			if (protocol.getValue().equals(value) && protocol.getConnectionType() == ConnectionType.SUBSCRIPTION) {
				return protocol;
			}
		}
		throw new RuntimeException("Unexpected Protocol '" + value + "'!");
	}

	@Override
	public ConnectionType getConnectionType() {
		switch (this) {
			case INPUT_TCP:
			case INPUT_UDP:
			case INPUT_MCAST:
			case INPUT_STCP:
			case INPUT_TLS:
			case INPUT_SSL:
				return ConnectionType.INPUT;

			case SUBSCRIPTION_TCP:
			case SUBSCRIPTION_UDP:
			case SUBSCRIPTION_MCAST:
			case SUBSCRIPTION_STCP:
			case SUBSCRIPTION_TLS:
			case SUBSCRIPTION_SSL:
				return ConnectionType.SUBSCRIPTION;

			default:
				throw new RuntimeException("Unexpected protocol \'" + this + "'.");
		}
	}


	@Override
	public boolean canConnect() {
		switch (this) {
			case INPUT_TCP:
			case INPUT_UDP:
			case INPUT_MCAST:
			case SUBSCRIPTION_TCP:
			case SUBSCRIPTION_UDP:
			case SUBSCRIPTION_MCAST:
			case SUBSCRIPTION_STCP:
			case SUBSCRIPTION_SSL:
			case SUBSCRIPTION_TLS:
				return false;

			case INPUT_STCP:
			case INPUT_SSL:
			case INPUT_TLS:
				return true;

			default:
				throw new RuntimeException("Unexpected protocol \'" + this + "'.");
		}
	}

	@Override
	public boolean canListen() {
		switch (this) {
			case INPUT_TCP:
			case INPUT_UDP:
			case INPUT_MCAST:
				return false;

			case INPUT_STCP:
			case INPUT_SSL:
			case INPUT_TLS:
			case SUBSCRIPTION_TCP:
			case SUBSCRIPTION_UDP:
			case SUBSCRIPTION_MCAST:
			case SUBSCRIPTION_STCP:
			case SUBSCRIPTION_SSL:
			case SUBSCRIPTION_TLS:
				return true;

			default:
				throw new RuntimeException("Unexpected protocol \'" + this + "'.");
		}
	}

	@Override
	public boolean canSend() {
		switch (this) {
			case INPUT_TCP:
			case INPUT_UDP:
			case INPUT_STCP:
			case INPUT_TLS:
			case INPUT_SSL:
			case INPUT_MCAST:
				return true;

			case SUBSCRIPTION_TCP:
			case SUBSCRIPTION_UDP:
			case SUBSCRIPTION_MCAST:
			case SUBSCRIPTION_STCP:
			case SUBSCRIPTION_SSL:
			case SUBSCRIPTION_TLS:
				return false;

			default:
				throw new RuntimeException("Unexpected protocol \'" + this + "'.");
		}
	}

	@Override
	public TestConnectivityState getInitialState() {
		switch (this) {
			case INPUT_TCP:
			case INPUT_UDP:
			case INPUT_MCAST:
				return TestConnectivityState.SendOnly;

			case INPUT_STCP:
			case INPUT_SSL:
			case INPUT_TLS:
				return TestConnectivityState.Disconnected;

			case SUBSCRIPTION_TCP:
			case SUBSCRIPTION_UDP:
			case SUBSCRIPTION_MCAST:
			case SUBSCRIPTION_STCP:
			case SUBSCRIPTION_SSL:
			case SUBSCRIPTION_TLS:
				return TestConnectivityState.ReceiveOnly;

			default:
				throw new RuntimeException("Unexpected protocol \'" + this + "'.");
		}

	}

	@Override
	public boolean isTLS() {
		switch (this) {
			case INPUT_TCP:
			case INPUT_UDP:
			case INPUT_MCAST:
			case INPUT_STCP:
			case SUBSCRIPTION_TCP:
			case SUBSCRIPTION_UDP:
			case SUBSCRIPTION_MCAST:
			case SUBSCRIPTION_STCP:
				return false;

			case INPUT_SSL:
			case INPUT_TLS:
			case SUBSCRIPTION_SSL:
			case SUBSCRIPTION_TLS:
				return true;

			default:
				throw new RuntimeException("Unexpected protocol \'" + this + "'.");
		}
	}

	@Override
	public boolean clientConnectionSeveredWithInputRemoval() {
		switch (this) {

			case INPUT_STCP:
			case INPUT_TLS:
			case INPUT_SSL:
			case SUBSCRIPTION_STCP:
			case SUBSCRIPTION_TLS:
			case SUBSCRIPTION_SSL:
				return false;

			case INPUT_TCP:
			case INPUT_UDP:
			case INPUT_MCAST:
			case SUBSCRIPTION_TCP:
			case SUBSCRIPTION_UDP:
			case SUBSCRIPTION_MCAST:
				return true;

			default:
				throw new RuntimeException("Unexpected protocol \'" + this + "'.");
		}
	}

	@Override
	public String toString() {
		return name();
	}

	@Override
	public String getConsistentUniqueReadableIdentifier() {
		return name();
	}

	ProtocolProfiles(@NotNull String value, @Nullable Integer coreNetworkVersion) {
		this.value = value;
		this.coreNetworkVersion = coreNetworkVersion;
	}

	@Override
	public int compare(ProtocolProfilesInterface o1, ProtocolProfilesInterface o2) {
		return o1.toString().compareTo(o2.toString());
	}

	public static enum ConnectionType {
		INPUT,
		SUBSCRIPTION
	}
}
