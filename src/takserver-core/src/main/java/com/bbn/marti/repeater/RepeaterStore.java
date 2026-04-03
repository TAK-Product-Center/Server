package com.bbn.marti.repeater;

import java.util.List;

import tak.server.cot.CotEventContainer;


public interface RepeaterStore {
	RepeatableContainer getRepeatedMessage(String uid);
	void addRepeatedMessage(String uid, RepeatableContainer repeatableContainer);
	boolean removeRepeatedMessage(String uid);
	int getRepeatedMessageCount();
	List<RepeatableContainer> getRepeatedMessages();
	void addCancelledMessage(String uid, CotEventContainer cotEventContainer);
	boolean removeCancelledMessage(String uid);
	List<CotEventContainer> getCancelledMessages();
}
