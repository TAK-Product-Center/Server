

package com.bbn.marti.groups;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.Reachability;
import com.bbn.marti.remote.groups.User;

/*
 * 
 *  Don't allow any users to reach each other.
 * 
 */
public class AllowNoneReachability implements Reachability<User> {
    
    Logger logger = LoggerFactory.getLogger(AllowNoneReachability.class);

    @Override
    public boolean isReachable(User src, User dest) {
        logger.trace("allowing all reachability - src: " + src + " dest: " + dest);
        
        return false;
    }

    @Override
    public NavigableSet<User> getAllReachableFrom(User src) {
       return new ConcurrentSkipListSet<>();
    }

    @Override
    public boolean isReachable(NavigableSet<Group> srcGroups, User dest) {
     
        return false;
    }
}