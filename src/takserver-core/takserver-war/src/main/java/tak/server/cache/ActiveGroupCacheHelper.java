package tak.server.cache;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import tak.server.Constants;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class ActiveGroupCacheHelper {

    private static final Logger logger = LoggerFactory.getLogger(ActiveGroupCacheHelper.class);

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private Ignite ignite;

    @Autowired
    private CoreConfig config;

    @Autowired
    private DataSource ds;


    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            // restore active group cache from the database
            if (config.getRemoteConfiguration().getAuth().isX509UseGroupCache()) {
                getActiveGroupsCache();
            }
        } catch (Exception e) {
            logger.error("exception initializing active_group_cache");
        }
    }

    public void setActiveGroupsForUser(String username, List<Group> groups) {

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
        // retrieve the active groups from the cache

        IgniteCache<Object, Object> activeGroupsCache = getActiveGroupsCache();
        if (activeGroupsCache == null) {
            throw new TakException("Unable to get activeGroupsCache");
        }

        return (List<Group>) activeGroupsCache.get(username);
    }

    private IgniteCache<Object, Object> getActiveGroupsCache() {

        IgniteCache<Object, Object> activeGroupCache = ignite.cache(Constants.ACTIVE_GROUPS_CACHE);
        if (activeGroupCache != null) {
            return activeGroupCache;
        }

        Map<String, List<Group>> activeGroups = loadActiveGroups();
        if (activeGroups != null) {
            activeGroupCache = ignite.createCache(Constants.ACTIVE_GROUPS_CACHE);
            Iterator it = activeGroups.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry userCache = (Map.Entry) it.next();
                activeGroupCache.put((String) userCache.getKey(), (List<Group>) userCache.getValue());
            }
        } else {
            logger.error("loadActiveGroups failed!");
        }

        return activeGroupCache;
    }

    public void saveActiveGroupsForUser(String username, List<Group> groups) {
        try {
            try (Connection connection = ds.getConnection()) {

                try (PreparedStatement statement = connection.prepareStatement(
                        "delete from active_group_cache where username = ?")) {
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

    public Map<String, List<Group>> loadActiveGroups() {
        try {
            ConcurrentHashMap<String, List<Group>> results = new ConcurrentHashMap<>();
            try (Connection connection = ds.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "select username, groupname, direction, enabled from active_group_cache ")) {
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
}
