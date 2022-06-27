package tak.server.federation.hub.ui;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/* Performs SSL configuration so that client certificates can be checked. */

@Configuration
public class SSLConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Autowired
    private FederationHubUIConfig fedHubConfig;

    @Override
    public void customize(ConfigurableServletWebServerFactory server) {
        Ssl ssl = new Ssl();
        ssl.setEnabled(true);
        ssl.setClientAuth(Ssl.ClientAuth.NEED);

        ssl.setKeyStore(fedHubConfig.getKeystoreFile());
        ssl.setKeyStorePassword(fedHubConfig.getKeystorePassword());
        ssl.setKeyStoreType(fedHubConfig.getKeystoreType());

        ssl.setTrustStore(fedHubConfig.getTruststoreFile());
        ssl.setTrustStorePassword(fedHubConfig.getTruststorePassword());
        ssl.setTrustStoreType(fedHubConfig.getTruststoreType());

        ssl.setKeyAlias(fedHubConfig.getKeyAlias());

        server.setSsl(ssl);
    }
}
