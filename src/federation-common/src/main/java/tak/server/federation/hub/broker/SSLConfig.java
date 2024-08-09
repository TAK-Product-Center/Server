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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSslServerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

public class SSLConfig {
    private final static Logger logger = LoggerFactory.getLogger(SSLConfig.class);

    private TrustManagerFactory trustMgrFactory;
    private KeyManagerFactory keyMgrFactory;
    private SslContext sslContextClientAuth;
    private SslContext sslContextNoAuth;
    private KeyStore trust;
    private KeyStore self;
    private FederationHubServerConfig config;

    public void initSslContext(FederationHubServerConfig config) {
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

            sslContextClientAuth = GrpcSslContexts.configure(SslContextBuilder.forServer(keyMgrFactory), SslProvider.OPENSSL) // this ensures that we are using OpenSSL, not JRE SSL
                .protocols("TLSv1.2","TLSv1.3")
                .trustManager(trustMgrFactory)
                .clientAuth(ClientAuth.REQUIRE) // client auth always required
                .build();
            
            sslContextNoAuth = GrpcSslContexts.configure(SslContextBuilder.forServer(keyMgrFactory), SslProvider.OPENSSL) // this ensures that we are using OpenSSL, not JRE SSL
                    .protocols("TLSv1.2","TLSv1.3")
                    .trustManager(trustMgrFactory)
                    .clientAuth(ClientAuth.NONE) // client auth always required
                    .build();

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

			// Get the current SSL context attached to the running server and update the truststore
			// Newly created SSL sessions will use this new truststore. Existing SSL sessions will not be forced to reconnect
			// (Note: This only applies to refreshes for adding CA's. When CA's are deleted, all connections will be forced to reconnect)
			OpenSslServerContext openSslServerSessionContextClientAuth = (OpenSslServerContext) sslContextClientAuth;
			openSslServerSessionContextClientAuth.updateSslContext(trustMgrFactory);
			
			OpenSslServerContext openSslServerSessionContextNoAuth = (OpenSslServerContext) sslContextNoAuth;
			openSslServerSessionContextNoAuth.updateSslContext(trustMgrFactory);
		} catch (Exception e) {
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

    public SslContext getSslContextClientAuth() {
        return sslContextClientAuth;
    }

    public SslContext getSslContextNoAuth() {
        return sslContextNoAuth;
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
