package tak.server.feeds;

import java.util.List;

/*
 * 
 * Model class representing a generic TAK Server data feed
 * 
 */
public class StreamingDataFeed extends DataFeed {

	public StreamingDataFeed(String uuid, String name, DataFeedType type, List<String> tags) {
		super(uuid, name, type, tags);
	}
	
	public StreamingDataFeed(com.bbn.marti.config.DataFeed datafeed) {
		super(datafeed);
	}
}
