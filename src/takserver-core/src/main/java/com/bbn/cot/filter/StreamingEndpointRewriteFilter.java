

package com.bbn.cot.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;

import javax.naming.ldap.LdapName;

import org.dom4j.Element;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.atakmap.Tak.ROL;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.sync.MissionMetadata;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionPermission;
import com.bbn.marti.sync.model.MissionSubscription;
import com.bbn.marti.sync.service.MissionService;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.federation.DistributedFederationManager;

public class StreamingEndpointRewriteFilter implements CotFilter {

	public static String DEFAULT_STREAMINGENDPOINT_KEY = "filter.streamingbroker";
	public static String EXPLICIT_PUBLISH_KEY = "explicitBrokeringPub";
	public static String EXPLICIT_CALLSIGN_KEY = "explicitBrokeringCallsign";
	public static String EXPLICIT_UID_KEY = "explicitBrokeringUid";
	public static String EXPLICIT_FEED_UID_KEY = "explicitFeedBrokeringUid";
	public static String EXPLICIT_MISSION_KEY = "explicitBrokeringMission";
	public static String EXPLICIT_MISSION_KEY_GUID = "explicitBrokeringMissionByGuid";

	public static String UID_ATTR = "uid";
	public static String CALLSIGN_ATTR = "callsign";
	public static String PUBLISH_ATTR = "publish";
	public static String MISSION_ATTR = "mission";
	public static String MISSION_ATTR_GUID = "mission-guid";
	public static String PATH_ATTR = "path";
	public static String AFTER_ATTR = "after";

  	public static String DEST_XPATH = String.format("/event/detail/marti/dest[@%s or @%s or @%s or @%s or @%s or @%s or @%s]", CALLSIGN_ATTR, PUBLISH_ATTR, UID_ATTR, MISSION_ATTR, PATH_ATTR, AFTER_ATTR, MISSION_ATTR_GUID);

  	private static final Logger logger = LoggerFactory.getLogger(StreamingEndpointRewriteFilter.class);

  	@Autowired
  	private SubscriptionManager subscriptionManager;

  	@Autowired
  	private SubscriptionStore subscriptionStore;

  	@Autowired
  	private RepositoryService repositoryService;

  	@Autowired
   	private DistributedFederationManager federationManager;

   	@Autowired
   	ApplicationContext context;

	private MissionService missionService;
	
	private final Logger changeLogger = LoggerFactory.getLogger(Constants.CHANGE_LOGGER);

	public StreamingEndpointRewriteFilter(MissionService missionService) {
		this.missionService = missionService;
	}

	@SuppressWarnings("unchecked")
    @Override
	public CotEventContainer filter(final CotEventContainer cot) {
		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

		if (logger.isTraceEnabled()) {
			logger.trace("StreamingEndpointFilter start");
		}

		if (config.getFilter().getStreamingbroker().isEnable()) {
			List<Node> destList = cot.getDocument().selectNodes(DEST_XPATH);

			if (destList.size() > 0) {
				List<String> publishList = new LinkedList<String>();
				List<String> callsignList = new LinkedList<String>();
				Set<String> uids = new HashSet<>();
				Set<String> missionNames = new HashSet<>();
				Set<UUID> missionGuids = new HashSet<>();
				Map<String, String> missionPathMap = new HashMap<>();
				Map<String, String> missionAfterMap = new HashMap<>();

				String clientUid = "";
				try {
					if (cot.getContextValue(Constants.CLIENT_UID_KEY) != null) {
						clientUid = (String)cot.getContextValue(Constants.CLIENT_UID_KEY);
					}

					Subscription sub = null;
					if (clientUid == null || clientUid.length() == 0 ) {
						Object handler = cot.getContextValue(Constants.SOURCE_TRANSPORT_KEY);
						if (handler != null) {
							sub = subscriptionStore.getByHandler((ChannelHandler) handler);
							clientUid = sub.clientUid;
						}
					}

					if (clientUid == null || clientUid.length() == 0 ) {
	                    User user = (User) cot.getContextValue(Constants.USER_KEY);
	                    if (user != null) {
							try {
								sub = subscriptionManager.getSubscription(user);
							} catch (IllegalStateException e) {
								logger.trace("filtering cot for non-socket web connection");
							}

							if (sub != null && sub.clientUid != null && sub.clientUid.length() > 0) {
								clientUid = sub.clientUid;
							} else if (user != null && user.getCert() != null
									&& user.getCert().getSubjectX500Principal() != null) {
								clientUid = user.getCert().getSubjectX500Principal().getName();
							}
						}
				    }

				} catch (Exception e) {
				    logger.error("exception getting client uid " + e.getMessage(), e);
				}

				for (Node destElem : destList) {
					Element detached = (Element) destElem.detach();

					if (detached.attribute(CALLSIGN_ATTR) != null) {
						callsignList.add(detached.attributeValue(CALLSIGN_ATTR));
					} else if (detached.attribute(PUBLISH_ATTR) != null) {
						publishList.add(detached.attributeValue(PUBLISH_ATTR));
					} else if (detached.attribute(UID_ATTR) != null) {
						uids.add(detached.attributeValue(UID_ATTR));
					} else if (detached.attribute(MISSION_ATTR) != null) {
					    missionNames.add(detached.attributeValue(MISSION_ATTR));
				        logger.debug("mission destination specified in message: {}", detached.attributeValue(MISSION_ATTR));

				        if (detached.attribute(PATH_ATTR) != null) {
				        	missionPathMap.put(detached.attributeValue(MISSION_ATTR), detached.attributeValue(PATH_ATTR));
				        	if (detached.attribute(AFTER_ATTR) != null) {
								missionAfterMap.put(detached.attributeValue(MISSION_ATTR), detached.attributeValue(AFTER_ATTR));
							}
						}
                    } else if (detached.attribute(MISSION_ATTR_GUID) != null) {
                    	
                    	String guidString = detached.attributeValue(MISSION_ATTR_GUID);
                    	
                    	logger.debug("mission guid string in message {}", guidString);
                    	
                    	// parse UUID
                    	UUID missionUuid = null;
                    	
                    	try {

                    		missionUuid = UUID.fromString(guidString);

                    	} catch (IllegalArgumentException e) {
                    		logger.warn("invalid mission guid in streaming message {}", guidString);
                    	}
                    	
                    	if (missionUuid != null) {

                    		missionGuids.add(missionUuid);
                    		
                    		logger.debug("mission destination specified in message: {}", missionUuid);

                    		if (detached.attribute(PATH_ATTR) != null) {
                    			missionPathMap.put(detached.attributeValue(MISSION_ATTR_GUID), detached.attributeValue(PATH_ATTR));
                    			if (detached.attribute(AFTER_ATTR) != null) {
                    				missionAfterMap.put(detached.attributeValue(MISSION_ATTR_GUID), detached.attributeValue(AFTER_ATTR));
                    			}
                    		}
                    	}
                    }
				}

				if (publishList.size() > 0) {
                    cot.setContextValue(EXPLICIT_PUBLISH_KEY, publishList);
                }

				if (callsignList.size() > 0 && !callsignList.contains("All Streaming")) {
                    cot.setContextValue(EXPLICIT_CALLSIGN_KEY, callsignList);
                }

				if (!missionNames.isEmpty()) {
				    cot.setContextValue(EXPLICIT_MISSION_KEY, missionNames);
				    
				    if (changeLogger.isDebugEnabled()) {
				    	changeLogger.debug("explicit missonNames in message {}", missionNames);
				    }
				}
				
				if (!missionGuids.isEmpty()) {
				    cot.setContextValue(EXPLICIT_MISSION_KEY_GUID, missionGuids);
				    
				    if (changeLogger.isDebugEnabled()) {
				    	changeLogger.debug("explicit missonGuids in message: {}", missionGuids);
				    }
				}

				// do add to mission

                logger.debug("explicit uids for message " + cot.getUid() + " " + uids);
                logger.debug("explicit callsigns for message " + cot.getUid() + " " + callsignList);

                // use thread pool?
    			processTracksByMissionName(cot, missionNames, clientUid, uids, missionPathMap, missionAfterMap);
    			processTracksByMissionGuid(cot, missionGuids, clientUid, uids, missionPathMap, missionAfterMap);

				if (uids.size() > 0) {
					cot.setContextValue(EXPLICIT_UID_KEY, new ArrayList<String>(uids));
				}
			}
		}

        // remove marti element completely
		Element martiElem = (Element) cot.getDocument().selectSingleNode(Constants.MARTI_XPATH);
		if (martiElem != null) {
			martiElem.detach();
		}

		return cot;
	}
	
	private void processTracksByMissionName(
			final CotEventContainer cot,
			Set<String> missionNames, String clientUid,
			Set<String> uids,
			Map<String, String> missionPathMap,
			Map<String, String> missionAfterMap) {

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

		// add the client uid for each mission subscriber to the explicit uid list
		try {

			String groupVector = RemoteUtil.getInstance().bitVectorToString(
					RemoteUtil.getInstance().getBitVectorForGroups(
							(NavigableSet<Group>)cot.getContext(Constants.GROUPS_KEY)));

			for (final String missionName : missionNames) {

				MissionSubscription missionSubscription = null;
				User user = (User) cot.getContextValue(Constants.USER_KEY);
				if (user != null) {
					missionSubscription = missionService
							.getMissionSubcriptionByMissionNameAndClientUidAndUsernameNoMission(
									missionName, clientUid, user.getName());

					if (missionSubscription == null
							&& user.getCert() != null && user.getCert().getSubjectX500Principal() != null) {
						// lookup the mission subscription based on CN, needed when input auth=ldap or auth=file
						String cn = new LdapName(user.getCert().getSubjectX500Principal().getName())
								.getRdns().stream().filter(i -> i.getType().equalsIgnoreCase("CN"))
								.findFirst().get().getValue().toString();
						missionSubscription = missionService
								.getMissionSubcriptionByMissionNameAndClientUidAndUsernameNoMission(
										missionName, clientUid, cn);
					}

				} else {
					missionSubscription = missionService.getMissionSubscriptionByMissionNameAndClientUidNoMission(
							missionName, clientUid);
				}

				if (missionSubscription == null) {
					logger.error("unable to find mission subscription for client " + missionName + ", " + clientUid);
					continue;
				} else {
					if (changeLogger.isDebugEnabled()) {
						changeLogger.debug("mission sub for explcit mission sender to " + missionName + ": " + missionSubscription);
					}
				}

				if (missionSubscription.getRole() != null && !missionSubscription.getRole().
						hasPermission(MissionPermission.Permission.MISSION_WRITE)) {
					logger.error("Illegal attempt to adding streaming content to mission!");
					continue;
				}

				Mission m = missionService.getMissionByNameCheckGroups(missionName, groupVector); // fetch the mission so that we have the guid available. Could be replaced with a call to get the guid only (more efficient).

				for (String missionClientUid : subscriptionManager.getMissionSubscriptions(m.getGuidAsUUID(), true)) {
					// don't send the event back to the submitter
					if (clientUid != null && clientUid.compareTo(missionClientUid) == 0) {
						continue;
					}

					uids.add(missionClientUid);
				}

				if (changeLogger.isDebugEnabled()) {
					changeLogger.debug("mission client uid for mission sub count: " + uids.size());
				}

				// final copy of variable to use in inner class
				final String finClientUid = clientUid;
				final String finGroupVector = groupVector;

				// TODO: refactor this for performance checks, separation of concerns
				CotEventContainer copyCot = cot.copy();

				final String fclientUid = clientUid;

				if (changeLogger.isDebugEnabled()) {
					changeLogger.debug("sending change to executor for clientUid: " + fclientUid + " and add uid: " + copyCot.getUid());
				}

				// Don't add content if this message came over NATS. It was already added on the origin node
				if (!cot.hasContextKey(Constants.NATS_MESSAGE_KEY)) {
					try {
						MissionContent missionContent = new MissionContent();
						missionContent.getUids().add(copyCot.getUid());

						if (missionPathMap.containsKey(missionName)) {

							if (missionAfterMap.containsKey(missionName)) {
								missionContent.setAfter(missionAfterMap.get(missionName));
							}

							MissionContent pathContent = new MissionContent();
							pathContent.getOrCreatePaths().put(
									missionPathMap.get(missionName), Arrays.asList(missionContent));
							missionContent = pathContent;
						}

						missionService.addMissionContent(m.getGuidAsUUID(), missionContent, finClientUid, finGroupVector);

						//TODO - add case here for mission guid. Where to get it
					} catch (Exception e) {
						logger.error("exception adding content uid to mission " + e.getMessage(), e);
					}
				}

				if (config.getFederation().isEnableFederation()
						&&	config.getFederation().isAllowMissionFederation()) {
					// federate this mission update (subject to group filtering)
					try {

						NavigableSet<Group> groups = null;

						String uid = cot.getUid();

						if (Strings.isNullOrEmpty(uid)) {
							throw new IllegalArgumentException("empty uid in cot for mission content add");
						}

						MissionContent content = new MissionContent();
						content.getUids().add(uid);

						if (cot.getContextValue(Constants.GROUPS_KEY) != null) {
							try {
								groups = (NavigableSet<Group>) cot.getContextValue(Constants.GROUPS_KEY);

								if (logger.isDebugEnabled()) {
									logger.debug("groups for message: " + cot + ": " + groups);
								}

							} catch (ClassCastException e) {
								logger.debug("Not trying to get group info for message with invalid type of groups object: " + cot);
							}
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("Groups context key not set for message: " + cot);
							}
						}

						if (groups != null) {

							MissionMetadata mission = repositoryService.getMissionMetadata(missionName);

							if (mission == null) {
								logger.debug("nothing to federate for non-existent mission {}", missionName);
								return;
							}


							if (config.getFederation().isFederateOnlyPublicMissions()) {
								if ("public".equals(mission.getTool())) {
									// allow public. no action needed as of now
								} else if (config.getNetwork().getMissionCopTool().equals(mission.getTool())) {
									if (!config.getVbm().isEnabled()) {
										logger.debug("not federating vbm mission action for mission " + missionName + " since vbm is disabled");
										return;
									}
								} else {
									logger.debug("not federating non-public mission action for mission " + missionName);
									return;
								}
							}

							ROL rol = RemoteUtil.getInstance().getROLforMissionChange(content, missionName, fclientUid, mission.getCreatorUid(), mission.getChatRoom(), mission.getTool(), mission.getDescription());

							if (logger.isDebugEnabled()) {
								logger.debug("rol to federate for mission change " + rol + " to groups " + groups);
							}

							federationManager.submitMissionFederateROL(rol, groups, missionName);
						} else {
							logger.warn("unable to federate mission uid add - cot message specified no groups");
						}

					} catch (Exception e) {
						logger.debug("exception adding content uid to mission " + e.getMessage(), e);
					}
				}
			}
		} catch (Exception e) {
			logger.debug("exception getting mission subscriber uids: " + e.getMessage(), e);
		}
	}
	
	private void processTracksByMissionGuid(
			final CotEventContainer cot,
			Set<UUID> missionGuids, String clientUid,
			Set<String> uids,
			Map<String, String> missionPathMap, // map of <missionName>, <path>?
			Map<String, String> missionAfterMap) {

//		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();
//
//		// add the client uid for each mission subscriber to the explicit uid list
//		try {
//
//			String groupVector = RemoteUtil.getInstance().bitVectorToString(
//					RemoteUtil.getInstance().getBitVectorForGroups(
//							(NavigableSet<Group>)cot.getContext(Constants.GROUPS_KEY)));
//
//			for (final UUID missionGuid : missionGuids) {
//
//				// TODO refactor this into a method
//
//				MissionSubscription missionSubscription = null;
//				User user = (User) cot.getContextValue(Constants.USER_KEY);
//				if (user != null) {
//					missionSubscription = missionService
//							.getMissionSubcriptionByMissionNameAndClientUidAndUsernameNoMission(
//									missionGuid.toString(), clientUid, user.getName());
//
//					if (missionSubscription == null
//							&& user.getCert() != null && user.getCert().getSubjectX500Principal() != null) {
//						// lookup the mission subscription based on CN, needed when input auth=ldap or auth=file
//						String cn = new LdapName(user.getCert().getSubjectX500Principal().getName())
//								.getRdns().stream().filter(i -> i.getType().equalsIgnoreCase("CN"))
//								.findFirst().get().getValue().toString();
//						missionSubscription = missionService
//								.getMissionSubcriptionByMissionGuidAndClientUidAndUsernameNoMission(
//										missionGuid.toString(), clientUid, cn);
//					}
//
//				} else {
//					missionSubscription = missionService.getMissionSubscriptionByMissionGuidAndClientUidNoMission(
//							missionGuid.toString(), clientUid);
//				}
//
//				if (missionSubscription == null) {
//					logger.error("unable to find mission subscription for client {}, {} ", missionGuid, clientUid);
//					continue;
//				} else {
//					changeLogger.debug("mission sub for explcit mission sender to {}: {}", missionGuid, missionSubscription);
//				}
//
//				if (missionSubscription.getRole() != null && !missionSubscription.getRole().
//						hasPermission(MissionPermission.Permission.MISSION_WRITE)) {
//					logger.error("Illegal attempt to adding streaming content to mission!");
//					continue;
//				}
//
//				for (String missionClientUid : subscriptionManager.getMissionSubscriptions(missionGuid, true)) {
//					// don't send the event back to the submitter
//					if (clientUid != null && clientUid.compareTo(missionClientUid) == 0) {
//						continue;
//					}
//
//					uids.add(missionClientUid);
//				}
//
//				if (changeLogger.isDebugEnabled()) {
//					changeLogger.debug("mission client uid for mission sub count: " + uids.size());
//				}
//
//				// final copy of variable to use in inner class
//				final String finClientUid = clientUid;
//				final String finGroupVector = groupVector;
//
//				// TODO: refactor this for performance checks, separation of concerns
//				CotEventContainer copyCot = cot.copy();
//
//				final String fclientUid = clientUid;
//
//				if (changeLogger.isDebugEnabled()) {
//					changeLogger.debug("sending change to executor for clientUid: " + fclientUid + " and add uid: " + copyCot.getUid());
//				}
//
////				Resources.missionContentProcessor.execute(() -> {
//					// Don't add content if this message came over NATS. It was already added on the origin node
//					if (!cot.hasContextKey(Constants.NATS_MESSAGE_KEY)) {
//						try {
//							MissionContent missionContent = new MissionContent();
//							missionContent.getUids().add(copyCot.getUid());
//
//							if (missionPathMap.containsKey(missionName)) {
//
//								if (missionAfterMap.containsKey(missionName)) {
//									missionContent.setAfter(missionAfterMap.get(missionName));
//								}
//
//								MissionContent pathContent = new MissionContent();
//								pathContent.getOrCreatePaths().put(
//										missionPathMap.get(missionName), Arrays.asList(missionContent));
//								missionContent = pathContent;
//							}
//
//							missionService.addMissionContent(missionName, missionContent, finClientUid, finGroupVector);
//
//							//TODO - add case here for mission guid. Where to get it
//						} catch (Exception e) {
//							logger.error("exception adding content uid to mission " + e.getMessage(), e);
//						}
//					}
//
//					if (config.getFederation().isEnableFederation()
//							&&	config.getFederation().isAllowMissionFederation()) {
//						// federate this mission update (subject to group filtering)
//						try {
//
//							NavigableSet<Group> groups = null;
//
//							String uid = cot.getUid();
//
//							if (Strings.isNullOrEmpty(uid)) {
//								throw new IllegalArgumentException("empty uid in cot for mission content add");
//							}
//
//							MissionContent content = new MissionContent();
//							content.getUids().add(uid);
//
//							if (cot.getContextValue(Constants.GROUPS_KEY) != null) {
//								try {
//									groups = (NavigableSet<Group>) cot.getContextValue(Constants.GROUPS_KEY);
//
//									if (logger.isDebugEnabled()) {
//										logger.debug("groups for message: " + cot + ": " + groups);
//									}
//
//								} catch (ClassCastException e) {
//									logger.debug("Not trying to get group info for message with invalid type of groups object: " + cot);
//								}
//							} else {
//								if (logger.isDebugEnabled()) {
//									logger.debug("Groups context key not set for message: " + cot);
//								}
//							}
//
//							if (groups != null) {
//
//								MissionMetadata mission = repositoryService.getMissionMetadata(missionName);
//
//								if (mission == null) {
//									logger.debug("nothing to federate for non-existent mission {}", missionName);
//									return;
//								}
//
//
//								if (config.getFederation().isFederateOnlyPublicMissions()) {
//									if ("public".equals(mission.getTool())) {
//										// allow public. no action needed as of now
//									} else if (config.getNetwork().getMissionCopTool().equals(mission.getTool())) {
//										if (!config.getVbm().isEnabled()) {
//											logger.debug("not federating vbm mission action for mission " + missionName + " since vbm is disabled");
//											return;
//										}
//									} else {
//										logger.debug("not federating non-public mission action for mission " + missionName);
//										return;
//									}
//								}
//
//								ROL rol = RemoteUtil.getInstance().getROLforMissionChange(content, missionName, fclientUid, mission.getCreatorUid(), mission.getChatRoom(), mission.getTool(), mission.getDescription());
//
//								if (logger.isDebugEnabled()) {
//									logger.debug("rol to federate for mission change " + rol + " to groups " + groups);
//								}
//
//								federationManager.submitMissionFederateROL(rol, groups, missionName);
//							} else {
//								logger.warn("unable to federate mission uid add - cot message specified no groups");
//							}
//
//						} catch (Exception e) {
//							logger.debug("exception adding content uid to mission " + e.getMessage(), e);
//						}
//					}
////				});
//			}
//		} catch (Exception e) {
//			logger.debug("exception getting mission subscriber uids: " + e.getMessage(), e);
//		}
	}
}
