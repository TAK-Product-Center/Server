package com.bbn.marti.jwt;

import com.bbn.marti.config.MissionTls;
import com.bbn.marti.config.Oauth;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.config.CoreConfigFacade;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
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
    private static JwtUtils instance = null;

    private KeyPair loadKeyPair(String keyStoreType, String keyStoreFile, String keyStorePass) {
        try {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(new FileInputStream(keyStoreFile), keyStorePass.toCharArray());

            PrivateKey privateKeyTmp = null;
            PublicKey publicKeyTmp = null;

            PrivateKey search = null;
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements() && privateKeyTmp == null) {
                String alias = aliases.nextElement();
                privateKeyTmp = (PrivateKey) keyStore.getKey(alias, keyStorePass.toCharArray());
                if (privateKeyTmp != null) {
                    Certificate cert = keyStore.getCertificate(alias);
                    publicKeyTmp = cert.getPublicKey();
                }
            }

            KeyPair keyPair = new KeyPair(publicKeyTmp, privateKeyTmp);
            return keyPair;

        } catch (Exception e) {
            logger.error("exception in loadKeyPair", e);
            return null;
        }
    }

    private void loadKeys()  {

        if (keysLoaded) {
            return;
        }

        try {
            //
            // load keys from tls keystore
            //
            CoreConfig coreConfig = CoreConfigFacade.getInstance();
            String keyStoreType = coreConfig.getRemoteConfiguration().getSecurity().getTls().getKeystore();
            String keyStoreFile = coreConfig.getRemoteConfiguration().getSecurity().getTls().getKeystoreFile();
            String keyStorePass = coreConfig.getRemoteConfiguration().getSecurity().getTls().getKeystorePass();

            KeyPair keyPair = loadKeyPair(keyStoreType, keyStoreFile, keyStorePass);
            if (keyPair == null) {
                logger.error("loadKeyPair returned null");
                return;
            }

            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

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
            Oauth oAuth = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth();
            if (oAuth == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("OAuth config not found");
                }
                return null;
            }

            if (oAuth.getAuthServer() == null) {
                logger.error("No auth server configured");
                return null;
            }

            List<RSAPublicKey> rsaPublicKeys = new ArrayList<>();

            for (Oauth.AuthServer authServer : oAuth.getAuthServer()) {
                byte[] keyBytes = Files.readAllBytes(Paths.get(authServer.getIssuer()));
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                rsaPublicKeys.add((RSAPublicKey) kf.generatePublic(spec));
            }

            return rsaPublicKeys;
        } catch (Exception e) {
            logger.error("exception in getExternalVerifiers!", e);
            return  null;
        }
    }

    private List<JwtParser> getExternalParsers(SignatureAlgorithm signatureAlgorithm) {
        List<JwtParser> jwtParsers = new ArrayList<>();
        List<RSAPublicKey> rsaPublicKeys = getExternalVerifiers();
        if (rsaPublicKeys != null) {
            for (RSAPublicKey rsaPublicKey : rsaPublicKeys) {
                jwtParsers.add(Jwts.parser().setSigningKey(rsaPublicKey));
            }
        }
        return jwtParsers;
    }

    public Claims parseClaim(String token, JwtParser jwtParser) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public Claims parseClaims(String token, List<JwtParser> jwtParsers) {
        for (JwtParser jwtParser : jwtParsers) {
            try {
                Claims claims = parseClaim(token, jwtParser);
                if (claims != null) {
                    return claims;
                }
            } catch (ExpiredJwtException exp) {
                throw exp;
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("exception in parseClaims", e);
                }
            }
        }

        return null;
    }

    public Claims parseClaims(String token, SignatureAlgorithm signatureAlgorithm) {
        List<JwtParser> jwtParsers = getExternalParsers(signatureAlgorithm);
        jwtParsers.add(getParser(signatureAlgorithm, privateKey));

        return parseClaims(token, jwtParsers);
    }

    public Claims parseMissionTokenClaims(String token) {
        List<JwtParser> jwtParsers = new ArrayList<>();

        jwtParsers.add(getParser(SignatureAlgorithm.HS256, privateKey));

        try {
            for (MissionTls missionTls : CoreConfigFacade.getInstance().getRemoteConfiguration().getSecurity().getMissionTls()) {
                KeyPair keyPair = loadKeyPair(
                        missionTls.getKeystore(), missionTls.getKeystoreFile(), missionTls.getKeystorePass());
                jwtParsers.add(Jwts.parser().setSigningKey(keyPair.getPrivate().getEncoded()));
            }
        } catch (Exception e) {
            logger.error("exception adding missionTls keystores", e);
        }

        return parseClaims(token, jwtParsers);
    }

}
