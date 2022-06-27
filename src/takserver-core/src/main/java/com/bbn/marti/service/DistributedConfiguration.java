

package com.bbn.marti.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.cache.event.CacheEntryEvent;
import javax.xml.bind.JAXBException;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.services.ServiceContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Async;
import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Auth.Ldap;
import com.bbn.marti.config.Buffer;
import com.bbn.marti.config.Buffer.LatestSA;
import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.ContactApi;
import com.bbn.marti.config.Dissemination;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.FederationServer;
import com.bbn.marti.config.Ferry;
import com.bbn.marti.config.Filter;
import com.bbn.marti.config.Geocache;
import com.bbn.marti.config.Network;
import com.bbn.marti.config.Qos;
import com.bbn.marti.config.Repeater;
import com.bbn.marti.config.Repository;
import com.bbn.marti.config.Security;
import com.bbn.marti.config.Submission;
import com.bbn.marti.config.Subscription;
import com.bbn.marti.config.Tls;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;

public class DistributedConfiguration implements CoreConfig, org.apache.ignite.services.Service {

	private static final long serialVersionUID = -2752863566907557239L;

	public static final String CONFIG_NAMESPACE = "http://bbn.com/marti/xml/config";

    public static final String DEFAULT_TRUSTSTORE = "certs/files/fed-truststore.jks";
    
    private static final String CORE_CONFIG_CACHE_KEY = "coreConfig";

    static final String DEFAULT_CONFIG_FILE = "CoreConfig.xml";
    static final String EXAMPLE_BASE_CONFIG_FILE = "CoreConfig.example.xml";
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
		List<Network.Input> inputList = new ArrayList(getRemoteConfiguration().getNetwork().getInput());
        for (Network.Input input : inputList) {
            if (input.getName().equals(name)) {
                getRemoteConfiguration().getNetwork().getInput().remove(input);
                saveChangesAndUpdateCache();
            }
        }
    }

    @SuppressWarnings("rawtypes")
	public synchronized ConnectionModifyResult updateInputGroupsNoSave(String inputName, String[] groupList) throws RemoteException {
        Network.Input input = getInputByName(inputName);
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
        Network.Input input = getInputByName(inputName);
        if (input == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }
        input.setArchive(desiredState);
        return ConnectionModifyResult.SUCCESS;
    }

    public ConnectionModifyResult setArchiveOnlyFlagNoSave(String inputName, boolean desiredState) {
        Network.Input input = getInputByName(inputName);
        if (input == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }
        input.setArchiveOnly(desiredState);
        return ConnectionModifyResult.SUCCESS;
    }

    @Override
    public void saveChanges() {
    	// if TAK Server processes are local to each other and we're not on the messaging process, don't save
    	if (IgniteHolder.getInstance().areTakserverIgnitesLocal() && !LocalConfiguration.getInstance().isMessagingProfileActive()) {
    		return;
    	}
    	
    	
    	
    	// if TAK Server processes are local to each other and we're not on the messaging process or in the cluster, don't save
    	if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled() 
				|| (IgniteHolder.getInstance().areTakserverIgnitesLocal() && LocalConfiguration.getInstance().isMessagingProfileActive())) {
    		try {
				synchronized (DistributedConfiguration.class) {
					MessageConversionUtil.saveJAXifiedObject(LocalConfiguration.CONFIG_FILE, getRemoteConfiguration(), false);
				}
			} catch (FileNotFoundException fnfe) {
				// do nothing. Happens in unit test.
			} catch (JAXBException | IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			logger.debug("Skipping Core Config Save for this process");
		}
    }

    public void updateCache() {
    	// cache the updated configuration that has also been saved to disk
    	getConfigurationCache().put(CORE_CONFIG_CACHE_KEY, getRemoteConfiguration());
    }

    @Override
    public void saveChangesAndUpdateCache() {
    	updateCache();
    	saveChanges();
    }

    public synchronized NetworkInputAddResult addInputAndSave(@NotNull Network.Input input) {
        for (Network.Input loopInput : getRemoteConfiguration().getNetwork().getInput()) {
            if (loopInput.getName().equals(input.getName())) {
                return NetworkInputAddResult.FAIL_INPUT_NAME_EXISTS;
            }
        }

        // add the new input to the local configuration object, not the object from the cache
        getRemoteConfiguration().getNetwork().getInput().add(input);

        saveChangesAndUpdateCache();

        if (logger.isDebugEnabled()) {
        	logger.debug("Save and cached new input on port " + input.getPort());
        }

        return NetworkInputAddResult.SUCCESS;
    }


    public synchronized Network.Input getInputByName(@NotNull String inputName) {
        List<Network.Input> inputList = getRemoteConfiguration().getNetwork().getInput();
        for (Network.Input input : inputList) {
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
        Network.Input input = getInputByName(inputName);
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
    public synchronized List<Network.Input> getNetworkInputs() {
        return getRemoteConfiguration().getNetwork().getInput();
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
