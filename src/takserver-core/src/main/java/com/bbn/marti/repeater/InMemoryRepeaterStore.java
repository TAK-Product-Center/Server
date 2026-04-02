
package com.bbn.marti.repeater;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;


/*
 * In-memory data structures for managing repeaters.*
 */
public class InMemoryRepeaterStore implements RepeaterStore {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryRepeaterStore.class);

    private final Map<String, RepeatableContainer> repeatedMessages = new ConcurrentHashMap<>();
    private final Map<String, CotEventContainer> cancelledMessages = new ConcurrentHashMap<>();

    @Override
    public RepeatableContainer getRepeatedMessage(String uid) {
        return (RepeatableContainer) repeatedMessages.get(uid);
    }

    @Override
    public void addRepeatedMessage(String uid, RepeatableContainer repeatableContainer) {
        try {
//            repeatableContainer.getCotEventContainer().getContext().keySet().removeIf(
//                    key -> !key.equals(Constants.GROUPS_KEY));
            repeatedMessages.put(uid, repeatableContainer);
        } catch (Exception e) {
            logger.error("exception in addRepeatedMessage", e);
        }
    }

    @Override
    public boolean removeRepeatedMessage(String uid) {
        return repeatedMessages.remove(uid) != null;
    }

    @Override
    public int getRepeatedMessageCount() {
        return repeatedMessages.size();
    }

    @Override
    public List<RepeatableContainer> getRepeatedMessages() {
        return new ArrayList<>(repeatedMessages.values());
    }

    @Override
    public void addCancelledMessage(String uid, CotEventContainer cotEventContainer) {
        cancelledMessages.put(uid, cotEventContainer);
    }

    @Override
    public boolean removeCancelledMessage(String uid) {
        return cancelledMessages.remove(uid) != null;
    }

    @Override
    public List<CotEventContainer> getCancelledMessages() {
        return new ArrayList<>(cancelledMessages.values());
    }
}
