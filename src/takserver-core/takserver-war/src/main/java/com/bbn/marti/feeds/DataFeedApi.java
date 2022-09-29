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

}
