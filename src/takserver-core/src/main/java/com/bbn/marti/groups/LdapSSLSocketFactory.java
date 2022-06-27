package com.bbn.marti.groups;

import com.bbn.marti.service.DistributedConfiguration;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LdapSSLSocketFactory extends SSLSocketFactory {

    Logger logger = LoggerFactory.getLogger(LdapSSLSocketFactory.class);
    private static LdapSSLSocketFactory instance = null;
    private SSLSocketFactory socketFactory = null;

    public static SocketFactory getDefault() {
        return getInstance();
    }

    private synchronized static SSLSocketFactory getInstance() {
        if (instance == null) {
            instance = new LdapSSLSocketFactory();
        }
        return instance;
    }

    public LdapSSLSocketFactory() {
        super();

        try {
            DistributedConfiguration config = DistributedConfiguration.getInstance();
            if (config == null || config.getAuth() == null || config.getAuth().getLdap() == null) {
                logger.error("Can't find ldap element in CoreConfig!");
                return;
            }

            String truststore = config.getAuth().getLdap().getLdapsTruststore();
            String truststoreFile = config.getAuth().getLdap().getLdapsTruststoreFile();
            String truststorePass = config.getAuth().getLdap().getLdapsTruststorePass();

            if (truststore == null || truststore.length() == 0 ||
                    truststoreFile == null || truststoreFile.length() == 0 ||
                    truststorePass == null || truststorePass.length() == 0) {
                logger.error("Can't find ldap truststore in CoreConfig!");
                return;
            }

            //
            // create an ssl context using the ldap truststore
            //
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(truststore);
            keyStore.load(new FileInputStream(truststoreFile), truststorePass.toCharArray());
            trustManagerFactory.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());

            // save the socket factory from the context
            socketFactory = sslContext.getSocketFactory();

        } catch (Exception e) {
            logger.error("Exception in LdapSSLSocketFactory()!, " + e.getMessage());
        }
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return socketFactory.createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(Socket socket, InputStream inputStream, boolean autoClose) throws IOException {
        return socketFactory.createSocket(socket, inputStream, autoClose);
    }

    @Override
    public Socket createSocket(String var1, int var2) throws IOException {
        return socketFactory.createSocket(var1, var2);
    }

    @Override
    public Socket createSocket(String var1, int var2, InetAddress var3, int var4) throws IOException {
        return socketFactory.createSocket(var1, var2, var3, var4);
    }

    @Override
    public Socket createSocket(InetAddress var1, int var2) throws IOException {
        return socketFactory.createSocket(var1, var2);
    }

    @Override
    public Socket createSocket(InetAddress var1, int var2, InetAddress var3, int var4) throws IOException {
        return socketFactory.createSocket(var1, var2, var3, var4);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return socketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return socketFactory.getSupportedCipherSuites();
    }
}