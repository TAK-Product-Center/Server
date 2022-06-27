

package com.bbn.marti.remote;

import java.util.Collection;

/**
 * Interface for management of alarms / repeating messages
 */
public interface RepeaterManager {
	void addMessage(String msg, String repeatType);
	Collection<Repeatable> getRepeatableMessages();
	Integer getRepeatableMessageCount();
	Integer getPeriodMillis();
	void setPeriodMillis(Integer periodMillis);
	boolean removeMessage(String uid, boolean generateMessage);
}
