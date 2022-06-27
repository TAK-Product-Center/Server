package com.bbn.marti.remote.groups;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * This service is generic and can be extended later to encompass both file and LDAP operations
 *
 */
public interface UserAccountManagementService <U>{
	
    @NotNull Set<String> getUserIdentifierList();
    
    boolean userExists(@NotNull String userIdentifier);
    
    U getUser(@NotNull String userIdentifier);
    
    boolean addUser(@NotNull U userObject);

    boolean updateUser(@NotNull U userObject);

    boolean removeUser(@NotNull String userIdentifier);
    
    @NotNull
	Set<String> getGroupNames();
            
    SimpleGroupWithUsersModel getUsersInGroup(String groupName);
    
}
