

package com.bbn.marti.remote;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Auth.Ldap;
import com.bbn.marti.config.Buffer.LatestSA;
import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.FederationServer;
import com.bbn.marti.config.Input;
import com.bbn.marti.config.Qos;
import com.bbn.marti.config.Repository;
import com.bbn.marti.config.Subscription;
import com.bbn.marti.config.Tls;
import com.bbn.marti.config.Vbm;
import com.bbn.marti.remote.groups.NetworkInputAddResult;

public interface CoreConfig {

    static final String DEFAULT_TRUSTSTORE = "certs/files/fed-truststore.jks";

    void saveChanges();

    Configuration getRemoteConfiguration();

	void saveChangesAndUpdateCache();

	void loadConfigFromCache();

	// Get the config directly from the cache. This is intended for troubleshooting the config cache (see ConfigAPI.java)
    Configuration getCachedConfiguration();

    void setAndSaveFederation(Federation federation);

    void setAndSaveLDAP(Ldap ldap);

    void setAndSaveMessagingConfig(LatestSA latestSA, Repository repository);

    void setAndSaveSecurityConfig(Tls tls, Auth auth, FederationServer fedServer);

    void setAndSaveCertificateSigningConfig(CertificateSigning certificateSigningConfig);

	void setAndSaveQos(Qos qos);

    void setAndSaveStoreForwardChatEnabled(boolean storeForwardChatEnabled);
	
    void setAndSaveVbmConfiguration(Vbm vbm);

    void setAndSaveServerId(String serverId);

    void setAndSaveEnterpriseSyncSizeLimit(int uploadSizeLimit);

    void addStaticSubscriptionAndSave(@NotNull Subscription.Static newStaticSubscription);

    void removeStaticSubscriptionAndSave(@NotNull String subscriptionIdentifier);

    boolean isContactApiFilter();
    
    Set<String> getContactApiWriteOnlyGroups();

    NetworkInputAddResult addInputAndSave(@NotNull Input input);

    void removeInputAndSave(String name) throws RemoteException;

    List<Input> getNetworkInputs();

    List<DataFeed> getNetworkDataFeeds();

    Input getInputByName(@NotNull String inputName);

    ConnectionModifyResult updateInputGroupsNoSave(String inputName, String[] groupList) throws RemoteException;

    ConnectionModifyResult setArchiveFlagNoSave(String inputName, boolean desiredState);

    ConnectionModifyResult setArchiveOnlyFlagNoSave(String inputName, boolean desiredState);

    ConnectionModifyResult setFederatedFlagNoSave(String inputName, boolean desiredState);

    ConnectionModifyResult updateTagsNoSave(String inputName, List<String> newTagList) throws RemoteException;

    ConnectionModifyResult setSyncCacheRetentionSeconds(String inputName, int desiredState);

    ConnectionModifyResult setSyncFlagNoSave(String dataFeedName, boolean desiredState);

    void removeDataFeedAndSave(String name) throws RemoteException;

    boolean isCluster();
}
