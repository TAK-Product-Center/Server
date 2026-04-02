

package com.bbn.marti.repeater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import org.apache.ignite.services.ServiceContext;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.cot.CotParserCreator;
import com.bbn.marti.remote.Repeatable;
import com.bbn.marti.remote.RepeaterManager;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;

import tak.server.Constants;
import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;


/**
 * This class acts as a manager for information related to the repeater service. It is accessible remotely
 * via ignite. It is mostly an information holder, the RepeaterService class makes use of this data to provide
 * the repeater capabilities.
 */
public class DistributedRepeaterManager implements RepeaterManager, org.apache.ignite.services.Service {

	private static final long serialVersionUID = -1008820760168943632L;

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	private transient ThreadLocal<CotParser> cotParser = new ThreadLocal<>();

	private static final Logger logger = LoggerFactory.getLogger(DistributedRepeaterManager.class);

	public DistributedRepeaterManager() {
		super();
	}

	public boolean isRepeating(String uid) {
		return repeaterStore().getRepeatedMessage(uid) != null;
	}

	@SuppressWarnings("unchecked")
	public void addMessage(CotEventContainer msg, String repeatableType) {

		if (logger.isDebugEnabled()) {
			logger.debug("adding new repeatable message: " + msg + " type " + repeatableType);
		}

		if (msg.getContextValue(Constants.GROUPS_KEY) != null) {
			try {
				NavigableSet<Group> groups = (NavigableSet<Group>) msg.getContextValue(Constants.GROUPS_KEY);

				logger.debug("repeatable message groups: " + groups);

			} catch (ClassCastException e) {
				logger.debug("Ignoring message with invalid type of group set object - " + msg);
			}
		}

		RepeatableContainer repeatableContainer = new RepeatableContainer(msg, repeatableType);
		repeaterStore().removeCancelledMessage(msg.getUid());
		repeaterStore().addRepeatedMessage(msg.getUid(), repeatableContainer);
	}

	public Integer getRepeatableMessageCount() {
		return repeaterStore().getRepeatedMessageCount();
	}

	public boolean removeRepeatedMessage(String uid, boolean generateMsg) {
		try {
			if (repeaterStore().getRepeatedMessage(uid) == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("could not find uid to remove {}", uid);
				}
				return false;
			}

			User user = (User) repeaterStore().getRepeatedMessage(uid).getCotEventContainer().getContextValue(Constants.USER_KEY);

			if (user != null) {
				// remove the repeater's user
				MessagingDependencyInjectionProxy.getInstance().groupManager().removeUser(user);
			}

			if (generateMsg) {
				repeaterStore().addCancelledMessage(uid, repeaterStore().getRepeatedMessage(uid).getCotEventContainer());
			}

			return repeaterStore().removeRepeatedMessage(uid);

		} catch (Exception e) {
			logger.info("Exception removing user for repeater - uid: " + uid + " " + e.getMessage(), e);
			return false;
		}
	}

	public boolean removeCancelledMessage(String uid) {
		return repeaterStore().removeCancelledMessage(uid);
	}

	public Collection<CotEventContainer> getMessages() {
		List<CotEventContainer> cotEvents = new ArrayList<CotEventContainer>();
		for(RepeatableContainer repeatedMessage : repeaterStore().getRepeatedMessages()) {
			cotEvents.add(repeatedMessage.getCotEventContainer());
		}
		return cotEvents;
	}

	public Collection<Repeatable> getRepeatableMessages() {
		List<Repeatable> contentList = new ArrayList<Repeatable>();
		for(RepeatableContainer repeatedMessage : repeaterStore().getRepeatedMessages()) {
			contentList.add(repeatedMessage.getRepeatable());
		}
		return contentList;
	}

	public Integer getPeriodMillis() {
		return CoreConfigFacade.getInstance().getRemoteConfiguration().getRepeater().getPeriodMillis();
	}

	public void setPeriodMillis(Integer periodMillis) {
		CoreConfigFacade.getInstance().getRemoteConfiguration().getRepeater().setPeriodMillis(periodMillis);
	}

	public Collection<CotEventContainer> getCancelledMessages() {
		return repeaterStore().getCancelledMessages();
	}

	private RepeaterStore repeaterStore() {
		return MessagingDependencyInjectionProxy.getInstance().repeaterStore();
	}
}
