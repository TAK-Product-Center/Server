package com.bbn.marti.test.shared.data.connections;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfilesInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on 10/12/15.
 */


public enum BaseConnections {
	tcp(ProtocolProfiles.INPUT_TCP, 8087, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None),
	udp(ProtocolProfiles.INPUT_UDP, 8087, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None),
	stcp(ProtocolProfiles.INPUT_STCP, 8088, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None),
	// TODO: Does mcast need proxy?
	mcast(ProtocolProfiles.INPUT_MCAST, 6970, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None, "239.2.3.1"),
	saproxy(ProtocolProfiles.INPUT_MCAST, 6969, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None, "239.2.3.1"),
	ssl(ProtocolProfiles.INPUT_SSL, 8089, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None),
	tls(ProtocolProfiles.INPUT_TLS, 8089, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None),
	authssl(ProtocolProfiles.INPUT_TLS, 8090, AuthType.FILE, null, GroupSetProfiles.Set_None),
	authtls(ProtocolProfiles.INPUT_TLS, 8090, AuthType.FILE, null, GroupSetProfiles.Set_None),
	stcp01(ProtocolProfiles.INPUT_STCP, 17700, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_01),
	stcp12(ProtocolProfiles.INPUT_STCP, 17701, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_12),
	stcp0(ProtocolProfiles.INPUT_STCP, 17702, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_0),
	authstcp(ProtocolProfiles.INPUT_STCP, 17703, AuthType.FILE, null, GroupSetProfiles.Set_None),
	stcp01t(ProtocolProfiles.INPUT_STCP, 17704, AuthType.ANONYMOUS, true, GroupSetProfiles.Set_01),
	stcp2f(ProtocolProfiles.INPUT_STCP, 17705, AuthType.ANONYMOUS, false, GroupSetProfiles.Set_2),
	tcp12(ProtocolProfiles.INPUT_TCP, 17707, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_12),
	tcp01t(ProtocolProfiles.INPUT_TCP, 17708, AuthType.ANONYMOUS, true, GroupSetProfiles.Set_01),
	tcp2f(ProtocolProfiles.INPUT_TCP, 17709, AuthType.ANONYMOUS, false, GroupSetProfiles.Set_2),
	udp01(ProtocolProfiles.INPUT_UDP, 17710, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_01),
	udp12t(ProtocolProfiles.INPUT_UDP, 17711, AuthType.ANONYMOUS, true, GroupSetProfiles.Set_12),
	udp3f(ProtocolProfiles.INPUT_UDP, 17712, AuthType.ANONYMOUS, false, GroupSetProfiles.Set_3),
	mcast01(ProtocolProfiles.INPUT_MCAST, 17713, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_01, "239.2.3.1"),
	mcast12t(ProtocolProfiles.INPUT_MCAST, 17714, AuthType.ANONYMOUS, true, GroupSetProfiles.Set_12, "239.2.3.1"),
	mcast3f(ProtocolProfiles.INPUT_MCAST, 17715, AuthType.ANONYMOUS, false, GroupSetProfiles.Set_3, "239.2.3.1"),
	subtcp(ProtocolProfiles.SUBSCRIPTION_TCP, 17716, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None),
	subudp(ProtocolProfiles.SUBSCRIPTION_UDP, 17717, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None),
	substcp(ProtocolProfiles.SUBSCRIPTION_STCP, 17718, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None),
	submcast(ProtocolProfiles.SUBSCRIPTION_MCAST, 17719, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_None, "239.2.3.1"),
	stcp3(ProtocolProfiles.INPUT_STCP, 17720, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_3),
	
	// Data feed connections
	stcp01_data(ProtocolProfiles.DATAFEED_STCP, 18742, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_01),
	stcp12_data(ProtocolProfiles.DATAFEED_STCP, 18743, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_12),
	stcp0_data(ProtocolProfiles.DATAFEED_STCP, 17723, AuthType.ANONYMOUS, null, "Streaming",  GroupSetProfiles.Set_0),
	authstcp_data(ProtocolProfiles.DATAFEED_STCP, 18746, AuthType.FILE, null, "Streaming", GroupSetProfiles.Set_None),
	stcp01t_data(ProtocolProfiles.DATAFEED_STCP, 17725, AuthType.ANONYMOUS, true, "Streaming", GroupSetProfiles.Set_01),
	stcp2f_data(ProtocolProfiles.DATAFEED_STCP, 17726, AuthType.ANONYMOUS, false, "Streaming", GroupSetProfiles.Set_2),
	tcp12_data(ProtocolProfiles.DATAFEED_TCP, 18749, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_12),
	tcp01t_data(ProtocolProfiles.DATAFEED_TCP, 18747, AuthType.ANONYMOUS, true, "Streaming", GroupSetProfiles.Set_01),
	tcp2f_data(ProtocolProfiles.DATAFEED_TCP, 18748, AuthType.ANONYMOUS, false, "Streaming", GroupSetProfiles.Set_2),
	udp01_data(ProtocolProfiles.DATAFEED_UDP, 18752, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_01),
	udp12t_data(ProtocolProfiles.DATAFEED_UDP, 18753, AuthType.ANONYMOUS, true, "Streaming", GroupSetProfiles.Set_12),
	udp3f_data(ProtocolProfiles.DATAFEED_UDP, 18754, AuthType.ANONYMOUS, false, "Streaming", GroupSetProfiles.Set_3),
	stcp_data(ProtocolProfiles.DATAFEED_STCP, 8088, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_None),
	stcp3_data(ProtocolProfiles.DATAFEED_STCP, 18720, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_3),
	mcast_data(ProtocolProfiles.DATAFEED_MCAST, 18734, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_None, "239.2.3.1"),
	saproxy_data(ProtocolProfiles.DATAFEED_MCAST, 6969, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_None, "239.2.3.1"),
	authssl_data(ProtocolProfiles.DATAFEED_TLS, 8090, AuthType.FILE, null, "Streaming", GroupSetProfiles.Set_None),
	mcast01_data(ProtocolProfiles.DATAFEED_MCAST, 18755, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_01, "239.2.3.1"),
	udp_data(ProtocolProfiles.DATAFEED_UDP, 8087, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_None),
	ssl_data(ProtocolProfiles.DATAFEED_SSL, 8089, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_None),
	mcast12t_data(ProtocolProfiles.DATAFEED_MCAST, 18756, AuthType.ANONYMOUS, true, "Streaming", GroupSetProfiles.Set_12, "239.2.3.1"),
	tls_data(ProtocolProfiles.DATAFEED_TLS, 8089, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_None),
	authtls_data(ProtocolProfiles.DATAFEED_TLS, 8090, AuthType.FILE, null, "Streaming", GroupSetProfiles.Set_None),
	mcast3f_data(ProtocolProfiles.DATAFEED_MCAST, 18715, AuthType.ANONYMOUS, false, "Streaming", GroupSetProfiles.Set_3, "239.2.3.1"),
	tcp_data(ProtocolProfiles.DATAFEED_TCP, 8087, AuthType.ANONYMOUS, null, "Streaming", GroupSetProfiles.Set_None);

	BaseConnections(@NotNull ProtocolProfiles protocol, @NotNull int port, @NotNull AuthType authType, @Nullable Boolean isAnon, @NotNull GroupSetProfiles groupSet, @NotNull String mcastGroup) {
		this.protocol = protocol;
		this.port = port;
		this.authType = authType;
		this.isAnon = isAnon;
		this.groupSet = groupSet;
		this.mcastGroup = mcastGroup;
		this.type = null;
	}

	BaseConnections(@NotNull ProtocolProfiles protocol, @NotNull int port, @NotNull AuthType authType, @Nullable Boolean isAnon, @NotNull GroupSetProfiles groupSet) {
		this.protocol = protocol;
		this.port = port;
		this.authType = authType;
		this.isAnon = isAnon;
		this.groupSet = groupSet;
		this.mcastGroup = null;
		this.type = null;
	}
	
	BaseConnections(@NotNull ProtocolProfiles protocol, @NotNull int port, @NotNull AuthType authType, @Nullable Boolean isAnon, @NotNull String type, @NotNull GroupSetProfiles groupSet, @NotNull String mcastGroup) {
		this.protocol = protocol;
		this.port = port;
		this.authType = authType;
		this.isAnon = isAnon;
		this.groupSet = groupSet;
		this.mcastGroup = mcastGroup;
		this.type = type;
	}
	
	BaseConnections(@NotNull ProtocolProfiles protocol, @NotNull int port, @NotNull AuthType authType, @Nullable Boolean isAnon, @NotNull String type, @NotNull GroupSetProfiles groupSet) {
		this.protocol = protocol;
		this.port = port;
		this.authType = authType;
		this.isAnon = isAnon;
		this.groupSet = groupSet;
		this.mcastGroup = null;
		this.type = type;
	}

	public static BaseConnections getByProtocolProfile(ProtocolProfilesInterface protocolProfile) {
		return BaseConnections.valueOf(protocolProfile.getValue());
	}

	@NotNull
	private final ProtocolProfiles protocol;

	@NotNull
	private final int port;

	@NotNull
	private final AuthType authType;

	@Nullable
	private final Boolean isAnon;

	@NotNull
	private final GroupSetProfiles groupSet;

	@Nullable
	private final String mcastGroup;
	
	@Nullable
	private final String type;

	@NotNull
	public ProtocolProfiles getProtocol() {
		return protocol;
	}

	@NotNull
	public int getPort() {
		return port;
	}

	@NotNull
	public AuthType getAuthType() {
		return authType;
	}

	@Nullable
	public Boolean getRawAnon() {
		return isAnon;
	}

	@NotNull
	public GroupSetProfiles getGroupSet() {
		return groupSet;
	}

	@Nullable
	public String getMcastGroup() {
		return mcastGroup;
	}
	
	@Nullable
	public String getType() {
		return type;
	}

	public boolean requiresAuthentication() {
		return AbstractConnection.requiresAuthentication(authType);
	}

	public boolean getAnonAccess() {
		return AbstractConnection.getAnonAccess(getAuthType(), getGroupSet(), getRawAnon());
	}

	public final String displayString() {
		return "{ getConsistentUniqueReadableIdentifier : \"" + name() + "\"" +
				", type: \"" + getProtocol().getConnectionType().name() + "\"" +
				", protocol : \"" + getProtocol().getValue() + "\"" +
				", port : \"" + getPort() + "\"" +
				", auth : \"" + getAuthType().value() + "\"" +
				", anon : " + (isAnon == null ? "undefined" : ("\"" + isAnon.toString() + "\"")) +
				", filtergroups : " + getGroupSet().displayString() +
				", group : " + (mcastGroup == null ? "undefined" : ("\"" + mcastGroup + "\"")) +
				" }";
	}
}
