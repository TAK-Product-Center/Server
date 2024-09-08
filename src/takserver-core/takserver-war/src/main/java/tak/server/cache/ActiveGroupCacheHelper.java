package tak.server.cache;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.google.common.collect.Sets;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import tak.server.Constants;

import javax.cache.CacheException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;


public class ActiveGroupCacheHelper {

    private static final Logger logger = LoggerFactory.getLogger(ActiveGroupCacheHelper.class);

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private Ignite ignite;

    @Autowired
    private DataSource ds;


    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            // restore active group cache from the database
            if (CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().isX509UseGroupCache()) {
                getActiveGroupsCache();
            }
        } catch (Exception e) {
            logger.error("exception initializing active_group_cache", e);
        }
    }

    public void setActiveGroupsForUser(String username, List<Group> groups) {

        username = username.toLowerCase();

        IgniteCache<Object, Object> activeGroupsCache = getActiveGroupsCache();
        if (activeGroupsCache == null) {
            throw new TakException("Unable to get activeGroupsCache");
        }

        // add the active groups to the cache
        activeGroupsCache.put(username, groups);

        // save the active groups to the database
        saveActiveGroupsForUser(username, groups);
    }

    public List<Group> getActiveGroupsForUser(String username) {

        username = username.toLowerCase();

        // retrieve the active groups from the cache
        IgniteCache<Object, Object> activeGroupsCache = getActiveGroupsCache();
        if (activeGroupsCache == null) {
            throw new TakException("Unable to get activeGroupsCache");
        }

        return (List<Group>) activeGroupsCache.get(username);
    }

    private synchronized IgniteCache<Object, Object> getActiveGroupsCache() {

        IgniteCache<Object, Object> activeGroupCache = ignite.cache(Constants.ACTIVE_GROUPS_CACHE);
        if (activeGroupCache != null) {
            return activeGroupCache;
        }

        Date start = new Date();
        logger.info("Populating the activeGroupCache");

        Map<String, List<Group>> activeGroups = loadActiveGroups();
        if (activeGroups != null) {
            try {
                activeGroupCache = ignite.getOrCreateCache(Constants.ACTIVE_GROUPS_CACHE);
                Iterator it = activeGroups.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry userCache = (Map.Entry) it.next();
                    activeGroupCache.put((String) userCache.getKey(), (List<Group>) userCache.getValue());
                }
            } catch (Exception e) {
                logger.error("exception in getActiveGroupsCache", e);
            }
        } else {
            logger.error("loadActiveGroups failed!");
        }

        logger.info("activeGroupCache cache warmed - took " + ((new Date().getTime() - start.getTime()) / 1000) + " seconds");

        return activeGroupCache;
    }

    private void saveActiveGroupsForUser(String username, List<Group> groups) {
        try {
            try (Connection connection = ds.getConnection()) {

                try (PreparedStatement statement = connection.prepareStatement(
                        "delete from active_group_cache where lower(username) = ?")) {
                    statement.setString(1, username);
                    statement.execute();
                } catch (SQLException e) {
                    logger.error("exception clearing active groups!", e);
                }

                for (Group group : groups) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "insert into active_group_cache ( username, groupname, direction, enabled ) " +
                                    " values ( ?, ?, ?, ? )")) {
                        statement.setString(1, username);
                        statement.setString(2, group.getName());
                        statement.setString(3, group.getDirection().name());
                        statement.setBoolean(4, group.getActive());
                        statement.execute();
                    } catch (SQLException e) {
                        logger.error("exception setting active groups!", e);
                    }
                }

            } catch (SQLException e) {
                logger.error("SQLException in saveActiveGroupsForUser!", e);
            }
        } catch (Exception e) {
            logger.error("exception in saveActiveGroupsForUser!", e);
        }
    }

    private Map<String, List<Group>> loadActiveGroups() {
        try {
            ConcurrentHashMap<String, List<Group>> results = new ConcurrentHashMap<>();
            try (Connection connection = ds.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "select distinct lower(username), groupname, direction, enabled from active_group_cache ")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String username = rs.getString(1);
                        String groupname = rs.getString(2);
                        String direction = rs.getString(3);
                        boolean enabled = rs.getBoolean(4);

                        List<Group> groups = results.get(username);
                        if (groups == null) {
                            groups = new CopyOnWriteArrayList<>();
                            results.put(username, groups);
                        }

                        Group tmp = new Group(groupname, direction == null ? Direction.IN : Direction.valueOf(direction));
                        tmp = groupManager.hydrateGroup(tmp);

                        Group group = new Group(tmp.getName(), tmp.getDirection());
                        group.setActive(enabled);
                        group.setBitpos(tmp.getBitpos());

                        groups.add(group);
                    }
                } catch (SQLException e) {
                    logger.error("SQLException in executeQuery!", e);
                    return null;
                }
            } catch (SQLException e) {
                logger.error("SQLException in loadActiveGroups!", e);
                return null;
            }

            return results;
        } catch (Exception e) {
            logger.error("exception in loadActiveGroups!", e);
            return null;
        }
    }

    public boolean assignGroupsCheckCache(Set<Group> groups, User user, String username) {

        username = username.toLowerCase();

        // check to see if we have any cache entries for the current username
        List<Group> activeGroups = getActiveGroupsForUser(username);
        if (activeGroups == null) {
            activeGroups = new LinkedList<>();
        }

        // recreate as a full LinkedList to support delete (cant delete on list coming from cache)
        activeGroups = new LinkedList<>(activeGroups);

        // keep track of this user even if there are no groups
        groupManager.addUser(user);

        // find groups that need to get removed from the cache
        Set<Group> cacheGroups = new ConcurrentSkipListSet<>(activeGroups);
        Set<Group> removals = Sets.difference(cacheGroups, groups);
        activeGroups.removeAll(removals);

        // find groups that need to get added to the cache
        Set<Group> adds = Sets.difference(groups, cacheGroups);
        activeGroups.addAll(adds);

        for (Group group : adds) {
            group.setActive(CoreConfigFacade.getInstance().getRemoteConfiguration()
                    .getAuth().isX509UseGroupCacheDefaultActive());
        }

        // if we only have one group, make sure its active
        if (activeGroups.size() == 1) {
            activeGroups.get(0).setActive(true);
            // need to check case when size=2 for IN/OUT groups with same name
        } else if (activeGroups.size() == 2 && activeGroups.get(0).getName().equals(activeGroups.get(1).getName())) {
            activeGroups.get(0).setActive(true);
            activeGroups.get(1).setActive(true);
        }

        // update the cache if required
        boolean updated = adds.size() > 0 || removals.size() > 0;
        if (updated) {
            setActiveGroupsForUser(username, activeGroups);
        }

        // remove any inactive cache entries prior to push the groups to the user
        Iterator<Group> activeGroupIter = activeGroups.iterator();
        while (activeGroupIter.hasNext()) {
            Group activeGroup = activeGroupIter.next();
            if (!activeGroup.getActive()) {
                activeGroupIter.remove();
            }
        }

        // do the group updates based on this set of groups
        groupManager.updateGroups(user, new ConcurrentSkipListSet<>(activeGroups));

        return updated;
    }
}
