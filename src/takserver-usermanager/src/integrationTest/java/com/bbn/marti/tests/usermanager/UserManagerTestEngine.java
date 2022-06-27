package com.bbn.marti.tests.usermanager;

import com.bbn.marti.takcl.AppModules.OfflineFileAuthModule;
import com.bbn.marti.takcl.AppModules.OnlineFileAuthModule;
import com.bbn.marti.takcl.AppModules.generic.AdvancedFileAuthModuleInterface;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TAKCLogging;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.engines.state.EnvironmentState;
import com.bbn.marti.test.shared.engines.state.StateEngine;
import com.bbn.marti.xml.bindings.Role;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import com.bbn.marti.xml.bindings.UserAuthenticationFile.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.slf4j.Logger;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created on 9/25/17.
 */
public class UserManagerTestEngine {

	private final EnvironmentState environmentState;
	private AdvancedFileAuthModuleInterface ofam;

	private static final Logger logger = TAKCLogging.getLogger(UserManagerTestEngine.class);

	private static final String ANON_GROUP = "__ANONYMOUS__";

	private final boolean useOfflineModule;

	private static boolean eq(Object o0, Object o1) {
		if (o0 == null) {
			return o1 == null;
		} else {
			return o1 != null && o0.equals(o1);
		}
	}

	private boolean checkPassword(String passwordA, String passwordB) {
		return eq(passwordA, passwordB) ||
				(passwordB != null && passwordB.startsWith("$2a$10") && BCrypt.checkpw(passwordA, passwordB)) ||
				(passwordA != null && passwordA.startsWith("$2a$10") && BCrypt.checkpw(passwordB, passwordA));
	}

	private void compareUsers(User userA, User userB) {
		if (userA != null && userB != null) {
			Assert.assertTrue("User identifiers do not match!",
					eq(userA.getIdentifier(), userB.getIdentifier()));
			Assert.assertTrue("User fingerprints do not match!",
					eq(userA.getFingerprint(), userB.getFingerprint()));

			Assert.assertTrue("User passowrds do not match!", checkPassword(userA.getPassword(), userB.getPassword()));
			Assert.assertTrue("User roles do not match!", eq(userA.getRole(), userB.getRole()));
			Assert.assertTrue("User group lists do not match!", eq(new HashSet<>(userA.getGroupList()), new HashSet<>(userB.getGroupList())));

		} else if (userA != null || userB != null) {
			Assert.fail("Users not equal since one is null!");
		}
	}

	public UserManagerTestEngine() {
		this(false);
	}

	public UserManagerTestEngine(@NotNull boolean useOfflineModule) {
		environmentState = StateEngine.data;
		this.useOfflineModule = useOfflineModule;
	}

	public void usermod(@NotNull AbstractServerProfile server, @NotNull String username, @Nullable Boolean delete, @Nullable String password, @Nullable String certpath,
	                    @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String... group) {
		innerUserCertMod(server, ModMode.USER_MOD, username, delete, password, certpath, administrator, fingerprint, group);
	}


	public void certmod(@NotNull AbstractServerProfile server, @NotNull String certpath, @Nullable Boolean delete, @Nullable String password, @Nullable Boolean administrator,
	                    @Nullable String fingerprint, @Nullable String... group) {
		innerUserCertMod(server, ModMode.CERT_MOD, null, delete, password, certpath, administrator, fingerprint, group);
	}

	private void innerUserCertMod(@NotNull AbstractServerProfile server, @NotNull ModMode modMode, @Nullable String username, @Nullable Boolean delete, @Nullable String password, @Nullable String certpath,
	                              @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String... group) {
		// Delayed loading since it must be created by the server being started up
		if (ofam == null) {
			if (useOfflineModule) {
				ofam = new OfflineFileAuthModule();

			} else {
				OnlineFileAuthModule ofam = new OnlineFileAuthModule();
				this.ofam = ofam;
				ofam.init(server);
			}
		}

		try {
			modVerificationPrep(server, modMode, username, delete, password, certpath, administrator, fingerprint, group);
			Thread.sleep(200);
			modAction(server, modMode, username, delete, password, certpath, administrator, fingerprint, group);
			Thread.sleep(800);
			modVerification(server, modMode, username, delete, password, certpath, administrator, fingerprint, group);
			modStateUpdate(server, modMode, username, delete, password, certpath, administrator, fingerprint, group);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}


	private void modVerificationPrep(@NotNull AbstractServerProfile server, @NotNull ModMode modMode, @Nullable String username, @Nullable Boolean delete,
	                                 @Nullable String password, @Nullable String certpath,
	                                 @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String... group) {
		try {
			switch (modMode) {
				case CERT_MOD:
					Assert.assertNotNull("Certpath must be supplied for the certmod command!", certpath);
					Assert.assertNull("Username cannot be supplied with certmod command!", username);
					X509Certificate certificate = SSLHelper.getCertificate(certpath);
					username = SSLHelper.getCertificateUserName(certificate);
					break;

				case USER_MOD:
					Assert.assertNotNull("Username must be supplied for the usermod command!", username);
					break;

				default:
					throw new RuntimeException("Unexpected ModMode '" + modMode.name() + "'!");
			}

			User diskUser = null;

			UserAuthenticationFile diskUserAuthenticationFile = environmentState.getState(server).getUserAuthenticationFileState().getCurrentServerUserAuthenticationFile();
			if (diskUserAuthenticationFile != null) {
				List<User> diskUsers = environmentState.getState(server).getUserAuthenticationFileState().getCurrentServerUserAuthenticationFile().getUser();

				for (User u : diskUsers) {
					if (u.getIdentifier().equals(username)) {
						diskUser = u;
						break;
					}
				}
			}

			List<User> localUsers = environmentState.getState(server).getUserAuthenticationFileState().getLocalUserAuthenticationFile().getUser();
			User localUser = null;
			for (User u : localUsers) {
				if (u.getIdentifier().equals(username)) {
					localUser = u;
					break;
				}
			}

			compareUsers(diskUser, localUser);

		} catch (IOException | CertificateException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void modAction(@NotNull AbstractServerProfile server, @NotNull ModMode modMode, @Nullable String username, @Nullable Boolean delete, @Nullable String password, @Nullable String certpath,
	                       @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String... group) {
		switch (modMode) {
			case CERT_MOD:
				Assert.assertNotNull("Certpath cannot be null for certmod!", certpath);
				ofam.certmod(certpath, delete, password, administrator, fingerprint, group, null, null);
				break;

			case USER_MOD:
				Assert.assertNotNull("Username cannot be null for usermod!", username);
				ofam.usermod(username, delete, password, certpath, administrator, fingerprint, group, null, null);
				break;

			default:
				throw new RuntimeException("Unexpected ModMode '" + modMode.name() + "'!");
		}
	}


	private void modVerification(@NotNull AbstractServerProfile server, @NotNull ModMode modMode, @Nullable String username, @Nullable Boolean delete,
	                             @Nullable String password, @Nullable String certpath,
	                             @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String... group) {
		logger.info("INPUT: username='" + username + "', password='" + password + "', certpath='" + certpath
				+ "', administrator='" + administrator + "', fingerprint='" + fingerprint + "', group='" +
				Arrays.toString(group) + "'");

		User changedFileUser = null;
		User localUser = null;
		User unchangedFileUser = null;

		try {
			if (modMode == ModMode.CERT_MOD) {
				Assert.assertNotNull("Certpath for certmod cannot be null!", certpath);
			}
			if (certpath != null) {
				X509Certificate certificate = SSLHelper.getCertificate(certpath);
				if (modMode == ModMode.CERT_MOD) {
					username = SSLHelper.getCertificateUserName(certificate);
				}
				if (fingerprint == null) {
					fingerprint = SSLHelper.getCertificateFingerprint(certificate);
				}
			}

			environmentState.getState(server).getUserAuthenticationFileState().updateCurrentServerAuthenticationFile();
			List<User> currentServerUsers = environmentState.getState(server).getUserAuthenticationFileState().getCurrentServerUserAuthenticationFile().getUser();

			for (User u : currentServerUsers) {
				if (u.getIdentifier().equals(username)) {
					changedFileUser = u;
					break;
				}
			}
			Assert.assertTrue("The user '" + username + "' has not been added to the UserAuthenticationFile!",
					changedFileUser != null);

			List<User> localUsers = environmentState.getState(server).getUserAuthenticationFileState().getLocalUserAuthenticationFile().getUser();
			for (User u : localUsers) {
				if (u.getIdentifier().equals(username)) {
					localUser = u;
					break;
				}
			}

			UserAuthenticationFile diskUserAuthenticationFile = environmentState.getState(server).getUserAuthenticationFileState().getPreviousServerUserAuthenticationFile();
			if (diskUserAuthenticationFile != null) {
				List<User> previousServerUsers = diskUserAuthenticationFile.getUser();
				for (User u : previousServerUsers) {
					if (u.getIdentifier().equals(username)) {
						unchangedFileUser = u;
						break;
					}
				}
			}

			Assert.assertNotNull("User '" + changedFileUser.getIdentifier() +
					"' does not exist in UserAuthenticationFile!", changedFileUser);

			if (unchangedFileUser == null) {
				if (password == null) {
					Assert.assertNull("User '" + username + "' should not have a password set!",
							changedFileUser.getPassword());
				} else {
					Assert.assertNotNull("User '" + username + "' should have a password set!",
							changedFileUser.getPassword());
					Assert.assertTrue("User '" + username + "' should have a hashed password!",
							changedFileUser.isPasswordHashed());
					Assert.assertTrue(checkPassword(password, changedFileUser.getPassword()));
				}

			} else {
				Assert.assertNotNull("Locally validated user should not be null if one existed prior to test!", localUser);
				if (password == null) {
					Assert.assertTrue("User '" + username + "' Should have the same password as prior to this test!",
							eq(unchangedFileUser.getPassword(), changedFileUser.getPassword()));
					Assert.assertTrue("Stored password hash should match password!", checkPassword(localUser.getPassword(), changedFileUser.getPassword()));

				} else {
					Assert.assertNotNull("User '" + username + "' should have a new password set!",
							changedFileUser.getPassword());
					Assert.assertTrue("User '" + username + "' should have a hashed password!",
							changedFileUser.isPasswordHashed());

					if (password.equals(localUser.getPassword())) {
						Assert.assertTrue("Password hash should not change if password was not changed!!",
								unchangedFileUser.getPassword().equals(changedFileUser.getPassword()));
					} else {
						Assert.assertTrue("Stored password hash should match password!", checkPassword(password, changedFileUser.getPassword()));
					}
				}
			}

			Assert.assertTrue("User fingerprint missmatch!",
					eq(changedFileUser.getFingerprint(), (localUser == null ? fingerprint : fingerprint == null ? localUser.getFingerprint() : fingerprint)));

			Role expectedRole;
			if (administrator == null) {
				if (localUser == null) {
					expectedRole = Role.ROLE_ANONYMOUS;
				} else {
					expectedRole = localUser.getRole();
				}
			} else {
				if (administrator) {
					expectedRole = Role.ROLE_ADMIN;
				} else {
					expectedRole = Role.ROLE_ANONYMOUS;
				}
			}
			Assert.assertTrue("User role missmatch! Expected '" + expectedRole.name() +
							"', got '" + changedFileUser.getRole() + "'.",
					eq(changedFileUser.getRole(), expectedRole));

			Set<String> expectedGroups = new HashSet<>();
			if (localUser == null) {
				if (group == null || group.length == 0) {
					expectedGroups.add(ANON_GROUP);
				} else {
					expectedGroups.addAll(Arrays.asList(group));
				}

			} else {
				if (group != null && group.length > 0) {
					expectedGroups.addAll(Arrays.asList(group));
				}
			}
			Set<String> remoteUserGroupSet = new HashSet<>(changedFileUser.getGroupList());
			Assert.assertEquals("Duplicates in group list in UserAuthorizationFile!",
					remoteUserGroupSet.size(), changedFileUser.getGroupList().size());
			Assert.assertTrue("User group sets do not match!",
					eq(remoteUserGroupSet, expectedGroups));

		} catch (CertificateException | IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
			throw new RuntimeException(e);
		} finally {
			if (unchangedFileUser == null) {
				logger.info("Initial Disk User: None");
			} else {
				logger.info("Initial Disk User:\n" + Util.getUserDisplayString(unchangedFileUser));
			}
			if (localUser == null) {
				logger.info("Expected Disk User: None");
			} else {
				logger.info("Expected Disk User:\n" + Util.getUserDisplayString(localUser));
			}
			if (changedFileUser == null) {
				logger.info("Actual Disk User: None");
			} else {
				logger.info("Actual Disk User:\n" + Util.getUserDisplayString(changedFileUser));
			}
		}
	}

	private void modStateUpdate(@NotNull AbstractServerProfile server, @NotNull ModMode modMode, @Nullable String username, @Nullable Boolean delete,
	                            @Nullable String password, @Nullable String certpath,
	                            @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String... group) {

		try {
			if (modMode == ModMode.CERT_MOD) {
				Assert.assertNotNull("Certpath for certmod cannot be null!", certpath);
			}
			if (certpath != null) {
				X509Certificate certificate = SSLHelper.getCertificate(certpath);
				if (modMode == ModMode.CERT_MOD) {
					username = SSLHelper.getCertificateUserName(certificate);
				}
				if (fingerprint == null) {
					fingerprint = SSLHelper.getCertificateFingerprint(certificate);
				}
			}

			List<User> localUsers = environmentState.getState(server).getUserAuthenticationFileState().getLocalUserAuthenticationFile().getUser();
			User locallyManagedUser = null;
			for (User u : localUsers) {
				if (u.getIdentifier().equals(username)) {
					locallyManagedUser = u;
					break;
				}
			}

			if (locallyManagedUser == null) {
				locallyManagedUser = new User();

				locallyManagedUser.setIdentifier(username);

				if (password != null) {
					locallyManagedUser.setPassword(password);
					locallyManagedUser.setPasswordHashed(false);
				}

				if (fingerprint != null) {
					locallyManagedUser.setFingerprint(fingerprint);
				}

				locallyManagedUser.setRole((administrator == null || !administrator) ? Role.ROLE_ANONYMOUS : Role.ROLE_ADMIN);

				if (group == null) {
					locallyManagedUser.getGroupList().add(ANON_GROUP);
				} else {
					locallyManagedUser.getGroupList().clear();
					for (String g : group) {
						locallyManagedUser.getGroupList().add(g);
					}
				}

				localUsers.add(locallyManagedUser);

			} else {
				if (password != null) {
					locallyManagedUser.setPassword(password);
					locallyManagedUser.setPasswordHashed(false);
				}

				if (fingerprint != null) {
					locallyManagedUser.setFingerprint(fingerprint);
				}

				if (administrator != null) {
					locallyManagedUser.setRole(administrator ? Role.ROLE_ADMIN : Role.ROLE_ANONYMOUS);
				}

				if (group != null) {
					locallyManagedUser.getGroupList().clear();
					for (String g : group) {
						locallyManagedUser.getGroupList().add(g);
					}
				}
			}

			List<User> currentServerUsers = environmentState.getState(server).getUserAuthenticationFileState().getCurrentServerUserAuthenticationFile().getUser();

			User currentUserFromDisk = null;
			for (User u : currentServerUsers) {
				if (u.getIdentifier().equals(username)) {
					currentUserFromDisk = u;
					break;
				}
			}
			compareUsers(locallyManagedUser, currentUserFromDisk);

		} catch (IOException | CertificateException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
