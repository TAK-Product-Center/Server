package com.bbn.marti.jwt;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Enumeration;

public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private ThreadLocal<JwtParser> jwtRsaParser = new ThreadLocal<>();
    private ThreadLocal<JwtParser> jwtHmacParser = new ThreadLocal<>();
    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;
    private boolean keysLoaded = false;
    private boolean keysGenerated = false;
    CoreConfig coreConfig;

    private static JwtUtils instance = null;

    private void loadKeys()  {

        if (keysLoaded) {
            return;
        }

        try {
            //
            // load keys from tls keystore
            //

            String keyStoreType = coreConfig().getRemoteConfiguration().getSecurity().getTls().getKeystore();
            String keyStoreFile = coreConfig().getRemoteConfiguration().getSecurity().getTls().getKeystoreFile();
            String keyStorePass = coreConfig().getRemoteConfiguration().getSecurity().getTls().getKeystorePass();

            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(new FileInputStream(keyStoreFile), keyStorePass.toCharArray());

            PrivateKey search = null;
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements() && privateKey == null) {
                String alias = aliases.nextElement();
                privateKey = (PrivateKey)keyStore.getKey(alias, keyStorePass.toCharArray());
                if (privateKey != null) {
                    Certificate cert = keyStore.getCertificate(alias);
                    publicKey = cert.getPublicKey();
                }
            }

            if (privateKey == null) {
                logger.error("JwtUtils unable to find PrivateKey in keystore!");
                return;
            }

            keysLoaded = true;
            keysGenerated = false;

            jwtRsaParser.remove();
            jwtHmacParser.remove();

        } catch (Exception e) {
            logger.error("Exception in JwtUtils loadKeys!", e);
        }
    }

    private void generateKeys()  {

        if (keysGenerated) {
            return;
        }

        try {
            //
            // generate a new KeyPair
            //
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            keysGenerated = true;
            keysLoaded = false;

            jwtRsaParser.remove();
            jwtHmacParser.remove();

        } catch (Exception e) {
            logger.error("Exception in JwtUtils generateKeys!", e);
        }
    }

    public static JwtUtils getInstance() {
        if (instance == null) {
            instance = new JwtUtils();
            instance.loadKeys();
        }
        return instance;
    }

    public static JwtUtils getInstanceGenerateKeys() {
        if (instance == null) {
            instance = new JwtUtils();
            instance.generateKeys();
        }
        return instance;
    }

    public PublicKey getPublicKey() { return publicKey; }
    public PrivateKey getPrivateKey() { return privateKey; }

    private JwtParser getParser(SignatureAlgorithm signatureAlgorithm) {

        JwtParser parser = null;
        if (signatureAlgorithm == SignatureAlgorithm.RS256) {
            parser = jwtRsaParser.get();
            if (parser == null) {
                parser = Jwts.parser();
                parser.setSigningKey(privateKey);
                jwtRsaParser.set(parser);
            }
        } else if (signatureAlgorithm == SignatureAlgorithm.HS256) {
            parser = jwtHmacParser.get();
            if (parser == null) {
                parser = Jwts.parser();
                parser.setSigningKey(privateKey.getEncoded());
                jwtHmacParser.set(parser);
            }
        }

        return parser;
    }

    public Claims parseClaims(String token, SignatureAlgorithm signatureAlgorithm) {
        return getParser(signatureAlgorithm).parseClaimsJws(token).getBody();
    }

    private CoreConfig coreConfig() {
        if (coreConfig == null) {
            synchronized(this) {
                if (coreConfig == null) {
                    if (SpringContextBeanForApi.getSpringContext() != null) {
                        coreConfig = SpringContextBeanForApi.getSpringContext().getBean(CoreConfig.class);
                    }
                }
            }
        }
        return coreConfig;
    }

}
