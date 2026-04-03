package com.bbn.cot.filter;

import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import tak.server.cot.CotEventContainer;


public class DisableBroadcastMapItemFilter implements CotFilter {

    private static final Logger logger = LoggerFactory.getLogger(DisableBroadcastMapItemFilter.class);

    public DisableBroadcastMapItemFilter() {
    }

    @Override
    public CotEventContainer filter(CotEventContainer c) {
        try {
            // drop any messages that have the archive detail but are missing the dest detail
            List<Node> archive = c.getDocument().selectNodes("/event/detail/archive");
            List<Node> dest = c.getDocument().selectNodes("/event/detail/marti/dest");
            if (archive != null && !archive.isEmpty() && (dest == null || dest.isEmpty())) {
                return null;
            }
        } catch (Exception e) {
            logger.error("exception enforcing DisableBroadcastMapItemFilter", e);
        }

        return c;
    }
}
