package com.bbn.marti.groups;

import java.util.Set;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.groups.value.FileAuthenticatorControl;
import com.bbn.marti.remote.groups.SimpleGroupWithUsersModel;
import com.bbn.marti.remote.groups.UserAccountManagementService;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;

/**
 * 
 * This service class manages distributed file user account.
 */
public class DistributedFileUserAccountManagementService implements org.apache.ignite.services.Service, UserAccountManagementService<FileUserAccount> {

	private static final long serialVersionUID = -6593846799515748042L;
	
	public static final Logger logger = LoggerFactory.getLogger(DistributedFileUserAccountManagementService.class);

    private FileAuthenticator getFileAuthenticator() {
    	synchronized(this) {
    		return MessagingDependencyInjectionProxy.getInstance().fileAuthenticator();
    	}
    }

    public DistributedFileUserAccountManagementService() {
        if (logger.isDebugEnabled()) {
            logger.debug("DistributedFileUserAccountManagementService constructor");
        }
    }

	@Override
	public Set<String> getUserIdentifierList() {
		return getFileAuthenticator().getUserList();
	}

	@Override
	public boolean userExists(String userIdentifier) {
		return getFileAuthenticator().userExists(userIdentifier);
	}

	@Override
	public boolean removeUser(String userIdentifier) {
		
	    FileAuthenticatorControl control = getFileAuthenticator().removeUser(userIdentifier);
	    
	    if (control.getFileUser() == null) { // User not found
	    	return false;
	    }    
	    return true;
	}

	@Override
	public Set<String> getGroupNames() {
		return getFileAuthenticator().getGroupNames();
	}

	@Override
	public FileUserAccount getUser(String userIdentifier) {
		
		UserAuthenticationFile.User user = getFileAuthenticator().getFirstUser(userIdentifier);
		
		if (user == null) { // User does not exist
			return null; 
		}
		FileUserAccount fileUserAccount = new FileUserAccount();
		fileUserAccount.setIdentifier(user.getIdentifier());
		fileUserAccount.setPassword(user.getPassword());
		fileUserAccount.setPasswordHashed(user.isPasswordHashed());
		fileUserAccount.setGroupListIN(user.getGroupListIN());
		fileUserAccount.setGroupListOUT(user.getGroupListOUT());
		fileUserAccount.setGroupList(user.getGroupList());

		return fileUserAccount;
	}

	@Override
	public boolean addUser(FileUserAccount userObject) {
		
		UserAuthenticationFile.User user = getFileAuthenticator().getFirstUser(userObject.getIdentifier());
		
		if (user != null) {
			return false; 
		}
		
		UserAuthenticationFile.User newUser = new UserAuthenticationFile.User();
		newUser.setIdentifier(userObject.getIdentifier());
		newUser.setPassword(userObject.getPassword());
		newUser.setPasswordHashed(userObject.getPasswordHashed());
		newUser.getGroupList().addAll(userObject.getGroupList());
		newUser.getGroupListIN().addAll(userObject.getGroupListIN());
		newUser.getGroupListOUT().addAll(userObject.getGroupListOUT());
		
    	FileAuthenticatorControl control = getFileAuthenticator().addOrUpdateUser(newUser, false, null);
    	if (control.getFileUser() == null) {
    		return false;
    	}
		return true;
	}

	@Override
	public boolean updateUser(FileUserAccount fileUserAccount) {
		UserAuthenticationFile.User existingUser = getFileAuthenticator().getFirstUser(fileUserAccount.getIdentifier());
		
		if (existingUser == null) { // User does not exist
			return false; 
		}
		
		UserAuthenticationFile.User newUser = new UserAuthenticationFile.User();
		newUser.setIdentifier(fileUserAccount.getIdentifier());
		
		boolean isPasswordAlreadyHashed;
		if (fileUserAccount.getPassword() != null) { // user wants to change password
			newUser.setPassword(fileUserAccount.getPassword());
			newUser.setPasswordHashed(true);
			isPasswordAlreadyHashed = false;
		} else {
			newUser.setPassword(existingUser.getPassword());
			newUser.setPasswordHashed(existingUser.isPasswordHashed());
			isPasswordAlreadyHashed = true;
		}

		if (fileUserAccount.getGroupListIN() != null) { // user wants to change group IN
			newUser.getGroupListIN().addAll(fileUserAccount.getGroupListIN());
		} else {
			newUser.getGroupListIN().addAll(existingUser.getGroupListIN());
		}
		
		if (fileUserAccount.getGroupListOUT() != null) { // user wants to change group OUT
			newUser.getGroupListOUT().addAll(fileUserAccount.getGroupListOUT());
		} else {
			newUser.getGroupListOUT().addAll(existingUser.getGroupListOUT());
		}
		
		if (fileUserAccount.getGroupList() != null) { // user wants to change group
			newUser.getGroupList().addAll(fileUserAccount.getGroupList());
		} else {
			newUser.getGroupList().addAll(existingUser.getGroupList());
		}
		
    	FileAuthenticatorControl control = getFileAuthenticator().addOrUpdateUser(newUser, isPasswordAlreadyHashed, existingUser);
    	if (control.getFileUser() == null) {
    		return false;
    	}
		return true;
		
	}

	@Override
	public SimpleGroupWithUsersModel getUsersInGroup(String groupName) {
		return getFileAuthenticator().getUsersInGroup(groupName);
	}

	@Override
	public void cancel(ServiceContext arg0) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("DistributedFileUserAccountManagementService service cancelled");
    	}		
	}

	@Override
	public void execute(ServiceContext arg0) throws Exception {
    	if (logger.isDebugEnabled()) {
    		logger.debug(getClass().getSimpleName() + " execute");
    	}	
	}

	@Override
	public void init(ServiceContext arg0) throws Exception {
    	if (logger.isDebugEnabled()) {
    		logger.debug("init DistributedFileUserAccountManagementService");
    	}
		
	}
}

