

package com.bbn.marti.remote.groups;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.bbn.marti.groups.value.FileAuthenticatorControl;
import com.bbn.marti.xml.bindings.Role;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import com.bbn.marti.xml.bindings.UserAuthenticationFile.User;

/**
 * Abstract interface used to define what can be done for user management
 * <p>
 * Created on 9/21/15.
 */
public interface FileUserManagementInterface {

	UserAuthenticationFile getUserAuthenticationFile();

    boolean userExists(@NotNull String userIdentifier);

    FileAuthenticatorControl addOrUpdateUser(@NotNull String userIdentifier, @Nullable String userPassword, boolean wasPasswordAlreadyHashed);

    FileAuthenticatorControl addOrUpdateUserFromCertificate(@NotNull X509Certificate certificate);

    FileAuthenticatorControl removeUser(@NotNull String userIdentifier);

    FileAuthenticatorControl addUserToGroup(@NotNull String userIdentifier, @NotNull String groupName);

    FileAuthenticatorControl removeUserFromGroup(@NotNull String userIdentifier, @NotNull String groupName);

    boolean isUserInGroup(@NotNull String userIdentifier, @NotNull String groupName);

    void saveChanges(FileAuthenticatorControl control) throws JAXBException, IOException;

    @NotNull
    Set<String> getUserList();

    Role getUserRole(@NotNull String userIdentifier);

    FileAuthenticatorControl setUserRole(@NotNull String userIdentifier, @NotNull Role role);

    @NotNull
    Map<String, Set<String>> getUsersWithGroups() throws FileNotFoundException;

    @NotNull
    Map<String, Set<String>> getRolesWithUsers();

    @NotNull
    Map<String, Set<String>> getGroupsWithUsers();

    Collection<UserAuthenticationFile.User> getAllUsers();

    @Nullable
    List<UserAuthenticationFile.User> getUsers(String... userIdentifier);

    FileAuthenticatorControl addOrUpdateUser(UserAuthenticationFile.User user, boolean wasPasswordAlreadyHashed, UserAuthenticationFile.User userPre);

    FileAuthenticatorControl setUserFingerprint(@NotNull String userIdentifier, @NotNull String fingerprint);

    String getUserFingerprint(@NotNull String userIdentifier);

	User getFirstUser(String userIdentifier);

	SimpleGroupWithUsersModel getUsersInGroup(String groupName);

	@NotNull
	Set<String> getGroupNames();
}
