package tak.server.plugins;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.sync.model.Mission;

public interface PluginMissionApi {

	List<Mission> getAllMissions(boolean passwordProtected, boolean defaultRole, String tool) throws Exception;
	
	Mission readMission(String name, boolean changes, boolean logs, Long secago, Date start, Date end) throws Exception;

	Mission createMission(String nameParam, String creatorUidParam, String[] groupNamesParam, String descriptionParam,
		  	String chatRoomParam, String baseLayerParam, String bboxParam, List<String> boundingPolygonParam,
		  	String pathParam, String classificationParam, String toolParam, String passwordParam,
		  	String roleParam, Long expirationParam, byte[] missionPackage) throws Exception;

	Mission createMission(String nameParam, String creatorUidParam, String[] groupNamesParam, String descriptionParam,
			String chatRoomParam, String baseLayerParam, String bboxParam, List<String> boundingPolygonParam,
			String pathParam, String classificationParam, String toolParam, String passwordParam,
			String roleParam, Long expirationParam, Boolean inviteOnly, byte[] missionPackage) throws Exception;
	
	Mission deleteMission(String name, String creatorUid, boolean deepDelete) throws Exception;

	Mission addMissionContent(String name, MissionContent content, String creatorUid) throws Exception;

	Mission removeMissionContent(String name, String hash, String uid, String creatorUid) throws Exception;
	
	Mission clearKeywords(String name, String creatorUid);
	
	Mission setKeywords(String name, List<String> keywords, String creatorUid);
	
	Mission removeKeyword(String name, String keyword, String creatorUid);

	void setParent(String childName, String parentName) throws Exception;

	void clearParent(String childName) throws Exception;

	Set<Mission> getChildren(String parentName)throws Exception;

	Mission getParent(String parentName) throws Exception;
	
	void addFeed(String missionName, String creatorUid, String dataFeedUid, String filterBbox, List<String> filterCotTypes, String filterCallsign);

	void removeFeed(String missionName, String feedUid, String creatorUid);

	MapLayer createMapLayer(String missionName, String creatorUid, MapLayer mapLayer) throws Exception;

	void deleteMapLayer(String missionName, String creatorUid, String uid) throws Exception;

	MapLayer updateMapLayer(String missionName, String creatorUid, MapLayer mapLayer) throws Exception;

}
