package com.bbn.marti.remote.socket;

import java.io.Serializable;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import com.bbn.marti.remote.groups.Group;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/*
 * Supertype for socket messages, so that they can use the same virtual endpoint
 * 
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "messageType")
@JsonSubTypes({ 
    @Type(value = SituationAwarenessMessage.class, name = "SituationAwarenessMessage"), 
    @Type(value = ChatMessage.class, name = "ChatMessage"),
    @Type(value = ControlMessage.class, name = "ControlMessage"),
    @Type(value = MissionChange.class, name = "MissionChange"),
        @Type(value = MissionPackageMessage.class, name = "MissionPackageMessage")
})
public abstract class TakMessage implements Serializable { 
	
	private static final String className = TakMessage.class.getSimpleName();
	
	public static String getClassName() {
		return className;
	}
   
    private static final long serialVersionUID = 8712634871L;
    
    protected final NavigableSet<String> missions = new ConcurrentSkipListSet<>();
    
    /*
     * Used when delivering to socket only 
     */
    public NavigableSet<String> getMissions() {
        return missions;
    }
    
    protected final NavigableSet<String> uids = new ConcurrentSkipListSet<>();
    
    /*
     * Used when delivering to socket only 
     */
    public NavigableSet<String> getUids() {
        return uids;
    }
    
    protected final NavigableSet<Group> groups = new ConcurrentSkipListSet<>();
    
    public NavigableSet<Group> getGroups() {
        return groups;
    }
    
    protected Set<String> addresses = new HashSet<>();

    public Set<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<String> addresses) {
        this.addresses = addresses;
    }
    
    protected Set<String> topics = new ConcurrentSkipListSet<>();

	public Set<String> getTopics() {
		return topics;
	}

	public void setTopics(Set<String> topics) {
		this.topics = topics;
	}
	
	@JsonIgnore
	public void jsonClear() {
		// clear field that shouldn't be included in external messages to webtak clients. These fields are used internally, so JsonIgnore can't be used.
	    getMissions().clear();
	    getUids().clear();
	    getGroups().clear();
	}
}
