package com.bbn.marti.feeds;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.ForbiddenException;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.util.CommonUtil;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.feeds.DataFeed;
import tak.server.feeds.DataFeed.DataFeedType;
import tak.server.feeds.DataFeedDTO;
import tak.server.feeds.DataFeedStats;
import tak.server.feeds.DataFeedStatsHelper;
import tak.server.plugins.PredicateDataFeed;

/*
 * API for managing datafeeds.
 * 
 * @See com.bbn.marti.network.SubmissionApi for related datafeed endpoint that are input-related
 * 
 */
@RestController
public class DataFeedApi extends BaseRestController {
	Logger logger = LoggerFactory.getLogger(DataFeedApi.class);

	@Autowired
	DataFeedService dataFeedService;
	
	@Autowired
	private CommonUtil commonUtil;
	
    @Autowired
    DataFeedRepository dataFeedRepository;
    
    @Autowired
    GroupManager groupManager;
    
    @Autowired
    RemoteUtil remoteUtil;
    
    // keep a reference to the currently active request
    @Autowired
    private HttpServletRequest request;

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
	
	@RequestMapping(value = "/datafeeds", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<DataFeed>>> getDataFeeds(HttpServletResponse response) {
		
		logger.debug("GET all {}", "datafeeds");
		
		setCacheHeaders(response);

		List<DataFeed> dataFeeds = new ArrayList<>();
		
		final String groupVector = commonUtil.getGroupVectorBitString(request);

		try {
			
			logger.debug("all feeds by group query {}", DataFeedRepository.ALL_FEEDS_BY_GROUP);
			
			List<DataFeedDTO> dataFeedDTOs = dataFeedRepository.getDataFeedsByGroups(groupVector);
			
			logger.debug("GET datafeeds by group result {}", dataFeedDTOs);

			if (dataFeedDTOs != null) {
				for (DataFeedDTO dto : dataFeedDTOs) {
					DataFeed dataFeed = dataFeedService.adaptDataFeedDTOtoDataFeed(dto);
					dataFeeds.add(dataFeed);
				}
			}
		} catch (Exception e) {
			logger.error("Exceptiong getting all data feeds", e);
			return new ResponseEntity<ApiResponse<List<DataFeed>>>(new ApiResponse<List<DataFeed>>(Constants.API_VERSION, DataFeed.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<ApiResponse<List<DataFeed>>>(new ApiResponse<List<DataFeed>>(Constants.API_VERSION, DataFeed.class.getName(), dataFeeds), HttpStatus.OK);
	}
	
	/* 
	 * create a new predicate datafeed (async)
	 * 
	 * Guid will be randomly assigned. Guid field in request JSON will be ignored. Duplicate names allowed.
	 *
	 */
	@RequestMapping(value = "/datafeeds/predicate", method = RequestMethod.POST)
	public Callable<ApiResponse<PredicateDataFeed>> createPredicateDataFeed(@RequestBody PredicateDataFeed pfeed) {
		
		final String requestGroupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groupManager.findGroups(pfeed.getFilterGroups())));
	
		return () -> {
			
			try {
			
			if (Strings.isNullOrEmpty(pfeed.getName()) || pfeed.getDataSourceEndpoint() == null) {
				throw new IllegalArgumentException("empty name or datasource endpoint URL in create predicate data feed request.");
			}
			
			pfeed.setUuid(UUID.randomUUID().toString());
			
			// Always assign UUID on create.
			Long feedId = dataFeedRepository.addDataFeed(
					pfeed.getUuid(),
					pfeed.getName(),
					DataFeedType.Predicate.ordinal(),
					AuthType.X_509.toString(),
					null,
					false,
					null,
					null,
					null,
					false,
					false,
					false,
					0,
					null,
					false,
					0,
					requestGroupVector,
					false,
					false,
					pfeed.getPredicateLang(),
					pfeed.getDataSourceEndpoint(),
					pfeed.getPredicate(),
					pfeed.getAuthType());
			
			logger.debug("predicate feed id {}", feedId);
			
			if (pfeed.getTags() != null && !pfeed.getTags().isEmpty()) {
        		dataFeedRepository.addDataFeedTags(feedId, pfeed.getTags().toArray(String[]::new));
        	}
			
			 // Needed for permissions to access data feed
	        if (pfeed.getFilterGroups().isEmpty()) {
	        	pfeed.getFilterGroups().add(Constants.ANON_GROUP);
	        }
			
			// default feed to __ANON__ group if none specified
			
	        dataFeedRepository.addDataFeedFilterGroups(feedId, pfeed.getFilterGroups().toArray(String[]::new));
			
			logger.info("create new predicate-based data feed: {}", pfeed);

			return new ApiResponse<PredicateDataFeed>(Constants.API_VERSION, PredicateDataFeed.class.getSimpleName(), pfeed);
			
			} catch (Exception e) {
				logger.error("exception saving predicate data feed", e);
				throw e;
			}
		};
	}
	
	/* 
	 * update an existing predicate datafeed (async)
	 * 
	 * If a feed with the given guid doesn't exist, respond with an error
	 *
	 */
	@RequestMapping(value = "/datafeeds/predicate", method = RequestMethod.PUT)
	public Callable<ApiResponse<PredicateDataFeed>> updatePredicateDataFeed(@RequestBody PredicateDataFeed pfeed, @RequestParam(value = "updateGroups", defaultValue = "false") Boolean updateGroups) {
			
		final String requestGroupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(commonUtil.getGroupsFromActiveRequest()));

		return () -> {
			
			if (Strings.isNullOrEmpty(pfeed.getUuid())) {
				throw new IllegalArgumentException("predicate feed in request has empty UUID");
			}
			
			if (!dataFeedRepository.doesFeedExist(pfeed.getUuid())) {
				throw new IllegalArgumentException("can't update non-existent feed with uuid " + pfeed.getUuid() + ". Use POST to create feed.");
			}
			
			List<DataFeedDTO> feeds = dataFeedRepository.getDataFeedByUUID(pfeed.getUuid());
			
			if (feeds.isEmpty()) {
				throw new IllegalArgumentException("data feed for uuid {} " + pfeed.getUuid() + " not found");
			}
			
			logger.debug("feedGroupVector {}", feeds.get(0).getGroupVector());
			
			// check if groups are allowed
			if (!remoteUtil.isGroupVectorAllowed(requestGroupVector, feeds.get(0).getGroupVector())) {
				throw new ForbiddenException("Group access denied");
			}
			
			// only update groups if specified. Log the change.
			logger.info("update predicate-based data feed: {} updateGroups: {}", pfeed, updateGroups);

			if (updateGroups) {
				logger.debug("update feed {} with groups", pfeed.getUuid());
						dataFeedRepository.updateDataFeedWithGroupVector(
						pfeed.getUuid(),
						pfeed.getName(),
						DataFeedType.Predicate.ordinal(),
						AuthType.X_509.toString(),
						null,
						false,
						null,
						null,
						null,
						false,
						false,
						false,
						0,
						null,
						false,
						0,
						requestGroupVector,
						false,
						false,
						pfeed.getPredicateLang(),
						pfeed.getDataSourceEndpoint(),
						pfeed.getPredicate(),
						pfeed.getAuthType());
			} else {
				logger.debug("update feed {} without groups", pfeed.getUuid());
				dataFeedRepository.updateDataFeed(
						pfeed.getUuid(),
						pfeed.getName(),
						DataFeedType.Predicate.ordinal(),
						AuthType.X_509.toString(),
						null,
						false,
						null,
						null,
						null,
						false,
						false,
						false,
						0,
						null,
						false,
						0,
						false,
						false,
						pfeed.getPredicateLang(),
						pfeed.getDataSourceEndpoint(),
						pfeed.getPredicate(),
						pfeed.getAuthType());
			}
			
			return new ApiResponse<PredicateDataFeed>(Constants.API_VERSION, PredicateDataFeed.class.getSimpleName(), pfeed);
		};
	}
	
	/* 
	 * delete a predicate datafeed datafeed (async)
	 * 
	 * If a feed with the given guid doesn't exist, respond with an error
	 *
	 */
	@RequestMapping(value = "/datafeeds/predicate/{feedGuid:.+}", method = RequestMethod.DELETE)
	public Callable<ApiResponse<PredicateDataFeed>> deletePredicateDataFeed(@PathVariable("feedGuid") String feedUuid) {
		
		final String requestGroupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(commonUtil.getGroupsFromRequest(request)));

		return () -> {
			
			if (Strings.isNullOrEmpty(feedUuid)) {
				throw new IllegalArgumentException("predicate feed in request has empty uuid");
			}
			
			if (!dataFeedRepository.doesFeedExist(feedUuid)) {
				throw new IllegalArgumentException("can't delete feed " + feedUuid + " - does not exist.");
			}
			
			List<DataFeedDTO> dfdtos = dataFeedRepository.getDataFeedByUUID(feedUuid);
			
			if (dfdtos.isEmpty()) {
				throw new IllegalStateException("No feeds exist for uuid " + feedUuid);
			}
			
			DataFeedDTO feed = dfdtos.get(0);
			
			// check if groups are allowed
			if (!remoteUtil.isGroupVectorAllowed(requestGroupVector, feed.getGroupVector())) {
				throw new ForbiddenException("Group access denied for feed " + feedUuid + " deletion");
			}

			logger.info("deleting predicate-based data feed by uuid: {}", feedUuid);
			
			dataFeedRepository.deleteDataFeedByUuid(feedUuid);
			
			return new ApiResponse<PredicateDataFeed>(Constants.API_VERSION, PredicateDataFeed.class.getSimpleName(), dataFeedService.DataFeedDTOtoPredicateDataFeed(feed));
		};
	}
	
	/* 
	 * get a predicate datafeed by guid (async)
	 * 
	 *
	 */
	@RequestMapping(value = "/datafeeds/predicate/{feedUuid:.+}", method = RequestMethod.GET)
	public Callable<ApiResponse<PredicateDataFeed>> getPredicateDataFeedByGuid(@PathVariable("feedUuid") String feedUuid) {

		return () -> {

			final String requestGroupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(commonUtil.getGroupsFromRequest(request)));


			if (Strings.isNullOrEmpty(feedUuid)) {
				throw new IllegalArgumentException("predicate feed in request has empty uuid");
			}

			if (!dataFeedRepository.doesFeedExist(feedUuid)) {
				throw new IllegalArgumentException("can't delete feed " + feedUuid + " - does not exist.");
			}

			List<DataFeedDTO> dfdtos = dataFeedRepository.getDataFeedByUUID(feedUuid);

			if (dfdtos.isEmpty()) {
				throw new IllegalStateException("No feed exists for uuid " + feedUuid);
			}
			
			DataFeedDTO feed = dfdtos.get(0);
			
			// check if groups are allowed
			if (!remoteUtil.isGroupVectorAllowed(requestGroupVector, feed.getGroupVector())) {
				throw new ForbiddenException("Group access denied for feed " + feedUuid);
			}
			
			if (!dataFeedService.isDataFeedDTOPredicateDataFeed(feed)) {
				throw new IllegalArgumentException("feed for uuid " + feedUuid + " is not a PredicateDataFeed");
			}

			return new ApiResponse<PredicateDataFeed>(Constants.API_VERSION, PredicateDataFeed.class.getSimpleName(), dataFeedService.DataFeedDTOtoPredicateDataFeed(feed));
		};
	}

	@RequestMapping(value = "/datafeeds/{uuid}/cots/{cot_type}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<String>>> getCotsForDataFeedByCotType(@PathVariable("uuid") String uuid, @PathVariable("cot_type") String cotType){
		List<String> cots = new ArrayList<>();
		try {
			cots = dataFeedService.getCotsForDataFeedByCotType(uuid, cotType);
		} catch (Exception e) {
			logger.error("Failed cot type filtering for data feeds", e);
			return new ResponseEntity<ApiResponse<List<String>>>(
					new ApiResponse<List<String>>(Constants.API_VERSION, DataFeed.class.getName(), null),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<ApiResponse<List<String>>>(
				new ApiResponse<List<String>>(Constants.API_VERSION, DataFeed.class.getName(), cots),
				HttpStatus.OK);
	}
	
	@RequestMapping(value = "/datafeeds/{uuid}/cots_types", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<String>>> getExistingCotTypesForDataFeed(@PathVariable("uuid") String uuid){
		List<String> cotTypes = new ArrayList<>();
		try {
			cotTypes = dataFeedService.getExistingCotTypesForDataFeed(uuid);
		} catch (Exception e) {
			logger.error("Failed getting existing cot types for data feeds", e);
			return new ResponseEntity<ApiResponse<List<String>>>(
					new ApiResponse<List<String>>(Constants.API_VERSION, DataFeed.class.getName(), null),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<ApiResponse<List<String>>>(
				new ApiResponse<List<String>>(Constants.API_VERSION, DataFeed.class.getName(), cotTypes),
				HttpStatus.OK);
	}

}
