

package com.bbn.marti.groups;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atakmap.Tak.CRUD;
import com.atakmap.Tak.ContactListEntry;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;
import com.bbn.cot.filter.StreamingEndpointRewriteFilter;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Input;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.Reachability;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.FederatedSubscriptionManager;
import com.bbn.marti.service.SSLConfig;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

import mil.af.rl.rol.value.ResourceDetails;
import tak.server.cot.CotEventContainer;
import tak.server.federation.FederateSubscription;
import tak.server.federation.FigFederateSubscription;
import tak.server.federation.MissionPackageAnnounce;
import tak.server.federation.MissionPackageDestinations;

/*
 * Utility class for groups and federation
 * 
 */
public class GroupFederationUtil {
    
    public static final String FEDERATE_ID_KEY = "federate.id";
    
    public static final String ANONYMOUS_USERNAME_BASE = "Anonymous";
    public static final String ANONYMOUS_USERNAME_DEFAULT_SUFFIX = "auto";
    public static final String ANONYMOUS_DEFAULT_GROUP = "__ANON__";
    private static final Logger logger = LoggerFactory.getLogger(GroupFederationUtil.class);

    private static GroupFederationUtil instance = null;

    @Autowired
    private GroupManager groupManager;
    
    @Autowired
    private SubscriptionManager subscriptionManager;
   
    @Autowired
    private FederatedSubscriptionManager federatedSubscriptionManager;

    @Autowired
	private SubscriptionStore subscriptionStore;
    
    // track cancellation status of periodic updates by connectionId
    public final Map<String, AtomicBoolean> updateCancelMap = new ConcurrentHashMap<>();

    public static GroupFederationUtil getInstance() {
    	if (instance == null) {
    		synchronized (GroupFederationUtil.class) {
    			if (instance == null) {	
    				instance = SpringContextBeanForApi.getSpringContext().getBean(GroupFederationUtil.class);
    			}
    		}
    	}

    	return instance;
    }
    
    public static Set<Group> parseGroupsAttribute(String groups) {
    
        if (Strings.isNullOrEmpty(groups)) {
            throw new IllegalArgumentException("empty groups string");
        }
        
        Set<Group> groupSet = new ConcurrentSkipListSet<>();
        
        for (String groupName : Splitter.on(';').split(groups)) {
            if (!Strings.isNullOrEmpty(groupName)) {
                groupSet.add(new Group(groupName, Direction.IN));
                groupSet.add(new Group(groupName, Direction.OUT));
            }
        }
        
        return groupSet;
    }
    
    public User getAnonymousUser() {
        return getUser(ANONYMOUS_USERNAME_DEFAULT_SUFFIX, true);
    }
    
    public User getUser(String suffix, boolean anonymous) {

        String username = GroupFederationUtil.ANONYMOUS_USERNAME_BASE + "_" + suffix;

        User user = new AuthenticatedUser(username, username, "", null, username, "", "");

        if (anonymous) {
			Set<Group> groups = new ConcurrentSkipListSet<>();

			groups.add(new Group(GroupFederationUtil.ANONYMOUS_DEFAULT_GROUP, Direction.IN));
			groups.add(new Group(GroupFederationUtil.ANONYMOUS_DEFAULT_GROUP, Direction.OUT));

			groupManager.updateGroups(user, groups);
		}
        
        Subscription sub = new Subscription();

        // set user on subscription, so that message brokering will be able to find the user
        sub.setUser(user);
        sub.callsign = user.getName();
        sub.clientUid = user.getName();

        subscriptionManager.addRawSubscription(sub);
        subscriptionManager.setClientForSubscription(sub);
    
        return user;
    }
    
    public void trackLatestSA(Subscription subscription, CotEventContainer cot) {
    	trackLatestSA(subscription, cot, false);
    }
    
    public void trackLatestSA(Subscription subscription, CotEventContainer cot, boolean force) {
      
        if (subscription == null || cot == null) {
            throw new IllegalArgumentException("null subscription or cot");
        }
        
		cot.setStored(true);

        if (cot.getUid() != null && subscription.clientUid != null && (force || cot.getUid().equals(subscription.clientUid))) {

            // save a defensive copy of this cot object
            subscription.setLatestSA(cot.copy());      
        }
    }
    
   
    
    public Collection<Subscription> getReachableSubscriptions(Subscription src) {

        if (src == null) {
        	return new ArrayList<>();
        }
        
        Collection<Subscription> subs = new ConcurrentLinkedDeque<>();
        
        Set<User> reachableUsers = getReachableUsers(src);
        
        for (User u : reachableUsers) {
        	if (u != null) {
        		try {
        			subs.add(subscriptionManager.getSubscription(u));
        		} catch (Exception e) {
        			if (logger.isTraceEnabled()) {
        				logger.trace("exception getting subscription for user", e);
        			}
        		}
        	}
        }
        
        return subs;
    }
    
    public Set<Subscription> getReachableSubscriptionsSet(Subscription src) {

        if (src == null) {
        	return new ConcurrentSkipListSet<>();
        }
        
        Set<Subscription> subs = ConcurrentHashMap.newKeySet();
        Set<User> reachableUsers = getReachableUsers(src);
        
        for (User u : reachableUsers) {
        	if (u != null) {
        		try {
        			subs.add(subscriptionManager.getSubscription(u));
        		} catch (Exception e) {
        			if (logger.isTraceEnabled()) {
        				logger.trace("exception getting subscription for user", e);
        			}
        		}
        	}
        }
        
        return subs;
    }
    
    public Set<Subscription> getReachableFederatedGroupMappingSubscriptons(Subscription sub) {
		// since federated group mapping doesnt follow traditional group reachability flow,
		// we need to check each FigFederateSubscription individually to determine reachability
		boolean srcFedUser = sub.getUser() instanceof FederateUser;
		Set<Subscription> reachable = ConcurrentHashMap.newKeySet();
		for (FederateSubscription destSub : SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederateSubscriptions()) {
			if (destSub instanceof FigFederateSubscription) {
				FigFederateSubscription destFedSub = (FigFederateSubscription) destSub;
				
				boolean isNotFedToFed = srcFedUser == false || (srcFedUser && !(destFedSub.getUser() instanceof FederateUser));
				boolean isNotSelf = !sub.getUser().equals(destFedSub.getUser());
				boolean isFederatedGroupMapping = destFedSub.getFederate().isFederatedGroupMapping();

				if (destFedSub != null && isNotFedToFed && isNotSelf && isFederatedGroupMapping) {
					NavigableSet<Group> srcGroups = groupManager.getGroups(sub.getUser());

					for (String mapping : destFedSub.getFederate().getInboundGroupMapping()) {
						Group localGroup = new Group(mapping.split(":")[1], Direction.IN);
						if (srcGroups.contains(localGroup)) {
							reachable.add(destSub);
							break;
						}
					}
				}
			}
		}
		
		return reachable;
    }
    
    public Set<User> getReachableUsers(Subscription src) {

    	if (src == null || groupManager == null) {
    		throw new IllegalArgumentException("null subscription");
    	}

    	ConnectionInfo srcInfo = ((AbstractBroadcastingChannelHandler) src.getHandler()).getConnectionInfo();

    	if (srcInfo == null) {
    		throw new IllegalStateException("connection info not available for subscription " + src);
    	}

    	Reachability<User> srcReach = groupManager.getReachability(srcInfo);

    	if (srcReach == null) {
    		throw new IllegalStateException("null reachability in subscription " + src);
    	}

    	User user = src.getUser();

    	if (user == null) {
    		throw new IllegalStateException("null user in subscription " + src);
    	}

    	Set<User> reachableUsers = null;

    	reachableUsers = srcReach.getAllReachableFrom(user);

    	return reachableUsers;
    }
    
    public void updateInputGroups(@NotNull Input input) {
        // Get the connectionId
        String connectionId = MessageConversionUtil.getConnectionId(input);


        // If a user exists for this connectionId, use it. Otherwise, create a new one and put him into the group manager
        User user = null;

        user = groupManager.getUserByConnectionId(connectionId);
      
        if (user == null) {
            user = new AuthenticatedUser(
                    connectionId,
                    connectionId,
                    "127.0.0.1",
                    null,
                    "dummyLogin",
                    "dummyPassword",
                    "");

            groupManager.putUserByConnectionId(user, connectionId);
          
            if (logger.isDebugEnabled()) {
            	logger.debug("anonymous user created: " + user);
            }
        }

        List<String> groupList = input.getFiltergroup();

        // Determine the anongroup value
        boolean anonGroup;

        boolean hasGroups = (groupList != null && !groupList.isEmpty());

        if (input.isAnongroup() == null) {
            anonGroup = !hasGroups;
        } else {
            anonGroup = input.isAnongroup();
        }


        // Determine the groups
        Set<String> staticGroups = null;

        if (groupList != null && !groupList.isEmpty()) {
            staticGroups = new HashSet<>(groupList);
        }

        Set<Group> groups = new ConcurrentSkipListSet<>();

        if (anonGroup) {
            groups.add(new Group(GroupFederationUtil.ANONYMOUS_DEFAULT_GROUP, Direction.IN));
            groups.add(new Group(GroupFederationUtil.ANONYMOUS_DEFAULT_GROUP, Direction.OUT));
        }

        if (staticGroups != null && !staticGroups.isEmpty()) {
            // configure static groups

            for(String str : staticGroups) {
                groups.add(new Group(str, Direction.IN));
                groups.add(new Group(str, Direction.OUT));
            }
        }

        // Update the groups
        groupManager.updateGroups(user, groups);

    }
   
    // Positively filter one direction only
    public NavigableSet<Group> filterGroupDirection(Direction direction, NavigableSet<Group> groups) {

        NavigableSet<Group> filteredGroups = new ConcurrentSkipListSet<>();
        
        for (Group group : groups) {
            
            if (group.getDirection().equals(direction)) {
                filteredGroups.add(group);
            }
        }
        
        return filteredGroups;
    }

	/**
	 * Apply the federate outbound groups to the incoming groups on the message and return the
	 * intersection.
	 */
	public Set<String> filterFedOutboundGroups(List<String> fedOutboundGroups, NavigableSet<Group> clientInboundGroups, String fedId) {
		Set<String> filteredOutboundSet = new HashSet<String>();

		if (!fedOutboundGroups.isEmpty() && !clientInboundGroups.isEmpty()) {
			// add the client group names first
			for (Group group : clientInboundGroups) {
				filteredOutboundSet.add(group.getName());
			}
			// apply the federate outbound groups, the matches with the client groups will be preserved
			filteredOutboundSet.retainAll(fedOutboundGroups);
			if (logger.isDebugEnabled()) {
				logger.debug("the filtered group set for federate : " + fedId + " groups: " + filteredOutboundSet);
			}
		}
		return filteredOutboundSet;
	}

	/**
	 * Map remote federate groups (outbound) automatically to local groups (inbound)
	 */
	public NavigableSet<Group> autoMapGroups(List<String> remoteFederateGroups) {

		NavigableSet<Group> autoMappedGroups = new ConcurrentSkipListSet<>();
		for (String remoteGroup : remoteFederateGroups) {
			Group group = groupManager.getGroup(remoteGroup, Direction.IN);
			if ( group != null) {
				autoMappedGroups.add(group);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(" automatically map groups: " + autoMappedGroups);
		}
		return autoMappedGroups;
	}

    public X509Certificate loadX509Cert(File caFile) throws CertificateException, IOException {

      try (FileInputStream in = new FileInputStream(caFile)){
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
      }
    }
    
	public MissionPackageAnnounce createPackageAnnounceCot(ResourceDetails dt) {
		
		final String baseUrl = DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getWebBaseUrl();

		String localMpUrl = baseUrl + "/sync/content?hash=" + dt.getSha256();
		
		String uid = dt.getSha256();
		String shaHash = dt.getSha256();
		String callsign = dt.getName();
		String filename = dt.getFilename();
		long sizeInBytes = dt.getSize();
		
		List<String> callsigns = new ArrayList<>();
		List<String> uids = new ArrayList<>();
		
		if (!Strings.isNullOrEmpty(dt.getSenderUid())) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				
				MissionPackageDestinations mpDests = mapper.readValue(dt.getSenderUid(), MissionPackageDestinations.class);
				
				if (logger.isDebugEnabled()) {
					logger.debug("mpDests: " + mpDests);
				}
				
				callsigns = mpDests.getCallsigns();
				uids = mpDests.getUids();

			} catch (Exception e) {
				logger.warn("exception deserializing mission package annoucement destinations", e);
			}
		} 
		
		if (logger.isDebugEnabled()) {
			logger.debug("create package announce cot for uids: " + uids + " - callsign: " + callsigns);
		}
		
		String time = DateUtil.toCotTime(System.currentTimeMillis()); // now
		String staleTime = DateUtil.toCotTime(System.currentTimeMillis()+100000); // 100 seconds from now
		String cot = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
				+ "<event version='2.0' uid='"+uid+"' type='b-f-t-r' time='"+time+"' start='"+time+"' stale='"+staleTime+"' how='h-e'>"
				+ "<point lat='0.0' lon='0.0' hae='9999999.0' ce='9999999' le='9999999' />"
				+ "<detail>"
				+ "<fileshare sha256='"+shaHash+"' senderUid='"+uid+"' name='"+filename+"' filename='"+filename+"' senderUrl='"+ localMpUrl +"' sizeInBytes='"+sizeInBytes+"' senderCallsign='"+callsign+"'/>"
				+ "<marti>";
		for (String uidDest : uids) {
			cot += "<dest uid='" + uidDest + "'/>";
		}
		
		for (String callsignDest : callsigns) {
			cot += "<dest callsign='" + callsignDest + "'/>";
		}
		//+ "<ackrequest uid='02629afa-ab2e-44a8-9048-2f517b72b221' ackrequested='true' tag='MP-Grizzly' endpoint='192.168.1.9:4242:tcp'/>"
		//+ "<precisionlocation geopointsrc='???' altsrc='???'/>"
		cot += "</marti>"
				+ "</detail>"
				+ "</event>";
		
		MissionPackageAnnounce result = new MissionPackageAnnounce();
		
		result.setAnnoucement(cot);
		result.setCallsigns(callsigns);
		result.setUids(uids);
		
		return result;
	}
	
	public ROL generatePackageAnnounceROL(CotEventContainer toSend) {
		logger.debug("mission package announce for FIG: " + toSend);

		/*
		 * <event version="2.0" uid="b695c532caba4b54953f8e2a4f0e5330"
		 *  type="b-f-t-r" 
		 *  time="2017-09-22T18:13:27Z"
		 *   start="2017-09-22T18:13:27Z" stale="2017-09-22T18:15:07Z" how="h-e">
		 *   <point lat="0.0" lon="0.0" hae="9999999.0" ce="9999999" le="9999999"/>
		 *   <detail>
		 *   <fileshare sha256="b695c532caba4b54953f8e2a4f0e5330"
		 *    senderUid="b695c532caba4b54953f8e2a4f0e5330"
		 *     name="a.pdf"
		 *      filename="a.pdf"
		 *     senderUrl="https://127.0.0.1:8443/Marti/sync/content?hash=b695c532caba4b54953f8e2a4f0e5330"
		 *      sizeInBytes="441359" 
		 *      senderCallsign="a.pdf"/><_flow-tags_ marti1="2017-09-22T18:13:27Z"/></detail></event>
		 */


		ResourceDetails details = new ResourceDetails();

		details.setSha256(toSend.getDocument().selectSingleNode("//detail/fileshare/@sha256").getStringValue());
		details.setTsStored(new Date());


		try {
			details.setSenderUid("");
			details.setSenderCallsign(toSend.getDocument().selectSingleNode("//detail/fileshare/@senderCallsign").getStringValue());
			details.setFilename(toSend.getDocument().selectSingleNode("//detail/fileshare/@filename").getStringValue());
			details.setName(toSend.getDocument().selectSingleNode("//detail/fileshare/@name").getStringValue());
			details.setSize(Integer.parseInt(toSend.getDocument().selectSingleNode("//detail/fileshare/@sizeInBytes").getStringValue()));
			details.setLocalPath("");
			details.setSenderUrl("");
			
			try {
				
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				
				// look for explicit addresses in the announcement
			    @SuppressWarnings("unchecked")
				List<String> callsigns = (List<String>) toSend.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY);
			    @SuppressWarnings("unchecked")
				List<String> uids = (List<String>) toSend.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY);
			    
			    if (callsigns == null) {
			    	callsigns = new ArrayList<>();
			    }
			    
			    if (uids == null) {
			    	uids = new ArrayList<>();
			    }
			    
			    MissionPackageDestinations mpDest = new MissionPackageDestinations();
			    
			    mpDest.setCallsigns(callsigns);
			    mpDest.setUids(uids);
			    
			    if (logger.isDebugEnabled()) {
			    	logger.debug("mission package announce callsigns: " + callsigns + " - uids: " + uids);
			    }
			    
			    String mpDestJson = mapper.writeValueAsString(mpDest);
			    
			    if (logger.isDebugEnabled()) {
			    	logger.debug("mission package dest json: " + mpDestJson);
			    }
			    
			    details.setSenderUid(mpDestJson);
				
			} catch (Exception e) {
				
			    logger.debug("exception getting explicit callsigns / uids from mission package announce message", e);
			}

			logger.debug("mission package announce details: " + details);

		} catch (Exception e) {
			logger.warn("exception parsing mission package announce CoT", e);
		}

		try {

			if (Strings.isNullOrEmpty(details.getSha256())) {
				throw new IllegalArgumentException("sha 256 hash missing from package announcment CoT." + toSend.asXml());
			}
			
			String detailsJson = new ObjectMapper().writeValueAsString(details);

			String announceRol = "announce package\n" + detailsJson + ";";

			if (logger.isDebugEnabled()) {
				logger.debug("sending 'announce package' ROL: " + announceRol);
			}

			return ROL.newBuilder().setProgram(announceRol).build();

		} catch (Exception e) {
			logger.warn("exception sending federated mission package announce ROL", e);
		}
		
		throw new IllegalStateException("insufficient information to generate mission package announcmement ROL from CoT " + toSend.asXml());
	}
	
	public void sendLatestContactsToFederate(FederateSubscription fedSubscription) {
		try {
			getLatestReachableContacts(fedSubscription).forEach((contact) -> {
				if (logger.isDebugEnabled()) {
					logger.debug("send contact " + contact + " to " + fedSubscription);
				}
				// attach federate out groups to the contact message
				List<String> srcGroups = groupManager.getGroups(fedSubscription.getUser())
						.stream()
						.filter(g -> g.getDirection() == Direction.OUT)
						.map(g -> g.getName())
						.collect(Collectors.toList());
				
				fedSubscription.submitLocalContact(FederatedEvent.newBuilder().setContact(contact).addAllFederateGroups(srcGroups).build(), System.currentTimeMillis());
			});
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception sending federated contacts");
			}
		}
	}
	
	// assemble a contact list to send to a FederateSubscription
    private Collection<ContactListEntry> getLatestReachableContacts(FederateSubscription fedSub) {
    	
    	Collection<ContactListEntry> result = new LinkedList<>();
    	
    	if (fedSub == null) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("null fedSub in getLatestReachableContacts");
    		}
    		
    		return result;
    	}
      
        try {

        	User fedUser = fedSub.getUser();
        	
        	if (fedUser == null) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("null fedUser for fedSub in getLatestReachableContacts");
        		}
        		return result;
        	}
        	
            Reachability<User> r = new CommonGroupDirectedReachability(groupManager);
            
            Set<User> reachableUsers;

            reachableUsers = r.getAllReachableFrom(fedUser);

            for (User u : reachableUsers) {
            	
            	// ignore other federates for this purpose
            	if (u instanceof FederateUser) continue;
            	
                try {
                    Subscription sub = subscriptionManager.getSubscription(u);
                    if (sub != null) {

                        if (sub instanceof FederateSubscription) {
                        	// don't include other federates as ContactListEntries
                        	continue;
                        } else {
                        	
                        	ContactListEntry contact = ContactListEntry.newBuilder()
                        	.setUid(sub.clientUid)
                        	.setCallsign(sub.callsign)
                        	.setOperation(CRUD.CREATE)
                        	.build();
                        	
                        	result.add(contact);
                        }
                    } 
                } catch (IllegalStateException ise) {
                	if (logger.isDebugEnabled()) {
                		logger.debug(ise.getMessage()); 
                	}
                } catch (Exception e) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("exception getting federated contact list entry", e);
                	}
                } 
            }

        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception assembling federated contact list", e);
        	}
        }
        
        return result;
    }
    
    public void removeFedSubForConnection(ConnectionInfo connection) {
    	federatedSubscriptionManager.removeFederateSubcription(connection);
    }

	public NavigableSet<Group> addFederateGroupMapping(Multimap<String, String> inboundMap, List<String> federateGroups) {
		NavigableSet<Group> mappedGroups = new ConcurrentSkipListSet<>();

		if (!inboundMap.isEmpty()) {
			for (String remoteGroup : federateGroups) {
				Collection<String> groups = inboundMap.get(remoteGroup);
				for (String gname : groups) {
					Group g = groupManager.getGroup(gname, Direction.IN);
					if ( g != null) {
						mappedGroups.add(g);
						if (logger.isDebugEnabled()) {
							logger.debug(" found match on remote: " + remoteGroup + " local group: " + g);
						}
					}
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(" apply federate mapped groups" + mappedGroups);
		}
		return mappedGroups;
	}

	// 
	public void collectRemoteFederateGroups(Set<String> groups, Federation.Federate federate) {
		subscriptionStore.getFederateRemoteGroups().put(federate.getId(), groups);
	}
	
	// check if the remote CA matches the self CA we have in takserver.jks
	public boolean isRemoteCASelfCA(X509Certificate remote) {
		if (remote == null) return false;
		try {
			SSLConfig sslConfig = SSLConfig.getInstance(DistributedConfiguration.getInstance()
					.getRemoteConfiguration()
					.getFederation()
					.getFederationServer()
					.getTls());
			
			KeyStore selfStore = sslConfig.getSelf();
			for (Enumeration<String> e = selfStore.aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();
				Certificate[] selfCerts = selfStore.getCertificateChain(alias);
				X509Certificate selfCA = (X509Certificate) selfCerts[1];

				if (remote.equals(selfCA)) {
					return true;
				}
			}
		} catch (Exception e) {
			logger.debug("Error trying to compare CA's");
		}
		return false;
	}

}
