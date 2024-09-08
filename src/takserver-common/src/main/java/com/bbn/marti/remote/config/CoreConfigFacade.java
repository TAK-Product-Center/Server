package com.bbn.marti.remote.config;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.config.*;
import com.bbn.marti.config.Auth.Ldap;
import com.bbn.marti.config.Buffer.LatestSA;
import com.bbn.marti.config.Federation.FederationServer;
import com.bbn.marti.remote.AuthenticationConfigInfo;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.MessagingConfigInfo;
import com.bbn.marti.remote.SecurityConfigInfo;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;
import org.apache.ignite.cluster.ClusterGroup;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.Constants;
import tak.server.ignite.IgniteHolder;
import tak.server.util.ActiveProfiles;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;


/**
 * This facade class hides access to the DistributedConfiguration and LocalConfiguration classes which
 * are only local to the ConfigService Microservice process and makes it "appear" local.  Note, the ConfigService
 * must be started *FIRST* before any others for this reason.
 */
public class CoreConfigFacade implements CoreConfig {

    private static final String MASK_WORD_FOR_DISPLAY = "********";

    private static final Logger logger = LoggerFactory.getLogger(CoreConfigFacade.class);

    // Singleton instance
    private static CoreConfigFacade instance = null;

    // 2 minutes should be more than long enough for the config service to start.  Otherwise, something is wrong.
    private static long DEFAULT_TIMEOUT_MILLIS = 120000;

    // The CoreConfig instance that we are wrapping / the facade for
    private CoreConfig coreConfig = null;

    // Local instance of the remoteConfiguration to avoid Ignite call behind the scenes
    private Configuration remoteConfig = null;

    // Local instance of the cachedConfiguration to avoid Ignite call behind the scenes
    private Configuration cachedConfig = null;

    private CoreConfigFacade() {
        if (ActiveProfiles.getInstance().isConfigProfileActive()) {
            coreConfig = DistributedConfiguration.getInstance();
        } else {
            // initial setting of coreConfig instance
            refreshConfiguration();
            // setup listener for updates to the
            IgniteHolder.getInstance().getIgnite().message().localListen(
                Constants.CONFIG_TOPIC_KEY, (nodeId, message) -> {
                    refreshConfiguration();
                    return true;
                });
        }
    }

    public static CoreConfigFacade getInstance() {
        if (instance == null) {
            synchronized (CoreConfigFacade.class) {
                if (instance == null) {
                    instance = new CoreConfigFacade();
                }
            }
        }
        return instance;
    }


    public void saveChanges() {
        coreConfig.saveChanges();
        notifyConfigDirty();
    }

    public Configuration getRemoteConfiguration() {
        if (remoteConfig == null) {
            remoteConfig = coreConfig.getRemoteConfiguration();
        }
        return remoteConfig;
    }

    public void saveChangesAndUpdateCache() {
        coreConfig.saveChangesAndUpdateCache();
        notifyConfigDirty();
    }

    public void loadConfigFromCache() {
        coreConfig.loadConfigFromCache();
    }

    public Configuration getCachedConfiguration() {
        if (cachedConfig == null) {
            cachedConfig = coreConfig.getCachedConfiguration();
        }
        return cachedConfig;
    }

    public void setAndSaveFederation(Federation federation) {
        coreConfig.setAndSaveFederation(federation);
        notifyConfigDirty();
    }

    public void setAndSaveLDAP(Ldap ldap) {
        coreConfig.setAndSaveLDAP(ldap);
        notifyConfigDirty();
    }

    public void setAndSaveMessagingConfig(LatestSA latestSA, Repository repository) {
        coreConfig.setAndSaveMessagingConfig(latestSA, repository);
        notifyConfigDirty();
    }

    public void setAndSaveSecurityConfig(Tls tls, Auth auth, FederationServer fedServer) {
        coreConfig.setAndSaveSecurityConfig(tls, auth, fedServer);
        notifyConfigDirty();
    }

    public void setAndSaveCertificateSigningConfig(CertificateSigning certificateSigningConfig) {
        coreConfig.setAndSaveCertificateSigningConfig(certificateSigningConfig);
        notifyConfigDirty();
    }

    public void setAndSaveQos(Qos qos) {
        coreConfig.setAndSaveQos(qos);
        notifyConfigDirty();
    }

    public void setAndSaveStoreForwardChatEnabled(boolean storeForwardChatEnabled) {
        coreConfig.setAndSaveStoreForwardChatEnabled(storeForwardChatEnabled);
        notifyConfigDirty();
    }

    public void setAndSaveVbmConfiguration(Vbm vbm) {
        coreConfig.setAndSaveVbmConfiguration(vbm);
        notifyConfigDirty();
    }

    public void setAndSaveServerId(String serverId) {
        coreConfig.setAndSaveServerId(serverId);
        notifyConfigDirty();
    }

    public void setAndSaveEnterpriseSyncSizeLimit(int uploadSizeLimit) {
        coreConfig.setAndSaveEnterpriseSyncSizeLimit(uploadSizeLimit);
        notifyConfigDirty();
    }

    public void addStaticSubscriptionAndSave(@NotNull Subscription.Static newStaticSubscription) {
        coreConfig.addStaticSubscriptionAndSave(newStaticSubscription);
        notifyConfigDirty();
    }

    public void removeStaticSubscriptionAndSave(@NotNull String subscriptionIdentifier) {
        coreConfig.removeStaticSubscriptionAndSave(subscriptionIdentifier);
        notifyConfigDirty();
    }

    public boolean isContactApiFilter() {
        return coreConfig.isContactApiFilter();
    }

    public boolean isCluster() {
        return coreConfig.isCluster();
    }

    public Set<String> getContactApiWriteOnlyGroups() {
        return coreConfig.getContactApiWriteOnlyGroups();
    }

    public NetworkInputAddResult addInputAndSave(@NotNull Input input) {
        NetworkInputAddResult nia = coreConfig.addInputAndSave(input);
        notifyConfigDirty();
        return nia;
    }

    public void removeInputAndSave(String name) throws RemoteException {
        coreConfig.removeInputAndSave(name);
        notifyConfigDirty();
    }

    public List<Input> getNetworkInputs() {
        return coreConfig.getNetworkInputs();
    }

    public List<DataFeed> getNetworkDataFeeds() {
        return coreConfig.getNetworkDataFeeds();
    }

    public Input getInputByName(@NotNull String inputName) {
        return coreConfig.getInputByName(inputName);
    }

    public ConnectionModifyResult updateInputGroupsNoSave(String inputName, String[] groupList) throws RemoteException {
        ConnectionModifyResult cmr = coreConfig.updateInputGroupsNoSave(inputName, groupList);
        notifyConfigDirty();
        return cmr;
    }

    public ConnectionModifyResult setArchiveFlagNoSave(String inputName, boolean desiredState) {
        ConnectionModifyResult cmr = coreConfig.setArchiveFlagNoSave(inputName, desiredState);
        notifyConfigDirty();
        return cmr;
    }

    public ConnectionModifyResult setArchiveOnlyFlagNoSave(String inputName, boolean desiredState) {
        ConnectionModifyResult cmr = coreConfig.setArchiveOnlyFlagNoSave(inputName, desiredState);
        notifyConfigDirty();
        return cmr;
    }

    public ConnectionModifyResult setFederatedFlagNoSave(String inputName, boolean desiredState) {
        ConnectionModifyResult cmr = coreConfig.setFederatedFlagNoSave(inputName, desiredState);
        notifyConfigDirty();
        return cmr;
    }

    public ConnectionModifyResult updateTagsNoSave(String inputName, List<String> newTagList) throws RemoteException {
        ConnectionModifyResult cmr = coreConfig.updateTagsNoSave(inputName, newTagList);
        notifyConfigDirty();
        return cmr;
    }

    public ConnectionModifyResult setSyncCacheRetentionSeconds(String inputName, int desiredState) {
        ConnectionModifyResult cmr = coreConfig.setSyncCacheRetentionSeconds(inputName, desiredState);
        notifyConfigDirty();
        return cmr;
    }

    public ConnectionModifyResult setSyncFlagNoSave(String dataFeedName, boolean desiredState) {
        ConnectionModifyResult cmr = coreConfig.setSyncFlagNoSave(dataFeedName, desiredState);
        notifyConfigDirty();
        return cmr;
    }

    public void removeDataFeedAndSave(String name) throws RemoteException {
        coreConfig.removeDataFeedAndSave(name);
        notifyConfigDirty();
    }

    /**
     * Internal method to refresh the DistributedConfiguration instance we are holding locally
     * Note that DistributedConfiguration implements the CoreConfig interface.
     *
     * @param group the clustergroup to retrieve the configuration for.
     * @return the CoreConfig implementation used by this process
     */
    private CoreConfig getConfiguration(ClusterGroup group) {
        CoreConfig distributedConfiguration = IgniteHolder.getInstance()
            .getIgnite()
            .services(group)
            .serviceProxy(Constants.DISTRIBUTED_CONFIGURATION, CoreConfig.class, false, DEFAULT_TIMEOUT_MILLIS);

        if (distributedConfiguration == null) {
            logger.error("Unable to retrieve configuration remotely from the Configuration Microservice.  The " +
                "Configuration Microservice must be started *before* the other services.  Please make sure it is " +
                "running and check /opt/tak/takserver-config.log for any errors in startup.");
        }

        return distributedConfiguration;
    }

    public void modifyMessagingConfig(MessagingConfigInfo info) {
        Configuration conf = getRemoteConfiguration();
        Repository repository = conf.getRepository();
        LatestSA latestSA = conf.getBuffer().getLatestSA();
        latestSA.setEnable(info.isLatestSA());
        repository.setNumDbConnections(info.getNumDbConnections());
        repository.setConnectionPoolAutoSize(info.isConnectionPoolAutoSize());
        repository.setArchive(info.isArchive());
        repository.getConnection().setUsername(info.getDbUsername());
        if (!info.getDbPassword().equals(MASK_WORD_FOR_DISPLAY)) {
            repository.getConnection().setPassword(info.getDbPassword());
        }
        repository.getConnection().setUrl(info.getDbUrl());
        repository.getConnection().setSslEnabled(info.isSslEnabled());
        repository.getConnection().setSslMode(info.getSslMode());
        repository.getConnection().setSslCert(info.getSslCert());
        repository.getConnection().setSslKey(info.getSslKey());
        repository.getConnection().setSslRootCert(info.getSslRootCert());
        setAndSaveMessagingConfig(latestSA, repository);
    }

    public void modifyAuthenticationConfig(AuthenticationConfigInfo info) {
        Configuration localConfig = getRemoteConfiguration();

        Ldap ldap = localConfig.getAuth().getLdap();
        if (ldap == null) {
            ldap = new Ldap();
        }
        ldap.setUrl(info.getUrl());
        ldap.setUserstring(info.getUserString());
        ldap.setUpdateinterval(info.getUpdateInterval());
        ldap.setGroupprefix(info.getGroupPrefix());
        ldap.setServiceAccountDN(info.getServiceAccountDN());
        ldap.setServiceAccountCredential(info.getServiceAccountCredential());
        ldap.setGroupBaseRDN(info.getGroupBaseRDN());
        if (logger.isDebugEnabled()) {
            logger.debug("group prefix is now: " + ldap.getGroupprefix());
        }
        setAndSaveLDAP(ldap);
    }

    public void modifySecurityConfig(SecurityConfigInfo info) {
        Configuration conf = getRemoteConfiguration();
        Tls tls = conf.getSecurity().getTls();
        FederationServer fedServer = conf.getFederation().getFederationServer();
        Auth auth = conf.getAuth();
        tls.setKeystoreFile(info.getKeystoreFile());
        tls.setTruststoreFile(info.getTruststoreFile());
        tls.setKeystorePass(info.getKeystorePass());
        tls.setTruststorePass(info.getTruststorePass());
        tls.setContext(info.getTlsVersion());
        auth.setX509Groups(info.isX509Groups());
        auth.setX509AddAnonymous(info.isX509addAnon());
        fedServer.getTls().setKeystoreFile(info.getKeystoreFile());
        fedServer.getTls().setKeystorePass(info.getKeystorePass());
        if (auth.getLdap() != null) {
            auth.getLdap().setX509Groups(info.isX509Groups());
            auth.getLdap().setX509AddAnonymous(info.isX509addAnon());
        }
        setAndSaveSecurityConfig(tls, auth, fedServer);

        if (info.isEnableEnrollment()) {
            CertificateSigning certificateSigning = CoreConfigFacade.getInstance().getRemoteConfiguration().getCertificateSigning();
            if (certificateSigning == null) {
                certificateSigning = new CertificateSigning();
            }

            CertificateConfig certificateConfig = certificateSigning.getCertificateConfig();
            if (certificateConfig == null) {
                certificateConfig = new CertificateConfig();
                certificateSigning.setCertificateConfig(certificateConfig);
            }

            NameEntries nameEntries = certificateConfig.getNameEntries();
            if (nameEntries == null) {
                nameEntries = new NameEntries();

                NameEntry nameEntry = new NameEntry();
                nameEntry.setName("O");
                nameEntry.setValue("TAK");
                nameEntries.getNameEntry().add(nameEntry);

                nameEntry = new NameEntry();
                nameEntry.setName("OU");
                nameEntry.setValue("TAK");
                nameEntries.getNameEntry().add(nameEntry);

                certificateConfig.setNameEntries(nameEntries);
            }

            certificateSigning.setCA(CAType.fromValue(info.getCaType()));

            if (certificateSigning.getCA() == CAType.TAK_SERVER) {
                TAKServerCAConfig takServerCAConfig = new TAKServerCAConfig();
                takServerCAConfig.setKeystore("JKS");
                takServerCAConfig.setSignatureAlg("SHA256WithRSA");
                takServerCAConfig.setKeystoreFile(info.getSigningKeystoreFile());
                takServerCAConfig.setKeystorePass(info.getSigningKeystorePass());
                takServerCAConfig.setValidityDays(info.getValidityDays());
                certificateSigning.setTAKServerCAConfig(takServerCAConfig);

            } else if (certificateSigning.getCA() == CAType.MICROSOFT_CA) {
                MicrosoftCAConfig microsoftCAConfig = new MicrosoftCAConfig();
                microsoftCAConfig.setUsername(info.getMscaUserName());
                microsoftCAConfig.setPassword(info.getMscaPassword());
                microsoftCAConfig.setTruststore(info.getMscaTruststore());
                microsoftCAConfig.setTruststorePass(info.getMscaTruststorePass());
                microsoftCAConfig.setTemplateName(info.getMscaTemplateName());
                certificateSigning.setMicrosoftCAConfig(microsoftCAConfig);
            }

            setAndSaveCertificateSigningConfig(certificateSigning);
        }
    }

    /**
     * Notifies all configuration facades that the configuration is dirty.
     */
    private void notifyConfigDirty() {
        IgniteHolder.getInstance().getIgnite().message().send(Constants.CONFIG_TOPIC_KEY, "");
    }

    /**
     * Called by the ConfigServiceConfiguration when there is an update and this needs to be updated
     */
    public void refreshConfiguration() {
        if (!ActiveProfiles.getInstance().isConfigProfileActive()) {
            coreConfig = getConfiguration(ClusterGroupDefinition.getConfigClusterDeploymentGroup(
                IgniteHolder.getInstance().getIgnite()));
            remoteConfig = coreConfig.getRemoteConfiguration();
            cachedConfig = coreConfig.getCachedConfiguration();
        }
    }
}
