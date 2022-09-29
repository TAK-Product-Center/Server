package com.bbn.marti.jwt;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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

    private JwtParser getParser(SignatureAlgorithm signatureAlgorithm, Key key) {

        JwtParser parser = null;
        if (signatureAlgorithm == SignatureAlgorithm.RS256) {
            parser = jwtRsaParser.get();
            if (parser == null) {
                parser = Jwts.parser();
                parser.setSigningKey(key);
                jwtRsaParser.set(parser);
            }
        } else if (signatureAlgorithm == SignatureAlgorithm.HS256) {
            parser = jwtHmacParser.get();
            if (parser == null) {
                parser = Jwts.parser();
                parser.setSigningKey(key.getEncoded());
                jwtHmacParser.set(parser);
            }
        }

        return parser;
    }

    public List<RSAPublicKey> getExternalVerifiers() {
        try {
            Oauth oAuth = coreConfig.getRemoteConfiguration().getAuth().getOauth();
            if (oAuth == null) {
                logger.error("OAuth config not found");
                return null;
            }

            if (oAuth.getAuthServer() == null || oAuth.getAuthServer().size() == 0) {
                logger.error("No auth servers configured");
                return null;
            }

            //
            // iterate across our configured authorization servers and add their issuer public keys
            //
            List<RSAPublicKey> rsaPublicKeys = new ArrayList<>();
            for (Oauth.AuthServer authServer : oAuth.getAuthServer()) {
                byte[] keyBytes = Files.readAllBytes(Paths.get(authServer.getIssuer()));
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                rsaPublicKeys.add((RSAPublicKey) kf.generatePublic(spec));
            }

            return rsaPublicKeys;
        } catch (Exception e) {
            logger.error("exception in getExternalVerifiers!");
            return  null;
        }
    }

    private List<JwtParser> getExternalParsers(SignatureAlgorithm signatureAlgorithm) {
        List<JwtParser> jwtParsers = new ArrayList<>();
        for (RSAPublicKey rsaPublicKey : getExternalVerifiers()) {
            jwtParsers.add(Jwts.parser().setSigningKey(rsaPublicKey));
        }
        return jwtParsers;
    }

    public Claims parseClaim(String token, JwtParser jwtParser) {
        try {
            Claims claims = jwtParser.parseClaimsJws(token).getBody();
            if (claims != null) {
                return claims;
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("exception parsing token " + token, e);
            }
        }

        return null;
    }

    public Claims parseClaims(String token, SignatureAlgorithm signatureAlgorithm) {
        // try to verify the claims using the tls keystore
        Claims claims = parseClaim(token, getParser(signatureAlgorithm, privateKey));
        if (claims != null) {
            return claims;
        }

        // try to verify the claims using the external verifiers
        for (JwtParser jwtParser : getExternalParsers(signatureAlgorithm)) {
            claims = parseClaim(token, jwtParser);
            if (claims != null) {
                return claims;
            }
        }

        return null;
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
