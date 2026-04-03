package com.bbn.marti.repeater;


import org.apache.ignite.IgniteCache;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;


/*
 * Ignite data structures for managing repeaters.*
 */
public class IgniteRepeaterStore implements RepeaterStore {

    private static final Logger logger = LoggerFactory.getLogger(IgniteRepeaterStore.class);

    private synchronized IgniteCache<Object, Object> getRepeatedMessagesCache() {
        IgniteCache<Object, Object> repeatedMessagesCache = IgniteHolder.getInstance().getIgnite()
                .cache(Constants.REPEATED_MESSAGES_CACHE);
        if (repeatedMessagesCache != null) {
            return repeatedMessagesCache;
        }
        return IgniteHolder.getInstance().getIgnite()
                .getOrCreateCache(Constants.REPEATED_MESSAGES_CACHE);
    }

    private synchronized IgniteCache<Object, Object> getCancelledMessagesCache() {
        IgniteCache<Object, Object> cancelledMessagesCache = IgniteHolder.getInstance().getIgnite()
                .cache(Constants.CANCELLED_MESSAGES_CACHE);
        if (cancelledMessagesCache != null) {
            return cancelledMessagesCache;
        }
        return IgniteHolder.getInstance().getIgnite()
                .getOrCreateCache(Constants.CANCELLED_MESSAGES_CACHE);
    }

    @Override
    public RepeatableContainer getRepeatedMessage(String uid) {
        return (RepeatableContainer) getRepeatedMessagesCache().get(uid);
    }

    @Override
    public void addRepeatedMessage(String uid, RepeatableContainer repeatableContainer) {
        try {
            repeatableContainer.getCotEventContainer().getContext().keySet().removeIf(
                    key -> !key.equals(Constants.GROUPS_KEY));
            getRepeatedMessagesCache().put(uid, repeatableContainer);
        } catch (Exception e) {
            logger.error("exception in addRepeatedMessage", e);
        }
    }

    @Override
    public boolean removeRepeatedMessage(String uid) {
        return getRepeatedMessagesCache().remove(uid);
    }

    @Override
    public int getRepeatedMessageCount() {
        return getRepeatedMessagesCache().size();
    }

    @Override
    public List<RepeatableContainer> getRepeatedMessages() {
        List<RepeatableContainer> values = new ArrayList<>();
        getRepeatedMessagesCache().forEach(cacheEntry ->
                values.add((RepeatableContainer) cacheEntry.getValue()));
        return values;
    }

    @Override
    public void addCancelledMessage(String uid, CotEventContainer cotEventContainer) {
        getCancelledMessagesCache().put(uid, cotEventContainer);
    }

    @Override
    public boolean removeCancelledMessage(String uid) {
        return getCancelledMessagesCache().remove(uid);
    }

    @Override
    public List<CotEventContainer> getCancelledMessages() {
        List<CotEventContainer> values = new ArrayList<>();
        getCancelledMessagesCache().forEach(cacheEntry ->
                values.add((CotEventContainer) cacheEntry.getValue()));
        return values;
    }
}
