package tak.server.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TakServerPlugin(
		name = "Data Feed Plugin Load Test",
		description = "Load Test Data Feed Plugin")
public class DataFeedMessageSenderPluginLoadTest extends MessageSenderBase {

	private static final Logger logger = LoggerFactory.getLogger(DataFeedMessageSenderPluginLoadTest.class);

	private int numberOfThreads = 1; // EXECUTOR_POOL_SIZE
	
	Set<String> groups;
	private List<String> feedUuids;

	private ScheduledFuture<?> future;
	private int numberOfFeeds = 2; // default value
	private int numberOfTracksPerFeed = 3; // default value
	

	private int delay = 10; // in milliseconds
	
	private ScheduledExecutorService worker;
	
	private AtomicInteger count;
	private long startTime;

	@SuppressWarnings("unchecked")
	public DataFeedMessageSenderPluginLoadTest() {

		if (config.containsProperty("groups")) {
			groups = new HashSet<String>((List<String>) config.getProperty("groups"));
		}
		
		if (config.containsProperty("numberOfFeeds")) {
			numberOfFeeds = (int)config.getProperty("numberOfFeeds");
		}
		
		if (config.containsProperty("numberOfTracksPerFeed")) {
			numberOfTracksPerFeed = (int)config.getProperty("numberOfTracksPerFeed");
		}
		
//		if (config.containsProperty("messageRatePerTrackInSecond")) {
//			messageRatePerTrackInSecond = (double)config.getProperty("messageRatePerTrackInSecond");
//		}				
//		period = (long)(1000L/messageRatePerTrackInSecond); // period is in MILLISECONDS
//		logger.info("Period in ms: {}", period);
		
		if (config.containsProperty("delay")) {
			delay = (int)config.getProperty("delay");
		}
		
		if (config.containsProperty("numberOfThreads")) {
			numberOfThreads = (int)config.getProperty("numberOfThreads");
		}
		
		worker = Executors.newScheduledThreadPool(numberOfThreads);
	
	}

	@Override
	public void start() {
		
		logger.info("Starting plugin " + getClass().getName());
//		logger.info("### Load test params: numberOfFeeds: {}, numberOfTracksPerFeed: {}, messageRatePerTrackInSecond: {}, numberOfThreads: {}", numberOfFeeds, numberOfTracksPerFeed, messageRatePerTrackInSecond, numberOfThreads);
		logger.info("### Load test params: numberOfFeeds: {}, numberOfTracksPerFeed: {}, delay: {}, numberOfThreads: {}", numberOfFeeds, numberOfTracksPerFeed, delay, numberOfThreads);

		feedUuids = new ArrayList<String>();
		count = new AtomicInteger(0);
		
		PluginDataFeedApi pluginDataFeedApi = getPluginDataFeedApi();

		// create new datafeeds
		for (int i = 0 ; i < numberOfFeeds; i++) {
//			String feedUuid = UUID.randomUUID().toString(); 
			String feedUuid = "feedUUID_loadtest_" + (i+1); // DataFeed UUIDs need to be known so that they can be added to missions in the client side
			String datafeedName = "loadTestPluginDataFeed_" + (i+1);
			List<String> tags = new ArrayList<>();
			tags.add("loadTest");
			logger.info("Creating new datafeed with uuid: {}", feedUuid);
			PluginDataFeed myPluginDataFeed = pluginDataFeedApi.create(feedUuid, datafeedName, tags);
			logger.info("Successfully created datafeed: {}", myPluginDataFeed.toString());
			
			feedUuids.add(feedUuid);
		}
		
		startTime = System.currentTimeMillis();
						
		future = worker.scheduleWithFixedDelay(new DataFeedMessageSender(), 3000, delay, TimeUnit.MILLISECONDS);
	}
	
	private class DataFeedMessageSender implements Runnable {

		@Override
		public void run() {
			
			String trackUid = "dummyTrackUid"; // this value will be replaced
			String SA = "<event version=\"2.0\" uid=\""+trackUid+"\" type=\"a-f-G-U-C\" how=\"m-g\" time=\"2020-02-12T13:16:07Z\" start=\"2020-02-12T13:16:05Z\" stale=\"2020-02-12T13:16:50Z\"><point lat=\"40.255716\" lon=\"-72.045306\" hae=\"-22.22983896651138\" ce=\"4.9\" le=\"9999999.0\"/><detail><__group name=\"Dark Blue\" role=\"Team Member\"/><precisionlocation geopointsrc=\"GPS\" altsrc=\"GPS\"/><status battery=\"32\"/><takv device=\"SAMSUNG SM-G975U1\" platform=\"ATAK-CIV\" os=\"29\" version=\"3.12.0-45691.45691-CIV\"/><track speed=\"0.0\" course=\"344.72362164876733\"/><contact endpoint=\"*:-1:stcp\" phone=\"19999999999\" callsign=\"coolata\"/><uid Droid=\"coolata\"/></detail></event>";
			
			int currentCount = count.incrementAndGet();

			try {
				// convert the CoT string into a Message
				Message message = getConverter().cotStringToDataMessage(SA, groups, Integer.toString(System.identityHashCode(this)));
				
				for (int feedIndex = 0; feedIndex < numberOfFeeds; feedIndex++) {
					
					for (int trackIndex = 0; trackIndex < numberOfTracksPerFeed; trackIndex++) {
						
						Message.Builder mb = message.toBuilder();

						mb.getPayloadBuilder().getCotEventBuilder().getDetailBuilder().getStatusBuilder().setBattery(currentCount);
						trackUid = "loadTest_" + "feed_" + feedIndex + "_" + "track_" + trackIndex;
						mb.getPayloadBuilder().getCotEventBuilder().setUid(trackUid);
						
						Message m = mb.build();
		 
						// Send messages to datafeed feedUuid
						String feedUuid = feedUuids.get(feedIndex);
						send(m, feedUuid);
						
						logger.debug("Sent message to datafeed UUID: {}, trackUid: {}, count: {}", feedUuid, trackUid, currentCount);	
						
					}
				}
				
				long numberOfMessagesSent = (long)numberOfFeeds * numberOfTracksPerFeed * currentCount;
				long timePassedInSecond = (System.currentTimeMillis() - startTime)/1000;
				logger.info("### Sent a total of {} messages in {} seconds, rate: {} messages/second", numberOfMessagesSent, timePassedInSecond, numberOfMessagesSent/timePassedInSecond);
				
			} catch (Exception e) {
				logger.error("Error in DataFeedMessageSender",e);	
				throw new RuntimeException(e);
			}
			
		}
		
	}
	
	private void deleteDataFeeds() {
		try {
			logger.info("Deleting datafeeds used in load testing");
			PluginDataFeedApi pluginDataFeedApi = getPluginDataFeedApi();
			for (String feedUuid : feedUuids) {
				pluginDataFeedApi.delete(feedUuid, new ArrayList<String>());
			}
			logger.info("Deleted all datafeeds used in load testing");

		} catch (Exception e) {
			logger.error("Error when deleting datafeeds", e);
		}
	}
	
	@Override
	public void stop() {
		
		if (future != null) {
			future.cancel(true);									
		}
		
		deleteDataFeeds();
	}
}
