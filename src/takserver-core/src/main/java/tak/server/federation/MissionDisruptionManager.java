package tak.server.federation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.persistence.Tuple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.ROL;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.sync.MissionChangeType;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.federation.MissionActionROLConverter;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;

import mil.af.rl.rol.value.DataFeedMetadata;
import mil.af.rl.rol.value.MissionMetadata;

/*
 */
public class MissionDisruptionManager {

	public MissionDisruptionManager (FederationManager federationManager, MissionService missionService, GroupManager groupManager, EnterpriseSyncService syncService, MissionActionROLConverter missionActionROLConverter) {
		this.fedManager = federationManager;
		this.missionService = missionService;
		this.groupManager = groupManager;
		this.syncService = syncService;
		this.malrc = missionActionROLConverter;
	}

	private final FederationManager fedManager;
	private final MissionService missionService;
	private final GroupManager groupManager;
	private final EnterpriseSyncService syncService;
	private final MissionActionROLConverter malrc;

	private static final Logger logger = LoggerFactory.getLogger(MissionDisruptionManager.class);
	private static final Logger fedHealthlogger = LoggerFactory.getLogger("fedhealth");


	public List<ROL> getMissionChangesAndTrackConnectEvent(Federate federate, String fedName, FederateSubscription fedSubscription) {
		
		List<ROL> rols = new CopyOnWriteArrayList<>();
		
		if (!DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation()) return rols;

		try {
			
			if (fedHealthlogger.isDebugEnabled()) {
				fedHealthlogger.debug("getMissionChangesAndTrackConnectEvent " + fedName);
			}

			if (federate == null) {
				logger.error("null federate - unable to send fed disruption data");
				return null;
			}

			if (federate.getId() == null) {
				logger.error("null federate id - unable to send fed disruption data");
				return null;
			}

			// find out what the last event was for the federate (a connection or disconnection)
			Tuple lastEvent = MessagingDependencyInjectionProxy.getInstance().fedEventRepo().getLastEventForFederate(federate.getId());

			if (logger.isDebugEnabled()) {
				if (lastEvent == null) {
					logger.debug("null lastEvent query result");
				} else {
					logger.debug("lastEvent time " + lastEvent.get(0) + " type " + lastEvent.get(1));
				}
			}

			Date lastEventTime = null;
			String lastEventType = "NONE";

			if (lastEvent == null || lastEvent.get(0) == null ) {
				logger.info("first federate connection for " + federate.getName() + " - syncing all missions subject to recency configuration");
				lastEventTime = new Date(0);
			} else {
				lastEventTime = (Date) lastEvent.get(0);
				lastEventType = (String) lastEvent.get(1);
				
				
				if (fedHealthlogger.isDebugEnabled()) {
					fedHealthlogger.debug("not first connect event for " + fedName);
					fedHealthlogger.debug("last event " + fedName + " " + lastEventType + " " + lastEventTime);
				}

			}

			Date now = new Date();
			
			fedManager.trackConnectEventForFederate(federate.getId(), fedName, false);

			long recencySecs = DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().getMissionFederationDisruptionToleranceRecencySeconds();
			long maxRecencyMillis;
			if (recencySecs == -1) {
			    maxRecencyMillis = lastEventTime.getTime();
			} else {
			    maxRecencyMillis = now.getTime() - (recencySecs * 1000);
			}

			long bestRecencyMillis = lastEventTime.getTime() > maxRecencyMillis ? lastEventTime.getTime() : maxRecencyMillis;
			
			long bestRecencyDiffMillis = System.currentTimeMillis() - bestRecencyMillis;
			
			if (fedHealthlogger.isDebugEnabled()) {
				fedHealthlogger.debug("bestRecencyMillis " + fedName + " " + bestRecencyMillis);
				fedHealthlogger.debug("bestRecencyDiffMillis " + fedName + " " + bestRecencyDiffMillis);
			}


			if (now.getTime() - lastEventTime.getTime() < 0) {
				logger.error("negative fed time diff - clock skew with database? can't send federate mission disruption query. Try clearing federation connection data.");
				return null;
			}

			if (logger.isDebugEnabled() ) {
				logger.debug("outbound groups on federate for fed mission exchange" + federate.getOutboundGroup());
			}

			NavigableSet<Group> outboundGroups = new ConcurrentSkipListSet<>(federate.getOutboundGroup().stream().map(og -> groupManager.getGroup(og, Direction.OUT)).collect(Collectors.toSet()));
			outboundGroups.addAll(new ConcurrentSkipListSet<>(federate.getOutboundGroup().stream().map(og -> groupManager.getGroup(og, Direction.IN)).collect(Collectors.toSet())));
			
			if (logger.isDebugEnabled() ) {
				logger.debug("outbound groups for fed mission exchange" + outboundGroups);
			}

			if (logger.isDebugEnabled() ) {
				logger.debug("start: " + lastEventTime + " end: " + now);
			}

			String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(outboundGroups));

			// get enterprise sync changes as ROL
			List<String> fileUids = new ArrayList<>();
			
			if (DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions()) {
				fileUids.addAll(MessagingDependencyInjectionProxy.getInstance().fedEventRepo().getMissionResourceHashesForToolForTimeInterval("public", new Date(bestRecencyMillis), now));
				// only federating public, but make an exception for cops if vbm is enabled
				if (DistributedConfiguration.getInstance().getRemoteConfiguration().getVbm().isEnabled()) {
					String tool = DistributedConfiguration.getInstance().getRemoteConfiguration().getNetwork().getMissionCopTool();
					fileUids.addAll(MessagingDependencyInjectionProxy.getInstance().fedEventRepo().getMissionResourceHashesForToolForTimeInterval(tool, new Date(bestRecencyMillis), now));
				}
			} else {
				fileUids.addAll(MessagingDependencyInjectionProxy.getInstance().fedEventRepo().getMissionResourceHashesForTimeInterval(new Date(bestRecencyMillis), now));
			}
				
			if (logger.isDebugEnabled()) {
				logger.debug("file uids: " + fileUids);
			}

			fileUids.forEach((uid) -> {
				try {

					List<com.bbn.marti.sync.Metadata> metadatas = syncService.getMetadataByUid(uid, groupVector);
					byte[] fileBytes = syncService.getContentByUid(uid, groupVector);

					ROL resourceROL = malrc.getInsertResourceROL(metadatas.get(0), fileBytes);

					if (logger.isDebugEnabled()) {
						try {
							if (resourceROL != null) {
								logger.debug("resource ROL: " + resourceROL.getProgram() + " payload " + (resourceROL.getPayloadCount() > 0 ? resourceROL.getPayload(0).getData() == null ? "null" : resourceROL.getPayload(0).getData().size() + " bytes" : " none"));
							}
						} catch (Exception e) { }
					}

					rols.add(resourceROL);

				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("error getting file from database", e);
					}
				}
			});

			Map<String, Long> missionRecencySeconds = new HashMap<String, Long>();
			Federation.MissionDisruptionTolerance mdt = DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().getMissionDisruptionTolerance();
			if (mdt != null) {
			    for (Federation.MissionDisruptionTolerance.Mission missionInterval : mdt.getMission()) {
			        missionRecencySeconds.put(missionInterval.getName(), missionInterval.getRecencySeconds());
			    }
			}
			
			List<Mission> fedMissions = new ArrayList<>();
			
			if (DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions()) {
				fedMissions.addAll(missionService.getAllMissions(true, true, "public", outboundGroups));
				// only federating public, but make an exception for cops if vbm is enabled
				if (DistributedConfiguration.getInstance().getRemoteConfiguration().getVbm().isEnabled()) {
					String tool = DistributedConfiguration.getInstance().getRemoteConfiguration().getNetwork().getMissionCopTool();
					fedMissions.addAll(missionService.getAllMissions(true, true, tool, outboundGroups));
				}
			} else {
				fedMissions.addAll(missionService.getAllMissions(true, true, null, outboundGroups));
			}

			// get mission changes as ROL
			for (Mission fedMission : fedMissions) {
				
				// Decide whether to send this mission to the federate
				boolean isFederateThisMission = (federate.isMissionFederateDefault() != null)?federate.isMissionFederateDefault(): true; 

				for (com.bbn.marti.config.Federation.Federate.Mission missionFederateConfig: federate.getMission()) {
					if (missionFederateConfig.getName().equals(fedMission.getName())) {
						isFederateThisMission = missionFederateConfig.isEnabled();
						break;	
					}
				}
				if (!isFederateThisMission) {
					if (logger.isDebugEnabled()) {
						logger.debug("MissionDisruptionManager: Not sending mission {} to federate {}", fedMission.getName(), federate.getName());
					}
					continue;
				}
				
			    long missionRecencyMillis = missionRecencySeconds.getOrDefault(fedMission.getName(), recencySecs) * 1000;
			    long maxMissionRecencyMillis;
			    if (missionRecencyMillis < 0) {
			        maxMissionRecencyMillis = lastEventTime.getTime();
			    } else {
			        maxMissionRecencyMillis = now.getTime() - missionRecencyMillis;
			    }

				long bestMissionRecencyMillis = lastEventTime.getTime() > maxMissionRecencyMillis ? lastEventTime.getTime() : maxMissionRecencyMillis;
				
				Date bestRecencyDate = new Date(bestMissionRecencyMillis);
				
				Set<MissionChange> missionChanges = missionService.getMissionChanges(fedMission.getName(), groupVector, null, bestRecencyDate, now, true);

				if (logger.isDebugEnabled()) {
                    logger.debug("changes for mission " + fedMission.getName() + " " + missionChanges);
				}

				ROL changeROL = null;
				
				for (MissionChange change : missionChanges) {
					switch (change.getType()) {
					case CREATE_MISSION: 
						MissionMetadata meta = malrc.missionToROLMissionMetadata(fedMission);
						meta.setName(change.getMissionName());
						meta.setCreatorUid(change.getCreatorUid());
						
						changeROL = malrc.createMissionToROL(meta);
						break;
					case ADD_CONTENT:

						MissionContent mc = new MissionContent();
						

						if (Strings.isNullOrEmpty(change.getContentHash()) && Strings.isNullOrEmpty(change.getContentUid())) {
							if (logger.isDebugEnabled()) {
								logger.debug("skipping empty mission change (no hash or uid)");
							}
							break;
						}

						if (!Strings.isNullOrEmpty(change.getContentHash())) {
							mc.getHashes().add(change.getContentHash());
						}
						
						if (!Strings.isNullOrEmpty(change.getContentUid())) {
							mc.getUids().add(change.getContentUid());

							try {

								fedSubscription.submit(missionService.getLatestCotEventContainerForUid(change.getContentUid(), groupVector));

							} catch (Exception e) {
								if (logger.isDebugEnabled()) {
									logger.debug("exception getting CotEventContainer for mission uid " + change.getContentUid(), e);
								}
							}
						}


						changeROL = malrc.addMissionContentToROL(mc, fedMission.getName(), change.getCreatorUid(), fedMission);

						break;
					case DELETE_MISSION:

						changeROL = malrc.deleteMissionToROL(fedMission.getName(), change.getCreatorUid());

						break;
					case REMOVE_CONTENT:

						changeROL = malrc.deleteMissionContentToROL(fedMission.getName(), change.getContentHash(), change.getContentUid(), change.getCreatorUid(), fedMission);

						break;
					case CREATE_DATA_FEED: {
						if (DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
							MissionFeed createdMissionFeed = missionService.getMissionFeed(change.getMissionFeedUid());
							if (createdMissionFeed != null) {
								DataFeed dataFeed = DistributedConfiguration.getInstance()
										.getRemoteConfiguration()
				    					.getNetwork()
				    					.getDatafeed()
				    					.stream()
				    					.filter(df -> df.getUuid().equals(createdMissionFeed.getDataFeedUid()))
				    					.findFirst().orElse(null);
								
								if (dataFeed != null && dataFeed.isFederated()) {
									
									DataFeedMetadata createFeedMeta = new DataFeedMetadata();

									createFeedMeta.setDataFeedUid(createdMissionFeed.getDataFeedUid());
									createFeedMeta.setFilterBbox(createdMissionFeed.getFilterBbox());
									createFeedMeta.setFilterCallsign(createdMissionFeed.getFilterCallsign());
									createFeedMeta.setFilterType(createdMissionFeed.getFilterType());

									createFeedMeta.setMissionFeedUid(change.getMissionFeedUid());
									createFeedMeta.setMissionName(change.getMissionName());
									
									createFeedMeta.setArchive(dataFeed.isArchive());
									createFeedMeta.setArchiveOnly(dataFeed.isArchiveOnly());
									createFeedMeta.setSync(dataFeed.isSync());
									createFeedMeta.setFeedName(dataFeed.getName());
									createFeedMeta.setAuthType(dataFeed.getAuth().toString());
									createFeedMeta.setTags(dataFeed.getTag());

									changeROL = malrc.createDataFeedToROL(createFeedMeta);
								}
								
							}
						}
						break;
					}
					case DELETE_DATA_FEED: {
						if (DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
							MissionFeed deleteMissionFeed = missionService.getMissionFeed(change.getMissionFeedUid());
							
							if (deleteMissionFeed != null) {
								DataFeed dataFeed = DistributedConfiguration.getInstance()
										.getRemoteConfiguration()
				    					.getNetwork()
				    					.getDatafeed()
				    					.stream()
				    					.filter(df -> df.getUuid().equals(deleteMissionFeed.getDataFeedUid()))
				    					.findFirst().orElse(null);
								
								if (dataFeed != null && dataFeed.isFederated()) {
									DataFeedMetadata deleteFeedMeta = new DataFeedMetadata();
									deleteFeedMeta.setMissionFeedUid(change.getMissionFeedUid());
									deleteFeedMeta.setMissionName(change.getMissionName());
									deleteFeedMeta.setDataFeedUid(deleteMissionFeed.getDataFeedUid());
									
									changeROL = malrc.deleteDataFeedToROL(deleteFeedMeta);
								}
								
							}
						}
						break;
					}
					default:
						// no-op
						break;
					}

					if (logger.isDebugEnabled()) {
						logger.debug("ROL for change: " + changeROL.toString());
					}

					if (changeROL != null) {
						rols.add(changeROL);
					}
				}
			}

			logger.info("last fed event for " + federate.getId() + " " + lastEventType + " " + lastEventTime);
			logger.info("time since last connection: " + (now.getTime() - lastEventTime.getTime()) + " ms");



		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception in federate connection event", e);
			}
		}

		return rols;
	}
	
	public List<ROL> getDataFeedEventsForFederatedDataFeedOnly() {
		List<ROL> rols = new CopyOnWriteArrayList<>();
		
		DistributedConfiguration.getInstance()
				.getRemoteConfiguration()
				.getNetwork()
				.getDatafeed()
				.stream()
				.forEach(dataFeed -> {
					try {
						
						if (dataFeed.isFederated()) {
							DataFeedMetadata createFeedMeta = new DataFeedMetadata();
							createFeedMeta.setDataFeedUid(dataFeed.getUuid());
							createFeedMeta.setArchive(dataFeed.isArchive());
							createFeedMeta.setArchiveOnly(dataFeed.isArchiveOnly());
							createFeedMeta.setSync(dataFeed.isSync());
							createFeedMeta.setFeedName(dataFeed.getName());
							createFeedMeta.setAuthType(dataFeed.getAuth().toString());
							createFeedMeta.setTags(dataFeed.getTag());
							
							rols.add(malrc.updateDataFeedToROL(createFeedMeta));
						}
						
					} catch (JsonProcessingException e) {
						if (logger.isDebugEnabled()) {
							logger.debug("exception in federate data feed event", e);
						}
					}
				});
		
		return rols;
	}
}

