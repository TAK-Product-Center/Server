

package com.bbn.marti.remote;

import tak.server.cot.CotEventContainer;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for management of alarms / repeating messages
 */
public interface RepeaterManager {
	void addMessage(CotEventContainer msg, String repeatableType);
	boolean isRepeating(String uid);
	Collection<CotEventContainer> getMessages();
	Collection<Repeatable> getRepeatableMessages();
	Collection<CotEventContainer> getCancelledMessages();
	Integer getRepeatableMessageCount();
	Integer getPeriodMillis();
	void setPeriodMillis(Integer periodMillis);
	boolean removeRepeatedMessage(String uid, boolean generateMessage);
	boolean removeCancelledMessage(String uid);
}
