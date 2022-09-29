package com.bbn.marti.test.shared.data.users;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * These should never be used directly! Use users from GenValidatingUsers instead!
 * Created on 10/12/15.
 */
public enum BaseUsers {
	s0_anonmissionuser("s0_anonmissionuser", null, GroupSetProfiles.Set_Anon),
	s0_anonmissionuserA("s0_anonmissionuserA", null, GroupSetProfiles.Set_Anon),
	anonuser(null, null, GroupSetProfiles.Set_None),
	authuser("authuser", "userPassAb^2aX", GroupSetProfiles.Set_None),
	authusert("authusert", "userPassAb^2aX", GroupSetProfiles.Set_Anon),
	authuser01("authuser01", "user01PassAb^2aX", GroupSetProfiles.Set_01),
	authuser12("authuser12", "user12PassAb^2aX", GroupSetProfiles.Set_12),
	authuser3("authuser3", "user3PassAb^2aX", GroupSetProfiles.Set_3),
	authuser012("authuser012", "user012PassAb^2aX", GroupSetProfiles.Set_012),
	authuser0("authuser0", "user0PassAb^2aX", GroupSetProfiles.Set_0),
	authuser2("authuser2", "user2PassAb^2aX", GroupSetProfiles.Set_2),
	authwssuser("authwssuser", "userPassAb^2aX", GroupSetProfiles.Set_None),
	authwssuser01("authwssuser01", "user01PassAb^2aX", GroupSetProfiles.Set_01),
	authwssuser12("authwssuser12", "user12PassAb^2aX", GroupSetProfiles.Set_12),
	authwssuser3("authwssuser3", "user3PassAb^2aX", GroupSetProfiles.Set_3),
	authwssuser012("authwssuser012", "user012PassAb^2aX", GroupSetProfiles.Set_012),
	authwssuser0("authwssuser0", "user0PassAb^2aX", GroupSetProfiles.Set_0),
	authwssuser2("authwssuser2", "user2PassAb^2aX", GroupSetProfiles.Set_2),
	authwssusert("authwssusert", "usertpassAb^2aX", GroupSetProfiles.Set_Anon),
	authwssuserA("authwssuserA", "userPassAb^2aX", GroupSetProfiles.Set_None),
	authwssuser01A("authwssuser01A", "user01PassAb^2aX", GroupSetProfiles.Set_01),
	authwssuser12A("authwssuser12A", "user12PassAb^2aX", GroupSetProfiles.Set_12),
	authwssuser3A("authwssuser3A", "user3PassAb^2aX", GroupSetProfiles.Set_3),
	authwssuser012A("authwssuser012A", "user012PassAb^2aX", GroupSetProfiles.Set_012),
	authwssuser0A("authwssuser0A", "user0PassAb^2aX", GroupSetProfiles.Set_0),
	authwssuser2A("authwssuser2A", "user2PassAb^2aX", GroupSetProfiles.Set_2),
	authwssusertA("authwssusertA", "usertpassAb^2aX", GroupSetProfiles.Set_Anon),
	authwssusertB("authwssusertB", "usertpassAb^2aX", GroupSetProfiles.Set_Anon),
	authwssusertC("authwssusertC", "usertpassAb^2aX", GroupSetProfiles.Set_Anon);

	@NotNull
	public String displayString() {
		return "{ username : \"" + userName + "\"" +
				", password : \"" + password + "\"" +
				", groups : " + groupSet.displayString() +
				" }";
	}

	public static Set<BaseUsers> generateAuthenticatingUserSet() {
		Set<BaseUsers> returnSet = new HashSet<>();
		BaseUsers[] users = BaseUsers.values();

		for (BaseUsers user : users) {
			if (user.hasCredentials) {
				returnSet.add(user);
			}
		}
		return returnSet;
	}

	public static BaseUsers getByConnection(AbstractConnection connection) {
		if (connection.requiresAuthentication()) {
			return authuser;
		} else {
			return anonuser;
		}
	}

	@Nullable
	public Path getCertPublicPemPath() {
		Path certPath = TAKCLConfigModule.getInstance().getCertificateDir().resolve(userName + ".pem").toAbsolutePath();
		if (Files.exists(certPath)) {
			return certPath;
		}
		return null;
	}

	@Nullable
	public Path getCertPrivateJksPath() {
		Path certPath = TAKCLConfigModule.getInstance().getCertificateDir().resolve(userName + ".jks").toAbsolutePath();
		if (Files.exists(certPath)) {
			return certPath;
		}
		return null;
	}

	public static BaseUsers getAnonymousUser() {
		return BaseUsers.anonuser;
	}

	@Nullable
	private final String userName;

	@Nullable
	private final String password;

	@NotNull
	private final GroupSetProfiles groupSet;

	private final boolean hasCredentials;

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public GroupSetProfiles getBaseGroupSet() {
		return groupSet;
	}

	BaseUsers(@Nullable String userName, @Nullable String password, @NotNull GroupSetProfiles groupSet) {
		this.userName = userName;
		this.password = password;
		this.groupSet = groupSet;

		hasCredentials = (userName != null && password != null);
	}

	public String nameModified(@Nullable String modifier) {
		return name() + (modifier == null ? "" : modifier);
	}

	public boolean hasAuthCredentials() {
		return (userName != null && password != null);
	}

}
