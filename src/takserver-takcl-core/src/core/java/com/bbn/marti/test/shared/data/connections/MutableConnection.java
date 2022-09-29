package com.bbn.marti.test.shared.data.connections;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import com.bbn.marti.test.shared.data.users.MutableUser;
import com.bbn.marti.test.shared.data.users.UserFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 3/7/16.
 */
public class MutableConnection extends AbstractConnection {

	public static class Builder {

		private static AtomicInteger customInstanceCounter = new AtomicInteger(0);

		private final AbstractServerProfile parentServer;
		private final BaseConnections baseConnection;
		private String identifier;
		private Integer port = null;

		private Builder(AbstractServerProfile parentServer, BaseConnections baseConnection) {
			this.parentServer = parentServer;
			this.baseConnection = baseConnection;
			this.identifier = parentServer.getConsistentUniqueReadableIdentifier() + "x" + customInstanceCounter.getAndIncrement();
		}

		public static Builder build(AbstractServerProfile parentServer, BaseConnections base) {
			return new Builder(parentServer, base);
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public MutableConnection create() {
			return new MutableConnection(
					identifier,
					baseConnection,
					port == null ? baseConnection.getPort() : port,
					parentServer);
		}

	}

	private ConcurrentSkipListSet<AbstractUser> userSet = new ConcurrentSkipListSet<>();

	@Nullable
	private final Boolean rawAnonAccess;

	@NotNull
	private final ProtocolProfiles protocol;

	private final int port;

	@NotNull
	private final AuthType authType;

	@NotNull
	private GroupSetProfiles groupSetProfile;

	@Nullable
	private final String mcastGroup;

	@Nullable
	private final String type;

	@NotNull
	private final AbstractServerProfile serverProfile;

	@NotNull
	private final String consistentUniqueReadableIdentifier;

	@NotNull
	private final GroupSetProfiles originalGroupSetProfile;

	@Nullable
	private final Boolean originalRawAnonAccess;

	@Override
	public Boolean getRawAnonAccessFlag() {
		return rawAnonAccess;
	}

	@Override
	public synchronized Set<AbstractUser> getUsers(@Nullable UserFilter filter) {

		if (filter == null) {
			return userSet;
		} else {
			return filter.filterUsers(userSet);
		}
	}

	@Override
	public ProtocolProfiles getProtocol() {
		return protocol;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public AuthType getAuthType() {
		return authType;
	}

	@Override
	public GroupSetProfiles getGroupSet() {
		return groupSetProfile;
	}

	@Override
	public String getMCastGroup() {
		return mcastGroup;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getConsistentUniqueReadableIdentifier() {
		return consistentUniqueReadableIdentifier;
	}

	@Override
	public String getDynamicName() {
		if (getGroupSet() == originalGroupSetProfile &&
				(getRawAnonAccessFlag() == null && originalRawAnonAccess == null || getRawAnonAccessFlag() == originalRawAnonAccess)) {
			return getConsistentUniqueReadableIdentifier();
		} else {
			return getConsistentUniqueReadableIdentifier() + "=" + getGroupSet().getTag() + (getAnonAccess() ? "t" : "f");
		}
	}

	@Override
	public AbstractServerProfile getServer() {
		return serverProfile;
	}

	private final Map<String, MutableUser> userMap = new HashMap<>();

	public MutableConnection(@NotNull ImmutableConnections immutableConnection, @NotNull AbstractServerProfile serverProfile) {
		this.consistentUniqueReadableIdentifier = immutableConnection.getConsistentUniqueReadableIdentifier();
		this.serverProfile = serverProfile;
		this.protocol = immutableConnection.getProtocol();
		this.port = immutableConnection.getPort();
		this.authType = immutableConnection.getAuthType();
		this.originalGroupSetProfile = immutableConnection.getGroupSet();
		this.groupSetProfile = this.originalGroupSetProfile;
		this.originalRawAnonAccess = immutableConnection.getRawAnonAccessFlag();
		this.rawAnonAccess = this.originalRawAnonAccess;
		this.mcastGroup = immutableConnection.getMCastGroup();
		this.type = immutableConnection.getType();
	}

	public MutableConnection(@NotNull String identifier, @NotNull BaseConnections connectionModel, int port, @NotNull AbstractServerProfile serverProfile) {
		this.consistentUniqueReadableIdentifier = identifier;
		this.serverProfile = serverProfile;
		this.protocol = connectionModel.getProtocol();
		this.port = port;
		this.authType = connectionModel.getAuthType();
		this.originalGroupSetProfile = connectionModel.getGroupSet();
		this.groupSetProfile = this.originalGroupSetProfile;
		this.originalRawAnonAccess = connectionModel.getRawAnon();
		this.rawAnonAccess = this.originalRawAnonAccess;
		this.mcastGroup = connectionModel.getMcastGroup();
		this.type = connectionModel.getType();
	}

	public void removeFromGroup(@NotNull GroupProfiles group) {
		Set<GroupProfiles> newGroupSet = new HashSet<>();
		newGroupSet.addAll(groupSetProfile.getGroups());
		newGroupSet.remove(group);
		for (GroupSetProfiles profile : GroupSetProfiles.values()) {
			if (profile.getGroups().equals(newGroupSet)) {
				this.groupSetProfile = profile;
				return;
			}
		}
		throw new RuntimeException("Not all combinations of GroupProfiles are accounted for in GroupSetProfiles!");
	}

	public void addToGroup(@NotNull GroupProfiles group) {
		Set<GroupProfiles> newGroupSet = new HashSet<>();
		newGroupSet.addAll(groupSetProfile.getGroups());
		newGroupSet.add(group);
		for (GroupSetProfiles profile : GroupSetProfiles.values()) {
			if (profile.getGroups().equals(newGroupSet)) {
				this.groupSetProfile = profile;
				return;
			}
		}
		throw new RuntimeException("Not all combinations of GroupProfiles are accounted for in GroupSetProfiles!");
	}

	public synchronized MutableUser generateConnectionUser(@NotNull BaseUsers templateUser, @NotNull boolean doValidation,
	                                                       @NotNull Path certPrivateJksPath, @NotNull Path certPublicPemPath) {
		MutableUser user = new MutableUser(templateUser, doValidation, this, null, certPrivateJksPath, certPublicPemPath);
		if (userMap.containsKey(user.getConsistentUniqueReadableIdentifier())) {
			throw new RuntimeException("MutableConnection '" + getConsistentUniqueReadableIdentifier() + "' already contains a user with the identifier '" + user.getConsistentUniqueReadableIdentifier() + "'!");
		} else {
			userMap.put(user.getConsistentUniqueReadableIdentifier(), user);
			userSet.add(user);
		}

		return user;
	}

	public synchronized MutableUser generateConnectionUser(@NotNull BaseUsers templateUser, @NotNull boolean doValidation) {
		MutableUser user;
		if (templateUser.getCertPublicPemPath() != null) {
			user = new MutableUser(templateUser, doValidation, this, null,
					templateUser.getCertPrivateJksPath(), templateUser.getCertPublicPemPath());
		} else {
			user = new MutableUser(templateUser, doValidation, this, null, null, null);
		}

		if (userMap.containsKey(user.getConsistentUniqueReadableIdentifier())) {
			throw new RuntimeException("MutableConnection '" + getConsistentUniqueReadableIdentifier() + "' already contains a user with the identifier '" + user.getConsistentUniqueReadableIdentifier() + "'!");
		} else {
			userMap.put(user.getConsistentUniqueReadableIdentifier(), user);
			userSet.add(user);
		}

		return user;
	}

	public synchronized MutableUser generateConnectionUser(@NotNull BaseUsers templateUser, @NotNull boolean doValidation, @NotNull String differentiationTag) {
		MutableUser user = new MutableUser(templateUser, doValidation, this, differentiationTag, null, null);

		if (userMap.containsKey(user.getConsistentUniqueReadableIdentifier())) {
			throw new RuntimeException("MutableConnection '" + getConsistentUniqueReadableIdentifier() + "' already contains a user with the identifier '" + user.getConsistentUniqueReadableIdentifier() + "'!");
		} else {
			userMap.put(user.getConsistentUniqueReadableIdentifier(), user);
			userSet.add(user);
		}

		return user;
	}

	public synchronized MutableUser generateConnectionUser(@NotNull String userName, @NotNull String userPassword, @NotNull boolean doValidation, @NotNull String differentiationTag) {
		MutableUser user = new MutableUser(userName, userPassword, doValidation, this, differentiationTag, null, null);

		if (userMap.containsKey(user.getConsistentUniqueReadableIdentifier())) {
			throw new RuntimeException("MutableConnection '" + getConsistentUniqueReadableIdentifier() + "' already contains a user with the identifier '" + user.getConsistentUniqueReadableIdentifier() + "'!");
		} else {
			userMap.put(user.getConsistentUniqueReadableIdentifier(), user);
			userSet.add(user);
		}

		return user;
	}

	public synchronized MutableUser getGeneratedUser(@NotNull String userIdentifier) {
		return userMap.get(userIdentifier);
	}
}
