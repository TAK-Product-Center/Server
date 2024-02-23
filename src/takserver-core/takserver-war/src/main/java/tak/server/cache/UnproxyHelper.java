package tak.server.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.hibernate.Hibernate;

import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.sync.model.ExternalMissionData;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.sync.model.Resource;

public class UnproxyHelper {
	
	public static void unproxyMission(Mission mission) {
		
		Set<ExternalMissionData> unproxiedExtDataSet = getUnproxySet(mission.getExternalData());
		mission.setExternalData(unproxiedExtDataSet);
		
		Set<MapLayer> unproxiedMapLayers = getUnproxySet(mission.getMapLayers());
		mission.setMapLayers(unproxiedMapLayers);
		
		Set<MissionFeed> unproxyMissionFeed = getUnproxySet(mission.getFeeds());
		mission.setFeeds(unproxyMissionFeed);
		
		Set<String> unproxiedKeywords = getUnproxySet(mission.getKeywords());
		mission.setKeywords(unproxiedKeywords);
		
		Set<Mission> unproxiedChildren = getUnproxySet(mission.getChildren());
		mission.setChildren(unproxiedChildren);
		
		Set<Resource> unproxiedContents = getUnproxySet(mission.getContents());
		mission.setContents(unproxiedContents);
		
		Set<MissionChange> unproxiedMissionChanges = getUnproxySet(mission.getMissionChanges());
		mission.setMissionChanges(unproxiedMissionChanges);
		
		Set<String> unproxiedUids = getUnproxySet(mission.getUids());
		mission.setUids(unproxiedUids);
		
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<T> getUnproxySet(Set<T> set) {
			
		Set<T> unproxiedSet = new ConcurrentSkipListSet<>();
		
		if (set != null) {
			for (T element : set) {
				unproxiedSet.add((T)Hibernate.unproxy(element));
			}
		}
				
		return unproxiedSet;

	}
}
