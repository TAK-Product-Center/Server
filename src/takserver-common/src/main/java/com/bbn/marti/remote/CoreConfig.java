

package com.bbn.marti.remote;

import java.util.Set;

import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Auth.Ldap;
import com.bbn.marti.config.Buffer.LatestSA;
import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.FederationServer;
import com.bbn.marti.config.Qos;
import com.bbn.marti.config.Repository;
import com.bbn.marti.config.Tls;

public interface CoreConfig {

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
    
    boolean isContactApiFilter();
    
    Set<String> getContactApiWriteOnlyGroups();
}
