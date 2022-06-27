package com.bbn.useraccountmanagement;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.groups.value.FileAuthenticatorControl;
import com.bbn.marti.remote.groups.FileUserManagementInterface;
import com.bbn.marti.remote.groups.SimpleGroupWithUsersModel;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import com.bbn.useraccountmanagement.model.GroupNameModel;
import com.bbn.useraccountmanagement.model.NewUserModel;
import com.bbn.useraccountmanagement.model.SimpleUserGroupModel;
import com.bbn.useraccountmanagement.model.UserGenerationInBulkModel;
import com.bbn.useraccountmanagement.model.UserPasswordModel;
import com.bbn.useraccountmanagement.model.UsernameModel;

import tak.server.util.PasswordUtils;
import tak.server.util.UsernameUtils;

@Validated
@RestController
@RequestMapping(value = "/user-management/api")
public class FileUserAccountManagementApi implements FileUserAccountManagementApiInterface{
	
    Logger logger = LoggerFactory.getLogger(FileUserAccountManagementApi.class);

    @Autowired
    @Qualifier("myFileUserManagementInterface")
    FileUserManagementInterface myFileUserManagementInterface;
  
/*
i.	Entering user name
ii.	Entering password (and confirming compliance of password complexity)
iii.	Select among available groups, or create new groups to associate with the user (specifying if the group is an IN group an OUT group, or both). This might be drag and drop (of available groups into IN, OUT, BOTH “buckets” for the user), auto-complete drop down fields, or other intuitive and easy to use mechanisms
 */
    @Override
    @RequestMapping(value = "/new-user", method = RequestMethod.POST)
    public void createOrUpdateFileUser(@RequestBody NewUserModel newUserModel) {
    	
    	createSingleFileUser(newUserModel);
    	
    	logger.info("Created new user: {}", newUserModel.getUsername());    		
    	
    }
    
	private void createSingleFileUser(NewUserModel newUserModel) {
		
		if (!UsernameUtils.isValidUsername(newUserModel.getUsername())) {
			
			throw new InvalidParameterException(UsernameUtils.ERROR_MESSAGE_FOR_INVALID_USERNAME);
		}
		
		if (!PasswordUtils.isValidPassword(newUserModel.getPassword())) {
			throw new InvalidParameterException(PasswordUtils.FAILED_COMPLEXITY_CHECK_ERROR_MESSAGE);
		}
		
		UserAuthenticationFile.User userPre = myFileUserManagementInterface.getFirstUser(newUserModel.getUsername());
		if (userPre != null) { 
			throw new InvalidParameterException("User already exists");
		}
		
    	try {
			UserAuthenticationFile.User newUser = new UserAuthenticationFile.User();
			newUser.setIdentifier(newUserModel.getUsername());
			newUser.setPassword(BCrypt.hashpw(newUserModel.getPassword(), BCrypt.gensalt()));
			newUser.setPasswordHashed(true);
			newUser.getGroupList().addAll(Arrays.asList(newUserModel.getGroupList()));
			newUser.getGroupListIN().addAll(Arrays.asList(newUserModel.getGroupListIN()));
			newUser.getGroupListOUT().addAll(Arrays.asList(newUserModel.getGroupListOUT()));
			
			myFileUserManagementInterface.addOrUpdateUser(newUser, true, null);
			
		} catch (Exception e) {
	    	logger.error("Error in createNewFileUser: {}", newUserModel.getUsername(), e);
			throw new RuntimeException(e);
		} 
    }

/*
 * 
b.	Provide a mechanism to bulk-generate user accounts
i.	Admin user provides pattern for usernames (e.g., “bbn-tak-[N]”)
ii.	System uses password generation mechanism to create passwords that meet TAK password complexity requirements
iii.	System produces output file with user/pass combos as a one-time accessible item (after which system forgets the unhashed passwords).
 */
    @Override
    @RequestMapping(value = "/new-users", method = RequestMethod.POST)
    public List<UserPasswordModel> createFileUsersInBulk(@RequestBody UserGenerationInBulkModel userGenerationInBulkModel) {
    	
    	if (!userGenerationInBulkModel.getUsernameExpression().contains("[N]")) {
			throw new InvalidParameterException("Username expression must contain [N]");
    	}
    	
    	try {

    		List<UserPasswordModel> re = new ArrayList<UserPasswordModel>();
    		for (int i = userGenerationInBulkModel.getStartN(); i<= userGenerationInBulkModel.getEndN(); i++) {
    			NewUserModel userModel = new NewUserModel();
    			String username = userGenerationInBulkModel.getUsernameExpression().replaceAll("\\[N\\]", String.valueOf(i));
    			userModel.setUsername(username);
    			String password = PasswordUtils.generatePassword();
    			userModel.setPassword(password);
    			userModel.setGroupList(userGenerationInBulkModel.getGroupList());
    			userModel.setGroupListIN(userGenerationInBulkModel.getGroupListIN());
    			userModel.setGroupListOUT(userGenerationInBulkModel.getGroupListOUT());
    			
    			try {
					createSingleFileUser(userModel);
	    			re.add(new UserPasswordModel(username, password));
				} catch (Exception e) {
			    	logger.error("Error in creating new user: {}. Reason: {}", username, e.getMessage());
				}
    		}
    		if (re.size() == 0) {
    	    	logger.error("No new user was created");
    			throw new InvalidParameterException("No new user was created. Check the inputs to make sure they are valid. Username requirements: minimum of 4 characters and contains only letters, numbers, dots, underscores and hyphens.");
    		}else {
    	    	logger.info("Created {} new users", re.size());
    		}
			return re;
		} catch (Exception e) {
	    	logger.error("Error in createFileUsersInBulk: {}", userGenerationInBulkModel.getUsernameExpression(), e);
			throw new RuntimeException(e);
		} 
    }
    
    
//    a.	Listing of all users
    @Override
    @RequestMapping(value = "/list-users", method = RequestMethod.GET)
    public Set<UsernameModel> getAllUsers() {
    	
    	Set<UsernameModel> re = new HashSet<>();
    	
    	Set<String> userList = myFileUserManagementInterface.getUserList();
    	userList.forEach(username->{
    		re.add(new UsernameModel(username));
    	});
    	
        return re;
    }
      
//  b.	Ability to see each user’s associated group information
    @Override
    @RequestMapping(value = "/get-groups-for-user/{username}", method = RequestMethod.GET)
    public SimpleUserGroupModel getGroupsForUsers(@PathVariable("username") String username) {
    	
    	UserAuthenticationFile.User user = myFileUserManagementInterface.getFirstUser(username);
    	
    	SimpleUserGroupModel re = new SimpleUserGroupModel();
        re.setUsername(username);
        re.setGroupList(user.getGroupList().toArray(new String[0]));
        re.setGroupListIN(user.getGroupListIN().toArray(new String[0]));
        re.setGroupListOUT(user.getGroupListOUT().toArray(new String[0]));
        
        return re;
    }
    
//  c.	Ability to change password 
    @Override
    @RequestMapping(value = "/change-user-password", method = RequestMethod.PUT)
    public void changeUserPassword(@RequestBody UserPasswordModel userPasswordModel) {
		
		if (!PasswordUtils.isValidPassword(userPasswordModel.getPassword())) {
			throw new InvalidParameterException(PasswordUtils.FAILED_COMPLEXITY_CHECK_ERROR_MESSAGE);
		}
		
		UserAuthenticationFile.User userPre = myFileUserManagementInterface.getFirstUser(userPasswordModel.getUsername());		
		if (userPre == null) {
			throw new InvalidParameterException("User not found!");
		}
		
    	try {
    		myFileUserManagementInterface.addOrUpdateUser(userPasswordModel.getUsername(), userPasswordModel.getPassword(), false);
			
		} catch (Exception e) {
	    	logger.error("Error in changeUserPassword: {}", userPasswordModel.getUsername(), e);
			throw new RuntimeException(e);
		} 
    }
    
//  d. Ability to add groups 
//  e. Ability to remove groups
    @Override
    @RequestMapping(value = "/update-groups", method = RequestMethod.PUT)
    public void updateGroupsForUser(@RequestBody SimpleUserGroupModel simpleUserModel) {

    	UserAuthenticationFile.User userPre = myFileUserManagementInterface.getFirstUser(simpleUserModel.getUsername());
		
		if (userPre == null) {
			throw new InvalidParameterException("User not found!");
		}
		
    	try {	
			UserAuthenticationFile.User newUser = new UserAuthenticationFile.User();
			newUser.setIdentifier(simpleUserModel.getUsername());
			newUser.getGroupList().addAll(Arrays.asList(simpleUserModel.getGroupList()));
			newUser.getGroupListIN().addAll(Arrays.asList(simpleUserModel.getGroupListIN()));
			newUser.getGroupListOUT().addAll(Arrays.asList(simpleUserModel.getGroupListOUT()));
			newUser.setPassword(userPre.getPassword());
			newUser.setPasswordHashed(true);
			newUser.setRole(userPre.getRole());
			newUser.setFingerprint(userPre.getFingerprint());
			
			myFileUserManagementInterface.addOrUpdateUser(newUser, true, userPre); 
			
	    	logger.info("Updated groups for user {}", simpleUserModel.getUsername());
	    	
		} catch (Exception e) {
	    	logger.error("Error in updateGroupsForUser: {}", simpleUserModel.getUsername(), e);
			throw new RuntimeException(e);
		} 
    }
    
//  f.	Ability to delete user
    @Override
    @RequestMapping(value = "/delete-user/{username}", method = RequestMethod.DELETE)
    public void deleteUser(@PathVariable String username) {

    	FileAuthenticatorControl control;
    	try {
			control = myFileUserManagementInterface.removeUser(username);
		} catch (Exception e) {
	    	logger.error("Error in deleteUser: {}", username, e);
			throw new RuntimeException(e);
		} 
		if (control!= null && control.getFileUser() == null) {
			throw new InvalidParameterException("User not found!");
		}
    	logger.info("Deleted user: {}", username);
    }
    
//	3. Ability to view existing groups
    @Override
    @RequestMapping(value = "/list-groupnames", method = RequestMethod.GET)
    public Set<GroupNameModel> getAllGroupNames() {
    	
    	Set<GroupNameModel> re = new HashSet<>();
    	
        Set<String> groupNames = myFileUserManagementInterface.getGroupNames();
        groupNames.forEach(groupName ->{
    		re.add(new GroupNameModel(groupName));
        });

        return re;
    }
    
//	a. Showing which users are in each, and if the group is IN, OUT, or both for them  
    @Override
    @RequestMapping(value = "/users-in-group/{group}", method = RequestMethod.GET)
    public SimpleGroupWithUsersModel getUsersInGroup(@PathVariable("group") String groupName) {
    	
    	return myFileUserManagementInterface.getUsersInGroup(groupName);

    }
    
}
