

package com.bbn.marti.remote.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.cache.event.CacheEntryEvent;
import jakarta.xml.bind.JAXBException;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.services.ServiceContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.util.ActiveProfiles;
import com.bbn.marti.config.Async;
import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Auth.Ldap;
import com.bbn.marti.config.Buffer;
import com.bbn.marti.config.Buffer.LatestSA;
import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.ContactApi;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Dissemination;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.FederationServer;
import com.bbn.marti.config.Ferry;
import com.bbn.marti.config.Filter;
import com.bbn.marti.config.Geocache;
import com.bbn.marti.config.Input;
import com.bbn.marti.config.Network;
import com.bbn.marti.config.Qos;
import com.bbn.marti.config.Repeater;
import com.bbn.marti.config.Repository;
import com.bbn.marti.config.Security;
import com.bbn.marti.config.Submission;
import com.bbn.marti.config.Subscription;
import com.bbn.marti.config.Tls;
import com.bbn.marti.config.Vbm;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;
import com.bbn.marti.remote.util.SpringContextBeanForApi;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;
import tak.server.util.JAXBUtils;

public class DistributedConfiguration implements CoreConfig, org.apache.ignite.services.Service {

	private static final long serialVersionUID = -2752863566907557239L;

    private static final String CORE_CONFIG_CACHE_KEY = "coreConfig";

    static DistributedConfiguration instance = null;

    private static final Logger logger = LoggerFactory.getLogger(DistributedConfiguration.class);
    
    public void setConfiguration(Configuration configuration) {
    	LocalConfiguration.getInstance().setConfiguration(configuration);
    }

    @Override
    public Configuration getRemoteConfiguration() {
    	return LocalConfiguration.getInstance().getConfiguration();
    }

    private IgniteCache<String, Configuration> configurationCache;
    private ContinuousQuery<String, Configuration> continuousConfigurationQuery = new ContinuousQuery<>();

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
    	if (getRemoteConfiguration().getCluster().isEnabled() && getRemoteConfiguration().getCluster().isCacheConfig()) {
    		loadConfigFromCache();
    	}
    	
    	continuousConfigurationQuery.setLocalListener((evts) -> {
    	     for (CacheEntryEvent<? extends String, ? extends Configuration> e : evts) {
    	    	 this.setConfiguration(e.getValue());
    	    	 saveChanges();
    	     }
      	 });
    	
    	getConfigurationCache().query(continuousConfigurationQuery);
    	
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

    private IgniteCache<String, Configuration> getConfigurationCache() {

		if (configurationCache == null) {
			CacheConfiguration<String, Configuration> cfg = new CacheConfiguration<String, Configuration>();
		
			cfg.setName(Constants.CONFIGURATION_CACHE_NAME);
			cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
			configurationCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache(cfg);
		}
		
		return configurationCache;
	}

    public static DistributedConfiguration getInstance() {
    	if (instance == null) {
    		synchronized(DistributedConfiguration.class) {
    			if (instance == null) {
    				try {
						instance = SpringContextBeanForApi.getSpringContext().getBean(DistributedConfiguration.class);
					}catch (Exception e) {
						instance = new DistributedConfiguration();
					}
    			}
    		}
    	}

    	return instance;
    }

    public synchronized void removeInputAndSave(String name) throws RemoteException {
        @SuppressWarnings({ "unchecked", "rawtypes" })
		List<Input> inputList = new ArrayList(getRemoteConfiguration().getNetwork().getInput());
        for (Input input : inputList) {
            if (input.getName().equals(name)) {
                getRemoteConfiguration().getNetwork().getInput().remove(input);
                saveChangesAndUpdateCache();
            }
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<DataFeed> dataFeedList = new ArrayList(getRemoteConfiguration().getNetwork().getDatafeed());
        for (DataFeed dataFeed: dataFeedList) {
        	if (dataFeed.getName().equals(name)) {
                getRemoteConfiguration().getNetwork().getDatafeed().remove(dataFeed);
                saveChangesAndUpdateCache();
        	}
        }
    }

    public synchronized void removeDataFeedAndSave(String name) throws RemoteException {
        @SuppressWarnings({ "unchecked", "rawtypes" })
		List<DataFeed> DataFeed = new ArrayList(getRemoteConfiguration().getNetwork().getDatafeed());
        for (DataFeed dataFeed : DataFeed) {
            if (dataFeed.getName().equals(name)) {
                getRemoteConfiguration().getNetwork().getDatafeed().remove(dataFeed);
                saveChangesAndUpdateCache();
            }
        }
    }

    @Override
    public boolean isCluster() {
        return getRemoteConfiguration().getCluster().isEnabled();
    }

    @SuppressWarnings("rawtypes")
	public synchronized ConnectionModifyResult updateInputGroupsNoSave(String inputName, String[] groupList) throws RemoteException {
        Input input = getInputByName(inputName);
        if (input == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }

        @SuppressWarnings("unchecked")
		List<String> currentFilterGroups = new ArrayList(input.getFiltergroup());

        @SuppressWarnings("unchecked")
		List<String> newGroupList = new ArrayList(Arrays.asList(groupList));

        for (String newGroupName : newGroupList) {
            if (!currentFilterGroups.contains(newGroupName)) {
                input.getFiltergroup().add(newGroupName);
            }
        }

        for (String existingGroupName : currentFilterGroups) {
            if (!newGroupList.contains(existingGroupName)) {
                input.getFiltergroup().remove(existingGroupName);
            }
        }
        return ConnectionModifyResult.SUCCESS;
    }

    public ConnectionModifyResult setArchiveFlagNoSave(String inputName, boolean desiredState) {
        Input input = getInputByName(inputName);
        if (input == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }
        input.setArchive(desiredState);
        return ConnectionModifyResult.SUCCESS;
    }

    public ConnectionModifyResult setArchiveOnlyFlagNoSave(String inputName, boolean desiredState) {
        Input input = getInputByName(inputName);
        if (input == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }
        input.setArchiveOnly(desiredState);
        return ConnectionModifyResult.SUCCESS;
    }
    
    public ConnectionModifyResult setFederatedFlagNoSave(String inputName, boolean desiredState) {
        Input input = getInputByName(inputName);
        if (input == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }
        input.setFederated(desiredState);
        return ConnectionModifyResult.SUCCESS;
    }

    public ConnectionModifyResult setSyncCacheRetentionSeconds(String inputName, int desiredState) {
        Input input = getInputByName(inputName);
        if (input == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }
        input.setSyncCacheRetentionSeconds(desiredState);
        return ConnectionModifyResult.SUCCESS;
    }


    @SuppressWarnings("rawtypes")
	public synchronized ConnectionModifyResult updateTagsNoSave(String inputName, List<String> newTagList) throws RemoteException {
		Input input = getInputByName(inputName);
		if (input == null) {
			return ConnectionModifyResult.FAIL_NONEXISTENT;
		}
		if (!(input instanceof DataFeed)) {
			return ConnectionModifyResult.FAIL_NOMOD_DFEED;
		}
		DataFeed dataFeed = (DataFeed) input;

		@SuppressWarnings("unchecked")
		List<String> currentTagList = new ArrayList(dataFeed.getTag());

		for (String newTagName : newTagList) {
			if (!currentTagList.contains(newTagName)) {
				dataFeed.getTag().add(newTagName);
			}
		}

		for (String existingTag : currentTagList) {
			if (!newTagList.contains(existingTag)) {
				dataFeed.getTag().remove(existingTag);
			}
		}
		return ConnectionModifyResult.SUCCESS;
	}

    public ConnectionModifyResult setSyncFlagNoSave(String dataFeedName, boolean desiredState) {
        Input input = getInputByName(dataFeedName);
        if (input == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }
        if (!(input instanceof DataFeed)) {
        	return ConnectionModifyResult.FAIL_NOMOD_DFEED;
        }
        DataFeed dataFeed = (DataFeed) input;
        dataFeed.setSync(desiredState);
        return ConnectionModifyResult.SUCCESS;
    }

    @Override
    public void saveChanges() {
    	// if TAK Server processes are local to each other and we're not on the messaging process, don't save
    	if (IgniteHolder.getInstance().areTakserverIgnitesLocal() && !ActiveProfiles.getInstance().isConfigProfileActive()) {
    		return;
    	}
    	
    	
    	// if TAK Server processes are local to each other and we're not on the config process or in the cluster, don't save
    	if (getRemoteConfiguration().getCluster().isEnabled()
				|| (IgniteHolder.getInstance().areTakserverIgnitesLocal() && ActiveProfiles.getInstance().isConfigProfileActive())) {
    		try {
				synchronized (DistributedConfiguration.class) {
					JAXBUtils.saveJAXifiedObject(LocalConfiguration.CONFIG_FILE, getRemoteConfiguration(), false);
				}
			} catch (FileNotFoundException fnfe) {
				// do nothing. Happens in unit test.
			} catch (JAXBException | IOException e) {
				throw new RuntimeException(e);
			}
		} else {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping Core Config Save for this process");
            }
		}
    }

    public void updateCache() {
    	// cache the updated configuration that has also been saved to disk
    	getConfigurationCache().put(CORE_CONFIG_CACHE_KEY, getRemoteConfiguration());
    }

    @Override
    public synchronized void saveChangesAndUpdateCache() {
    	updateCache();
    	saveChanges();
    }

    public synchronized NetworkInputAddResult addInputAndSave(@NotNull Input input) {
        if (input instanceof DataFeed) {
        	for (Input feedLoop : getRemoteConfiguration().getNetwork().getDatafeed()) {
                if (((DataFeed) feedLoop).getName().equals(((DataFeed) input).getName())) {
                    return NetworkInputAddResult.FAIL_INPUT_NAME_EXISTS;
                }
                if (feedLoop.getName().equals(input.getName())) {
                    return NetworkInputAddResult.FAIL_INPUT_NAME_EXISTS;
                }
            }
        	getRemoteConfiguration().getNetwork().getDatafeed().add((DataFeed) input);
        } else {
        	for (Input loopInput : getRemoteConfiguration().getNetwork().getInput()) {
                if (loopInput.getName().equals(input.getName())) {
                    return NetworkInputAddResult.FAIL_INPUT_NAME_EXISTS;
                }
            }
            getRemoteConfiguration().getNetwork().getInput().add(input);
        }

        saveChangesAndUpdateCache();

        if (logger.isDebugEnabled()) {
        	logger.debug("Save and cached new input on port " + input.getPort());
        }

        return NetworkInputAddResult.SUCCESS;
    }


    public synchronized Input getInputByName(@NotNull String inputName) {
        List<Input> inputList = getRemoteConfiguration().getNetwork().getInput();
        for (Input input : inputList) {
            if (input.getName().equals(inputName)) {
                return input;
            }
        }
		List<DataFeed> dataFeedList = getRemoteConfiguration().getNetwork().getDatafeed();
		for (Input input : dataFeedList) {
			if (input.getName().equals(inputName)) {
				return input;
			}
		}
        return null;
    }

    private synchronized Subscription.Static getStaticSubscriptionByName(@NotNull String subscriptionName) {
        List<Subscription.Static> subList = getRemoteConfiguration().getSubscription().getStatic();
        for (Subscription.Static subscription : subList) {
            if (subscription.getName().equals(subscriptionName)) {
                return subscription;
            }
        }
        return null;
    }

    public synchronized void setInputArchiveFlagAndSave(@NotNull String inputName, boolean desiredState) {
        Input input = getInputByName(inputName);
        if (input != null && input.isArchive() != desiredState) {
            input.setArchive(desiredState);
            saveChangesAndUpdateCache();
        }
    }

    public synchronized void addStaticSubscriptionAndSave(@NotNull Subscription.Static newStaticSubscription) {
        getRemoteConfiguration().getSubscription().getStatic().add(newStaticSubscription);
        saveChangesAndUpdateCache();
    }

    public synchronized void removeStaticSubscriptionAndSave(@NotNull String subscriptionIdentifier) {
        Subscription.Static subscription = getStaticSubscriptionByName(subscriptionIdentifier);
        if (subscription != null) {
            getRemoteConfiguration().getSubscription().getStatic().remove(subscription);
            saveChangesAndUpdateCache();
        }
    }

    @NotNull
    public synchronized List<Input> getNetworkInputs() {
        return getRemoteConfiguration().getNetwork().getInput();
    }

    @NotNull
    public synchronized List<DataFeed> getNetworkDataFeeds() {
        return getRemoteConfiguration().getNetwork().getDatafeed();
    }

    public Network getNetwork() {
        return getRemoteConfiguration().getNetwork();
    }

    public Auth getAuth() {
        return getRemoteConfiguration().getAuth();
    }

    public Submission getSubmission() {
        return getRemoteConfiguration().getSubmission();
    }

    public Subscription getSubscription() {
        return getRemoteConfiguration().getSubscription();
    }

    public Repository getRepository() {
        return getRemoteConfiguration().getRepository();
    }

    public Repeater getRepeater() {
        return getRemoteConfiguration().getRepeater();
    }

    public Filter getFilter() {
        return getRemoteConfiguration().getFilter();
    }

    public Buffer getBuffer() {
        return getRemoteConfiguration().getBuffer();
    }

    public Dissemination getDissemination() {
        return getRemoteConfiguration().getDissemination();
    }

    public Security getSecurity() {
    	
    	// for unit tests
    	if (getRemoteConfiguration().getSecurity() == null) {
    		getRemoteConfiguration().setSecurity(new Security());
    	}
    	
    	// for unit tests
    	if (getRemoteConfiguration().getSecurity().getTls() == null) {
    		getRemoteConfiguration().getSecurity().setTls(new Tls());
    	}
    	
        return getRemoteConfiguration().getSecurity();
    }

    @Nullable
    public Ferry getFerry() {
        return getRemoteConfiguration().getFerry();
    }

    @Nullable
    public Async getAsync() {
        return getRemoteConfiguration().getAsync();
    }

    public Geocache getGeocache() {
    	return getRemoteConfiguration().getGeocache();
    }
    
    public Vbm getVbm() {
    	return getRemoteConfiguration().getVbm();
    }

 	@Override
	public Configuration getCachedConfiguration() {
    	return getRemoteConfiguration();
	}

	@Override
	public void loadConfigFromCache() {
		if (getConfigurationCache().get(CORE_CONFIG_CACHE_KEY) == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("configuration cache miss");
			}
			getConfigurationCache().put(CORE_CONFIG_CACHE_KEY, getRemoteConfiguration());
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("configuration cache hit: " + getRemoteConfiguration());
			}
		}
		setConfiguration(getConfigurationCache().get(CORE_CONFIG_CACHE_KEY));
		saveChanges();
	}
	

	@Override
	public void setAndSaveFederation(Federation federation) {
	    getRemoteConfiguration().setFederation(federation);
        saveChangesAndUpdateCache();
	}
	
	@Override
	public void setAndSaveQos(Qos qos) {
	    getRemoteConfiguration().getFilter().setQos(qos);
        saveChangesAndUpdateCache();
	}

	@Override
	public void setAndSaveLDAP(Ldap ldap) {
	    getRemoteConfiguration().getAuth().setLdap(ldap);
	    saveChangesAndUpdateCache();
	}

	@Override
	public void setAndSaveMessagingConfig(LatestSA latestSA, Repository repository) {
	    getRemoteConfiguration().getBuffer().setLatestSA(latestSA);
	    getRemoteConfiguration().setRepository(repository);
	    saveChangesAndUpdateCache();
	}

	@Override
	public void setAndSaveSecurityConfig(Tls tls, Auth auth, FederationServer fedServer) {
	    getRemoteConfiguration().getSecurity().setTls(tls);
	    getRemoteConfiguration().setAuth(auth);
	    getRemoteConfiguration().getFederation().setFederationServer(fedServer);
	    saveChangesAndUpdateCache();
	}

	@Override
    public void setAndSaveCertificateSigningConfig(CertificateSigning certificateSigningConfig) {
        getRemoteConfiguration().setCertificateSigning(certificateSigningConfig);
        saveChangesAndUpdateCache();
    }
	
	@Override
    public void setAndSaveStoreForwardChatEnabled(boolean storeForwardChatEnabled) {
	    getRemoteConfiguration().getBuffer().getQueue().setEnableStoreForwardChat(storeForwardChatEnabled);
	    saveChangesAndUpdateCache();
    }
	
	@Override
    public void setAndSaveVbmConfiguration(Vbm vbm) {
	    getRemoteConfiguration().setVbm(vbm);
	    saveChangesAndUpdateCache();
    }

    @Override
    public void setAndSaveServerId(String serverId) {
        getRemoteConfiguration().getNetwork().setServerId(serverId);
        saveChangesAndUpdateCache();
    }

    @Override
    public void setAndSaveEnterpriseSyncSizeLimit(int uploadSizeLimt) {
        getRemoteConfiguration().getNetwork().setEnterpriseSyncSizeLimitMB(uploadSizeLimt);
        saveChangesAndUpdateCache();
    }

	@Override
	public boolean isContactApiFilter() {
		return getRemoteConfiguration().getFilter().getContactApi() != null;
	}

	@Override
	public Set<String> getContactApiWriteOnlyGroups() {
		Set<String> result = new ConcurrentSkipListSet<>();
		
		if (!isContactApiFilter()) {
			return result;
		}
		
		for (ContactApi contactApiConf : getRemoteConfiguration().getFilter().getContactApi()) {
			result.add(contactApiConf.getGroupName());
		}
		
		return result;
	}
}
