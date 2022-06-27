package com.bbn.marti.sync.api;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.ContactApi;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.RemoteContact;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.util.CommonUtil;
import com.google.common.base.Strings;

/*
 * 
 * REST API for contacts
 * 
 */
@RestController
public class ContactsApi extends BaseRestController {

	private static final Logger logger = LoggerFactory.getLogger(ContactsApi.class);

	// keep a reference to the currently active request
	@Autowired
	private HttpServletRequest request;

	@Autowired
	SubscriptionManagerLite subMgr;

	@Autowired
	ContactManager contactManager;

	@Autowired
	FederationManager federationManager;

	@Autowired
	private CommonUtil martiUtil;
	
	@Autowired
	CoreConfig coreConfig;

	// GET all subscriptions
	@RequestMapping(value = "/contacts/all", method = RequestMethod.GET)
	ResponseEntity<List<RemoteSubscription>> getAllContacts(
			@RequestParam(value = "sortBy", defaultValue = "CALLSIGN") SubscriptionSortField sortBy,
			@RequestParam(value = "direction", defaultValue = "ASCENDING") SubscriptionSortOrder direction,
			@RequestParam(value = "noFederates", defaultValue = "false") boolean noFederates) throws RemoteException {

		String groupVector = null;

		Set<Group> inWriteOnlyFilteredGroups = new ConcurrentSkipListSet<>();
		
		try {
			// Get group vector for the user associated with this session
			groupVector = martiUtil.getGroupBitVector(request, Direction.OUT);
			
			Set<Group> groups = martiUtil.getGroupsFromActiveRequest();
			
			if (groups != null) {
				if (!coreConfig.getRemoteConfiguration().getFilter().getContactApi().isEmpty()) {
					for (ContactApi filter : coreConfig.getRemoteConfiguration().getFilter().getContactApi()) {
						if (filter.isWriteOnly() && !Strings.isNullOrEmpty(filter.getGroupName())) {
							if (martiUtil.hasAccessWriteOnly(groups, filter.getGroupName())) {
								inWriteOnlyFilteredGroups.add(new Group(filter.getGroupName(), Direction.IN));
							}
						}
					}
				}
			}
			
			if (logger.isDebugEnabled()) {
				logger.debug("groups bit vector: " + groupVector);
				logger.debug("inWriteOnlyFilteredGroups: " + inWriteOnlyFilteredGroups);
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception getting group membership for current web user " + e.getMessage());
			}
		}
		
		List<RemoteSubscription> subscriptions = subMgr.getSubscriptionsWithGroupAccess(groupVector, noFederates, inWriteOnlyFilteredGroups);

		if (logger.isDebugEnabled()) {
			logger.debug("get all contacts (subscriptions): " + subscriptions);
		}

		boolean reversed = false;

		if (direction.equals(SubscriptionSortOrder.DESCENDING)) {
			reversed = true;
		}
		try {
			Set<RemoteContact> fedContacts = new HashSet<>();
			for(Federate federate : federationManager.getAllFederates()) {
				Collection<RemoteContact> tmpContacts = federationManager.getContactsForFederate(federate.getId(), groupVector);
				for(RemoteContact con : tmpContacts) {
					fedContacts.add(con);
				}
			}
			for(RemoteContact contact : fedContacts) {
				RemoteSubscription sub = new RemoteSubscription();
				sub.callsign = contact.getContactName();
				sub.clientUid = contact.getUid();
				sub.uid = contact.getUid();
				subscriptions.add(sub);
			}
		}
		catch(Exception e) {
			logger.warn("Exception getting federate contacts - federate server might be configured incorrectly " + e.toString());
		}
		if (sortBy.equals(SubscriptionSortField.CALLSIGN)) {
			Collections.sort(subscriptions, RemoteSubscription.sortByCallsign(reversed));
		} else {
			Collections.sort(subscriptions, RemoteSubscription.sortByClientUid(reversed));
		}

		return new ResponseEntity<List<RemoteSubscription>>(subscriptions, new HttpHeaders(), HttpStatus.OK);
	}
}
