

package com.bbn.marti.remote.groups;

import java.util.NavigableSet;
import java.util.Set;


public interface Reachability<T> {

    /*
     * Find out if two objects are connected.
     * 
     */
    boolean isReachable(T src, T dest);
    
    Set<User> getAllReachableFrom(T src);

    boolean isReachable(NavigableSet<Group> srcGroups, User dest);
}
