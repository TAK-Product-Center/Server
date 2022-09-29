package tak.server.feeds;

import java.util.List;

public class PluginDataFeed extends DataFeed {
	
	public PluginDataFeed(String uuid, String name, DataFeedType type, List<String> tags) {
		super(uuid, name, type, tags);
	}
	
	public PluginDataFeed(com.bbn.marti.config.DataFeed datafeed) {
		super(datafeed);
	}

}
