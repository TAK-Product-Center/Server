package tak.server.federation.hub.ui.keycloak;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import tak.server.federation.hub.ui.FederationHubUIConfig;

public class KeycloakTokenParser {
	private Logger logger = LoggerFactory.getLogger(KeycloakTokenParser.class);
	
	private ThreadLocal<JwtParser> jwtRsaParser = new ThreadLocal<>();
    private ThreadLocal<JwtParser> jwtHmacParser = new ThreadLocal<>();
    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;
    private boolean keysLoaded = false;

    private FederationHubUIConfig fedHubConfig = null;
    
    public KeycloakTokenParser(FederationHubUIConfig fedHubConfig) {
    	this.fedHubConfig = fedHubConfig;
    	loadKeys();
    }

    private void loadKeys()  {

        if (keysLoaded) {
            return;
        }

        try {
            //
            // load keys from tls keystore
            //
            String keyStoreType = fedHubConfig.getKeystoreType();
            String keyStoreFile = fedHubConfig.getKeystoreFile();
            String keyStorePass = fedHubConfig.getKeystorePassword();

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

            jwtRsaParser.remove();
            jwtHmacParser.remove();

        } catch (Exception e) {
            logger.error("Exception in JwtUtils loadKeys!", e);
        }
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
            List<RSAPublicKey> rsaPublicKeys = new ArrayList<>();

            byte[] keyBytes = Files.readAllBytes(Paths.get("/opt/tak/certs/sclz.2.der"));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            rsaPublicKeys.add((RSAPublicKey) kf.generatePublic(spec));

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

    public Claims parseClaims(String token, SignatureAlgorithm signatureAlgorithm) {
        List<JwtParser> jwtParsers = getExternalParsers(signatureAlgorithm);
        jwtParsers.add(getParser(signatureAlgorithm, privateKey));

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
}
