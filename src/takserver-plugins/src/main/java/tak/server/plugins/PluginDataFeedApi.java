package tak.server.plugins;

import java.util.Collection;
import java.util.List;

public interface PluginDataFeedApi {
	
	  PluginDataFeed create(String uuid, String name, List<String> tags, boolean archive, boolean sync);
	  PluginDataFeed create(String uuid, String name, List<String> tags);
	  void delete(String uuid);
	  Collection<PluginDataFeed> getAllPluginDataFeeds();
	  
}
