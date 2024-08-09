package tak.server.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.config.DataFeed;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import tak.server.cot.CotEventContainer;

public class DataFeedCotCacheHelper {
	private static final Logger logger = Logger.getLogger(DataFeedCotCacheHelper.class);

	private volatile static DataFeedCotCacheHelper instance;
	public static DataFeedCotCacheHelper getInstance() {
		if (instance == null) {
			synchronized (DataFeedCotCacheHelper.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(DataFeedCotCacheHelper.class);
				}
			}
		}
		return instance;
	}

	private volatile Map<String, Cache<String, CotEventContainer>> dataFeedCaches = new HashMap<>();
	private Cache<String, CotEventContainer> latestSACacheForDataFeed(DataFeed dataFeed) {
		Cache<String, CotEventContainer> cache = dataFeedCaches.get(dataFeed.getUuid());
		if (cache == null) {
			synchronized (this) {
				if (cache == null) {
					Caffeine<Object, Object> builder = Caffeine.newBuilder();
					cache = builder.expireAfterWrite(dataFeed.getSyncCacheRetentionSeconds(), TimeUnit.SECONDS).build();

					dataFeedCaches.put(dataFeed.getUuid(), cache);
				}
			}
		}

		return cache;
	}

	public void cacheDataFeedEvent(DataFeed dataFeed, CotEventContainer data) {
		if (dataFeed.isSync() && CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getLatestSA().isEnable()) {

			Cache<String, CotEventContainer> cache = latestSACacheForDataFeed(dataFeed);
			cache.put(data.getUid(), data);
		}
	}

	public Collection<CotEventContainer> getCachedDataFeedEvents(String dataFeedUid) {
		Cache<String, CotEventContainer> cache = dataFeedCaches.get(dataFeedUid);

		if (cache == null) {
			return new ArrayList<>();
		} else {
			// deep copy
			return cache.asMap().values().stream().map(CotEventContainer::copy).collect(Collectors.toList());
		}
	}
}
