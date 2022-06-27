package com.bbn.marti.test.shared.data.users;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.test.shared.data.DataUtil;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Created on 2/5/16.
 */
public class MutableUser extends AbstractUser {

	private String cotUidOverrideValue = null;
	private String cotCallsignOverrideValue = null;

	@Override
	public String getUserName() {
		return userName;
	}


	@Override
	public String getPassword() {
		return userPassword;
	}

	@Override
	public String getDynamicName() {
		String accessTag = DataUtil.generateUserAccessTag(getDefinedGroupSet(), getConnection());


		if (accessTag.equals(originalAccessTag) && cotUidOverrideValue == null && cotCallsignOverrideValue == null) {
			return getConsistentUniqueReadableIdentifier();

		} else {
			String differences = null;

			if (!accessTag.equals(originalAccessTag)) {
				String connectionGroupSetTag = getConnection().getGroupSet().getTag() + (getConnection().getRawAnonAccessFlag() == null ? "" : getConnection().getAnonAccess() ? "t" : "f");
				String userGroupSetTag = getDefinedGroupSet().getTag();
				differences = (userGroupSetTag.equals("") ? "" : ("+" + userGroupSetTag)) + connectionGroupSetTag + "=" + accessTag;
			}

			if (cotUidOverrideValue != null) {
				differences = (differences == null ? "" : differences + ",") + "uid=" + cotUidOverrideValue;
			}
			if (cotCallsignOverrideValue != null) {
				differences = (differences == null ? "" : differences + ",") + "callsign=" + cotCallsignOverrideValue;
			}

			return getConsistentUniqueReadableIdentifier() + "[" + differences + "]";
		}
	}

	@Override
	public AbstractServerProfile getServer() {
		return getConnection().getServer();
	}


	@Override
	public AbstractConnection getConnection() {
		return connectionInstance;
	}

	@Override
	public Path getCertPublicPemPath() {
		return certPublicPemPath;
	}

	@Override
	public Path getCertPrivateJksPath() {
		return certPrivateJksPath;
	}

	@Override
	public boolean doValidation() {
		return doValidation;
	}

	@Override
	public GroupSetProfiles getDefinedGroupSet() {
		return definedGroupSet;
	}

	public void invalidatePassword(@NotNull String newPassword) {
		futurePassword = newPassword;
	}

	public boolean isPasswordValid() {
		return userPassword.equals(futurePassword);
	}

	public void updatePassword() {
		userPassword = futurePassword;
	}

	public void removeFromGroup(@NotNull GroupProfiles group) {
		Set<GroupProfiles> newGroupSet = new HashSet<>();
		newGroupSet.addAll(definedGroupSet.getGroups());
		newGroupSet.remove(group);
		for (GroupSetProfiles profile : GroupSetProfiles.values()) {
			if (profile.getGroups().equals(newGroupSet)) {
				this.definedGroupSet = profile;
				return;
			}
		}
	}

	public void addToGroup(@NotNull GroupProfiles group) {
		Set<GroupProfiles> newGroupSet = new HashSet<>();
		newGroupSet.addAll(definedGroupSet.getGroups());
		newGroupSet.add(group);
		for (GroupSetProfiles profile : GroupSetProfiles.values()) {
			if (profile.getGroups().equals(newGroupSet)) {
				this.definedGroupSet = profile;
				return;
			}
		}
	}

	private final String consistentUniqueReadableUserIdentifier;
	private final AbstractConnection connectionInstance;
	private final String userName;
	private String userPassword;
	private String futurePassword;
	private GroupSetProfiles definedGroupSet;
	private final boolean doValidation;
	private final String originalAccessTag;
	private final Path certPrivateJksPath;
	private final Path certPublicPemPath;

	// TODO: Incorproate dynamic cert generation for mutable users?

	public MutableUser(@NotNull BaseUsers templateUser, @NotNull boolean doValidation, @NotNull AbstractConnection connectionInstance,
	                   @Nullable String differentiationTag, @Nullable Path certPrivateJksPath, @Nullable Path certPublicPemPath) {
		this.connectionInstance = connectionInstance;
		this.consistentUniqueReadableUserIdentifier = DataUtil.generateUserName(templateUser, doValidation, connectionInstance, differentiationTag);
		this.userName = templateUser.getUserName();
		this.userPassword = templateUser.getPassword();
		this.futurePassword = userPassword;
		this.definedGroupSet = templateUser.getBaseGroupSet();
		this.doValidation = doValidation;
		this.originalAccessTag = DataUtil.generateUserAccessTag(this.definedGroupSet, this.connectionInstance);
		this.certPrivateJksPath = certPrivateJksPath == null ? templateUser.getCertPrivateJksPath() : certPrivateJksPath;
		this.certPublicPemPath = certPublicPemPath == null ? templateUser.getCertPublicPemPath() : certPublicPemPath;
	}

	public MutableUser(@NotNull String userName, @NotNull String userPassword, @NotNull boolean doValidation, @NotNull AbstractConnection connectionInstance,
	                   @Nullable String differentiationTag, @Nullable Path certPrivateJksPath, @Nullable Path certPublicPemPath) {
		this.connectionInstance = connectionInstance;
		this.consistentUniqueReadableUserIdentifier = DataUtil.generateCustomUserName(userName, GroupSetProfiles.Set_None, doValidation, connectionInstance, differentiationTag);
		this.userName = userName;
		this.userPassword = userPassword;
		this.futurePassword = userPassword;
		this.definedGroupSet = GroupSetProfiles.Set_None;
		this.doValidation = doValidation;
		Path publicPath = TAKCLConfigModule.getInstance().getCertificateDir().resolve(userName + ".pem").toAbsolutePath();
		this.certPublicPemPath = certPublicPemPath != null ? certPublicPemPath : Files.exists(publicPath) ? publicPath : null;
		Path privatePath = TAKCLConfigModule.getInstance().getCertificateDir().resolve(userName + ".jks");
		this.certPrivateJksPath = certPrivateJksPath != null ? certPrivateJksPath : Files.exists(privatePath) ? privatePath : null;
		this.originalAccessTag = DataUtil.generateUserAccessTag(this.definedGroupSet, this.connectionInstance);
	}

	@Override
	public String getConsistentUniqueReadableIdentifier() {
		return consistentUniqueReadableUserIdentifier;
	}

	@Override
	public String getCotCallsign() {
		if (cotCallsignOverrideValue == null) {
			return super.getCotCallsign();
		} else {
			return cotCallsignOverrideValue;
		}
	}

	@Override
	public String getCotUid() {
		if (cotUidOverrideValue == null) {
			return super.getCotUid();
		} else {
			return cotUidOverrideValue;
		}
	}

	@Override
	public boolean isUserCredentialsValid() {
		return !getConnection().requiresAuthentication() ||
				(getUserName() != null && getPassword() != null && userPassword.equals(futurePassword));
	}

	public MutableUser overrideCotUid(@Nullable String cotUidOverrideValue) {
		this.cotUidOverrideValue = cotUidOverrideValue;
		return this;
	}

	public MutableUser overrideCotCallsign(@Nullable String cotCallsignOverrideValue) {
		this.cotCallsignOverrideValue = cotCallsignOverrideValue;
		return this;
	}
}
