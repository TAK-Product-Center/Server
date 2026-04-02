package com.bbn.cot.filter;

import org.dom4j.Element;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.UUID;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.sync.service.MissionService;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;


public class StrictUidMissionMemebershipFilter implements CotFilter {

    private static final Logger logger = LoggerFactory.getLogger(StrictUidMissionMemebershipFilter.class);

    private SubscriptionManager subMgr;
    private MissionService missionService;

    public StrictUidMissionMemebershipFilter(SubscriptionManager subMgr, MissionService missionService) {
        this.subMgr = subMgr;
        this.missionService = missionService;
    }

    @Override
    public CotEventContainer filter(CotEventContainer c) {
        try {
            // is this cot event contained in a mission?
            Collection<UUID> missionGuids = subMgr.getMissionsForContentUid(c.getUid());
            if (missionGuids != null && !missionGuids.isEmpty()) {

                //
                // ensure that the event is being resubmitted to the same mission
                //

                UUID missionGuid = null;
                List<Node> missionNode = c.getDocument().selectNodes(
                        "/event/detail/marti/dest[@mission]");
                if (missionNode != null && !missionNode.isEmpty()) {
                    String missionName =
                            ((Element) missionNode.get(0)).attributeValue("mission");
                    NavigableSet<Group> groups = (NavigableSet<Group>)c.getContext(Constants.GROUPS_KEY);
                    String groupVector = RemoteUtil.getInstance().bitVectorToString(
                            RemoteUtil.getInstance().getBitVectorForGroups(groups));
                    missionGuid = missionService.getMissionGuidByNameCheckGroups(missionName, groupVector);
                } else {
                    List<Node> missionGuidNode = c.getDocument().selectNodes(
                            "/event/detail/marti/dest[@mission-guid]");
                    if (missionGuidNode != null && !missionGuidNode.isEmpty()) {
                        missionGuid = UUID.fromString(
                                ((Element) missionGuidNode.get(0)).attributeValue("mission-guid"));
                    }
                }

                if (missionGuid == null) {
                    logger.error("Illegal attempt to send mission event outside of a mission context {}",
                            c.getUid());
                    return null;
                } else if (!missionGuids.contains(missionGuid)) {
                    logger.error("Illegal attempt to send mission event to a different mission {} {}",
                            c.getUid(), missionGuid);
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("exception enforcing strictUidMissionMembership", e);
        }

        return c;
    }
}
