package tak.server.federation.hub;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import tak.server.federation.hub.broker.FederationHubServerConfig;
import tak.server.federation.hub.ui.FederationHubUIConfig;

public class FedhubJwtUtils {
	private static final Logger logger = LoggerFactory.getLogger(FedhubJwtUtils.class);

	private ThreadLocal<JwtParser> jwtRsaParser = new ThreadLocal<>();
	private ThreadLocal<JwtParser> jwtHmacParser = new ThreadLocal<>();
	private PrivateKey privateKey = null;
	private SecretKeySpec secretKeySpec = null;
	private PublicKey publicKey = null;
	private boolean keysLoaded = false;
	
	private static String keyStoreType = "";
	private static String keyStoreFile = "";
	private static String keyStorePass = "";
	
	private static FedhubJwtUtils instance = null;

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

	private void loadKeys() {

		if (keysLoaded) {
			return;
		}

		try {
			//
			// load keys from tls keystore
			KeyPair keyPair = loadKeyPair(keyStoreType, keyStoreFile, keyStorePass);
			if (keyPair == null) {
				logger.info("loadKeyPair returned null");
				return;
			}

			publicKey = keyPair.getPublic();
			privateKey = keyPair.getPrivate();
			secretKeySpec = new SecretKeySpec(privateKey.getEncoded(), privateKey.getAlgorithm());

			if (privateKey == null) {
				logger.info("FedHubJwtUtils unable to find PrivateKey in keystore!");
				return;
			}

			keysLoaded = true;

			jwtRsaParser.remove();
			jwtHmacParser.remove();

		} catch (Exception e) {
			logger.error("Exception in loadKeys!", e);
		}
	}

	public static FedhubJwtUtils getInstance(FederationHubUIConfig fedHubConfig) {
		if (instance == null) {
			keyStoreFile = fedHubConfig.getKeystoreFile();
			keyStorePass = fedHubConfig.getKeystorePassword();
			keyStoreType = fedHubConfig.getKeystoreType();
			instance = new FedhubJwtUtils();
			instance.loadKeys();
		}
		return instance;
	}

	public static FedhubJwtUtils getInstance(FederationHubServerConfig fedHubConfig) {
		if (instance == null) {
			keyStoreFile = fedHubConfig.getKeystoreFile();
			keyStorePass = fedHubConfig.getKeystorePassword();
			keyStoreType = fedHubConfig.getKeystoreType();
			instance = new FedhubJwtUtils();
			instance.loadKeys();
		}
		return instance;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

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

	public Claims parseClaim(String token) {
		JwtParser jwtParser = getParser(SignatureAlgorithm.HS256, privateKey);
		return jwtParser.parseClaimsJws(token).getBody();
	}

	public String createToken(String clientFingerprint, String clientGroup, long expiration) {
		List<String> clientGroups = new ArrayList<>();
		clientGroups.add(clientGroup);

		return createToken(clientFingerprint, clientGroups, expiration);
	}

	public String createToken(String clientFingerprint, List<String> clientGroups, long expiration) {
		try {
			Date now = new Date();

			JwtBuilder builder = Jwts.builder().setId("someid").setIssuedAt(now).setSubject("subject")
					.setIssuer("issuer").signWith(SignatureAlgorithm.HS256, secretKeySpec)
					.claim("federation", "federation claim val").claim("clientFingerprint", clientFingerprint)
					.claim("clientGroups", clientGroups);

			if (expiration > 0)
				builder.setExpiration(new Date(expiration));

			return builder.compact();

		} catch (Exception e) {
			logger.error("Exception in createMissionToken!", e);
			return null;
		}
	}
}
