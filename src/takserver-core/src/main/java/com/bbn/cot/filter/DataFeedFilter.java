package com.bbn.cot.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.locationtech.jts.geom.Polygon;

import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.GeospatialFilter;
import com.bbn.marti.config.GeospatialFilter.BoundingBox;
import com.bbn.marti.util.GeomUtils;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;

/*
 *  
 */
public class DataFeedFilter {
	
	private static DataFeedFilter instance = null;
		
	public static DataFeedFilter getInstance() {
		if (instance == null) {
			synchronized (DataFeedFilter.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(DataFeedFilter.class);
				}
			}
		}
		return instance;
	}

	public void filter(CotEventContainer cot, DataFeed dataFeed) {
		
		if (cot != null && dataFeed != null) {
			Element detailElem = cot.getDocument().getRootElement().element("detail");
			if (detailElem == null) {
				detailElem = DocumentHelper.makeElement(cot.getDocument().getRootElement(), "detail");
            }

			Element sourceElement = DocumentHelper.makeElement(detailElem, "source");
			sourceElement.addAttribute("type", "dataFeed");
			sourceElement.addAttribute("name", dataFeed.getName());
			sourceElement.addAttribute("uid", dataFeed.getUuid());
			
			dataFeed.getTag().forEach(tag-> {
				Element tagElement = DocumentHelper.createElement("tag");
				tagElement.addText(tag);
				sourceElement.add(tagElement);
			});
						
			// if vbm is enabled, only broker messages to clients subscribed to a mission that is linked to this data feed			
			if (DistributedConfiguration.getInstance().getRemoteConfiguration().getVbm().isEnabled()) {
				// get missions associated with this data feed
				List<Mission> feedMissions = MessagingDependencyInjectionProxy.getInstance().missionService().getMissionsForDataFeed(dataFeed.getUuid());
				
				// figure out which missions should filter CoT based on bounding box
				List<Mission> feedMissionsInCotBbox = new ArrayList<>();		
				for (Mission mission : feedMissions) {
					// no bbox, allow mission				
					if (Strings.isNullOrEmpty(mission.getBbox()) && Strings.isNullOrEmpty(mission.getBoundingPolygon())) {
						feedMissionsInCotBbox.add(mission);
					} 
					// use polygon over bbox					
					else if (!Strings.isNullOrEmpty(mission.getBoundingPolygon())) {
						Polygon polygon = GeomUtils.postgisBoundingPolygonToPolygon(mission.getBoundingPolygon());
						// valid bounding box
						if (polygon != null) {
							double latitude = Double.parseDouble(cot.getLat());
					        double longitude = Double.parseDouble(cot.getLon());
					        
							// if we received back non null, cot passed the filter
							if (GeomUtils.polygonContainsCoordinate(polygon, latitude, longitude)) {
								feedMissionsInCotBbox.add(mission);
							} 
						}
					} 
					// fallback to bbox						
					else {
						BoundingBox boundingBox = getBoundingBoxFromBboxString(mission.getBbox());
						// valid bounding box
						if (boundingBox != null) {
							GeospatialFilter gf = new GeospatialFilter();
							gf.getBoundingBox().add(boundingBox);
							GeospatialEventFilter gef = new GeospatialEventFilter(gf, true, false);
							CotEventContainer c = gef.filter(cot);
							// if we received back non null, cot passed the filter
							if (c != null) {
								feedMissionsInCotBbox.add(mission);
							} 
						}
					}
				}
				
				// Collect all the mission subscriber uids for valid feed missions
				List<String> feedMissionClients = feedMissionsInCotBbox
					.stream()
					.map(mission -> mission.getName())
					.map(missionName -> SubscriptionStore.getInstance().getLocalUidsByMission(missionName)) 
					.flatMap(clientUids -> clientUids.stream())
					.distinct()
					.collect(Collectors.toList());
				
				// by adding explicit UIDs, this CoT event will go into explicit brokering rather than implicit				
				cot.setContextValue(StreamingEndpointRewriteFilter.EXPLICIT_FEED_UID_KEY, feedMissionClients);
			} 
			
			cot.setContext(Constants.DATA_FEED_KEY, dataFeed);
		}
	}

	// compute bbox from string and cache it for instant lookup next time
	BoundingBox getBoundingBoxFromBboxString(String bbox) {

		BoundingBox cachedBoundingBox = cache().getIfPresent(bbox);
		if (cachedBoundingBox != null) {
			return cachedBoundingBox;
		}

		BoundingBox boundingBox = GeomUtils.getBoundingBoxFromBboxString(bbox);

		cache().put(bbox, boundingBox);

		return boundingBox;
	}

	private Cache<String, BoundingBox> cache;
	private Cache<String, BoundingBox> cache() {
		if (cache == null) {
			synchronized (this) {
				if (cache == null) {
					Caffeine<Object, Object> builder = Caffeine.newBuilder();
					cache = builder.maximumSize(100).build();
				}
			}
		}
		return cache;
	}
}
