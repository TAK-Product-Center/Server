package tak.server.federation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.exception.TakException;
import com.bbn.roger.fig.model.FigServerConfig;

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
    private FigServerConfig config;

    public SslContext initSslContext(FigServerConfig config) {
        this.config = config;

        try {
            // initialize keystore
            self = KeyStore.getInstance(config.getKeystoreType());
            self.load(getFileOrResource(config.getKeystoreFile()), config.getKeystorePassword().toCharArray());
            keyMgrFactory = KeyManagerFactory.getInstance(config.getKeymanagerType());
            keyMgrFactory.init(self, config.getKeystorePassword().toCharArray());

            // initialize truststore
            trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trust = KeyStore.getInstance(config.getTruststoreType());
            trust.load(getFileOrResource(config.getTruststoreFile()), config.getTruststorePassword().toCharArray());
            trustMgrFactory.init(trust);

            SslContextBuilder sslContextBuilder = GrpcSslContexts.configure(SslContextBuilder.forServer(keyMgrFactory), SslProvider.OPENSSL) // this ensures that we are using OpenSSL, not JRE SSL
            		.trustManager(trustMgrFactory)
                    .clientAuth(ClientAuth.REQUIRE); // client auth always required

            String context = "TLSv1.2,TLSv1.3";

            String ciphers = config.getCiphers();
            if (!Strings.isNullOrEmpty(ciphers)) {
                sslContextBuilder = sslContextBuilder.ciphers(Arrays.asList(ciphers.split(",")));
                // only set context from config if cipher is also present
                if (!Strings.isNullOrEmpty(config.getContext())) {
                    context = config.getContext();
                }
            }

            sslContext = sslContextBuilder.protocols(Arrays.asList(context.split(","))).build();

            return sslContext;

        } catch (Exception e) {
            throw new TakException(e);
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
        } catch (Exception e) {
            logger.warn("Exeception refreshing SSL Context", e);
        }
    }

    public static KeyStore initTrust(FigServerConfig config, TrustManagerFactory trustManagerFactory) {
        KeyStore trust;
        try {
            String truststoreType = config.getTruststoreType();
            String truststoreFile = config.getTruststoreFile();
            String truststorePassword = config.getTruststorePassword();

            trust = KeyStore.getInstance(truststoreType);
            trust.load(new FileInputStream(truststoreFile), truststorePassword.toCharArray());
            trustManagerFactory.init(trust);
            return trust;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new TakException(e);
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
