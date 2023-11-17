package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.AdvancedFileAuthModuleInterface;
import com.bbn.marti.takcl.AppModules.generic.ServerAppModuleInterface;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.xml.bindings.Role;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;

import tak.server.util.PasswordUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Used to manage the user authentication file in offline mode. Using it in online mode will likely have no effect, but could cause other unforseen issues.
 */
public class OfflineFileAuthModule implements ServerAppModuleInterface, AdvancedFileAuthModuleInterface {

	private String fileLocation;

	private boolean isInitialized = false;

	private UserAuthenticationFile authenticationFile;

	private final HashMap<String, UserAuthenticationFile.User> userFileMap = new HashMap<>();

	public OfflineFileAuthModule() {
	}

	@Override
	public synchronized void init(@NotNull AbstractServerProfile serverIdentifier) throws EndUserReadableException {
		// TODO: This is a little hacky...
		if (!isInitialized) {

			fileLocation = serverIdentifier.getUserAuthFilePath();
			File f = new File(fileLocation);
			if (f.exists()) {
				try {
					this.authenticationFile = Util.loadJAXifiedXML(fileLocation, UserAuthenticationFile.class.getPackage().getName());
				} catch (JAXBException e) {
					throw new EndUserReadableException("The existing user authentication file at '" +
							f.getAbsolutePath() + "' appears to be improperly formatted!", e);

				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			} else {
				this.authenticationFile = new UserAuthenticationFile();
				saveChanges();
			}


			for (UserAuthenticationFile.User user : authenticationFile.getUser()) {
				userFileMap.put(user.getIdentifier(), user);
			}

			isInitialized = true;
		}
	}

	@Override
	public void halt() { }

	private synchronized void saveChanges() {
		try {
			Util.saveJAXifiedObject(fileLocation, authenticationFile, true);
		} catch (IOException | JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getCommandDescription() {
		return "Modifies the User authorization file defined in the TAKCL configuration file.  The TAKCL " +
				"configuration file should be in the same directory as the TAKCL executable and named TAKCLConfigModule.xml." +
				" if file authroization is not enabled for the ServerInstance, this file will have no effect.";
	}

	public static boolean noValue(String value) {
		return (value == null || "".equals(value.trim()));
	}

	public String addUpdateUser(@NotNull final String userName, @Nullable final String userPassword,
	                            @Nullable final String userFingerprint, @Nullable final Set<String> groupSet) {

		UserAuthenticationFile.User user = null;
		boolean isNew = true;

		// If the user exists, get the existing data
		List<UserAuthenticationFile.User> userList = authenticationFile.getUser();
		for (UserAuthenticationFile.User loopUser : userList) {
			if (loopUser.getIdentifier().equals(userName)) {
				user = loopUser;
				isNew = false;
				break;
			}
		}

		if (user == null) {
			if (noValue(userFingerprint) && noValue(userPassword)) {
				throw new RuntimeException("Cannot add a new user without a password or fingerprint!");
			} else {
				user = new UserAuthenticationFile.User();
				user.setIdentifier(userName);
			}
		}

		// Set the password
		if (!noValue(userPassword)) {
			String hashedPassword = BCrypt.hashpw(userPassword, BCrypt.gensalt());
			user.setPassword(hashedPassword);
			user.setPasswordHashed(true);

		}

		// Set the fingerprint
		if (!noValue(userFingerprint)) {
			user.setFingerprint(userFingerprint);
		}

		if (groupSet != null) {
			user.getGroupList().clear();
			user.getGroupList().addAll(groupSet);
		}

		if (isNew) {
			authenticationFile.getUser().add(user);
		}

		saveChanges();
		if (isNew) {
			return "Added user '" + userName + "'\n";
		} else {
			return "Updated user '" + userName + "'\n";
		}
	}

	/**
	 * Adds the specified user to the specified group if the user exists
	 *
	 * @param userName  The user to add to the group
	 * @param groupName The group to add the user to
	 */
	public synchronized String addUserToGroup(String userName, String groupName) {
		List<UserAuthenticationFile.User> userList = authenticationFile.getUser();

		for (UserAuthenticationFile.User user : userList) {
			if (user.getIdentifier().equals(userName)) {

				List<String> groupList = user.getGroupList();

				if (!groupList.contains(groupName)) {
					groupList.add(groupName);
				}
				break;
			}
		}
		saveChanges();
		return "Added user '" + userName + "' to group '" + groupName + "'\n";
	}

	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[]{TakclRunMode.LOCAL_SERVER_INTERACTION};
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.STOPPED;
	}

	/**
	 * Removes the user authentication file, which is automatically generated as needed.
	 */
	public synchronized void resetConfig() {
		try {
			if ((new File(fileLocation)).exists()) {
				Files.copy(Paths.get(fileLocation), Paths.get(fileLocation + ".bak"), StandardCopyOption.REPLACE_EXISTING);
				Files.delete(Paths.get(fileLocation));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static UserAuthenticationFile readCurrentConfigFromDisk(@NotNull AbstractServerProfile serverIdentifier) {
		try {
			String fileLocation = serverIdentifier.getUserAuthFilePath();
			return Util.loadJAXifiedXML(fileLocation, UserAuthenticationFile.class.getPackage().getName());
		} catch (FileNotFoundException | JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	private synchronized String addOrUpdateCertificates(@Nullable String newGroupIdentifier, @NotNull String... certificateFilepath) {
		StringBuilder sb = new StringBuilder();
		try {
			for (String certificateLocation : certificateFilepath) {
				X509Certificate certificate = SSLHelper.getCertificate(certificateLocation);
				String userName = SSLHelper.getCertificateUserName(certificate);
				String fingerprint = SSLHelper.getCertificateFingerprint(certificate);

				sb.append(addUpdateUser(userName, null, fingerprint, null));
				if (newGroupIdentifier != null) {
					sb.append(addUserToGroup(userName, newGroupIdentifier));
				}
			}

			return sb.toString();

		} catch (IOException | CertificateException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String usermod(@NotNull String username, @Nullable Boolean delete, @Nullable String password, @Nullable String certpath,
            @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String[] group, @Nullable String[] inGroup, @Nullable String[] outGroup)
			throws EndUserReadableException {

		if (delete != null && delete) {
			return "Deletion not supported in offline mode!";

		} else {
			return innerUserCertMod(username, password, certpath, administrator, fingerprint, group, inGroup, outGroup);
		}
	}


	@Override
	public String certmod(@NotNull String certpath, @Nullable Boolean delete, @Nullable String password, @Nullable Boolean administrator,
	                      @Nullable String fingerprint, @Nullable String[] group, @Nullable String[] inGroup, @Nullable String[] outGroup) throws EndUserReadableException {
		if (delete != null && delete) {
			return "Deletion not supported in offline mode!";
		} else {
			return innerUserCertMod(null, password, certpath, administrator, fingerprint, group, inGroup, outGroup);
		}
	}

	private String innerUserCertMod(@Nullable String username, @Nullable String password, @Nullable String certpath,
	                                @Nullable Boolean administrator, @Nullable String fingerprint,
	                                @Nullable String[] group, @Nullable String[] inGroup, @Nullable String[] outGroup) throws EndUserReadableException {
		StringBuilder rval = new StringBuilder();

		if (password  != null && !PasswordUtils.isValidPassword(password)) {
			throw new EndUserReadableException(PasswordUtils.FAILED_COMPLEXITY_CHECK_ERROR_MESSAGE);
		}

		if (username == null && certpath == null) {
			throw new EndUserReadableException("A username or certificate path must be provided!");
		}

		if (certpath != null) {
			if (fingerprint == null) {
				fingerprint = SSLHelper.loadCertFingerprintForEndUser(certpath);
			}
			if (username == null) {
				username = SSLHelper.loadCertUsernameForEndUser(certpath);
			}
		}

		boolean newUser = false;

		List<UserAuthenticationFile.User> userList = getUsers(username);

		UserAuthenticationFile.User user;

		if (userList == null || userList.isEmpty()) {
			newUser = true;
			user = new UserAuthenticationFile.User();
			user.setIdentifier(username);

		} else {
			user = userList.get(0);
		}
        // password validated above
		if (password != null) {
			user.setPassword(password);
			user.setPasswordHashed(false);
		}

		if (administrator != null) {
			if (administrator) {
				user.setRole(Role.ROLE_ADMIN);
			} else {
				user.setRole(Role.ROLE_ANONYMOUS);
			}
		}

		if (fingerprint != null) {
			user.setFingerprint(fingerprint);
		}

		if (group != null && group.length > 0) {
			for (String grp : group) {
				if (!user.getGroupList().contains(grp)) {
					user.getGroupList().add(grp);
				}
			}
		}

		if (inGroup != null && inGroup.length > 0) {
			for (String grp : inGroup) {
				if (!user.getGroupListIN().contains(grp)) {
					user.getGroupListIN().add(grp);
				}
			}
		}

		if (outGroup != null && outGroup.length > 0) {
			for (String grp : outGroup) {
				if (!user.getGroupListOUT().contains(grp)) {
					user.getGroupListOUT().add(grp);
				}
			}
		}

		addOrUpdateUser(user);

		if (newUser) {
			rval.append("New User Added:\n").append(Util.getUserDisplayString(user));

		} else {
			rval.append("User Updated:\n").append(Util.getUserDisplayString(user));
		}

		if (user.getFingerprint() == null && user.getPassword() == null) {
			rval.append("\n\nNO FINGERPRINT, CERTIFICATE, OR PASSWORD WAS PROVIDED FOR THIS USER AND THEY WILL NOT BE USABLE!");
		}
		return rval.toString();
	}

	private void addUser(@NotNull String userIdentifier, @Nullable String fingerprint, @Nullable String password) {
		// TODO: Deauthorize user
		UserAuthenticationFile.User user = new UserAuthenticationFile.User();
		user.setIdentifier(userIdentifier);
		user.setFingerprint(fingerprint);
		updateUserPassword(user, password);
		userFileMap.put(userIdentifier, user);
		authenticationFile.getUser().add(user);
	}

	public synchronized String addOrUpdateUser(@NotNull String userIdentifier, @Nullable String userPassword) {
		UserAuthenticationFile.User user = userFileMap.get(userIdentifier);

		if (user == null) {
			addUser(userIdentifier, null, userPassword);
			return "Added User '" + userIdentifier + "'";

		} else {
			updateUserPassword(user, userPassword);
			return "Updated password for user '" + userIdentifier + "'";
		}
	}

	public List<UserAuthenticationFile.User> getUsers(String... userIdentifier) {
		LinkedList<UserAuthenticationFile.User> userList = new LinkedList<>();
		for (String uid : userIdentifier) {
			if (userFileMap.containsKey(uid)) {
				userList.add(userFileMap.get(uid));
			}
		}
		return userList;
	}

	private static void updateUserPassword(UserAuthenticationFile.User user, String newPassword) {
		if (newPassword == null) {
			user.setPassword(null);
			user.setPasswordHashed(false);
		} else {
			// TODO: Is this strong enough?
			String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
			user.setPassword(hashedPassword);
			user.setPasswordHashed(true);
		}
	}

	public synchronized String setUserFingerprint(@NotNull String userIdentifier, @NotNull String fingerprint) {
		UserAuthenticationFile.User user = userFileMap.get(userIdentifier);

		if (user == null) {
			addUser(userIdentifier, fingerprint, null);
			return "The user '" + userIdentifier + "' has been added.";
		} else {
			user.setFingerprint(fingerprint);
			return "The fingerprint for user '" + userIdentifier + "' has been updated.";
		}
	}

	public synchronized void removeUserFromGroup(@NotNull String userIdentifier, @NotNull String groupName) {
		UserAuthenticationFile.User user = userFileMap.get(userIdentifier);
		while (user.getGroupList().contains(groupName)) {
			user.getGroupList().remove(groupName);
		}
	}

	public void addOrUpdateUser(UserAuthenticationFile.User newUser) {
		try {
			if (newUser.getIdentifier() == null) {
// TODO: Do not throw RuntimeExceptions!
				throw new RuntimeException("User must have an identifier!");
			}

			String uid = newUser.getIdentifier();
			UserAuthenticationFile.User realUser;

			if (userFileMap.containsKey(newUser.getIdentifier())) {
				realUser = userFileMap.get(newUser.getIdentifier());

				String np = newUser.getPassword();
				String rp = realUser.getPassword();

				if (!((np == null && rp == null) || (np != null && np.equals(rp))) && (
						(np == null || rp == null || !BCrypt.checkpw(np, rp)))) {
					if (newUser.isPasswordHashed()) {
						throw new RuntimeException("Passwords do not match and the passwordHashed flag has not changed. " +
								"Password changes must be submitted as raw passwords with the passwordHashed change matching that state.");
					} else {
						updateUserPassword(realUser, newUser.getPassword());
					}
				}

				String nf = newUser.getFingerprint();
				String rf = realUser.getFingerprint();
				if (!((nf == null && rf == null) || (nf != null && nf.equals(rf)))) {
					setUserFingerprint(uid, newUser.getFingerprint());
				}

				if (newUser.getRole() != realUser.getRole()) {
					realUser.setRole(newUser.getRole());
				}

				// Determine which groups the user needs to be added to
				Set<String> addGroups = new HashSet<>(newUser.getGroupList());
				addGroups.removeAll(realUser.getGroupList());
				for (String group : addGroups) {
					addUserToGroup(uid, group);
				}

				// Determine which groups the user needs to be removed from
				Set<String> removeGroups = new HashSet<>(realUser.getGroupList());
				removeGroups.removeAll(newUser.getGroupList());
				for (String group : removeGroups) {
					removeUserFromGroup(uid, group);
				}

			} else {
				userFileMap.put(newUser.getIdentifier(), newUser);
				authenticationFile.getUser().add(newUser);
				addOrUpdateUser(uid, newUser.getPassword());

				if (newUser.getGroupList().size() > 0) {
					for (String group : newUser.getGroupList()) {
						addUserToGroup(uid, group);
					}
				} else {
					addUserToGroup(uid, "__ANON__");
				}
			}
			saveChanges();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


}
