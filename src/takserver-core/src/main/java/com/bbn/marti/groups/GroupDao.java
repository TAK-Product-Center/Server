package com.bbn.marti.groups;

import java.util.NavigableSet;

import org.jetbrains.annotations.NotNull;

import com.bbn.marti.remote.groups.Group;

/*
 * Persistence layer for Group objects
 * 
 * This is intended to be used with in conjunction with a caching layer. So this class doesn't try
 * to cache anything.
 * 
 * The public methods are intended to be extracted into an interface if we ever need one. IE, if there is ever more than one implementation.
 * 
 * 
 */
public interface GroupDao {
    
	/*
     * Save a group (synchronous). If the group exists, don't try to save it, just return the saved group. This populates the bit position field in the Group object.
     *  
     */
    Group save(final Group group); 
    
    /*
     * Load a group by name
     */
    Group load(String groupName);
    
    /*
     * Return all saved groups
     * 
     */
    NavigableSet<Group> fetchAll();
    
    /*
     * Delete a group by name
     */
    void deleteGroup(@NotNull String groupName);
    
}
