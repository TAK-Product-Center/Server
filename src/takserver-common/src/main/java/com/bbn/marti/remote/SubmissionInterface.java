

package com.bbn.marti.remote;

import java.util.Date;
import java.util.List;
import java.util.NavigableSet;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;

import tak.server.cot.CotEventContainer;

public interface SubmissionInterface {

	// Submit CoT message to specified groups
    public boolean submitCot(String cotMessage, NavigableSet<Group> groups);
    
    // Submit CoT message to specified groups
    public boolean submitCot(String cotMessage, NavigableSet<Group> groups, boolean federate);

    // Submit CoT message to specified groups, on behalf of the User
    public boolean submitCot(String cotMessage, NavigableSet<Group> groups, boolean federate, User user);

    public boolean submitMissionPackageCotAtTime(String cotMessage, String missionName, Date timestamp, NavigableSet<Group> groups, String clientUid);

    // Submit explicitly addressed CoT message to intersection of specified groups, callsigns and uids.
    // resbumission indicates the event is being resent from this server, trims flow tags and turns off archiving
    public boolean submitCot(CotEventContainer cotMessage, List<String> uids, List<String> callsigns, NavigableSet<Group> groups, boolean federate, boolean resubmission);
    
    public boolean submitCot(String cotMessage, List<String> uids, List<String> callsigns, NavigableSet<Group> groups, boolean federate, boolean resubmission);
}
