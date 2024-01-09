package com.bbn.marti.groups;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.Group.Type;
import com.google.common.base.Strings;

/*
 * Interface for group persistence
 * 
 * 
 */
public class PersistentGroupDao implements GroupDao {
    
    private static final Logger logger = LoggerFactory.getLogger(PersistentGroupDao.class);
    
    private DataSourceTransactionManager tm;
    
    @Autowired
    private DataSource ds;
    
	@PostConstruct
    private void init() {
    	tm = new DataSourceTransactionManager(ds);
    }
    
    /*
     * Save a group (synchronous). If the group exists, don't try to save it, just return the saved group. This populates the bit position field in the Group object.
     *  
     */
    @Override
    public Group save(final @NotNull Group group) {
        
        // TODO: input validation
        
        if (group == null || Strings.isNullOrEmpty(group.getName())) {
            throw new IllegalArgumentException("null group, or empty group name");
        }
        
        try {
        	// Do all of this in a transaction. Mutual exclusion provided at the db level by the lock on the sequence table.
            return new TransactionTemplate(tm).execute(new TransactionCallback<Group>() {
                @Override
                public Group doInTransaction(TransactionStatus status) {
                    try {
                        JdbcTemplate template = new JdbcTemplate(ds);

                        // acquire a table lock on the sequence table, and block concurrent saves.
                        template.execute("lock table group_bitpos_sequence;");

                        Integer bitpos = null;
                        
                        Group existing = load(group.getName(), template);

                        if (existing == null) {
                            // group does not exist in db - save it

                            // allocate a bit position
                            bitpos = template.query("update group_bitpos_sequence set bitpos = bitpos + 1 returning bitpos + 1",
                                    new ResultSetExtractor<Integer>() { @Override public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                                        // group exists, return it
                                        if (rs.next()) {
                                            return rs.getInt(1);
                                        } else {
                                            throw new IllegalStateException("unable to increment bit position");
                                        }}});

                            logger.debug("allocated new bit position: " + bitpos);

                            template.update("insert into groups (name, bitpos, create_ts, type) values (?, ?, now(), ?)", // SQL
                                    new Object[] {group.getName(), bitpos, group.getType().ordinal()}); // SQL params
                        } else {
                            // group does exist in db - so update fields from the saved object
                            group.update(existing);

                            logger.debug("group exists: not saving " + existing);
                            return existing;
                        }

                        if (bitpos != null) {
                            group.setBitpos(bitpos);
                        }
                    } catch (Exception e) {

                        logger.debug("exception persisting group " + group);

                        logger.debug("exception saving group " + group + " " + e.getMessage(), e);
                    } 
                    
                    return group;
                }
            });
		} catch (CannotCreateTransactionException e) {
			logger.info("Could not save group. " + e.getMessage());
		}
        
        return group;
    }
    
    /*
     * Load a group by name
     */
    @Override
    public Group load(@NotNull String groupName) {
        
        if (Strings.isNullOrEmpty(groupName)) {
            throw new IllegalArgumentException("empty group name");
        }
        
        // Load a group without locking
        return load(groupName, new JdbcTemplate(ds));
    }
    
    private Group load(@NotNull String groupName, @NotNull JdbcTemplate template) {
        if (Strings.isNullOrEmpty(groupName) || template == null) {
            throw new IllegalArgumentException("empty group name or JdbcTemplate");
        }

        // load the group from the database
        return template.query("select name, bitpos, create_ts, type from groups where name = ?",
                new Object[]{groupName}, // statement param - group name
                new ResultSetExtractor<Group>() { @Override public Group extractData(ResultSet rs) throws SQLException, DataAccessException {
                    // group exists, return it
                    if (rs.next()) {
                        Group group = new Group();

                        group.setName(rs.getString(1));
                        group.setBitpos(rs.getInt(2));
                        group.setCreated(rs.getDate(3));
                        group.setType(rs.getInt(4) == 0 ? Type.LDAP : Type.SYSTEM);
                        group.setDirection(Direction.OUT);

                        return group;
                    } else {
                        return null;  
                    }}});    
    }
    
    
    /*
     * Return all saved groups
     * 
     */
    @Override
    public NavigableSet<Group> fetchAll() {

        JdbcTemplate template = new JdbcTemplate(ds);

        // load the group from the database
        return template.query("select name, bitpos, create_ts, type from groups",
                new ResultSetExtractor<NavigableSet<Group>>() { @Override public NavigableSet<Group> extractData(ResultSet rs) throws SQLException, DataAccessException {

                    NavigableSet<Group> groups = new ConcurrentSkipListSet<>();

                    // group exists, return it
                    while (rs.next()) {
                        Group group = new Group();

                        group.setName(rs.getString(1));
                        group.setBitpos(rs.getInt(2));
                        group.setCreated(rs.getDate(3));
                        group.setType(rs.getInt(4) == 0 ? Type.LDAP : Type.SYSTEM);
                        group.setDirection(Direction.OUT); // No direction is saved in the table

                        if (group.getName() != null && group.getDirection() != null) {
                            groups.add(group);
                        }
                    }

                    return groups;
                }
        }
                );    
    }
    
    /*
     * Delete a group by name
     */
    @Override
    public void deleteGroup(@NotNull String groupName) {
        
        if (Strings.isNullOrEmpty(groupName)) {
            throw new IllegalArgumentException("empty group name");
        }
        
        JdbcTemplate template = new JdbcTemplate();
        
        int count = template.update("delete from groups where name = ?", // SQL
                new Object[] {groupName}); // SQL params
        
        if (count == 0) {
            throw new NotFoundException("group " + groupName + " not found - nothing to delete");
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug(count + " records deleted for group name " + groupName);
        }
    }
}
