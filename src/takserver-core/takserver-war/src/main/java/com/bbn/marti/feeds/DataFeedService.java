package com.bbn.marti.feeds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.base.Strings;

/*
 */
public class DataFeedService {
	
	private static final Logger logger = LoggerFactory.getLogger(DataFeedService.class);

	private final DataSource dataSource;
	private final DataFeedRepository dataFeedRepository;
	
	private static DataFeedService dataFeedService;
	private synchronized DataFeedService getDataFeedService() {
		if (dataFeedService != null) {
			return dataFeedService;
		}

		try {
			dataFeedService = SpringContextBeanForApi.getSpringContext().getBean(DataFeedService.class);
			return dataFeedService;
		} catch (Exception e) {
			logger.error("exception trying to get DataFeedService bean!", e);
			return this;
		}
	}
	
	public DataFeedService(DataSource dataSource, DataFeedRepository dataFeedRepository) {
		this.dataSource = dataSource;
		this.dataFeedRepository = dataFeedRepository;
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
}
