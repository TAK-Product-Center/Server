package tak.server.messaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.network.PluginDataFeedJdbc;
import com.bbn.marti.remote.groups.NetworkInputAddResult;
import com.bbn.marti.remote.service.InputManager;
import com.bbn.marti.sync.model.DataFeedDao;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.config.AuthType;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;

import tak.server.Constants;
import tak.server.cache.PluginDatafeedCacheHelper;
import tak.server.feeds.DataFeed.DataFeedType;
import tak.server.ignite.MessagingIgniteBroker;
import tak.server.plugins.PluginDataFeed;
import tak.server.plugins.PluginDataFeedApi;

public class DistributedPluginDataFeedApi implements PluginDataFeedApi, org.apache.ignite.services.Service {

	private static final long serialVersionUID = 2276211741137405196L;
	
	private static final Logger logger = LoggerFactory.getLogger(DistributedPluginDataFeedApi.class);
	
	@Autowired
	private PluginDatafeedCacheHelper pluginDatafeedCacheHelper;
	
	@Autowired
	private PluginDataFeedJdbc pluginDataFeedJdbc;

	private DataFeedRepository dataFeedRepository() {
	    return MessagingDependencyInjectionProxy.getInstance().dataFeedRepository();
	}
	
	 
	@Override
	public PluginDataFeed create(String uuid, String name, List<String> tags, boolean archive, boolean sync) {
		
		logger.info("Calling create() method in DistributedPluginDataFeedApi, uuid: {}, name: {}, tags: {}, archive: {}, sync: {}", uuid, name, tags, archive, sync);

		DataFeedRepository dataFeedRepository = dataFeedRepository();

		Long dataFeedId;
		if (dataFeedRepository.getDataFeedByUUID(uuid).size() > 0) {
			
			logger.info("Updating datafeed uuid {}", uuid);

			try {
				// Updating dataFeed in metrics for UI
				DataFeed dataFeed = new DataFeed();
				dataFeed.setName(name);
				dataFeed.setType("Plugin");
				dataFeed.setUuid(uuid);
				for (String tag: tags) {
					dataFeed.getTag().add(tag);
				}
				dataFeed.setAuth(AuthType.ANONYMOUS);
				dataFeed.setPort(0);
				dataFeed.setProtocol("Plugin");
				dataFeed.setAuthRequired(false);
				dataFeed.setGroup("");
				dataFeed.setIface("");
				dataFeed.setArchive(archive);
				dataFeed.setAnongroup(false);
				dataFeed.setArchiveOnly(false);
				dataFeed.setCoreVersion(0);
				dataFeed.setCoreVersion2TlsVersions("");
				dataFeed.setSync(sync);

				MessagingIgniteBroker.brokerServiceCalls(service -> ((InputManager) service)
						.modifyInput(name, dataFeed), Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

			} catch (Exception e) {
				logger.warn("Exception updating plugin data feed for UI", e.getMessage());
			}

			dataFeedId = dataFeedRepository.updateDataFeed(uuid, name, DataFeedType.Plugin.ordinal(),
					AuthType.ANONYMOUS.toString(), 0, false, "",
					null, null, archive, false, false ,0, "", sync);
			
			logger.info("Updated datafeed uuid {}, row id", uuid, dataFeedId);
			
			try {
				dataFeedRepository.removeAllDataFeedTagsById(dataFeedId);					
				logger.info("Removed old tags for datafeed row id  {}", dataFeedId);
			}catch(Exception e) {
				logger.warn("Exception when removing all tags for datafeed row id {}. Reason: {}", dataFeedId, e.getMessage());
			}

			if (tags.size() > 0) {
				for (String tag : tags) {
					dataFeedRepository.addDataFeedTag(dataFeedId, tag);
				}
				logger.info("Added new tags for datafeed row id  {}", dataFeedId);
			}
			
			// update cache
			PluginDataFeed re = new PluginDataFeed(uuid, name, tags, archive, sync);
			List<PluginDataFeed> pluginDataFeeds = new ArrayList<PluginDataFeed>();
			pluginDataFeeds.add(re);
			pluginDatafeedCacheHelper.cachePluginDatafeed(uuid, pluginDataFeeds);
			
			pluginDatafeedCacheHelper.invalidate(PluginDatafeedCacheHelper.ALL_PLUGIN_DATAFEED_KEY);	

			return re; 

		} else {
			
			logger.info("Adding datafeed uuid {}", uuid);

			try {
				// Add dataFeed to metrics for UI
				DataFeed dataFeed = new DataFeed();
				dataFeed.setName(name);
				dataFeed.setType("Plugin");
				dataFeed.setUuid(uuid);
				for (String tag: tags) {
					dataFeed.getTag().add(tag);
				}
				dataFeed.setAuth(AuthType.ANONYMOUS);
				dataFeed.setPort(0);
				dataFeed.setProtocol("Plugin");
				dataFeed.setAuthRequired(false);
				dataFeed.setGroup("");
				dataFeed.setIface("");
				dataFeed.setArchive(archive);
				dataFeed.setAnongroup(false);
				dataFeed.setArchiveOnly(false);
				dataFeed.setCoreVersion(0);
				dataFeed.setCoreVersion2TlsVersions("");
				dataFeed.setSync(sync);

				MessagingIgniteBroker.brokerServiceCalls(service -> ((InputManager) service)
						.createDataFeed(dataFeed), Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

			} catch (Exception e) {
				logger.warn("Exception adding plugin data feed for UI", e.getMessage());
			}
			
			dataFeedId = dataFeedRepository.addDataFeed(uuid, name, DataFeedType.Plugin.ordinal(),
					AuthType.ANONYMOUS.toString(), 0, false, "",
					null, null, archive, false, false ,0, "", sync);
			
			logger.info("Added datafeed uuid {}, row id", uuid, dataFeedId);

			try {
				dataFeedRepository.removeAllDataFeedTagsById(dataFeedId);					
				logger.info("Removed old tags for datafeed row id  {}", dataFeedId);
			}catch(Exception e) {
				logger.warn("Exception when removing all tags for datafeed row id {}. Reason: {}", dataFeedId, e.getMessage());
			}
			
			if (tags.size() > 0) {
				for (String tag : tags) {
					dataFeedRepository.addDataFeedTag(dataFeedId, tag);
				}
				logger.info("Added new tags for datafeed row id  {}", dataFeedId);
			}
			
			// update cache
			PluginDataFeed re = new PluginDataFeed(uuid, name, tags, archive, sync);
			List<PluginDataFeed> pluginDataFeeds = new ArrayList<PluginDataFeed>();
			pluginDataFeeds.add(re);
			pluginDatafeedCacheHelper.cachePluginDatafeed(uuid, pluginDataFeeds);
			
			pluginDatafeedCacheHelper.invalidate(PluginDatafeedCacheHelper.ALL_PLUGIN_DATAFEED_KEY);	

			return re; 
		}	
	}
	
	@Override
	public PluginDataFeed create(String uuid, String name, List<String> tags) {
		
		return create(uuid, name, tags, true, false);
	
	}

	@Override
	public void delete(String uuid) {
		
		logger.info("Calling delete method in DistributedPluginDataFeedApi, uuid: {}", uuid);

		DataFeedRepository dataFeedRepository = dataFeedRepository();

		List<DataFeedDao> dataFeedDaos = dataFeedRepository.getDataFeedByUUID(uuid);
		
		if (dataFeedDaos.size() == 0) {
			logger.warn("There is no datafeed with uuid {} to delete", uuid);
			return;
		}
		
		if (dataFeedDaos.size() > 1) {
			logger.warn("There are {} datafeeds with uuid {}", dataFeedDaos.size(), uuid);
			return;
		}
		
		for (DataFeedDao dataFeedDao: dataFeedDaos) {
			
			logger.info("Removing all tags for datafeed row id {}", dataFeedDao.getId());
			try {
				dataFeedRepository.removeAllDataFeedTagsById(dataFeedDao.getId());					
				logger.info("Removed all tags for datafeed row id  {}", dataFeedDao.getId());
			}catch(Exception e) {
				logger.warn("Exception when removing all tags for datafeed row id {}. Reason: {}", dataFeedDao.getId(), e.getMessage());
			}
			
			logger.info("Deleting datafeed row id {}", dataFeedDao.getId());
			dataFeedRepository.deleteDataFeedById(dataFeedDao.getId());
			logger.info("Deleted datafeed row id {}", dataFeedDao.getId());
		}
		
		// update cache
		pluginDatafeedCacheHelper.cachePluginDatafeed(uuid, new ArrayList<PluginDataFeed>());
		
		pluginDatafeedCacheHelper.invalidate(PluginDatafeedCacheHelper.ALL_PLUGIN_DATAFEED_KEY);	
				
	}
	
	@Override
	public Collection<PluginDataFeed> getAllPluginDataFeeds() {
		
		logger.info("Calling getAllPluginDataFeeds() in DistributedPluginDataFeedApi");
		
		List<PluginDataFeed> allPluginDatafeeds = pluginDatafeedCacheHelper.getAllPluginDatafeeds();
		
		if (allPluginDatafeeds == null) { // not in cache
			
			if (logger.isDebugEnabled()) {
				logger.debug("Result not in cache");
			}

			allPluginDatafeeds = pluginDataFeedJdbc.getPluginDataFeeds();

	    	// cache the result
	    	pluginDatafeedCacheHelper.cacheAllPluginDatafeeds(allPluginDatafeeds);
		}else {
			
			if (logger.isDebugEnabled()) {
				logger.debug("Get result from cache");
			}
		}

    	return allPluginDatafeeds;
	}

	
	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
    	
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

}
