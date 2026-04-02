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
    authwssadmin012("authwssadmin012", "authwssadminPassAb^2aX", GroupSetProfiles.Set_012, true),
	s0_anonmissionuser("s0_anonmissionuser", null, GroupSetProfiles.Set_Anon, false),
	s0_anonmissionuserA("s0_anonmissionuserA", null, GroupSetProfiles.Set_Anon, false),
	anonuser(null, null, GroupSetProfiles.Set_None, false),
	authuser("authuser", "userPassAb^2aX", GroupSetProfiles.Set_None, false),
	authusert("authusert", "userPassAb^2aX", GroupSetProfiles.Set_Anon, false),
	authuser01("authuser01", "user01PassAb^2aX", GroupSetProfiles.Set_01, false),
	authuser12("authuser12", "user12PassAb^2aX", GroupSetProfiles.Set_12, false),
	authuser3("authuser3", "user3PassAb^2aX", GroupSetProfiles.Set_3, false),
	authuser012("authuser012", "user012PassAb^2aX", GroupSetProfiles.Set_012, false),
	authuser0("authuser0", "user0PassAb^2aX", GroupSetProfiles.Set_0, false),
	authuser2("authuser2", "user2PassAb^2aX", GroupSetProfiles.Set_2, false),
	authwssuser("authwssuser", "userPassAb^2aX", GroupSetProfiles.Set_None, false),
	authwssuser01("authwssuser01", "user01PassAb^2aX", GroupSetProfiles.Set_01, false),
	authwssuser12("authwssuser12", "user12PassAb^2aX", GroupSetProfiles.Set_12, false),
	authwssuser3("authwssuser3", "user3PassAb^2aX", GroupSetProfiles.Set_3, false),
	authwssuser012("authwssuser012", "user012PassAb^2aX", GroupSetProfiles.Set_012, false),
	authwssuser0("authwssuser0", "user0PassAb^2aX", GroupSetProfiles.Set_0, false),
	authwssuser2("authwssuser2", "user2PassAb^2aX", GroupSetProfiles.Set_2, false),
	authwssusert("authwssusert", "usertpassAb^2aX", GroupSetProfiles.Set_Anon, false),
	authwssuserA("authwssuserA", "userPassAb^2aX", GroupSetProfiles.Set_None, false),
	authwssuser01A("authwssuser01A", "user01PassAb^2aX", GroupSetProfiles.Set_01, false),
	authwssuser12A("authwssuser12A", "user12PassAb^2aX", GroupSetProfiles.Set_12, false),
	authwssuser3A("authwssuser3A", "user3PassAb^2aX", GroupSetProfiles.Set_3, false),
	authwssuser012A("authwssuser012A", "user012PassAb^2aX", GroupSetProfiles.Set_012, false),
	authwssuser0A("authwssuser0A", "user0PassAb^2aX", GroupSetProfiles.Set_0, false),
	authwssuser2A("authwssuser2A", "user2PassAb^2aX", GroupSetProfiles.Set_2, false),
	authwssusertA("authwssusertA", "usertpassAb^2aX", GroupSetProfiles.Set_Anon, false),
	authwssusertB("authwssusertB", "usertpassAb^2aX", GroupSetProfiles.Set_Anon, false),
	authwssusertC("authwssusertC", "usertpassAb^2aX", GroupSetProfiles.Set_Anon, false);

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

     public boolean isAdmin() {
        return admin;
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

    private final boolean admin;

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public GroupSetProfiles getBaseGroupSet() {
		return groupSet;
	}

    BaseUsers(@Nullable String userName, @Nullable String password, @NotNull GroupSetProfiles groupSet, boolean admin) {
		this.userName = userName;
		this.password = password;
		this.groupSet = groupSet;
        this.admin = admin;

		hasCredentials = (userName != null && password != null);
	}

	public String nameModified(@Nullable String modifier) {
		return name() + (modifier == null ? "" : modifier);
	}

	public boolean hasAuthCredentials() {
		return (userName != null && password != null);
	}

}
