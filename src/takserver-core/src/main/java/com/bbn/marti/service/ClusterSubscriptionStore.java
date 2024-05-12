package com.bbn.marti.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.ConnectionStatusValue;
import com.bbn.marti.sync.service.MissionService;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;

/**
 *  
 *  Subscription Store wrapper for operating in the cluster so that we can hit the cache instead of local storage. 
 *  Will also avoid cache overhead in standard non-cluster deployment 
 */
public class ClusterSubscriptionStore extends SubscriptionStore {
	private static final Logger logger = LoggerFactory.getLogger(ClusterSubscriptionStore.class);

	private IgniteAtomicSequence missionSequence;
	private IgniteAtomicSequence missionContentsSequence;
	
	private IgniteCache<String, MissionEntry> missionUidCache;
	
	private IgniteCache<String, MissionContentsEntry> missionContentsUidCache;
	
	public IgniteAtomicSequence missionSequence() {
		if (missionSequence == null)
			missionSequence = ignite.atomicSequence("autoMissionSequence", 0, true);
		
		return missionSequence;
	}
	
	public IgniteAtomicSequence missionContentsSequence() {
		if (missionContentsSequence == null)
			missionContentsSequence = ignite.atomicSequence("autoMissionContentSequence", 0, true);
		
		return missionContentsSequence;
	}
	
	public IgniteCache<String, MissionEntry> missionUidCache() {
		if (missionUidCache == null) {
			CacheConfiguration<String, MissionEntry> missionCfg = new CacheConfiguration<>("missionUidCache");
			missionCfg.setIndexedTypes(String.class, MissionEntry.class);
			missionUidCache = ignite.getOrCreateCache(missionCfg);
		}
		return missionUidCache;
	}

	public IgniteCache<String, MissionContentsEntry> missionContentsUidCache() {
		if (missionContentsUidCache == null) {
			CacheConfiguration<String, MissionContentsEntry> missionContentCfg = new CacheConfiguration<>("missionContentsUidCache");
			missionContentCfg.setIndexedTypes(String.class, MissionContentsEntry.class);
			missionContentsUidCache = ignite.getOrCreateCache(missionContentCfg);
		}
		return missionContentsUidCache;
	}
	
	
	@Override
	public List<ConnectionStatus> getActiveConnectionInfo() {
		List<ConnectionStatus> l = new LinkedList<>();
		
		getFederationSubscriptionCache().forEach(e -> {
			ConnectionStatus status = new ConnectionStatus(ConnectionStatusValue.CONNECTED, "", e.getValue().federate, e.getValue().connectionInfo);
			status.setConnection(e.getValue().connectionInfo);
			status.setFederate(e.getValue().federate);
			l.add(status);
		});
		
		return l;
	}
	
	@Override
	public void putUidToMission(UUID mission, String uid) {
		super.putUidToMission(mission, uid);
		MissionEntry missionEntry = new MissionEntry();
		missionEntry.clientUid = uid;
		
		// TODO fix this for mission name - need to track down usages of the name 
//		missionEntry.missionName = mission;
		missionEntry.missionGuid = mission;
		
		missionUidCache().putIfAbsent(mission+"-"+uid, missionEntry);
	}

	@Override
	public void removeUidByMission(UUID missionGuid, String uid) {
		super.removeUidByMission(missionGuid, uid);
		SqlFieldsQuery deleteQry = new SqlFieldsQuery("DELETE FROM MissionEntry me WHERE me.missionGuid=? AND me.clientUid=?");
		deleteQry.setArgs(missionGuid.toString(), uid);
		missionUidCache().query(deleteQry);
	}

	// This will return all UIDS across all nodes for the mission.
	@Override
	public Collection<String> getUidsByMission(UUID mission) {
		return getAllClientsForMission(mission);
	}
	
	// This will return only the UIDS local to this node. This is preferred over
	// getUidsByMission(String mission) unless you really need all the UIDS
	@Override
	public Collection<String> getLocalUidsByMission(UUID mission) {
		return super.getLocalUidsByMission(mission);
	}

	@Override
	// already taken care of by putUidToMission
	public void putMissionToUid(String uid, UUID mission) {}

	@Override
	// already taken care of by removeUidByMission
	public void removeMissionByUid(String uid, UUID missionGuid) {}

	@Override
	public Collection<UUID> getMissionsByUid(String uid) {
		return getAllMissionsForClient(uid);
	}
	
	@Override
	public void putUidToMissionContents(UUID missionGuid, String uid) {
		
		MissionContentsEntry missionContentsEntry = new MissionContentsEntry();
		missionContentsEntry.contentUid = uid;
		// TODO: missionName is not populated here. If needed, we can fetch it from MissionService
		//missionContentsEntry.missionName = mission;
				
		missionContentsUidCache().putIfAbsent(missionGuid+"-"+uid, missionContentsEntry);
	}

	@Override
	public void removeUidByMissionContents(UUID missionGuid, String uid) {
		SqlFieldsQuery deleteQry = new SqlFieldsQuery("DELETE FROM MissionContentsEntry mce WHERE mce.missionGuid=? AND mce.contentUid=?");
		deleteQry.setArgs(missionGuid.toString(), uid);
		missionContentsUidCache().query(deleteQry);
	}

	@Override
	public Collection<String> getUidsByMissionContents(UUID missionGuid) {
		return getAllContentsUidsForMission(missionGuid);
	}

	@Override
	// already taken care of by putUidToMissionContents
	public void putMissionToContentsUid(String uid, UUID missionGuid) {}

	@Override
	// already taken care of by removeUidByMissionContents
	public void removeMissionByContentsUid(String uid, UUID missionGuid) {}

	@Override
	public Collection<UUID> getMissionsByContentsUid(String uid) {
		return getAllMissionsForMissionContents(uid);
	}
	
	@Override
	public void removeMission(UUID missionGuid, Set<String> uids) {
		for (String contentUid : uids) {
			removeUidByMissionContents(missionGuid, contentUid);
		}
	}
	
	private static class MissionEntry implements Serializable {
		private static final long serialVersionUID = -5840505788889535916L;
		// check if Ignite supports native UUID type or not?
		@QuerySqlField(index = true) public UUID missionGuid;
		@QuerySqlField(index = true) public String missionName;
		@QuerySqlField(index = true) public String clientUid;
	}
	
	private Collection<UUID> getAllMissionsForClient(String clientUid) {
		Collection<UUID> missions = new ArrayList<>();

//		SqlFieldsQuery qry = new SqlFieldsQuery("select me.missionName from MissionEntry me WHERE me.clientUid=?");
//		qry.setArgs(clientUid);
//		
//		SqlFieldsQuery missionQry = qry;
//		try (QueryCursor<List<?>> cursor = missionUidCache().query(missionQry)) {
//			for (List<?> row : cursor) {
//				for (Object missionColumn : row) {
//					missions.add(missionColumn.toString());
//				}
//			}
//		}
		
		// TODO: does this work?
		SqlFieldsQuery qry = new SqlFieldsQuery("select me.missionGuid from MissionEntry me WHERE me.clientUid=?");
		qry.setArgs(clientUid);
		
		SqlFieldsQuery missionQry = qry;
		try (QueryCursor<List<?>> cursor = missionUidCache().query(missionQry)) {
			for (List<?> row : cursor) {
				for (Object missionColumn : row) {
					
					if (missionColumn == null) continue;
					
					UUID missionGuid = null;
					
					try {
						missionGuid = UUID.fromString(missionColumn.toString());
					} catch (IllegalArgumentException e) {
						logger.error("skipping invalid mission guid {}", missionColumn.toString());
					}
							
					missions.add(missionGuid);
				}
			}
		}
		
		return missions;
	}
	
	private Collection<String> getAllClientsForMission(UUID missionName) {	
		Collection<String> uids = new ArrayList<>();

//		SqlFieldsQuery qry = new SqlFieldsQuery("select me.clientUid from MissionEntry me WHERE me.missionName=?");
		SqlFieldsQuery qry = new SqlFieldsQuery("select me.clientUid from MissionEntry me WHERE me.missionGuid=?");
		qry.setArgs(missionName);
		
		SqlFieldsQuery missionQry = qry;
		try (QueryCursor<List<?>> cursor = missionUidCache().query(missionQry)) {
			for (List<?> row : cursor) {
				for (Object uidsColumn : row) {
					uids.add(uidsColumn.toString());
				}
			}
		}
		
		return uids;
	}
	
	private Collection<String> getAllLocalClientsForMission(UUID missionGuid) {
		Collection<String> uids = new ArrayList<>();
		
		SqlFieldsQuery qry = new SqlFieldsQuery("select me.clientUid from MissionEntry me inner join \""  + Constants.IGNITE_SUBSCRIPTION_CLIENTUID_TRACKER_CACHE +  "\".RemoteSubscription rs ON me.clientUid = rs._KEY WHERE me.missionGuid=? AND originNode=?");
		qry.setArgs(missionGuid.toString(), IgniteHolder.getInstance().getIgniteId());
		
		SqlFieldsQuery missionQry = qry;
		try (QueryCursor<List<?>> cursor = missionUidCache().query(missionQry)) {
			for (List<?> row : cursor) {
				for (Object uidsColumn : row) {
					uids.add(uidsColumn.toString());
				}
			}
		}
				
		return uids;
	}
	
	private static class MissionContentsEntry implements Serializable {
		private static final long serialVersionUID = 7974868084190100530L;
		@QuerySqlField(index = true) public String contentUid;
		@QuerySqlField(index = true) public String missionName;
	}
	
	private Collection<String> getAllContentsUidsForMission(UUID missionGuid) {
		Collection<String> uids = new ArrayList<>();

		SqlFieldsQuery qry = new SqlFieldsQuery("select mce.contentUid from MissionContentsEntry mce WHERE mce.missionGuid=?");
		qry.setArgs(missionGuid.toString());
		
		SqlFieldsQuery missionQry = qry;
		try (QueryCursor<List<?>> cursor = missionContentsUidCache().query(missionQry)) {
			for (List<?> row : cursor) {
				for (Object uidColumn : row) {
					uids.add(uidColumn.toString());
				}
			}
		}
		
		return uids;
	}
	
	private Collection<UUID> getAllMissionsForMissionContents(String uid) {	
		Collection<UUID> missionGuids = new ArrayList<>();

		SqlFieldsQuery qry = new SqlFieldsQuery("select mce.missionName from MissionContentsEntry mce WHERE mce.contentUid=?");
		qry.setArgs(uid);
		
		SqlFieldsQuery missionQry = qry;
		try (QueryCursor<List<?>> cursor = missionContentsUidCache().query(missionQry)) {
			for (List<?> row : cursor) {
				for (Object missionsColumn : row) {
					missionGuids.add(UUID.fromString((String) missionsColumn));
				}
			}
		}
		
		return missionGuids;
	}
}
