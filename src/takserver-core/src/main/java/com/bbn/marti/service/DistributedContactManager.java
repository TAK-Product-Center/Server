

package com.bbn.marti.service;

import java.util.Collection;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.RemoteContact;
import com.bbn.marti.remote.RemoteFile;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.google.common.base.Strings;


public class DistributedContactManager implements ContactManager, org.apache.ignite.services.Service {
	
	private static final long serialVersionUID = 305271019746031737L;

	private static final Logger logger = LoggerFactory.getLogger(DistributedContactManager.class);
	
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

	@Override
	public Collection<RemoteFile> getFileList() {
		return subscriptionStore().getFileSet();
	}

	@Override
	public Collection<RemoteContact> getContactList() {
		return subscriptionStore().getContacts();
	}

	public String getCallsignFromUid(String uid) {
		String ret = null;
		RemoteContact contact = subscriptionStore().getContactByContactUid(uid);
		if (contact != null) {  
			ret = contact.getContactName();
		}
		return ret;
	}

	@Override
	public void updateContact(RemoteContact contact) {
		subscriptionStore().putContactToContactUid(contact.getUid(), contact); 
	}

	/*
	 * Explicitly remove a contact
	 * @see com.bbn.marti.remote.ContactManagerInterface#removeContact(java.lang.String)
	 */
	@Override
	public void removeContact(String uid) {
		if (Strings.isNullOrEmpty(uid)) {
			throw new IllegalArgumentException ("empty uid for remove contact");
		}

		subscriptionStore().removeContactByContactUid(uid);
	}

	@Override
	public void addContact(RemoteContact contact) {

		if (logger.isDebugEnabled()) {
			logger.debug("adding remote contact " + contact);
		}

		if (contact == null || contact.getUid() == null) {
			throw new IllegalArgumentException("null contact");
		}

		subscriptionStore().putContactToContactUid(contact.getUid(), contact);     
	}

	private SubscriptionStore subscriptionStore() {
		return MessagingDependencyInjectionProxy.getInstance().subscriptionStore();
	}
}
