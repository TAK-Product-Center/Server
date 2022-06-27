package com.bbn.useraccountmanagement;

import java.util.List;
import java.util.Set;

import com.bbn.marti.remote.groups.SimpleGroupWithUsersModel;
import com.bbn.useraccountmanagement.model.GroupNameModel;
import com.bbn.useraccountmanagement.model.NewUserModel;
import com.bbn.useraccountmanagement.model.SimpleUserGroupModel;
import com.bbn.useraccountmanagement.model.UserGenerationInBulkModel;
import com.bbn.useraccountmanagement.model.UserPasswordModel;
import com.bbn.useraccountmanagement.model.UsernameModel;


public interface FileUserAccountManagementApiInterface {
	

    public void createOrUpdateFileUser(NewUserModel newUserModel);

    public List<UserPasswordModel> createFileUsersInBulk(UserGenerationInBulkModel userGenerationInBulkModel);
    
    public Set<UsernameModel> getAllUsers();
      
    public SimpleUserGroupModel getGroupsForUsers(String username);
    
    public void changeUserPassword(UserPasswordModel userPasswordModel);
    
    public void updateGroupsForUser(SimpleUserGroupModel simpleUserModel);
    
    public void deleteUser(String username);
    
    public Set<GroupNameModel> getAllGroupNames();
    
    public SimpleGroupWithUsersModel getUsersInGroup(String groupName);
    
 
}
