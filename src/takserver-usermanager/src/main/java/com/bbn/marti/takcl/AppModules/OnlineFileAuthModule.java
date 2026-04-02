package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.groups.value.FileAuthenticatorControl;
import com.bbn.marti.remote.groups.FileUserManagementInterface;
import com.bbn.marti.takcl.AppModules.generic.FileAuthModuleInterface;
import com.bbn.marti.takcl.AppModules.generic.ServerAppModuleInterface;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TakclIgniteHelper;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.generated.CLINonvalidatingUsers;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.xml.bindings.Role;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import jakarta.xml.bind.JAXBException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.StringUtils;
import tak.server.util.PasswordUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Allows remote administration of the file based authentication system. The authentication system must be enabled prior
 * to server start or all remote user authentication commands will fail with a RemoteException!
 */
public class OnlineFileAuthModule implements ServerAppModuleInterface, FileAuthModuleInterface {

    private FileUserManagementInterface userManagementInterface;

    private AbstractServerProfile server;

    public OnlineFileAuthModule() {
    }

    @NotNull
    @Override
    public TakclRunMode[] getRunModes() {
        return new TakclRunMode[]{TakclRunMode.REMOTE_SERVER_INTERACTION};
    }

    @Override
    public ServerState getRequiredServerState() {
        return ServerState.RUNNING;
    }

    @Override
    public String getCommandDescription() {
        return "File-based authentication user management interface.  File authorization must be enabled using the " +
            "offline configuration module or the generated file will go unused.";
    }

    @Override
    public synchronized void init(@NotNull AbstractServerProfile server) throws EndUserReadableException {
        this.server = server;
        if (userManagementInterface == null) {
            userManagementInterface = TakclIgniteHelper.getUserManager(server);
        }
    }

    @Override
    public synchronized void halt() {
        if (server != null) {
            TakclIgniteHelper.closeAssociatedIgniteInstance(server);
        }
    }

    @Override
    public String usermod(@NotNull String username, @Nullable Boolean delete, @Nullable String password,
                          @Nullable String certpath, @Nullable Boolean administrator, @Nullable String fingerprint,
                          @Nullable String[] group, @Nullable String[] inGroup, @Nullable String[] outGroup,
                          @Nullable Boolean appendGroups, @Nullable Boolean removeGroups, @Nullable Boolean displayInfo) throws EndUserReadableException {

        appendGroups = appendGroups != null && appendGroups;
        removeGroups = removeGroups != null && removeGroups;

        if (displayInfo != null && displayInfo) {
            UserAuthenticationFile.User user = userManagementInterface.getFirstUser(username);
            if (user == null) {
                throw new EndUserReadableException("Could not find a user with the name '" + username + "'!");
            }
            return Util.getUserDisplayString(user);

        } else if (delete != null && delete) {
            String result = removeUserFromGroups(username);
            userManagementInterface.removeUser(username);
            return result;

        } else {
            return innerUserCertMod(username, password, certpath, administrator, fingerprint, group, inGroup, outGroup, appendGroups, removeGroups);
        }
    }

    private UserAuthenticationFile.User getUser(String username) {
        List<UserAuthenticationFile.User> userList = userManagementInterface.getUsers(username);

        UserAuthenticationFile.User user;

        if (userList == null || userList.isEmpty()) {
            user = new UserAuthenticationFile.User();
            user.setIdentifier(username);

        } else {
            user = userList.get(0);
        }

        return user;
    }

    private String innerUserCertMod(@Nullable String username, @Nullable String password, @Nullable String certpath,
                                    @Nullable Boolean administrator, @Nullable String fingerprint,
                                    @Nullable String[] group, @Nullable String[] inGroup, @Nullable String[] outGroup,
                                    boolean appendGroups, boolean removeGroups) throws EndUserReadableException {

        StringBuilder rval = new StringBuilder();

        if (password != null && !PasswordUtils.isValidPassword(password)) {
            throw new EndUserReadableException(PasswordUtils.FAILED_COMPLEXITY_CHECK_ERROR_MESSAGE);
        }

        if (appendGroups && removeGroups) {
            throw new EndUserReadableException("The '--append' and '--remove' specifiers cannot be supplied at the same time!");
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

        List<UserAuthenticationFile.User> userList = userManagementInterface.getUsers(username);

        UserAuthenticationFile.User user;

        if (userList == null || userList.isEmpty()) {
            newUser = true;
            user = new UserAuthenticationFile.User();
            user.setIdentifier(username);

        } else {
            user = userList.get(0);
        }

        UserAuthenticationFile.User userChanges = new UserAuthenticationFile.User();
        userChanges.getGroupList().addAll(user.getGroupList());
        userChanges.getGroupListIN().addAll(user.getGroupListIN());
        userChanges.getGroupListOUT().addAll(user.getGroupListOUT());

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

        BiConsumer<List<String>, String[]> modifyGroups = (userGroupList, modifiedGroupList) -> {
            if (modifiedGroupList != null && modifiedGroupList.length > 0) {
                if (appendGroups) {
                    userGroupList.addAll(Arrays.asList(modifiedGroupList));
                } else if (removeGroups) {
                    userGroupList.removeAll(Arrays.asList(modifiedGroupList));

                } else {
                    userGroupList.clear();
                    userGroupList.addAll(Arrays.asList(modifiedGroupList));
                }
            }
        };

        modifyGroups.accept(user.getGroupList(), group);
        modifyGroups.accept(user.getGroupListIN(), inGroup);
        modifyGroups.accept(user.getGroupListOUT(), outGroup);

        @SuppressWarnings("unused")
        FileAuthenticatorControl result = userManagementInterface.addOrUpdateUser(user, false, userChanges);

        fileSaveChanges();

        if (newUser) {
            rval.append("New User Added:\n").append(Util.getUserDisplayString(userManagementInterface.getFirstUser(username)));

        } else {
            rval.append("User Updated:\n").append(Util.getUserDisplayString(userManagementInterface.getFirstUser(username)));
        }

        if (user.getFingerprint() == null && user.getPassword() == null) {
            rval.append("\n\nNO FINGERPRINT, CERTIFICATE, OR PASSWORD WAS PROVIDED FOR THIS USER AND THEY WILL NOT BE USABLE!");
        }
        return rval.toString();
    }

    @Override
    public String certmod(@NotNull String certpath, @Nullable Boolean delete, @Nullable String password,
                          @Nullable Boolean administrator, @Nullable String fingerprint, @Nullable String[] group,
                          @Nullable String[] inGroup, @Nullable String[] outGroup, @Nullable Boolean appendGroups,
                          @Nullable Boolean removeGroups, @Nullable Boolean displayInfo) throws EndUserReadableException {

        appendGroups = appendGroups != null && appendGroups;
        removeGroups = removeGroups != null && removeGroups;

        if (displayInfo != null && displayInfo) {
            String username = SSLHelper.loadCertUsernameForEndUser(certpath);
            if (username == null) {
                throw new EndUserReadableException("Could not find an existing user for the supplied certificate!");
            }
            UserAuthenticationFile.User user = userManagementInterface.getFirstUser(username);
            if (user == null) {
                throw new EndUserReadableException("Could not find a user with the name '" + username + "' extracted from the supplied certificate!");
            }
            return Util.getUserDisplayString(user);

        } else if (delete != null && delete) {
            String username = SSLHelper.loadCertUsernameForEndUser(certpath);
            return removeUsers(username);
        } else {
            return innerUserCertMod(null, password, certpath, administrator, fingerprint, group, inGroup, outGroup, appendGroups, removeGroups);
        }
    }

    @Override
    public String addOrUpdateUser(String userIdentifier, String userPassword) {
        userManagementInterface.addOrUpdateUser(userIdentifier, userPassword, false);
        fileSaveChanges();
        return "Added User '" + userIdentifier + "'";
    }

    @Override
    public String removeUsers(String... userName) {
        for (String name : userName) {
            userManagementInterface.removeUser(name);
        }
        fileSaveChanges();

        StringBuilder sb = new StringBuilder();
        for (String name : userName) {
            if (sb.length() == 0) {
                sb = new StringBuilder();
                sb.append("Removed Users:\n").append("\t").append(name).append("\n");
            } else {
                sb.append("\t").append(name).append("\n");
            }

        }
        return sb.toString();
    }

    @Override
    public String addUsersToGroup(String groupIdentifier, String... userName) {
        for (String name : userName) {
            if (!userManagementInterface.isUserInGroup(name, groupIdentifier)) {
                userManagementInterface.addUserToGroup(name, groupIdentifier);
            }
            userManagementInterface.addUserToGroup(name, groupIdentifier);
        }
        fileSaveChanges();
        return "The group \"" + groupIdentifier + "\" now includes the users \"" +
            Util.joinString(userName, "\", \"") + "\"";

    }

    @Override
    public String addUserToGroups(String userName, String... groupIdentifier) {
        if (!userManagementInterface.userExists(userName)) {
            return "The user \"" + userName + "\" must be created before having groups assigned!";
        }

        for (String gid : groupIdentifier) {
            addUsersToGroup(gid, userName);
        }
        fileSaveChanges();
        String prepend = "Added user '" + userName + "' to group '";
        return prepend + StringUtils.arrayToDelimitedString(groupIdentifier, "'\n" + prepend);
    }

    @Override
    public String removeUserFromGroups(String userName, String... groupIdentifier) {

        if (!userManagementInterface.userExists(userName)) {
            return "The user with the identifier '" + userName + "' does not exist!";
        }
        for (String gid : groupIdentifier) {
            if (userManagementInterface.isUserInGroup(userName, gid)) {
                userManagementInterface.removeUserFromGroup(userName, gid);
            }
        }
        fileSaveChanges();
        String prepend = "Removed user '" + userName + "' from group '";
        return prepend + StringUtils.arrayToDelimitedString(groupIdentifier, "'\n" + prepend);

    }

    @Override
    public String removeUsersFromGroup(String groupIdentifier, String... userName) {
        for (String name : userName) {
            userManagementInterface.removeUserFromGroup(name, groupIdentifier);
        }
        fileSaveChanges();
        String prepend = "Removed from group '" + groupIdentifier + "' user '";
        return prepend + StringUtils.arrayToDelimitedString(userName, "'\n" + prepend);

    }


    private void fileSaveChanges() {
        try {
            userManagementInterface.saveChanges(null);
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addPredefinedUserAndGroups(@NotNull CLINonvalidatingUsers user) {
        addOrUpdateUser(user.getUser().getUserName(), user.getUser().getPassword());

        for (GroupProfiles group : user.getUser().getDefinedGroupSet().getGroups()) {
            addUsersToGroup(group.name(), user.getUser().getUserName());
        }
    }

    @Override
    public String addUsers(String... userName) {
        Set<String> userSet = userManagementInterface.getUserList();

        SecureRandom random = new SecureRandom();

        // Easy way to guarantee four digits for simplicity...
        String passwordBase =
            Integer.toString(random.nextInt(9)) +
                Integer.toString(random.nextInt(9)) +
                Integer.toString(random.nextInt(9)) +
                Integer.toString(random.nextInt(9));

        StringBuilder sb = new StringBuilder();
        for (String name : userName) {
            if (!userSet.contains(name)) {
                userManagementInterface.addOrUpdateUser(name, passwordBase + name, false);
                sb.append("Added user: '").append(name).append("' with password: '").append(passwordBase).append(name).append("'\n");
            }
        }
        fileSaveChanges();
        return sb.toString();

    }

    @Override
    public String getUserRole(String userName) {
        return userManagementInterface.getUserRole(userName).toString();
    }

    @Override
    public String getRoleList() {
        return Util.displayableTable(userManagementInterface.getRolesWithUsers(), "Role", "Users");
    }

    @Override
    public String getUserList() {
        try {
            return Util.displayableTable(userManagementInterface.getUsersWithGroups(),
                "Username", "Groups");
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    @Override
    public String getGroupList() {
        return Util.displayableTable(userManagementInterface.getGroupsWithUsers(), "Group", "Users");
    }

    @Override
    public String getUserFingerprint(String userName) throws EndUserReadableException {
        String fingerprint = userManagementInterface.getUserFingerprint(userName);

        if (fingerprint == null || fingerprint.equals("")) {
            throw new EndUserReadableException(
                "Could not get a fingerprint for '" + userName + "'. Are you sure the user exists and has a fingerprint set?"
            );
        }
        return fingerprint;
    }


    @Override
    public String setUserRole(String userName, Role role) {
        userManagementInterface.setUserRole(userName, role);
        fileSaveChanges();
        return "Set the role of user '" + userName + "' to '" + role.value() + "'.";
    }


    @Override
    public String setUserFingerprint(String username, String fingerprint) {
        userManagementInterface.setUserFingerprint(username, fingerprint);
        fileSaveChanges();
        return "The fingerprint for user '" + username + "' has been updated.";
    }

    private String addOrUpdateCertificatesNoSave(@Nullable String newGroupIdentifier, @NotNull String... certificateFilepath) {
        try {
            StringBuilder sb = new StringBuilder();

            for (String certificateLocation : certificateFilepath) {
                X509Certificate certificate = SSLHelper.getCertificate(certificateLocation);
                String userName = SSLHelper.getCertificateUserName(certificate);
                boolean userExists = userManagementInterface.userExists(userName);
                userManagementInterface.addOrUpdateUserFromCertificate(certificate);
                if (newGroupIdentifier != null) {
                    userManagementInterface.addUserToGroup(SSLHelper.getCertificateUserName(certificate), newGroupIdentifier);
                    if (userExists) {
                        sb.append("Added to group '").append(newGroupIdentifier).append("' new user '")
                            .append(userName).append("' with fingerprint\n");
                    } else {
                        sb.append("Added to group '").append(newGroupIdentifier).append("' existing user '")
                            .append(userName).append("' with fingerprint\n");
                    }
                } else {
                    if (userExists) {
                        sb.append("Updated fingerprint for user '").append(userName).append("'\n");
                    } else {
                        sb.append("Added user '").append(userName).append("' with fingerprint\n");
                    }
                }
            }

            return sb.toString();

        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String addCertificates(String... certificateFilepath) {
        String rval = addOrUpdateCertificatesNoSave(null, certificateFilepath);
        fileSaveChanges();
        return rval;
    }

    @Override
    public String addCertificatesToGroup(String groupIdentifier, String... certificateFilepath) {
        String rval = addOrUpdateCertificatesNoSave(groupIdentifier, certificateFilepath);
        fileSaveChanges();
        return rval;
    }

    @Override
    public String addCertificateToGroups(String certificateFilepath, String... groupIdentifier) {
        try {
            StringBuilder sb = new StringBuilder();

            // Get the username
            X509Certificate certificate = SSLHelper.getCertificate(certificateFilepath);
            String userName = SSLHelper.getCertificateUserName(certificate);

            // Add the certificate
            sb.append(addOrUpdateCertificatesNoSave(null, certificateFilepath));

            // Add the user to the groups
            if (groupIdentifier != null && groupIdentifier.length > 0) {
                sb.append(addUserToGroups(userName, groupIdentifier));
            } else {
                fileSaveChanges();
            }

            return sb.toString();

        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String addAdminCertificates(String... certificateFilepath) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(addOrUpdateCertificatesNoSave(null, certificateFilepath));
            for (String cert : certificateFilepath) {
                // Get the username
                X509Certificate certificate = SSLHelper.getCertificate(cert);
                String userName = SSLHelper.getCertificateUserName(certificate);
                sb.append(setUserRole(userName, Role.ROLE_ADMIN));
            }
            fileSaveChanges();
            return sb.toString();
        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String addAdminCertificateToGroups(String certificateFilepath, String... groupIdentifier) {
        try {
            StringBuilder sb = new StringBuilder();

            sb.append(addAdminCertificates(certificateFilepath));

            // Get the username
            X509Certificate certificate = SSLHelper.getCertificate(certificateFilepath);
            String userName = SSLHelper.getCertificateUserName(certificate);
            sb.append(setUserRole(userName, Role.ROLE_ADMIN));
            sb.append(addUserToGroups(userName, groupIdentifier));
            fileSaveChanges();
            return sb.toString();
        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    UserAuthenticationFile.User copyUser(UserAuthenticationFile.User srcUser) {
        UserAuthenticationFile.User userCopy = new UserAuthenticationFile.User();

        userCopy.setIdentifier(srcUser.getIdentifier());
        userCopy.setPassword(srcUser.getPassword());
        userCopy.setPasswordHashed(srcUser.isPasswordHashed());
        userCopy.setRole(srcUser.getRole());
        userCopy.setFingerprint(srcUser.getFingerprint());
        userCopy.getGroupList().addAll(srcUser.getGroupList());
        userCopy.getGroupListIN().addAll(srcUser.getGroupListIN());
        userCopy.getGroupListOUT().addAll(srcUser.getGroupListOUT());

        return userCopy;
    }
}
