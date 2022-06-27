package tak.server.federation.hub.broker;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

public class SSLConfig {
    private final static Logger logger = LoggerFactory.getLogger(SSLConfig.class);

    private TrustManagerFactory trustMgrFactory;
    private KeyManagerFactory keyMgrFactory;
    private SslContext sslContext; // context used to source the tabula rasa engine
    private KeyStore trust;
    private KeyStore self;
    private FederationHubServerConfig config;

    public SslContext initSslContext(FederationHubServerConfig config) {
        this.config = config;

        try {
            // Initialize keystore.
            self = KeyStore.getInstance(config.getKeystoreType());
            if (Strings.isNullOrEmpty(config.getKeystorePassword())) {
                throw new IllegalArgumentException("empty or null key store password ");
            }
            self.load(getFileOrResource(config.getKeystoreFile()), config.getKeystorePassword().toCharArray());
            keyMgrFactory = KeyManagerFactory.getInstance(config.getKeyManagerType());
            keyMgrFactory.init(self, config.getKeystorePassword().toCharArray());

            // Initialize truststore.
            trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trust = KeyStore.getInstance(config.getTruststoreType());
            if (Strings.isNullOrEmpty(config.getTruststorePassword())) {
                throw new IllegalArgumentException("empty or null truststore password ");
            }
            trust.load(getFileOrResource(config.getTruststoreFile()), config.getTruststorePassword().toCharArray());
            trustMgrFactory.init(trust);

            sslContext = GrpcSslContexts.configure(SslContextBuilder.forServer(keyMgrFactory), SslProvider.OPENSSL) // this ensures that we are using OpenSSL, not JRE SSL
                .protocols("TLSv1.2","TLSv1.3")
                .trustManager(trustMgrFactory)
                .clientAuth(ClientAuth.REQUIRE) // client auth always required
                .build();

            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SSLEngine buildServerEngine() {
        return null;
    }

    public SSLEngine buildClientEngine() {
        return null;
    }

    public synchronized void refresh() {
        try {
            this.trust = initTrust(config, trustMgrFactory);
//            sslContext.init(keyMgrFactory.getKeyManagers(), trustMgrFactory.getTrustManagers(), new SecureRandom());
        } catch (RuntimeException e) {
            logger.warn("Exeception refreshing SSL Context", e);
        }
    }

    public static KeyStore initTrust(FederationHubServerConfig config, TrustManagerFactory trustManagerFactory)
            throws RuntimeException {
        KeyStore trust;
        try {
            String truststoreType = config.getTruststoreType();
            String truststoreFile = config.getTruststoreFile();
            if (Strings.isNullOrEmpty(config.getTruststorePassword())) {
                throw new IllegalArgumentException("empty or null truststore password ");
            }
            String truststorePassword = config.getTruststorePassword();

            trust = KeyStore.getInstance(truststoreType);
            trust.load(new FileInputStream(truststoreFile), truststorePassword.toCharArray());
            trustManagerFactory.init(trust);
            return trust;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public TrustManagerFactory getTrustMgrFactory() {
        return trustMgrFactory;
    }

    public void setTrustMgrFactory(TrustManagerFactory trustMgrFactory) {
        this.trustMgrFactory = trustMgrFactory;
    }

    public KeyManagerFactory getKeyMgrFactory() {
        return keyMgrFactory;
    }

    public void setKeyMgrFactory(KeyManagerFactory keyMgrFactory) {
        this.keyMgrFactory = keyMgrFactory;
    }

    public SslContext getSslContext() {
        return sslContext;
    }


    public KeyStore getTrust() {
        return trust;
    }

    public KeyStore getSelf() {
        return self;
    }

    private InputStream getFileOrResource(String name) throws IOException {

        if (getClass().getResource(name) != null) {
            // it's a resource
            logger.debug(name + " is a resource");

            return this.getClass().getResourceAsStream(name);
        }

        // it's a file, or nothing
        logger.debug(name + " could be a file");

        return new FileInputStream(name);
    }


}
