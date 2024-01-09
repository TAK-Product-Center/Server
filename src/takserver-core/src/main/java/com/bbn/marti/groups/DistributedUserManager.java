package com.bbn.marti.groups;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.JAXBException;

import org.apache.ignite.services.ServiceContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.groups.value.FileAuthenticatorControl;
import com.bbn.marti.remote.groups.FileUserManagementInterface;
import com.bbn.marti.remote.groups.SimpleGroupWithUsersModel;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.xml.bindings.Role;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import com.bbn.marti.xml.bindings.UserAuthenticationFile.User;


public class DistributedUserManager implements org.apache.ignite.services.Service, FileUserManagementInterface {

	private static final long serialVersionUID = -9068929120331688334L;

	public static final Logger logger = LoggerFactory.getLogger(DistributedUserManager.class);

    private synchronized FileAuthenticator getFileAuthenticator() {
        
        return MessagingDependencyInjectionProxy.getInstance().fileAuthenticator();
    }

    public DistributedUserManager() {
        if (logger.isDebugEnabled()) {
            // TODO: Throw Exception here?
            logger.info("DistributedUserManager constructor");
        }
    }

    @Override
    public boolean userExists(@NotNull String userIdentifier) {
        return getFileAuthenticator().userExists(userIdentifier);
    }

    @Override
    public FileAuthenticatorControl addOrUpdateUser(@NotNull String userIdentifier, @Nullable String userPassword, boolean wasPasswordAlreadyHashed) {
    	FileAuthenticator fileAuthenticator = getFileAuthenticator();
    	FileAuthenticatorControl control;
        try {
        	control = fileAuthenticator.addOrUpdateUser(userIdentifier, userPassword, wasPasswordAlreadyHashed);
            fileAuthenticator.saveChanges(control);
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    	return control;
    }

    @Override
    public FileAuthenticatorControl addOrUpdateUserFromCertificate(@NotNull X509Certificate certificate) {
    	FileAuthenticator fileAuthenticator = getFileAuthenticator();
    	FileAuthenticatorControl control;
        try {
        	control = fileAuthenticator.addOrUpdateUserFromCertificate(certificate);
            fileAuthenticator.saveChanges(control);
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    	return control;
    }

    @Override
    public FileAuthenticatorControl removeUser(@NotNull String userIdentifier) {
    	 FileAuthenticator fileAuthenticator = getFileAuthenticator();
    	 FileAuthenticatorControl control;
         try {
             control = fileAuthenticator.removeUser(userIdentifier);
             fileAuthenticator.saveChanges(control);
         } catch (IOException | JAXBException e) {
             e.printStackTrace();
             throw new RuntimeException(e);
         }
    	return control;
    }

    @Override
    public FileAuthenticatorControl addUserToGroup(@NotNull String userIdentifier, @NotNull String groupName) {
    	FileAuthenticator fileAuthenticator = getFileAuthenticator();
    	FileAuthenticatorControl control;
        try {
        	control = fileAuthenticator.addUserToGroup(userIdentifier, groupName);
            fileAuthenticator.saveChanges(control);
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    	return control;
    }

    @Override
    public FileAuthenticatorControl removeUserFromGroup(@NotNull String userIdentifier, @NotNull String groupName) {
    	FileAuthenticator fileAuthenticator = getFileAuthenticator();
    	FileAuthenticatorControl control;
        try {
        	control = fileAuthenticator.removeUserFromGroup(userIdentifier, groupName);
            fileAuthenticator.saveChanges(control);
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    	return control;
    }

    @Override
    public boolean isUserInGroup(@NotNull String userIdentifier, @NotNull String groupName) {
        return getFileAuthenticator().isUserInGroup(userIdentifier, groupName);
    }

    @Override
    public void saveChanges(FileAuthenticatorControl control) throws JAXBException, IOException {
    	try {
			getFileAuthenticator().saveChanges(null);
		} catch (Exception e) {
			logger.info("Error saving CoreConfig: " + e);
		} 
    }

    @NotNull
    @Override
    public Set<String> getUserList() {
        return new HashSet<>(getFileAuthenticator().getUserList());
    }

    @Override
    public Role getUserRole(@NotNull String userIdentifier) {
        return getFileAuthenticator().getUserRole(userIdentifier);
    }

    @Override
    public FileAuthenticatorControl setUserRole(@NotNull String userIdentifier, @NotNull Role role) {
    	FileAuthenticator fileAuthenticator = getFileAuthenticator();
    	FileAuthenticatorControl control;
        try {
        	control = fileAuthenticator.setUserRole(userIdentifier, role);
            fileAuthenticator.saveChanges(control);
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    	return control;
    }

    @NotNull
    @Override
    public Map<String, Set<String>> getUsersWithGroups() {
        return getFileAuthenticator().getUsersWithGroups();
    }

    @NotNull
    @Override
    public Map<String, Set<String>> getRolesWithUsers() {
        return getFileAuthenticator().getRolesWithUsers();
    }

    @NotNull
    @Override
    public Map<String, Set<String>> getGroupsWithUsers() {
        return getFileAuthenticator().getGroupsWithUsers();
    }

    @Override
    public Collection<UserAuthenticationFile.User> getAllUsers() {
        return getFileAuthenticator().getAllUsers();
    }

    @Override
    @Nullable
    public List<UserAuthenticationFile.User> getUsers(String... userIdentifier) {
        return getFileAuthenticator().getUsers(userIdentifier);
    }

    @Override
    public FileAuthenticatorControl addOrUpdateUser(UserAuthenticationFile.User user, boolean wasPasswordAlreadyHashed, UserAuthenticationFile.User userPre) {
    	FileAuthenticator fileAuthenticator = getFileAuthenticator();
    	FileAuthenticatorControl control;
		try {
			control = fileAuthenticator.addOrUpdateUser(user, wasPasswordAlreadyHashed, userPre);
			fileAuthenticator.saveChanges(control);
		} catch (IOException | JAXBException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    	return control;
    }

    @Override
    public FileAuthenticatorControl setUserFingerprint(@NotNull String userIdentifier, @NotNull String fingerprint) {
    	FileAuthenticator fileAuthenticator = getFileAuthenticator();
    	FileAuthenticatorControl control;
		try {
			control = fileAuthenticator.setUserFingerprint(userIdentifier, fingerprint);
			fileAuthenticator.saveChanges(control);
		} catch (IOException | JAXBException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    	return control;
    }

    @Override
    public String getUserFingerprint(@NotNull String userIdentifier) {
        return getFileAuthenticator().getUserFingerprint(userIdentifier);
    }

    @Override
    public void cancel(ServiceContext ctx) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("DistributedUserManager service cancelled");
    	}
    }

    @Override
    public void init(ServiceContext ctx) throws Exception {
    	if (logger.isDebugEnabled()) {
    		logger.debug("init DistributedUserManager");
    	}
    }

    @Override
    public void execute(ServiceContext ctx) throws Exception {
    	if (logger.isDebugEnabled()) {
    		logger.debug(getClass().getSimpleName() + " execute");
    	}
    }

	@Override
	public UserAuthenticationFile getUserAuthenticationFile() {
		return getFileAuthenticator().getUserAuthenticationFile();
	}

	@Override
	public User getFirstUser(String userIdentifier) {
		return getFileAuthenticator().getFirstUser(userIdentifier);
	}
	
	@Override
	public SimpleGroupWithUsersModel getUsersInGroup(String groupName) {
		return getFileAuthenticator().getUsersInGroup(groupName);
	}

	@Override
	public Set<String> getGroupNames(){
		return getFileAuthenticator().getGroupNames();
	}
}
