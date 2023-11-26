package tak.server.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.network.PluginDataFeedJdbc;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.service.InputManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;

import tak.server.Constants;
import tak.server.cache.DatafeedCacheHelper;
import tak.server.feeds.DataFeedDTO;
import tak.server.feeds.DataFeed.DataFeedType;
import tak.server.ignite.MessagingIgniteBroker;
import tak.server.plugins.PluginDataFeed;
import tak.server.plugins.PluginDataFeedApi;
import tak.server.plugins.PredicateDataFeed;

public class DistributedPluginDataFeedApi implements PluginDataFeedApi, org.apache.ignite.services.Service {

	private static final long serialVersionUID = 2276211741137405196L;

	private static final Logger logger = LoggerFactory.getLogger(DistributedPluginDataFeedApi.class);

	public DistributedPluginDataFeedApi() {
	}

	private DataFeedRepository dataFeedRepository() {
		return MessagingDependencyInjectionProxy.getInstance().dataFeedRepository();
	}

	private PluginDataFeedJdbc pluginDataFeedJdbc() {
		return MessagingDependencyInjectionProxy.getInstance().pluginDataFeedJdbc();
	}

	private DatafeedCacheHelper pluginDatafeedCacheHelper() {
		return MessagingDependencyInjectionProxy.getInstance().pluginDatafeedCacheHelper();
	}

	private GroupManager groupManager() {
		return MessagingDependencyInjectionProxy.getInstance().groupManager();
	}

	private RemoteUtil remoteUtil() {
		return MessagingDependencyInjectionProxy.getInstance().remoteUtil();
	}

	@Override
	public PluginDataFeed create(String uuid, String name, List<String> tags, boolean archive, boolean sync, List<String> groupNames, boolean federated, boolean binaryPayloadWebsocketOnly) {

		try {

			logger.info("Calling create() method in DistributedPluginDataFeedApi, uuid: {}, name: {}, tags: {}, archive: {}, sync: {}, groupNames: {}, federated: {}, binaryPayloadWebsocketOnly: {}", uuid, name, tags, archive, sync, groupNames, federated, binaryPayloadWebsocketOnly);

			DatafeedCacheHelper pluginDatafeedCacheHelper = pluginDatafeedCacheHelper();
			GroupManager groupManager = groupManager();
			RemoteUtil remoteUtil = remoteUtil();
			DataFeedRepository dataFeedRepository = dataFeedRepository();

			if (groupNames.isEmpty()) {
				groupNames.add(Constants.ANON_GROUP);
			}

			Set<Group> groups = groupManager.findGroups(groupNames);
			String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));

			Long dataFeedId;
			if (dataFeedRepository.getDataFeedByGroup(name, groupVector).size() > 0) {

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
					dataFeed.getFiltergroup().addAll(groupNames);
					dataFeed.setFederated(federated);
					dataFeed.setBinaryPayloadWebsocketOnly(binaryPayloadWebsocketOnly);

					MessagingIgniteBroker.brokerServiceCalls(service -> ((InputManager) service)
							.modifyInput(name, dataFeed), Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

				} catch (Exception e) {
					logger.warn("Exception updating plugin data feed for UI", e.getMessage());
				}

				dataFeedId = dataFeedRepository.updateDataFeedWithGroupVector(uuid, name, DataFeedType.Plugin.ordinal(),
						AuthType.ANONYMOUS.toString(), 0, false, "Plugin",
						"", "", archive, false, false ,0, "", sync, 3600, groupVector, federated, binaryPayloadWebsocketOnly, null, null, null, null);

				logger.info("Updated datafeed uuid {}, row id", uuid, dataFeedId);

				try {
					dataFeedRepository.removeAllDataFeedTagsById(dataFeedId);
					logger.info("Removed old tags for datafeed row id  {}", dataFeedId);
				}catch(Exception e) {
					logger.warn("Exception when removing all tags for datafeed row id {}. Reason: {}", dataFeedId, e.getMessage());
				}

				if (tags.size() > 0) {
					dataFeedRepository.addDataFeedTags(dataFeedId, tags);
					logger.info("Added new tags for datafeed row id  {}", dataFeedId);
				}

				try {
					dataFeedRepository.removeAllDataFeedFilterGroupsById(dataFeedId);
					logger.info("Removed old filter groups for datafeed row id  {}", dataFeedId);
				}catch(Exception e) {
					logger.warn("Exception when removing all filter groups for datafeed row id {}. Reason: {}", dataFeedId, e.getMessage());
				}

				if (groupNames.size() > 0) {
					dataFeedRepository.addDataFeedFilterGroups(dataFeedId, groupNames);
				}

				// update cache
				PluginDataFeed re = new PluginDataFeed(uuid, name, tags, archive, sync, groupNames, federated, binaryPayloadWebsocketOnly);
				List<PluginDataFeed> pluginDataFeeds = new ArrayList<PluginDataFeed>();
				pluginDataFeeds.add(re);
				pluginDatafeedCacheHelper.cachePluginDatafeed(uuid, pluginDataFeeds);

				pluginDatafeedCacheHelper.invalidate(DatafeedCacheHelper.ALL_PLUGIN_DATAFEED_KEY);

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
					dataFeed.getFiltergroup().addAll(groupNames);
					dataFeed.setFederated(federated);
					dataFeed.setBinaryPayloadWebsocketOnly(binaryPayloadWebsocketOnly);

					MessagingIgniteBroker.brokerServiceCalls(service -> ((InputManager) service)
							.createDataFeed(dataFeed), Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

				} catch (Exception e) {
					logger.warn("Exception adding plugin data feed for UI", e.getMessage());
				}

				dataFeedId = dataFeedRepository.addDataFeed(uuid, name, DataFeedType.Plugin.ordinal(),
						AuthType.ANONYMOUS.toString(), 0, false, "Plugin",
						"", "", archive, false, false ,0, "", sync, 3600, groupVector, federated, binaryPayloadWebsocketOnly, null, null, null, null);

				logger.info("Added datafeed uuid {}, row id", uuid, dataFeedId);

				try {
					dataFeedRepository.removeAllDataFeedTagsById(dataFeedId);
					logger.info("Removed old tags for datafeed row id  {}", dataFeedId);
				}catch(Exception e) {
					logger.warn("Exception when removing all tags for datafeed row id {}. Reason: {}", dataFeedId, e.getMessage());
				}

				if (tags.size() > 0) {
					dataFeedRepository.addDataFeedTags(dataFeedId, tags);
					logger.info("Added new tags for datafeed row id  {}", dataFeedId);
				}

				try {
					dataFeedRepository.removeAllDataFeedFilterGroupsById(dataFeedId);
					logger.info("Removed old filter groups for datafeed row id  {}", dataFeedId);
				}catch(Exception e) {
					logger.warn("Exception when removing all filter groups for datafeed row id {}. Reason: {}", dataFeedId, e.getMessage());
				}

				if (groupNames.size() > 0) {
					dataFeedRepository.addDataFeedFilterGroups(dataFeedId, groupNames);
				}

				// update cache
				PluginDataFeed re = new PluginDataFeed(uuid, name, tags, archive, sync, groupNames, federated, binaryPayloadWebsocketOnly);
				List<PluginDataFeed> pluginDataFeeds = new ArrayList<PluginDataFeed>();
				pluginDataFeeds.add(re);
				pluginDatafeedCacheHelper.cachePluginDatafeed(uuid, pluginDataFeeds);

				pluginDatafeedCacheHelper.invalidate(DatafeedCacheHelper.ALL_PLUGIN_DATAFEED_KEY);

				return re;
			}

		} catch (TakException e) {
			if (logger.isDebugEnabled()) {
				logger.error("TakException in create", e);
			}
			throw e;
		} catch (Exception e) {
			logger.error("exception in create", e);
			throw e;
		}
	}
	
	@Override
	public PluginDataFeed create(String uuid, String name, List<String> tags, boolean archive, boolean sync, List<String> groupNames, boolean federated) {
		
		return create(uuid, name, tags, archive, sync, groupNames, federated, false);

	}
	
	@Override
	public PluginDataFeed create(String uuid, String name, List<String> tags, boolean archive, boolean sync, List<String> groupNames) {
		
		return create(uuid, name, tags, archive, sync, groupNames, true);
	
	}

	@Override
	public PluginDataFeed create(String uuid, String name, List<String> tags, boolean archive, boolean sync) {

		return create(uuid, name, tags, archive, sync, Arrays.asList(Constants.ANON_GROUP), true);

	}

	@Override
	public PluginDataFeed create(String uuid, String name, List<String> tags) {

		return create(uuid, name, tags, true, false);

	}	
	
	@Override
	public void delete(String uuid, List<String> groupNames) {

		try {
			DatafeedCacheHelper pluginDatafeedCacheHelper = pluginDatafeedCacheHelper();
			GroupManager groupManager = groupManager();
			RemoteUtil remoteUtil = remoteUtil();

			logger.info("Calling delete method in DistributedPluginDataFeedApi, uuid: {}, groupNames: {}", uuid, groupNames);

			DataFeedRepository dataFeedRepository = dataFeedRepository();
			Set<Group> groups = groupManager.findGroups(groupNames);
			String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));


			List<DataFeedDTO> dataFeedDaos = dataFeedRepository.getDataFeedByUUID(uuid);

			if (dataFeedDaos.size() == 0) {
				logger.warn("There is no datafeed with uuid {} to delete", uuid);
				return;
			}

			if (dataFeedDaos.size() > 1) {
				logger.warn("There are {} datafeeds with uuid {}", dataFeedDaos.size(), uuid);
				return;
			}


			try {
				MessagingIgniteBroker.brokerVoidServiceCalls(service -> ((InputManager) service).deleteDataFeed(dataFeedDaos.get(0).getName()),
						Constants.DISTRIBUTED_INPUT_MANAGER, InputManager.class);

			} catch (Exception e) {
				logger.error("Exception deleting data feed from config file.", e);
			}

			for (DataFeedDTO dataFeedDao : dataFeedDaos) {

				logger.info("Removing all tags for datafeed row id {}", dataFeedDao.getId());
				try {
					dataFeedRepository.removeAllDataFeedTagsById(dataFeedDao.getId());
					logger.info("Removed all tags for datafeed row id  {}", dataFeedDao.getId());
				} catch (Exception e) {
					logger.warn("Exception when removing all tags for datafeed row id {}. Reason: {}", dataFeedDao.getId(), e.getMessage());
				}

				try {
					dataFeedRepository.removeAllDataFeedFilterGroupsById(dataFeedDao.getId());
					logger.info("Removed all filter groups for datafeed row id  {}", dataFeedDao.getId());
				} catch (Exception e) {
					logger.warn("Exception when removing all filter groups for datafeed row id {}. Reason: {}", dataFeedDao.getId(), e.getMessage());
				}

				logger.info("Deleting datafeed row id {}", dataFeedDao.getId());
				dataFeedRepository.deleteDataFeedById(dataFeedDao.getId(), groupVector);
				logger.info("Deleted datafeed row id {}", dataFeedDao.getId());
			}

			// update cache
			pluginDatafeedCacheHelper.cachePluginDatafeed(uuid, new ArrayList<PluginDataFeed>());

			pluginDatafeedCacheHelper.invalidate(DatafeedCacheHelper.ALL_PLUGIN_DATAFEED_KEY);

		} catch (TakException e) {
			if (logger.isDebugEnabled()) {
				logger.error("TakException in delete", e);
			}
			throw e;
		} catch (Exception e) {
			logger.error("exception in delete", e);
			throw e;
		}
	}

	@Override
	public Collection<PluginDataFeed> getAllPluginDataFeeds() {

		try {

			DatafeedCacheHelper pluginDatafeedCacheHelper = pluginDatafeedCacheHelper();

			List<PluginDataFeed> allPluginDatafeeds = pluginDatafeedCacheHelper.getAllPluginDatafeeds();

			if (allPluginDatafeeds == null) { // not in cache

				if (logger.isDebugEnabled()) {
					logger.debug("Result not in cache");
				}

				allPluginDatafeeds = pluginDataFeedJdbc().getPluginDataFeeds();

				// cache the result
				pluginDatafeedCacheHelper.cacheAllPluginDatafeeds(allPluginDatafeeds);
			} else {

				if (logger.isDebugEnabled()) {
					logger.debug("Get result from cache");
				}
			}

			return allPluginDatafeeds;

		} catch (TakException e) {
			if (logger.isDebugEnabled()) {
				logger.error("TakException in getAllPluginDataFeeds", e);
			}
			throw e;
		} catch (Exception e) {
			logger.error("exception in getAllPluginDataFeeds", e);
			throw e;
		}
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

	@Override
	public PredicateDataFeed createPredicateFeed(String uuid, String name, List<String> tags) {
		return new PredicateDataFeed();
	}

}
