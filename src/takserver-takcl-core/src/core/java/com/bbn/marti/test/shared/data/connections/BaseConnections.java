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
	stcp3(ProtocolProfiles.INPUT_STCP, 17720, AuthType.ANONYMOUS, null, GroupSetProfiles.Set_3);


	BaseConnections(@NotNull ProtocolProfiles protocol, @NotNull int port, @NotNull AuthType authType, @Nullable Boolean isAnon, @NotNull GroupSetProfiles groupSet, @NotNull String mcastGroup) {
		this.protocol = protocol;
		this.port = port;
		this.authType = authType;
		this.isAnon = isAnon;
		this.groupSet = groupSet;
		this.mcastGroup = mcastGroup;
	}

	BaseConnections(@NotNull ProtocolProfiles protocol, @NotNull int port, @NotNull AuthType authType, @Nullable Boolean isAnon, @NotNull GroupSetProfiles groupSet) {
		this.protocol = protocol;
		this.port = port;
		this.authType = authType;
		this.isAnon = isAnon;
		this.groupSet = groupSet;
		this.mcastGroup = null;
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
