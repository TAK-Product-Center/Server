package tak.server.cache;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.TakException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import tak.server.plugins.PluginDataFeed;

public class PluginDatafeedCacheHelper {

    private static final Logger log = LoggerFactory.getLogger(PluginDatafeedCacheHelper.class);

	private Cache<String, List<PluginDataFeed>> pluginDatafeedCache;

	public static final String ALL_PLUGIN_DATAFEED_KEY = "ALL_PLUGIN_DATAFEED_KEY";

	@Autowired
	private CoreConfig config;
	
	private boolean init = false;

	@EventListener({ContextRefreshedEvent.class})
	public void init() {

		if (log.isDebugEnabled()) {
			log.debug("in init");
		}
		
		pluginDatafeedCache = Caffeine.newBuilder()
				  .expireAfterWrite(config.getCachedConfiguration().getBuffer().getQueue().getPluginDatafeedCacheSeconds(), TimeUnit.SECONDS)
				  .build();
		
		log.info("Done initializing PluginDatafeedCacheHelper with buffer cache value: {} seconds", String.valueOf(config.getCachedConfiguration().getBuffer().getQueue().getPluginDatafeedCacheSeconds()));
		
		init = true;
	}

	public void clearCache() {

		try {
			
			if (pluginDatafeedCache != null) {
				pluginDatafeedCache.invalidateAll();
			}
		} catch (Exception e) {
			throw new TakException(e);
		} 
	}

	public Cache<String, List<PluginDataFeed>> getPluginDatafeedCache() {
		if (!init) {
			throw new TakException("plugin data feed cache not initialized");
		}
		
		return pluginDatafeedCache;
	}

	public List<PluginDataFeed> getPluginDatafeed(String feedUuid) {
		return getPluginDatafeedCache().getIfPresent(feedUuid);
	}
	
	public void cachePluginDatafeed(String feedUuid, List<PluginDataFeed> pluginDatafeed) {
		getPluginDatafeedCache().put(feedUuid, pluginDatafeed);
	}
	
	public List<PluginDataFeed> getAllPluginDatafeeds() {
		return getPluginDatafeedCache().getIfPresent(ALL_PLUGIN_DATAFEED_KEY);
	}
	
	public void cacheAllPluginDatafeeds(List<PluginDataFeed> allPluginDatafeeds) {
		getPluginDatafeedCache().put(ALL_PLUGIN_DATAFEED_KEY, allPluginDatafeeds);
	}
	
	public void invalidate(String key) {
		getPluginDatafeedCache().invalidate(key);
	}

}
