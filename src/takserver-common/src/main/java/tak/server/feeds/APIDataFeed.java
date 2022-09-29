package tak.server.feeds;

import java.util.List;

public class APIDataFeed extends DataFeed {
	
	public APIDataFeed(String uuid, String name, DataFeedType type, List<String> tags) {
		super(uuid, name, type, tags);
	}
	
	public APIDataFeed(com.bbn.marti.config.DataFeed datafeed) {
		super(datafeed);
	}

}
