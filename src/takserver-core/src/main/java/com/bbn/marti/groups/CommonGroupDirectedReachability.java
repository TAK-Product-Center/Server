

package com.bbn.marti.groups;

import java.io.Serializable;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.Node;
import com.bbn.marti.remote.groups.Reachability;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.SpringContextBeanForApi;

import com.bbn.marti.remote.config.CoreConfigFacade;

/*
 *
 *  Determine common group membership by non-recursive traversal.
 *
 */
public class CommonGroupDirectedReachability implements Reachability<User>, Serializable {

	private static final long serialVersionUID = -4520635157040006791L;
	
	private static final Logger logger = LoggerFactory.getLogger(CommonGroupDirectedReachability.class);

	private final GroupManager groupManager;

	private static CommonGroupDirectedReachability instance;

	public static CommonGroupDirectedReachability getInstance() {
		if (instance == null) {
			synchronized (CommonGroupDirectedReachability.class) {
				if (instance == null) {
					instance = new CommonGroupDirectedReachability(SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class));
				}
			}
		}

		return instance;
	}

	public CommonGroupDirectedReachability(GroupManager groupManager) {
		this.groupManager = groupManager;
	}

	@Override
	public boolean isReachable(User src, User dest) {

		if (logger.isTraceEnabled()) {
			logger.trace("src: " + src + " dest: " + dest);
		}

		if (src == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("null src user - not allowing delivery");
			}
			return false;
		}

		if (dest == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("null dest user");
			}
			return false;
		}

		// don't allow reachability between federates
		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation() != null) {
			if (src.getClass().equals(FederateUser.class) && dest.getClass().equals(FederateUser.class)) {
				if (logger.isDebugEnabled()) {
					logger.debug("federate " + src + " can't reach federate" + dest);
				}
				return false;
			}
		}

		// look for the first group (breadth-first search) that src and dest have in common
		NavigableSet<Group> groups = null;
		groups = groupManager.getGroups(src);

		if (logger.isTraceEnabled()) {
			logger.trace("src groups: " + groups);
		}

		// null group here suggests that one of the users was concurrently deleted
		if (groups == null) {
			return false;
		}

		return isReachable(groups, dest);
	}

	@Override
	public boolean isReachable(NavigableSet<Group> groups, User dest) {

		if (groups == null || groups.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("null or empty srcGroups");
			}
			return false;
		}

		if (dest == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("null dest user");
			}
			return false;
		}

		for (Group inGroup : groups) {

			// filter out groups that are not IN groups
			if (inGroup.getDirection().equals(Direction.IN)) {
				// get the corresponding OUT group (expected average case O(1) lookup)
				Group outGroup = groupManager.getGroup(inGroup.getName(), Direction.OUT);

				// If there is no corresponding OUT group registered, keep looking
				if (outGroup == null) {
					continue;
				}

				if (logger.isTraceEnabled()) {
					logger.trace("for src inGroup " + inGroup + " checking outGroup " + outGroup + " having members " + outGroup.getNeighbors() + " for dest user " + dest);
				}

				if (outGroup.getNeighbors().contains(dest)) {
					if (logger.isTraceEnabled()) {
						logger.trace("outGroup " + outGroup + " contains dest " +  dest);
					}
					return true;
				} else {
					if (logger.isTraceEnabled()) {
						logger.trace("no match");
					}
				}
			}
		}

		// they have no groups in common
		return false;
	}

	@Override
	public Set<User> getAllReachableFrom(User src) {
		
		Set<User> users = new HashSet<>();
		
		if (src == null) {
			throw new IllegalArgumentException("null src user");
		}

		boolean federateUser = false;
		if (src instanceof FederateUser) {
			federateUser = true;
		}

		final boolean fedUser = federateUser;
		// look for the first group (breadth-first search) that src and dest have in common
		NavigableSet<Group> groups = groupManager.getGroups(src);

		// null group here suggests that one of the users was concurrently deleted
		if (groups == null) {
			return users;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("src user " + src + " groups: " + groups);
		}

		for (Group group : groups) {
			try {
				// only consider OUT groups that the src user is a member of
				if (group.getDirection().equals(Direction.OUT)) {

					// get the corresponding IN group (expected average case O(1) lookup)
					Group inGroup = groupManager.getGroup(group.getName(), Direction.IN);

					// If there is no corresponding IN group registered, keep looking
					if (inGroup == null) {
						continue;
					}

					for (Node node : inGroup.getNeighbors()) {
						if (node.isLeaf()) {
							User user = (User) node;
							if (fedUser == false || (fedUser && !(user instanceof FederateUser))) {
								if (!user.equals(src)) {
									users.add(user);
								}
							}
						} else {
							if (logger.isTraceEnabled()) {
								logger.trace("ignoring non-User child inside Group");
							}
						}

					}
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception comparing in - out groups", e);
				}
			}
		}

		return users;
	}
}
