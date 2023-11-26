package com.bbn.marti.feeds;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.feeds.DataFeed;
import tak.server.feeds.DataFeed.DataFeedType;
import tak.server.feeds.DataFeedDTO;
import tak.server.plugins.PredicateDataFeed;

/*
 */
public class DataFeedService {
	
	private static final Logger logger = LoggerFactory.getLogger(DataFeedService.class);
	
	private final DataSource dataSource;
	private final DataFeedRepository dataFeedRepository;
	
	private static DataFeedService dataFeedService;
	public static synchronized DataFeedService getDataFeedService() {
		if (dataFeedService != null) {
			return dataFeedService;
		}

		try {
			dataFeedService = SpringContextBeanForApi.getSpringContext().getBean(DataFeedService.class);
			return dataFeedService;
		} catch (Exception e) {
			logger.error("exception trying to get DataFeedService bean!", e);
			return null;
		}
	}
	
	public DataFeedService(DataSource dataSource, DataFeedRepository dataFeedRepository) {
		this.dataSource = dataSource;
		this.dataFeedRepository = dataFeedRepository;
	}
	
	@Cacheable(value = Constants.DATA_FEED_CACHE, key="{#root.methodName, #root.args[0]}", sync = true)
	public DataFeedDTO getDataFeedByUid(String feed_uid) {
		return dataFeedRepository.getDataFeedByUUID(feed_uid).stream().findFirst().orElse(null);
	}
	
	@Cacheable(value = Constants.DATA_FEED_CACHE, key="{#root.methodName, #root.args[0]}", sync = true)
	public DataFeedDTO getDataFeedById(Long feed_id) {
		return dataFeedRepository.getDataFeedById(feed_id).stream().findFirst().orElse(null);
	}
	
	@Cacheable(value = Constants.DATA_FEED_CACHE, key="{#root.methodName, #root.args[0]}", sync = true)
	public DataFeedDTO getDataFeedByName(String name) {
		return dataFeedRepository.getDataFeedByName(name).stream().findFirst().orElse(null);
	}
	
	@Cacheable(value = Constants.DATA_FEED_CACHE, key="{#root.methodName}", sync = true)
	public List<DataFeedDTO> getCachedDataFeeds() {
		return dataFeedRepository.getDataFeeds();
	}
	
	@Cacheable(value = Constants.DATA_FEED_CACHE, key="{#root.methodName, #root.args[0]}", sync = true)
	public List<String> getDataFeedTagsById(Long id) {
		return dataFeedRepository.getDataFeedTagsById(id);
	}
	
	@Cacheable(value = Constants.DATA_FEED_CACHE, key="{#root.methodName, #root.args[0]}", sync = true)
	public List<DataFeedDTO> getDataFeedsByGroup(String groupVector) {
		return dataFeedRepository.getDataFeedsByGroups(groupVector);
	}
	
	public List<String> getDataFeedsWithinBbox(double minLat, double maxLat, double minLong, double maxLong) {
		List<String> dataFeedsInBounds = new ArrayList<>();
				
		String minLatS = String.valueOf(minLat);
		String maxLatS = String.valueOf(maxLat);
		String minLongS = String.valueOf(minLong);
		String maxLongS = String.valueOf(maxLong);
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement("WITH feeds_in_bounds as (SELECT DISTINCT data_feed_cot.data_feed_id FROM data_feed_cot "
					+ "INNER JOIN cot_router ON cot_router.id=data_feed_cot.cot_router_id AND st_within(cot_router.event_pt, "
					+ "ST_GeomFromText('POLYGON((' || ? || '))', 4326))) "
					+ "SELECT data_feed.* FROM data_feed INNER JOIN feeds_in_bounds ON feeds_in_bounds.data_feed_id=data_feed.id;")) {
				
				String polyString = 
						  minLatS + " " + minLongS + "," 
						+ minLatS + " " + maxLongS + "," 
						+ maxLatS + " "  + maxLongS + ","
						+ maxLatS + " "  + minLongS + ","
						+ minLatS  + " "  +  minLongS;
				
				ps.setString(1, polyString);
				
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						String uuid = rs.getString("uuid");
						if (!Strings.isNullOrEmpty(uuid))
							dataFeedsInBounds.add(uuid);
					}
				}
			} catch (Exception e) {
				logger.info("Exception with query in getDataFeedsWithinBounds! " + e.getMessage(), e);
			}
		} catch (Exception e) {
			logger.info("Exception with connection in getDataFeedsWithinBounds! " + e.getMessage(), e);
		}
		
		return dataFeedsInBounds;
	}
	
	public List<String> getDataFeedsWithinPolyBounds(List<String> points) {
		List<String> dataFeedsInBounds = new ArrayList<>();
		// poly needs 3 points minimum
		if (points.size() < 3) {
			logger.info("Polygon requires at least 3 points. Found size " + points.size());
			return dataFeedsInBounds;
		}

		try {
			// check that all points are valid numbers
			for (String point : points) {
				String[] xy = point.split(",");
				if (xy.length != 2) {
					logger.info("Point is not in the format of <x>,<y>" + Arrays.deepToString(xy));
					return dataFeedsInBounds;
				}
				Double.parseDouble(xy[0]);
				Double.parseDouble(xy[1]);
 			}
		} catch (Exception e) {
			logger.error("Error parsing points for data feed bounds",e);
			return dataFeedsInBounds;
		}
		
		// first != last, so append first to end to close the poly		
		if (!points.get(0).replace(" ", "").equals(points.get(points.size()-1).replace(" ", ""))) {
			points.add(points.get(0));
		}
		
		String polyString = points.stream()
				.map(p->p.replace(" ", ""))
				.map(p->p.replace(",", " "))
				.collect(Collectors.joining(","));
				
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement("WITH feeds_in_bounds as (SELECT DISTINCT data_feed_cot.data_feed_id FROM data_feed_cot "
					+ "INNER JOIN cot_router ON cot_router.id=data_feed_cot.cot_router_id AND st_within(cot_router.event_pt, "
					+ "ST_GeomFromText('POLYGON((' || ? || '))', 4326))) "
					+ "SELECT data_feed.* FROM data_feed INNER JOIN feeds_in_bounds ON feeds_in_bounds.data_feed_id=data_feed.id;")) {
				
				ps.setString(1, polyString);
				
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						String uuid = rs.getString("uuid");
						if (!Strings.isNullOrEmpty(uuid))
							dataFeedsInBounds.add(uuid);
					}
				}
			} catch (Exception e) {
				logger.info("Exception with query in getDataFeedsWithinBounds! " + e.getMessage(), e);
			}
		} catch (Exception e) {
			logger.info("Exception with connection in getDataFeedsWithinBounds! " + e.getMessage(), e);
		}
		
		return dataFeedsInBounds;
	}
		 
    public DataFeed adaptDataFeedDTOtoDataFeed(DataFeedDTO feedDTO) {
		DataFeedType type = DataFeedType.values()[feedDTO.getType()];
		List<String> tags = dataFeedRepository.getDataFeedTagsById(feedDTO.getId());
		List<String> filterGroups = dataFeedRepository.getDataFeedFilterGroupsById(feedDTO.getId());
		AuthType auth = AuthType.valueOf(feedDTO.getAuth());

    	DataFeed dataFeed = new DataFeed(feedDTO.getUUID(), feedDTO.getName(), type,  new ArrayList<String>());

    	dataFeed.setAuth(auth);
    	dataFeed.setAnongroup(feedDTO.getAnongroup());
    	dataFeed.setAuthRequired(feedDTO.getAuthRequired());
    	dataFeed.setProtocol(feedDTO.getProtocol());
    	dataFeed.setGroup(feedDTO.getFeedGroup());
    	dataFeed.setIface(feedDTO.getIface());
    	dataFeed.setArchive(feedDTO.getArchive());
    	dataFeed.setAnongroup(feedDTO.getAnongroup());
    	dataFeed.setArchiveOnly(feedDTO.getArchiveOnly());
    	dataFeed.setCoreVersion(feedDTO.getCoreVersion().intValue());
    	dataFeed.setCoreVersion2TlsVersions(feedDTO.getCoreVersion2TlsVersions());
    	dataFeed.setSync(feedDTO.isSync());
    	dataFeed.setTags(tags);
    	dataFeed.setFilterGroups(filterGroups);
    	dataFeed.setSyncCacheRetentionSeconds(feedDTO.getSyncCacheRetentionSeconds());
    	dataFeed.setFederated(feedDTO.getFederated());
    	dataFeed.setBinaryPayloadWebsocketOnly(feedDTO.getBinaryPayloadWebsocketOnly());
    	dataFeed.setPredicateLang(feedDTO.getPredicateLang());
    	dataFeed.setPredicateDataSourceEndpoint(feedDTO.getDataSourceEndpoint());
    	dataFeed.setPredicate(feedDTO.getPredicate());
    	dataFeed.setPredicateAuthType(feedDTO.getAuthType());
    	
		if (feedDTO.getPort() != null && feedDTO.getPort() == 0) {
			dataFeed.setPort(null);
		} else {
			dataFeed.setPort(feedDTO.getPort());
		}

    	return dataFeed;
    }
    
    public PredicateDataFeed DataFeedDTOtoPredicateDataFeed(DataFeedDTO feedDTO) {
    	
    	if (feedDTO == null) {
    		throw new IllegalArgumentException("null feedDTO");
    	}
    	
    	DataFeedType type = DataFeedType.values()[feedDTO.getType()];
    	
    	if (!type.getClass().equals(DataFeedType.Predicate.getClass())) {
    		throw new IllegalArgumentException(type + " is not a " + PredicateDataFeed.class.getSimpleName());
    	}
    	
		List<String> tags = dataFeedRepository.getDataFeedTagsById(feedDTO.getId());
		List<String> filterGroups = dataFeedRepository.getDataFeedFilterGroupsById(feedDTO.getId());

    	PredicateDataFeed predDataFeed = new PredicateDataFeed();
    	
    	predDataFeed.setUuid(feedDTO.getUUID());
    	predDataFeed.setName(feedDTO.getName());
    	predDataFeed.setTags(tags);
    	predDataFeed.setFilterGroups(filterGroups);
    	predDataFeed.setArchive(feedDTO.getArchive());
    	predDataFeed.setSync(feedDTO.isSync());
    	predDataFeed.setTags(tags);
    	predDataFeed.setFilterGroups(filterGroups);
    	predDataFeed.setFederated(feedDTO.getFederated());
    	predDataFeed.setPredicateLang(feedDTO.getPredicateLang());
    	predDataFeed.setDataSourceEndpoint(feedDTO.getDataSourceEndpoint());
    	predDataFeed.setPredicate(feedDTO.getPredicate());

    	return predDataFeed;
    }
    
    public boolean isDataFeedDTOPredicateDataFeed(DataFeedDTO feedDTO) {
    	
    	DataFeedType type = DataFeedType.values()[feedDTO.getType()];
    	
    	return type.getClass().equals(DataFeedType.Predicate.getClass());
    	
    }

	public List<String> getCotsForDataFeedByCotType(String dataFeedUuid, String cotType) throws SQLException {
		
		List<String> cotUUIDs = new ArrayList<>();
		
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement("SELECT cot_router.uid from cot_router "
					+ "INNER JOIN data_feed_cot ON cot_router.id = data_feed_cot.cot_router_id "
					+ "INNER JOIN data_feed ON data_feed_cot.data_feed_id = data_feed.id "
					+ "WHERE data_feed.uuid = ? AND cot_router.cot_type = ? ;")) {
				
				ps.setString(1, dataFeedUuid);
				ps.setString(2, cotType);

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						cotUUIDs.add(rs.getString(1));
					}
				}
			} 
		}
		
		return cotUUIDs;
		
	}
	
	public List<String> getExistingCotTypesForDataFeed(String dataFeedUuid) throws SQLException {
		
		List<String> cotTypes = new ArrayList<>();
		
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement ps = connection.prepareStatement("SELECT DISTINCT cot_router.cot_type from cot_router "
					+ "INNER JOIN data_feed_cot ON cot_router.id = data_feed_cot.cot_router_id "
					+ "INNER JOIN data_feed ON data_feed_cot.data_feed_id = data_feed.id "
					+ "WHERE data_feed.uuid = ? ;")) {
				
				ps.setString(1, dataFeedUuid);

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						cotTypes.add(rs.getString(1));
					}
				}
			} 
		} 
		
		return cotTypes;
		
	}
	
}
