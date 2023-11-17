package com.bbn.marti.feeds;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;

import tak.server.Constants;
import tak.server.feeds.DataFeed;
import tak.server.feeds.DataFeedStats;
import tak.server.feeds.DataFeedStatsHelper;

/*
 */
public class DataFeedApi extends BaseRestController {
	Logger logger = LoggerFactory.getLogger(DataFeedApi.class);

	@Autowired
	DataFeedService dataFeedService;

	@RequestMapping(value = "/datafeeds/bounds/{bbox}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<String>>> getDataFeedsInBbox(@PathVariable("bbox") String bbox) {
		List<String> dataFeeds = new ArrayList<>();
		try {
			String[] bboxArr = bbox.split(",");
			if (bboxArr.length == 4)  {
				double maxLat, minLat, maxLong, minLong;
				maxLat = Double.valueOf(bboxArr[0]);
				minLong = Double.valueOf(bboxArr[1]);
				minLat = Double.valueOf(bboxArr[2]);
				maxLong = Double.valueOf(bboxArr[3]);
				
				dataFeeds = dataFeedService.getDataFeedsWithinBbox(minLat, maxLat, minLong, maxLong);
			}
		} catch (Exception e) {
			logger.error("Failed getting data feeds", e);
			return new ResponseEntity<ApiResponse<List<String>>>(
					new ApiResponse<List<String>>(Constants.API_VERSION, DataFeed.class.getName(), null),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<ApiResponse<List<String>>>(
				new ApiResponse<List<String>>(Constants.API_VERSION, DataFeed.class.getName(), dataFeeds),
				HttpStatus.OK);
	}
	
	@RequestMapping(value = "/datafeeds/bounds/polygon", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<String>>> getDataFeedsInPolygon(@RequestBody List<String> points) {
		List<String> dataFeeds = new ArrayList<>();
		try {
			dataFeeds = dataFeedService.getDataFeedsWithinPolyBounds(points);
		} catch (Exception e) {
			logger.error("Failed getting data feeds", e);
			return new ResponseEntity<ApiResponse<List<String>>>(
					new ApiResponse<List<String>>(Constants.API_VERSION, DataFeed.class.getName(), null),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<ApiResponse<List<String>>>(
				new ApiResponse<List<String>>(Constants.API_VERSION, DataFeed.class.getName(), dataFeeds),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/datafeeds/stats/{uuid}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<DataFeedStats>> getStatsForDataFeed(@PathVariable("uuid") String uuid) {
		DataFeedStatsHelper dataFeedStatsHelper = DataFeedStatsHelper.getInstance();
		DataFeedStats dataFeedStats = null;
		HttpStatus hs = HttpStatus.OK;
		try {
   			    dataFeedStats = dataFeedStatsHelper.getLatestDataFeedStats(uuid);
		} catch (Exception e) {
			hs = HttpStatus.INTERNAL_SERVER_ERROR;
			logger.error("Failed getting data feeds", e);
		}

		return new ResponseEntity<ApiResponse<DataFeedStats>>(
				new ApiResponse<DataFeedStats>(Constants.API_VERSION, DataFeedStats.class.getName(), dataFeedStats),
				hs);
	}

	@RequestMapping(value = "/datafeeds/stats", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<DataFeedStats>>> getStatsForDataFeed() {
		DataFeedStatsHelper dataFeedStatsHelper = DataFeedStatsHelper.getInstance();
		List<DataFeedStats> dataFeedStatsList = null;
		HttpStatus hs = HttpStatus.OK;

		try {
			dataFeedStatsList = dataFeedStatsHelper.getAllLatestDataFeedStats();
		} catch (Exception e) {
			hs = HttpStatus.INTERNAL_SERVER_ERROR;
			logger.error("Failed getting data feeds", e);
		}

		return new ResponseEntity<ApiResponse<List<DataFeedStats>>>(
				new ApiResponse<List<DataFeedStats>>(Constants.API_VERSION, DataFeedStats.class.getName(), dataFeedStatsList),
				hs);
	}

}
